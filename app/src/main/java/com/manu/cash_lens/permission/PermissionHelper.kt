package com.manu.cash_lens.permission

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class PermissionHelper(private val activity: Activity) {

    companion object {
        const val SMS_PERMISSION_CODE = 100
    }

    fun hasSmsPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            activity,
            Manifest.permission.READ_SMS
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun requestSmsPermission() {
        ActivityCompat.requestPermissions(
            activity,
            arrayOf(Manifest.permission.READ_SMS),
            SMS_PERMISSION_CODE
        )
    }
}