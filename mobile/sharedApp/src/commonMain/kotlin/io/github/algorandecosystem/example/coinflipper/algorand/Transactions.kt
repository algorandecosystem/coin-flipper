package io.github.algorandecosystem.example.coinflipper.algorand

import kotlin.io.encoding.Base64

data class SuggestedParams(
    val fee: Long,
    val minFee: Long,
    val genesisId: String,
    val genesisHash: ByteArray,
    val lastRound: Long,
) {
    val firstValid: Long get() = lastRound
    val lastValid: Long get() = lastRound + 1000

    companion object {
        fun fromAlgod(response: TransactionParamsResponse) =
            SuggestedParams(
                fee = response.fee,
                minFee = response.minFee,
                genesisId = response.genesisId,
                genesisHash = Base64.decode(response.genesisHash),
                lastRound = response.lastRound,
            )
    }
}

enum class OnComplete(
    val code: Int,
) {
    NoOp(0),
    OptIn(1),
    CloseOut(2),
}

data class BoxReference(
    /** 0 means the app being called; otherwise a 1-based index into foreignApps. */
    val appIndex: Int,
    val name: ByteArray,
)

/**
 * An unsigned Algorand transaction. Only the payment and application-call fields this app needs
 * are modelled. [encode] produces the canonical msgpack form that algod and every SDK agree on.
 */
sealed class Transaction {
    abstract val sender: Address
    abstract val params: SuggestedParams
    abstract var group: ByteArray?

    /** Flat fee; if null, the suggested-params fee (min fee floor) is used. */
    abstract val flatFee: Long?

    protected abstract fun typeFields(): Map<String, Any>

    fun fee(): Long = flatFee ?: maxOf(params.minFee, params.fee * (encodeWithFee(0).size + SIGNATURE_OVERHEAD))

    fun encode(): ByteArray = encodeWithFee(fee())

    private fun encodeWithFee(fee: Long): ByteArray {
        val fields = linkedMapOf<String, Any>()
        if (fee > 0) fields["fee"] = fee
        if (params.firstValid > 0) fields["fv"] = params.firstValid
        if (params.genesisId.isNotEmpty()) fields["gen"] = params.genesisId
        fields["gh"] = params.genesisHash
        group?.let { fields["grp"] = it }
        if (params.lastValid > 0) fields["lv"] = params.lastValid
        fields["snd"] = sender.publicKey
        fields.putAll(typeFields())
        return MsgPackWriter.encode(fields)
    }

    /** Raw 32-byte transaction ID (SHA-512/256 over "TX" + canonical bytes). */
    fun rawTxId(): ByteArray = sha512t256("TX".encodeToByteArray(), encode())

    fun txId(): String = Base32.encode(rawTxId())

    companion object {
        private const val SIGNATURE_OVERHEAD = 75

        /** Computes and assigns the atomic group ID for [txns]. */
        fun assignGroupId(txns: List<Transaction>) {
            require(txns.size in 1..16) { "A group must contain 1 to 16 transactions" }
            txns.forEach { it.group = null }
            val txList = MsgPackWriter.encode(mapOf("txlist" to txns.map { it.rawTxId() }))
            val gid = sha512t256("TG".encodeToByteArray(), txList)
            txns.forEach { it.group = gid }
        }
    }
}

class PaymentTransaction(
    override val sender: Address,
    override val params: SuggestedParams,
    val receiver: Address,
    val amount: Long,
    override val flatFee: Long? = null,
    override var group: ByteArray? = null,
) : Transaction() {
    override fun typeFields(): Map<String, Any> =
        buildMap {
            if (amount > 0) put("amt", amount)
            put("rcv", receiver.publicKey)
            put("type", "pay")
        }
}

class ApplicationCallTransaction(
    override val sender: Address,
    override val params: SuggestedParams,
    val appId: Long,
    val onComplete: OnComplete = OnComplete.NoOp,
    val appArgs: List<ByteArray> = emptyList(),
    val accounts: List<Address> = emptyList(),
    val foreignApps: List<Long> = emptyList(),
    val boxes: List<BoxReference> = emptyList(),
    override val flatFee: Long? = null,
    override var group: ByteArray? = null,
) : Transaction() {
    override fun typeFields(): Map<String, Any> =
        buildMap {
            if (appArgs.isNotEmpty()) put("apaa", appArgs)
            if (onComplete.code != 0) put("apan", onComplete.code)
            if (accounts.isNotEmpty()) put("apat", accounts.map { it.publicKey })
            if (boxes.isNotEmpty()) {
                put(
                    "apbx",
                    boxes.map { box ->
                        buildMap<String, Any> {
                            if (box.appIndex != 0) put("i", box.appIndex)
                            if (box.name.isNotEmpty()) put("n", box.name)
                        }
                    },
                )
            }
            if (foreignApps.isNotEmpty()) put("apfa", foreignApps)
            if (appId != 0L) put("apid", appId)
            put("type", "appl")
        }
}
