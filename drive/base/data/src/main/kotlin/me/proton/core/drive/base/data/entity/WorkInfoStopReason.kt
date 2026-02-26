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

package me.proton.core.drive.base.data.entity

enum class WorkInfoStopReason {
    FOREGROUND_SERVICE_TIMEOUT,
    NOT_STOPPED,
    UNKNOWN,
    CANCELLED_BY_APP,
    PREEMPT,
    TIMEOUT,
    DEVICE_STATE,
    CONSTRAINT_BATTERY_NOT_LOW,
    CONSTRAINT_CHARGING,
    CONSTRAINT_CONNECTIVITY,
    CONSTRAINT_DEVICE_IDLE,
    CONSTRAINT_STORAGE_NOT_LOW,
    QUOTA,
    BACKGROUND_RESTRICTION,
    APP_STANDBY,
    USER,
    SYSTEM_PROCESSING,
    ESTIMATED_APP_LAUNCH_TIME_CHANGED,
}
