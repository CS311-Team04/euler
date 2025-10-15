// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    id("org.sonarqube") version "6.3.1.5724"
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.jetbrainsKotlinAndroid) apply false
    alias(libs.plugins.googleServices) apply false
}

// Configure SonarQube to only analyze the app module
sonar {
    properties {
        property("sonar.projectKey", "CS311-Team04_euler")
        property("sonar.projectName", "euler")
        property("sonar.organization", "cs311-team04")
        property("sonar.host.url", "https://sonarcloud.io")
        
        // Only analyze app module, skip root project
        property("sonar.modules", "app")
        property("sonar.sources", "")
        property("sonar.tests", "")
    }
}

// Ensure JaCoCo report is generated before SonarQube analysis
tasks.named("sonar") {
    dependsOn(":app:jacocoTestReport")
}
