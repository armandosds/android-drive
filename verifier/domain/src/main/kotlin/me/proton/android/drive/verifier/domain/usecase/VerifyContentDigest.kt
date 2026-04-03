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

package me.proton.android.drive.verifier.domain.usecase

import me.proton.android.drive.verifier.domain.exception.ContentDigestVerifierException
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.base.domain.usecase.GetContentDigest
import java.io.File
import javax.inject.Inject

class VerifyContentDigest @Inject constructor(
    private val getContentDigest: GetContentDigest,
) {

    suspend operator fun invoke(
        claimed: String,
        file: File,
    ): Result<Unit> = coRunCatching {
        verifyInput(claimed, file)
        verifyContentDigest(
            claimed = claimed,
            actual = getContentDigest(file).getOrThrow()
        )
    }

    private fun verifyInput(claimed: String, file: File) {
        if (claimed.isEmpty()) {
            throw ContentDigestVerifierException.InvalidClaimed(
                message = "Claimed content digest is empty",
            )
        }
        if (claimed.isBlank()) {
            throw ContentDigestVerifierException.InvalidClaimed(
                message = "Claimed content digest is blank",
            )
        }
        if (file.exists().not()) {
            throw ContentDigestVerifierException.InvalidFile(
                message = "File does not exist",
            )
        }
        if (file.length() == 0L) {
            throw ContentDigestVerifierException.InvalidFile(
                message = "File length is zero",
            )
        }
    }

    private fun verifyContentDigest(claimed: String, actual: String) {
        if (!claimed.equals(other = actual, ignoreCase = true)) {
            throw ContentDigestVerifierException.Mismatch(
                message = "Claimed=${claimed.take(MAX_DIGEST_CHARS)}, actual=${actual.take(MAX_DIGEST_CHARS)}",
            )
        }
    }

    companion object {
        private const val MAX_DIGEST_CHARS = 4
    }
}
