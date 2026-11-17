package io.github.algorandecosystem.example.coinflipper.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import io.github.algorandecosystem.example.coinflipper.domain.repositories.ActiveAccountRepository
import io.github.algorandecosystem.example.coinflipper.domain.repositories.WalletLockRepository
import kotlinx.coroutines.flow.first
import okio.Path.Companion.toPath

fun createDataStore(producePath: () -> String): DataStore<Preferences> =
    PreferenceDataStoreFactory.createWithPath(
        corruptionHandler = null,
        migrations = emptyList(),
        produceFile = { producePath().toPath() },
    )

class AppSettings(
    private val dataStore: DataStore<Preferences>,
) : WalletLockRepository,
    ActiveAccountRepository {
    override suspend fun isLocked(): Boolean = dataStore.data.first()[WALLET_LOCKED] ?: false

    override suspend fun setLocked(locked: Boolean) {
        dataStore.edit { it[WALLET_LOCKED] = locked }
    }

    override suspend fun getActiveAddress(): String? = dataStore.data.first()[ACTIVE_ADDRESS]

    override suspend fun setActiveAddress(address: String?) {
        dataStore.edit {
            if (address == null) it.remove(ACTIVE_ADDRESS) else it[ACTIVE_ADDRESS] = address
        }
    }

    companion object {
        val EXAMPLE_APP_SETTING = stringPreferencesKey("example_app")
        private val WALLET_LOCKED = booleanPreferencesKey("wallet_locked")
        private val ACTIVE_ADDRESS = stringPreferencesKey("active_address")
    }
}
