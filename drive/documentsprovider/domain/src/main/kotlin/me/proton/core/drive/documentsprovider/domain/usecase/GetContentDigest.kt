/*
 * Copyright (c) 2025 Proton AG.
 * This file is part of Proton Core.
 *
 * Proton Core is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Proton Core is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Proton Core.  If not, see <https://www.gnu.org/licenses/>.
 */

package me.proton.core.drive.documentsprovider.domain.usecase

import me.proton.core.drive.base.domain.extension.toResult
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.documentsprovider.domain.entity.DocumentId
import me.proton.core.drive.drivelink.domain.entity.DriveLink
import me.proton.core.drive.drivelink.domain.usecase.GetDriveLink
import me.proton.core.drive.link.domain.entity.FileId
import me.proton.core.drive.link.domain.extension.userId
import java.io.FileInputStream
import javax.inject.Inject
import me.proton.core.drive.base.domain.usecase.GetContentDigest as GetBaseContentDigest
import me.proton.core.drive.drivelink.crypto.domain.usecase.GetContentDigest as GetDriveLinkContentDigest

class GetContentDigest @Inject constructor(
    private val openDocument: OpenDocument,
    private val getDriveLink: GetDriveLink,
    private val getBaseContentDigest: GetBaseContentDigest,
    private val getDriveLinkContentDigest: GetDriveLinkContentDigest,
) {

    suspend operator fun invoke(
        fileId: FileId,
        fallbackToRecalculateFromFile: Boolean = false,
    ): Result<String> = coRunCatching {
        invoke(
            driveLink = getDriveLink(fileId = fileId).toResult().getOrThrow(),
            fallbackToRecalculateFromFile = fallbackToRecalculateFromFile,
        ).getOrThrow()
    }

    suspend operator fun invoke(
        driveLink: DriveLink.File,
        fallbackToRecalculateFromFile: Boolean = false,
    ): Result<String> = coRunCatching {
        getDriveLinkContentDigest(driveLink)
            .recoverCatching {
                if (fallbackToRecalculateFromFile) {
                    getContentDigestFromFile(driveLink).getOrThrow()
                } else {
                    throw it
                }
            }
            .getOrThrow()
    }

    private suspend fun getContentDigestFromFile(
        driveLink: DriveLink.File
    ): Result<String> = coRunCatching {
        val pfd = openDocument(
            documentId = DocumentId(driveLink.userId, driveLink.id),
            mode = "r",
            signal = null,
        )
        FileInputStream(pfd.fileDescriptor).use { inputStream ->
            getBaseContentDigest(inputStream).getOrThrow()
        }
    }
}
