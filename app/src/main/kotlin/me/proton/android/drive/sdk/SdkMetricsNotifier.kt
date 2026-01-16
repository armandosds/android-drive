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

package me.proton.android.drive.sdk

import me.proton.drive.sdk.telemetry.BlockVerificationErrorEvent
import me.proton.drive.sdk.telemetry.DecryptionErrorEvent
import me.proton.drive.sdk.telemetry.DownloadEvent
import me.proton.drive.sdk.telemetry.UploadEvent
import me.proton.drive.sdk.telemetry.VerificationErrorEvent
import javax.inject.Inject

class SdkMetricsNotifier @Inject constructor(
    private val sdkDownloadMetricsNotifier: SdkDownloadMetricsNotifier,
    private val sdkIntegrityMetricsNotifier: SdkIntegrityMetricsNotifier,
    private val sdkUploadMetricsNotifier: SdkUploadMetricsNotifier,
) {

    suspend operator fun invoke(downloadEvent: DownloadEvent) =
        sdkDownloadMetricsNotifier(downloadEvent = downloadEvent)

    suspend operator fun invoke(decryptionErrorEvent: DecryptionErrorEvent) =
        sdkIntegrityMetricsNotifier(decryptionErrorEvent = decryptionErrorEvent)

    suspend operator fun invoke(verificationErrorEvent: VerificationErrorEvent) =
        sdkIntegrityMetricsNotifier(verificationErrorEvent = verificationErrorEvent)

    suspend operator fun invoke(blockVerificationErrorEvent: BlockVerificationErrorEvent) =
        sdkIntegrityMetricsNotifier(blockVerificationErrorEvent = blockVerificationErrorEvent)

    suspend operator fun invoke(uploadEvent: UploadEvent) =
        sdkUploadMetricsNotifier(uploadEvent = uploadEvent)
}
