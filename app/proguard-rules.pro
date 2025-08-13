# EventBus
-keepattributes *Annotation*
-keepclassmembers class ** {
    @org.greenrobot.eventbus.Subscribe <methods>;
}
-keep enum org.greenrobot.eventbus.ThreadMode { *; }

# Facebook Nullsafe annotations (optional)
-dontwarn com.facebook.infer.annotation.**
-keep class com.facebook.infer.annotation.** { *; }

# IronSource (only if you’re not shipping it)
-dontwarn com.ironsource.**
# If R8 still keeps some mediation paths, you may need:
# -keep class com.ironsource.** { *; }
# Keep AdMob ads
-keep public class com.google.android.gms.ads.** {
    public *;
}
-keep class com.google.ads.** {
    *;
}
-keep class com.google.android.gms.internal.** {
    *;
}

# Required for mediation adapters
-keep class * extends java.lang.annotation.Annotation
-keep class com.tappx.** { *; }
-keep interface com.tappx.** { *; }
-dontwarn com.google.android.gms.ads.**
-dontwarn com.tappx.**

# Gson specific rules
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapter
-keep class * extends com.google.gson.TypeAdapter
-keep class * extends com.google.gson.reflect.TypeToken

-keepattributes Signature
-keep class com.hkapps.messagepro.model.** { *; }

-keepclasseswithmembernames,includedescriptorclasses class * {
    native <methods>;
}

#Attributes
-keepattributes Signature
-keepattributes Annotation
-keepattributes EnclosingMethod
-keepattributes InnerClasses
-keepattributes Exceptions
-keepattributes SourceFile,LineNumberTable

#Specifies not to ignore non-public library classes.
-dontskipnonpubliclibraryclasses

#Specifies not to ignore package visible library class members
-dontskipnonpubliclibraryclassmembers

#Specifies to print any warnings about unresolved references and other important problems, but to continue processing in any case.
-ignorewarnings

-optimizationpasses 5
