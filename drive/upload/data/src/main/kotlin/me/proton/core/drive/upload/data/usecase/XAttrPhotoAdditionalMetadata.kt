/*
 * Copyright (c) 2026 Proton AG.
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

package me.proton.core.drive.upload.data.usecase

import me.proton.core.drive.base.data.api.Dto
import me.proton.core.drive.base.domain.formatter.DateTimeFormatter
import me.proton.core.drive.file.base.domain.entity.XAttr
import me.proton.core.drive.file.base.domain.extension.captureTime
import me.proton.core.drive.file.base.domain.extension.subjectCoordinates
import me.proton.core.drive.linkupload.domain.entity.UploadFileLink
import me.proton.core.drive.upload.domain.usecase.PhotoAdditionalMetadata
import me.proton.core.util.kotlin.serialize
import javax.inject.Inject

class XAttrPhotoAdditionalMetadata @Inject constructor(
    private val dateTimeFormatter: DateTimeFormatter,
) : PhotoAdditionalMetadata {
    override fun invoke(uploadFileLink: UploadFileLink) = with(uploadFileLink) {
        listOfNotNull(
            mediaResolution?.let { mediaResolution ->
                Dto.MEDIA to XAttr.Media(
                    width = mediaResolution.width,
                    height = mediaResolution.height,
                    duration = mediaDuration?.inWholeSeconds?.toDouble()
                ).serialize().toByteArray()
            },

            location?.let { location ->
                Dto.LOCATION to XAttr.Location(
                    latitude = location.latitude,
                    longitude = location.longitude,
                ).serialize().toByteArray()
            },

            cameraExifTags?.let { cameraExifTags ->
                fileCreationDateTime?.captureTime(dateTimeFormatter)?.let { captureTime ->
                    Dto.CAMERA to XAttr.Camera(
                        captureTime = captureTime,
                        device = cameraExifTags.model,
                        orientation = cameraExifTags.orientation,
                        subjectCoordinates = cameraExifTags.subjectCoordinates,
                    ).serialize().toByteArray()
                }
            },

            ).toMap()
    }
}
