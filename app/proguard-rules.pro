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

# MSAL - Ignore optional dependencies
-dontwarn org.bouncycastle.**
-dontwarn com.google.crypto.tink.**
-dontwarn edu.umd.cs.findbugs.annotations.**
-dontwarn com.google.auto.value.**
-dontwarn com.microsoft.device.display.**

# OpenTelemetry - Ignore optional dependencies
-dontwarn io.opentelemetry.**

# Keep MSAL classes
-keep class com.microsoft.identity.** { *; }
-keep class com.microsoft.identity.client.** { *; }