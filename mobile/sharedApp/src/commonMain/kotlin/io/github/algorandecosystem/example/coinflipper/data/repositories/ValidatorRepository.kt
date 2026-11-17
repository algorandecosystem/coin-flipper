package io.github.algorandecosystem.example.coinflipper.data.repositories

import io.github.algorandecosystem.example.coinflipper.algorand.AbiContract
import io.github.algorandecosystem.example.coinflipper.algorand.AlgodClient
import io.github.algorandecosystem.example.coinflipper.algorand.AlgorandAccount
import io.github.algorandecosystem.example.coinflipper.algorand.BoxReference
import io.github.algorandecosystem.example.coinflipper.data.local.dao.ExampleDao
import io.github.algorandecosystem.example.coinflipper.data.local.entities.ValidatorEntity
import io.github.algorandecosystem.example.coinflipper.data.models.Valid
import io.github.algorandecosystem.example.coinflipper.data.models.ValidatorConfig
import io.github.algorandecosystem.example.coinflipper.data.network.NfdApi
import io.github.algorandecosystem.example.coinflipper.utils.Constants
import io.github.algorandecosystem.example.coinflipper.utils.getValidatorListBoxName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class ValidatorRepository(
    algod: AlgodClient,
    private val database: ExampleDao,
    private val nfdApi: NfdApi,
) : AlgorandRepository(algod) {
    companion object {
        private const val TAG: String = "ValidatorRepository"
    }

    private var contract: AbiContract? = null

    private suspend fun validatorRegistryContract(): AbiContract =
        contract ?: loadContract("ValidatorRegistry.arc4.json").also { contract = it }

    val coroutineScope = CoroutineScope(Dispatchers.Default)

    fun loadValidatorDataIntoDb() {
        val validatorList: List<ValidatorEntity> =
            listOf(
                ValidatorEntity(
                    id = 1,
                    name = "reti.algo",
                    algoStaked = 25000,
                    apy = 3.03,
                    avatar = null,
                    percentToValidator = 5,
                    epochRoundLength = 1296,
                    minEntryStake = 5,
                ),
                ValidatorEntity(
                    id = 2,
                    name = "steventest.algo",
                    algoStaked = 25000,
                    apy = 3.03,
                    avatar = null,
                    percentToValidator = 1,
                    epochRoundLength = 32000,
                    minEntryStake = 1000,
                ),
                ValidatorEntity(
                    id = 3,
                    name = "algoleaguestestnet.algo",
                    algoStaked = 25000,
                    apy = 3.03,
                    avatar = null,
                    percentToValidator = 5,
                    epochRoundLength = 10000,
                    minEntryStake = 10,
                ),
                ValidatorEntity(
                    id = 7,
                    name = "DTM6B4ZQ5HC7SMDG3INL672WWWPRIXZUSABXHMIMDXGUXU63PPD7S7KLBA",
                    algoStaked = null,
                    apy = null,
                    avatar = null,
                    percentToValidator = 1,
                    epochRoundLength = 31104,
                    minEntryStake = 1,
                ),
                ValidatorEntity(
                    id = 8,
                    name = "reti.algo",
                    algoStaked = 25000,
                    apy = 3.03,
                    avatar = null,
                    percentToValidator = 5,
                    epochRoundLength = 1296,
                    minEntryStake = 5,
                ),
                ValidatorEntity(
                    id = 9,
                    name = "steventest.algo",
                    algoStaked = 25000,
                    apy = 3.03,
                    avatar = null,
                    percentToValidator = 1,
                    epochRoundLength = 32000,
                    minEntryStake = 1000,
                ),
                ValidatorEntity(
                    id = 10,
                    name = "algoleaguestestnet.algo",
                    algoStaked = 25000,
                    apy = 3.03,
                    avatar = null,
                    percentToValidator = 5,
                    epochRoundLength = 10000,
                    minEntryStake = 10,
                ),
                ValidatorEntity(
                    id = 11,
                    name = "DTM6B4ZQ5HC7SMDG3INL672WWWPRIXZUSABXHMIMDXGUXU63PPD7S7KLBA",
                    algoStaked = null,
                    apy = null,
                    percentToValidator = 1,
                    epochRoundLength = 31104,
                    minEntryStake = 1,
                ),
                ValidatorEntity(
                    id = 12,
                    name = "reti.algo",
                    algoStaked = 25000,
                    apy = 3.03,
                    avatar = null,
                    percentToValidator = 5,
                    epochRoundLength = 1296,
                    minEntryStake = 5,
                ),
                ValidatorEntity(
                    id = 13,
                    name = "steventest.algo",
                    algoStaked = 25000,
                    apy = 3.03,
                    avatar = null,
                    percentToValidator = 1,
                    epochRoundLength = 32000,
                    minEntryStake = 1000,
                ),
                ValidatorEntity(
                    id = 14,
                    name = "algoleaguestestnet.algo",
                    algoStaked = 25000,
                    apy = 3.03,
                    avatar = null,
                    percentToValidator = 5,
                    epochRoundLength = 10000,
                    minEntryStake = 10,
                ),
                ValidatorEntity(
                    id = 15,
                    name = "DTM6B4ZQ5HC7SMDG3INL672WWWPRIXZUSABXHMIMDXGUXU63PPD7S7KLBA",
                    algoStaked = null,
                    apy = null,
                    avatar = null,
                    percentToValidator = 1,
                    epochRoundLength = 31104,
                    minEntryStake = 1,
                ),
            )

        coroutineScope.launch {
            validatorList.forEach {
                insertValidatorIntoDb(it)
            }
        }
    }

    fun insertValidatorIntoDb(validatorEntity: ValidatorEntity) =
        coroutineScope.launch {
            database.insertValidator(validatorEntity)
        }

    fun getAllValidatorsFromDb(): Flow<List<ValidatorEntity>> = database.getAllValidatorsAsFlow()

    fun getValidatorByIdFromDb(validatorId: Int): Flow<ValidatorEntity?> = database.getValidatorByIdAsFlow(validatorId)

    /** Read-only registry call, run through algod simulate so it costs no fee. */
    suspend fun getNumberOfValidators(account: AlgorandAccount): Long =
        simulateMethodCall(
            appId = Constants.RETI_APP_ID_TESTNET,
            sender = account.address,
            method = validatorRegistryContract().method("getNumValidators"),
        ) as? Long ?: 0L

    suspend fun fetchValidatorInfo(
        account: AlgorandAccount,
        validatorId: Long,
    ) {
        val returnValue =
            simulateMethodCall(
                appId = Constants.RETI_APP_ID_TESTNET,
                sender = account.address,
                method = validatorRegistryContract().method("getValidatorConfig"),
                methodArgs = listOf(validatorId),
                boxReferences =
                    listOf(
                        BoxReference(0, getValidatorListBoxName(validatorId)),
                        BoxReference(0, getValidatorListBoxName(0L)),
                    ),
            ) ?: return

        val validatorConfig = validatorConfigFromABIReturn(returnValue) ?: return
        try {
            var valid: Valid? = null
            val nfdRecord =
                if (validatorConfig.NFDForInfo != 0L) {
                    valid = nfdApi.isValid(validatorConfig.NFDForInfo)
                    nfdApi.fetchNfd(valid.name)
                } else {
                    null
                }

            val avatar =
                nfdRecord?.properties?.userDefined?.avatar
                    ?: if (valid?.name?.contains(".algo") == true) "https://app.nf.domains/img/nfd-image-placeholder.jpg" else null

            insertValidatorIntoDb(
                ValidatorEntity(
                    id = validatorId.toInt(),
                    name = valid?.name ?: validatorConfig.Owner,
                    algoStaked = 25000,
                    apy = 3.03,
                    avatar = avatar,
                    percentToValidator = validatorConfig.PercentToValidator.toInt(),
                    epochRoundLength = validatorConfig.EpochRoundLength.toInt(),
                    minEntryStake = validatorConfig.MinEntryStake.toInt(),
                    inactive = false,
                ),
            )
        } catch (e: Exception) {
            // ignore if network error
        }
    }
}

