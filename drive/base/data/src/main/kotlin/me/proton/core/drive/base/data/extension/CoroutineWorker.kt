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

package me.proton.core.drive.base.data.extension

import androidx.annotation.RequiresApi
import androidx.work.CoroutineWorker
import me.proton.core.drive.base.data.entity.WorkInfoStopReason


val CoroutineWorker.stopReasonAsEnum
    @RequiresApi(31)
    get(): WorkInfoStopReason = when (stopReason) {
        -128 -> WorkInfoStopReason.FOREGROUND_SERVICE_TIMEOUT
        -256 -> WorkInfoStopReason.NOT_STOPPED
        -512 -> WorkInfoStopReason.UNKNOWN
        1 -> WorkInfoStopReason.CANCELLED_BY_APP
        2 -> WorkInfoStopReason.PREEMPT
        3 -> WorkInfoStopReason.TIMEOUT
        4 -> WorkInfoStopReason.DEVICE_STATE
        5 -> WorkInfoStopReason.CONSTRAINT_BATTERY_NOT_LOW
        6 -> WorkInfoStopReason.CONSTRAINT_CHARGING
        7 -> WorkInfoStopReason.CONSTRAINT_CONNECTIVITY
        8 -> WorkInfoStopReason.CONSTRAINT_DEVICE_IDLE
        9 -> WorkInfoStopReason.CONSTRAINT_STORAGE_NOT_LOW
        10 -> WorkInfoStopReason.QUOTA
        11 -> WorkInfoStopReason.BACKGROUND_RESTRICTION
        12 -> WorkInfoStopReason.APP_STANDBY
        13 -> WorkInfoStopReason.USER
        14 -> WorkInfoStopReason.SYSTEM_PROCESSING
        15 -> WorkInfoStopReason.ESTIMATED_APP_LAUNCH_TIME_CHANGED
        else -> WorkInfoStopReason.UNKNOWN
    }
