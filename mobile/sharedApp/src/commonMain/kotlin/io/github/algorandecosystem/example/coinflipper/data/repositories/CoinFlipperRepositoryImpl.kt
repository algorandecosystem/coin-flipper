package io.github.algorandecosystem.example.coinflipper.data.repositories

import io.github.algorandecosystem.example.coinflipper.algorand.AbiContract
import io.github.algorandecosystem.example.coinflipper.algorand.AlgodClient
import io.github.algorandecosystem.example.coinflipper.algorand.AlgorandAccount
import io.github.algorandecosystem.example.coinflipper.domain.model.BetResult
import io.github.algorandecosystem.example.coinflipper.domain.repositories.CoinFlipperRepository
import io.github.algorandecosystem.example.coinflipper.utils.Constants
import io.github.algorandecosystem.example.coinflipper.utils.Log

class CoinFlipperRepositoryImpl(
    algod: AlgodClient,
) : AlgorandRepository(algod),
    CoinFlipperRepository {
    private companion object {
        const val TAG = "CoinFlipperRepository"
        private const val APP_ID = Constants.COINFLIP_APP_ID_TESTNET
    }

    private var contract: AbiContract? = null

    private suspend fun coinFlipperContract(): AbiContract = contract ?: loadContract("CoinFlipper.arc4.json").also { contract = it }

    override suspend fun optIn(account: AlgorandAccount): Boolean = appOptIn(account, APP_ID)?.confirmedRound != null

    override suspend fun closeOut(account: AlgorandAccount): Boolean = closeOutApp(account, APP_ID)?.confirmedRound != null

    override suspend fun placeBet(
        account: AlgorandAccount,
        amountMicroAlgos: Long,
        isHeads: Boolean,
    ): Long? {
        val payment = createPaymentArgument(account, APP_ID, amountMicroAlgos)
        val result =
            methodCallTransaction(
                appId = APP_ID,
                account = account,
                method = coinFlipperContract().method("flip_coin"),
                methodArgs = listOf(payment, isHeads),
            )
        // flip_coin returns the commitment round.
        return result?.returnValue as? Long
    }

    override suspend fun settleBet(account: AlgorandAccount): BetResult? {
        val result =
            methodCallTransaction(
                appId = APP_ID,
                account = account,
                method = coinFlipperContract().method("settle"),
                methodArgs = listOf(account.address, Constants.RANDOM_BEACON_APPID),
            )
        val values = result?.returnValue as? List<*> ?: return null
        val won = values.getOrNull(0) as? Boolean ?: return null
        val amount = values.getOrNull(1) as? Long ?: return null
        return BetResult(won, amount).also { Log.d(TAG, "Settle ${result.txId}: $it") }
    }
}
