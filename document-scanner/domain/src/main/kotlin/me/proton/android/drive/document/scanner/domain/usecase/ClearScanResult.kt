/*
 * Copyright (c) 2026 Proton AG.
 * This file is part of Proton Drive.
 *
 * Proton Drive is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Proton Drive is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Proton Drive.  If not, see <https://www.gnu.org/licenses/>.
 */

package me.proton.android.drive.document.scanner.domain.usecase

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.proton.android.drive.document.scanner.domain.entity.ScanResult
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.domain.util.coRunCatching
import javax.inject.Inject

class ClearScanResult @Inject constructor(
    private val getScanResult: GetScanResult,
    private val removeScanResult: RemoveScanResult,
) {

    suspend operator fun invoke(userId: UserId, id: Long): Result<Unit> = coRunCatching {
        val scanResult = getScanResult(userId, id).getOrThrow()
        scanResult.deleteAllFiles()
        removeScanResult(userId, id).getOrThrow()
    }

    suspend fun ScanResult.deleteAllFiles() = withContext(Dispatchers.IO) {
        document?.let {
            document.file.deleteRecursively()
        }
        pages.forEach { page -> page.file.deleteRecursively() }
    }
}
