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
                    val transaction = Transaction(
                        amount = 0.0,
                        recipient = sender,
                        date = "",
                        type = "Unknown"
                    )

                    messages.add(transaction)
                }
            }
        }

        return messages
    }
}