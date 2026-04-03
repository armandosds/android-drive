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

package me.proton.android.drive.usecase

import android.content.Context
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import dagger.hilt.android.qualifiers.ApplicationContext
import me.proton.android.drive.entity.DynamicShortcut
import me.proton.core.drive.base.domain.util.coRunCatching
import javax.inject.Inject

class UpdateDynamicShortcuts @Inject constructor(
    @param:ApplicationContext private val appContext: Context,
    private val shortcutManager: ShortcutManager,
) {

    operator fun invoke(dynamicShortcuts: List<DynamicShortcut>) = coRunCatching {
        val allShortcutIds = DynamicShortcut.ID.entries.toList()
        shortcutManager.removeDynamicShortcuts(allShortcutIds.map { it.name })
        shortcutManager.addDynamicShortcuts(dynamicShortcuts)
    }

    private fun ShortcutManager.addDynamicShortcuts(
        dynamicShortcuts: List<DynamicShortcut>
    ): Boolean = addDynamicShortcuts(
        dynamicShortcuts.mapIndexed { index, shortcut ->
            ShortcutInfo.Builder(appContext, shortcut.id.name)
                .setRank(index)
                .setShortLabel(shortcut.shortLabel)
                .setLongLabel(shortcut.longLabel)
                .setIcon(shortcut.icon)
                .setIntent(shortcut.intent)
                .build()
        }
    )
}
