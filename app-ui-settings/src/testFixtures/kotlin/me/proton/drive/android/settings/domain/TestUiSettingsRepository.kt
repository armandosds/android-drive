/*
 * Copyright (c) 2025 Proton AG.
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

package me.proton.drive.android.settings.domain

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.domain.entity.TimestampS
import me.proton.drive.android.settings.domain.entity.HomeTab
import me.proton.drive.android.settings.domain.entity.LayoutType
import me.proton.drive.android.settings.domain.entity.ThemeStyle
import me.proton.drive.android.settings.domain.entity.UiSettings
import me.proton.drive.android.settings.domain.entity.WhatsNewKey

class TestUiSettingsRepository : UiSettingsRepository {

    var onboardingShown: Long = 0L
    var whatsNew: Long = 0L
    var ratingBooster: Long = 0L

    override fun getUiSettingsFlow(userId: UserId): Flow<UiSettings> = emptyFlow()

    override suspend fun updateLayoutType(
        userId: UserId,
        layoutType: LayoutType
    ) {
    }

    override suspend fun updateThemeStyle(
        userId: UserId,
        themeStyle: ThemeStyle
    ) {
    }

    override suspend fun updateHomeTab(
        userId: UserId,
        homeTab: HomeTab
    ) {
    }

    override suspend fun hasShownOnboarding(): Boolean = onboardingShown > 0L

    override suspend fun updateOnboardingShown(timestamp: TimestampS) {
        onboardingShown = timestamp.value
    }

    override suspend fun hasShownWhatsNew(key: WhatsNewKey): Boolean = whatsNew > 0L

    override suspend fun updateWhatsNewShown(
        key: WhatsNewKey,
        timestamp: TimestampS
    ) {
        whatsNew = timestamp.value
    }

    override suspend fun hasShownRatingBooster(): Boolean = ratingBooster > 0L

    override suspend fun updateRatingBoosterShown(timestamp: TimestampS) {
        ratingBooster = timestamp.value
    }

    override suspend fun hasShownOverlay(): Boolean =
        hasShownOnboarding() ||
                hasShownWhatsNew(key = WhatsNewKey.UPLOAD_FOLDER) ||
                hasShownRatingBooster()
}