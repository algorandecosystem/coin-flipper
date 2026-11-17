package io.github.algorandecosystem.example.coinflipper.algorand

/**
 * An Algorand account the app can sign with. Accounts are created and recovered through the
 * wallet-sdk-ui onboarding flow, which keeps the keys encrypted in the SDK's database. This class
 * holds no key material: [sign] asks the SDK for the key each time and the key is wiped afterwards.
 */
class AlgorandAccount(
    val address: Address,
    val type: Type,
    private val signBytes: suspend (ByteArray) -> ByteArray?,
) {
    enum class Type(
        /** Lowest fee per transaction this signer needs (the SDK's Falcon25 floor is 0.003 ALGO). */
        val minFeeMicroAlgos: Long,
    ) {
        Falcon25(3000L),
        Algo25(1000L),
        HdKey(1000L),
    }

    /** Raises the fee floor in [params] to what this account's signature type needs. */
    fun adjust(params: SuggestedParams): SuggestedParams = params.copy(minFee = maxOf(params.minFee, type.minFeeMicroAlgos))

    /** Signs [txn] and returns the msgpack-encoded signed transaction. */
    suspend fun sign(txn: Transaction): ByteArray {
        val signed = signBytes(txn.encode())
        check(signed != null && signed.isNotEmpty()) { "Signing failed for transaction ${txn.txId()}" }
        return signed
    }
}
