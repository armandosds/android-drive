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

package me.proton.android.drive.entity

import android.content.Intent
import android.graphics.drawable.Icon

data class DynamicShortcut(
    val id: ID,
    val shortLabel: String,
    val longLabel: String,
    val icon: Icon,
    val intent: Intent,
) {
    enum class ID {
        DYNAMIC_SCAN_DOCUMENT,
        DYNAMIC_FILES,
        DYNAMIC_PHOTOS,
        DYNAMIC_COMPUTERS,
        DYNAMIC_SHARED,
    }
}
