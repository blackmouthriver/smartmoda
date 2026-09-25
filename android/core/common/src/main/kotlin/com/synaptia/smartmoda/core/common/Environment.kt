package com.synaptia.smartmoda.core.common

/**
 * Configuracion del entorno, resuelta en compilacion desde local.properties o el entorno de CI.
 *
 * EN-0005. Nada de esto esta escrito en el codigo ni en el repositorio.
 *
 * Sobre la clave anon: es publica a proposito y va dentro del APK. Lo que protege los datos
 * no es esconderla, sino el Row Level Security del servidor
 * (ver supabase/migrations/0001_tenancy.sql). La clave que SI es secreta, service_role,
 * nunca llega al cliente: el propio build lo impide (gradle/secrets.gradle.kts).
 */
object Environment {

    val supabaseUrl: String get() = BuildConfig.SUPABASE_URL
    val supabaseAnonKey: String get() = BuildConfig.SUPABASE_ANON_KEY

    /**
     * Si el entorno esta configurado.
     *
     * Se prefiere avisar en ejecucion antes que romper la compilacion: quien clona el
     * repositorio puede compilar de inmediato y descubrir que le falta configurar, en lugar
     * de encontrarse un build roto sin saber por que.
     */
    val isConfigured: Boolean
        get() = supabaseUrl.isNotBlank() && supabaseAnonKey.isNotBlank()

    /** Mensaje para el desarrollador. Nunca se muestra a un usuario final. */
    val missingConfigHint: String
        get() = buildString {
            append("Faltan variables de entorno. Copia .env.example y rellena local.properties:\n")
            if (supabaseUrl.isBlank()) append("  SUPABASE_URL=\n")
            if (supabaseAnonKey.isBlank()) append("  SUPABASE_ANON_KEY=\n")
            append("Instrucciones en supabase/README.md")
        }
}
