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

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import me.proton.core.drive.base.domain.log.LogTag
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.util.kotlin.CoreLogger
import me.proton.drive.sdk.LoggerProvider
import me.proton.drive.sdk.ProtonDriveSdk
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GetOrCreateSdkLoggerProvider @Inject constructor() {
    private var loggerProvider: LoggerProvider? = null
    private val mutex = Mutex()

    suspend operator fun invoke(): Result<LoggerProvider> = coRunCatching {
        mutex.withLock {
            if (loggerProvider == null) {
                loggerProvider = createSdkLoggerProvider()
                CoreLogger.d(LogTag.DRIVE_SDK, "Created logger provider")
            }
            requireNotNull(loggerProvider)
        }
    }

    private suspend fun createSdkLoggerProvider() =
        ProtonDriveSdk.loggerProvider { level, category, message ->
            val shortCategory = category.split(".").lastOrNull().orEmpty()
            level.log("drive.sdk.$shortCategory", message)
        }

    private val LoggerProvider.Level.log: (String, String) -> Unit get() = when (this) {
        LoggerProvider.Level.VERBOSE -> CoreLogger::v
        LoggerProvider.Level.DEBUG -> CoreLogger::d
        LoggerProvider.Level.INFO -> CoreLogger::i
        LoggerProvider.Level.WARN -> CoreLogger::w
        LoggerProvider.Level.ERROR -> CoreLogger::e
    }
}
