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

package me.proton.android.drive.document.scanner.domain.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import me.proton.android.drive.document.scanner.domain.provider.DocumentScannerProvider
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.feature.flag.domain.entity.FeatureFlagId.Companion.driveAndroidDocumentScanner
import me.proton.core.drive.feature.flag.domain.extension.on
import me.proton.core.drive.feature.flag.domain.usecase.GetFeatureFlagFlow
import javax.inject.Inject

class IsScannerAvailable @Inject constructor(
    private val documentScannerProvider: DocumentScannerProvider,
    private val getFeatureFlagFlow: GetFeatureFlagFlow,
) {

    operator fun invoke(userId: UserId): Flow<Boolean> = combine(
        documentScannerProvider.available,
        getFeatureFlagFlow(
            featureFlagId = driveAndroidDocumentScanner(userId),
            emitNotFoundInitially = false,
        )
    ) { isAvailable, documentScannerFeatureFlag ->
        isAvailable && documentScannerFeatureFlag.on
    }
}
