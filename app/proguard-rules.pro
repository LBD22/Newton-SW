# Newton Field App — R8 keep rules.
#
# Most dependencies (Hilt, Room, Compose, OkHttp, Timber) ship `consumer-rules.pro`
# inside their AAR, so we only keep what is NOT covered there: kotlinx.serialization
# (compiler-plugin synthesised members), kabeja (Java reflection for DXF handlers),
# and OSMDroid (resource lookup by name).

# ─── kotlinx.serialization ────────────────────────────────────────────────
# Canonical rules from the kotlinx.serialization README. Keeps the synthetic
# Companion + serializer() members the compiler plugin emits so they survive
# R8 shrinking. Without these every @Serializable class throws
# `SerializationException: Serializer for class X is not found` in release.

-keepattributes RuntimeVisibleAnnotations,AnnotationDefault,InnerClasses

-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1> {
    static <1>$Companion Companion;
}

-if @kotlinx.serialization.Serializable class ** {
    static **$* *;
}
-keepclassmembers class <2>$<3> {
    kotlinx.serialization.KSerializer serializer(...);
}

-if @kotlinx.serialization.Serializable class ** {
    public static ** INSTANCE;
}
-keepclassmembers class <1> {
    public static <1> INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}

# Polymorphic serialization reads annotations at runtime.
-keep,includedescriptorclasses class kotlinx.serialization.json.** { *; }

# ─── kabeja (DXF reader/writer) ───────────────────────────────────────────
# kabeja loads DXF entity handlers via reflection through org.xml.sax. R8
# strips the handler classes otherwise; DXF import then yields empty drawings.
-keep class org.kabeja.** { *; }
-dontwarn org.kabeja.**
-dontwarn org.apache.batik.**
-dontwarn org.apache.fop.**
-dontwarn javax.xml.**
-dontwarn org.w3c.dom.svg.**

# ─── OSMDroid ─────────────────────────────────────────────────────────────
# Tile sources and SharedPreferences keys are looked up by class/name.
-keep class org.osmdroid.** { *; }
-dontwarn org.osmdroid.**

# ─── Conservative line-number retention for crash reports ─────────────────
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
