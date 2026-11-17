package io.github.algorandecosystem.example.coinflipper.domain.usecases

import io.github.algorandecosystem.example.coinflipper.algorand.AccountInformation
import io.github.algorandecosystem.example.coinflipper.domain.repositories.AccountRepository
import io.github.algorandecosystem.example.coinflipper.domain.session.AccountSession

/** Re-reads the signed-in account from algod and publishes it to the [AccountSession]. */
class RefreshAccountUseCase(
    private val session: AccountSession,
    private val repository: AccountRepository,
) {
    suspend operator fun invoke(): AccountInformation? {
        val account = session.account.value ?: return null
        return repository.getAccountInfo(account)?.also { session.updateAccountInfo(it) }
    }
}
