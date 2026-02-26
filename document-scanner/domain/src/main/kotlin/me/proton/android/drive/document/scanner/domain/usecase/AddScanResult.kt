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

import android.net.Uri
import me.proton.android.drive.document.scanner.domain.entity.ScanResult
import me.proton.android.drive.document.scanner.domain.repository.ScanResultRepository
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.domain.entity.TimestampS
import me.proton.core.drive.base.domain.util.coRunCatching
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

class AddScanResult @Inject constructor(
    private val scanResultRepository: ScanResultRepository,
) {

    suspend operator fun invoke(
        userId: UserId,
        createTime: TimestampS,
        pdfUri: Uri? = null,
        pageUris: List<Uri> = emptyList(),
    ): Result<ScanResult> = coRunCatching {
        val formatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")
        val localTime = LocalDateTime.ofInstant(
            Instant.ofEpochSecond(createTime.value),
            ZoneId.systemDefault(),
        )
        scanResultRepository.createScanResult(
            userId = userId,
            createTime = createTime,
            basename = "Scan_${localTime.format(formatter)}",
            pdfUri = pdfUri,
            pageUris = pageUris,
        )
    }
}
