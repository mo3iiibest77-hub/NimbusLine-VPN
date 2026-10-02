plugins { id("com.android.application"); id("org.jetbrains.kotlin.android"); id("org.jetbrains.kotlin.plugin.serialization") }
android {
 namespace="com.nimbusline.vpn"; compileSdk=35
 defaultConfig { applicationId="com.nimbusline.vpn"; minSdk=24; targetSdk=35; versionCode=1; versionName="0.1.0" }
 buildFeatures { compose=true }
 composeOptions { kotlinCompilerExtensionVersion="1.5.15" }
 packaging { jniLibs.useLegacyPackaging=true; resources.excludes += "/META-INF/{AL2.0,LGPL2.1}" }
}
dependencies {
 implementation(files("libs/libv2ray.aar"))
 implementation(platform("androidx.compose:compose-bom:2024.12.01"))
 implementation("androidx.activity:activity-compose:1.10.0")
 implementation("androidx.compose.ui:ui")
 implementation("androidx.compose.ui:ui-tooling-preview")
 implementation("androidx.compose.material3:material3")
 implementation("androidx.work:work-runtime-ktx:2.10.0")
 implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
 implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
}
