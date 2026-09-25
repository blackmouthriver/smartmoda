// Lectura de secretos para la compilacion.
//
// EN-0005. Regla unica: ningun secreto vive en el codigo ni en el repositorio.
// Se leen de local.properties (maquina del desarrollador) o del entorno (CI), en ese orden.
// Ambos estan fuera de git.

/**
 * Extrae el rol declarado dentro de una clave de Supabase.
 *
 * Las claves clasicas son JWT: el payload lleva {"role":"anon"} o {"role":"service_role"}.
 * Las nuevas se distinguen por prefijo: sb_publishable_ frente a sb_secret_.
 */
val supabaseKeyRole = fun(key: String): String? {
    if (key.isBlank()) return null
    if (key.startsWith("sb_secret_")) return "service_role"
    if (key.startsWith("sb_publishable_")) return "anon"

    val parts = key.split(".")
    if (parts.size != 3) return null
    return runCatching {
        val payload = String(java.util.Base64.getUrlDecoder().decode(parts[1]))
        Regex("\"role\"\\s*:\\s*\"([^\"]+)\"").find(payload)?.groupValues?.get(1)
    }.getOrNull()
}

/** Lee un valor de local.properties y, si no esta, del entorno. Vacio si no existe. */
extra["secretOrEmpty"] = fun(target: org.gradle.api.Project, name: String): String {
    val local = target.rootProject.file("local.properties")
    if (local.exists()) {
        val props = java.util.Properties()
        local.inputStream().use { props.load(it) }
        val fromFile = props.getProperty(name)
        if (!fromFile.isNullOrBlank()) return fromFile.trim()
    }
    return System.getenv(name)?.trim().orEmpty()
}

/**
 * Devuelve la clave anon verificando que NO sea la service_role.
 *
 * Por que este guard existe: en el panel de Supabase las dos claves aparecen una al lado de
 * la otra y se copian igual de facil. La diferencia es que la service_role OMITE el Row Level
 * Security por completo. En un cliente movil seria catastrofica, porque cualquiera puede
 * extraerla del APK y leer o escribir los datos de todos los tenants.
 *
 * La clave anon, en cambio, esta pensada para ser publica: lo que la protege es el RLS
 * (ver supabase/migrations/0001_tenancy.sql).
 */
extra["requireAnonKey"] = fun(target: org.gradle.api.Project): String {
    val local = target.rootProject.file("local.properties")
    var key = ""
    if (local.exists()) {
        val props = java.util.Properties()
        local.inputStream().use { props.load(it) }
        key = props.getProperty("SUPABASE_ANON_KEY")?.trim().orEmpty()
    }
    if (key.isBlank()) key = System.getenv("SUPABASE_ANON_KEY")?.trim().orEmpty()

    val role = supabaseKeyRole(key)
    if (role != null && role != "anon") {
        throw org.gradle.api.GradleException(
            "SUPABASE_ANON_KEY contiene una clave de rol '$role'.\n" +
                "La clave service_role omite el Row Level Security y NO puede ir en un cliente\n" +
                "movil: cualquiera puede extraerla del APK. Usa la clave anon / publishable."
        )
    }
    return key
}
