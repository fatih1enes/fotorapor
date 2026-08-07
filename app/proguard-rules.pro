# ===========================
# PhotoReport - ProGuard/R8 Rules
# Production-ready configuration
# ===========================

# Keep line numbers for crash reports
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Keep annotations (Hilt, Room, etc. rely on them)
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes InnerClasses,EnclosingMethod
-keepattributes RuntimeVisibleAnnotations,RuntimeVisibleParameterAnnotations

# ===========================
# Hilt / Dagger & Room & Coroutines & Firebase
# Note: These libraries provide consumer ProGuard rules in their AARs.
# We only keep the -dontwarn or specific rules not covered by defaults.
# ===========================
-dontwarn dagger.hilt.**
-dontwarn androidx.room.**
-dontwarn kotlinx.coroutines.**
-dontwarn com.google.firebase.**

# ===========================
# CameraX / AndroidX
# ===========================
-dontwarn androidx.camera.**
-dontwarn androidx.work.**
-dontwarn androidx.startup.**
-dontwarn androidx.lifecycle.**

# ===========================
# Compose Navigation
# We keep this for safe-args or reflection-based route handling
# ===========================
-keep class androidx.navigation.compose.** { *; }

# ===========================
# Okio (used by Coil)
# ===========================
-dontwarn okio.**

# ===========================
# General Android
# ===========================
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    !static !transient <fields>;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# ===========================
# AVIF Coder (JNI & Reflection used)
# ===========================
-keep class com.radzivon.bartoshyk.avif.** { *; }
