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
# --- MSAL and security providers ---
-keep class com.microsoft.** { *; }
-keep class org.bouncycastle.** { *; }
-keep class com.google.crypto.tink.** { *; }
-keep class com.google.auto.value.** { *; }
-keep class edu.umd.cs.findbugs.annotations.** { *; }

-dontwarn org.bouncycastle.**
-dontwarn com.google.crypto.tink.**
-dontwarn com.microsoft.**

# Keep MSAL (Microsoft Authentication Library) classes
-keep class com.microsoft.identity.** { *; }
-dontwarn com.microsoft.identity.**

# Keep gson (used internally by MSAL)
-keep class com.google.gson.** { *; }
-dontwarn com.google.gson.**

# Keep classes used for reflection
-keepattributes Signature
-keepattributes *Annotation*

# Prevent warnings from okhttp and okio
-dontwarn okhttp3.**
-dontwarn okio.**

# (Optional) Keep your auth config JSON resources
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
