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

package me.proton.core.drive.feature.flag.domain.usecase

import me.proton.core.domain.entity.UserId
import me.proton.core.drive.feature.flag.domain.entity.FeatureFlagId
import me.proton.core.drive.feature.flag.domain.extension.on
import me.proton.core.user.domain.UserManager
import me.proton.core.user.domain.extension.hasSubscription
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import javax.inject.Inject

class IsSpringSalePromoEnabled @Inject constructor(
    private val getFeatureFlag: GetFeatureFlag,
    private val userManager: UserManager,
) {
    // Time limit: April 2nd, 2026 at 11:00 UTC (12:00 CET)
    private val limitUtc = ZonedDateTime.of(
        LocalDateTime.of(2026, 4, 2, 11, 0, 0),
        ZoneId.of("UTC")
    )

    private val nowUtc: ZonedDateTime get() = ZonedDateTime.now(ZoneId.of("UTC"))

    suspend operator fun invoke(userId: UserId): Boolean =
        nowUtc.isBefore(limitUtc) && isSpringSalePromoEnabled(userId) && isUserWithoutProtonSubscription(userId)

    private suspend fun isSpringSalePromoEnabled(userId: UserId): Boolean =
        getFeatureFlag(FeatureFlagId.driveAndroidSpringSale2026(userId)).on

    private suspend fun isUserWithoutProtonSubscription(userId: UserId): Boolean =
        userManager.getUser(userId).hasSubscription().not()
}
