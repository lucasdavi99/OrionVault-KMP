package com.cuboidestudio.orionvault.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cuboidestudio.orionvault.AppContainer
import com.cuboidestudio.orionvault.domain.model.VaultFolder
import com.cuboidestudio.orionvault.domain.model.VaultItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Navegação em árvore de pastas + listagem de itens da pasta atual. */
class VaultViewModel(private val container: AppContainer) : ViewModel() {
    /** Caminho da raiz até a pasta atual; vazio significa raiz do cofre. */
    private val _breadcrumb = MutableStateFlow<List<VaultFolder>>(emptyList())
    val breadcrumb: StateFlow<List<VaultFolder>> = _breadcrumb.asStateFlow()

    private val _subfolders = MutableStateFlow<List<VaultFolder>>(emptyList())
    val subfolders: StateFlow<List<VaultFolder>> = _subfolders.asStateFlow()

    private val _items = MutableStateFlow<List<VaultItem>>(emptyList())
    val items: StateFlow<List<VaultItem>> = _items.asStateFlow()

    /** Contas sem pasta associada, para a aba "Contas" do Dashboard (independe da pasta navegada). */
    private val _unfiledItems = MutableStateFlow<List<VaultItem>>(emptyList())
    val unfiledItems: StateFlow<List<VaultItem>> = _unfiledItems.asStateFlow()

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
            val unlocked = container.vaultRepository.isUnlocked()
            // Leitura do banco + decriptação (custosa para listas grandes) roda fora da main thread
            // para não travar a UI enquanto a tela de contas carrega.
            val (subfolders, items, unfiledItems) = withContext(Dispatchers.Default) {
                Triple(
                    container.vaultRepository.listFolders(folderId),
                    // Contas avulsas (folderId nulo) só aparecem na aba "Contas" — a raiz de "Pastas" mostra só pastas.
                    if (unlocked && folderId != null) container.vaultRepository.listItems(folderId) else emptyList(),
                    if (unlocked) container.vaultRepository.listItems(null) else emptyList()
                )
            }
            _subfolders.value = subfolders
            _items.value = items
            _unfiledItems.value = unfiledItems
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
