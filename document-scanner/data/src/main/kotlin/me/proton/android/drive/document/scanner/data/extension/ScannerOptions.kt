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

package me.proton.android.drive.document.scanner.data.extension

import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import me.proton.android.drive.document.scanner.domain.entity.ScannerOptions

fun ScannerOptions.OutputFormat.toResultFormat(): Int = when (this) {
    ScannerOptions.OutputFormat.PDF -> GmsDocumentScannerOptions.RESULT_FORMAT_PDF
    ScannerOptions.OutputFormat.JPEG -> GmsDocumentScannerOptions.RESULT_FORMAT_JPEG
}
