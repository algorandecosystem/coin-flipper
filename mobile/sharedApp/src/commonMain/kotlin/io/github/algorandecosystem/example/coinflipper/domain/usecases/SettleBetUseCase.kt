package io.github.algorandecosystem.example.coinflipper.domain.usecases

import io.github.algorandecosystem.example.coinflipper.domain.model.BetResult
import io.github.algorandecosystem.example.coinflipper.domain.repositories.CoinFlipperRepository
import io.github.algorandecosystem.example.coinflipper.domain.session.AccountSession

/** Settles the signed-in account's bet against the randomness beacon, or returns null on failure. */
class SettleBetUseCase(
    private val session: AccountSession,
    private val repository: CoinFlipperRepository,
) {
    suspend operator fun invoke(): BetResult? {
        val account = session.account.value ?: return null
        return repository.settleBet(account)
    }
}
