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

package me.proton.core.drive.folder.create.domain.provider

import me.proton.core.drive.folder.create.domain.entity.OpenFolderExtra
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.drive.messagequeue.domain.ActionProvider
import java.io.Serializable
import javax.inject.Inject
import javax.inject.Singleton
import me.proton.core.drive.i18n.R as I18N

@Singleton
class OpenFolderActionProvider @Inject constructor() : ActionProvider {
    private val lock = Any()
    private var owner: Any? = null
    private var navigateToFolder: ((FolderId, String?) -> Unit)? = null

    fun register(navigateToFolder: (FolderId, String?) -> Unit): Any = Any().apply {
        synchronized(lock) {
            owner = this
            this@OpenFolderActionProvider.navigateToFolder = navigateToFolder
        }
    }

    fun unregister(token: Any) {
        synchronized(lock) {
            if (owner === token) {
                owner = null
                navigateToFolder = null
            }
        }
    }

    override fun provideAction(
        extra: Serializable?
    ): ActionProvider.Action? = navigateToFolder?.let { navigate ->
        when (extra) {
            is OpenFolderExtra -> ActionProvider.Action(
                label = I18N.string.common_open_action,
                onAction = { navigate(extra.folderId, extra.name) },
            )
            else -> null
        }
    }
}
