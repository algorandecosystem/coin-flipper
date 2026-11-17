package io.github.algorandecosystem.example.coinflipper.algorand

import org.kotlincrypto.hash.sha2.SHA512t

/** SHA-512/256, the hash Algorand uses for addresses, transaction IDs, group IDs and ABI selectors. */
internal fun sha512t256(vararg parts: ByteArray): ByteArray {
    val digest = SHA512t(256)
    parts.forEach { digest.update(it) }
    return digest.digest()
}

internal fun Long.toBigEndianBytes(): ByteArray = ByteArray(8) { i -> (this ushr (8 * (7 - i))).toByte() }

internal fun ByteArray.readBigEndian(
    offset: Int,
    length: Int,
): Long {
    var value = 0L
    for (i in offset until offset + length) {
        value = (value shl 8) or (this[i].toLong() and 0xff)
    }
    return value
}

/** RFC 4648 base32 without padding, as used by Algorand addresses and transaction IDs. */
internal object Base32 {
    private const val ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"

    fun encode(data: ByteArray): String {
        val out = StringBuilder((data.size * 8 + 4) / 5)
        var buffer = 0
        var bits = 0
        for (b in data) {
            buffer = (buffer shl 8) or (b.toInt() and 0xff)
            bits += 8
            while (bits >= 5) {
                out.append(ALPHABET[(buffer shr (bits - 5)) and 31])
                bits -= 5
            }
        }
        if (bits > 0) out.append(ALPHABET[(buffer shl (5 - bits)) and 31])
        return out.toString()
    }

    fun decode(text: String): ByteArray {
        val out = ArrayList<Byte>(text.length * 5 / 8)
        var buffer = 0
        var bits = 0
        for (c in text.trimEnd('=')) {
            val v = ALPHABET.indexOf(c)
            require(v >= 0) { "Invalid base32 character '$c'" }
            buffer = (buffer shl 5) or v
            bits += 5
            if (bits >= 8) {
                out.add((buffer shr (bits - 8)).toByte())
                bits -= 8
            }
        }
        return out.toByteArray()
    }
}

/** A 32-byte Algorand public key with its checksummed base32 string form. */
class Address(
    val publicKey: ByteArray,
) {
    init {
        require(publicKey.size == 32) { "Address public key must be 32 bytes" }
    }

    override fun toString(): String = Base32.encode(publicKey + checksum(publicKey))

    override fun equals(other: Any?): Boolean = other is Address && other.publicKey.contentEquals(publicKey)

    override fun hashCode(): Int = publicKey.contentHashCode()

    companion object {
        private fun checksum(publicKey: ByteArray) = sha512t256(publicKey).copyOfRange(28, 32)

        fun fromString(address: String): Address {
            val decoded = Base32.decode(address)
            require(address.length == 58 && decoded.size == 36) { "Invalid Algorand address length" }
            val publicKey = decoded.copyOfRange(0, 32)
            require(checksum(publicKey).contentEquals(decoded.copyOfRange(32, 36))) { "Invalid Algorand address checksum" }
            return Address(publicKey)
        }

        fun forApplication(appId: Long): Address = Address(sha512t256("appID".encodeToByteArray(), appId.toBigEndianBytes()))
    }
}

/**
 * Minimal MessagePack writer producing Algorand's canonical encoding: callers pass map entries
 * already filtered of empty values; keys are sorted here.
 */
internal class MsgPackWriter {
    private val out = ArrayList<Byte>()

    fun toByteArray(): ByteArray = out.toByteArray()

    private fun byte(b: Int) {
        out.add(b.toByte())
    }

    private fun bytes(b: ByteArray) {
        b.forEach { out.add(it) }
    }

    private fun bigEndian(
        value: Long,
        size: Int,
    ) {
        for (i in size - 1 downTo 0) byte(((value ushr (8 * i)) and 0xff).toInt())
    }

    fun write(value: Any?) {
        when (value) {
            is Int -> writeUInt(value.toLong())
            is Long -> writeUInt(value)
            is String -> writeString(value)
            is ByteArray -> writeBinary(value)
            is Raw -> bytes(value.bytes)
            is Map<*, *> -> writeMap(value)
            is List<*> -> writeArray(value)
            is Boolean -> byte(if (value) 0xc3 else 0xc2)
            else -> error("Unsupported msgpack value: $value")
        }
    }

    private fun writeUInt(value: Long) {
        require(value >= 0) { "Only unsigned integers are supported" }
        when {
            value < 128 -> byte(value.toInt())
            value < 0x100 -> byte(0xcc).also { bigEndian(value, 1) }
            value < 0x10000 -> byte(0xcd).also { bigEndian(value, 2) }
            value < 0x100000000L -> byte(0xce).also { bigEndian(value, 4) }
            else -> byte(0xcf).also { bigEndian(value, 8) }
        }
    }

    private fun writeString(value: String) {
        val utf8 = value.encodeToByteArray()
        val n = utf8.size
        when {
            n < 32 -> byte(0xa0 or n)
            n < 0x100 -> byte(0xd9).also { bigEndian(n.toLong(), 1) }
            n < 0x10000 -> byte(0xda).also { bigEndian(n.toLong(), 2) }
            else -> byte(0xdb).also { bigEndian(n.toLong(), 4) }
        }
        bytes(utf8)
    }

    private fun writeBinary(value: ByteArray) {
        val n = value.size
        when {
            n < 0x100 -> byte(0xc4).also { bigEndian(n.toLong(), 1) }
            n < 0x10000 -> byte(0xc5).also { bigEndian(n.toLong(), 2) }
            else -> byte(0xc6).also { bigEndian(n.toLong(), 4) }
        }
        bytes(value)
    }

    private fun writeArray(value: List<*>) {
        val n = value.size
        when {
            n < 16 -> byte(0x90 or n)
            n < 0x10000 -> byte(0xdc).also { bigEndian(n.toLong(), 2) }
            else -> byte(0xdd).also { bigEndian(n.toLong(), 4) }
        }
        value.forEach { write(it) }
    }

    private fun writeMap(value: Map<*, *>) {
        val entries = value.entries.sortedBy { it.key as String }
        val n = entries.size
        when {
            n < 16 -> byte(0x80 or n)
            n < 0x10000 -> byte(0xde).also { bigEndian(n.toLong(), 2) }
            else -> byte(0xdf).also { bigEndian(n.toLong(), 4) }
        }
        entries.forEach { (k, v) ->
            writeString(k as String)
            write(v)
        }
    }

    /** Bytes that are already msgpack-encoded (e.g. a signed transaction) and are embedded verbatim. */
    class Raw(
        val bytes: ByteArray,
    )

    companion object {
        fun encode(value: Any?): ByteArray = MsgPackWriter().apply { write(value) }.toByteArray()
    }
}
