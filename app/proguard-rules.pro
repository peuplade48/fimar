<<<<<<< HEAD
# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile
=======
# Apartman Takip ProGuard Kuralları

# WebView ve JavaScript Arayüzünü koru
-keepattributes JavascriptInterface
-keepattributes *Annotation*

# JSBridge sınıfını ve metodlarını tamamen koru
-keepclassmembers class com.apartmantakip.MainActivity$JSBridge {
    @android.webkit.JavascriptInterface <methods>;
}
-keep class com.apartmantakip.MainActivity$JSBridge { *; }

# WebView metodlarını koru
-keepclassmembers class android.webkit.WebView {
   public *;
}

# Tüm projeyi obfuscation'dan koru (Hata payını sıfıra indirmek için)
-keep class com.apartmantakip.** { *; }

# Kotlin ve Coroutines
-keep class kotlinx.coroutines.** { *; }
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# Google ve JSON
-keep class org.json.** { *; }
-keep class com.google.** { *; }
>>>>>>> 15eeeb48745c5cd9f26e5738dbf9ad2f0f1bebf1
