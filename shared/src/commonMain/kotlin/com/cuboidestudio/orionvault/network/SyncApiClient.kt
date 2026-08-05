@file:OptIn(ExperimentalTime::class)

package com.cuboidestudio.orionvault.network

import com.cuboidestudio.orionvault.crypto.KdfParams
import com.cuboidestudio.orionvault.domain.model.SyncFolder
import com.cuboidestudio.orionvault.domain.model.SyncItem
import com.cuboidestudio.orionvault.session.AccountSessionManager
import io.ktor.client.HttpClient
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.encodeURLParameter
import io.ktor.http.isSuccess
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * Wrapper fino sobre o PostgREST do Supabase (`/rest/v1/...`).
 *
 * Zero-knowledge: os únicos dados que saem daqui são blobs AEAD já cifrados pelo repositório e
 * os metadados que já eram texto claro localmente (`items.title`, `folders.name`). Nem a Master
 * Password, nem a Secret Key, nem a chave derivada do cofre existem neste arquivo.
 *
 * Toda chamada resolve o bearer token via [AccountSessionManager.currentAccessToken], que
 * renova de forma transparente um token expirado antes da requisição.
 */
class SyncApiClient(
    private val httpClient: HttpClient,
    private val json: Json,
    private val accountSessionManager: AccountSessionManager
) {
    // --- Folders ---

    suspend fun insertFolder(folder: SyncFolder, userId: String): PushResult {
        val dto = RemoteFolderDto(
            id = folder.id,
            userId = userId,
            parentId = folder.parentId,
            name = folder.name,
            depth = folder.depth,
            version = 1,
            deleted = false,
            createdAt = folder.createdAt.toIso(),
            updatedAt = folder.updatedAt.toIso()
        )
        val rows = decodeFolders(post("${SupabaseConfig.restBaseUrl}/folders", dto, RemoteFolderDto.serializer()))
        return rows.firstOrNull()?.let { PushResult.Applied(it.version) } ?: PushResult.Conflict
    }

    suspend fun updateFolderIfVersion(folder: SyncFolder, expectedVersion: Long): PushResult {
        val patch = RemoteFolderPatch(
            parentId = folder.parentId,
            name = folder.name,
            depth = folder.depth,
            version = expectedVersion + 1,
            deleted = false,
            updatedAt = folder.updatedAt.toIso()
        )
        return patchRow("folders", folder.id, expectedVersion, patch, RemoteFolderPatch.serializer())
    }

    suspend fun deleteFolderIfVersion(folder: SyncFolder, expectedVersion: Long): PushResult {
        val patch = RemoteFolderPatch(
            version = expectedVersion + 1,
            deleted = true,
            updatedAt = folder.updatedAt.toIso()
        )
        return patchRow("folders", folder.id, expectedVersion, patch, RemoteFolderPatch.serializer())
    }

    suspend fun fetchFoldersSince(cursor: Long, userId: String): List<Pair<SyncFolder, Boolean>> {
        val url = "${SupabaseConfig.restBaseUrl}/folders" +
            "?user_id=eq.$userId&updated_at=gt.${cursor.toIso().encodeURLParameter()}&order=updated_at.asc"
        return decodeFolders(get(url)).map { it.toSyncFolder() to it.deleted }
    }

    suspend fun fetchFolder(id: String): SyncFolder? =
        decodeFolders(get("${SupabaseConfig.restBaseUrl}/folders?id=eq.$id")).firstOrNull()?.toSyncFolder()

    // --- Items ---

    suspend fun insertItem(item: SyncItem, userId: String): PushResult {
        val dto = item.toRemoteDto(userId, version = 1, deleted = false)
        val rows = decodeItems(post("${SupabaseConfig.restBaseUrl}/items", dto, RemoteItemDto.serializer()))
        return rows.firstOrNull()?.let { PushResult.Applied(it.version) } ?: PushResult.Conflict
    }

    suspend fun updateItemIfVersion(item: SyncItem, expectedVersion: Long): PushResult {
        val patch = RemoteItemPatch(
            folderId = item.folderId,
            title = item.title,
            usernameCiphertext = CipherFieldCodec.ciphertextOf(item.usernameCipher),
            usernameNonce = CipherFieldCodec.nonceOf(item.usernameCipher),
            emailCiphertext = CipherFieldCodec.ciphertextOf(item.emailCipher),
            emailNonce = CipherFieldCodec.nonceOf(item.emailCipher),
            passwordCiphertext = CipherFieldCodec.ciphertextOf(item.passwordCipher) ?: "",
            passwordNonce = CipherFieldCodec.nonceOf(item.passwordCipher) ?: "",
            urlCiphertext = CipherFieldCodec.ciphertextOf(item.urlCipher),
            urlNonce = CipherFieldCodec.nonceOf(item.urlCipher),
            notesCiphertext = CipherFieldCodec.ciphertextOf(item.notesCipher),
            notesNonce = CipherFieldCodec.nonceOf(item.notesCipher),
            version = expectedVersion + 1,
            deleted = false,
            updatedAt = item.updatedAt.toIso()
        )
        return patchRow("items", item.id, expectedVersion, patch, RemoteItemPatch.serializer())
    }

    suspend fun deleteItemIfVersion(item: SyncItem, expectedVersion: Long): PushResult {
        val patch = RemoteItemPatch(
            version = expectedVersion + 1,
            deleted = true,
            updatedAt = item.updatedAt.toIso()
        )
        return patchRow("items", item.id, expectedVersion, patch, RemoteItemPatch.serializer())
    }

    /** Snapshot de histórico gravado após cada escrita bem-sucedida em `items` (plano 1.3). */
    suspend fun insertItemVersion(item: SyncItem, version: Long, deviceId: String) {
        val dto = RemoteItemVersionDto(
            itemId = item.id,
            version = version,
            title = item.title,
            usernameCiphertext = CipherFieldCodec.ciphertextOf(item.usernameCipher),
            usernameNonce = CipherFieldCodec.nonceOf(item.usernameCipher),
            emailCiphertext = CipherFieldCodec.ciphertextOf(item.emailCipher),
            emailNonce = CipherFieldCodec.nonceOf(item.emailCipher),
            passwordCiphertext = CipherFieldCodec.ciphertextOf(item.passwordCipher) ?: "",
            passwordNonce = CipherFieldCodec.nonceOf(item.passwordCipher) ?: "",
            urlCiphertext = CipherFieldCodec.ciphertextOf(item.urlCipher),
            urlNonce = CipherFieldCodec.nonceOf(item.urlCipher),
            notesCiphertext = CipherFieldCodec.ciphertextOf(item.notesCipher),
            notesNonce = CipherFieldCodec.nonceOf(item.notesCipher),
            deviceId = deviceId
        )
        post("${SupabaseConfig.restBaseUrl}/item_versions", dto, RemoteItemVersionDto.serializer())
    }

    suspend fun fetchItemsSince(cursor: Long, userId: String): List<Pair<SyncItem, Boolean>> {
        val url = "${SupabaseConfig.restBaseUrl}/items" +
            "?user_id=eq.$userId&updated_at=gt.${cursor.toIso().encodeURLParameter()}&order=updated_at.asc"
        return decodeItems(get(url)).map { it.toSyncItem() to it.deleted }
    }

    suspend fun fetchItem(id: String): SyncItem? =
        decodeItems(get("${SupabaseConfig.restBaseUrl}/items?id=eq.$id")).firstOrNull()?.toSyncItem()

    /**
     * Um item qualquer, não deletado, desta conta — usado só para verificar (via tentativa de
     * decifragem) se a Master Password/Secret Key digitadas na restauração batem com o
     * dispositivo original, antes de finalizar o cofre local. Retorna nulo se a conta ainda não
     * tem nenhum item sincronizado (nesse caso não há como verificar antecipadamente).
     */
    suspend fun fetchSampleItem(userId: String): SyncItem? {
        val url = "${SupabaseConfig.restBaseUrl}/items" +
            "?user_id=eq.$userId&deleted=eq.false&limit=1&select=*"
        return decodeItems(get(url)).firstOrNull()?.toSyncItem()
    }

    // --- Parâmetros de KDF do cofre ---

    /**
     * Parâmetros do Argon2id publicados pelo dispositivo que criou o cofre desta conta, ou nulo
     * se a conta ainda não tem cofre associado. Usado na restauração em um segundo dispositivo,
     * que precisa do salt exato do primeiro para derivar a mesma chave.
     */
    @OptIn(ExperimentalEncodingApi::class)
    suspend fun fetchVaultKdf(userId: String): KdfParams? {
        val text = get("${SupabaseConfig.restBaseUrl}/vault_kdf?user_id=eq.$userId&select=*")
        val dto = json.decodeFromString(ListSerializer(RemoteVaultKdfDto.serializer()), text).firstOrNull()
            ?: return null
        return KdfParams(
            version = dto.version,
            opsLimit = dto.opsLimit.toULong(),
            memLimitBytes = dto.memLimitBytes,
            salt = Base64.decode(dto.salt)
        )
    }

    /**
     * Publica os parâmetros locais, se a conta ainda não tiver nenhum. `resolution=ignore-duplicates`
     * faz um conflito de chave primária virar no-op: o primeiro dispositivo vence e a linha nunca é
     * sobrescrita por outro que tenha criado um cofre novo por engano.
     */
    @OptIn(ExperimentalEncodingApi::class)
    suspend fun pushVaultKdfIfAbsent(params: KdfParams, userId: String) {
        val dto = RemoteVaultKdfDto(
            userId = userId,
            version = params.version,
            opsLimit = params.opsLimit.toLong(),
            memLimitBytes = params.memLimitBytes,
            salt = Base64.encode(params.salt),
            createdAt = Clock.System.now().toString()
        )
        val response = httpClient.post("${SupabaseConfig.restBaseUrl}/vault_kdf") {
            applyAuth()
            header("Prefer", "resolution=ignore-duplicates,return=minimal")
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(RemoteVaultKdfDto.serializer(), dto))
        }
        requireSuccess(response)
    }

    // --- HTTP plumbing ---

    private suspend fun HttpRequestBuilder.applyAuth() {
        val token = accountSessionManager.currentAccessToken() ?: error("Sessão de nuvem expirada")
        header("apikey", SupabaseConfig.anonKey)
        header("Authorization", "Bearer $token")
    }

    private suspend fun get(url: String): String {
        val response = httpClient.get(url) { applyAuth() }
        return requireSuccess(response)
    }

    private suspend fun <T> post(
        url: String,
        body: T,
        serializer: kotlinx.serialization.KSerializer<T>
    ): String {
        val response = httpClient.post(url) {
            applyAuth()
            header("Prefer", "return=representation")
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(serializer, body))
        }
        return requireSuccess(response)
    }

    private suspend fun <T> patchRow(
        table: String,
        id: String,
        expectedVersion: Long,
        body: T,
        serializer: kotlinx.serialization.KSerializer<T>
    ): PushResult {
        val response = httpClient.patch(
            "${SupabaseConfig.restBaseUrl}/$table?id=eq.$id&version=eq.$expectedVersion"
        ) {
            applyAuth()
            header("Prefer", "return=representation")
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(serializer, body))
        }
        val text = requireSuccess(response)
        // Com `return=representation`, 0 linhas afetadas devolve um array vazio: conflito.
        val versions = json.decodeFromString(ListSerializer(VersionOnlyDto.serializer()), text)
        return versions.firstOrNull()?.let { PushResult.Applied(it.version) } ?: PushResult.Conflict
    }

    private fun decodeFolders(text: String): List<RemoteFolderDto> =
        json.decodeFromString(ListSerializer(RemoteFolderDto.serializer()), text)

    private fun decodeItems(text: String): List<RemoteItemDto> =
        json.decodeFromString(ListSerializer(RemoteItemDto.serializer()), text)

    private suspend fun requireSuccess(response: HttpResponse): String {
        val text = response.bodyAsText()
        if (response.status.isSuccess()) return text
        val message = runCatching {
            json.decodeFromString(SupabaseErrorResponse.serializer(), text).bestMessage()
        }.getOrNull()
        error(message ?: "Falha na sincronização (HTTP ${response.status.value})")
    }

    private fun SyncItem.toRemoteDto(userId: String, version: Long, deleted: Boolean) = RemoteItemDto(
        id = id,
        userId = userId,
        folderId = folderId,
        title = title,
        usernameCiphertext = CipherFieldCodec.ciphertextOf(usernameCipher),
        usernameNonce = CipherFieldCodec.nonceOf(usernameCipher),
        emailCiphertext = CipherFieldCodec.ciphertextOf(emailCipher),
        emailNonce = CipherFieldCodec.nonceOf(emailCipher),
        passwordCiphertext = CipherFieldCodec.ciphertextOf(passwordCipher) ?: "",
        passwordNonce = CipherFieldCodec.nonceOf(passwordCipher) ?: "",
        urlCiphertext = CipherFieldCodec.ciphertextOf(urlCipher),
        urlNonce = CipherFieldCodec.nonceOf(urlCipher),
        notesCiphertext = CipherFieldCodec.ciphertextOf(notesCipher),
        notesNonce = CipherFieldCodec.nonceOf(notesCipher),
        version = version,
        deleted = deleted,
        createdAt = createdAt.toIso(),
        updatedAt = updatedAt.toIso()
    )
}

