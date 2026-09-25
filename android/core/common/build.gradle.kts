plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

// EN-0005: los secretos se leen de local.properties o del entorno, nunca del codigo.
apply(from = rootProject.file("gradle/secrets.gradle.kts"))

@Suppress("UNCHECKED_CAST")
val requireAnonKey = extra["requireAnonKey"] as (org.gradle.api.Project) -> String

@Suppress("UNCHECKED_CAST")
val secretOrEmpty = extra["secretOrEmpty"] as (org.gradle.api.Project, String) -> String

android {
    namespace = "com.synaptia.smartmoda.core.common"
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

    buildFeatures { buildConfig = true }

    defaultConfig {
        // Vacios si no hay configuracion local: la app avisa en ejecucion en vez de
        // no compilar. Un desarrollador que clona el repositorio puede compilar de
        // inmediato, y Environment.isConfigured le dice que falta.
        buildConfigField("String", "SUPABASE_URL", "\"${secretOrEmpty(project, "SUPABASE_URL")}\"")
        buildConfigField("String", "SUPABASE_ANON_KEY", "\"${requireAnonKey(project)}\"")
    }
}

kotlin {
    compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) }
}

dependencies {
    implementation(libs.coroutines.core)
    implementation(libs.androidx.core.ktx)

    testImplementation(libs.junit5.jupiter)
    testImplementation(libs.kotest.assertions)
    testRuntimeOnly(libs.junit5.launcher)
}

tasks.withType<Test> { useJUnitPlatform() }
