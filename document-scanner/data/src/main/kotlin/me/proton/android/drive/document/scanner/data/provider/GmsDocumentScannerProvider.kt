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

package me.proton.android.drive.document.scanner.data.provider

import android.app.Activity
import android.content.Context
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.mlkit.vision.documentscanner.GmsDocumentScanner
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import me.proton.android.drive.document.scanner.data.extension.setScannerOptions
import me.proton.android.drive.document.scanner.domain.entity.ScannerOptions
import me.proton.android.drive.document.scanner.domain.provider.DocumentScannerProvider
import me.proton.core.drive.base.data.entity.LoggerLevel
import me.proton.core.drive.base.data.extension.log
import me.proton.core.drive.base.domain.log.LogTag
import me.proton.core.util.kotlin.CoreLogger
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@Singleton
class GmsDocumentScannerProvider @Inject constructor(

) : DocumentScannerProvider {
    private var scanner: GmsDocumentScanner? = null
    private val mutex = Mutex()
    private val _available: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val available: StateFlow<Boolean>
        get() = _available

    fun check(activity: Activity, coroutineScope: CoroutineScope) {
        coroutineScope.launch {
            mutex.withLock {
                if (playServicesAvailable(activity).not()) {
                    CoreLogger.i(
                        tag = LogTag.DOCUMENT_SCANNER,
                        message = "Play services are NOT available on a device"
                    )
                    return@launch
                }
                if (scanner != null) {
                    return@launch
                }
                val isAvailable = suspendCoroutine { continuation ->
                    getOrCreateScanner()
                        .getStartScanIntent(activity)
                        .addOnSuccessListener {
                            CoreLogger.i(
                                tag = LogTag.DOCUMENT_SCANNER,
                                message = "Start scan intent was successful"
                            )
                            continuation.resume(true)
                        }
                        .addOnFailureListener { error ->
                            error.log(
                                tag = LogTag.DOCUMENT_SCANNER,
                                message = "Start scan intent failed",
                                level = LoggerLevel.INFO
                            )
                            continuation.resume(false)
                        }
                }
                _available.value = isAvailable
            }
        }
    }

    private fun playServicesAvailable(context: Context): Boolean =
        GoogleApiAvailability
            .getInstance()
            .isGooglePlayServicesAvailable(context) == ConnectionResult.SUCCESS

    private fun getOrCreateScanner(): GmsDocumentScanner =
        scanner ?: createScanner().also { scanner = it }

    private fun createScanner() = GmsDocumentScanning.getClient(
        GmsDocumentScannerOptions.Builder().apply {
            setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL)
            setGalleryImportAllowed(false)
            setScannerOptions(ScannerOptions.default)
        }.build()
    )
}
