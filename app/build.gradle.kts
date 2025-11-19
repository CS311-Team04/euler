import java.util.Properties

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.jetbrainsKotlinAndroid)
    id("jacoco")
    id("org.sonarqube")
    alias(libs.plugins.ktfmt)
    alias(libs.plugins.googleServices) apply false
}

// Apply Google Services whenever the JSON is present (file can be provisioned on CI)
val hasGoogleServicesJson = file("google-services.json").exists()
if (hasGoogleServicesJson) {
    apply(plugin = libs.plugins.googleServices.get().pluginId)
}

/**
 * Gradle's `buildConfigField` expects string literals to be wrapped in quotes and have any embedded
 * quotes escaped. This helper normalises values coming from env/properties before we inject them.
 */
fun quoteBuildConfig(value: String): String = "\"${value.replace("\"", "\\\"")}\""

val localProperties = Properties().apply {
    val propertiesFile = rootProject.file("local.properties")
    if (propertiesFile.exists()) {
        propertiesFile.inputStream().use { load(it) }
    }
}

fun resolveConfigValue(key: String): String? =
    localProperties.getProperty(key)?.trim().takeUnless { it.isNullOrEmpty() }
        ?: System.getenv(key)?.trim().takeUnless { it.isNullOrEmpty() }

val voiceChatOverrides = Properties().apply {
    val file = rootProject.file("voicechat.properties")
    if (file.exists()) {
        file.inputStream().use { load(it) }
    }
}

fun resolveVoiceChatOverride(key: String, fallback: String = ""): String =
    voiceChatOverrides.getProperty(key, fallback).trim()

val llmHttpEndpoint: String =
    resolveConfigValue("LLM_HTTP_ENDPOINT") ?: run {
        resolveVoiceChatOverride(
            "llm.httpEndpoint",
            "http://10.0.2.2:5002/euler-e8edb/us-central1/answerWithRagHttp")
    }
val llmHttpApiKey: String =
    resolveConfigValue("LLM_HTTP_API_KEY") ?: resolveVoiceChatOverride("llm.httpApiKey")

android {
    namespace = "com.android.sample"
    compileSdk = 34

    signingConfigs {
        val releaseStoreFile = resolveConfigValue("RELEASE_STORE_FILE")
        val releaseStorePassword = resolveConfigValue("RELEASE_STORE_PASSWORD")
        val releaseKeyAlias = resolveConfigValue("RELEASE_KEY_ALIAS")
        val releaseKeyPassword = resolveConfigValue("RELEASE_KEY_PASSWORD")

        if (
            releaseStoreFile != null &&
            releaseStorePassword != null &&
            releaseKeyAlias != null &&
            releaseKeyPassword != null
        ) {
            create("release") {
                storeFile = rootProject.file(releaseStoreFile)
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    defaultConfig {
        applicationId = "com.android.sample"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
        // Default region for Firebase Functions (prod)
        buildConfigField ("String", "FUNCTIONS_REGION", "\"europe-west6\"")
        buildConfigField ("String", "FUNCTIONS_HOST", "\"10.0.2.2\"")
        buildConfigField ("int",    "FUNCTIONS_PORT", "5002")
        buildConfigField ("boolean","USE_FUNCTIONS_EMULATOR", "true")
        buildConfigField("String", "LLM_HTTP_ENDPOINT", quoteBuildConfig(llmHttpEndpoint))
        buildConfigField("String", "LLM_HTTP_API_KEY", quoteBuildConfig(llmHttpApiKey))
    }

    buildTypes {
        release {
            signingConfigs.findByName("release")?.let { signingConfig = it }
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // Ensure release APK talks to production Functions, not local emulator
            buildConfigField ("boolean","USE_FUNCTIONS_EMULATOR", "false")
            // Disable local HTTP fallback in release by default
            buildConfigField("String", "LLM_HTTP_ENDPOINT", "\"\"")
        }

        debug {
            enableUnitTestCoverage = true
            enableAndroidTestCoverage = false
            // Keep emulator defaults for local development
            buildConfigField ("boolean","USE_FUNCTIONS_EMULATOR", "true")
        }
    }



    buildFeatures {
        compose = true
        buildConfig = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.10"
    }


    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/versions/9/OSGI-INF/MANIFEST.MF"
            excludes += "/META-INF/MANIFEST.MF"
            excludes += "/META-INF/DEPENDENCIES"
            excludes += "/META-INF/LICENSE"
            excludes += "/META-INF/LICENSE.txt"
            excludes += "/META-INF/license.txt"
            excludes += "/META-INF/NOTICE"
            excludes += "/META-INF/NOTICE.txt"
            excludes += "/META-INF/notice.txt"
            excludes += "/META-INF/ASL2.0"
            excludes += "/META-INF/*.kotlin_module"
        }
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
        }
    }

    // JaCoCo fix: exclude SDK and BouncyCastle libraries
    // to avoid instrumentation errors during tests
    tasks.withType<Test> {
        configure<JacocoTaskExtension> {
            isIncludeNoLocationClasses = true
            excludes = listOf(
                "jdk.internal.*",
                "org.bouncycastle.*",
                "com.microsoft.identity.*",
                "com.google.crypto.tink.*",
                "com.google.auto.value.*",
                "com.google.code.findbugs.*"
            )
        }
    }

    // Robolectric setup
    sourceSets.getByName("testDebug") {
        val test = sourceSets.getByName("test")

        java.setSrcDirs(test.java.srcDirs)
        res.setSrcDirs(test.res.srcDirs)
        resources.setSrcDirs(test.resources.srcDirs)
    }

    sourceSets.getByName("test") {
        java.setSrcDirs(emptyList<File>())
        res.setSrcDirs(emptyList<File>())
        resources.setSrcDirs(emptyList<File>())
    }
    buildToolsVersion = "34.0.0"
}

