package com.netero.grao.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.netero.grao.R

object GraoColors {
    val Creme = Color(0xFFF2E8D5)
    val CremeEscuro = Color(0xFFEDE1CB)
    val Borda = Color(0xFFCDBFA6)
    val Marrom = Color(0xFF3B2A20)
    val MarromClaro = Color(0xFF6B5646)
    val Laranja = Color(0xFFE2683C)
    val Ferrugem = Color(0xFFA8461F)
    val Ambar = Color(0xFFF4A72C)
    val Preto = Color(0xFF141210)
    val Grafite = Color(0xFF1E1A16)
    val CamaraEscura = Color(0xFF1A0F0C)
    val TextoClaro = Color(0xFFD9CBB4)
    val Papel = Color(0xFFFFFDF8)
}

// Fontes embutidas (licença SIL OFL, ver /licenses)
val TituloFont: FontFamily = FontFamily(Font(R.font.fraunces_semibold, FontWeight.SemiBold))
val MonoFont: FontFamily = FontFamily(Font(R.font.share_tech_mono, FontWeight.Normal))

@Composable
fun GraoTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = GraoColors.Laranja,
            onPrimary = GraoColors.Preto,
            background = GraoColors.Creme,
            onBackground = GraoColors.Marrom,
            surface = GraoColors.Creme,
            onSurface = GraoColors.Marrom,
        ),
        content = content,
    )
}
