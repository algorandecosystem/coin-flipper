package io.github.algorandecosystem.example.coinflipper.algorand

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * ARC-4 ABI types. Only static types are supported (uintN, bool, byte, address, T[N] and tuples of
 * those), which covers every method this app calls.
 */
sealed class AbiType {
    abstract val byteLength: Int

    data class UInt(
        val bits: Int,
    ) : AbiType() {
        override val byteLength get() = bits / 8
    }

    data object Bool : AbiType() {
        override val byteLength = 1
    }

    data object AddressType : AbiType() {
        override val byteLength = 32
    }

    data class StaticArray(
        val element: AbiType,
        val length: Int,
    ) : AbiType() {
        override val byteLength get() =
            if (element == Bool) (length + 7) / 8 else element.byteLength * length
    }

    data class Tuple(
        val elements: List<AbiType>,
    ) : AbiType() {
        override val byteLength: Int get() {
            var size = 0
            var i = 0
            while (i < elements.size) {
                if (elements[i] == Bool) {
                    var run = 0
                    while (i < elements.size && elements[i] == Bool) {
                        run++
                        i++
                    }
                    size += (run + 7) / 8
                } else {
                    size += elements[i].byteLength
                    i++
                }
            }
            return size
        }
    }

    fun encode(value: Any): ByteArray =
        when (this) {
            is UInt -> {
                val v = (value as Number).toLong()
                ByteArray(byteLength) { i -> if (byteLength - 1 - i < 8) (v ushr (8 * (byteLength - 1 - i))).toByte() else 0 }
            }
            Bool -> byteArrayOf(if (value as Boolean) 0x80.toByte() else 0)
            AddressType ->
                when (value) {
                    is Address -> value.publicKey
                    is String -> Address.fromString(value).publicKey
                    else -> error("Expected an address, got $value")
                }
            is StaticArray -> Tuple(List(length) { element }).encode(value)
            is Tuple -> encodeTuple(value as List<*>)
        }

    private fun Tuple.encodeTuple(values: List<*>): ByteArray {
        require(values.size == elements.size) { "Expected ${elements.size} tuple values, got ${values.size}" }
        val out = ArrayList<Byte>()
        var i = 0
        while (i < elements.size) {
            if (elements[i] == Bool) {
                val start = i
                while (i < elements.size && elements[i] == Bool) i++
                val packed = ByteArray((i - start + 7) / 8)
                for (j in start until i) {
                    if (values[j] as Boolean) {
                        val bit = j - start
                        packed[bit / 8] = (packed[bit / 8].toInt() or (0x80 ushr (bit % 8))).toByte()
                    }
                }
                packed.forEach { out.add(it) }
            } else {
                elements[i].encode(values[i]!!).forEach { out.add(it) }
                i++
            }
        }
        return out.toByteArray()
    }

    /** Decodes to Long (uint ≤ 64 bits), Boolean, String (address) or List<Any> (arrays/tuples). */
    fun decode(bytes: ByteArray): Any =
        when (this) {
            is UInt -> {
                require(bits <= 64) { "uint$bits values larger than 64 bits are not supported" }
                bytes.readBigEndian(0, byteLength)
            }
            Bool -> (bytes[0].toInt() and 0x80) != 0
            AddressType -> Address(bytes.copyOfRange(0, 32)).toString()
            is StaticArray -> Tuple(List(length) { element }).decode(bytes)
            is Tuple -> decodeTuple(bytes)
        }

    private fun Tuple.decodeTuple(bytes: ByteArray): List<Any> {
        val result = ArrayList<Any>(elements.size)
        var offset = 0
        var i = 0
        while (i < elements.size) {
            if (elements[i] == Bool) {
                val start = i
                while (i < elements.size && elements[i] == Bool) i++
                for (j in start until i) {
                    val bit = j - start
                    result.add((bytes[offset + bit / 8].toInt() and (0x80 ushr (bit % 8))) != 0)
                }
                offset += (i - start + 7) / 8
            } else {
                val size = elements[i].byteLength
                result.add(elements[i].decode(bytes.copyOfRange(offset, offset + size)))
                offset += size
                i++
            }
        }
        return result
    }

