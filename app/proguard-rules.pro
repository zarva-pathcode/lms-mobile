# Preserve line numbers for crash debugging
-keepattributes SourceFile,LineNumberTable
-keepattributes *Annotation*, InnerClasses, EnclosingMethod, Signature

# === kotlinx.serialization ===
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.myschedule.**$$serializer { *; }
-keepclassmembers class com.myschedule.** {
    *** Companion;
}
-keepclasseswithmembers class com.myschedule.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# === Firebase Firestore ===
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.firebase.**
-dontwarn com.google.android.gms.**

# === Supabase / Ktor ===
-keep class io.github.jan.supabase.** { *; }
-keep class io.ktor.** { *; }
-dontwarn io.github.jan.supabase.**
-dontwarn io.ktor.**

# === Coil ===
-keep class coil.** { *; }
-dontwarn coil.**

# === Navigation Compose (data class args) ===
-keepclassmembers class * extends androidx.navigation.NavArgs { *; }
-keepclassmembers class * extends androidx.navigation.NavType { *; }

# === Model / Data classes (prevent obfuscation) ===
-keep class com.myschedule.id.data.** { *; }
-keep class com.myschedule.id.SessionManager { *; }

# === Keep serializable/parcelable ===
-keepclassmembers class * implements java.io.Serializable { *; }
-keepclassmembers class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator CREATOR;
}