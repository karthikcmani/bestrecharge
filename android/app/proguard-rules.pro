# Proguard rules for BestRecharge
-keep class com.bestrecharge.app.data.models.** { *; }
-keep class com.bestrecharge.app.data.local.entities.** { *; }
-keepclassmembers class * {
    @androidx.room.* <fields>;
    @androidx.room.* <methods>;
}
