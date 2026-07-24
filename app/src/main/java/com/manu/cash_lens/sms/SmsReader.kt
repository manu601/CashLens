package com.manu.cash_lens.sms

import android.content.Context
import android.provider.Telephony
import android.util.Log
import com.manu.cash_lens.models.Transaction
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.regex.Pattern

class SmsReader(private val context: Context) {

    fun getMpesaMessages(): List<Transaction> {

        val messages = mutableListOf<Transaction>()

        val cursor = context.contentResolver.query(
            Telephony.Sms.CONTENT_URI,
            arrayOf(
                Telephony.Sms.ADDRESS,
                Telephony.Sms.BODY,
                Telephony.Sms.DATE
            ),
            null,
            null,
            "${Telephony.Sms.DATE} DESC"
        )

        cursor?.use {

            val addressIndex = it.getColumnIndex(Telephony.Sms.ADDRESS)
            val bodyIndex = it.getColumnIndex(Telephony.Sms.BODY)
            val dateIndex = it.getColumnIndex(Telephony.Sms.DATE)

            while (it.moveToNext()) {

                val provider = it.getString(addressIndex)
                val body = it.getString(bodyIndex)
                val smsTimestamp = it.getLong(dateIndex)
                if (provider.equals("MPESA", ignoreCase = true)) {
                    Log.d(
                        "LATEST_MPESA",
                        "Date=${Date(smsTimestamp)}\n$body"
                    )
                }

                Log.d("MPESA_SMS", body)

                if (
                    provider.equals("MPESA", ignoreCase = true) &&
                    body.contains("Confirmed", true) &&
                    (
                            body.contains("New M-PESA balance", true) ||
                                    body.contains("M-PESA balance is", true)
                            )
                ) {

                    // Ignore Fuliza borrow messages
                    if (body.contains("Fuliza M-PESA amount is", true)) {
                        continue
                    }

                    val amountRegex = Pattern.compile(
                        "Ksh\\s*([0-9,]+\\.[0-9]{2})",
                        Pattern.CASE_INSENSITIVE
                    )

                    val matcher = amountRegex.matcher(body)

                    val amount = if (matcher.find()) {
                        matcher.group(1)
                            .replace(",", "")
                            .toDouble()
                    } else {
                        0.0
                    }


                         val type = when {

                             body.contains("fully pay your outstanding Fuliza", true) ||
                                     body.contains("partially pay your outstanding Fuliza", true) ->
                                 "Fuliza Repayment"

                             body.contains("sent to", true) ->
                                 "Sent"

                             body.contains("received", true) ->
                                 "Received"

                             body.contains("paid to", true) ->
                                 "PayBill"

                             else ->
                                 "Other"
                         }


                         val recipient = when(type) {

                             "Fuliza Repayment" ->
                                 "Fuliza"

                             "Sent" -> {
                            val regex = Pattern.compile(
                                "sent to (.+?) on",
                                Pattern.CASE_INSENSITIVE
                            )

                            val match = regex.matcher(body)

                            if (match.find()) match.group(1)
                            else "Unknown"
                        }


                        "Received" -> {
                            val regex = Pattern.compile(
                                "from (.+?) on",
                                Pattern.CASE_INSENSITIVE
                            )

                            val match = regex.matcher(body)

                            if (match.find()) match.group(1)
                            else "Unknown"
                        }


                        "PayBill" -> {
                            val regex = Pattern.compile(
                                "paid to (.+?)\\. on|paid to (.+?) on",
                                Pattern.CASE_INSENSITIVE
                            )

                            val match = regex.matcher(body)

                            if (match.find()) {
                                match.group(1)
                                    ?: match.group(2)
                                    ?: "Unknown"
                            } else {
                                "Unknown"
                            }
                        }


                        else -> "Unknown"
                    }


                    val dateTimeRegex = Pattern.compile(
                        "on\\s+([0-9]{1,2}/[0-9]{1,2}/[0-9]{2,4})\\s+at\\s+([0-9]{1,2}:[0-9]{2}\\s*[AP]M)",
                        Pattern.CASE_INSENSITIVE
                    )

                    val dateTimeMatcher = dateTimeRegex.matcher(body)

                    var date = ""
                    var time = ""

                    if (dateTimeMatcher.find()) {
                        date = dateTimeMatcher.group(1)
                        time = dateTimeMatcher.group(2)
                    }


                    if (date.isEmpty() || time.isEmpty()) {

                        val timestampDate = Date(smsTimestamp)

                        date = SimpleDateFormat(
                            "dd/MM/yyyy",
                            Locale.getDefault()
                        ).format(timestampDate)

                        time = SimpleDateFormat(
                            "hh:mm a",
                            Locale.getDefault()
                        ).format(timestampDate)
                    }


                    val receiptRegex = Pattern.compile(
                        "^([A-Z0-9]{10})\\s+Confirmed",
                        Pattern.CASE_INSENSITIVE
                    )

                    val receiptMatcher = receiptRegex.matcher(body)

                    val receipt = if (receiptMatcher.find()) {
                        receiptMatcher.group(1)
                    } else {
                        ""
                    }


                    val balanceRegex = Pattern.compile(
                        "(?:New\\s+|Your\\s+)?M-PESA balance is\\s*(?:Ksh\\s*)?([0-9,]+\\.[0-9]{2})",
                        Pattern.CASE_INSENSITIVE
                    )
                    val balanceMatcher = balanceRegex.matcher(body)

                    val found = balanceMatcher.find()

                    Log.d("BALANCE_REGEX", "Found=$found")

                    val balance = if (found) {
                        val value = balanceMatcher.group(1)
                            .replace(",", "")
                            .toDouble()

                        Log.d("BALANCE_REGEX", "Parsed Balance=$value")

                        value
                    } else {
                        Log.d("BALANCE_REGEX", "FAILED TO MATCH:\n$body")
                        0.0
                    }


                    val feeRegex = Pattern.compile(
                        "Transaction cost,?\\s*K(?:sh|ES)\\s*([0-9,]+\\.[0-9]{2})",
                        Pattern.CASE_INSENSITIVE
                    )

                    val feeMatcher = feeRegex.matcher(body)

                    val fee = if (feeMatcher.find()) {
                        feeMatcher.group(1)
                            .replace(",", "")
                            .toDouble()
                    } else {
                        0.0
                    }
                    Log.d(
                        "BALANCE_PARSE",
                        "Receipt=$receipt Balance=$balance SMS=$body"
                    )


                    val transaction = Transaction(
                        receipt = receipt,
                        provider = provider,
                        amount = amount,
                        recipient = recipient,
                        date = date,
                        time = time,
                        type = type,
                        balance = balance,
                        fee = fee,
                        smsTimestamp = smsTimestamp
                    )


                    messages.add(transaction)
                }
            }
        }

        return messages
    }
    fun getLatestFulizaStatus(): com.manu.cash_lens.models.FulizaStatus {

        var outstanding = 0.0
        var available = 0.0

        val cursor = context.contentResolver.query(
            Telephony.Sms.CONTENT_URI,
            arrayOf(
                Telephony.Sms.ADDRESS,
                Telephony.Sms.BODY
            ),
            null,
            null,
            "${Telephony.Sms.DATE} DESC"
        )

        cursor?.use {

            val addressIndex = it.getColumnIndex(Telephony.Sms.ADDRESS)
            val bodyIndex = it.getColumnIndex(Telephony.Sms.BODY)

            while (it.moveToNext()) {

                val provider = it.getString(addressIndex)
                val body = it.getString(bodyIndex)

                if (!provider.equals("MPESA", true))
                    continue

                // Outstanding amount
                val outstandingRegex = Pattern.compile(
                    "outstanding amount is Ksh\\s*([0-9,]+\\.[0-9]{2})",
                    Pattern.CASE_INSENSITIVE
                )

                val outstandingMatcher = outstandingRegex.matcher(body)

                if (outstandingMatcher.find()) {

                    outstanding = outstandingMatcher.group(1)
                        .replace(",", "")
                        .toDouble()
                }

                // Available limit
                val availableRegex = Pattern.compile(
                    "Available Fuliza M-PESA limit is Ksh\\s*([0-9,]+\\.[0-9]{2})",
                    Pattern.CASE_INSENSITIVE
                )

                val availableMatcher = availableRegex.matcher(body)

                if (availableMatcher.find()) {

                    available = availableMatcher.group(1)
                        .replace(",", "")
                        .toDouble()

                    break
                }
            }
        }

        return com.manu.cash_lens.models.FulizaStatus(
            outstanding,
            available
        )
    }
}