// When a library is used both by robolectric and connected tests, use this function
fun DependencyHandlerScope.globalTestImplementation(dep: Any) {
    androidTestImplementation(dep)
    testImplementation(dep)
}

dependencies {
    // Firebase
    implementation(platform("com.google.firebase:firebase-bom:33.7.0"))
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.firebase:firebase-analytics-ktx")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.8.1")
    implementation("com.google.firebase:firebase-functions-ktx")
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.8.1")

    // CustomTabs for Firebase OAuth browser flow
    implementation("androidx.browser:browser:1.8.0")

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(platform(libs.compose.bom))
    testImplementation(libs.junit)
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
    globalTestImplementation(libs.androidx.junit)
    globalTestImplementation(libs.androidx.espresso.core)

    // ------------- Jetpack Compose ------------------
    val composeBom = platform(libs.compose.bom)
    implementation(composeBom)
    globalTestImplementation(composeBom)

    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    // Material Design 3
    implementation(libs.compose.material3)
    // Material Icons Extended for additional icons (Language, DarkMode, Extension, etc.)
    implementation(libs.material.icons.extended)
    // Integration with activities
    implementation(libs.compose.activity)
    // Integration with ViewModels
    implementation(libs.compose.viewmodel)
    // Android Studio Preview support
    implementation(libs.compose.preview)
    debugImplementation(libs.compose.tooling)
    // UI Tests
    globalTestImplementation(libs.compose.test.junit)
    debugImplementation(libs.compose.test.manifest)

    // Navigation
    implementation("androidx.navigation:navigation-compose:2.7.5")

    // --------- Kaspresso test framework ----------
    globalTestImplementation(libs.kaspresso)
    globalTestImplementation(libs.kaspresso.compose)

    // ----------       Robolectric     ------------
    testImplementation(libs.robolectric)

    // ----------       Mockito         ------------
    testImplementation(libs.mockito.core)
    testImplementation(libs.mockk)
    androidTestImplementation("org.mockito:mockito-android:5.8.0")
    androidTestImplementation("org.mockito:mockito-core:5.8.0")
    testImplementation("org.mockito:mockito-inline:4.11.0")
    testImplementation("org.mockito.kotlin:mockito-kotlin:4.1.0")
    androidTestImplementation("org.mockito:mockito-android:4.11.0")
    androidTestImplementation("org.mockito:mockito-core:4.11.0")

    implementation("com.microsoft.identity.client:msal:6.0.1")

    // --- Additional security dependencies required by MSAL ---
    implementation("org.bouncycastle:bcprov-jdk18on:1.78.1")
    implementation("org.bouncycastle:bcpkix-jdk18on:1.78.1")
    implementation("com.google.crypto.tink:tink-android:1.12.0")
    implementation("com.google.auto.value:auto-value-annotations:1.10.4")
    implementation("com.google.code.findbugs:jsr305:3.0.2")
    implementation("com.github.spotbugs:spotbugs-annotations:4.8.3")

    debugImplementation("androidx.compose.ui:ui-tooling")
    implementation("androidx.compose.ui:ui-tooling-preview")

    // Networking for HTTP clients (LLM access, etc.)
    implementation(libs.okhttp)

    // DataStore for preferences
    implementation(libs.datastore.core)
    implementation(libs.datastore.preferences)

}

