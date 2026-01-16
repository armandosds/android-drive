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

package me.proton.core.drive.feature.flag.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import me.proton.core.domain.entity.UserId
import me.proton.core.featureflag.domain.entity.FeatureFlag
import me.proton.core.featureflag.domain.entity.FeatureId
import me.proton.core.featureflag.domain.entity.Scope
import me.proton.core.featureflag.domain.repository.FeatureFlagRepository

class TestCoreFeatureFlagRepository(
    val featureFlags: MutableMap<FeatureId, FeatureFlag> = mutableMapOf()
) : FeatureFlagRepository {

    override suspend fun awaitNotEmptyScope(
        userId: UserId?,
        scope: Scope
    ) {
    }

    override suspend fun get(
        userId: UserId?,
        featureId: FeatureId,
        refresh: Boolean
    ): FeatureFlag? = featureFlags[featureId]

    override suspend fun get(
        userId: UserId?,
        featureIds: Set<FeatureId>,
        refresh: Boolean
    ): List<FeatureFlag> =
        featureIds.mapNotNull { featureFlags[it] }

    override suspend fun getAll(userId: UserId?): List<FeatureFlag> =
        featureFlags.values.toList()

    override fun getValue(
        userId: UserId?,
        featureId: FeatureId
    ): Boolean? =
        featureFlags[featureId]?.value

    override fun observe(
        userId: UserId?,
        featureId: FeatureId,
        refresh: Boolean
    ): Flow<FeatureFlag?> =
        flow {
            featureFlags[featureId]
        }

    override fun observe(
        userId: UserId?,
        featureIds: Set<FeatureId>,
        refresh: Boolean
    ): Flow<List<FeatureFlag>> =
        flow {
            get(userId = userId, featureIds = featureIds, refresh = refresh)
        }

    override fun prefetch(
        userId: UserId?,
        featureIds: Set<FeatureId>
    ) {
    }

    override fun refreshAllOneTime(userId: UserId?) {
    }

    override fun refreshAllPeriodic(
        userId: UserId?,
        immediately: Boolean
    ) {
    }

    override suspend fun update(featureFlag: FeatureFlag) {
        featureFlags[featureFlag.featureId] = featureFlag
    }
}