package io.github.algorandecosystem.example.coinflipper.domain.session

import io.github.algorandecosystem.example.coinflipper.algorand.AccountInformation
import io.github.algorandecosystem.example.coinflipper.algorand.AlgorandAccount
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * The signed-in account and its latest on-chain state, shared by every screen. This is the single
 * source of truth: view models observe it and use cases update it.
 */
class AccountSession {
    private val _account = MutableStateFlow<AlgorandAccount?>(null)
    val account: StateFlow<AlgorandAccount?> = _account.asStateFlow()

    private val _accountInfo = MutableStateFlow<AccountInformation?>(null)
    val accountInfo: StateFlow<AccountInformation?> = _accountInfo.asStateFlow()

    fun signIn(account: AlgorandAccount) {
        _accountInfo.value = null
        _account.value = account
    }

    fun signOut() {
        _account.value = null
        _accountInfo.value = null
    }

    fun updateAccountInfo(info: AccountInformation) {
        // Ignore a response for an account that is no longer signed in.
        if (info.address == _account.value?.address?.toString()) _accountInfo.value = info
    }
}
