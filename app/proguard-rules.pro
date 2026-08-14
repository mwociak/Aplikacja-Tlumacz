# R8/ProGuard configuration for Tłumacz EN-PL

# General R8 settings
-ignorewarnings
-keepattributes Signature, InnerClasses, AnnotationDefault, RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations, RuntimeVisibleDeclarations

# Retrofit 2
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepattributes Signature, InnerClasses

# OkHttp 3 / Okio
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase

# Moshi
-dontwarn com.squareup.moshi.**
-keep class com.squareup.moshi.** { *; }
-keep @com.squareup.moshi.JsonQualifier interface *

# Kotlin Coroutines
-dontwarn kotlinx.coroutines.**
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.coroutines.** {
    volatile <fields>;
}

# Hilt / Dagger
-dontwarn dagger.internal.DoubleCheck
-dontwarn hilt_aggregated_deps.**
-dontwarn javax.annotation.**

# Google / ErrorProne
-dontwarn com.google.errorprone.annotations.**

# JSR305
-dontwarn javax.annotation.**
