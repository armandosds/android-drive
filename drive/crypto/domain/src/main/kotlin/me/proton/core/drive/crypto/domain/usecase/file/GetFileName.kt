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

package me.proton.core.drive.crypto.domain.usecase.file

import me.proton.core.drive.base.domain.extension.toResult
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.key.domain.entity.NodeHashKey
import me.proton.core.drive.key.domain.usecase.GetNodeHashKey
import me.proton.core.drive.key.domain.usecase.GetNodeKey
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.drive.link.domain.entity.Link
import me.proton.core.drive.link.domain.usecase.GetLink
import me.proton.core.drive.link.domain.usecase.ValidateLinkName
import me.proton.core.drive.share.domain.entity.Share
import me.proton.core.drive.share.domain.usecase.GetShare
import javax.inject.Inject

class GetFileName @Inject constructor(
    private val getNodeKey: GetNodeKey,
    private val getNodeHashKey: GetNodeHashKey,
    private val validateLinkName: ValidateLinkName,
    private val avoidDuplicateFileName: AvoidDuplicateFileName,
    private val getShare: GetShare,
    private val getLink: GetLink,
) {
    suspend operator fun invoke(name: String, folderId: FolderId) = coRunCatching {
        val folder = getLink(folderId).toResult().getOrThrow()
        val folderKey = getNodeKey(folder).getOrThrow()
        val folderHashKey = getNodeHashKey(folder, folderKey).getOrThrow()
        invoke(
            name = name,
            folder = folder,
            folderHashKey = folderHashKey
        ).getOrThrow()
    }

    suspend operator fun invoke(
        name: String,
        folder: Link.Folder,
        folderHashKey: NodeHashKey,
    ) = coRunCatching {
        if (folder.allowDuplicateFileName()) {
            validateLinkName(name).getOrThrow()
        } else {
            avoidDuplicateFileName(
                fileName = validateLinkName(name).getOrThrow(),
                parentFolderId = folder.id,
                folderHashKey = folderHashKey,
            ).getOrThrow()
        }
    }

    private suspend fun Link.Folder.allowDuplicateFileName() =
        getShare(id.shareId).toResult().getOrThrow().type == Share.Type.PHOTO
}
