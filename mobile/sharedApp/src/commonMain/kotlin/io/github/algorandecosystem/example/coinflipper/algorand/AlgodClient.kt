package io.github.algorandecosystem.example.coinflipper.algorand

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.io.encoding.Base64

@Serializable
data class TransactionParamsResponse(
    val fee: Long,
    @SerialName("min-fee") val minFee: Long,
    @SerialName("genesis-id") val genesisId: String,
    @SerialName("genesis-hash") val genesisHash: String,
    @SerialName("last-round") val lastRound: Long,
)

@Serializable
data class TealValue(
    val type: Int,
    val uint: Long = 0,
    val bytes: String = "",
)

@Serializable
data class TealKeyValue(
    val key: String,
    val value: TealValue,
)

@Serializable
data class ApplicationLocalState(
    val id: Long,
    @SerialName("key-value") val keyValue: List<TealKeyValue> = emptyList(),
)

@Serializable
data class AccountInformation(
    val address: String,
    val amount: Long,
    @SerialName("min-balance") val minBalance: Long = 0,
    @SerialName("apps-local-state") val appsLocalState: List<ApplicationLocalState> = emptyList(),
)

@Serializable
data class PendingTransactionResponse(
    @SerialName("confirmed-round") val confirmedRound: Long? = null,
    @SerialName("pool-error") val poolError: String = "",
    val logs: List<String> = emptyList(),
) {
    fun decodedLogs(): List<ByteArray> = logs.map { Base64.decode(it) }
}

@Serializable
private data class PostTransactionsResponse(
    val txId: String,
)

@Serializable
private data class NodeStatus(
    @SerialName("last-round") val lastRound: Long,
)

@Serializable
private data class SimulateTxnResult(
    @SerialName("txn-result") val txnResult: PendingTransactionResponse,
)

@Serializable
private data class SimulateGroupResult(
    @SerialName("txn-results") val txnResults: List<SimulateTxnResult>,
    @SerialName("failure-message") val failureMessage: String? = null,
)

@Serializable
private data class SimulateResponse(
    @SerialName("txn-groups") val txnGroups: List<SimulateGroupResult>,
)

class AlgodException(
    message: String,
) : Exception(message)

/** Minimal algod v2 REST client covering what the app needs. */
class AlgodClient(
    private val baseUrl: String,
    private val apiToken: String = "",
    private val apiTokenHeader: String = "X-Algo-API-Token",
    httpClient: HttpClient = HttpClient(),
) {
    private val http =
        httpClient.config {
            expectSuccess = false
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true }, contentType = ContentType.Any)
            }
        }

    private suspend inline fun <reified T> HttpResponse.bodyOrThrow(): T {
        if (!status.isSuccess()) throw AlgodException("algod ${status.value}: ${bodyAsText()}")
        return body()
    }

    private fun io.ktor.client.request.HttpRequestBuilder.auth() {
        if (apiToken.isNotEmpty()) header(apiTokenHeader, apiToken)
    }

    suspend fun suggestedParams(): SuggestedParams =
        SuggestedParams.fromAlgod(http.get("$baseUrl/v2/transactions/params") { auth() }.bodyOrThrow())

    suspend fun accountInformation(address: String): AccountInformation = http.get("$baseUrl/v2/accounts/$address") { auth() }.bodyOrThrow()

    suspend fun lastRound(): Long = http.get("$baseUrl/v2/status") { auth() }.bodyOrThrow<NodeStatus>().lastRound

    /** Submits already-signed transactions (concatenated msgpack) and returns the first transaction ID. */
    suspend fun sendRawTransactions(signedTxns: List<ByteArray>): String =
        http
            .post("$baseUrl/v2/transactions") {
                auth()
                contentType(ContentType("application", "x-binary"))
                setBody(signedTxns.reduce { acc, bytes -> acc + bytes })
            }.bodyOrThrow<PostTransactionsResponse>()
            .txId

    suspend fun pendingTransaction(txId: String): PendingTransactionResponse =
        http.get("$baseUrl/v2/transactions/pending/$txId") { auth() }.bodyOrThrow()

    suspend fun waitForConfirmation(
        txId: String,
        maxRounds: Int = 10,
    ): PendingTransactionResponse {
        var round = lastRound()
        repeat(maxRounds) {
            val pending = pendingTransaction(txId)
            if ((pending.confirmedRound ?: 0) > 0) return pending
            if (pending.poolError.isNotEmpty()) throw AlgodException("Transaction $txId rejected: ${pending.poolError}")
            http.get("$baseUrl/v2/status/wait-for-block-after/$round") { auth() }.bodyOrThrow<NodeStatus>()
            round++
        }
        throw AlgodException("Transaction $txId not confirmed after $maxRounds rounds")
    }

    /**
     * Simulates an unsigned group (empty signatures allowed) and returns each transaction's result.
     * Used for read-only ABI calls so they cost no fees and never touch chain state.
     */
    suspend fun simulate(txns: List<Transaction>): List<PendingTransactionResponse> {
        val request =
            mapOf(
                "allow-empty-signatures" to true,
                "txn-groups" to listOf(mapOf("txns" to txns.map { mapOf("txn" to MsgPackWriter.Raw(it.encode())) })),
            )
        val response =
            http
                .post("$baseUrl/v2/transactions/simulate") {
                    auth()
                    contentType(ContentType("application", "msgpack"))
                    setBody(MsgPackWriter.encode(request))
                }.bodyOrThrow<SimulateResponse>()
        val group = response.txnGroups.single()
        group.failureMessage?.let { throw AlgodException("Simulation failed: $it") }
        return group.txnResults.map { it.txnResult }
    }
}
