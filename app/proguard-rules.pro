# Proguard rules for PicoTTS-Pro
-keep class com.rme.picottspro.** { *; }
-keep class com.svox.pico.** { *; }
-keep class com.android.tts.compat.** { *; }

# Keep annotations and reflection metadata
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# Suppress warnings from unused third party references
-dontwarn **
