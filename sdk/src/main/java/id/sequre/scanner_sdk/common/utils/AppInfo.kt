package id.sequre.scanner_sdk.common.utils

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import java.security.MessageDigest

object AppInfo {
    fun getAppSHA(context: Context): String? {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val info = context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.GET_SIGNING_CERTIFICATES
                ).signingInfo

                if (info == null) return null

                return if (info.hasMultipleSigners()) {
                    toHex(info.apkContentsSigners[0].toByteArray())
                } else {
                    toHex(info.signingCertificateHistory[0].toByteArray())
                }

            } else {
                @Suppress("DEPRECATION")
                val info = context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.GET_SIGNATURES
                )
                @Suppress("DEPRECATION")
                val signatures = info.signatures

                if (signatures == null) return null
                return toHex(signatures[0].toByteArray())
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    private fun toHex(array: ByteArray): String? {
        return try {
            val md = MessageDigest.getInstance("SHA")
            md.update(array)
            md.digest()
                .mapIndexed { index, byte ->
                    String.format("%02X", byte) +
                            if (index < md.digest().size - 1) ":" else ""
                }
                .joinToString("")
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}