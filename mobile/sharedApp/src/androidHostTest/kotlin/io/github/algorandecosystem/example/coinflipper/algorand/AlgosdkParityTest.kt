package io.github.algorandecosystem.example.coinflipper.algorand

import com.algorand.algosdk.abi.ABIType
import com.algorand.algosdk.abi.Contract
import com.algorand.algosdk.account.Account
import com.algorand.algosdk.builder.transaction.MethodCallTransactionBuilder
import com.algorand.algosdk.builder.transaction.PaymentTransactionBuilder
import com.algorand.algosdk.transaction.AppBoxReference
import com.algorand.algosdk.transaction.AtomicTransactionComposer
import com.algorand.algosdk.transaction.TransactionWithSigner
import com.algorand.algosdk.util.Encoder
import com.algorand.algosdk.v2.client.model.TransactionParametersResponse
import io.github.algorandecosystem.example.coinflipper.utils.getValidatorListBoxName
import java.io.File
import java.math.BigInteger
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import com.algorand.algosdk.crypto.Address as SdkAddress
import com.algorand.algosdk.transaction.Transaction as SdkTransaction

/**
 * Cross-checks the shared (multiplatform) Algorand encoder against the official Java SDK:
 * every transaction must encode to identical bytes, IDs and group IDs.
 */
class AlgosdkParityTest {
    private val sdkAccount = Account() // random key; only the address matters for encoding
    private val sender = Address(sdkAccount.address.bytes)
    private val genesisHash = ByteArray(32) { it.toByte() }

    private val sdkParams =
        TransactionParametersResponse().apply {
            fee = 0
            minFee = 1000
            genesisId = "testnet-v1.0"
            this.genesisHash = this@AlgosdkParityTest.genesisHash
            lastRound = 50_123_456L
            consensusVersion = "future"
        }
    private val params =
        SuggestedParams(fee = 0, minFee = 1000, genesisId = "testnet-v1.0", genesisHash = genesisHash, lastRound = 50_123_456L)

    private fun contractJson(name: String) = File("src/commonMain/composeResources/files/$name").readText()

    private fun assertSameTxn(
        expected: SdkTransaction,
        actual: Transaction,
    ) {
        assertContentEquals(Encoder.encodeToMsgPack(expected), actual.encode(), "msgpack bytes differ")
        assertEquals(expected.txID(), actual.txId())
    }

    @Test
    fun addressesMatch() {
        assertEquals(sdkAccount.address.toString(), sender.toString())
        assertEquals(sdkAccount.address, SdkAddress(Address.fromString(sdkAccount.address.toString()).publicKey))
        assertEquals(SdkAddress.forApplication(680099143L).toString(), Address.forApplication(680099143L).toString())
    }

    @Test
    fun paymentMatches() {
        val receiver = SdkAddress.forApplication(680099143L)
        val expected =
            PaymentTransactionBuilder
                .Builder()
                .sender(sdkAccount.address)
                .receiver(receiver)
                .amount(50_000)
                .suggestedParams(sdkParams)
                .build()
        assertSameTxn(expected, PaymentTransaction(sender, params, Address(receiver.bytes), 50_000))
    }

    @Test
    fun optInAndCloseOutMatch() {
        val optIn =
            SdkTransaction
                .ApplicationOptInTransactionBuilder()
                .sender(sdkAccount.address)
                .suggestedParams(sdkParams)
                .applicationId(680099143L)
                .build()
        assertSameTxn(optIn, ApplicationCallTransaction(sender, params, 680099143L, OnComplete.OptIn))

        val close =
            SdkTransaction
                .ApplicationCloseTransactionBuilder()
                .sender(sdkAccount.address)
                .suggestedParams(sdkParams)
                .applicationId(680099143L)
                .build()
        assertSameTxn(close, ApplicationCallTransaction(sender, params, 680099143L, OnComplete.CloseOut))
    }

    private fun sdkMethodCall(
        contract: Contract,
        method: String,
        appId: Long,
        args: List<Any>,
        boxes: List<AppBoxReference>? = null,
    ): List<SdkTransaction> {
        val builder =
            MethodCallTransactionBuilder
                .Builder()
                .applicationId(appId)
                .sender(sdkAccount.address)
                .signer(sdkAccount.transactionSigner)
                .method(contract.getMethodByName(method))
                .methodArguments(args)
                .onComplete(SdkTransaction.OnCompletion.NoOpOC)
                .suggestedParams(sdkParams)
        boxes?.let { builder.boxReferences(it) }
        val atc = AtomicTransactionComposer()
        atc.addMethodCall(builder.build())
        return atc.buildGroup().map { it.txn }
    }

