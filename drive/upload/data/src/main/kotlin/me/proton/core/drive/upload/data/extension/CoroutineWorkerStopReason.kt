/*
 * Copyright (c) 2026 Proton AG.
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

package me.proton.core.drive.upload.data.extension

import me.proton.core.drive.base.data.entity.WorkInfoStopReason
import me.proton.core.drive.observability.domain.metrics.UploadWorkerCancellationTotal.Reason

fun WorkInfoStopReason.toReason() = when (this) {
    WorkInfoStopReason.FOREGROUND_SERVICE_TIMEOUT -> Reason.timeout
    WorkInfoStopReason.NOT_STOPPED -> Reason.other
    WorkInfoStopReason.UNKNOWN -> Reason.unknown
    WorkInfoStopReason.CANCELLED_BY_APP -> Reason.user
    WorkInfoStopReason.PREEMPT -> Reason.constraint
    WorkInfoStopReason.TIMEOUT -> Reason.timeout
    WorkInfoStopReason.DEVICE_STATE -> Reason.os_limit
    WorkInfoStopReason.CONSTRAINT_BATTERY_NOT_LOW -> Reason.constraint
    WorkInfoStopReason.CONSTRAINT_CHARGING -> Reason.constraint
    WorkInfoStopReason.CONSTRAINT_CONNECTIVITY -> Reason.constraint
    WorkInfoStopReason.CONSTRAINT_DEVICE_IDLE -> Reason.constraint
    WorkInfoStopReason.CONSTRAINT_STORAGE_NOT_LOW -> Reason.constraint
    WorkInfoStopReason.QUOTA -> Reason.os_limit
    WorkInfoStopReason.BACKGROUND_RESTRICTION -> Reason.os_limit
    WorkInfoStopReason.APP_STANDBY -> Reason.os_limit
    WorkInfoStopReason.USER -> Reason.user
    WorkInfoStopReason.SYSTEM_PROCESSING -> Reason.os_limit
    WorkInfoStopReason.ESTIMATED_APP_LAUNCH_TIME_CHANGED -> Reason.os_limit
}
