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

package me.proton.android.drive.ui.viewmodel

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.launch
import me.proton.android.drive.document.scanner.domain.usecase.CollectDocumentScannerResult
import me.proton.android.drive.extension.getDefaultMessage
import me.proton.android.drive.extension.log
import me.proton.core.drive.base.domain.log.LogTag.VIEW_MODEL
import me.proton.core.drive.base.domain.provider.ConfigurationProvider
import me.proton.core.drive.base.domain.usecase.BroadcastMessages
import me.proton.core.drive.base.presentation.extension.require
import me.proton.core.drive.base.presentation.viewmodel.UserViewModel
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.drive.messagequeue.domain.entity.BroadcastMessage
import me.proton.core.drive.share.domain.entity.ShareId
import javax.inject.Inject

@HiltViewModel
class ScanDocumentViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    @param:ApplicationContext private val appContext: Context,
    private val collectDocumentScannerResult: CollectDocumentScannerResult,
    private val broadcastMessages: BroadcastMessages,
    private val configurationProvider: ConfigurationProvider,
) : ViewModel(), UserViewModel by UserViewModel(savedStateHandle) {

    private val folderId = FolderId(
        shareId = ShareId(userId, savedStateHandle.require(KEY_SHARE_ID)),
        id = savedStateHandle.require(KEY_FOLDER_ID),
    )

    fun onScanDocumentResult(
        result: GmsDocumentScanningResult?,
        navigateToScanDocumentName: (FolderId, Long, String) -> Unit,
        navigateBack: () -> Unit,
    ) = viewModelScope.launch {
        if (result == null) {
            navigateBack()
            return@launch
        }
        collectDocumentScannerResult(
            userId = userId,
            pdfUri = result.pdf?.uri,
            pageUris = result.pages?.map { page -> page.imageUri } ?: emptyList(),
        )
            .onFailure { error ->
                error.log(VIEW_MODEL, "Collection of document scanner result failed")
                broadcastMessages(
                    userId = userId,
                    message = error.getDefaultMessage(
                        context = appContext,
                        useExceptionMessage = configurationProvider.useExceptionMessage,
                    ),
                    type = BroadcastMessage.Type.ERROR,
                )
                navigateBack()
            }
            .onSuccess { scanResult ->
                navigateToScanDocumentName(folderId, scanResult.id, scanResult.basename)
            }
    }

    fun onScanDocumentError(error: Throwable) {
        error.log(VIEW_MODEL, "Scan document error occurred")
        broadcastMessages(
            userId = userId,
            message = error.getDefaultMessage(
                context = appContext,
                useExceptionMessage = configurationProvider.useExceptionMessage,
            ),
            type = BroadcastMessage.Type.ERROR,
        )
    }

    companion object {
        const val KEY_SHARE_ID = "shareId"
        const val KEY_FOLDER_ID = "folderId"
    }
}
