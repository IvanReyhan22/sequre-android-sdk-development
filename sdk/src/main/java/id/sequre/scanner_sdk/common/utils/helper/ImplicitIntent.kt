package id.sequre.scanner_sdk.common.utils.helper

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import id.sequre.scanner_sdk.R

object ImplicitIntent {
    fun navigateToAppSettings(context: Context) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
        }
        if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
        } else {
            Toast.makeText(context,
                context.getString(R.string.unable_to_open_app_settings), Toast.LENGTH_SHORT).show()
        }
    }
}