/*
 * Copyright (c) 2021-2024 Proton AG.
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
package me.proton.core.drive.drivelink.download.data.manager

import androidx.work.WorkManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.domain.entity.Percentage
import me.proton.core.drive.drivelink.domain.entity.DriveLink
import me.proton.core.drive.drivelink.download.data.extension.uniqueWorkName
import me.proton.core.drive.drivelink.download.domain.entity.NetworkType
import me.proton.core.drive.drivelink.download.domain.manager.DownloadWorkManager
import me.proton.core.drive.drivelink.download.domain.usecase.DownloadCleanup
import me.proton.core.drive.drivelink.download.domain.usecase.GetDownloadingDriveLinks
import javax.inject.Inject

class DownloadWorkManagerImpl @Inject constructor(
    private val workManager: WorkManager,
    private val downloadCleanup: DownloadCleanup,
    private val getDownloadingDriveLinks: GetDownloadingDriveLinks,
) : DownloadWorkManager {

    override suspend fun download(
        driveLink: DriveLink,
        retryable: Boolean,
        networkType: NetworkType,
    ) {
        error("This should not be called")
    }

    override fun cancel(driveLink: DriveLink): Unit = with (driveLink) {
        workManager.cancelAllWorkByTag(driveLink.uniqueWorkName)
    }

    override suspend fun cancelAll(userId: UserId) {
        getDownloadingDriveLinks(userId).first().forEach { driveLink ->
            cancel(driveLink)
            downloadCleanup(
                volumeId = driveLink.volumeId,
                linkId = driveLink.id,
            )
        }
    }

    override fun getProgressFlow(driveLink: DriveLink.File): Flow<Percentage>? = null
}
