package io.github.algorandecosystem.example.coinflipper.domain.usecases

import io.github.algorandecosystem.example.coinflipper.domain.repositories.CoinFlipperRepository
import io.github.algorandecosystem.example.coinflipper.domain.session.AccountSession
import io.github.algorandecosystem.example.coinflipper.utils.Constants

/** Places a bet with the signed-in account and returns its commitment round, or null on failure. */
class PlaceBetUseCase(
    private val session: AccountSession,
    private val repository: CoinFlipperRepository,
) {
    suspend operator fun invoke(
        isHeads: Boolean,
        amountMicroAlgos: Long = Constants.DEFAULT_MICRO_ALGO_BET_AMOUNT.toLong(),
    ): Long? {
        val account = session.account.value ?: return null
        return repository.placeBet(account, amountMicroAlgos, isHeads)
    }
}
