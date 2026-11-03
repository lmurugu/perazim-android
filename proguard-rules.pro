# Perazim Android R8/ProGuard rules

# Room Database
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao class *
-dontwarn androidx.room.paging.**

# Annotations and generic signatures
-keepattributes Signature, *Annotation*, InnerClasses, EnclosingMethod

# Local persistence entities and domain models
-keep class org.perazimchurch.app.data.local.entity.** { *; }
-keep class org.perazimchurch.app.domain.model.** { *; }

# Enums
-keepclassmembers enum org.perazimchurch.app.** {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# QR / ZXing
-keep class com.google.zxing.** { *; }
