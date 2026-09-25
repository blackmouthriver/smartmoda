package com.synaptia.smartmoda.core.designsystem

import androidx.compose.ui.graphics.Color

/**
 * Paleta de SmartModa, derivada de `contracts/design-tokens.json`.
 *
 * Los valores canonicos estan en OKLCH porque su luminosidad es perceptual: dos colores con la
 * misma L pesan visualmente igual aunque cambien de hue. Compose no entiende OKLCH, asi que
 * aqui viven sus equivalentes sRGB ya calculados.
 *
 * Principio 90/7/3: 90% neutros, 7% acento, 3% semanticos. En un marketplace el color dominante
 * debe ser el de la mercancia, no el del contenedor. Si la interfaz tiene personalidad cromatica
 * fuerte, pelea con cada foto de producto.
 *
 * ESTE ARCHIVO SE GENERARA desde los tokens en EN-1903. Hasta entonces se mantiene a mano y
 * debe coincidir con `contracts/design-tokens.json`.
 */
object SmartModaColors {

    // --- Neutros: el anfitrion -------------------------------------------------------
    // Hue 70 (arena) con croma bajo. No son grises: el gris puro enfria la piel en las fotos
    // editoriales y hace ver amarillentos los blancos de estudio.
    val Paper = Color(0xFFFDFBFA)
    val Bone50 = Color(0xFFF9F6F2)
    val Sand100 = Color(0xFFF0ECE7)
    val Sand200 = Color(0xFFE2DDD8)
    val Sand300 = Color(0xFFC8C3BD)
    val Stone400 = Color(0xFF9D9791)
    val Stone600 = Color(0xFF6E6862)
    val Graphite800 = Color(0xFF413C36)
    val Ink950 = Color(0xFF17130E)

    // --- Acento primario: Arcilla (hue 40) -------------------------------------------
    // Terracota, no rojo. El relleno de boton se fija en luminosidad 0.53 y no en 0.62
    // porque es la frontera donde el texto blanco alcanza 4.5:1.
    val ClayTint = Color(0xFFFFEEE8)
    val ClayBase = Color(0xFFAE471F)
    val ClayVivid = Color(0xFFCB6440)
    val ClayText = Color(0xFF873616)
    val ClayPressed = Color(0xFF571E08)

    // --- Acento secundario: Indigo (hue 265) -----------------------------------------
    // Frio contra calido. Sostiene los momentos en que el usuario deja de desear y empieza
    // a decidir: pago, devoluciones, verificacion de vendedor.
    val IndigoTint = Color(0xFFEBF2FF)
    val IndigoBase = Color(0xFF4266BF)
    val IndigoVivid = Color(0xFF5C82DA)
    val IndigoText = Color(0xFF314E94)
    val IndigoPressed = Color(0xFF1C3060)

    // --- Semanticos ------------------------------------------------------------------
    // RN-026: NO personalizables por tenant. Un error debe verse igual en todas las tiendas.
    // Arcilla (40) y error (15) son vecinos: se distinguen por rol, no por color. El error
    // nunca aparece como bloque relleno junto a producto, solo como texto, icono y borde.
    val Success = Color(0xFF298646)
    val Warning = Color(0xFFBE8700)
    val Error = Color(0xFFBD1F44)
    val Info = Color(0xFF4266BF)

    // --- Modo oscuro -----------------------------------------------------------------
    // No es la paleta clara invertida. El fondo sube a 0.16 y nunca es negro puro: el negro
    // absoluto hace flotar las fotos con fondo blanco y cansa en scroll largo.
    val DarkBackground = Color(0xFF100D0A)
    val DarkSurface = Color(0xFF1B1814)
    val DarkBorder = Color(0xFF312D28)
    val DarkTextSecondary = Color(0xFFA9A49E)
    val ClayDark = Color(0xFFDF8C6F)
    val IndigoDark = Color(0xFF8CAAEB)
}
