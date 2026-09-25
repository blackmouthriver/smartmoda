package com.synaptia.smartmoda.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.synaptia.smartmoda.core.designsystem.SmartModaTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // EN-1608: se instala ANTES de super.onCreate. El splash del sistema se descarta en
        // cuanto hay contenido, de modo que no anade espera artificial al arranque.
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            SmartModaTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { padding ->
                    Placeholder(Modifier.padding(padding))
                }
            }
        }
    }
}

/**
 * Pantalla temporal del esqueleto. La sustituye US-0101 (onboarding) en el Sprint 1.
 */
@Composable
private fun Placeholder(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = "SmartModa")
    }
}
