package io.github.algorandecosystem.example.coinflipper.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.michaeltchuang.walletsdk.core.foundation.EventDelegate
import com.michaeltchuang.walletsdk.core.foundation.EventViewModel
import com.michaeltchuang.walletsdk.core.foundation.StateDelegate
import com.michaeltchuang.walletsdk.core.foundation.StateViewModel
import io.github.algorandecosystem.example.coinflipper.algorand.AlgorandAccount
import io.github.algorandecosystem.example.coinflipper.domain.session.AccountSession
import io.github.algorandecosystem.example.coinflipper.domain.usecases.LockWalletUseCase
import io.github.algorandecosystem.example.coinflipper.domain.usecases.RefreshAccountUseCase
import io.github.algorandecosystem.example.coinflipper.domain.usecases.SignOutUseCase
import io.github.algorandecosystem.example.coinflipper.utils.Log
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/** Account tab: shows the address and balance, opens the wallet SDK account sheet, locks or signs out. */
class AccountViewModel(
    private val session: AccountSession,
    private val refreshAccount: RefreshAccountUseCase,
    private val lockWallet: LockWalletUseCase,
    private val signOut: SignOutUseCase,
    private val stateDelegate: StateDelegate<ViewState>,
    private val eventDelegate: EventDelegate<ViewEvent>,
) : ViewModel(),
    StateViewModel<AccountViewModel.ViewState> by stateDelegate,
    EventViewModel<AccountViewModel.ViewEvent> by eventDelegate {
    sealed interface ViewState {
        data object NoAccount : ViewState

        data class Content(
            val address: String,
            val accountType: AlgorandAccount.Type,
            /** Null until the first balance has loaded. */
            val balanceMicroAlgos: Long?,
            val isRefreshing: Boolean = false,
            val isSignOutConfirmationVisible: Boolean = false,
        ) : ViewState
    }

    sealed interface ViewEvent {
        data class OpenAccountDetails(
            val address: String,
        ) : ViewEvent
    }

    init {
        stateDelegate.setDefaultState(ViewState.NoAccount)
        viewModelScope.launch {
            combine(session.account, session.accountInfo) { account, info -> account to info }
                .collect { (account, info) ->
                    stateDelegate.updateState { current ->
                        if (account == null) {
                            ViewState.NoAccount
                        } else {
                            ViewState.Content(
                                address = account.address.toString(),
                                accountType = account.type,
                                balanceMicroAlgos = info?.amount,
                                isRefreshing = (current as? ViewState.Content)?.isRefreshing ?: false,
                                isSignOutConfirmationVisible =
                                    (current as? ViewState.Content)?.isSignOutConfirmationVisible ?: false,
                            )
                        }
                    }
                }
        }
    }

    fun onRefresh() {
        viewModelScope.launch {
            setRefreshing(true)
            try {
                refreshAccount()
            } finally {
                setRefreshing(false)
            }
        }
    }

    fun onAccountDetailsClick() {
        stateDelegate.onState<ViewState.Content> {
            eventDelegate.sendEvent(viewModelScope, ViewEvent.OpenAccountDetails(it.address))
        }
    }

    /** The SDK sheet can change the balance (dispenser, send ALGO), so refresh when it closes. */
    fun onAccountDetailsClosed() = onRefresh()

    /** Signs out but keeps the account stored; the login screen offers to unlock it again. */
    fun onLockWallet() {
        viewModelScope.launch { lockWallet() }
    }

    fun onSignOutClick() = setSignOutConfirmationVisible(true)

    fun onSignOutDismiss() = setSignOutConfirmationVisible(false)

    /** Removes the account from the device; it can only come back through Restore. */
    fun onSignOutConfirm() {
        setSignOutConfirmationVisible(false)
        viewModelScope.launch {
            try {
                signOut()
            } catch (e: Exception) {
                Log.e(TAG, "Could not remove account: $e")
            }
        }
    }

    /** Removed from the wallet SDK's account sheet; stay on login until the user picks an account. */
    fun onAccountDeleted() {
        viewModelScope.launch { lockWallet() }
    }

    private fun setSignOutConfirmationVisible(visible: Boolean) {
        stateDelegate.updateState { (it as? ViewState.Content)?.copy(isSignOutConfirmationVisible = visible) ?: it }
    }

    private fun setRefreshing(refreshing: Boolean) {
        stateDelegate.updateState { (it as? ViewState.Content)?.copy(isRefreshing = refreshing) ?: it }
    }

    private companion object {
        const val TAG = "AccountViewModel"
    }
}
