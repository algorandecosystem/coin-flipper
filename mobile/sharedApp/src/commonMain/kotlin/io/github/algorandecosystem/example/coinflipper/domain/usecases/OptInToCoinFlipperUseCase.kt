package io.github.algorandecosystem.example.coinflipper.domain.usecases

import io.github.algorandecosystem.example.coinflipper.domain.repositories.CoinFlipperRepository
import io.github.algorandecosystem.example.coinflipper.domain.session.AccountSession

/** Opts the signed-in account in to the coin flipper app. Returns true once confirmed. */
class OptInToCoinFlipperUseCase(
    private val session: AccountSession,
    private val repository: CoinFlipperRepository,
) {
    suspend operator fun invoke(): Boolean {
        val account = session.account.value ?: return false
        return repository.optIn(account)
    }
}
