package com.cuboidestudio.orionvault.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cuboidestudio.orionvault.AppContainer
import com.cuboidestudio.orionvault.domain.repository.FolderDepthExceededException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class FolderEditorViewModel(
    private val container: AppContainer,
    private val parentId: String?,
    private val folderId: String?
) : ViewModel() {
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _saved = MutableStateFlow(false)
    val saved: StateFlow<Boolean> = _saved.asStateFlow()

    val isEditing: Boolean get() = folderId != null

    fun save(name: String) {
        if (name.isBlank()) {
            _errorMessage.value = "Nome não pode ser vazio"
            return
        }
        viewModelScope.launch {
            container.sessionManager.recordActivity()
            try {
                if (folderId != null) {
                    container.vaultRepository.renameFolder(folderId, name)
                } else {
                    container.vaultRepository.createFolder(parentId, name)
                }
                _errorMessage.value = null
                _saved.value = true
            } catch (e: FolderDepthExceededException) {
                _errorMessage.value = e.message
            }
        }
    }
}
