# Add project specific ProGuard rules here.

# AdMob (por si activas minify en release)
-keep class com.google.android.gms.ads.** { *; }
-dontwarn com.google.android.gms.ads.**