    companion object {
        fun parse(type: String): AbiType {
            if (type.startsWith("(") && type.endsWith(")")) {
                return Tuple(splitTuple(type.substring(1, type.length - 1)).map { parse(it) })
            }
            Regex("""^(.+)\[(\d+)]$""").matchEntire(type)?.let { m ->
                return StaticArray(parse(m.groupValues[1]), m.groupValues[2].toInt())
            }
            return when {
                type == "bool" -> Bool
                type == "address" -> AddressType
                type == "byte" -> UInt(8)
                type.startsWith("uint") -> UInt(type.removePrefix("uint").toInt())
                else -> error("Unsupported ABI type: $type")
            }
        }

        private fun splitTuple(inner: String): List<String> {
            if (inner.isEmpty()) return emptyList()
            val parts = mutableListOf<String>()
            var depth = 0
            var start = 0
            inner.forEachIndexed { idx, c ->
                when (c) {
                    '(' -> depth++
                    ')' -> depth--
                    ',' ->
                        if (depth == 0) {
                            parts += inner.substring(start, idx)
                            start = idx + 1
                        }
                }
            }
            parts += inner.substring(start)
            return parts
        }
    }
}

@Serializable
data class AbiArg(
    val type: String,
    val name: String? = null,
)

@Serializable
data class AbiReturns(
    val type: String,
)

@Serializable
data class AbiMethod(
    val name: String,
    val args: List<AbiArg>,
    val returns: AbiReturns,
) {
    val signature: String get() = "$name(${args.joinToString(",") { it.type }})${returns.type}"

    val selector: ByteArray get() = sha512t256(signature.encodeToByteArray()).copyOfRange(0, 4)
}

@Serializable
data class AbiContract(
    val name: String,
    val methods: List<AbiMethod>,
) {
    fun method(name: String): AbiMethod = methods.single { it.name == name }

    companion object {
        private val json = Json { ignoreUnknownKeys = true }

        fun fromJson(text: String): AbiContract = json.decodeFromString(serializer(), text)
    }
}

/** A transaction passed as an ABI `pay`/`txn` argument; it is placed in the group before the call. */
class TransactionArgument(
    val transaction: Transaction,
)

/** Builds the transaction group for an ARC-4 method call, mirroring the Java SDK's MethodCallTransactionBuilder. */
object MethodCallBuilder {
    private val TRANSACTION_TYPES = setOf("txn", "pay", "keyreg", "acfg", "axfer", "afrz", "appl")

    fun build(
        sender: Address,
        params: SuggestedParams,
        appId: Long,
        method: AbiMethod,
        args: List<Any>,
        boxes: List<BoxReference> = emptyList(),
        flatFee: Long? = null,
    ): List<Transaction> {
        require(args.size == method.args.size) { "${method.name} expects ${method.args.size} args, got ${args.size}" }
        val group = mutableListOf<Transaction>()
        val appArgs = mutableListOf(method.selector)
        val accounts = mutableListOf<Address>()
        val foreignApps = mutableListOf<Long>()

        method.args.zip(args).forEach { (arg, value) ->
            when (arg.type) {
                in TRANSACTION_TYPES -> group += (value as TransactionArgument).transaction
                "account" -> {
                    val address = if (value is Address) value else Address.fromString(value as String)
                    val index =
                        if (address == sender) {
                            0
                        } else {
                            accounts.indexOf(address).takeIf { it >= 0 }?.plus(1)
                                ?: accounts.add(address).let { accounts.size }
                        }
                    appArgs += byteArrayOf(index.toByte())
                }
                "application" -> {
                    val id = (value as Number).toLong()
                    val index =
                        if (id == appId) {
                            0
                        } else {
                            foreignApps.indexOf(id).takeIf { it >= 0 }?.plus(1)
                                ?: foreignApps.add(id).let { foreignApps.size }
                        }
                    appArgs += byteArrayOf(index.toByte())
                }
                else -> appArgs += AbiType.parse(arg.type).encode(value)
            }
        }
        require(appArgs.size <= 16) { "Methods with more than 15 ABI args are not supported" }

        group +=
            ApplicationCallTransaction(
                sender = sender,
                params = params,
                appId = appId,
                appArgs = appArgs,
                accounts = accounts,
                foreignApps = foreignApps,
                boxes = boxes,
                flatFee = flatFee,
            )
        return group
    }

    private val RETURN_PREFIX = byteArrayOf(0x15, 0x1f, 0x7c, 0x75)

    /** Extracts and decodes the ARC-4 return value from the method call's last log entry. */
    fun decodeReturn(
        method: AbiMethod,
        logs: List<ByteArray>,
    ): Any? {
        if (method.returns.type == "void") return null
        val last = logs.lastOrNull() ?: error("Method ${method.name} produced no logs")
        require(last.size >= 4 && last.copyOfRange(0, 4).contentEquals(RETURN_PREFIX)) { "Missing ARC-4 return prefix" }
        return AbiType.parse(method.returns.type).decode(last.copyOfRange(4, last.size))
    }
}
