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

package me.proton.android.drive.document.scanner.domain.entity

import me.proton.core.drive.base.domain.entity.TimestampS
import java.io.File

data class ScanResult(
    val id: Long,
    val createTime: TimestampS,
    val basename: String,
    val document: Output.Document? = null,
    val pages: List<Output.Page> = emptyList(),
) {
    sealed class Output(
        open val format: ScannerOptions.OutputFormat,
        open val file: File,
    ) {

        data class Document(
            override val format: ScannerOptions.OutputFormat,
            override val file: File,
        ) : Output(format, file)

        data class Page(
            val number: Int,
            override val format: ScannerOptions.OutputFormat,
            override val file: File,
        ) : Output(format, file)
    }
}
