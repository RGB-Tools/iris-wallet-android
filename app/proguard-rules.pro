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
-dontwarn java.awt.*
-keep class com.sun.jna.* { *; }
-keep class org.rgbtools.* { *; }
-keepclassmembers class * extends com.sun.jna.* { public *; }
-keep class com.google.api.services.drive.** { *; }
-keepclassmembers class * {
  @com.google.api.client.util.Key <fields>;
}
-keepclassmembers class * {
  @com.google.api.client.util.Value <fields>;
}
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation
-keep,allowobfuscation,allowshrinking class retrofit2.Response

# Google Play Services Auth (older libraries)
-keep class com.google.android.gms.auth.** { *; }
-keep class com.google.android.gms.common.** { *; }
-keep class com.google.android.gms.signin.** { *; }
-keep class com.google.api.client.googleapis.extensions.android.gms.auth.** { *; }
-keep class com.google.api.client.googleapis.auth.oauth2.GoogleCredential { *; }
-keep public class com.google.api.client.googleapis.auth.oauth2.GoogleIdToken
-keep public class com.google.api.client.googleapis.auth.oauth2.GoogleIdToken$Payload {
    public <init>();
    *;
}

# Credential Manager API (newer library that the app actually uses)
-keep class androidx.credentials.** { *; }
-keep interface androidx.credentials.** { *; }
-keepclassmembers class androidx.credentials.** {
    public <methods>;
    public <fields>;
}

# Google ID Library (newer library that the app actually uses)
-keep class com.google.android.libraries.identity.googleid.** { *; }
-keep interface com.google.android.libraries.identity.googleid.** { *; }
-keepclassmembers class com.google.android.libraries.identity.googleid.** {
    public <methods>;
    public <fields>;
}

# Keep Google ID Token classes and their constructors
-keep class com.google.android.libraries.identity.googleid.GoogleIdTokenCredential {
    public <init>(...);
    public <methods>;
}
-keep class com.google.android.libraries.identity.googleid.GetGoogleIdOption {
    public <init>(...);
    public <methods>;
}
-keep class com.google.android.libraries.identity.googleid.GetGoogleIdOption$Builder {
    public <init>(...);
    public <methods>;
}

-keepattributes Signature
-keepattributes InnerClasses
-keepattributes EnclosingMethod

-keep public class com.google.api.client.json.webtoken.JsonWebSignature$Header {
    public <init>();
    *;
}
