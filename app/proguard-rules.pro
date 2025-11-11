# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.kts.

# Keep Compose classes
-keep class androidx.compose.** { *; }
-keep class kotlin.** { *; }

# Keep game classes
-keep class com.tetris.game.** { *; }
-keep class com.tetris.ui.** { *; }

-dontwarn javax.annotation.**
-dontwarn kotlin.Unit
