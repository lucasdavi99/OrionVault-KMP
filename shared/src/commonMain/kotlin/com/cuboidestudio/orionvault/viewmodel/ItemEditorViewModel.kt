package com.cuboidestudio.orionvault.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cuboidestudio.orionvault.AppContainer
import com.cuboidestudio.orionvault.domain.model.VaultItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ItemEditorViewModel(
    private val container: AppContainer,
    private val folderId: String,
    private val itemId: String?
) : ViewModel() {
    private val _item = MutableStateFlow<VaultItem?>(null)
    val item: StateFlow<VaultItem?> = _item.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _saved = MutableStateFlow(false)
    val saved: StateFlow<Boolean> = _saved.asStateFlow()

    init {
        if (itemId != null) {
            viewModelScope.launch {
                _item.value = container.vaultRepository.listItems(folderId).firstOrNull { it.id == itemId }
            }
        }
    }

    fun save(title: String, username: String?, password: String, url: String?, notes: String?) {
        if (title.isBlank() || password.isBlank()) {
            _errorMessage.value = "Título e senha são obrigatórios"
            return
        }
        _errorMessage.value = null
        viewModelScope.launch {
            container.sessionManager.recordActivity()
            if (itemId != null) {
                container.vaultRepository.updateItem(itemId, title, username, password, url, notes)
            } else {
                container.vaultRepository.createItem(folderId, title, username, password, url, notes)
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