    @Test
    fun flipCoinGroupMatches() {
        val json = contractJson("CoinFlipper.arc4.json")
        val sdkPay =
            PaymentTransactionBuilder
                .Builder()
                .sender(sdkAccount.address)
                .receiver(SdkAddress.forApplication(680099143L))
                .amount(50_000)
                .suggestedParams(sdkParams)
                .build()
        val expected =
            sdkMethodCall(
                Encoder.decodeFromJson(json, Contract::class.java),
                "flip_coin",
                680099143L,
                listOf(TransactionWithSigner(sdkPay, sdkAccount.transactionSigner), true),
            )

        val pay = PaymentTransaction(sender, params, Address.forApplication(680099143L), 50_000)
        val actual =
            MethodCallBuilder.build(
                sender,
                params,
                680099143L,
                AbiContract.fromJson(json).method("flip_coin"),
                listOf(TransactionArgument(pay), true),
            )
        Transaction.assignGroupId(actual)

        assertEquals(2, actual.size)
        expected.zip(actual).forEach { (e, a) -> assertSameTxn(e, a) }
        assertContentEquals(expected[0].group.bytes, actual[0].group)
    }

    @Test
    fun settleWithReferenceArgsMatches() {
        val json = contractJson("CoinFlipper.arc4.json")
        val beacon = 600011887L
        val expected =
            sdkMethodCall(
                Encoder.decodeFromJson(json, Contract::class.java),
                "settle",
                680099143L,
                listOf(sdkAccount.address, BigInteger.valueOf(beacon)),
            )
        val actual =
            MethodCallBuilder.build(
                sender,
                params,
                680099143L,
                AbiContract.fromJson(json).method("settle"),
                listOf(sender, beacon),
            )
        assertSameTxn(expected.single(), actual.single())
    }

    @Test
    fun validatorConfigCallWithBoxesMatches() {
        val json = contractJson("ValidatorRegistry.arc4.json")
        val expected =
            sdkMethodCall(
                Encoder.decodeFromJson(json, Contract::class.java),
                "getValidatorConfig",
                673404372L,
                listOf(BigInteger.valueOf(7)),
                listOf(AppBoxReference(0, getValidatorListBoxName(7)), AppBoxReference(0, getValidatorListBoxName(0))),
            )
        val actual =
            MethodCallBuilder.build(
                sender,
                params,
                673404372L,
                AbiContract.fromJson(json).method("getValidatorConfig"),
                listOf(7L),
                listOf(BoxReference(0, getValidatorListBoxName(7)), BoxReference(0, getValidatorListBoxName(0))),
            )
        assertSameTxn(expected.single(), actual.single())
    }

    @Test
    fun abiTupleDecodingMatches() {
        val type =
            "(uint64,address,address,uint64,uint8,address,uint64[4],uint64,uint64,uint64,uint32,uint32,address,uint64,uint64,uint8,uint64,uint64)"
        val a1 = Account().address
        val a2 = Account().address
        val values =
            arrayOf<Any>(
                BigInteger.valueOf(7),
                a1,
                a2,
                BigInteger.valueOf(123456),
                BigInteger.valueOf(2),
                a1,
                arrayOf<Any>(BigInteger.ONE, BigInteger.TWO, BigInteger.ZERO, BigInteger.valueOf(Long.MAX_VALUE)),
                BigInteger.valueOf(10),
                BigInteger.valueOf(11),
                BigInteger.valueOf(12),
                BigInteger.valueOf(1296),
                BigInteger.valueOf(50000),
                a2,
                BigInteger.valueOf(1_000_000),
                BigInteger.valueOf(70_000_000_000_000),
                BigInteger.valueOf(3),
                BigInteger.ZERO,
                BigInteger.valueOf(99),
            )
        val encoded = ABIType.valueOf(type).encode(values)
        val decoded = AbiType.parse(type).decode(encoded) as List<*>

        assertEquals(7L, decoded[0])
        assertEquals(a1.toString(), decoded[1])
        assertEquals(listOf(1L, 2L, 0L, Long.MAX_VALUE), decoded[6])
        assertEquals(1296L, decoded[10])
        assertEquals(a2.toString(), decoded[12])
        assertEquals(99L, decoded[17])
        // And our encoder must round-trip to the same bytes.
        assertContentEquals(encoded, AbiType.parse(type).encode(decoded.map { it!! }))

        val boolTuple = "(bool,uint64)"
        val ret = ABIType.valueOf(boolTuple).encode(arrayOf<Any>(true, BigInteger.valueOf(100_000)))
        assertEquals(listOf(true, 100_000L), AbiType.parse(boolTuple).decode(ret))
    }
}
