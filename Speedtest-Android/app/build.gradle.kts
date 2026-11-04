plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.licensee)
}

android {
    namespace = "org.librespeed.speedtest"
    compileSdk = 37

    defaultConfig {
        applicationId = "org.librespeed.speedtest"
        minSdk = 26
        targetSdk = 36
        versionCode = 10
        versionName = "2.0.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    testOptions {
        //the engine reads android.os.Build for its default User-Agent
        unitTests.isReturnDefaultValues = true
        //robolectric screenshot tests render real resources
        unitTests.isIncludeAndroidResources = true
        unitTests.all { test ->
            //the roborazzi gradle plugin does not support AGP 9 yet; forward its
            //mode flags manually: -Proborazzi.test.record=true / verify / compare
            listOf("roborazzi.test.record", "roborazzi.test.verify", "roborazzi.test.compare").forEach { key ->
                providers.gradleProperty(key).orNull?.let { test.systemProperty(key, it) }
            }
        }
    }

    signingConfigs {
        //populated from the environment in CI; local builds fall back to the debug key
        create("release") {
            val keystorePath = System.getenv("SIGNING_KEYSTORE")
            if (!keystorePath.isNullOrEmpty()) {
                storeFile = file(keystorePath)
                storePassword = System.getenv("SIGNING_STORE_PASSWORD")
                keyAlias = System.getenv("SIGNING_KEY_ALIAS")
                keyPassword = System.getenv("SIGNING_KEY_PASSWORD")
            }
        }
    }

    androidResources {
        //the app ships en and cs; without this, Material3 and the AndroidX
        //libraries contribute strings for ~85 locales the user can never see
        localeFilters += listOf("en", "cs")
    }

    packaging {
        resources {
            //build-time metadata that has no reader on the device
            excludes += listOf(
                "kotlin/**",
                "META-INF/*.version",
                "META-INF/**/LICENSE*",
                "META-INF/DEPENDENCIES",
                "DebugProbesKt.bin"
            )
        }
    }

    //the Play Console dependency block is opaque binary metadata; the APK on
    //GitHub and F-Droid gains nothing from it (F-Droid flags it)
    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (!System.getenv("SIGNING_KEYSTORE").isNullOrEmpty()) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

licensee {
    allow("Apache-2.0")
    allow("MIT")
    allow("BSD-3-Clause")
}

//ships the licensee report as an asset so the licenses screen shows the real dependency list
abstract class LicenseeAssetTask : DefaultTask() {
    @get:InputFile
    abstract val inputFile: RegularFileProperty

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @TaskAction
    fun copy() {
        inputFile.get().asFile.copyTo(outputDir.get().file("licenses.json").asFile, overwrite = true)
    }
}

androidComponents {
    onVariants { variant ->
        val capitalized = variant.name.replaceFirstChar { it.uppercase() }
        val copyTask = tasks.register("copy${capitalized}LicenseeAsset", LicenseeAssetTask::class.java) {
            inputFile.set(layout.buildDirectory.file("reports/licensee/android$capitalized/artifacts.json"))
            dependsOn("licenseeAndroid$capitalized")
        }
        variant.sources.assets?.addGeneratedSourceDirectory(copyTask, LicenseeAssetTask::outputDir)
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material3.window.size)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.window)
    implementation(libs.androidx.work.runtime)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    testImplementation(libs.junit)
    testImplementation(libs.mockwebserver)
    //real org.json for unit tests; the mockable android.jar only has stubs
    testImplementation(libs.json)
    testImplementation(libs.robolectric)
    testImplementation(libs.roborazzi)
    testImplementation(libs.roborazzi.compose)
    testImplementation(libs.androidx.test.ext.junit)
    testImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
}
