package io.github.algorandecosystem.example.coinflipper.di

import io.github.algorandecosystem.example.coinflipper.algorand.AlgodClient
import io.github.algorandecosystem.example.coinflipper.data.network.NfdApi
import io.github.algorandecosystem.example.coinflipper.utils.Constants
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.core.qualifier.named
import org.koin.dsl.module

/** Qualifier for this app's HttpClient, so it doesn't override the wallet SDK's own HttpClient in Koin. */
val APP_HTTP_CLIENT = named("coinFlipperHttpClient")

val provideHttpClientModules =
    module {
        single(APP_HTTP_CLIENT) {
            HttpClient {
                install(ContentNegotiation) {
                    json(json = Json { ignoreUnknownKeys = true }, contentType = ContentType.Any)
                }
            }
        }
        single { NfdApi(get(APP_HTTP_CLIENT)) }
        single {
            AlgodClient(
                baseUrl = Constants.ALGOD_API_ADDR,
                apiToken = Constants.ALGOD_API_TOKEN,
                apiTokenHeader = Constants.ALGOD_API_TOKEN_KEY,
            )
        }
    }
