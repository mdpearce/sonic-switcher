# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.kts.
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
-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# ========================================
# FFmpegKit ProGuard Rules
# ========================================

# Keep all native methods (JNI)
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep FFmpegKit classes and native methods
-keep class com.arthenica.ffmpegkit.** { *; }
-keep interface com.arthenica.ffmpegkit.** { *; }

# Keep native callback methods
-keepclassmembers class com.arthenica.ffmpegkit.** {
    native <methods>;
    public <init>(...);
}

# Preserve annotations for FFmpegKit
-keepattributes *Annotation*

# ========================================
# Hilt/Dagger ProGuard Rules
# ========================================

# Keep Hilt generated classes
-keep class dagger.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }

# Keep injected constructors
-keepclasseswithmembers class * {
    @javax.inject.Inject <init>(...);
}

# Keep Hilt Android entry points
-keep @dagger.hilt.android.lifecycle.HiltViewModel class * extends androidx.lifecycle.ViewModel {
    public <init>(...);
}

# ========================================
# Room Database ProGuard Rules
# ========================================

# Keep Room classes
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# ========================================
# Kotlin Coroutines ProGuard Rules
# ========================================

# ServiceLoader support for coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# Most of volatile fields are updated with AFU and should not be mangled
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# ========================================
# Jetpack Compose ProGuard Rules
# ========================================

# Keep Compose runtime
-keep class androidx.compose.runtime.** { *; }
-keep class androidx.compose.ui.** { *; }

# Keep @Composable functions
-keep @androidx.compose.runtime.Composable class * { *; }
-keepclassmembers class * {
    @androidx.compose.runtime.Composable <methods>;
}

# ========================================
# Firebase ProGuard Rules  
# ========================================

# Keep Firebase classes
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }

# ========================================
# General Android ProGuard Rules
# ========================================

# Keep Serializable classes
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# Keep Parcelable implementations
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

# Preserve line numbers for debugging
-renamesourcefileattribute SourceFile
-keepattributes SourceFile,LineNumberTable
