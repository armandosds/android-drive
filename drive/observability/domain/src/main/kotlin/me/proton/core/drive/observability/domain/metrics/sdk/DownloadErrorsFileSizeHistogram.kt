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

package me.proton.core.drive.observability.domain.metrics.sdk

import io.swagger.v3.oas.annotations.media.Schema
import kotlinx.serialization.Required
import kotlinx.serialization.Serializable
import me.proton.core.drive.observability.domain.metrics.common.NoLabels
import me.proton.core.drive.observability.domain.metrics.sdk.DownloadErrorsFileSizeHistogram.Companion.SCHEMA_ID
import me.proton.core.observability.domain.entity.SchemaId

@Serializable
@Schema(description = "Total file size of the errored download")
@SchemaId(SCHEMA_ID)
data class DownloadErrorsFileSizeHistogram(
    @Required override val Labels: NoLabels = NoLabels,
    @Required override val Value: Long,
) : DriveObservabilityData() {

    companion object {
        const val SCHEMA_ID = "https://proton.me/drive_sdk_download_errors_file_size_histogram_v1.schema.json"
    }
}
