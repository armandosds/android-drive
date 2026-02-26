/*
 * Copyright (c) 2026 Proton AG.
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

package me.proton.core.drive.observability.data.extension

import me.proton.core.drive.base.domain.entity.Bytes
import me.proton.core.drive.observability.domain.metrics.StringBuckets

fun Bytes.toStringBuckets(): StringBuckets = when {
    value < (1L shl 10) -> "2**10"
    value < (1L shl 20) -> "2**20"
    value < (1L shl 22) -> "2**22"
    value < (1L shl 25) -> "2**25"
    value < (1L shl 30) -> "2**30"
    else -> "xxxxl"
}
