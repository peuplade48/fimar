# FİMAR ProGuard Kuralları

# WebView JavaScript arayüzü - obfuscation'dan koru
-keepclassmembers class com.fimar.MainActivity$JSBridge {
    @android.webkit.JavascriptInterface <methods>;
}
-keepattributes JavascriptInterface

# Kotlin Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.** { volatile <fields>; }

# Bluetooth Helper
-keep class com.fimar.BluetoothPrinterHelper { *; }
-keep class com.fimar.WhatsAppHelper { *; }

# AndroidX
-keep class androidx.** { *; }
-keep interface androidx.** { *; }

# Genel
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable
-keep public class * extends java.lang.Exception
