package com.netero.grao.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
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

@Composable
fun BottomNav(current: Screen, dark: Boolean, onSelect: (Screen) -> Unit) {
    val bg = if (dark) GraoColors.Preto else GraoColors.CremeEscuro
    val idle = if (dark) GraoColors.TextoClaro else GraoColors.MarromClaro
    val active = if (dark) GraoColors.Ambar else GraoColors.Ferrugem

    Column {
        HorizontalDivider(color = if (dark) GraoColors.Marrom else Color(0xFFDCCFB8))
        Row(
            Modifier.fillMaxWidth().height(72.dp).background(bg),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            listOf(Screen.Camera to "Câmera", Screen.Gallery to "Galeria", Screen.Develop to "Revelar").forEach { (s, label) ->
                val selected = s == current
                Column(
                    Modifier
                        .width(88.dp)
                        .height(56.dp)
                        .clickable { onSelect(s) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Box(
                        Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(if (selected) active else Color.Transparent)
                    )
                    Text(
                        label,
                        color = if (selected) active else idle,
                        fontSize = 13.sp,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                    )
                }
            }
        }
    }
}
