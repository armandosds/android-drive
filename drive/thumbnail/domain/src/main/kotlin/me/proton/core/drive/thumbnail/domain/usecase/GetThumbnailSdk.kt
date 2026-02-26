/*
 * Copyright (c) 2022-2023 Proton AG.
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

package me.proton.core.drive.thumbnail.domain.usecase

import me.proton.core.drive.base.domain.extension.toResult
import me.proton.core.drive.base.domain.log.logId
import me.proton.core.drive.base.domain.provider.ProtonDriveClientProvider
import me.proton.core.drive.base.domain.provider.ProtonPhotosClientProvider
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.file.base.domain.entity.ThumbnailType
import me.proton.core.drive.link.domain.entity.FileId
import me.proton.core.drive.link.domain.entity.Link
import me.proton.core.drive.link.domain.extension.isPhoto
import me.proton.core.drive.link.domain.extension.userId
import me.proton.core.drive.link.domain.usecase.GetLink
import me.proton.core.drive.volume.domain.entity.VolumeId
import me.proton.drive.sdk.Uid
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.nio.channels.Channels
import javax.inject.Inject
import me.proton.drive.sdk.entity.ThumbnailType as SdkThumbnailType

class GetThumbnailSdk @Inject constructor(
    private val protonDriveClientProvider: ProtonDriveClientProvider,
    private val protonPhotosClientProvider: ProtonPhotosClientProvider,
    private val getLink: GetLink,
) {

    suspend operator fun invoke(
        volumeId: VolumeId,
        fileId: FileId,
        thumbnailType: ThumbnailType,
    ): Result<InputStream> = coRunCatching {
        val link = getLink(fileId).toResult().getOrThrow()
        val outputStream = if (link.isPhoto) {
            link.getPhotoThumbnail(volumeId, thumbnailType)
        } else {
            link.getFileThumbnail(volumeId, thumbnailType)
        }
        checkNotNull(outputStream) { "Thumbnail not found for ${fileId.id.logId()}" }
            .toByteArray().inputStream()
    }

    private suspend fun Link.File.getFileThumbnail(
        volumeId: VolumeId,
        thumbnailType: ThumbnailType,
    ): ByteArrayOutputStream? {
        var outputStream: ByteArrayOutputStream? = null
        val fileUid = Uid.makeNodeUid(
            volumeId = volumeId.id,
            nodeId = id.id,
        )
        protonDriveClientProvider
            .getOrCreate(userId)
            .getOrThrow()
            .getThumbnails(
                fileUids = listOf(fileUid),
                type = thumbnailType.toSdkType(),
            ) {
                Channels.newChannel(ByteArrayOutputStream().also { outputStream = it })
            }
        return outputStream
    }

    private suspend fun Link.File.getPhotoThumbnail(
        volumeId: VolumeId,
        thumbnailType: ThumbnailType,
    ): ByteArrayOutputStream? {
        var outputStream: ByteArrayOutputStream? = null
        val photoUid = Uid.makeNodeUid(
            volumeId = volumeId.id,
            nodeId = id.id,
        )
        protonPhotosClientProvider
            .getOrCreate(userId)
            .getOrThrow()
            .getThumbnails(
                photoUids = listOf(photoUid),
                type = thumbnailType.toSdkType(),
            ) {
                Channels.newChannel(ByteArrayOutputStream().also { outputStream = it })
            }
        return outputStream
    }
}

private fun ThumbnailType.toSdkType() = when (this) {
    ThumbnailType.DEFAULT -> SdkThumbnailType.THUMBNAIL
    ThumbnailType.PHOTO -> SdkThumbnailType.PREVIEW
}
