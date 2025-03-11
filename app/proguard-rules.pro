# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# Optimization and obfuscation rules
-optimizationpasses 8
-dontusemixedcaseclassnames
-dontskipnonpubliclibraryclasses
-dontpreverify
-verbose
-optimizations !code/simplification/arithmetic,!field/*,!class/merging/*

# Keep the application class and main activity
-keep class com.noxpeteam.txpacker.MainActivity { *; }
-keep class com.noxpeteam.txpacker.SettingsActivity { *; }

# Keep all native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep all classes in the support library
-keep class androidx.** { *; }
-keep interface androidx.** { *; }
-keep class com.google.android.material.** { *; }
-dontwarn androidx.**
-dontwarn com.google.android.material.**

# Keep security libraries
-keep class net.zetetic.** { *; }
-keep class com.google.crypto.** { *; }
-dontwarn net.zetetic.**
-dontwarn com.google.crypto.**

# Keep all model classes
-keep class com.noxpeteam.txpacker.models.** { *; }

# Keep all enum classes
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Keep all Parcelable classes
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

# Keep all Serializable classes
-keepnames class * implements java.io.Serializable
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    !static !transient <fields>;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# Remove logging for production
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}

# Additional protection against reverse engineering
-flattenpackagehierarchy 'com.noxpeteam.txpacker'
-repackageclasses 'com.noxpeteam.txpacker'
-allowaccessmodification

# Prevent class name leakage
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes Exceptions

# Protect against decompilation
-obfuscationdictionary dictionary.txt
-classobfuscationdictionary dictionary.txt
-packageobfuscationdictionary dictionary.txt

# Protect native libraries and core classes
-keep class com.noxpeteam.txpacker.PermissionManager { *; }
-keep class com.noxpeteam.txpacker.Logger { *; }

# Prevent reflection-based attacks
-keep class * extends java.lang.annotation.Annotation { *; }
-keepattributes RuntimeVisibleAnnotations
-keepattributes RuntimeInvisibleAnnotations

# Prevent stack trace leakage
-renamesourcefileattribute SourceFile
-keepattributes SourceFile,LineNumberTable,Exceptions

# Protect against string analysis
-adaptclassstrings
-adaptresourcefilenames
-adaptresourcefilecontents

# Additional native code protection
-keepclasseswithmembers class * {
    native <methods>;
}

# Optimize native calls
-keep,includedescriptorclasses class com.noxpeteam.txpacker.NativeTextProvider {
    native <methods>;
}

# Prevent native library extraction
-keep class com.noxpeteam.txpacker.NativeTextProvider$Companion {
    private static <fields>;
}