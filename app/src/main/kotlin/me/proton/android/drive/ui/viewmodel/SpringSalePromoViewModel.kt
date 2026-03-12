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

package me.proton.android.drive.ui.viewmodel

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import me.proton.android.drive.extension.log
import me.proton.android.drive.ui.viewevent.SpringSalePromoViewEvent
import me.proton.android.drive.ui.viewstate.SpringSalePromoViewState
import me.proton.android.drive.usecase.notification.MarkSpringSalePromoAsShown
import me.proton.core.drive.base.domain.log.LogTag.VIEW_MODEL
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.base.presentation.common.Action
import me.proton.core.drive.base.presentation.common.getThemeDrawableId
import me.proton.core.drive.base.presentation.viewmodel.UserViewModel
import me.proton.core.plan.domain.usecase.GetDynamicPlansAdjustedPrices
import me.proton.core.plan.domain.usecase.ObserveUserCurrency
import me.proton.core.plan.presentation.entity.PlanCycle
import me.proton.core.plan.presentation.usecase.ComposeAutoRenewText
import me.proton.core.presentation.utils.formatCentsPriceDefaultLocale
import javax.inject.Inject
import me.proton.core.drive.base.presentation.R as BasePresentation
import me.proton.core.drive.i18n.R as I18N
import me.proton.core.presentation.R as CorePresentation

