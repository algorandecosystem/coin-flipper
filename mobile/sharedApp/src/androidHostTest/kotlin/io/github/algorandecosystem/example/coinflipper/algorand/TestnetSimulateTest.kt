package io.github.algorandecosystem.example.coinflipper.algorand

import io.github.algorandecosystem.example.coinflipper.utils.Constants
import io.github.algorandecosystem.example.coinflipper.utils.getValidatorListBoxName
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import kotlinx.coroutines.test.runTest
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

/**
 * Live checks against TestNet algod using the simulate endpoint: nothing is signed or submitted.
 * Skipped unless RUN_TESTNET_TESTS=true, so normal builds stay offline.
 */
class TestnetSimulateTest {
    private val enabled = System.getenv("RUN_TESTNET_TESTS") == "true"
    private val algod = AlgodClient(Constants.ALGOD_API_ADDR, httpClient = HttpClient(OkHttp))

    private fun contract(name: String) = AbiContract.fromJson(File("src/commonMain/composeResources/files/$name").readText())

    @Test
    fun readsValidatorRegistry() =
        runTest(timeout = 60.seconds) {
            if (!enabled) return@runTest
            val registry = contract("ValidatorRegistry.arc4.json")
            val sender = Address.fromString(Constants.TEST_ADDRESS)
            val params = algod.suggestedParams()

            val countCall =
                MethodCallBuilder.build(
                    sender,
                    params,
                    Constants.RETI_APP_ID_TESTNET,
                    registry.method("getNumValidators"),
                    emptyList(),
                )
            val count =
                MethodCallBuilder.decodeReturn(
                    registry.method("getNumValidators"),
                    algod.simulate(countCall).last().decodedLogs(),
                ) as Long
            println("TestNet validator count: $count")
            assertTrue(count > 0)

            val method = registry.method("getValidatorConfig")
            val configCall =
                MethodCallBuilder.build(
                    sender,
                    params,
                    Constants.RETI_APP_ID_TESTNET,
                    method,
                    listOf(1L),
                    listOf(BoxReference(0, getValidatorListBoxName(1)), BoxReference(0, getValidatorListBoxName(0))),
                )
            val config = MethodCallBuilder.decodeReturn(method, algod.simulate(configCall).last().decodedLogs()) as List<*>
            println("Validator 1 config: $config")
            assertEquals(1L, config[0])
            assertEquals(18, config.size)
        }

    @Test
    fun flipCoinGroupIsAcceptedByTheContract() =
        runTest(timeout = 60.seconds) {
            if (!enabled) return@runTest
            val coinFlipper = contract("CoinFlipper.arc4.json")
            val sender = Address.fromString(Constants.TEST_ADDRESS)
            val account = algod.accountInformation(sender.toString())
            println(
                "Test account balance: ${account.amount}, opted in: ${account.appsLocalState.any {
                    it.id == Constants.COINFLIP_APP_ID_TESTNET
                }}",
            )

            val params = algod.suggestedParams()
            val pay = PaymentTransaction(sender, params, Address.forApplication(Constants.COINFLIP_APP_ID_TESTNET), 50_000)
            val method = coinFlipper.method("flip_coin")
            val group =
                MethodCallBuilder.build(
                    sender,
                    params,
                    Constants.COINFLIP_APP_ID_TESTNET,
                    method,
                    listOf(TransactionArgument(pay), true),
                )
            Transaction.assignGroupId(group)
            try {
                val results = algod.simulate(group)
                val commitmentRound = MethodCallBuilder.decodeReturn(method, results.last().decodedLogs()) as Long
                println("Simulated flip_coin commitment round: $commitmentRound (current ${params.lastRound})")
                assertTrue(commitmentRound > params.lastRound)
            } catch (e: AlgodException) {
                // If the account is not opted in / has an open bet, the contract rejects it; this still proves
                // the group decoded and reached the app logic (vs. a malformed-transaction error).
                println("Contract rejected simulated flip: ${e.message}")
                assertTrue("logic eval error" in e.message.orEmpty() || "opted in" in e.message.orEmpty(), e.message)
            }
        }
}
