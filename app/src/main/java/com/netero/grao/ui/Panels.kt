package com.netero.grao.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.netero.grao.gl.Fx
import com.netero.grao.ui.theme.GraoColors
import kotlin.math.roundToInt

/** Chip arredondado reaproveitado em várias telas. */
@Composable
fun Chip(label: String, selected: Boolean, modifier: Modifier = Modifier, dot: Boolean = false, onClick: () -> Unit) {
    Box(
        modifier
            .height(40.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(if (selected) GraoColors.Laranja else GraoColors.Grafite)
            .border(1.dp, if (selected) GraoColors.Laranja else GraoColors.Marrom, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            if (dot) Box(Modifier.size(6.dp).clip(CircleShape).background(if (selected) GraoColors.Preto else GraoColors.Ambar))
            Text(
                label,
                color = if (selected) GraoColors.Preto else GraoColors.Creme,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                fontSize = 13.sp,
            )
        }
    }
}

/** Barra de intensidade com o valor em % ao lado. */
@Composable
fun AmountSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float> = 0f..1f,
    onChange: (Float) -> Unit,
) {
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = GraoColors.TextoClaro, fontSize = 12.sp)
            Text("${(value * 100).roundToInt()}", color = GraoColors.Ambar, fontSize = 12.sp)
        }
        Slider(
            value = value,
            onValueChange = onChange,
            valueRange = range,
            colors = SliderDefaults.colors(
                thumbColor = GraoColors.Laranja,
                activeTrackColor = GraoColors.Laranja,
                inactiveTrackColor = GraoColors.Marrom,
            ),
            modifier = Modifier.height(36.dp),
        )
    }
}

/**
 * Lista de efeitos empilháveis: toque escolhe qual ajustar, a barra muda a força.
 * Um ponto marca os que estão ligados.
 */
@Composable
fun EffectsPanel(
    effects: Map<Fx, Float>,
    onChange: (Fx, Float) -> Unit,
    exclude: Set<Fx> = emptySet(),
) {
    val list = Fx.values().filter { it !in exclude }
    var selected by remember { mutableStateOf(list.first()) }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(vertical = 2.dp)) {
            items(list, key = { it.name }) { fx ->
                Chip(
                    label = fx.label,
                    selected = fx == selected,
                    dot = (effects[fx] ?: 0f) > 0f,
                ) {
                    if (fx == selected) {
                        // segundo toque liga/desliga
                        onChange(fx, if ((effects[fx] ?: 0f) > 0f) 0f else 0.6f)
                    } else {
                        selected = fx
                        if ((effects[fx] ?: 0f) == 0f) onChange(fx, 0.6f)
                    }
                }
            }
        }
        Text(selected.description, color = GraoColors.TextoClaro, fontSize = 12.sp)
        AmountSlider(label = "Força: ${selected.label}", value = effects[selected] ?: 0f) { onChange(selected, it) }
    }
}

@Composable
fun SegmentedTabs(options: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        options.forEachIndexed { i, label ->
            Box(
                Modifier
                    .height(40.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (i == selected) GraoColors.Creme else Color.Transparent)
                    .clickable { onSelect(i) }
                    .padding(horizontal = 14.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label,
                    color = if (i == selected) GraoColors.Preto else GraoColors.TextoClaro,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.sp,
                )
            }
        }
    }
}

@Composable
fun Spacer8() = Box(Modifier.width(8.dp))

/**
 * Abas de categoria em texto (diferente dos itens): a ativa fica laranja e sublinhada.
 * Cada aba mostra quantos itens tem.
 */
@Composable
fun CategoryTabs(tabs: List<Pair<String, Int>>, selected: Int, onSelect: (Int) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(18.dp), contentPadding = PaddingValues(horizontal = 2.dp)) {
        items(tabs.size) { i ->
            val (label, count) = tabs[i]
            val on = i == selected
            Column(
                Modifier
                    .height(40.dp)
                    .clickable { onSelect(i) },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        label.uppercase(),
                        color = if (on) GraoColors.Laranja else GraoColors.TextoClaro,
                        fontSize = 13.sp,
                        fontWeight = if (on) FontWeight.Bold else FontWeight.Medium,
                        letterSpacing = 1.sp,
                    )
                    Text(
                        " $count",
                        color = if (on) GraoColors.Laranja.copy(alpha = 0.7f) else GraoColors.TextoClaro.copy(alpha = 0.5f),
                        fontSize = 10.sp,
                    )
                }
                Box(
                    Modifier
                        .padding(top = 4.dp)
                        .size(width = 18.dp, height = 2.dp)
                        .background(if (on) GraoColors.Laranja else Color.Transparent)
                )
            }
        }
    }
}
