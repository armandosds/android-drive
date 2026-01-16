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

package me.proton.core.drive.upload.domain.usecase

import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import me.proton.core.drive.base.domain.entity.Bytes
import me.proton.core.drive.base.domain.entity.FileTypeCategory
import me.proton.core.drive.base.domain.entity.TimestampS
import me.proton.core.drive.base.domain.entity.toFileTypeCategory
import me.proton.core.drive.base.domain.entity.toTimestampS
import me.proton.core.drive.base.domain.extension.bytes
import me.proton.core.drive.base.domain.extension.toResult
import me.proton.core.drive.base.domain.log.LogTag
import me.proton.core.drive.base.domain.provider.ConfigurationProvider
import me.proton.core.drive.base.domain.provider.DriveClientProvider
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.crypto.domain.usecase.file.GetFileName
import me.proton.core.drive.eventmanager.base.domain.usecase.UpdateEventAction
import me.proton.core.drive.file.base.domain.entity.ThumbnailType
import me.proton.core.drive.linkupload.domain.entity.UploadFileLink
import me.proton.core.drive.linkupload.domain.entity.UploadState
import me.proton.core.drive.linkupload.domain.usecase.UpdateUploadFileCreationTime
import me.proton.core.drive.linkupload.domain.usecase.UpdateUploadState
import me.proton.core.drive.share.domain.entity.Share
import me.proton.core.drive.share.domain.usecase.GetShare
import me.proton.core.drive.thumbnail.domain.usecase.CreateThumbnail
import me.proton.core.drive.upload.domain.resolver.UriResolver
import me.proton.core.util.kotlin.CoreLogger
import me.proton.drive.sdk.DriveClient
import me.proton.drive.sdk.entity.FileUploaderRequest
import me.proton.drive.sdk.entity.UploadResult
import me.proton.drive.sdk.extension.withCancellable
import me.proton.drive.sdk.internal.Uid
import me.proton.drive.sdk.uploader
import java.io.InputStream
import javax.inject.Inject
import me.proton.drive.sdk.entity.ThumbnailType as SdkThumbnailType

class UploadFileSdk @Inject constructor(
    private val driveClientProvider: DriveClientProvider,
    private val uriResolver: UriResolver,
    private val updateEventAction: UpdateEventAction,
    private val createThumbnail: CreateThumbnail,
    private val updateUploadState: UpdateUploadState,
    private val getFileName: GetFileName,
    private val updateUploadFileCreationTime: UpdateUploadFileCreationTime,
    private val configurationProvider: ConfigurationProvider,
    private val getShare: GetShare,
) {
    suspend operator fun invoke(
        uploadFileLink: UploadFileLink,
        uriString: String,
        block: suspend (Bytes) -> Unit,
    ) = coRunCatching {
        val driveClient = driveClientProvider.getOrCreate(uploadFileLink.userId).getOrThrow()

        updateUploadState(uploadFileLink.id, UploadState.CREATING_NEW_FILE).getOrThrow()
        updateUploadFileCreationTime(uploadFileLink.id, TimestampS()).getOrThrow()
        val fileName = getFileName(
            name = uploadFileLink.name,
            folderId = uploadFileLink.parentLinkId,
        ).getOrThrow()
        val size = requireNotNull(uriResolver.getSize(uriString)) {
            "Cannot get size of $uriString"
        }
        val lastModified = requireNotNull(uriResolver.getLastModified(uriString)) {
            "Cannot get last modified of $uriString"
        }.toTimestampS()
        uriResolver.useInputStream(uriString) { inputStream ->
            driveClient.upload(
                uploadFileLink = uploadFileLink,
                name = fileName,
                size = size,
                lastModified = lastModified,
                inputStream = inputStream,
                thumbnails = uploadFileLink.createThumbnails(uriString),
                block = block,
            )
        }
    }.onFailure {
        updateUploadState(uploadFileLink.id, UploadState.IDLE)
    }

    private suspend fun UploadFileLink.createThumbnails(
        uriString: String,
    ): Map<SdkThumbnailType, ByteArray> {
        val defaultThumbnail = this@UploadFileSdk.createThumbnail(
            uri = uriString,
            mimeType = mimeType,
            type = ThumbnailType.DEFAULT,
        ).getOrThrow()
        val photoThumbnail = if (isBiggerThenPhotoThumbnail && isImagePhoto()) {
            this@UploadFileSdk.createThumbnail(
                uri = uriString,
                mimeType = mimeType,
                type = ThumbnailType.PHOTO,
            ).getOrThrow()
        } else {
            null
        }
        return buildMap {
            defaultThumbnail?.let { put(SdkThumbnailType.THUMBNAIL, defaultThumbnail) }
            photoThumbnail?.let { put(SdkThumbnailType.PREVIEW, photoThumbnail) }
        }
    }

    private suspend fun DriveClient.upload(
        uploadFileLink: UploadFileLink,
        name: String,
        size: Bytes,
        lastModified: TimestampS,
        inputStream: InputStream,
        thumbnails: Map<SdkThumbnailType, ByteArray>,
        block: suspend (Bytes) -> Unit
    ): UploadResult = updateEventAction(uploadFileLink.userId, uploadFileLink.volumeId) {
        coroutineScope {
            try {
                withCancellable(
                    uploader(
                        FileUploaderRequest(
                            parentFolderUid = Uid.makeNodeUid(
                                volumeId = uploadFileLink.volumeId.id,
                                nodeId = uploadFileLink.parentLinkId.id,
                            ),
                            name = name,
                            mediaType = uploadFileLink.mimeType,
                            fileSize = size.value,
                            lastModificationTime = lastModified.value,
                            overrideExistingDraftByOtherClient = false,
                        ),
                    )
                ) { uploader ->
                    var uploading = false
                    withCancellable(
                        uploader.uploadFromStream(
                            coroutineScope = this,
                            inputStream = inputStream,
                            thumbnails = thumbnails,
                            progress = { bytesCompleted, bytesInTotal ->
                                CoreLogger.i(
                                    LogTag.DRIVE_SDK,
                                    "upload progress: $bytesCompleted/$bytesInTotal"
                                )
                                if (!uploading) {
                                    uploading = true
                                    updateUploadState(
                                        uploadFileLink.id,
                                        UploadState.UPLOADING_BLOCKS
                                    ).getOrThrow()
                                }
                                block(bytesCompleted.bytes)
                            }
                        )) { controller ->
                        controller.awaitCompletion()
                    }
                }
            } finally {
                if (!isActive) {
                    withContext(NonCancellable) {
                        updateUploadState(uploadFileLink.id, UploadState.IDLE)
                    }
                }
            }
        }
    }

    private val UploadFileLink.isBiggerThenPhotoThumbnail: Boolean
        get() = mediaResolution?.let { resolution ->
            resolution.width > configurationProvider.thumbnailPhoto.maxWidth ||
                    resolution.height > configurationProvider.thumbnailPhoto.maxHeight
        } ?: false

    private suspend fun UploadFileLink.isPhoto(): Boolean {
        val share = getShare(shareId, flowOf(false)).toResult().getOrThrow()
        return share.type == Share.Type.PHOTO
    }

    private val UploadFileLink.isImage: Boolean get() = mimeType.toFileTypeCategory() == FileTypeCategory.Image

    private suspend fun UploadFileLink.isImagePhoto(): Boolean = isPhoto() && isImage
}
