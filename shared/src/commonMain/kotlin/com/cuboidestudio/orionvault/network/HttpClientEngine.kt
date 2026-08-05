package com.cuboidestudio.orionvault.network

import io.ktor.client.HttpClient
import kotlinx.serialization.json.Json

/**
 * Cria o [HttpClient] já configurado (ContentNegotiation + Logging) com o engine nativo de
 * cada plataforma. Segue exatamente o padrão de `createSqlDriver`/`createSecureCredentialStore`:
 * o `expect` devolve o objeto pronto, mantendo a configuração específica de engine local a
 * cada source set.
 */
expect fun createHttpClient(json: Json): HttpClient
