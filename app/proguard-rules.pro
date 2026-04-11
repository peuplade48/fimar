# Apartman Takip ProGuard Kuralları

# WebView ve JavaScript Arayüzünü koru
-keepattributes JavascriptInterface
-keepattributes *Annotation*

# JSBridge sınıfını ve metodlarını tamamen koru
-keepclassmembers class com.example.apartmantakip.MainActivity$WebAppInterface {
    @android.webkit.JavascriptInterface <methods>;
}
-keep class com.example.apartmantakip.MainActivity$WebAppInterface { *; }

# WebView metodlarını koru
-keepclassmembers class android.webkit.WebView {
   public *;
}

# Kotlin ve Coroutines
-keep class kotlinx.coroutines.** { *; }
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# Google ve JSON
-keep class org.json.** { *; }
-keep class com.google.** { *; }
