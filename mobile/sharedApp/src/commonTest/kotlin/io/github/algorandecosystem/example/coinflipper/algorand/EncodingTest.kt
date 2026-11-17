package io.github.algorandecosystem.example.coinflipper.algorand

import io.github.algorandecosystem.example.coinflipper.utils.Constants
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * Runs on every target (JVM and iOS), so the native build of the encoder is checked too.
 * Expected values were produced by the Java algosdk (see AlgosdkParityTest for full parity checks).
 */
class EncodingTest {
    private fun ByteArray.hex() = joinToString("") { (it.toInt() and 0xff).toString(16).padStart(2, '0') }

    @Test
    fun applicationAddress() {
        assertEquals("I5EK4KAPTUKAHAZSG4OR3BIDWVVKBXDDU36HXB2EGUP7PU43XQUPJ3IFUU", Address.forApplication(680099143L).toString())
    }

    @Test
    fun addressRoundTripAndChecksum() {
        val address = Address.fromString(Constants.TEST_ADDRESS)
        assertEquals(Constants.TEST_ADDRESS, address.toString())
        val corrupted = Constants.TEST_ADDRESS.dropLast(1) + if (Constants.TEST_ADDRESS.last() == 'A') "B" else "A"
        assertFailsWith<IllegalArgumentException> { Address.fromString(corrupted) }
    }

    @Test
    fun methodSelectors() {
        val flip = AbiMethod("flip_coin", listOf(AbiArg("pay"), AbiArg("bool")), AbiReturns("uint64"))
        val settle = AbiMethod("settle", listOf(AbiArg("account"), AbiArg("application")), AbiReturns("(bool,uint64)"))
        assertEquals("7330ee8a", flip.selector.hex())
        assertEquals("982fa8e5", settle.selector.hex())
    }

    @Test
    fun abiRoundTrip() {
        val type = AbiType.parse("(bool,uint64,address,uint64[2],bool,bool)")
        val value = listOf(true, 42L, Constants.TEST_ADDRESS, listOf(1L, Long.MAX_VALUE), false, true)
        val encoded = type.encode(value)
        assertEquals(type.byteLength, encoded.size)
        assertEquals(value, type.decode(encoded))
    }

    @Test
    fun msgpackCanonicalOrdering() {
        // Keys are sorted, ints use the smallest encoding, byte arrays use bin.
        val bytes = MsgPackWriter.encode(mapOf("b" to 300, "a" to byteArrayOf(1, 2)))
        assertContentEquals(
            byteArrayOf(
                0x82.toByte(),
                0xa1.toByte(),
                'a'.code.toByte(),
                0xc4.toByte(),
                2,
                1,
                2,
                0xa1.toByte(),
                'b'.code.toByte(),
                0xcd.toByte(),
                0x01,
                0x2c,
            ),
            bytes,
        )
    }
}
