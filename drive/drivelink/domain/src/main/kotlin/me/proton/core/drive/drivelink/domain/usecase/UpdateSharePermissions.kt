/*
 * Copyright (c) 2024 Proton AG.
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

import me.proton.core.drive.base.domain.entity.Permissions
import me.proton.core.drive.base.domain.extension.getOrNull
import me.proton.core.drive.base.domain.extension.toResult
import me.proton.core.drive.base.domain.log.LogTag.SHARING
import me.proton.core.drive.drivelink.domain.entity.DriveLink
import me.proton.core.drive.link.domain.extension.shareId
import me.proton.core.drive.link.domain.extension.userId
import me.proton.core.drive.linknode.domain.usecase.GetLinkAncestors
import me.proton.core.drive.share.crypto.domain.usecase.GetPhotoShare
import me.proton.core.drive.share.domain.entity.Share
import me.proton.core.drive.share.domain.usecase.GetHighestSharePermissions
import me.proton.core.drive.share.domain.usecase.GetMainShare
import me.proton.core.drive.share.domain.usecase.GetShare
import javax.inject.Inject

class UpdateSharePermissions @Inject constructor(
    private val getMainShare: GetMainShare,
    private val getPhotoShare: GetPhotoShare,
    private val getShare: GetShare,
    private val getLinkAncestors: GetLinkAncestors,
    private val getHighestSharePermissions: GetHighestSharePermissions,
) {
    suspend operator fun invoke(driveLink: DriveLink): DriveLink {
        val share = getShare(driveLink.shareId).toResult().getOrNull(SHARING, "Cannot find share")
            ?: return driveLink
        val sharePermissions = when (share.type) {
            Share.Type.STANDARD -> getPermissions(share, driveLink)
            else -> Permissions.owner
        }
        return when (driveLink) {
            is DriveLink.Folder -> driveLink.copy(sharePermissions = sharePermissions)
            is DriveLink.File -> driveLink.copy(sharePermissions = sharePermissions)
            is DriveLink.Album -> driveLink.copy(sharePermissions = sharePermissions)
        }
    }

    private suspend fun getPermissions(
        share: Share,
        driveLink: DriveLink,
    ): Permissions = checkMainShare(share, driveLink)
        ?: checkPhotoShare(share, driveLink)
        ?: checkLinks(driveLink)

    private suspend fun checkLinks(driveLink: DriveLink): Permissions =
        getLinkAncestors(driveLink.id).toResult().getOrNull()?.mapNotNull { link ->
            link.sharingDetails?.shareId
        }.orEmpty().let { shareIds ->
            getHighestSharePermissions(shareIds).getOrNull(SHARING, "Cannot get share permissions")
        } ?: Permissions.viewer

    private suspend fun checkPhotoShare(
        share: Share,
        driveLink: DriveLink
    ): Permissions? {
        val photoShare = getPhotoShare(driveLink.userId)
            .toResult()
            .getOrNull(SHARING, "Cannot get photo share")
        return Permissions.owner.takeIf { photoShare?.volumeId == share.volumeId }
    }

    private suspend fun checkMainShare(
        share: Share,
        driveLink: DriveLink
    ): Permissions? {
        val mainShare = getMainShare(driveLink.userId)
            .toResult()
            .getOrNull(SHARING, "Cannot get main share")
        return Permissions.owner.takeIf { mainShare?.volumeId == share.volumeId }
    }
}
