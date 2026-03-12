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

import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.data.datastore.GetUserDataStore
import me.proton.core.drive.base.data.extension.get
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.feature.flag.domain.usecase.IsSpringSalePromoEnabled
import javax.inject.Inject
import me.proton.core.drive.base.data.datastore.GetUserDataStore.Keys.springSalePromo2026LastShown
import me.proton.core.drive.base.domain.entity.TimestampMs
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

class ShouldShowSpringSalePromo @Inject constructor(
    private val isSpringSalePromoEnabled: IsSpringSalePromoEnabled,
    private val getUserDataStore: GetUserDataStore,
) {
    private val timeframes = listOf(
        Timeframe(
            begin = LocalDateTime.of(2026, 3, 16, 12, 0, 0),
            end = LocalDateTime.of(2026, 3, 24, 12, 0, 0),
        ),
        Timeframe(
            begin = LocalDateTime.of(2026, 3, 26, 12, 0, 0),
            end = LocalDateTime.of(2026, 4, 2, 23, 59, 59),
        ),
    )

    suspend operator fun invoke(
        userId: UserId,
        now: LocalDateTime = LocalDateTime.now()
    ): Result<Boolean> = coRunCatching {
        isSpringSalePromoEnabled(userId) && isWithinTimeframe(now) && wasNotAlreadyShown(userId, now)
    }

    private suspend fun wasNotAlreadyShown(userId: UserId, now: LocalDateTime): Boolean =
        timeframes.firstOrNull { timeframe -> now.isWithinTimeframe(timeframe) }?.let { currentTimeframe ->
            getLastShown(userId)
                ?.toLocalDateTime()
                ?.isNotWithinTimeframe(currentTimeframe)
                ?: true
        } ?: true

    private fun isWithinTimeframe(now: LocalDateTime): Boolean = timeframes.any { timeframe ->
        now.isWithinTimeframe(timeframe)
    }

    private fun LocalDateTime.isWithinTimeframe(timeframe: Timeframe): Boolean =
        isAfter(timeframe.begin) && isBefore(timeframe.end)

    private fun LocalDateTime.isNotWithinTimeframe(timeframe: Timeframe): Boolean = !this.isWithinTimeframe(timeframe)

    private fun TimestampMs.toLocalDateTime(): LocalDateTime =
        Instant.ofEpochMilli(value)
            .atZone(ZoneId.systemDefault())
            .toLocalDateTime()

    private suspend fun getLastShown(userId: UserId): TimestampMs? =
        getUserDataStore(userId).get(springSalePromo2026LastShown)?.let { TimestampMs(it) }

    data class Timeframe(
        val begin: LocalDateTime,
        val end: LocalDateTime,
    )
}
