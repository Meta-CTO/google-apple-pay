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

@file:OptIn(ExperimentalForeignApi::class)

package com.khalid.multiplatform.googleapple.payments

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.datetime.toNSDateComponents
import platform.Foundation.NSCalendarUnit
import platform.Foundation.NSCalendarUnitDay
import platform.Foundation.NSCalendarUnitEra
import platform.Foundation.NSCalendarUnitHour
import platform.Foundation.NSCalendarUnitMinute
import platform.Foundation.NSCalendarUnitMonth
import platform.Foundation.NSCalendarUnitQuarter
import platform.Foundation.NSCalendarUnitSecond
import platform.Foundation.NSCalendarUnitYear
import platform.Foundation.NSCalendarUnitYearForWeekOfYear
import platform.Foundation.NSDecimalNumber
import platform.Foundation.NSJSONReadingOptions
import platform.Foundation.NSLog
import platform.PassKit.PKMerchantCapability3DS
import platform.PassKit.PKPayment
import platform.PassKit.PKPaymentAuthorizationStatus
import platform.PassKit.PKPaymentAuthorizationViewController
import platform.PassKit.PKPaymentAuthorizationViewControllerDelegateProtocol
import platform.PassKit.PKPaymentNetworkAmex
import platform.PassKit.PKPaymentNetworkDiscover
import platform.PassKit.PKPaymentNetworkMasterCard
import platform.PassKit.PKPaymentNetworkVisa
import platform.PassKit.PKPaymentRequest
import platform.PassKit.PKPaymentSummaryItem
import platform.UIKit.UIApplication
import platform.darwin.NSObject
import platform.Foundation.NSJSONSerialization
import platform.Foundation.NSURL
import platform.PassKit.PKPaymentSummaryItemType
import platform.PassKit.PKRecurringPaymentRequest
import platform.PassKit.PKRecurringPaymentSummaryItem

@Suppress("ForbiddenComment")
class ApplePayModelImpl(val config: PaymentConfig) : PaymentInterface {

    override suspend fun canMakePayments(): Boolean {
        return PKPaymentAuthorizationViewController.canMakePayments()
    }

    @Throws(Throwable::class)
    override suspend fun makePayments(
        amount: String,
        callback: (result: Result<PaymentResult>) -> Unit
    ) {
        val paymentRequest = PKPaymentRequest()
        paymentRequest.merchantIdentifier = config.gatewayMerchantId
        paymentRequest.countryCode = config.countryCode
        paymentRequest.currencyCode = config.currencyCode
        paymentRequest.supportedNetworks = listOf(
            PKPaymentNetworkVisa,
            PKPaymentNetworkMasterCard,
            PKPaymentNetworkAmex,
            PKPaymentNetworkDiscover
        )
        paymentRequest.merchantCapabilities = PKMerchantCapability3DS

        val paymentItem = PKPaymentSummaryItem()
        paymentItem.label = config.label
        paymentItem.amount = NSDecimalNumber(amount)

        paymentRequest.paymentSummaryItems = listOf(paymentItem)

        if(config.isRecurringPayment && config.recurringPaymentData != null) {
            val recurringPayment = PKRecurringPaymentSummaryItem()
            recurringPayment.setLabel(config.recurringPaymentData.recurringLabel)
            recurringPayment.setAmount(NSDecimalNumber(amount))
            recurringPayment.setType(config.recurringPaymentData.amountType.toPKPaymentSummaryItemType())

            recurringPayment.intervalUnit = config.recurringPaymentData.frequencyUnit.toNSCalendarUnit()
            recurringPayment.intervalCount = config.recurringPaymentData.interval.toLong()
            recurringPayment.startDate = config.recurringPaymentData.startDate?.toNSDateComponents()?.date
            recurringPayment.endDate = config.recurringPaymentData.endDate?.toNSDateComponents()?.date

            val recurring = PKRecurringPaymentRequest()

            recurring.paymentDescription = config.recurringPaymentData.paymentDescription
            recurring.regularBilling = recurringPayment
            recurring.managementURL = NSURL(string = config.recurringPaymentData.managementURL)
            paymentRequest.recurringPaymentRequest = recurring
        }

        /**
         * TODO:
         * {"message":"This method does I/O on the main thread underneath that can lead to UI
         * responsiveness issues when called on main thread. Consider ways to optimize this
         * code path","antipattern trigger":"-[NSBundle bundleIdentifier]","message type":
         * "suppressable","show in console":"0"}
         * */
        val paymentController =
            PKPaymentAuthorizationViewController(paymentRequest = paymentRequest)
        paymentController.delegate = PaymentAuthorizationDelegate(callback)

        val window = UIApplication.sharedApplication.keyWindow
        window?.rootViewController?.presentViewController(
            paymentController,
            animated = true,
            completion = null
        )

    }

}

private class PaymentAuthorizationDelegate(private val callback: (result: Result<PaymentResult>) -> Unit) :
    NSObject(),
    PKPaymentAuthorizationViewControllerDelegateProtocol {
    override fun paymentAuthorizationViewController(
        controller: PKPaymentAuthorizationViewController,
        didAuthorizePayment: PKPayment,
        completion: (PKPaymentAuthorizationStatus) -> Unit
    ) {
        NSLog("Payment authorized")
        // Handle payment authorization here
        completion(PKPaymentAuthorizationStatus.PKPaymentAuthorizationStatusSuccess)
        // Token will be empty on Simulator
        NSLog("Payment token ${didAuthorizePayment.token.paymentData}")

        val token = didAuthorizePayment.token.paymentData

        val jsonString =
            NSJSONSerialization.JSONObjectWithData(token, NSJSONReadingOptions.MIN_VALUE, null)

        callback(
            Result.Success(
                PaymentResult(
                    status = PaymentStatus.SUCCESS,
                    receipt = jsonString.toString()
                )
            )
        )
    }

    override fun paymentAuthorizationViewControllerDidFinish(controller: PKPaymentAuthorizationViewController) {
        NSLog("Payment finished")
        controller.dismissViewControllerAnimated(true, completion = null)
    }
}

private fun FrequencyUnit.toNSCalendarUnit(): NSCalendarUnit {
    return when (this) {
        FrequencyUnit.ERA -> NSCalendarUnitEra
        FrequencyUnit.YEAR -> NSCalendarUnitYear
        FrequencyUnit.MONTH -> NSCalendarUnitMonth
        FrequencyUnit.WEEK -> NSCalendarUnitYearForWeekOfYear
        FrequencyUnit.DAY -> NSCalendarUnitDay
        FrequencyUnit.HOUR -> NSCalendarUnitHour
        FrequencyUnit.MINUTE -> NSCalendarUnitMinute
        FrequencyUnit.SECOND -> NSCalendarUnitSecond
        FrequencyUnit.QUARTER -> NSCalendarUnitQuarter
    }
}

private fun AmountType.toPKPaymentSummaryItemType(): PKPaymentSummaryItemType {
    return when (this) {
        AmountType.FINAL -> PKPaymentSummaryItemType.PKPaymentSummaryItemTypeFinal
        AmountType.PENDING -> PKPaymentSummaryItemType.PKPaymentSummaryItemTypePending
    }
}