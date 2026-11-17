package io.github.algorandecosystem.example.coinflipper.utils

fun truncatedAlgorandAddress(str: String): String {
    val maxLength = 35
    return if (str.endsWith(".algo")) {
        str
    } else if (str.length > maxLength) {
        "${str.substring(0, 5)} ... ${str.substring(str.length - 6)}"
    } else {
        "empty"
    }
}

/** Box name for a validator entry in the ValidatorRegistry: "v" + uint64 big-endian id. */
fun getValidatorListBoxName(id: Long): ByteArray = byteArrayOf('v'.code.toByte()) + ByteArray(8) { i -> (id ushr (8 * (7 - i))).toByte() }
