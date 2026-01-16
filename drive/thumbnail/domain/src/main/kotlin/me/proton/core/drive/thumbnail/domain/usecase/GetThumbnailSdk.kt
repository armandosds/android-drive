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

import me.proton.core.drive.base.domain.log.logId
import me.proton.core.drive.base.domain.provider.DriveClientProvider
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.file.base.domain.entity.ThumbnailType
import me.proton.core.drive.link.domain.entity.FileId
import me.proton.core.drive.link.domain.extension.userId
import me.proton.core.drive.volume.domain.entity.VolumeId
import me.proton.drive.sdk.Uid
import java.io.ByteArrayOutputStream
import java.io.InputStream
import javax.inject.Inject
import me.proton.drive.sdk.entity.ThumbnailType as SdkThumbnailType

class GetThumbnailSdk @Inject constructor(
    private val driveClientProvider: DriveClientProvider,
) {

    suspend operator fun invoke(
        volumeId: VolumeId,
        fileId: FileId,
        thumbnailType: ThumbnailType,
    ): Result<InputStream> = coRunCatching {
        val client = driveClientProvider.getOrCreate(fileId.userId).getOrThrow()
        var outputStream: ByteArrayOutputStream? = null
        val fileUid = Uid.makeNodeUid(
            volumeId = volumeId.id,
            nodeId = fileId.id
        )
        client.getThumbnails(listOf(fileUid), thumbnailType.toSdkType()) {
            ByteArrayOutputStream().also { outputStream = it }
        }
        checkNotNull(outputStream) { "Thumbnail not found for ${fileId.id.logId()}" }
            .toByteArray().inputStream()
    }
}

private fun ThumbnailType.toSdkType() = when (this) {
    ThumbnailType.DEFAULT -> SdkThumbnailType.THUMBNAIL
    ThumbnailType.PHOTO -> SdkThumbnailType.PREVIEW
}
