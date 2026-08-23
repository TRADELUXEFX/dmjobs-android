# ProGuard rules for DM Jobs App

# Keep application classes
-keep class com.dmjobs.app.** { *; }
-keep class com.dmjobs.app.data.** { *; }
-keep class com.dmjobs.app.ui.** { *; }
-keep class com.dmjobs.app.service.** { *; }

# Supabase/Postgrest libraries
-keep class io.github.jan.supabase.** { *; }
-keep class io.github.jan.supabase.auth.** { *; }
-keep class io.github.jan.supabase.postgrest.** { *; }
-keep class io.github.jan.supabase.realtime.** { *; }

# Ktor client
-keep class io.ktor.** { *; }
-keep class io.ktor.client.** { *; }

# Kotlin serialization
-keepattributes *Annotation*
-keepattributes Signature
-keepclassmembers class * {
    @kotlin.serialization.Serializable <init>(...);
}
-keep,allowobfuscation @interface kotlinx.serialization.Serializable
-keep class kotlinx.serialization.** { *; }
-keepclasseswithmembers class **.*KotlinSerializationPluginKt { *; }

# Kotlin coroutines
-keep class kotlinx.coroutines.** { *; }
-keepclasseswithmembernames class kotlinx.coroutines.** { native <methods>; }

# Android/AndroidX
-keep class android.** { *; }
-keep class androidx.** { *; }
-keepclasseswithmembernames class androidx.** { native <methods>; }

# View binding
-keep class **.databinding.** { *; }

# Parcelable
-keep class * implements android.os.Parcelable { *; }

# Keep enum values/fields
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Remove logging
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}

# Preserve line numbers for crash reports
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Keep custom exceptions
-keep class com.dmjobs.app.** extends java.lang.Exception { *; }
