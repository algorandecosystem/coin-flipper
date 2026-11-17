package io.github.algorandecosystem.example.coinflipper.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.algorandecosystem.example.coinflipper.algorand.AlgorandAccount
import io.github.algorandecosystem.example.coinflipper.domain.repositories.WalletAccountRepository
import io.github.algorandecosystem.example.coinflipper.domain.repositories.WalletLockRepository
import io.github.algorandecosystem.example.coinflipper.domain.session.AccountSession
import io.github.algorandecosystem.example.coinflipper.domain.usecases.CreateAccountUseCase
import io.github.algorandecosystem.example.coinflipper.domain.usecases.GetUnlockableAddressUseCase
import io.github.algorandecosystem.example.coinflipper.domain.usecases.UnlockWalletUseCase
import io.github.algorandecosystem.example.coinflipper.generated.resources.Res
import io.github.algorandecosystem.example.coinflipper.generated.resources.error_account_create
import io.github.algorandecosystem.example.coinflipper.utils.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString

/**
 * App-wide state shared across navigation: signing in through the wallet-sdk-ui onboarding sheet,
 * and the shared snackbar. Per-screen logic lives in the screens' own MVI view models.
 */
open class AlgorandBaseViewModel(
    private val session: AccountSession,
    private val walletAccounts: WalletAccountRepository,
    private val walletLock: WalletLockRepository,
    private val unlockWallet: UnlockWalletUseCase,
    private val getUnlockableAddress: GetUnlockableAddressUseCase,
    private val createAccount: CreateAccountUseCase,
) : ViewModel() {
    val accountStateFlow: StateFlow<AlgorandAccount?> = session.account
    val account: AlgorandAccount? get() = session.account.value

    private val _storedAccountAddress = MutableStateFlow<String?>(null)

    /** Address of an account already stored in the wallet SDK, shown on the login screen to unlock. */
    val storedAccountAddress = _storedAccountAddress.asStateFlow()

    private val _isWalletChecked = MutableStateFlow(false)

    /** False until the launch check for a stored account has finished. */
    val isWalletChecked = _isWalletChecked.asStateFlow()

    private val _isCreatingAccount = MutableStateFlow(false)

    /** True while a new account is being generated and stored; the Create button is disabled. */
    val isCreatingAccount = _isCreatingAccount.asStateFlow()

    private val _snackBarMessage = MutableStateFlow("")
    val snackBarMessage = _snackBarMessage.asStateFlow()

    /** Addresses that existed before the onboarding sheet opened, so a newly added account can be picked. */
    private var addressesBeforeOnboarding: Set<String> = emptySet()

    init {
        viewModelScope.launch {
            // Sign back in with the stored account, unless the user locked the wallet.
            if (walletLock.isLocked() || !unlock()) refreshStoredAccount()
            _isWalletChecked.value = true
        }
        // Keep the login screen's Unlock button current after locking or deleting an account.
        viewModelScope.launch {
            session.account.collect { if (it == null) refreshStoredAccount() }
        }
    }

    /** Login screen: sign in with the account already stored in the wallet SDK. */
    fun onUnlockWallet() {
        viewModelScope.launch { unlock() }
    }

    /** Login screen: create a new account directly, without the SDK's onboarding sheet. */
    fun onCreateAccount() {
        if (_isCreatingAccount.value) return
        _isCreatingAccount.value = true
        viewModelScope.launch {
            try {
                if (!createAccount()) setSnackBarMessage(Res.string.error_account_create)
            } catch (e: Exception) {
                Log.e(TAG, "Could not create account: $e")
                setSnackBarMessage(Res.string.error_account_create)
            } finally {
                _isCreatingAccount.value = false
            }
        }
    }

    /** Call when the wallet-sdk-ui onboarding sheet is opened. */
    fun onOnboardingStarted() {
        viewModelScope.launch {
            addressesBeforeOnboarding = walletAccounts.getSignableAddresses()
        }
    }

    /** Call when the onboarding sheet closes; signs in if an account was created or restored. */
    fun onOnboardingFinished() {
        viewModelScope.launch {
            // Nothing new was added (sheet dismissed, or an already-stored account restored): stay on login.
            if (walletAccounts.getSignableAddresses() == addressesBeforeOnboarding) {
                refreshStoredAccount()
                return@launch
            }
            unlock(exclude = addressesBeforeOnboarding)
        }
    }

    private suspend fun unlock(exclude: Set<String> = emptySet()): Boolean =
        try {
            unlockWallet(exclude)
        } catch (e: Exception) {
            Log.e(TAG, "Could not load wallet account: $e")
            false
        }

    private suspend fun refreshStoredAccount() {
        _storedAccountAddress.value =
            try {
                getUnlockableAddress()
            } catch (e: Exception) {
                null
            }
    }

    fun setSnackBarMessage(resource: StringResource) {
        viewModelScope.launch {
            _snackBarMessage.value = getString(resource = resource)
        }
    }

    fun setSnackBarMessage(str: String) {
        viewModelScope.launch {
            _snackBarMessage.value = str
        }
    }

    private companion object {
        const val TAG = "AlgorandBaseViewModel"
    }
}

sealed class ScreenState {
    data object Loading : ScreenState()

    data class Error(
        val errorMessage: String,
    ) : ScreenState()

    data class Success(
        val responseData: Any,
    ) : ScreenState()
}
