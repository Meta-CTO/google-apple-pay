/*
 * Copyright 2024 Mohammed Khalid Hamid.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.khalid.multiplatform.googleapple.payments

import kotlinx.datetime.LocalDate


data class PaymentConfig(
    /**
     * The merchant’s two-letter ISO 3166 country code.
     *
     * required by Apple and Google Pay
     * */
    val countryCode: String,
    /**
     * The three-letter ISO 4217 currency code that determines the
     * currency the payment request uses.
     *
     * required by Apple and Google Pay
     * */
    val currencyCode: String,

    /**
     * required by Google Pay
     * */
    val allowBillingAddress: Boolean = false,

    /**
     *  required by Apple and Google Pay
     * */
    val allowedCards: List<AllowedCards>,

    /**
     *  required by Apple and Google Pay
     * */
    val supportedCards : List<SupportedCardMethods>,

    /**
     * required by Apple Pay
     * */
    val label: String = "",

    /**
     * sample : example
     *
     * required by Google Pay
     * */
    val gateway: String = "",
    /**
     * sample : exampleGatewayMerchantId
     *
     * required by Apple and Google Pay
     * */
    val gatewayMerchantId: String = "",
    /**
     * required by Google Pay
     * */
    val merchantName: String = "",


    /**
     * required by Google Pay
     * */
    val shippingDetails: ShippingDetails? = null,

    /**
     * required by Google Pay
     * */
    val paymentsEnvironment: Int = 3, // WalletConstants.ENVIRONMENT_TEST

    /**
     * to set if recurring payment is enabled
     */
    val isRecurringPayment: Boolean = false,

    /**
     * required by Apple Pay if isRecurringPayment is true
     * */
    val recurringPaymentData: RecurringPaymentData? = null
)

/**
 * Recurring payment data
 * startDate: The date the recurring payment starts. If not specified, the payment starts immediately.
 * endDate: The date the recurring payment ends. If not specified, the payment continues indefinitely.
 * interval: The interval between payments. For example, if the interval is 2 and the frequency unit is MONTH, the payment occurs every two months.
 * frequencyUnit: The unit of time between payments.
 */
data class RecurringPaymentData(
    val startDate: LocalDate? = null,
    val endDate: LocalDate? = null,
    val interval: Int,
    val frequencyUnit: FrequencyUnit,
    val paymentDescription: String,
    val recurringLabel: String,
    val managementURL: String,
    val amountType: AmountType
)

enum class AmountType {
    FINAL,
    PENDING
}

enum class FrequencyUnit {
    ERA,
    YEAR,
    MONTH,
    DAY,
    HOUR,
    MINUTE,
    SECOND,
    WEEK,
    QUARTER
}

enum class AllowedCards {
    VISA,
    MASTERCARD,
    AMEX,
    JCB, // Only supported by GooglePay
    DISCOVER
}

enum class SupportedCardMethods {
    PAN_ONLY,                   // supported only by GooglePay
    /**
     * supported by ApplePay with value Capability3DS
     * supported by GooglePay with value CRYPTOGRAM_3DS
     * */
    CRYPTOGRAM_3DS,             // supported by both Apple and Google Pay
    CapabilityInstantFundsOut,  // supported only by ApplePay
    /**
     * Chip based CC
     * */
    CapabilityEMV,              // supported only by ApplePay
    CapabilityCredit,           // supported only by ApplePay
    CapabilityDebit,            // supported only by ApplePay
}
