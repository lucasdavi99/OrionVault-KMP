package com.cuboidestudio.orionvault.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cuboidestudio.orionvault.AppContainer
import com.cuboidestudio.orionvault.domain.model.VaultFolder
import com.cuboidestudio.orionvault.domain.model.VaultItem
import com.cuboidestudio.orionvault.domain.util.BreachStatus
import com.cuboidestudio.orionvault.domain.util.PasswordReuseChecker
import com.cuboidestudio.orionvault.domain.util.Sha1
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

sealed interface ItemEditorStep {
    /** Etapa prévia, exibida somente na criação, onde o usuário escolhe quais campos opcionais incluir. */
    data object FieldConfig : ItemEditorStep
    data object Form : ItemEditorStep
}

class ItemEditorViewModel(
    private val container: AppContainer,
    private val folderId: String?,
    private val itemId: String?
) : ViewModel() {
    private val _item = MutableStateFlow<VaultItem?>(null)
    val item: StateFlow<VaultItem?> = _item.asStateFlow()

    private val _step = MutableStateFlow<ItemEditorStep>(
        if (itemId == null) ItemEditorStep.FieldConfig else ItemEditorStep.Form
    )
    val step: StateFlow<ItemEditorStep> = _step.asStateFlow()

    private val _includeUsername = MutableStateFlow(false)
    val includeUsername: StateFlow<Boolean> = _includeUsername.asStateFlow()

    private val _includeEmail = MutableStateFlow(false)
    val includeEmail: StateFlow<Boolean> = _includeEmail.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _saved = MutableStateFlow(false)
    val saved: StateFlow<Boolean> = _saved.asStateFlow()

    private val _folders = MutableStateFlow<List<VaultFolder>>(emptyList())
    val folders: StateFlow<List<VaultFolder>> = _folders.asStateFlow()

    private val _selectedFolderId = MutableStateFlow(folderId)
    val selectedFolderId: StateFlow<String?> = _selectedFolderId.asStateFlow()

    /** Senhas de todos os outros itens do cofre (a própria [itemId] fica de fora), para a checagem de reuso. */
    private val _otherPasswords = MutableStateFlow<List<String>>(emptyList())

    private val _reuseCount = MutableStateFlow(0)
    val reuseCount: StateFlow<Int> = _reuseCount.asStateFlow()

    private val _breachStatus = MutableStateFlow<BreachStatus>(BreachStatus.Idle)
    val breachStatus: StateFlow<BreachStatus> = _breachStatus.asStateFlow()

    private var breachCheckSettingEnabled = false
    private var breachCheckJob: Job? = null

    init {
        viewModelScope.launch {
            _folders.value = container.vaultRepository.listAllFolders()
        }
        if (itemId != null) {
            viewModelScope.launch {
                val loaded = container.vaultRepository.listItems(folderId).firstOrNull { it.id == itemId }
                _item.value = loaded
                _includeUsername.value = loaded?.username != null
                _includeEmail.value = loaded?.email != null
                if (loaded != null) {
                    _selectedFolderId.value = loaded.folderId
                }
            }
        }
        viewModelScope.launch {
            _otherPasswords.value = container.vaultRepository.listAllItems()
                .filter { it.id != itemId }
                .map { it.password }
        }
        viewModelScope.launch {
            breachCheckSettingEnabled = container.secureCredentialStore.loadBreachCheckEnabled()
        }
    }

    /** Chamado a cada mudança do campo de senha no editor — nunca bloqueia `save()`. */
    fun onPasswordChanged(password: String) {
        _reuseCount.value = PasswordReuseChecker.countUsages(password, _otherPasswords.value)

        breachCheckJob?.cancel()
        if (!breachCheckSettingEnabled || password.isBlank()) {
            _breachStatus.value = BreachStatus.Idle
            return
        }
        breachCheckJob = viewModelScope.launch {
            _breachStatus.value = BreachStatus.Checking
            delay(500)
            val digest = Sha1.hex(password)
            val prefix = digest.take(5)
            val suffix = digest.substring(5)
            val result = withTimeoutOrNull(6_000) {
                container.breachCheckApiClient.checkRange(prefix)
            }
            _breachStatus.value = result?.getOrNull()
                ?.firstOrNull { it.suffix == suffix }
                ?.let { BreachStatus.Breached(it.count) }
                ?: if (result?.isSuccess == true) BreachStatus.NotBreached else BreachStatus.CheckFailed
        }
    }

    fun selectFolder(id: String?) {
        _selectedFolderId.value = id
    }

    fun setIncludeUsername(value: Boolean) {
        _includeUsername.value = value
    }

    fun setIncludeEmail(value: Boolean) {
        _includeEmail.value = value
    }

    /** Chamado pelo botão "Avançar" da etapa prévia (somente na criação). */
    fun advanceToForm() {
        _step.value = ItemEditorStep.Form
    }

    fun save(title: String, username: String?, email: String?, password: String, url: String?, notes: String?) {
        if (title.isBlank() || password.isBlank()) {
            _errorMessage.value = "Título e senha são obrigatórios"
            return
        }
        _errorMessage.value = null
        viewModelScope.launch {
            container.sessionManager.recordActivity()
            val targetFolderId = _selectedFolderId.value
            if (itemId != null) {
                container.vaultRepository.updateItem(itemId, targetFolderId, title, username, email, password, url, notes)
            } else {
                container.vaultRepository.createItem(targetFolderId, title, username, email, password, url, notes)
            }
            _saved.value = true
        }
    }

    fun delete() {
        val id = itemId ?: return
        viewModelScope.launch {
            container.sessionManager.recordActivity()
            container.vaultRepository.deleteItem(id)
            _saved.value = true
        }
    }
}