// JaCoCo configuration with Java 17 compatibility
jacoco {
    toolVersion = "0.8.14"
}

// Force JaCoCo version for all tasks
configurations.all {
    resolutionStrategy {
        force("org.jacoco:org.jacoco.agent:0.8.14")
        force("org.jacoco:org.jacoco.core:0.8.14")
        force("org.jacoco:org.jacoco.report:0.8.14")
    }
}

tasks.withType<Test> {
    // Configure Jacoco for each tests
    configure<JacocoTaskExtension> {
        isIncludeNoLocationClasses = true
        excludes = listOf(
            "jdk.internal.*",
            "org.bouncycastle.*",
            "com.microsoft.identity.*",
            "com.google.crypto.tink.*",
            "com.google.auto.value.*",
            "com.google.code.findbugs.*"
        )
    }
}


tasks.register("jacocoTestReport", JacocoReport::class) {
    mustRunAfter("testDebugUnitTest", "connectedDebugAndroidTest")

    reports {
        xml.required = true
        html.required = true
    }

    val fileFilter = listOf(
        "**/R.class",
        "**/R$*.class",
        "**/BuildConfig.*",
        "**/Manifest*.*",
        "**/*Test*.*",
        "android/**/*.*",
    )

    val debugTree = fileTree("${project.layout.buildDirectory.get()}/tmp/kotlin-classes/debug") {
        exclude(fileFilter)
    }

    val mainSrc = "${project.layout.projectDirectory}/src/main/java"
    sourceDirectories.setFrom(files(mainSrc))
    classDirectories.setFrom(files(debugTree))
    executionData.setFrom(fileTree(project.layout.buildDirectory.get()) {
        include("outputs/unit_test_code_coverage/debugUnitTest/testDebugUnitTest.exec")
        // Exclude Android test coverage to avoid JaCoCo conflicts
        // include("outputs/code_coverage/debugAndroidTest/connected/*/coverage.ec")
    })

    doLast {
        // Fix for JaCoCo coverage issue: remove lines with nr="65535" from XML report
        val reportFile = reports.xml.outputLocation.asFile.get()
        if (reportFile.exists()) {
            val content = reportFile.readText()
            val cleanedContent = content.replace("<line[^>]+nr=\"65535\"[^>]*>".toRegex(), "")
            reportFile.writeText(cleanedContent)
        }
    }
}

// SonarQube configuration - simplified for CI compatibility
sonar {
    properties {
        property("sonar.projectKey", "CS311-Team04_euler")
        property("sonar.projectName", "euler")
        property("sonar.organization", "cs311-team04")
        property("sonar.host.url", "https://sonarcloud.io")

        // Basic source configuration - relative to project root
        property("sonar.sources", "src/main/java")

        // Only add tests if directory exists and has content
        val testDir = file("src/test/java")
        if (testDir.exists() && testDir.listFiles()?.isNotEmpty() == true) {
            property("sonar.tests", "src/test/java")
        }

        // Basic exclusions
        property("sonar.exclusions", "**/build/**,**/R.java,**/R.kt,**/BuildConfig.*,**/*.xml,**/res/**")

        // Coverage - only if report exists
        val coverageReport = file("${project.layout.buildDirectory.get()}/reports/jacoco/jacocoTestReport/jacocoTestReport.xml")
        if (coverageReport.exists()) {
            property("sonar.coverage.jacoco.xmlReportPaths", coverageReport.absolutePath)
        }

        // Test results - only if exists
        val testResults = file("${project.layout.buildDirectory.get()}/test-results/testDebugUnitTest")
        if (testResults.exists()) {
            property("sonar.junit.reportPaths", testResults.absolutePath)
        }
    }
}