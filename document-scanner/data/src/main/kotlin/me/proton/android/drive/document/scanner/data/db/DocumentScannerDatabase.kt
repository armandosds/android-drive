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

package me.proton.android.drive.document.scanner.data.db

import androidx.sqlite.db.SupportSQLiteDatabase
import me.proton.android.drive.document.scanner.data.db.dao.ScanResultDao
import me.proton.core.data.room.db.Database
import me.proton.core.data.room.db.migration.DatabaseMigration

interface DocumentScannerDatabase : Database {
    val scanResultDao: ScanResultDao

    companion object {
        val MIGRATION_0 = object : DatabaseMigration {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                        CREATE TABLE IF NOT EXISTS `ScanResultInfoEntity` (
                            `id` INTEGER NOT NULL,
                            `user_id` TEXT NOT NULL,
                            `create_time` INTEGER NOT NULL,
                            `name` TEXT NOT NULL,

                            PRIMARY KEY(`id`),
                            FOREIGN KEY(`user_id`) REFERENCES `AccountEntity`(`userId`)
                            ON UPDATE NO ACTION ON DELETE CASCADE
                        )
                    """.trimIndent()
                )
                database.execSQL(
                    """
                        CREATE TABLE IF NOT EXISTS `ScanResultDocumentEntity` (
                            `id` INTEGER NOT NULL,
                            `type` TEXT NOT NULL,
                            `uri` TEXT NOT NULL,

                            PRIMARY KEY(`type`, `uri`),
                            FOREIGN KEY(`id`) REFERENCES `ScanResultInfoEntity`(`id`)
                            ON UPDATE NO ACTION ON DELETE CASCADE
                        )
                    """.trimIndent()
                )
                database.execSQL(
                    """
                        CREATE TABLE IF NOT EXISTS `ScanResultPageEntity` (
                            `id` INTEGER NOT NULL,
                            `type` TEXT NOT NULL,
                            `uri` TEXT NOT NULL,
                            `index` INTEGER NOT NULL,

                            PRIMARY KEY(`type`, `uri`),
                            FOREIGN KEY(`id`) REFERENCES `ScanResultInfoEntity`(`id`)
                            ON UPDATE NO ACTION ON DELETE CASCADE
                        )
                    """.trimIndent()
                )
                database.execSQL(
                    """
                        CREATE UNIQUE INDEX IF NOT EXISTS `index_ScanResultPageEntity_id_index` ON `ScanResultPageEntity` (`id`, `index`)
                    """.trimIndent()
                )
            }
        }
    }
}
