-keep class dev.abstra.pro.data.model.** { *; }
-keep class dev.abstra.pro.data.db.entities.** { *; }
-keep class dev.abstra.pro.ai.model.** { *; }
-keep class dev.abstra.pro.lyrics.model.** { *; }

-keep class org.jaudiotagger.** { *; }
-dontwarn org.jaudiotagger.**

-keep class com.google.gson.** { *; }
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

-keep class android.media.audiofx.** { *; }
