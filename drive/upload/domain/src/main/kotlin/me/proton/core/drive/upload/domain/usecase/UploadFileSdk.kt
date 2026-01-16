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
import me.proton.core.drive.base.domain.entity.toFileTypeCategory
import me.proton.core.drive.base.domain.extension.bytes
import me.proton.core.drive.base.domain.extension.toResult
import me.proton.core.drive.base.domain.provider.ConfigurationProvider
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.eventmanager.base.domain.usecase.UpdateEventAction
import me.proton.core.drive.file.base.domain.entity.ThumbnailType
import me.proton.core.drive.linkupload.domain.entity.UploadFileLink
import me.proton.core.drive.linkupload.domain.entity.UploadState
import me.proton.core.drive.linkupload.domain.usecase.UpdateUploadState
import me.proton.core.drive.share.domain.entity.Share
import me.proton.core.drive.share.domain.usecase.GetShare
import me.proton.core.drive.thumbnail.domain.usecase.CreateThumbnail
import me.proton.core.drive.upload.domain.manager.UploadSdkManager
import me.proton.core.drive.upload.domain.resolver.UriResolver
import me.proton.drive.sdk.UploadController
import okio.FileNotFoundException
import javax.inject.Inject
import me.proton.drive.sdk.entity.ThumbnailType as SdkThumbnailType

class UploadFileSdk @Inject constructor(
    private val uploadSdkManager: UploadSdkManager,
    private val uriResolver: UriResolver,
    private val updateEventAction: UpdateEventAction,
    private val createThumbnail: CreateThumbnail,
    private val updateUploadState: UpdateUploadState,
    private val configurationProvider: ConfigurationProvider,
    private val getShare: GetShare,
) {
    suspend operator fun invoke(
        uploadFileLink: UploadFileLink,
        uriString: String,
        block: suspend (Bytes) -> Unit,
    ) = coRunCatching {
        coroutineScope {
            var controller: UploadController? = null
            try {
                updateUploadState(uploadFileLink.id, UploadState.UPLOADING_BLOCKS).getOrThrow()

                controller = uploadSdkManager.controller(uploadFileLink) { uploader ->
                    uploader.uploadFromStream(
                        coroutineScope = this,
                        inputStream = uriResolver.inputStream(uriString)
                            ?: throw FileNotFoundException("Cannot open stream for upload${uploadFileLink.id}"),
                        thumbnails = uploadFileLink.createThumbnails(uriString),
                        progress = { bytesCompleted, bytesInTotal ->
                            block(bytesCompleted.bytes)
                        }
                    )
                }
                //controller.resume()
                val result = controller.awaitCompletion()
                updateEventAction(uploadFileLink.userId, uploadFileLink.volumeId) {
                    uploadSdkManager.close(uploadFileLink)
                    result
                }
            } finally {
                if (!isActive) {
                    withContext(NonCancellable) {
                        //controller?.pause()
                        // replace cancel by pause when implemented
                        uploadSdkManager.cancel(uploadFileLink)
                        updateUploadState(uploadFileLink.id, UploadState.IDLE)
                    }
                }
            }
        }
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
