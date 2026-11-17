package io.github.algorandecosystem.example.coinflipper.data.models

/** Decoded ValidatorRegistry `getValidatorConfig` tuple. Addresses are base32 strings. */
data class ValidatorConfig(
    val ID: Long = 0,
    val Owner: String = "",
    val Manager: String = "",
    val NFDForInfo: Long = 0,
    val EntryGatingType: Long = 0,
    val EntryGatingAddress: String = "",
    val EntryGatingAssets: List<Long> = emptyList(),
    val GatingAssetMinBalance: Long = 0,
    val RewardTokenId: Long = 0,
    val RewardPerPayout: Long = 0,
    val EpochRoundLength: Long = 0,
    val PercentToValidator: Long = 0,
    val ValidatorCommissionAddress: String = "",
    val MinEntryStake: Long = 0,
    val MaxAlgoPerPool: Long = 0,
    val PoolsPerNode: Long = 0,
    val SunsettingOn: Long = 0,
    val SunsettingTo: Long = 0,
)
