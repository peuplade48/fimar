# FİMAR / Apartman Takip ProGuard Kuralları

# WebView JavaScript arayüzü - Silinmesini engelle
-keepclassmembers class com.apartmantakip.MainActivity$JSBridge {
    @android.webkit.JavascriptInterface <methods>;
}

# Paket ismini koru
-keep class com.apartmantakip.** { *; }

-keepattributes JavascriptInterface
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable

# Kotlin Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.** { volatile <fields>; }

# Helper sınıflarını koru
-keep class com.apartmantakip.BluetoothPrinterHelper { *; }
-keep class com.apartmantakip.WhatsAppHelper { *; }

# AndroidX
-keep class androidx.** { *; }
-keep interface androidx.** { *; }

# Google/JSON
-keep class org.json.** { *; }
-keep class com.google.** { *; }
