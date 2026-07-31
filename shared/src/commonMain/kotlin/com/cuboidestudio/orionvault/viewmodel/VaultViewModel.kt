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

/** Navegação em árvore de pastas + listagem de itens da pasta atual. */
class VaultViewModel(private val container: AppContainer) : ViewModel() {
    /** Caminho da raiz até a pasta atual; vazio significa raiz do cofre. */
    private val _breadcrumb = MutableStateFlow<List<VaultFolder>>(emptyList())
    val breadcrumb: StateFlow<List<VaultFolder>> = _breadcrumb.asStateFlow()

    private val _subfolders = MutableStateFlow<List<VaultFolder>>(emptyList())
    val subfolders: StateFlow<List<VaultFolder>> = _subfolders.asStateFlow()

    private val _items = MutableStateFlow<List<VaultItem>>(emptyList())
    val items: StateFlow<List<VaultItem>> = _items.asStateFlow()

    val currentFolderId: String? get() = _breadcrumb.value.lastOrNull()?.id

    init {
        refresh()
    }

    fun navigateInto(folder: VaultFolder) {
        onActivity()
        _breadcrumb.value = _breadcrumb.value + folder
        refresh()
    }

    fun navigateUp() {
        onActivity()
        _breadcrumb.value = _breadcrumb.value.dropLast(1)
        refresh()
    }

    fun navigateToRoot() {
        onActivity()
        _breadcrumb.value = emptyList()
        refresh()
    }

    fun refresh() {
        onActivity()
        viewModelScope.launch {
            val folderId = currentFolderId
            _subfolders.value = container.vaultRepository.listFolders(folderId)
            _items.value = if (folderId != null) container.vaultRepository.listItems(folderId) else emptyList()
        }
    }

    fun deleteItem(id: String) {
        viewModelScope.launch {
            container.vaultRepository.deleteItem(id)
            refresh()
        }
    }

    fun onActivity() {
        container.sessionManager.recordActivity()
    }
}
