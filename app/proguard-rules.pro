# Add project specific ProGuard rules here.
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
-keep class com.halmeoni.transit.data.api.** { *; }
-keep class com.halmeoni.transit.domain.model.** { *; }