@kotlinx.serialization.Serializable
internal data class VersionOnlyDto(val version: Long = 1)

@OptIn(ExperimentalTime::class)
internal fun Long.toIso(): String = Instant.fromEpochMilliseconds(this).toString()

@OptIn(ExperimentalTime::class)
internal fun String.isoToEpochMillis(): Long =
    runCatching { Instant.parse(this).toEpochMilliseconds() }.getOrDefault(0L)

internal fun RemoteFolderDto.toSyncFolder() = SyncFolder(
    id = id,
    parentId = parentId,
    name = name,
    depth = depth,
    createdAt = createdAt.isoToEpochMillis(),
    updatedAt = updatedAt.isoToEpochMillis(),
    version = version,
    syncState = 0L,
    syncedVersion = version
)

internal fun RemoteItemDto.toSyncItem() = SyncItem(
    id = id,
    folderId = folderId,
    title = title,
    usernameCipher = CipherFieldCodec.toStorage(usernameCiphertext, usernameNonce),
    emailCipher = CipherFieldCodec.toStorage(emailCiphertext, emailNonce),
    passwordCipher = CipherFieldCodec.toStorage(passwordCiphertext, passwordNonce) ?: "",
    urlCipher = CipherFieldCodec.toStorage(urlCiphertext, urlNonce),
    notesCipher = CipherFieldCodec.toStorage(notesCiphertext, notesNonce),
    createdAt = createdAt.isoToEpochMillis(),
    updatedAt = updatedAt.isoToEpochMillis(),
    version = version,
    syncState = 0L,
    syncedVersion = version
)
