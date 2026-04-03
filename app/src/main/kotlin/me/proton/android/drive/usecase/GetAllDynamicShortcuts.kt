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
import android.content.Intent
import dagger.hilt.android.qualifiers.ApplicationContext
import me.proton.android.drive.entity.DynamicShortcut
import javax.inject.Inject
import android.graphics.drawable.Icon
import android.net.Uri
import androidx.core.net.toUri
import me.proton.android.drive.R
import me.proton.android.drive.extension.deepLinkBaseUrl
import me.proton.android.drive.ui.navigation.Screen
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.domain.extension.toResult
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.drive.link.domain.extension.rootFolderId
import me.proton.core.drive.share.domain.usecase.GetMainShare
import me.proton.core.drive.i18n.R as I18N


class GetAllDynamicShortcuts @Inject constructor(
    @param:ApplicationContext private val appContext: Context,
    private val getMainShare: GetMainShare
) {

    suspend operator fun invoke(userId: UserId): List<DynamicShortcut> = listOfNotNull(
        getMainShare(userId).toResult().getOrNull()?.let { mainShare ->
            scanDocument(mainShare.rootFolderId)
        },
        files,
        photos,
        computers,
        shared,
    )

    private fun scanDocument(folderId: FolderId): DynamicShortcut = DynamicShortcut(
        id = DynamicShortcut.ID.DYNAMIC_SCAN_DOCUMENT,
        shortLabel = appContext.getString(I18N.string.scan_document_shortcut_short_label),
        longLabel = appContext.getString(I18N.string.scan_document_shortcut_long_label),
        icon = Icon.createWithResource(appContext, R.mipmap.ic_shortcut_scan_document),
        intent = actionView(buildScanDocumentUrl(Screen.Home.TAB_FILES, folderId).toUri()),
    )

    private val files: DynamicShortcut get() = DynamicShortcut(
        id = DynamicShortcut.ID.DYNAMIC_FILES,
        shortLabel = appContext.getString(I18N.string.title_files),
        longLabel = appContext.getString(I18N.string.title_files),
        icon = Icon.createWithResource(appContext, R.mipmap.ic_shortcut_files),
        intent = actionView(buildTabUrl(Screen.Home.TAB_FILES).toUri()),
    )

    private val photos: DynamicShortcut get() = DynamicShortcut(
        id = DynamicShortcut.ID.DYNAMIC_PHOTOS,
        shortLabel = appContext.getString(I18N.string.photos_title),
        longLabel = appContext.getString(I18N.string.photos_title),
        icon = Icon.createWithResource(appContext, R.mipmap.ic_shortcut_photos),
        intent = actionView(buildTabUrl(Screen.Home.TAB_PHOTOS).toUri()),
    )

    private val computers: DynamicShortcut get() = DynamicShortcut(
        id = DynamicShortcut.ID.DYNAMIC_COMPUTERS,
        shortLabel = appContext.getString(I18N.string.computers_title),
        longLabel = appContext.getString(I18N.string.computers_title),
        icon = Icon.createWithResource(appContext, R.mipmap.ic_shortcut_computers),
        intent = actionView(buildTabUrl(Screen.Home.TAB_COMPUTERS).toUri()),
    )

    private val shared: DynamicShortcut get() = DynamicShortcut(
        id = DynamicShortcut.ID.DYNAMIC_SHARED,
        shortLabel = appContext.getString(I18N.string.title_shared),
        longLabel = appContext.getString(I18N.string.title_shared),
        icon = Icon.createWithResource(appContext, R.mipmap.ic_shortcut_shared),
        intent = actionView(buildTabUrl(Screen.Home.TAB_SHARED_TABS).toUri()),
    )

    private fun actionView(uri: Uri): Intent = Intent(
        Intent.ACTION_VIEW,
        uri,
    )

    private fun buildTabUrl(tab: String): String = buildString {
        append(appContext.deepLinkBaseUrl)
        append("/launcher")
        append("?redirection=$tab")
    }

    private fun buildScanDocumentUrl(tab: String, folderId: FolderId): String = buildString {
        append(buildTabUrl(tab))
        append("&action=${Screen.Launcher.ACTION_SCAN_DOCUMENT}")
        append("&shareId=${folderId.shareId.id}")
        append("&folderId=${folderId.id}")
    }
}
