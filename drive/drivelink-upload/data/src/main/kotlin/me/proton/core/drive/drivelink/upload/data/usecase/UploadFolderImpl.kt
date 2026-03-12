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

package me.proton.core.drive.drivelink.upload.data.usecase

import androidx.work.ExistingWorkPolicy
import androidx.work.WorkManager
import androidx.work.await
import me.proton.core.drive.base.domain.log.LogTag
import me.proton.core.drive.base.domain.log.logId
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.drivelink.upload.data.worker.UploadFolderWorker
import me.proton.core.drive.drivelink.upload.domain.usecase.UploadFolder
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.util.kotlin.CoreLogger
import javax.inject.Inject

class UploadFolderImpl @Inject constructor(
    private val workManager: WorkManager,
) : UploadFolder {

    override suspend operator fun invoke(
        folderId: FolderId,
        uriString: String,
        shouldBroadcastMessage: Boolean,
    ): Result<Unit> = coRunCatching {
        CoreLogger.i(LogTag.UPLOAD, "Uploading folder from uri: $uriString in ${folderId.id.logId()}")
        workManager.enqueueUniqueWork(
            uniqueWorkName = UploadFolderWorker.getUniqueWorkName(uriString),
            existingWorkPolicy = ExistingWorkPolicy.KEEP,
            request = UploadFolderWorker.getWorkRequest(
                folderId = folderId,
                uriString = uriString,
                shouldBroadcastMessage = shouldBroadcastMessage,
            ),
        ).await()
    }
}
