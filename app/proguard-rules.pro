# -----------------------------------------------------------------------------
# ProGuard / R8 Rules for Ebook Reader (Production Readiness)
# -----------------------------------------------------------------------------

# Readium Kotlin Toolkit 3.3.0
-keep class org.readium.** { *; }
-dontwarn org.readium.**
-keepattributes *Annotation*,InnerClasses,EnclosingMethod,Signature,Exceptions

# Kotlin Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}

# Android Room 2.7.2
-keepclassmembers class * extends androidx.room.RoomDatabase {
    <init>();
}
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# OkHttp 4.12.0
-keepattributes Signature
-keepattributes *Annotation*
-keepclassmembers class okhttp3.internal.publicsuffix.PublicSuffixDatabase {
    private byte[] *;
}
-dontwarn okhttp3.**
-dontwarn okio.**

# DataStore & Protobuf
-dontwarn androidx.datastore.**
