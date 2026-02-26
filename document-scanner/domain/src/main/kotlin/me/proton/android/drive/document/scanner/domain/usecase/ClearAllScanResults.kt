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

import me.proton.android.drive.document.scanner.domain.repository.ScanResultRepository
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.domain.entity.TimestampMs
import me.proton.core.drive.base.domain.entity.TimestampS
import me.proton.core.drive.base.domain.entity.toTimestampS
import me.proton.core.drive.base.domain.extension.getOrNull
import me.proton.core.drive.base.domain.log.LogTag
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.util.kotlin.CoreLogger
import javax.inject.Inject
import kotlin.time.Duration.Companion.hours

class ClearAllScanResults @Inject constructor(
    private val scanResultRepository: ScanResultRepository,
    private val clearScanResult: ClearScanResult,
) {

    suspend operator fun invoke(
        userId: UserId,
        beforeTime: TimestampS = TimestampMs(
            value = System.currentTimeMillis() - DEFAULT_BEFORE_TIME_HOURS.hours.inWholeMilliseconds
        ).toTimestampS()
    ) = coRunCatching {
        do {
            val scanResults = scanResultRepository.getScanResultsOlderThan(
                userId = userId,
                beforeTime = beforeTime,
                count = PAGE_COUNT,
            )
            if (scanResults.isNotEmpty()) {
                CoreLogger.i(
                    tag = LogTag.DOCUMENT_SCANNER,
                    message = "Found ${scanResults.size} scan results to clear"
                )
            }
            scanResults.forEach { scanResult ->
                clearScanResult(userId, scanResult.id).getOrNull(
                    tag = LogTag.DOCUMENT_SCANNER,
                    message = "Failed to clear scan result ${scanResult.id}"
                )
            }
        } while (scanResults.isNotEmpty())
    }

    companion object {
        private const val PAGE_COUNT = 50
        private const val DEFAULT_BEFORE_TIME_HOURS = 24
    }
}
