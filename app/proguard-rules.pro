# ===========================
# Elektrik - ProGuard/R8 Rules
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
# Hilt / Dagger
# ===========================
-dontwarn dagger.hilt.**
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }
# Keep Hilt generated components
-keep class **_HiltModules* { *; }
-keep class **_Factory { *; }
-keep class **_MembersInjector { *; }
-keep class * implements dagger.hilt.internal.GeneratedComponent { *; }
-keep class * implements dagger.hilt.internal.GeneratedComponentManager { *; }
# Keep @HiltAndroidApp, @AndroidEntryPoint, @HiltViewModel, @HiltWorker annotated classes
-keep @dagger.hilt.android.HiltAndroidApp class * { *; }
-keep @dagger.hilt.android.AndroidEntryPoint class * { *; }
-keep @dagger.hilt.android.lifecycle.HiltViewModel class * { *; }
-keep @androidx.hilt.work.HiltWorker class * { *; }
# Keep @Inject annotated constructors
-keepclassmembers class * {
    @javax.inject.Inject <init>(...);
}
-keepclassmembers class * {
    @dagger.assisted.AssistedInject <init>(...);
}

# ===========================
# Room
# ===========================
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-keepclassmembers class * {
    @androidx.room.Query <methods>;
    @androidx.room.Insert <methods>;
    @androidx.room.Update <methods>;
    @androidx.room.Delete <methods>;
    @androidx.room.Transaction <methods>;
}
-dontwarn androidx.room.**

# ===========================
# Kotlin Coroutines
# ===========================
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}
-keep class kotlin.coroutines.Continuation
-dontwarn kotlinx.coroutines.**

# ===========================
# Coil & Media3
# Note: Consumer rules are provided by their respective AARs.
# ===========================

# ===========================
# CameraX
# ===========================
-keep class androidx.camera.** { *; }
-dontwarn androidx.camera.**

# ===========================
# AndroidX / Jetpack
# ===========================
-keep class androidx.work.** { *; }
-dontwarn androidx.work.**
-keep class androidx.startup.** { *; }
-dontwarn androidx.startup.**
-keep class androidx.lifecycle.** { *; }
-dontwarn androidx.lifecycle.**

# ===========================
# Compose
# ===========================
-dontwarn androidx.compose.**

# ===========================
# Compose Navigation
# ===========================
-keep class androidx.navigation.compose.** { *; }
-keep class androidx.navigation.** { *; }

# ===========================
# Okio (used by Coil)
# ===========================
-dontwarn okio.**
-keep class okio.** { *; }

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