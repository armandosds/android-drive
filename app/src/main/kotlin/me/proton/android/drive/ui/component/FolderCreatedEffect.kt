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

package me.proton.android.drive.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.drive.link.domain.entity.LinkId
import kotlin.time.Duration.Companion.seconds

class FolderHighlightState {
    var scrollToId by mutableStateOf<LinkId?>(null)
    var highlightedId by mutableStateOf<LinkId?>(null)

    fun onScrollCompleted() {
        highlightedId = scrollToId
        scrollToId = null
    }
}

@Composable
fun rememberFolderHighlightState(): FolderHighlightState {
    val state = remember { FolderHighlightState() }
    LaunchedEffect(state.highlightedId) {
        if (state.highlightedId != null) {
            delay(2.seconds)
            state.highlightedId = null
        }
    }
    return state
}

@Composable
fun FolderCreatedEffect(
    flow: Flow<FolderId>,
    onConsumed: () -> Unit,
    action: (FolderId) -> Unit,
) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            flow.collect { folderId ->
                onConsumed()
                action(folderId)
            }
        }
    }
}
