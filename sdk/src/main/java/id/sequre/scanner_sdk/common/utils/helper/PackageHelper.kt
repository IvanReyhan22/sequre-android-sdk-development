package id.sequre.scanner_sdk.common.utils.helper

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build

object PackageHelper {

    fun getVersionName(context: Context): String {
        return try {
            val packageManager = context.packageManager
            val packageName = context.packageName
            val packageInfo = packageManager.getPackageInfo(packageName, 0)
            @Suppress("DEPRECATION")
            val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.longVersionCode
            } else {
                packageInfo.versionCode
            }
            val versionName = packageInfo.versionName
            /// failsafe
            val version = "$versionCode - $versionName"
            version
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
            "Unknown"
        }
    }

    fun isSystemHasFlashFeature(context: Context): Boolean {
        return context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)
    }
}