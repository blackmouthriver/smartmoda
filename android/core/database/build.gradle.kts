plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.synaptia.smartmoda.core.database"
    compileSdk = 35

    defaultConfig {
        minSdk = 26
        consumerProguardFiles("consumer-rules.pro")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    sourceSets["main"].kotlin.srcDir("src/main/kotlin")
    sourceSets["test"].kotlin.srcDir("src/test/kotlin")
}

kotlin {
    compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) }
}

android {
    defaultConfig {
        ksp { arg("room.schemaLocation", "$projectDir/schemas") }
    }
}

dependencies {
    implementation(project(":android:core:common"))
    implementation(project(":shared-domain"))
    implementation(libs.coroutines.core)
    api(libs.room.runtime)
    api(libs.room.ktx)
    ksp(libs.room.compiler)

    testImplementation(libs.junit5.jupiter)
    testImplementation(libs.kotest.assertions)
    testRuntimeOnly(libs.junit5.launcher)
}

tasks.withType<Test> { useJUnitPlatform() }
