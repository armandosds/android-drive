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

package me.proton.android.drive.document.scanner.presentation.component

import android.content.ActivityNotFoundException
import androidx.activity.compose.LocalActivity
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import kotlinx.coroutines.launch
import me.proton.android.drive.document.scanner.data.extension.setScannerOptions
import me.proton.android.drive.document.scanner.domain.entity.ScannerOptions
import me.proton.core.compose.activity.rememberLauncher

fun interface DocumentScannerLauncher {
    fun launchWithNotFound(onNotFound: () -> Unit)
}

@Composable
fun rememberDocumentScannerLauncher(
    scannerOptions: ScannerOptions? = null,
    onResult: (GmsDocumentScanningResult?) -> Unit,
    onError: (Throwable) -> Unit,
): DocumentScannerLauncher {
    val activityLauncher = rememberLauncher(
        contracts = ActivityResultContracts.StartIntentSenderForResult(),
        onResult = { result ->
            onResult(
                GmsDocumentScanningResult.fromActivityResultIntent(result.data)
            )
        },
    )
    val activity = LocalActivity.current
    val coroutineScope = rememberCoroutineScope()
    val currentScannerOptions by rememberUpdatedState(scannerOptions)

    return remember(activity, activityLauncher) {
        DocumentScannerLauncher { onNotFound ->
            val scannerOptionsSnapshot = currentScannerOptions
            if (activity == null) {
                onError(RuntimeException("Activity is not attached"))
            } else {
                coroutineScope.launch {
                    GmsDocumentScanning.getClient(
                        GmsDocumentScannerOptions.Builder().apply {
                            setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL)
                            setGalleryImportAllowed(false)
                            setScannerOptions(scannerOptionsSnapshot ?: ScannerOptions.default)
                        }.build()
                    )
                        .getStartScanIntent(activity)
                        .addOnSuccessListener { intentSender ->
                            try {
                                activityLauncher.launch(IntentSenderRequest.Builder(intentSender).build())
                            } catch (_: ActivityNotFoundException) {
                                onNotFound()
                            } catch (e: Exception) {
                                onError(e)
                            }
                        }
                        .addOnFailureListener { error ->
                            onError(error)
                        }
                }
            }
        }
    }
}
