# Keep R8 line numbers for crash reporting
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Keep Nocturne Audio Data Models for Firestore & JSON deserialization
-keepclassmembers class com.nocturne.audio.model.** {
    <fields>;
    <init>(...);
}

# Firebase & App Check rules
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**

# Media3 ExoPlayer & Session rules
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# Coil Image Loader
-keep class coil.** { *; }
-dontwarn coil.**

# Credential Manager
-keep class androidx.credentials.** { *; }
-dontwarn androidx.credentials.**
