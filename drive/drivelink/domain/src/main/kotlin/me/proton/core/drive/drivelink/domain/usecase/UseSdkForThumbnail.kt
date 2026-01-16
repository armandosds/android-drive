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

package me.proton.core.drive.drivelink.domain.usecase

import me.proton.core.drive.base.domain.extension.orOwner
import me.proton.core.drive.base.domain.extension.toResult
import me.proton.core.drive.base.domain.provider.ConfigurationProvider
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.drivelink.domain.entity.DriveLink
import me.proton.core.drive.feature.flag.domain.entity.FeatureFlagId.Companion.driveAndroidSDKThumbnailMain
import me.proton.core.drive.feature.flag.domain.entity.FeatureFlagId.Companion.driveAndroidSDKThumbnailPhoto
import me.proton.core.drive.feature.flag.domain.extension.on
import me.proton.core.drive.feature.flag.domain.usecase.GetFeatureFlag
import me.proton.core.drive.link.domain.entity.AlbumId
import me.proton.core.drive.link.domain.entity.LinkId
import me.proton.core.drive.link.domain.extension.shareId
import me.proton.core.drive.link.domain.extension.userId
import me.proton.core.drive.share.domain.usecase.GetShare
import me.proton.core.drive.volume.domain.entity.Volume.Type
import me.proton.core.drive.volume.domain.usecase.GetVolume
import javax.inject.Inject

class UseSdkForThumbnail @Inject constructor(
    private val configurationProvider: ConfigurationProvider,
    private val getDriveLink: GetDriveLink,
    private val getVolume: GetVolume,
    private val getShare: GetShare,
    private val getFeatureFlag: GetFeatureFlag,
) {
    suspend operator fun invoke(linkId: LinkId) = coRunCatching {
        invoke(getDriveLink(linkId).toResult().getOrThrow()).getOrThrow()
    }

    suspend operator fun invoke(driveLink: DriveLink) = coRunCatching {
        configurationProvider.preferSdkForThumbnail
                && driveLink.parentId !is AlbumId
                && driveLink.featureFlag()
    }

    private suspend fun DriveLink.featureFlag(): Boolean = when (volumeType()) {
        null, Type.UNKNOWN -> false
        Type.REGULAR -> getFeatureFlag(driveAndroidSDKThumbnailMain(userId)).on
        Type.PHOTO -> getFeatureFlag(driveAndroidSDKThumbnailPhoto(userId)).on
    }

    private suspend fun DriveLink.volumeType(): Type? = if (sharePermissions.orOwner.isAdmin) {
        getVolume(userId, volumeId).toResult().getOrThrow().type
    } else {
        getShare(shareId).toResult().getOrThrow().volumeType
    }

}
