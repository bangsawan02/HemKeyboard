# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# Aggressive optimizations
-optimizationpasses 5
-allowaccessmodification
-dontpreverify

# Room rules
-keepclassmembers class * extends androidx.room.RoomDatabase {
    <init>(...);
}

# Keep Compose internal members
-keepclassmembers class androidx.compose.ui.platform.AndroidComposeView {
    *** onCheckIsTextEditor(...);
}

# General optimizations
-repackageclasses ''
-overloadaggressively

# Remove logging calls
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}

# Preserve important attributes
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# Keep Room generated code
-keep class * {
    @androidx.room.Dao *;
    @androidx.room.Entity *;
}
