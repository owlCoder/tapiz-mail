-keepattributes *Annotation*
-dontwarn okhttp3.**
-dontwarn retrofit2.**

# AndroidX Security Crypto (Tink) references errorprone annotations that aren't shipped as a
# runtime dependency — safe to ignore, they're compile-time-only annotations.
-dontwarn com.google.errorprone.annotations.**
