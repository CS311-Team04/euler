plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.gms.google-services")
    id("jacoco")
}

android {
    namespace = "ch.epfl.euler"
    compileSdk = 35

    defaultConfig {
        applicationId = "ch.epfl.euler"
        minSdk = 26
        targetSdk = 35
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
            enableUnitTestCoverage = true
            enableAndroidTestCoverage = true
        }
    }

    testCoverage { jacocoVersion = "0.8.11" }

    buildFeatures { compose = true }

    // Aligné avec Kotlin 2.0.21
    //composeOptions { kotlinCompilerExtensionVersion = "1.7.3" }

    // Java 17 partout
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }   // <— le fix

    packaging { resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" } }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
        }
    }

    // Tes redirections de sourceSets test ↔ testDebug, si tu en as besoin :
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

fun DependencyHandlerScope.globalTestImplementation(dep: Any) {
    androidTestImplementation(dep); testImplementation(dep)
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.8.1")


    // Firebase BOM
    implementation(platform("com.google.firebase:firebase-bom:33.5.1"))
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.firebase:firebase-analytics-ktx")

    // === Compose BOM (une seule fois) ===
    implementation(platform("androidx.compose:compose-bom:2024.10.01"))

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    // Navigation/Lifecycle compatibles API 35 + AGP 8.6
    implementation("androidx.navigation:navigation-compose:2.9.5")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.0")

    // Pour l’annotation @Preview
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation(libs.androidx.ui.tooling.preview)
    // Pour l’outil d’inspection en debug (Layout Inspector, etc.)
    debugImplementation("androidx.compose.ui:ui-tooling")

    // === Autres deps depuis le catalog (si ces alias existent) ===
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.lifecycle.runtime.ktx)

    testImplementation(libs.junit)
    debugImplementation(libs.androidx.ui.tooling)
    globalTestImplementation(libs.androidx.junit)
    globalTestImplementation(libs.androidx.espresso.core)

    // UI test Compose
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    // Kaspresso / Robolectric si utilisés
    globalTestImplementation(libs.kaspresso)
    globalTestImplementation(libs.kaspresso.compose)
    testImplementation(libs.robolectric)
}

tasks.withType<Test>().configureEach {
    extensions.configure(JacocoTaskExtension::class.java) {
        isIncludeNoLocationClasses = true
        excludes = listOf("jdk.internal.*")
    }
}

tasks.register("jacocoTestReport", JacocoReport::class) {
    mustRunAfter("testDebugUnitTest", "connectedDebugAndroidTest")
    reports { xml.required.set(true); html.required.set(true) }

    val fileFilter = listOf("**/R.class","**/R$*.class","**/BuildConfig.*","**/Manifest*.*","**/*Test*.*","android/**/*.*")
    val debugTree = fileTree("${project.layout.buildDirectory.get()}/tmp/kotlin-classes/debug") { exclude(fileFilter) }

    sourceDirectories.setFrom(files("${project.layout.projectDirectory}/src/main/java"))
    classDirectories.setFrom(files(debugTree))
    executionData.setFrom(fileTree(project.layout.buildDirectory.get()) {
        include("outputs/unit_test_code_coverage/debugUnitTest/testDebugUnitTest.exec")
        include("outputs/code_coverage/debugAndroidTest/connected/*/coverage.ec")
    })
}
