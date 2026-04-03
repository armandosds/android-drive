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

package me.proton.core.drive.drivelink.crypto.domain.usecase

import me.proton.core.drive.base.domain.provider.ConfigurationProvider
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.crypto.domain.usecase.DecryptLinkXAttr
import me.proton.core.drive.drivelink.domain.entity.DriveLink
import me.proton.core.drive.file.base.domain.entity.XAttr
import me.proton.core.drive.file.base.domain.extension.toXAttr
import javax.inject.Inject

class GetContentDigest @Inject constructor(
    private val configurationProvider: ConfigurationProvider,
    private val decryptLinkXAttr: DecryptLinkXAttr,
) {

    suspend operator fun invoke(
        driveLink: DriveLink.File
    ): Result<String> = coRunCatching {
        requireNotNull(driveLink.xAttr) { "No XAttr found" }
        invoke(
            xAttr = decryptLinkXAttr(driveLink).getOrThrow().text.toXAttr().getOrThrow()
        ).getOrThrow()
    }

    operator fun invoke(
        xAttr: XAttr,
    ): Result<String> = coRunCatching {
        val digests = requireNotNull(xAttr.common?.digests) { "XAttr does not contain digests" }
        requireNotNull(digests[configurationProvider.contentDigestAlgorithm]) {
            "Digests does not contain content digest algorithm ${configurationProvider.contentDigestAlgorithm}"
        }
    }
}
