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

package me.proton.core.drive.observability.data.extension

import me.proton.core.drive.observability.domain.metrics.sdk.common.Field
import me.proton.drive.sdk.telemetry.EncryptedField as SdkEncryptedField

fun SdkEncryptedField.toField(): Field? = when (this) {
    SdkEncryptedField.UNRECOGNIZED -> null
    SdkEncryptedField.SHARE_KEY -> Field.shareKey
    SdkEncryptedField.NODE_KEY -> Field.nodeKey
    SdkEncryptedField.NODE_NAME -> Field.nodeName
    SdkEncryptedField.NODE_HASH_KEY -> Field.nodeHashKey
    SdkEncryptedField.NODE_EXTENDED_ATTRIBUTES -> Field.nodeExtendedAttributes
    SdkEncryptedField.NODE_CONTENT_KEY -> Field.nodeContentKey
    SdkEncryptedField.CONTENT -> Field.content
}
