package io.github.algorandecosystem.example.coinflipper.data.repositories

import com.michaeltchuang.walletsdk.core.account.domain.model.core.AccountCreation
import com.michaeltchuang.walletsdk.core.account.domain.model.local.LocalAccount
import com.michaeltchuang.walletsdk.core.account.domain.usecase.core.NameRegistrationUseCase
import com.michaeltchuang.walletsdk.core.account.domain.usecase.local.GetAlgo25SecretKey
import com.michaeltchuang.walletsdk.core.account.domain.usecase.local.GetFalcon25Seed
import com.michaeltchuang.walletsdk.core.account.domain.usecase.local.GetHdSeed
import com.michaeltchuang.walletsdk.core.account.domain.usecase.local.GetLocalAccounts
import com.michaeltchuang.walletsdk.core.algosdk.createFalcon25Account
import com.michaeltchuang.walletsdk.core.algosdk.signAlgo25Transaction
import com.michaeltchuang.walletsdk.core.algosdk.signFalcon25Transaction
import com.michaeltchuang.walletsdk.core.algosdk.signHdKeyTransaction
import com.michaeltchuang.walletsdk.core.encryption.encryptByteArray
import com.michaeltchuang.walletsdk.core.encryption.initializeEncryptionManager
import com.michaeltchuang.walletsdk.core.foundation.utils.CreationType
import io.github.algorandecosystem.example.coinflipper.algorand.Address
import io.github.algorandecosystem.example.coinflipper.algorand.AlgorandAccount
import io.github.algorandecosystem.example.coinflipper.domain.repositories.WalletAccountRepository

/**
 * Creates accounts and reads the ones the wallet-sdk-ui onboarding flow recovered. Keys stay encrypted
 * in the SDK's database and are only decrypted for the duration of a single signature.
 */
class WalletAccountRepositoryImpl(
    private val getLocalAccounts: GetLocalAccounts,
    private val getFalcon25Seed: GetFalcon25Seed,
    private val getAlgo25SecretKey: GetAlgo25SecretKey,
    private val getHdSeed: GetHdSeed,
    private val nameRegistration: NameRegistrationUseCase,
) : WalletAccountRepository {
    /** Addresses of the accounts this app can sign with. */
    override suspend fun getSignableAddresses(): Set<String> = signableAccounts().map { it.address }.toSet()

    /**
     * Returns a signable account, preferring one whose address is not in [exclude] (so a freshly
     * created account wins over ones that already existed), then [preferredAddress], then Falcon25
     * over other types.
     */
    override suspend fun loadAccount(
        exclude: Set<String>,
        preferredAddress: String?,
    ): AlgorandAccount? {
        val accounts = signableAccounts()
        val candidates = accounts.filter { it.address !in exclude }.ifEmpty { accounts }
        val local =
            candidates.firstOrNull { it.address == preferredAddress }
                ?: candidates.lastOrNull { it is LocalAccount.Falcon25 }
                ?: candidates.lastOrNull()
                ?: return null
        return toAlgorandAccount(local)
    }

    override suspend fun createAccount(): String {
        initializeEncryptionManager()
        val falcon25 = createFalcon25Account() ?: error("Could not generate a Falcon25 key")
        try {
            nameRegistration.addNewAccount(
                AccountCreation(
                    address = falcon25.address,
                    customName = null,
                    isBackedUp = false,
                    type =
                        AccountCreation.Type.Falcon25(
                            publicKey = falcon25.publicKey,
                            encryptedPrivateKey = encryptByteArray(falcon25.privateKey),
                            encryptedEntropy = encryptByteArray(falcon25.entropy),
                            encryptedSeed = encryptByteArray(falcon25.seed),
                        ),
                    creationType = CreationType.CREATE,
                ),
            )
        } finally {
            falcon25.privateKey.fill(0)
            falcon25.entropy.fill(0)
            falcon25.seed.fill(0)
        }
        return falcon25.address
    }

    override suspend fun deleteAccount(address: String) = nameRegistration.deleteAccount(address)

    private fun toAlgorandAccount(local: LocalAccount): AlgorandAccount? {
        val address = Address.fromString(local.address)
        return when (local) {
            is LocalAccount.Falcon25 ->
                AlgorandAccount(address, AlgorandAccount.Type.Falcon25) { txn ->
                    getFalcon25Seed(local.address)?.useAndWipe { signFalcon25Transaction(txn, it) }
                }
            is LocalAccount.Algo25 ->
                AlgorandAccount(address, AlgorandAccount.Type.Algo25) { txn ->
                    getAlgo25SecretKey(local.address)?.useAndWipe { signAlgo25Transaction(it, txn) }
                }
            is LocalAccount.HdKey ->
                AlgorandAccount(address, AlgorandAccount.Type.HdKey) { txn ->
                    getHdSeed(local.seedId)?.useAndWipe {
                        signHdKeyTransaction(txn, it, local.account, local.change, local.keyIndex)
                    }
                }
            else -> null
        }
    }

    private suspend fun signableAccounts(): List<LocalAccount> =
        getLocalAccounts().filter {
            it is LocalAccount.Falcon25 || it is LocalAccount.Algo25 || it is LocalAccount.HdKey
        }

    private inline fun <T> ByteArray.useAndWipe(block: (ByteArray) -> T): T =
        try {
            block(this)
        } finally {
            fill(0)
        }
}
