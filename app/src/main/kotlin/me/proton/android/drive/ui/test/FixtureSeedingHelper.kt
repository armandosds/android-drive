package me.proton.android.drive.ui.test

import android.content.Context
import androidx.annotation.RestrictTo
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import me.proton.core.crypto.common.pgp.DecryptedText
import me.proton.core.crypto.common.pgp.VerificationStatus
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.domain.extension.toResult
import me.proton.core.drive.drivelink.domain.entity.DriveLink
import me.proton.core.drive.drivelink.list.domain.usecase.GetDecryptedDriveLinks
import me.proton.core.drive.file.base.domain.usecase.CreateXAttr as FileBaseCreateXAttr
import me.proton.core.drive.crypto.domain.repository.DecryptedTextRepository
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.drive.link.domain.entity.Link
import me.proton.core.drive.link.domain.usecase.InsertOrUpdateLinks
import me.proton.core.drive.share.domain.entity.Share
import me.proton.core.drive.share.domain.entity.ShareId
import me.proton.core.drive.share.domain.usecase.GetShare
import me.proton.core.drive.volume.domain.entity.Volume
import me.proton.core.drive.volume.domain.usecase.GetOldestActiveVolume

@RestrictTo(RestrictTo.Scope.TESTS)
internal class FixtureSeedingHelper private constructor(
    private val entryPoint: FixtureSeedingEntryPoint,
) {

    companion object {
        fun fromContext(context: Context): FixtureSeedingHelper =
            FixtureSeedingHelper(
                EntryPointAccessors.fromApplication(
                    context,
                    FixtureSeedingEntryPoint::class.java
                )
            )
    }

    suspend fun getOldestActiveVolume(
        userId: UserId,
        type: Volume.Type,
    ): Result<Volume> = entryPoint
        .getOldestActiveVolume(userId, type)
        .toResult()

    suspend fun getDecryptedDriveLinks(parentId: FolderId): Result<List<DriveLink>> =
        entryPoint.decryptedDriveLinks(parentId)

    suspend fun getShare(shareId: ShareId): Result<Share> =
        entryPoint.getShare(shareId).toResult()

    suspend fun insertOrUpdateLinks(link: Link) {
        entryPoint.insertOrUpdateLinks(link)
    }

    val createXAttr = entryPoint.createXAttr

    suspend fun addDecryptedXAttr(
        userId: UserId,
        keyId: String,
        xAttrJson: String,
    ) {
        entryPoint.decryptedTextRepository.addDecryptedText(
            userId,
            keyId,
            DecryptedText(
                text = xAttrJson,
                status = VerificationStatus.Success,
            ),
        )
    }

}

@EntryPoint
@InstallIn(SingletonComponent::class)
@RestrictTo(RestrictTo.Scope.TESTS)
internal interface FixtureSeedingEntryPoint {
    val getOldestActiveVolume: GetOldestActiveVolume
    val getShare: GetShare
    val insertOrUpdateLinks: InsertOrUpdateLinks
    val decryptedDriveLinks: GetDecryptedDriveLinks
    val createXAttr: FileBaseCreateXAttr
    val decryptedTextRepository: DecryptedTextRepository
}
