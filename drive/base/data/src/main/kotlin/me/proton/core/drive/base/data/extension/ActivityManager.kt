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

package me.proton.core.drive.base.data.extension

import android.app.ActivityManager
import android.os.Process
import me.proton.core.drive.base.data.usecase.GetMemoryInfoImpl

val ActivityManager.logMemoryInfo: String get() = buildString {
    GetMemoryInfoImpl(this@logMemoryInfo).invoke().getOrNull()?.let { memoryInfo ->
        append("Low on memory: ${memoryInfo.isLowOnMemory}. ")
    }
    val rt = Runtime.getRuntime()
    append("JVM heap (MiB): ")
    append("max=${rt.maxMemory() / 1048576}, ")
    append("used=${rt.totalMemory() / 1048576}, ")
    append("free=${rt.freeMemory() / 1048576}. ")
    val pid = Process.myPid()
    val procInfo = getProcessMemoryInfo(intArrayOf(pid))[0]
    procInfo?.let {
        append("PSS (MiB): ")
        append("total=${procInfo.totalPss / 1024}, ")
        append("dalvik=${procInfo.dalvikPss / 1024}, ")
        append("native=${procInfo.nativePss / 1024}")
    }
}
