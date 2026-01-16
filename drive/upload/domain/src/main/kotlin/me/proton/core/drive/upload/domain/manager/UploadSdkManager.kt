/*
 * Copyright (c) 2025 Proton AG.
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

package me.proton.core.drive.upload.domain.manager

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import me.proton.core.drive.base.domain.log.LogTag.UploadTag.logTag
import me.proton.core.drive.base.domain.provider.DriveClientProvider
import me.proton.core.drive.linkupload.domain.entity.UploadFileLink
import me.proton.core.drive.upload.domain.exception.InconsistencyException
import me.proton.core.util.kotlin.CoreLogger
import me.proton.drive.sdk.DriveClient
import me.proton.drive.sdk.UploadController
import me.proton.drive.sdk.Uploader
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UploadSdkManager @Inject constructor(
    private val driveClientProvider: DriveClientProvider,
) {

    private data class UploadState(
        val scope: CoroutineScope,
        val mutex: Mutex,
        var uploader: Uploader? = null,
        var controller: UploadController? = null
    )

    private val states = ConcurrentHashMap<Long, UploadState>()

    suspend fun enqueue(uploadFileLink: UploadFileLink, block: suspend (DriveClient) -> Uploader) {
        with(uploadFileLink.state()) {
            mutex.withLock {
                withContext(scope.coroutineContext) {
                    if (uploader == null) {
                        CoreLogger.i(uploadFileLink.id.logTag(), "Creating uploader")
                        val driveClient = driveClientProvider
                            .getOrCreate(uploadFileLink.userId)
                            .getOrThrow()
                        uploader = block(driveClient)
                    }
                }
            }
        }
    }

    suspend fun controller(
        uploadFileLink: UploadFileLink,
        block: suspend (Uploader) -> UploadController
    ): UploadController = with(uploadFileLink.state()) {
        mutex.withLock {
            withContext(scope.coroutineContext) {
                val uploader = uploader
                    ?: error("Upload was not enqueued or cancelled for ${uploadFileLink.id}")

                controller ?: block(uploader).also { controller = it }
            }
        }
    }

    suspend fun close(uploadFileLink: UploadFileLink) {
        val id = uploadFileLink.id
        val state = states.remove(id) ?: return
        with(state) {
            CoreLogger.i(
                id.logTag(), "Closing sdk: " +
                        "uploader: ${uploader != null}, " +
                        "controller: ${controller != null}, " +
                        "scope: ${scope.isActive}"
            )

            mutex.withLock {
                controller?.close()
                uploader?.close()
                scope.cancel()
            }
        }
    }

    suspend fun cancel(uploadFileLink: UploadFileLink) {
        val id = uploadFileLink.id
        val state = states.remove(id) ?: return
        with(state) {
            CoreLogger.i(
                id.logTag(), "Cancelling sdk: " +
                        "uploader: ${uploader != null}, " +
                        "controller: ${controller != null}, " +
                        "scope: ${scope.isActive}"
            )
            mutex.withLock {
                controller?.apply {
                    cancel()
                    dispose()
                    close()
                }
                uploader?.apply {
                    cancel()
                    close()
                }
                scope.cancel()
            }
        }
    }

    private fun UploadFileLink.state(): UploadState =
        states.computeIfAbsent(id) {
            UploadState(
                scope = CoroutineScope(Dispatchers.IO + Job()),
                mutex = Mutex(),
            )
        }

}
