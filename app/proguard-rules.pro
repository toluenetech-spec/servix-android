# ---------------------------------------------------------------------------
# Servix R8 rules (release build). Every rule below exists because a library
# reads classes/annotations reflectively; deleting them breaks the app silently.
# ---------------------------------------------------------------------------

# Keep our own line numbers useful in Play Console crash reports without
# disabling obfuscation of the app code itself.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# --- kotlinx.serialization ---------------------------------------------------
# The plugin generates a synthetic `Companion.serializer()` and a `$serializer`
# class per @Serializable type; R8 otherwise strips them as unreachable because
# they are only called reflectively through `serializer<T>()`.
-keepattributes *Annotation*, InnerClasses, Signature, RuntimeVisibleAnnotations
-keepclassmembers @kotlinx.serialization.Serializable class ** {
    *** Companion;
    *** INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}
-keepclasseswithmembers class ** {
    kotlinx.serialization.KSerializer serializer(...);
}
# Keep the generated serializer classes and their constructors.
-keep,allowobfuscation class **$$serializer {
    *;
}
-keep,includedescriptorclasses class ng.name.servix.app.data.model.**$$serializer { *; }
# Enums are serialized by name.
-keepclassmembers enum * {
    java.lang.Enum[] $VALUES;
    *;
}
-keepclassmembers class ng.name.servix.app.data.model.** {
    <fields>;
}

# --- Retrofit ----------------------------------------------------------------
# Retrofit builds the service implementation with dynamic proxies and reads
# annotations + generic signatures from the interface methods.
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations, AnnotationDefault, Signature, Exceptions
-keep,allowobfuscation,allowshrinking interface ng.name.servix.app.data.network.ServixApiService
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation
-keepclasseswithmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-dontwarn retrofit2.**
-dontwarn javax.annotation.**
-keep class kotlin.Metadata { *; }
-dontwarn org.codehaus.mojo.animal_sniffer.**

# --- OkHttp --------------------------------------------------------------
# Platform-specific optional integrations that we never ship.
-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
-dontwarn org.slf4j.**
-dontwarn org.graalvm.**
-dontwarn sun.misc.**
-dontwarn com.google.errorprone.annotations.**
-dontwarn kotlinx.serialization.internal.**

# --- Coil ----------------------------------------------------------------
-dontwarn coil.**
-keep class coil.Coil { *; }

# --- WorkManager / notifications -----------------------------------------
# The worker class name is stored in the WorkManager database and re-instantiated
# by reflection, so it must survive shrinking with a stable name.
-keep class ng.name.servix.app.notifications.NotificationsWorker { *; }

# --- Misc: keep data classes' names for readable logs ---------------------
-keepnames class ng.name.servix.app.data.model.** { *; }
