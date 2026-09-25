rootProject.name = "smartmoda"

pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

// Dominio compartido: Kotlin puro, sin Android.
// Al ser un modulo JVM, el compilador impide por construccion que importe Android o Spring.
// Esa es la garantia de ADR-0012: corre igual en movil, servidor y navegador.
include(":shared-domain")

// Aplicacion Android
include(":android:app")
include(":android:core:common")
include(":android:core:designsystem")
include(":android:core:crypto")
include(":android:core:ml")
include(":android:core:database")
include(":android:feature:sizing")