/** Maps the 18-field getValidatorConfig ABI tuple onto [ValidatorConfig]. */
fun validatorConfigFromABIReturn(returnVal: Any): ValidatorConfig? =
    try {
        val arr = returnVal as? List<*> ?: throw IllegalArgumentException("unknown value returned from abi: $returnVal")
        require(arr.size == 18) { "should be 18 elements returned in ValidatorConfig response" }
        ValidatorConfig(
            ID = arr[0] as Long,
            Owner = arr[1] as String,
            Manager = arr[2] as String,
            NFDForInfo = arr[3] as Long,
            EntryGatingType = arr[4] as Long,
            EntryGatingAddress = arr[5] as String,
            EntryGatingAssets = (arr[6] as List<*>).map { it as Long },
            GatingAssetMinBalance = arr[7] as Long,
            RewardTokenId = arr[8] as Long,
            RewardPerPayout = arr[9] as Long,
            EpochRoundLength = arr[10] as Long,
            PercentToValidator = arr[11] as Long,
            ValidatorCommissionAddress = arr[12] as String,
            MinEntryStake = arr[13] as Long,
            MaxAlgoPerPool = arr[14] as Long,
            PoolsPerNode = arr[15] as Long,
            SunsettingOn = arr[16] as Long,
            SunsettingTo = arr[17] as Long,
        )
    } catch (e: Exception) {
        null
    }
