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

package me.proton.android.drive.document.scanner.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import me.proton.android.drive.document.scanner.data.db.entity.ScanResultDocumentEntity
import me.proton.android.drive.document.scanner.data.db.entity.ScanResultEntity
import me.proton.android.drive.document.scanner.data.db.entity.ScanResultInfoEntity
import me.proton.android.drive.document.scanner.data.db.entity.ScanResultPageEntity
import me.proton.core.domain.entity.UserId

@Dao
interface ScanResultDao {

    @Upsert
    suspend fun upsertScanResultInfo(entity: ScanResultInfoEntity): Long

    @Upsert
    suspend fun upsertScanResultDocument(vararg entities: ScanResultDocumentEntity)

    @Upsert
    suspend fun upsertScanResultPage(vararg entities: ScanResultPageEntity)

    @Transaction
    @Query("""
        SELECT * FROM ScanResultInfoEntity
        WHERE user_id = :userId AND id = :id
    """)
    suspend fun getScanResultEntity(userId: UserId, id: Long): ScanResultEntity

    @Query("""
        DELETE FROM ScanResultInfoEntity
        WHERE user_id = :userId AND id = :id
    """)
    suspend fun deleteScanResult(userId: UserId, id: Long)

    @Transaction
    @Query("""
        SELECT * FROM ScanResultInfoEntity
        WHERE user_id = :userId AND create_time < :beforeTime
        ORDER BY create_time DESC
        LIMIT :limit
    """)
    suspend fun getScanResultEntityOlderThan(
        userId: UserId,
        beforeTime: Long,
        limit: Int
    ): List<ScanResultEntity>
}
