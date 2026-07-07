-keepattributes *Annotation*
-dontwarn okhttp3.**
-dontwarn retrofit2.**

# AndroidX Security Crypto (Tink) references errorprone annotations that aren't shipped as a
# runtime dependency — safe to ignore, they're compile-time-only annotations.
-dontwarn com.google.errorprone.annotations.**

# JavaMail (com.sun.mail:android-mail) resolves Store/Transport provider implementations via
# `META-INF/javamail.providers` + reflection (Class.forName) inside javax.mail.Session, not via
# any direct compile-time reference R8 can see — without these keep rules, R8 strips/renames
# the provider classes in release builds and every connect() fails with
# NoSuchProviderException("imaps")/("imap")/("smtp") even though the exact same account works
# fine in a debug (unminified) build.
-keep class com.sun.mail.** { *; }
-keep class javax.mail.** { *; }
-keep class com.sun.activation.** { *; }
-keepnames class com.sun.mail.** { *; }
-dontwarn com.sun.mail.**
-dontwarn javax.mail.**
