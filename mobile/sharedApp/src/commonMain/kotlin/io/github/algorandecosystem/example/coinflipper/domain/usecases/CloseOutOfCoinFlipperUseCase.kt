package io.github.algorandecosystem.example.coinflipper.domain.usecases

import io.github.algorandecosystem.example.coinflipper.domain.repositories.CoinFlipperRepository
import io.github.algorandecosystem.example.coinflipper.domain.session.AccountSession

/** Closes the signed-in account out of the coin flipper app. Returns true once confirmed. */
class CloseOutOfCoinFlipperUseCase(
    private val session: AccountSession,
    private val repository: CoinFlipperRepository,
) {
    suspend operator fun invoke(): Boolean {
        val account = session.account.value ?: return false
        return repository.closeOut(account)
    }
}
