package com.netero.grao.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.netero.grao.Screen
import com.netero.grao.ui.theme.GraoColors

/** Barra de baixo: abas em pílula, a ativa em destaque. */
@Composable
fun BottomNav(current: Screen, dark: Boolean = true, onSelect: (Screen) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(GraoColors.Preto)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        listOf(
            Screen.Camera to "Câmera",
            Screen.Gallery to "Galeria",
            Screen.Editor to "Editar",
            Screen.Develop to "Revelar",
        ).forEach { (s, label) ->
            val selected = s == current
            Box(
                Modifier
                    .height(44.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(if (selected) GraoColors.Grafite else Color.Transparent)
                    .clickable { onSelect(s) }
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label,
                    color = if (selected) GraoColors.Laranja else GraoColors.TextoClaro,
                    fontSize = 14.sp,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                )
            }
        }
    }
}
