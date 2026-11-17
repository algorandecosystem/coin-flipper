package io.github.algorandecosystem.example.coinflipper.data.repositories

import io.github.algorandecosystem.example.coinflipper.algorand.AbiContract
import io.github.algorandecosystem.example.coinflipper.algorand.AbiMethod
import io.github.algorandecosystem.example.coinflipper.algorand.AccountInformation
import io.github.algorandecosystem.example.coinflipper.algorand.Address
import io.github.algorandecosystem.example.coinflipper.algorand.AlgodClient
import io.github.algorandecosystem.example.coinflipper.algorand.AlgorandAccount
import io.github.algorandecosystem.example.coinflipper.algorand.ApplicationCallTransaction
import io.github.algorandecosystem.example.coinflipper.algorand.BoxReference
import io.github.algorandecosystem.example.coinflipper.algorand.MethodCallBuilder
import io.github.algorandecosystem.example.coinflipper.algorand.OnComplete
import io.github.algorandecosystem.example.coinflipper.algorand.PaymentTransaction
import io.github.algorandecosystem.example.coinflipper.algorand.PendingTransactionResponse
import io.github.algorandecosystem.example.coinflipper.algorand.Transaction
import io.github.algorandecosystem.example.coinflipper.algorand.TransactionArgument
import io.github.algorandecosystem.example.coinflipper.domain.repositories.AccountRepository
import io.github.algorandecosystem.example.coinflipper.generated.resources.Res
import io.github.algorandecosystem.example.coinflipper.utils.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext

data class MethodCallResult(
    val txId: String,
    val confirmedRound: Long,
    val returnValue: Any?,
)

open class AlgorandRepository(
    protected val algod: AlgodClient,
) : AccountRepository {
    companion object {
        private const val TAG: String = "AlgorandRepository"

        /** Loads an ARC-4 contract description bundled under composeResources/files. */
        suspend fun loadContract(fileName: String): AbiContract = AbiContract.fromJson(Res.readBytes("files/$fileName").decodeToString())
    }

    override suspend fun getAccountInfo(account: AlgorandAccount): AccountInformation? =
        withContext(Dispatchers.IO) {
            try {
                algod.accountInformation(account.address.toString()).also {
                    Log.d(TAG, "Account Balance: ${it.amount} microAlgos")
                }
            } catch (e: Exception) {
                Log.e(TAG, e.toString())
                null
            }
        }

    private suspend fun sendSingleAppCall(
        account: AlgorandAccount,
        appId: Long,
        onComplete: OnComplete,
    ): PendingTransactionResponse? =
        withContext(Dispatchers.IO) {
            try {
                val txn =
                    ApplicationCallTransaction(
                        sender = account.address,
                        params = account.adjust(algod.suggestedParams()),
                        appId = appId,
                        onComplete = onComplete,
                    )
                val txId = algod.sendRawTransactions(listOf(account.sign(txn)))
                Log.d(TAG, "Transaction $txId")
                algod.waitForConfirmation(txId).also {
                    Log.d(TAG, "$onComplete app-id $appId confirmed in round ${it.confirmedRound}")
                }
            } catch (e: Exception) {
                Log.e(TAG, e.toString())
                null
            }
        }

    suspend fun appOptIn(
        account: AlgorandAccount,
        appId: Long,
    ): PendingTransactionResponse? = sendSingleAppCall(account, appId, OnComplete.OptIn)

    suspend fun closeOutApp(
        account: AlgorandAccount,
        appId: Long,
    ): PendingTransactionResponse? = sendSingleAppCall(account, appId, OnComplete.CloseOut)

    override suspend fun getCurrentRound(): Long =
        withContext(Dispatchers.IO) {
            try {
                algod.lastRound()
            } catch (e: Exception) {
                0L
            }
        }

    /** Creates a payment from [account] to the application's address, to be passed as a `pay` ABI argument. */
    suspend fun createPaymentArgument(
        account: AlgorandAccount,
        appId: Long,
        amount: Long,
    ): TransactionArgument =
        TransactionArgument(
            PaymentTransaction(
                sender = account.address,
                params = account.adjust(algod.suggestedParams()),
                receiver = Address.forApplication(appId),
                amount = amount,
            ),
        )

    /** Signs, submits and confirms an ARC-4 method call, returning the decoded return value. */
    suspend fun methodCallTransaction(
        appId: Long,
        account: AlgorandAccount,
        method: AbiMethod,
        methodArgs: List<Any> = emptyList(),
        boxReferences: List<BoxReference> = emptyList(),
    ): MethodCallResult? =
        withContext(Dispatchers.IO) {
            try {
                val group =
                    MethodCallBuilder.build(
                        sender = account.address,
                        params = account.adjust(algod.suggestedParams()),
                        appId = appId,
                        method = method,
                        args = methodArgs,
                        boxes = boxReferences,
                    )
                if (group.size > 1) Transaction.assignGroupId(group)
                algod.sendRawTransactions(group.map { account.sign(it) })
                val callTxId = group.last().txId()
                val confirmed = algod.waitForConfirmation(callTxId)
                MethodCallResult(
                    txId = callTxId,
                    confirmedRound = confirmed.confirmedRound ?: 0L,
                    returnValue = MethodCallBuilder.decodeReturn(method, confirmed.decodedLogs()),
                )
            } catch (e: Exception) {
                Log.e(TAG, e.toString())
                null
            }
        }

    /** Runs a read-only ARC-4 method call through algod's simulate endpoint (no signature, no fee). */
    suspend fun simulateMethodCall(
        appId: Long,
        sender: Address,
        method: AbiMethod,
        methodArgs: List<Any> = emptyList(),
        boxReferences: List<BoxReference> = emptyList(),
    ): Any? =
        withContext(Dispatchers.IO) {
            try {
                val group =
                    MethodCallBuilder.build(
                        sender = sender,
                        params = algod.suggestedParams(),
                        appId = appId,
                        method = method,
                        args = methodArgs,
                        boxes = boxReferences,
                    )
                if (group.size > 1) Transaction.assignGroupId(group)
                val results = algod.simulate(group)
                MethodCallBuilder.decodeReturn(method, results.last().decodedLogs())
            } catch (e: Exception) {
                Log.e(TAG, e.toString())
                null
            }
        }
}
