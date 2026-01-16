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

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import me.proton.core.drive.base.domain.log.LogTag.DRIVE_SDK
import me.proton.core.util.kotlin.CoreLogger
import me.proton.drive.sdk.MetricCallback
import me.proton.drive.sdk.telemetry.ApiRetrySucceededEvent
import me.proton.drive.sdk.telemetry.BlockVerificationErrorEvent
import me.proton.drive.sdk.telemetry.DecryptionErrorEvent
import me.proton.drive.sdk.telemetry.DownloadEvent
import me.proton.drive.sdk.telemetry.UploadEvent
import me.proton.drive.sdk.telemetry.VerificationErrorEvent
import kotlin.coroutines.CoroutineContext

class DriveMetricCallback(
    private val sdkMetricsNotifier: SdkMetricsNotifier,
    coroutineContext: CoroutineContext,
) : MetricCallback {
    private val coroutineScope = CoroutineScope(
        context = coroutineContext + Job(parent = coroutineContext[Job])
    )

    override fun onApiRetrySucceededEvent(event: ApiRetrySucceededEvent) = log(event)

    override fun onBlockVerificationErrorEvent(event: BlockVerificationErrorEvent) = log(event) {
        sdkMetricsNotifier(blockVerificationErrorEvent = event)
    }

    override fun onDecryptionErrorEvent(event: DecryptionErrorEvent) = log(event) {
        sdkMetricsNotifier(decryptionErrorEvent = event)
    }

    override fun onDownloadEvent(event: DownloadEvent) = log(event) {
        sdkMetricsNotifier(downloadEvent = event)
    }

    override fun onUploadEvent(event: UploadEvent) = log(event) {
        sdkMetricsNotifier(uploadEvent = event)
    }

    override fun onVerificationErrorEvent(event: VerificationErrorEvent) = log(event) {
        sdkMetricsNotifier(verificationErrorEvent = event)
    }

    private fun log(event: Any, block: (suspend () -> Unit)? = null) {
        coroutineScope.launch {
            CoreLogger.d(tag = DRIVE_SDK, message = "metric: $event")
            block?.invoke()
        }
    }
}
