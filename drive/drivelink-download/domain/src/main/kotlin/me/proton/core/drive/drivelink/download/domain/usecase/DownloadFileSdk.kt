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
import me.proton.core.drive.base.domain.extension.toResult
import me.proton.core.drive.base.domain.log.LogTag
import me.proton.core.drive.base.domain.log.LogTag.DOWNLOAD
import me.proton.core.drive.base.domain.log.logId
import me.proton.core.drive.base.domain.provider.DriveClientProvider
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.drivelink.domain.usecase.GetDriveLink
import me.proton.core.drive.link.domain.entity.FileId
import me.proton.core.drive.link.domain.extension.userId
import me.proton.core.drive.linkdownload.domain.entity.DownloadState
import me.proton.core.drive.linkdownload.domain.usecase.SetDownloadState
import me.proton.core.drive.thumbnail.domain.usecase.GetThumbnailPermanentFile
import me.proton.core.drive.volume.domain.entity.VolumeId
import me.proton.core.util.kotlin.CoreLogger
import me.proton.drive.sdk.downloader
import me.proton.drive.sdk.extension.withCancellable
import me.proton.drive.sdk.internal.Uid
import javax.inject.Inject

class DownloadFileSdk @Inject constructor(
    private val driveClientProvider: DriveClientProvider,
    private val setDownloadState: SetDownloadState,
    private val getThumbnailPermanentFile: GetThumbnailPermanentFile,
    private val moveFileIfExists: MoveFileIfExists,
    private val getDriveLink: GetDriveLink,
) {

    suspend operator fun invoke(
        volumeId: VolumeId,
        fileId: FileId,
        revisionId: String,
        progress: MutableStateFlow<Percentage>,
    ): Result<Unit> = coRunCatching {
        setDownloadState(fileId, DownloadState.Downloading)
        val file = moveFileIfExists(fileId).getOrThrow()
        val driveLink = getDriveLink(fileId).toResult().getOrThrow()
        getThumbnailPermanentFile(volumeId, driveLink.link, revisionId).getOrThrow()
        if (file.exists()) {
            CoreLogger.d(LogTag.DOWNLOAD, "File already downloaded")
            setDownloadState(driveLink.link, DownloadState.Ready)
            return@coRunCatching
        }

        val volumeId = driveLink.volumeId
        val fileId = driveLink.id
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
                            outputStream = file.outputStream()
                        ) { bytesCompleted: Long, bytesInTotal: Long ->
                            CoreLogger.i(
                                LogTag.DRIVE_SDK,
                                "download progress: $bytesCompleted/$bytesInTotal"
                            )
                            progress.tryEmit(Percentage(bytesCompleted.toFloat() / bytesInTotal))
                        }
                    ) { controller ->
                        controller.awaitCompletion()
                    }
                }
            } finally {
                if (!isActive) {
                    withContext(NonCancellable) {
                        file.delete()
                        setDownloadState(fileId, DownloadState.Error)
                    }
                }
            }
        }
        setDownloadState(fileId, DownloadState.Ready)
    }.onFailure { error ->
        CoreLogger.e(DOWNLOAD, error, "Cannot download ${fileId.id.logId()}")
        setDownloadState(fileId, DownloadState.Error)
    }
}
