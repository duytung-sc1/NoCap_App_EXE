import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

val releaseKeystoreFile = rootProject.file("keystore.properties")
val releaseKeystore = Properties().apply {
    if (releaseKeystoreFile.isFile) {
        releaseKeystoreFile.inputStream().use(::load)
    }
}
val hasReleaseSigning = releaseKeystoreFile.isFile

android {
    namespace = "com.nocap.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.nocap.app"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"
        buildConfigField("String", "BACKEND_BASE_URL", "\"https://nocap-ebook-api.buiminhhien001.workers.dev\"")
        resValue("string", "default_web_client_id", "847491126060-3dikoskpsf80ibrnf799tivmpe8vj2bn.apps.googleusercontent.com")

        val proProduct = providers.gradleProperty("PLAY_PRO_PRODUCT_ID").orElse("").get()
        require(proProduct.matches(Regex("[a-zA-Z0-9._-]*")))
        buildConfigField("String", "PLAY_PRO_PRODUCT_ID", "\"$proProduct\"")
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = file(requireNotNull(releaseKeystore.getProperty("storeFile")))
                storePassword = requireNotNull(releaseKeystore.getProperty("storePassword"))
                keyAlias = requireNotNull(releaseKeystore.getProperty("keyAlias"))
                keyPassword = requireNotNull(releaseKeystore.getProperty("keyPassword"))
            }
        }
    }

    buildTypes {
        debug {
            buildConfigField("String", "BACKEND_BASE_URL", "\"https://nocap-ebook-api-qa.buiminhhien001.workers.dev\"")
        }
        release {
            isMinifyEnabled = false
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        create("productionPreview") {
            initWith(getByName("release"))
            matchingFallbacks += listOf("release")
            signingConfig = signingConfigs.getByName("debug")
            isDebuggable = false
            buildConfigField("String", "BACKEND_BASE_URL", "\"https://nocap-ebook-api.buiminhhien001.workers.dev\"")
            // Internal preview never enables purchases, even with a local product property.
            buildConfigField("String", "PLAY_PRO_PRODUCT_ID", "\"\"")
        }
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    testOptions {
        unitTests {
            isReturnDefaultValues = true
        }
    }

    buildFeatures {
        buildConfig = true
        compose = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/INDEX.LIST"
            excludes += "/META-INF/io.netty.versions.properties"
        }
    }
}

tasks.matching { it.name == "preReleaseBuild" }.configureEach {
    doFirst {
        check(hasReleaseSigning) {
            "Missing keystore.properties. A production release must use the permanent NoCap signing key."
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    implementation("com.android.billingclient:billing-ktx:9.1.0")
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.5")

    // Jetpack Compose BOM & UI (2024.10.01 -> Compose 1.7.5, compatible with compileSdk 36 / AGP 8.10.1)
    val composeBom = platform("androidx.compose:compose-bom:2024.10.01")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-core")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.navigation:navigation-compose:2.8.5")
    implementation("androidx.fragment:fragment-ktx:1.8.5")

    // Room Database 2.7.2 (KSP2-compatible)
    implementation("androidx.room:room-runtime:2.7.2")
    implementation("androidx.room:room-ktx:2.7.2")
    ksp("androidx.room:room-compiler:2.7.2")

    // DataStore Preferences
    implementation("androidx.datastore:datastore-preferences:1.1.2")

    // WorkManager (architectural foundation only; BookDownloadWorker added in Milestone 3)
    implementation("androidx.work:work-runtime-ktx:2.10.0")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

    // Coil 2.7.0 — standard lightweight image loading for Android Jetpack Compose
    implementation("io.coil-kt:coil-compose:2.7.0")

    // OkHttp 4.12.0 for WorkManager book streaming downloads
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.jsoup:jsoup:1.18.3")

    // -------------------------------------------------------------------
    // Readium Kotlin Toolkit 3.3.0 (EPUB & PDF Reader Engine)
    // -------------------------------------------------------------------
    implementation("org.readium.kotlin-toolkit:readium-shared:3.3.0")
    implementation("org.readium.kotlin-toolkit:readium-streamer:3.3.0")
    implementation("org.readium.kotlin-toolkit:readium-navigator:3.3.0")
    implementation("org.readium.kotlin-toolkit:readium-adapter-pdfium:3.3.0")

    // -------------------------------------------------------------------
    // Firebase BoM & Auth (M8B)
    // -------------------------------------------------------------------

    // -------------------------------------------------------------------
    // Android Credential Manager & Google ID for Google Sign-In
    // -------------------------------------------------------------------
    implementation("androidx.credentials:credentials:1.3.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.3.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")

    // Unit Testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20240303")
    testImplementation("net.sf.kxml:kxml2:2.3.0")
    testImplementation("androidx.room:room-testing:2.7.2")
    testImplementation("org.xerial:sqlite-jdbc:3.45.1.0")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
