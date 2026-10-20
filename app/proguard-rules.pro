# Keep Android components referenced in AndroidManifest
-keep public class com.example.BridgeApplication { *; }
-keep public class com.example.MainActivity { *; }
-keep public class com.example.telecom.BridgeConnectionService { *; }
-keep public class com.example.telecom.BridgeCallRedirectionService { *; }
-keep public class com.example.autoselect.BrilliantOutAccessibilityService { *; }
-keep public class com.example.telecom.BootReceiver { *; }
-keep public class com.example.handoff.HandoffActionReceiver { *; }

-keepclassmembers class * {
    @androidx.annotation.Keep <fields>;
    @androidx.annotation.Keep <methods>;
}

-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
