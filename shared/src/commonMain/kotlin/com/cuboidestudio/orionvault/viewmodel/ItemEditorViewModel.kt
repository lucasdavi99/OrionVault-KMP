package com.cuboidestudio.orionvault.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cuboidestudio.orionvault.AppContainer
import com.cuboidestudio.orionvault.domain.model.VaultFolder
import com.cuboidestudio.orionvault.domain.model.VaultItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

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
