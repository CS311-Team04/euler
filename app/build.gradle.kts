plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.jetbrainsKotlinAndroid)
    // alias(libs.plugins.googleServices) // Disabled for CI - google-services.json not available
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
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }

        debug {
            enableUnitTestCoverage = true
            enableAndroidTestCoverage = false
        }
    }



    buildFeatures {
        compose = true
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

    // Correctif JaCoCo : exclure les libs du SDK et BouncyCastle
    // pour éviter les erreurs d'instrumentation pendant les tests
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

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(platform(libs.compose.bom))
    testImplementation(libs.junit)
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
    androidTestImplementation("org.mockito:mockito-android:5.8.0")
    androidTestImplementation("org.mockito:mockito-core:5.8.0")

    implementation("com.microsoft.identity.client:msal:6.0.1")

    // --- Dépendances de sécurité manquantes requises par MSAL ---
    implementation("org.bouncycastle:bcprov-jdk18on:1.78.1")
    implementation("org.bouncycastle:bcpkix-jdk18on:1.78.1")
    implementation("com.google.crypto.tink:tink-android:1.12.0")
    implementation("com.google.auto.value:auto-value-annotations:1.10.4")
    implementation("com.google.code.findbugs:jsr305:3.0.2")
    implementation("com.github.spotbugs:spotbugs-annotations:4.8.3")

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

// SonarQube configuration with JaCoCo 0.8.14
sonar {
    properties {
        property("sonar.projectKey", "CS311-Team04_euler")
        property("sonar.projectName", "euler")
        property("sonar.organization", "cs311-team04")
        property("sonar.host.url", "https://sonarcloud.io")

        // Sources and tests - corrected paths
        property("sonar.sources", "app/src/main/java")
        property("sonar.tests", "app/src/test/java")

        // Exclusions
        property("sonar.exclusions", "**/build/**,**/R.java,**/R.kt,**/BuildConfig.*,**/*.xml,**/res/**,**/androidTest/**")
        property("sonar.test.inclusions", "**/*Test.kt,**/*Test.java")
        property("sonar.coverage.exclusions", "**/*Test.kt,**/*Test.java,**/test/**/*,**/androidTest/**/*")

        // Coverage reports - corrected paths
        property("sonar.coverage.jacoco.xmlReportPaths", "${project.layout.buildDirectory.get()}/reports/jacoco/jacocoTestReport/jacocoTestReport.xml")
        property("sonar.junit.reportPaths", "${project.layout.buildDirectory.get()}/test-results/testDebugUnitTest/")
        property("sonar.androidLint.reportPaths", "${project.layout.buildDirectory.get()}/reports/lint-results-debug.xml")
        
        // Force coverage calculation
        property("sonar.coverage.jacoco.xmlReportPaths", "${project.layout.buildDirectory.get()}/reports/jacoco/jacocoTestReport/jacocoTestReport.xml")
        property("sonar.java.coveragePlugin", "jacoco")
        property("sonar.dynamicAnalysis", "reuseReports")
    }
}
