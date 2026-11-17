package io.github.algorandecosystem.example.coinflipper.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.algorandecosystem.example.coinflipper.data.local.entities.ValidatorEntity
import io.github.algorandecosystem.example.coinflipper.data.repositories.ValidatorRepository
import io.github.algorandecosystem.example.coinflipper.utils.Log
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class ValidatorDetailUIState {
    object Loading : ValidatorDetailUIState()

    data class Error(
        val message: String,
    ) : ValidatorDetailUIState()

    data class Success(
        val result: ValidatorEntity,
    ) : ValidatorDetailUIState()
}

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
open class ValidatorDetailViewModel(
    private val validatorRepository: ValidatorRepository,
) : ViewModel() {
    var algorandBaseViewModel: AlgorandBaseViewModel? = null

    private val _validatorDetailUIState = MutableStateFlow<ValidatorDetailUIState>(ValidatorDetailUIState.Loading)
    val validatorDetailUIState = _validatorDetailUIState.asStateFlow()

    fun fetchValidatorById(validatorId: Int) {
        viewModelScope.launch {
            val validatorFromDb =
                validatorRepository
                    .getValidatorByIdFromDb(validatorId)
                    .collect { validator ->
                        Log.e(TAG, "validator result is $validator")
                        if (validator == null) {
                            _validatorDetailUIState.value = ValidatorDetailUIState.Error("Validator does not exist")
                        } else {
                            _validatorDetailUIState.value = ValidatorDetailUIState.Success(validator)
                        }
                    }
            // .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ValidatorDetailUIState.Loading)
        }
    }

    private companion object {
        const val TAG = "ValidatorDetailViewModel"
    }
}
