import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
}

// Developer settings from the root local.properties (never committed). A missing file, key or
// empty value gives the default, so CI builds without the file
// (docs/03-architecture.md §10, docs/fake-data.md §5.1). Values are read when the build runs:
// after changing local.properties, run the app again; BuildConfig is regenerated.
val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.isFile) file.inputStream().use { load(it) }
}

fun localSetting(key: String, default: String): String =
    localProperties.getProperty(key)?.trim()?.takeIf { it.isNotEmpty() } ?: default

val useFakeData: Boolean = when (val value = localSetting("USE_FAKE_DATA", "true").lowercase()) {
    "true" -> true
    "false" -> false
    else -> error("USE_FAKE_DATA in local.properties must be true or false, not \"$value\"")
}

// Only the characters are checked here, so the value can go into BuildConfig as a Java string.
// Whether the name exists is checked by the app at startup (Scenario.fromName), which stops the
// app with "Unknown FAKE_SCENARIO: …" (docs/fake-data.md §5.1).
val fakeScenario: String = localSetting("FAKE_SCENARIO", "normal").also { value ->
    require(value.matches(Regex("[A-Za-z0-9_]+"))) {
        "FAKE_SCENARIO in local.properties may contain only letters, digits and _, not \"$value\""
    }
}

val fakeDelayMs: Long = localSetting("FAKE_DELAY_MS", "800").let { value ->
    value.toLongOrNull()?.takeIf { it >= 0 }
        ?: error("FAKE_DELAY_MS in local.properties must be a whole number of milliseconds (0 or more), not \"$value\"")
}

android {
    namespace = "vn.edu.usth.cryptodashboard"
    compileSdk {
        version = release(37) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "vn.edu.usth.cryptodashboard"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("boolean", "USE_FAKE_DATA", useFakeData.toString())
        buildConfigField("String", "FAKE_SCENARIO", "\"$fakeScenario\"")
        buildConfigField("long", "FAKE_DELAY_MS", "${fakeDelayMs}L")
    }

    buildFeatures {
        buildConfig = true
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.livedata)
    implementation(libs.androidx.lifecycle.viewmodel)
    implementation(libs.material)
    implementation(libs.mpandroidchart)
    implementation(libs.volley)
    implementation(libs.androidx.swiperefreshlayout)
    testImplementation(libs.androidx.arch.core.testing)
    testImplementation(libs.junit)
    testImplementation(libs.org.json)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
}