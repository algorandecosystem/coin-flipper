package io.github.algorandecosystem.example.coinflipper.domain.usecases

import io.github.algorandecosystem.example.coinflipper.domain.repositories.AccountRepository

/** Returns the latest round algod has seen, or 0 if it can't be reached. */
class GetCurrentRoundUseCase(
    private val repository: AccountRepository,
) {
    suspend operator fun invoke(): Long = repository.getCurrentRound()
}
