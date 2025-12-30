plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}
android {
    namespace = "com.example.csterminal"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.example.csterminal"
        minSdk = 27
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        aidl = true
    }
    sourceSets {
        getByName("main") {
            aidl.srcDir("src/main/java/aidl")
        }
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "META-INF/DEPENDENCIES"
            excludes += "META-INF/LICENSE"
            excludes += "META-INF/LICENSE.txt"
            excludes += "META-INF/license.txt"
            excludes += "META-INF/NOTICE"
            excludes += "META-INF/NOTICE.txt"
            excludes += "META-INF/notice.txt"
            excludes += "META-INF/ASL2.0"
            excludes += "META-INF/FastDoubleParser-LICENSE"
            excludes += "META-INF/FastDoubleParser-NOTICE"
            excludes += "META-INF/io.netty.versions.properties"
            excludes += "META-INF/INDEX.LIST"
            excludes += "META-INF/DISCLAIMER"
            excludes += "mozilla/public-suffix-list.txt"
            excludes += "org/bouncycastle/x509/CertPathReviewerMessages_de.properties"
            excludes += "org/bouncycastle/x509/CertPathReviewerMessages.properties"
        }
    }
    configurations.all {
        resolutionStrategy {
            force("org.bouncycastle:bcprov-jdk15on:1.70")
            dependencySubstitution {
                substitute(module("org.bouncycastle:bcprov-jdk15to18")).using(module("org.bouncycastle:bcprov-jdk15on:1.70"))
                substitute(module("com.google.guava:listenablefuture:1.0")).using(module("com.google.guava:guava:31.1-android"))
            }
            force("com.google.guava:guava:31.1-android")
        }
    }
}
tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
}
dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material3.adaptive.navigation.suite)
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.2")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.zxing:core:3.4.1")
    implementation("com.journeyapps:zxing-android-embedded:4.3.0")
    implementation("org.apache.httpcomponents.client5:httpclient5:5.4.1")
    implementation(libs.zxing.core)
    implementation(libs.firebase.crashlytics.buildtools)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    implementation("org.bitcoindevkit:bdk-android:0.30.0")
    implementation("org.web3j:core:4.12.3") {
        exclude(group = "org.bouncycastle")
    }
    implementation(libs.kotlin.bip39)
    implementation("org.bitcoinj:bitcoinj-core:0.16.2")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.bouncycastle:bcprov-jdk15on:1.70")
    implementation(libs.xrpl4j.client)
    implementation(libs.xrpl4j.model)
    implementation(libs.xrpl4j.crypto.bouncycastle)
    implementation(libs.xrpl4j.address.codec)
    implementation(libs.lottie.compose)
}
