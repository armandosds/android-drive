/*
 * Copyright (c) 2026 Proton AG.
 * This file is part of Proton Drive.
 *
 * Proton Drive is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Proton Drive is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Proton Drive.  If not, see <https://www.gnu.org/licenses/>.
 */

package me.proton.android.drive.ui.handler

import androidx.lifecycle.SavedStateHandle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.drive.share.domain.entity.ShareId

class FolderCreatedHandlerDelegate(
    private val savedStateHandle: SavedStateHandle,
) : FolderCreatedHandler {

    private val _folderCreatedFlow = MutableStateFlow(
        savedStateHandle.get<String>(KEY_FOLDER_ID)?.let { id ->
            val userId = savedStateHandle.get<String>(KEY_USER_ID) ?: return@let null
            val shareId = savedStateHandle.get<String>(KEY_SHARE_ID) ?: return@let null
            FolderId(ShareId(UserId(userId), shareId), id)
        }
    )

    val folderCreatedFlow: Flow<FolderId> = _folderCreatedFlow
        .filterNotNull()

    override fun onFolderCreated(folderId: FolderId) {
        savedStateHandle[KEY_USER_ID] = folderId.shareId.userId.id
        savedStateHandle[KEY_SHARE_ID] = folderId.shareId.id
        savedStateHandle[KEY_FOLDER_ID] = folderId.id
        _folderCreatedFlow.value = folderId
    }

    fun consume() {
        savedStateHandle[KEY_FOLDER_ID] = null
        _folderCreatedFlow.value = null
    }

    companion object {
        const val KEY_USER_ID = "key.folderCreatedUserId"
        const val KEY_SHARE_ID = "key.folderCreatedShareId"
        const val KEY_FOLDER_ID = "key.folderCreatedId"
    }
}
