/*
 * Copyright (c) 2024 Proton AG.
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

package me.proton.core.drive.upload.data.handler

import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import kotlinx.coroutines.flow.firstOrNull
import me.proton.core.drive.base.data.entity.WorkInfoStopReason
import me.proton.core.drive.base.domain.extension.getOrNull
import me.proton.core.drive.base.domain.extension.toResult
import me.proton.core.drive.base.domain.log.LogTag
import me.proton.core.drive.base.domain.log.LogTag.UPLOAD
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.drivelink.domain.usecase.UseSdkForUpload
import me.proton.core.drive.linkupload.domain.entity.UploadFileLink
import me.proton.core.drive.linkupload.domain.extension.toInitiator
import me.proton.core.drive.observability.data.extension.toShareType
import me.proton.core.drive.observability.domain.constraint.CountConstraint
import me.proton.core.drive.observability.domain.constraint.MinimumIntervalConstraint
import me.proton.core.drive.observability.domain.metrics.UploadErroringUsersTotal
import me.proton.core.drive.observability.domain.metrics.UploadErrorsFileSizeHistogram
import me.proton.core.drive.observability.domain.metrics.UploadErrorsTotal
import me.proton.core.drive.observability.domain.metrics.UploadErrorsTransferSizeHistogram
import me.proton.core.drive.observability.domain.metrics.UploadWorkerCancellationTotal
import me.proton.core.drive.observability.domain.metrics.common.Pipeline
import me.proton.core.drive.observability.domain.metrics.common.ShareType
import me.proton.core.drive.observability.domain.metrics.common.UploadErrorsBuckets
import me.proton.core.drive.observability.domain.usecase.EnqueueObservabilityEvent
import me.proton.core.drive.share.domain.entity.ShareId
import me.proton.core.drive.share.domain.usecase.GetShare
import me.proton.core.drive.upload.data.exception.UploadWorkerException
import me.proton.core.drive.upload.data.extension.getFileSize
import me.proton.core.drive.upload.data.extension.log
import me.proton.core.drive.upload.data.extension.logTag
import me.proton.core.drive.upload.data.extension.toReason
import me.proton.core.drive.upload.data.extension.toUploadErrorType
import me.proton.core.drive.upload.domain.handler.UploadErrorHandler
import me.proton.core.drive.upload.domain.manager.UploadErrorManager
import me.proton.core.drive.upload.domain.manager.UploadWorkManager
import me.proton.core.drive.user.domain.extension.isWithoutProtonSubscription
import me.proton.core.user.domain.usecase.GetUser
import me.proton.core.util.kotlin.CoreLogger
import javax.inject.Inject
import kotlin.time.Duration.Companion.minutes

class ObservabilityUploadErrorHandler @Inject constructor(
    private val getShare: GetShare,
    private val enqueueObservabilityEvent: EnqueueObservabilityEvent,
    private val getUser: GetUser,
    private val uploadWorkManager: UploadWorkManager,
    private val minimumIntervalConstraint: MinimumIntervalConstraint,
    private val countConstraint: CountConstraint,
    private val useSdkForUpload: UseSdkForUpload,
) : UploadErrorHandler {

    override suspend fun onError(uploadError: UploadErrorManager.Error) {
        coRunCatching {
            val uploadedBySdk = useSdkForUpload(uploadError.uploadFileLink.parentLinkId)
                .getOrNull(UPLOAD, "Cannot check for sdk usage")
            if (uploadedBySdk == true) {
                CoreLogger.d(
                    tag = uploadError.uploadFileLink.logTag(),
                    message = "Skipping observability upload error for file uploaded by SDK",
                )
                return
            }
            val shareType = getShareType(uploadError.uploadFileLink.shareId)
            notifyUploadErrorsTotalMetric(uploadError, shareType)
            notifyUploadErrorsFileSizeHistogramMetric(uploadError)
            notifyUploadErrorsTransferSizeHistogramMetric(uploadError)
            notifyUploadErroringUsersTotalMetric(uploadError, shareType)
            notifyUploadWorkerStopped(uploadError)
        }.onFailure { error ->
            error.log(
                tag = uploadError.uploadFileLink.logTag(),
                message = "Cannot handle error for: ${uploadError.uploadFileLink.id}",
            )
        }
    }

    private suspend fun getShareType(shareId: ShareId): ShareType =
        getShare(shareId)
            .toResult()
            .onFailure { error -> error.log(LogTag.UPLOAD, "Failed getting share") }
            .getOrThrow()
            .type.toShareType()

    private suspend fun notifyUploadErrorsTotalMetric(uploadError: UploadErrorManager.Error, shareType: ShareType) {
        enqueueObservabilityEvent(
            UploadErrorsTotal(
                Labels = UploadErrorsTotal.LabelsData(
                    type = uploadError.throwable.toUploadErrorType(),
                    shareType = shareType,
                    initiator = uploadError.uploadFileLink.toInitiator(),
                )
            ),
            constraint = countConstraint(
                userId = uploadError.uploadFileLink.userId,
                key = uploadError.key,
                maxCount = 1,
            )
        )
    }

    private suspend fun notifyUploadErrorsFileSizeHistogramMetric(uploadError: UploadErrorManager.Error) {
        uploadError.uploadFileLink.size?.let { bytes ->
            enqueueObservabilityEvent(
                UploadErrorsFileSizeHistogram(
                    Value = UploadErrorsBuckets.getBucketValue(bytes.value)
                )
            )
        }
    }

    private suspend fun notifyUploadErroringUsersTotalMetric(
        uploadError: UploadErrorManager.Error,
        shareType: ShareType,
        excludedErrorTypes: Set<UploadErrorsTotal.Type> = setOf(
            UploadErrorsTotal.Type.free_space_exceeded,
            UploadErrorsTotal.Type.too_many_children,
            UploadErrorsTotal.Type.network_error,
        )
    ) {
        if (uploadError.throwable.toUploadErrorType() !in excludedErrorTypes) {
            val user = getUser(uploadError.uploadFileLink.userId, refresh = false)
            notifyUploadErroringUsersTotalMetric(
                uploadError = uploadError,
                shareType = shareType,
                isFreeUser = user.isWithoutProtonSubscription,
            )
        }
    }

    private suspend fun notifyUploadErroringUsersTotalMetric(
        uploadError: UploadErrorManager.Error,
        shareType: ShareType,
        isFreeUser: Boolean,
    ) {
        enqueueObservabilityEvent(
            observabilityData = UploadErroringUsersTotal(
                Labels = UploadErroringUsersTotal.LabelsData(
                    plan = if (isFreeUser) UploadErroringUsersTotal.Plan.free else UploadErroringUsersTotal.Plan.paid,
                    shareType = shareType,
                    initiator = uploadError.uploadFileLink.toInitiator(),
                )
            ),
            constraint = minimumIntervalConstraint(
                userId = uploadError.uploadFileLink.userId,
                schemaId = UploadErroringUsersTotal.SCHEMA_ID,
                interval = 5.minutes,
            )
        )
    }

    private suspend fun notifyUploadWorkerStopped(
        uploadError: UploadErrorManager.Error,
        includeWorkerNames: Map<String, Pipeline> = mapOf(
            "BlockUploadWorker" to Pipeline.legacy,
            "UploadFileSdkWorker" to Pipeline.default
        )
    ) {
        val error = uploadError.throwable
        if (error is UploadWorkerException && error.name in includeWorkerNames.keys) {
            if(VERSION.SDK_INT >= VERSION_CODES.S && error.stopReason != null) {
                notifyUploadWorkerStopped(
                    uploadFileLink = uploadError.uploadFileLink,
                    pipeline = includeWorkerNames.getValue(error.name),
                    stopReason = error.stopReason,
                )
            }
        }
    }

    private suspend fun notifyUploadWorkerStopped(
        uploadFileLink: UploadFileLink,
        pipeline: Pipeline,
        stopReason: WorkInfoStopReason,
    ) {
        enqueueObservabilityEvent(
            observabilityData = UploadWorkerCancellationTotal(
                Labels = UploadWorkerCancellationTotal.LabelsData(
                    pipeline = pipeline,
                    initiator = uploadFileLink.toInitiator(),
                    reason = stopReason.toReason(),
                    fileSize = uploadFileLink.getFileSize()
                )
            ),
            constraint = minimumIntervalConstraint(
                userId = uploadFileLink.userId,
                schemaId = UploadWorkerCancellationTotal.SCHEMA_ID,
                interval = 5.minutes,
            )
        )
    }

    private suspend fun notifyUploadErrorsTransferSizeHistogramMetric(uploadError: UploadErrorManager.Error) {
        uploadError.uploadFileLink.size?.let { bytes ->
            val uploadedBytes = uploadWorkManager.getProgressFlow(uploadError.uploadFileLink)
                ?.firstOrNull()
                ?.let { progress ->
                    (bytes.value * progress.value).toLong()
                } ?: 0L
            enqueueObservabilityEvent(
                UploadErrorsTransferSizeHistogram(
                    Value = UploadErrorsBuckets.getBucketValue(uploadedBytes)
                )
            )
        }
    }

    private val UploadErrorManager.Error.key: String get() = buildString {
        append(uploadFileLink.key)
        append(throwable.toUploadErrorType().toString())
    }

    private val UploadFileLink.key: String get() = buildString {
        append(uriString)
        append(".")
    }
}
