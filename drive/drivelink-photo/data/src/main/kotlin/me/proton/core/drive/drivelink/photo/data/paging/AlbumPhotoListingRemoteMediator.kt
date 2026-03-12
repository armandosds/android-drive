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

package me.proton.core.drive.drivelink.photo.data.paging

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import androidx.paging.RemoteMediator.MediatorResult
import me.proton.core.drive.base.data.entity.LoggerLevel
import me.proton.core.drive.base.data.extension.log
import me.proton.core.drive.base.domain.log.LogTag
import me.proton.core.drive.base.domain.log.logId
import me.proton.core.drive.drivelink.photo.data.db.DriveLinkPhotoDatabase
import me.proton.core.drive.drivelink.photo.data.db.entity.AlbumPhotoListingRemoteKeyEntity
import me.proton.core.drive.drivelink.photo.domain.entity.AlbumPhotoListingsPage
import me.proton.core.drive.link.domain.entity.AlbumId
import me.proton.core.drive.link.domain.extension.userId
import me.proton.core.drive.photo.domain.entity.PhotoListing
import me.proton.core.drive.volume.domain.entity.VolumeId
import me.proton.core.network.domain.ApiException
import me.proton.core.util.kotlin.CoreLogger
import javax.inject.Inject

@OptIn(ExperimentalPagingApi::class)
class AlbumPhotoListingRemoteMediator @Inject constructor(
    private val pagedListKey: String,
    private val volumeId: VolumeId,
    private val albumId: AlbumId,
    private val database: DriveLinkPhotoDatabase,
    private val remoteAlbumPhotoListings: suspend (pageKey: String?) -> Result<AlbumPhotoListingsPage>,
    private val deleteAllLocalAlbumPhotoListings: suspend (AlbumId) -> Result<Unit>,
) : RemoteMediator<Int, PhotoListing.Album>() {
    private val remoteKeyDao = database.albumPhotoListingRemoteKeyDao

    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, PhotoListing.Album>,
    ): MediatorResult {
        return try {
            CoreLogger.v(LogTag.PAGING, "Remote ${loadType.name}")
            val pageKey = when (loadType) {
                LoadType.REFRESH -> null
                LoadType.PREPEND -> return MediatorResult.Success(endOfPaginationReached = true)
                LoadType.APPEND -> getLastRemoteKey()?.let { remoteKey ->
                    remoteKey.nextKey ?: return MediatorResult.Success(endOfPaginationReached = true)
                } ?: state.pages
                    .lastOrNull { page -> page.data.size == state.config.pageSize }
                    ?.data?.lastOrNull()?.linkId?.id
            }
            val albumPhotoListingsPage = remoteAlbumPhotoListings(pageKey)
                .onFailure { error ->
                    error.log(
                        tag = LogTag.PAGING,
                        message = "Getting remote album photo listings failed",
                        level = LoggerLevel.WARNING
                    )
                    return MediatorResult.Error(error)
                }
                .getOrThrow()
            val endOfPaginationReached = !albumPhotoListingsPage.hasMore
            CoreLogger.v(
                tag = LogTag.PAGING,
                message = buildString {
                    append("loaded album photo listings ")
                    append("(${albumPhotoListingsPage.albumPhotoListings.size}), ")
                    append("endOfPaginationReached = $endOfPaginationReached")
                },
            )
            val nextPageKey = if (endOfPaginationReached) {
                null
            } else {
                albumPhotoListingsPage.anchoreId
            }
            CoreLogger.v(LogTag.PAGING, "pageKey ($pageKey) nextPageKey ($nextPageKey)")
            val remoteKeys = albumPhotoListingsPage.albumPhotoListings.map { albumPhotoListings ->
                AlbumPhotoListingRemoteKeyEntity(
                    id = 0,
                    key = pagedListKey,
                    userId = albumId.userId,
                    volumeId = volumeId.id,
                    shareId = albumPhotoListings.albumId.shareId.id,
                    albumId = albumPhotoListings.albumId.id,
                    linkId = albumPhotoListings.linkId.id,
                    prevKey = pageKey,
                    nextKey = nextPageKey,
                )
            }
            database.inTransaction {
                if (loadType == LoadType.REFRESH) {
                    remoteKeyDao.deleteKeys(pagedListKey, volumeId.id, albumId.shareId.id, albumId.id)
                    deleteAllLocalAlbumPhotoListings(albumId)
                }
                albumPhotoListingsPage.saveAction()
                remoteKeyDao.insertOrUpdate(*remoteKeys.toTypedArray())
            }
            MediatorResult.Success(endOfPaginationReached)
        } catch (e: ApiException) {
            e.log(
                tag = LogTag.PAGING,
                message = "Failed to load $loadType with $state",
                level = LoggerLevel.WARNING,
            )
            MediatorResult.Error(e)
        }
    }

    private suspend fun getLastRemoteKey(): AlbumPhotoListingRemoteKeyEntity? {
        val lastRemoteKey = remoteKeyDao.getLastRemoteKey(
            key = pagedListKey,
            volumeId = volumeId.id,
            shareId = albumId.shareId.id,
            albumId = albumId.id,
        )
        CoreLogger.v(
            LogTag.PAGING,
            "last db remote key ${lastRemoteKey?.linkId?.logId()} - ${lastRemoteKey?.shareId?.logId()}"
        )
        return lastRemoteKey
    }
}
