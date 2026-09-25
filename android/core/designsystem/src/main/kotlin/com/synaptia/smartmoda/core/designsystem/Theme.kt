package com.synaptia.smartmoda.core.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val LightColors = lightColorScheme(
    primary = SmartModaColors.ClayBase,
    onPrimary = Color.White,
    primaryContainer = SmartModaColors.ClayTint,
    onPrimaryContainer = SmartModaColors.ClayPressed,
    secondary = SmartModaColors.IndigoBase,
    onSecondary = Color.White,
    secondaryContainer = SmartModaColors.IndigoTint,
    onSecondaryContainer = SmartModaColors.IndigoPressed,
    background = SmartModaColors.Paper,
    onBackground = SmartModaColors.Graphite800,
    surface = SmartModaColors.Bone50,
    onSurface = SmartModaColors.Graphite800,
    surfaceVariant = SmartModaColors.Sand100,
    onSurfaceVariant = SmartModaColors.Stone600,
    outline = SmartModaColors.Sand300,
    outlineVariant = SmartModaColors.Sand200,
    error = SmartModaColors.Error,
    onError = Color.White,
)

private val DarkColors = darkColorScheme(
    primary = SmartModaColors.ClayDark,
    onPrimary = SmartModaColors.Ink950,
    secondary = SmartModaColors.IndigoDark,
    onSecondary = SmartModaColors.Ink950,
    background = SmartModaColors.DarkBackground,
    onBackground = SmartModaColors.Paper,
    surface = SmartModaColors.DarkSurface,
    onSurface = SmartModaColors.Paper,
    surfaceVariant = SmartModaColors.DarkSurface,
    onSurfaceVariant = SmartModaColors.DarkTextSecondary,
    outline = SmartModaColors.DarkBorder,
    outlineVariant = SmartModaColors.DarkBorder,
    error = SmartModaColors.Error,
    onError = Color.White,
)

/**
 * Personalizacion por tenant (US-1703).
 *
 * Lo que un comercio PUEDE cambiar: acentos, tipografia, radios, densidad, logo.
 * Lo que NO puede: los colores semanticos (RN-026) ni la paleta de navegacion, carrito y
 * pago (RN-027). Por eso este objeto solo lleva los acentos: los semanticos no estan aqui,
 * asi que no hay forma de sobrescribirlos aunque alguien lo intente.
 */
data class TenantBranding(
    val primary: Color? = null,
    val secondary: Color? = null,
    val cornerRadiusMedium: androidx.compose.ui.unit.Dp = 12.dp,
    /** US-1704: preajuste de movimiento. `prefers-reduced-motion` del sistema lo anula. */
    val motionPreset: MotionPreset = MotionPreset.STANDARD,
) {
    companion object {
        /** Sin tenant activo se usa la identidad de la plataforma. */
        val Platform = TenantBranding()
    }
}

enum class MotionPreset(val transitionMillis: Int) {
    NONE(0),
    SUBTLE(160),
    STANDARD(240),
    EXPRESSIVE(400),
}

val LocalTenantBranding = staticCompositionLocalOf { TenantBranding.Platform }

/** Objetivo tactil minimo. RNF-06, sin excepcion. */
val MinTouchTarget = 48.dp

@Composable
fun SmartModaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    branding: TenantBranding = TenantBranding.Platform,
    content: @Composable () -> Unit,
) {
    val base = if (darkTheme) DarkColors else LightColors

    // El tenant solo puede desplazar los acentos. Los semanticos de `base` sobreviven intactos.
    val scheme = base.copy(
        primary = branding.primary ?: base.primary,
        secondary = branding.secondary ?: base.secondary,
    )

    CompositionLocalProvider(LocalTenantBranding provides branding) {
        MaterialTheme(
            colorScheme = scheme,
            typography = Typography(),
            content = content,
        )
    }
}
