package com.manu.cash_lens.sms

import android.content.Context
import android.provider.Telephony
import com.manu.cash_lens.models.Transaction
import android.util.Log
import java.util.regex.Pattern

class SmsReader(private val context: Context) {

    fun getMpesaMessages(): List<Transaction> {

        val messages = mutableListOf<Transaction>()

        val cursor = context.contentResolver.query(
            Telephony.Sms.Inbox.CONTENT_URI,
            arrayOf(
                Telephony.Sms.ADDRESS,
                Telephony.Sms.BODY
            ),
            null,
            null,
            "${Telephony.Sms.DATE} DESC"
        )

        cursor?.use {

            val addressIndex = it.getColumnIndex(
                Telephony.Sms.ADDRESS
            )

            val bodyIndex = it.getColumnIndex(
                Telephony.Sms.BODY
            )

            while (it.moveToNext()) {

                val sender = it.getString(addressIndex)
                val body = it.getString(bodyIndex)
                Log.d("MPESA_SMS", body)

                // Only keep M-Pesa messages

                if (
                    body.contains("Confirmed", true) &&
                    body.contains("New M-PESA balance", true) &&
                    body.contains("Ksh", true)

                ) {
                    val amountRegex = Pattern.compile("Ksh([0-9,]+\\.[0-9]{2})", Pattern.CASE_INSENSITIVE)

                    val matcher = amountRegex.matcher(body)

                    val amount = if (matcher.find()) {
                        matcher.group(1)
                            .replace(",", "")
                            .toDouble()
                    } else {
                        0.0
                    }
                    val type = when {
                        body.contains("sent to", ignoreCase = true) -> "Sent"
                        body.contains("received", ignoreCase = true) -> "Received"
                        body.contains("paid to", ignoreCase = true) -> "PayBill"
                        else -> "Other"
                    }
                    val recipient = when (type) {

                        "Sent" -> {
                            val regex = Pattern.compile(
                                "sent to (.+?) on",
                                Pattern.CASE_INSENSITIVE
                            )

                            val match = regex.matcher(body)

                            if (match.find()) match.group(1) else "Unknown"
                        }

                        "Received" -> {
                            val regex = Pattern.compile(
                                "from (.+?) on",
                                Pattern.CASE_INSENSITIVE
                            )

                            val match = regex.matcher(body)

                            if (match.find()) match.group(1) else "Unknown"
                        }

                        "PayBill" -> {
                            val regex = Pattern.compile(
                                "paid to (.+?)\\. on|paid to (.+?) on",
                                Pattern.CASE_INSENSITIVE
                            )

                            val match = regex.matcher(body)

                            if (match.find()) {
                                match.group(1) ?: match.group(2) ?: "Unknown"
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
                    val transaction = Transaction(
                        amount = amount,
                        recipient = recipient,
                        date = date,
                        time = time,
                        type = type
                    )

                    messages.add(transaction)
                }
            }
        }

        return messages
    }
}