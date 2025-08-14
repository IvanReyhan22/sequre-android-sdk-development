# Uncomment this to preserve the line number information for
# debugging stack traces.
-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

-dontwarn java.lang.invoke.StringConcatFactory
-keep class id.sequre.scanner_sdk.data.remote.response.**{ *; }
-keepclassmembers class id.sequre.scanner_sdk.data.remote.response.** {
    public static <fields>;
}

# Keep all public classes and methods (so the base app can access them)
-keep class id.sequre.scanner_sdk.** { public *; }
-keep class androidx.lifecycle.ViewModel { *; }
-keep class id.sequre.scanner_sdk.**ViewModel { public *; }

# Keep Jetpack Compose, ViewModel, Coroutines, and Android classes
-keep class androidx.compose.** { *; }
-keep class androidx.lifecycle.** { *; }
-keep class kotlinx.coroutines.** { *; }
-keep class android.graphics.** { *; }

-keepclassmembers class id.sequre.scanner_sdk.screens.scanner.ScannerViewKt {
   public *;
}
-keepclassmembers class id.sequre.scanner_sdk.screens.scanner.defaults.ScannerViewColors {
   public *;
}
-keepclassmembers class ** {
    static <methods>;
}

# Keep Sealed Classes (Prevents Runtime Issues)
-keep class id.sequre.scanner_sdk.screens.scanner.enums.ScannerState { *; }
-keep class id.sequre.scanner_sdk.screens.scanner.enums.ScannerState$* { *; }