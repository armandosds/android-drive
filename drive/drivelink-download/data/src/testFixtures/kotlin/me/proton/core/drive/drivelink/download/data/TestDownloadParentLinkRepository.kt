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

package me.proton.core.drive.drivelink.download.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.drivelink.download.domain.entity.DownloadParentLink
import me.proton.core.drive.drivelink.download.domain.repository.DownloadParentLinkRepository
import me.proton.core.drive.link.domain.entity.LinkId
import me.proton.core.drive.volume.domain.entity.VolumeId

class TestDownloadParentLinkRepository : DownloadParentLinkRepository {

    val links = mutableListOf<DownloadParentLink>()

    override fun getCountFlow(userId: UserId): Flow<Int> = emptyFlow()

    override suspend fun add(downloadParentLink: DownloadParentLink) {
        links.add(downloadParentLink)
    }

    override suspend fun delete(id: Long) {
    }

    override suspend fun delete(
        volumeId: VolumeId,
        linkId: LinkId
    ) {
    }

    override suspend fun deleteAll(userId: UserId) {
    }

    override suspend fun getAllParentLinks(userId: UserId): List<DownloadParentLink> =
        links
}