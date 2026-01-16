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

package me.proton.core.drive.drivelink.download.domain.usecase

import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import me.proton.core.drive.base.domain.entity.Percentage
import me.proton.core.drive.base.domain.extension.getOrNull
import me.proton.core.drive.base.domain.extension.toResult
import me.proton.core.drive.base.domain.log.LogTag.DOWNLOAD
import me.proton.core.drive.base.domain.log.logId
import me.proton.core.drive.base.domain.provider.DriveClientProvider
import me.proton.core.drive.base.domain.usecase.GetDownloadStagingTempFolder
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.drivelink.domain.extension.decryptedFileName
import me.proton.core.drive.drivelink.domain.usecase.GetDriveLink
import me.proton.core.drive.link.domain.entity.FileId
import me.proton.core.drive.link.domain.extension.userId
import me.proton.core.drive.linkdownload.domain.entity.DownloadState
import me.proton.core.drive.linkdownload.domain.usecase.SetDownloadState
import me.proton.core.drive.linkdownload.domain.usecase.SetSignatureVerificationFailed
import me.proton.core.drive.thumbnail.domain.usecase.GetThumbnailPermanentFile
import me.proton.core.drive.volume.domain.entity.VolumeId
import me.proton.core.util.kotlin.CoreLogger
import me.proton.drive.sdk.DownloadController
import me.proton.drive.sdk.ProtonDriveSdkException
import me.proton.drive.sdk.ProtonSdkError
import me.proton.drive.sdk.ProtonSdkError.ErrorDomain
import me.proton.drive.sdk.Uid
import me.proton.drive.sdk.downloader
import me.proton.drive.sdk.extension.withCancellable
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import javax.inject.Inject

class DownloadFileSdk @Inject constructor(
    private val driveClientProvider: DriveClientProvider,
    private val setDownloadState: SetDownloadState,
    private val getThumbnailPermanentFile: GetThumbnailPermanentFile,
    private val moveFileIfExists: MoveFileIfExists,
    private val getDriveLink: GetDriveLink,
    private val getDownloadStagingTempFolder: GetDownloadStagingTempFolder,
    private val setSignatureVerificationFailed: SetSignatureVerificationFailed,
    private val removeSignatureVerificationFailed: SetSignatureVerificationFailed,
) {

    suspend operator fun invoke(
        volumeId: VolumeId,
        fileId: FileId,
        revisionId: String,
        progress: MutableStateFlow<Percentage>,
    ): Result<Unit> {
        var file: File? = null
        var tmpFile: File? = null
        return coRunCatching {
            setDownloadState(fileId, DownloadState.Downloading)
            file = moveFileIfExists(fileId).getOrThrow()
            val driveLink = getDriveLink(fileId).toResult().getOrThrow()
            getThumbnailPermanentFile(volumeId, driveLink.link, revisionId).getOrThrow()
            if (file.exists()) {
                CoreLogger.d(DOWNLOAD, "File already downloaded")
                setDownloadState(driveLink.link, DownloadState.Ready)
                return@coRunCatching
            }

            val volumeId = driveLink.volumeId
            val fileId = driveLink.id

            tmpFile = File(
                getDownloadStagingTempFolder(userId = fileId.userId, volumeId.id, revisionId),
                driveLink.decryptedFileName,
            )

            val client = driveClientProvider.getOrCreate(fileId.userId).getOrThrow()
            coroutineScope {
                try {
                    withCancellable(
                        client.downloader(
                            Uid.makeNodeRevisionUid(
                                volumeId = volumeId.id,
                                nodeId = fileId.id,
                                revisionId = revisionId,
                            )
                        )
                    ) { downloader ->
                        withCancellable(
                            downloader.downloadToStream(
                                coroutineScope = this,
                                outputStream = tmpFile.outputStream()
                            ) { bytesCompleted: Long, bytesInTotal: Long ->
                                progress.tryEmit(Percentage(bytesCompleted.toFloat() / bytesInTotal))
                            }
                        ) { controller ->
                            try {
                                removeSignatureVerificationFailed(fileId).getOrNull(
                                    tag = DOWNLOAD,
                                    message= "Failed to remove that signature verification failed"
                                )
                                controller.awaitCompletion()
                            } catch (e: ProtonDriveSdkException) {
                                val error = e.error
                                if (controller.isNotVerified(error)) {
                                    CoreLogger.w(
                                        DOWNLOAD,
                                        e,
                                        "File downloaded but not verified, continuing"
                                    )
                                    setSignatureVerificationFailed(fileId).getOrNull(
                                        tag = DOWNLOAD,
                                        message = "Failed to set that signature verification failed"
                                    )
                                } else {
                                    throw e
                                }
                            }
                        }
                    }
                } finally {
                    if (!isActive) {
                        withContext(NonCancellable) {
                            tmpFile.delete()
                            setDownloadState(fileId, DownloadState.Error)
                        }
                    }
                }
            }

            Files.move(
                tmpFile.toPath(),
                file.toPath(),
                StandardCopyOption.REPLACE_EXISTING
            )
            setDownloadState(fileId, DownloadState.Ready)
        }.onFailure { error ->
            CoreLogger.e(DOWNLOAD, error, "Cannot download ${fileId.id.logId()}")
            tmpFile?.delete()
            file?.delete()
            setDownloadState(fileId, DownloadState.Error)
        }
    }

    private suspend fun DownloadController.isNotVerified(error: ProtonSdkError?): Boolean {
        return error != null
                && error.domain == ErrorDomain.DataIntegrity
                && isDownloadCompleteWithVerificationIssue()
    }
}
