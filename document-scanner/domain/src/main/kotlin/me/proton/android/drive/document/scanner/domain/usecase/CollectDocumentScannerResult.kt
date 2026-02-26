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

package me.proton.android.drive.document.scanner.domain.usecase

import android.net.Uri
import me.proton.android.drive.document.scanner.domain.entity.ScanResult
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.domain.entity.TimestampS
import me.proton.core.drive.base.domain.extension.getOrNull
import me.proton.core.drive.base.domain.log.LogTag.DOCUMENT_SCANNER
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.upload.domain.usecase.CopyUriToPermanentTempFolder
import javax.inject.Inject

class CollectDocumentScannerResult @Inject constructor(
    private val copyUriToPermanentTempFolder: CopyUriToPermanentTempFolder,
    private val addScanResult: AddScanResult,
) {

    suspend operator fun invoke(
        userId: UserId,
        pdfUri: Uri? = null,
        pageUris: List<Uri> = emptyList(),
    ): Result<ScanResult> = coRunCatching {
        addScanResult(
            userId = userId,
            createTime = TimestampS(),
            pdfUri = copyPdfScannedDocument(userId, pdfUri),
            pageUris = copyPagesScannedDocument(userId, pageUris)
        ).getOrThrow()
    }

    private suspend fun copyPdfScannedDocument(userId: UserId, uri: Uri?): Uri? =
        uri?.let {
            copyUriToPermanentTempFolder(
                userId = userId,
                uriString = uri.toString(),
                path = SCAN_FOLDER,
            )
                .getOrNull(DOCUMENT_SCANNER, "Failed to copy pdf document")
        }

    private suspend fun copyPagesScannedDocument(userId: UserId, uris: List<Uri>): List<Uri> =
        uris.mapNotNull { uri ->
            copyUriToPermanentTempFolder(
                userId = userId,
                uriString = uri.toString(),
                path = SCAN_FOLDER,
            )
                .getOrNull(DOCUMENT_SCANNER, "Failed to copy image page")
        }

    companion object {
        private const val SCAN_FOLDER = "scan"
    }
}
