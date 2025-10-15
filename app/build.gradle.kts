plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.jetbrainsKotlinAndroid)
    // Google Services plugin disabled for security (google-services.json removed)
    // alias(libs.plugins.googleServices)
    id("jacoco")
    id("org.sonarqube")
    alias(libs.plugins.ktfmt)
}

android {
    namespace = "com.android.sample"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.android.sample"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables { useSupportLibrary = true }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            enableUnitTestCoverage = false
            enableAndroidTestCoverage = false
        }
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.4.3"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs += listOf("-Xsuppress-version-warnings", "-Xskip-metadata-version-check")
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}

dependencies {
    // Firebase
    implementation(platform("com.google.firebase:firebase-bom:33.7.0"))
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.firebase:firebase-analytics-ktx")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.8.1")

    // Compose
    implementation(platform("androidx.compose:compose-bom:2024.10.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation(libs.compose.preview)
    debugImplementation(libs.compose.tooling)

    // Navigation & Lifecycle
    implementation("androidx.navigation:navigation-compose:2.7.6")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")

    // Core Android
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.lifecycle.runtime.ktx)

    // Testing
    testImplementation(libs.junit)
    testImplementation("io.mockk:mockk:1.13.8")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    testImplementation(libs.robolectric)
    
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}

// JaCoCo configuration
tasks.withType<Test> {
    extensions.configure(JacocoTaskExtension::class) {
        isIncludeNoLocationClasses = true
        excludes = listOf("jdk.internal.*")
    }
}

tasks.register("jacocoTestReport", JacocoReport::class) {
    dependsOn("testDebugUnitTest")
    reports {
        xml.required.set(true)
        html.required.set(true)
    }

    val fileFilter = listOf(
        "**/R.class",
        "**/R$*.class",
        "**/BuildConfig.*",
        "**/Manifest*.*",
        "**/*Test*.*",
        "android/**/*.*"
    )
    val debugTree = fileTree("${project.layout.buildDirectory.get()}/tmp/kotlin-classes/debug") {
        exclude(fileFilter)
    }

    sourceDirectories.setFrom(files("${project.layout.projectDirectory}/src/main/java"))
    classDirectories.setFrom(files(debugTree))
    executionData.setFrom(
        fileTree(project.layout.buildDirectory.get()) {
            include("outputs/unit_test_code_coverage/debugUnitTest/testDebugUnitTest.exec")
        }
    )
}

// SonarQube configuration
sonar {
    properties {
        property("sonar.projectKey", "CS311-Team04_euler")
        property("sonar.projectName", "euler")
        property("sonar.organization", "cs311-team04")
        property("sonar.host.url", "https://sonarcloud.io")
        
        // Test and coverage reports
        property("sonar.junit.reportPaths", "${project.layout.buildDirectory.get()}/test-results/testDebugUnitTest/")
        property("sonar.androidLint.reportPaths", "${project.layout.buildDirectory.get()}/reports/lint-results-debug.xml")
        property("sonar.coverage.jacoco.xmlReportPaths", "${project.layout.buildDirectory.get()}/reports/jacoco/jacocoTestReport/jacocoTestReport.xml")
        
        // Sources and tests
        property("sonar.sources", "src/main/java")
        property("sonar.tests", "src/test/java")
        
        // Exclusions
        property("sonar.exclusions", "**/build/**,**/R.java,**/R.kt,**/BuildConfig.*,**/*.xml,**/res/**")
        property("sonar.test.inclusions", "**/*Test.kt,**/*Test.java")
        property("sonar.coverage.exclusions", "**/*Test.kt,**/*Test.java,**/test/**/*,**/androidTest/**/*")
        
        // Binaries
        property("sonar.java.binaries", "${project.layout.buildDirectory.get()}/intermediates/javac/debug/classes,${project.layout.buildDirectory.get()}/tmp/kotlin-classes/debug")
    }
}

// ktfmt configuration
ktfmt {
    kotlinLangStyle()
}