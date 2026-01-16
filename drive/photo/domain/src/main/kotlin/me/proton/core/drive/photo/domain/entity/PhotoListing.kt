/*
 * Copyright (c) 2023 Proton AG.
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

package me.proton.core.drive.photo.domain.entity

import me.proton.core.drive.base.domain.entity.TimestampS
import me.proton.core.drive.link.domain.entity.AlbumId
import me.proton.core.drive.link.domain.entity.FileId
import me.proton.core.drive.link.domain.entity.PhotoTag
import me.proton.core.drive.volume.domain.entity.VolumeId


sealed interface PhotoListing {
    val volumeId: VolumeId
    val linkId: FileId
    val captureTime: TimestampS
    val nameHash: String?
    val contentHash: String?
    val tag: PhotoTag?
    val relatedPhotos: List<RelatedPhoto>
    val activeRevisionId: String?
    val defaultThumbnailId: String?

    data class Volume(
        override val volumeId: VolumeId,
        override val linkId: FileId,
        override val captureTime: TimestampS,
        override val nameHash: String?,
        override val contentHash: String?,
        override val tag: PhotoTag? = null,
        override val relatedPhotos: List<RelatedPhoto> = emptyList(),
        override val activeRevisionId: String? = null,
        override val defaultThumbnailId: String? = null,
    ) : PhotoListing

    data class Album(
        override val volumeId: VolumeId,
        override val linkId: FileId,
        override val captureTime: TimestampS,
        override val nameHash: String?,
        override val contentHash: String?,
        override val relatedPhotos: List<RelatedPhoto> = emptyList(),
        override val activeRevisionId: String? = null,
        override val defaultThumbnailId: String? = null,
        val albumId: AlbumId,
        val addedTime: TimestampS,
        val isChildOfAlbum: Boolean,
    ) : PhotoListing {

        override val tag: PhotoTag? = null

        enum class SortBy {
            CAPTURED,
            ADDED,
        }
    }

    data class RelatedPhoto(
        val linkId: FileId,
        val captureTime: TimestampS,
        val nameHash: String?,
        val contentHash: String?,
    )
}
