package com.cuboidestudio.orionvault.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

actual fun createHttpClient(json: Json): HttpClient = HttpClient(OkHttp) {
    install(ContentNegotiation) { json(json) }
    // LogLevel.NONE por padrão: corpos de requisição carregam blobs cifrados e tokens de
    // sessão; nunca logar isso em builds de produção.
    install(Logging) { level = LogLevel.NONE }
}
