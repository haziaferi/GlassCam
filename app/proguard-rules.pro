# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in C:\Android/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# --- Data Binding insurance ---------------------------------------------------
# minifyEnabled is false today, so R8 does not run and none of this is active yet.
# These rules are cheap insurance so that flipping minifyEnabled to true later does
# not silently break the settings/data-binding UI: layout expressions invoke model
# getters reflectively by name, and the custom @BindingAdapter("setViewState") is
# referenced only from XML (no direct Java caller for R8 to see as a root).
-keepclassmembers class * {
    @androidx.databinding.BindingAdapter <methods>;
}
-keep class glasscam.cam.ui.binding.** { *; }
# Getters used in binding expressions on the settings model types.
-keepclassmembers class glasscam.cam.apis.basecamera.parameters.AbstractParameter {
    public *** get*();
}
-keepclassmembers class * implements glasscam.settings.mode.BooleanSettingModeInterface {
    public *** get();
}