@HiltViewModel
class SpringSalePromoViewModel @Inject constructor(
    @ApplicationContext appContext: Context,
    savedStateHandle: SavedStateHandle,
    observeUserCurrency: ObserveUserCurrency,
    private val getDynamicPlansAdjustedPrices: GetDynamicPlansAdjustedPrices,
    private val composeAutoRenewText: ComposeAutoRenewText,
    private val markSpringSalePromoAsShown: MarkSpringSalePromoAsShown,
) : ViewModel(), UserViewModel by UserViewModel(savedStateHandle) {
    private var viewEvent: SpringSalePromoViewEvent? = null

    private val storageImageResId: Int get() = getThemeDrawableId(
        light = BasePresentation.drawable.img_storage_light_32,
        dark = BasePresentation.drawable.img_storage_dark_32,
        dayNight = BasePresentation.drawable.img_storage_daynight_32,
    )

    private val documentImageResId: Int get() = getThemeDrawableId(
        light = BasePresentation.drawable.img_document_light_32,
        dark = BasePresentation.drawable.img_document_dark_32,
        dayNight = BasePresentation.drawable.img_document_daynight_32,
    )

    private val secureImageResId: Int get() = getThemeDrawableId(
        light = BasePresentation.drawable.img_secure_light_32,
        dark = BasePresentation.drawable.img_secure_dark_32,
        dayNight = BasePresentation.drawable.img_secure_daynight_32,
    )

    private val privateImageResId: Int get() = getThemeDrawableId(
        light = BasePresentation.drawable.img_private_light_32,
        dark = BasePresentation.drawable.img_private_dark_32,
        dayNight = BasePresentation.drawable.img_private_daynight_32,
    )

    private val titleImageResId: Int get() = getThemeDrawableId(
        light = BasePresentation.drawable.img_spring_sale_promo_light,
        dark = BasePresentation.drawable.img_spring_sale_promo_dark,
        dayNight = BasePresentation.drawable.img_spring_sale_promo_daynight,
    )

    private val backgroundResId: Int get() = getThemeDrawableId(
        light = BasePresentation.drawable.bg_spring_sale_promo_port_light,
        dark = BasePresentation.drawable.bg_spring_sale_promo_port_dark,
        dayNight = BasePresentation.drawable.bg_spring_sale_promo_port_daynight,
    )

    private val backgroundLandResId: Int get() = getThemeDrawableId(
        light = BasePresentation.drawable.bg_spring_sale_promo_land_light,
        dark = BasePresentation.drawable.bg_spring_sale_promo_land_dark,
        dayNight = BasePresentation.drawable.bg_spring_sale_promo_land_daynight,
    )

    private val items = setOf(
        SpringSalePromoViewState.Item(
            imageResId = storageImageResId,
            title = appContext.getString(I18N.string.promo_storage_title),
        ),
        SpringSalePromoViewState.Item(
            imageResId = documentImageResId,
            title = appContext.getString(I18N.string.promo_document_title),
        ),
        SpringSalePromoViewState.Item(
            imageResId = secureImageResId,
            title = appContext.getString(I18N.string.promo_secure_title),
        ),
        SpringSalePromoViewState.Item(
            imageResId = privateImageResId,
            title = appContext.getString(I18N.string.promo_private_title),
        ),
    )

    private val initialViewState = SpringSalePromoViewState(
        titleImageResId = titleImageResId,
        backgroundResId = backgroundResId,
        backgroundLandResId = backgroundLandResId,
        closeAction = Action.Icon(
            iconResId = CorePresentation.drawable.ic_proton_cross,
            contentDescriptionResId = I18N.string.common_close_action,
            onAction = { viewEvent?.onClose?.invoke() },
        ),
        items = items,
        getDealButtonResId = I18N.string.promo_claim_offer_button,
        monthlyPrice = "",
        yearlyPrice = "",
        period = "12 ${appContext.getString(I18N.string.common_month).lowercase()}s",
        monthlyPricePeriod = "/${appContext.getString(I18N.string.common_month).lowercase()}",
        yearlyPricePeriod = "/${appContext.getString(I18N.string.common_year).lowercase()}",
        autoRenewPrice = "",
    )
    val viewState: Flow<SpringSalePromoViewState> = observeUserCurrency(userId).map { userCurrency ->
        val cycle = PlanCycle.YEARLY.value
        val plans = coRunCatching { getDynamicPlansAdjustedPrices(userId).plans }
            .onFailure { error ->
                error.log(VIEW_MODEL, "Failed to get dynamic plans")
            }
            .getOrNull()
        plans?.firstOrNull { plan -> plan.name == DRIVE_PLUS_1_TB }
            ?.let { plan ->
                val availableCurrencies = plan.instances[cycle]?.price?.keys?.map { it.uppercase() } ?: emptyList()
                val currency = if (availableCurrencies.contains(userCurrency.uppercase())) {
                    userCurrency
                } else {
                    availableCurrencies.firstOrNull() ?: ""
                }
                val monthlyPrice = takeIf { currency.isNotEmpty() }
                    ?.let {
                        plan.instances[cycle]?.price[currency]?.current?.toDouble()?.div(12)?.formatCentsPriceDefaultLocale(currency)
                    } ?: ""
                val yearlyPrice = takeIf { currency.isNotEmpty() }
                    ?.let {
                        plan.instances[cycle]?.price[currency]?.current?.toDouble()?.formatCentsPriceDefaultLocale(currency)
                    } ?: ""
                initialViewState.copy(
                    monthlyPrice = appContext.getString(I18N.string.promo_offer_per_month_details, monthlyPrice),
                    yearlyPrice = yearlyPrice,
                    autoRenewPrice = composeAutoRenewText(plan.instances[cycle]?.price[currency], cycle) ?: ""
                )
            } ?: initialViewState
    }

    fun viewEvent(
        navigateToSubscription: () -> Unit,
        navigateBack: () -> Unit,
    ): SpringSalePromoViewEvent = object : SpringSalePromoViewEvent {
        override val onClose = navigateBack
        override val onGetDeal = {
            navigateToSubscription.invoke().also { navigateBack.invoke() }
        }
        override val onPromoShown = { onShown() }
    }.also { viewEvent ->
        this.viewEvent = viewEvent
    }

    private fun onShown() {
        viewModelScope.launch {
            markSpringSalePromoAsShown(userId)
                .onFailure { error ->
                    error.log(VIEW_MODEL, "Failed to mark spring sale promo as shown")
                }
        }
    }

    companion object {
        private const val DRIVE_PLUS_1_TB = "drive1tb2025"
    }
}
