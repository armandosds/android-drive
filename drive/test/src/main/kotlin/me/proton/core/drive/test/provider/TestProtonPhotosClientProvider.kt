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

package me.proton.core.drive.test.provider

import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.domain.provider.ProtonPhotosClientProvider
import me.proton.drive.sdk.ProtonPhotosClient
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TestProtonPhotosClientProvider @Inject constructor() : ProtonPhotosClientProvider {

    override suspend fun getOrCreate(userId: UserId): Result<ProtonPhotosClient> {
        error("Cannot use SDK in test")
    }
}
