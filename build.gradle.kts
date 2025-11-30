// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    id("org.sonarqube") version "6.3.1.5724"
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.jetbrainsKotlinAndroid) apply false
    alias(libs.plugins.googleServices) apply false
}

sonar {
    properties {
        property("sonar.projectKey", "CS311-Team04_euler")
        property("sonar.projectName", "euler")
        property("sonar.organization", "cs311-team04")
        property("sonar.host.url", "https://sonarcloud.io")
        // Comma-separated paths to the various directories containing the *.xml JUnit report files. Each path may be absolute or relative to the project base directory.
         property("sonar.junit.reportPaths", "${project.layout.buildDirectory.get()}/test-results/testDebugunitTest/")
        // Paths to xml files with Android Lint issues. If the main flavor is changed, this file will have to be changed too.
        property("sonar.androidLint.reportPaths", "${project.layout.buildDirectory.get()}/reports/lint-results-debug.xml")
        // Paths to JaCoCo XML coverage report files.
        property("sonar.coverage.jacoco.xmlReportPaths", "${project.layout.buildDirectory.get()}/reports/jacoco/jacocoTestReport/jacocoTestReport.xml")
    }
}
