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

package me.proton.android.drive.document.scanner.data.repository

import android.net.Uri
import me.proton.android.drive.document.scanner.data.db.DocumentScannerDatabase
import me.proton.android.drive.document.scanner.data.db.entity.ScanResultDocumentEntity
import me.proton.android.drive.document.scanner.data.db.entity.ScanResultInfoEntity
import me.proton.android.drive.document.scanner.data.db.entity.ScanResultPageEntity
import me.proton.android.drive.document.scanner.data.extension.toScanResult
import me.proton.android.drive.document.scanner.domain.entity.ScanResult
import me.proton.android.drive.document.scanner.domain.entity.ScannerOptions
import me.proton.android.drive.document.scanner.domain.repository.ScanResultRepository
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.domain.entity.TimestampS
import javax.inject.Inject

class ScanResultRepositoryImpl @Inject constructor(
    private val db: DocumentScannerDatabase,
) : ScanResultRepository {

    override suspend fun createScanResult(
        userId: UserId,
        createTime: TimestampS,
        basename: String,
        pdfUri: Uri?,
        pageUris: List<Uri>
    ): ScanResult {
        val id = db.inTransaction {
            val id = db.scanResultDao.upsertScanResultInfo(
                ScanResultInfoEntity(
                    id = 0L,
                    userId = userId,
                    createTime = createTime.value,
                    name = basename,
                )
            )
            pdfUri?.let { uri ->
                db.scanResultDao.upsertScanResultDocument(
                    ScanResultDocumentEntity(
                        id = id,
                        type = ScannerOptions.OutputFormat.PDF,
                        uriString = uri.toString(),
                    )
                )
            }
            pageUris.mapIndexed { index, uri ->
                ScanResultPageEntity(
                    id = id,
                    type = ScannerOptions.OutputFormat.JPEG,
                    uriString = uri.toString(),
                    pageNumber = index + 1,
                )
            }.toTypedArray().let {
                db.scanResultDao.upsertScanResultPage(*it)
            }
            id
        }
        return getScanResult(userId, id)
    }

    override suspend fun getScanResult(userId: UserId, id: Long): ScanResult =
        db.scanResultDao.getScanResultEntity(userId, id).toScanResult()

    override suspend fun removeScanResult(userId: UserId, id: Long) =
        db.scanResultDao.deleteScanResult(userId, id)

    override suspend fun getScanResultsOlderThan(
        userId: UserId,
        beforeTime: TimestampS,
        count: Int
    ): List<ScanResult> =
        db.scanResultDao.getScanResultEntityOlderThan(
            userId = userId,
            beforeTime = beforeTime.value,
            limit = count
        ).map { entity -> entity.toScanResult() }
}
