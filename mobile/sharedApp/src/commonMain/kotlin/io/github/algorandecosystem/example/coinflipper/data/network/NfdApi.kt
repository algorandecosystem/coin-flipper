package io.github.algorandecosystem.example.coinflipper.data.network

import io.github.algorandecosystem.example.coinflipper.data.model.NfdRecord
import io.github.algorandecosystem.example.coinflipper.data.models.Valid
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import org.koin.core.component.KoinComponent

class NfdApi(
    private val client: HttpClient,
    private val baseUrl: String = "https://api.testnet.nf.domains",
) : KoinComponent {
    suspend fun isValid(appId: Long) = client.get("$baseUrl/nfd/isValid/$appId").body<Valid>()

    suspend fun fetchNfd(nfd: String) = client.get("$baseUrl/nfd/$nfd").body<NfdRecord>()
}
