package id.sequre.scanner_sdk.common.utils.helper

import android.util.Log
import id.sequre.scanner_sdk.BuildConfig

object FlavorHelper {
    var currentFlavor = Flavor.fromFlavor(BuildConfig.FLAVOR)

    init {
        Log.d("FlavorHelper", "Current Flavor: $currentFlavor")
    }
}

enum class Flavor(val string: String) {
    PRODUCTION("production"),
    STAGING("staging"),
    DEV("dev");

    companion object {
        fun fromFlavor(flavor: String): Flavor? {
            return entries.find { it.string == flavor }
        }
    }

}