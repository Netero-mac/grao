package com.netero.grao.ui

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.netero.grao.GraoViewModel
import com.netero.grao.data.Roll
import com.netero.grao.film.Films
import com.netero.grao.ui.theme.GraoColors
import com.netero.grao.ui.theme.MonoFont
import com.netero.grao.ui.theme.TituloFont
import kotlinx.coroutines.delay
import java.io.File
import kotlin.math.ceil
import kotlin.math.sqrt

@Composable
fun DevelopScreen(vm: GraoViewModel, modifier: Modifier = Modifier) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        var tick = 0
        while (true) {
            delay(1000)
            now = System.currentTimeMillis()
            if (++tick % 5 == 0) vm.refresh()
        }
    }

    val developing = vm.rolls.filter { it.inDevelopment }.sortedBy { it.readyAt }
    val first = developing.firstOrNull()

    ShakeDetector(enabled = first != null) { first?.let { vm.shake(it) } }

    Column(
        modifier
            .background(GraoColors.CamaraEscura)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 28.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Text("CÂMARA ESCURA", color = GraoColors.Laranja, fontSize = 12.sp, letterSpacing = 2.sp)

        if (first == null) {
            Text(
                "Nada revelando agora",
                fontFamily = TituloFont, fontWeight = FontWeight.SemiBold, fontSize = 28.sp,
                color = GraoColors.Creme, textAlign = TextAlign.Center,
            )
            Text(
                "Quando o rolo encher, ele vem para cá e revela em até ${com.netero.grao.data.RollRepository.DEVELOP_MINUTES} minutos.",
                color = GraoColors.TextoClaro, fontSize = 14.sp, textAlign = TextAlign.Center,
            )
        } else {
            Text(
                "Rolo cheio!\nHora de revelar",
                fontFamily = TituloFont, fontWeight = FontWeight.SemiBold, fontSize = 32.sp,
                lineHeight = 36.sp, color = GraoColors.Creme, textAlign = TextAlign.Center,
            )
            DevelopingRoll(vm, first, now)
            Button(
                onClick = { vm.shake(first) },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(containerColor = GraoColors.Laranja, contentColor = GraoColors.Preto),
            ) {
                Text("Agitar para revelar mais rápido", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
            Text("ou chacoalhe o celular", color = GraoColors.TextoClaro, fontSize = 12.sp)

            developing.drop(1).forEach { r ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Rolo %02d · ${Films.byId(r.filmId).name}".format(r.number), color = GraoColors.TextoClaro, fontSize = 13.sp)
                    Text(remainingText(r, now), color = GraoColors.Ambar, fontFamily = MonoFont, fontSize = 13.sp)
                }
            }
        }

        val current = vm.currentRoll
        if (current != null && current.shots > 0) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(GraoColors.Grafite, RoundedCornerShape(12.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    "Rolo atual: %02d/%02d · ${Films.byId(current.filmId).name}".format(current.shots, current.capacity),
                    color = GraoColors.Creme, fontSize = 14.sp,
                )
                OutlinedButton(onClick = { vm.rewind() }, modifier = Modifier.fillMaxWidth().height(48.dp)) {
                    Text("Rebobinar e revelar agora", color = GraoColors.Ambar)
                }
            }
        }
    }
}

@Composable
private fun DevelopingRoll(vm: GraoViewModel, roll: Roll, now: Long) {
    val full = roll.fullAt ?: now
    val ready = roll.readyAt ?: now
    val progress = if (ready <= full) 1f else ((now - full).toFloat() / (ready - full)).coerceIn(0f, 1f)
    val cover by produceState<File?>(initialValue = null, roll.id) {
        value = vm.photosOf(roll).firstOrNull()
    }

    Box(
        Modifier
            .width(240.dp)
            .rotate(-2f)
            .shadow(10.dp)
            .background(GraoColors.Papel)
            .padding(start = 12.dp, top = 12.dp, end = 12.dp, bottom = 48.dp)
    ) {
        Box(Modifier.fillMaxWidth().aspectRatio(1f).background(Color2A2420)) {
            cover?.let {
                AsyncImage(
                    model = it,
                    contentDescription = "Foto revelando",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().alpha(0.12f + 0.88f * progress),
                )
            }
        }
    }

    Text("Sua foto está secando…", color = GraoColors.TextoClaro, fontSize = 15.sp)

    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Rolo %02d · ${roll.shots} poses".format(roll.number), color = GraoColors.TextoClaro, fontSize = 13.sp)
            Text(remainingText(roll, now), color = GraoColors.Ambar, fontFamily = MonoFont, fontSize = 13.sp)
        }
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth().height(8.dp),
            color = GraoColors.Laranja,
            trackColor = GraoColors.Marrom,
        )
    }
}

private val Color2A2420 = androidx.compose.ui.graphics.Color(0xFF2A2420)

private fun remainingText(roll: Roll, now: Long): String {
    val ready = roll.readyAt ?: return ""
    val minutes = ceil((ready - now) / 60_000.0).toInt()
    return if (minutes <= 0) "revelando…" else "pronto em $minutes min"
}

@Composable
private fun ShakeDetector(enabled: Boolean, onShake: () -> Unit) {
    val context = LocalContext.current
    val current by rememberUpdatedState(onShake)
    DisposableEffect(enabled) {
        if (!enabled) return@DisposableEffect onDispose {}
        val sm = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val sensor = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        var last = 0L
        val listener = object : SensorEventListener {
            override fun onSensorChanged(e: SensorEvent) {
                val x = e.values[0]
                val y = e.values[1]
                val z = e.values[2]
                val g = sqrt(x * x + y * y + z * z) / SensorManager.GRAVITY_EARTH
                val t = System.currentTimeMillis()
                if (g > 2.3f && t - last > 900) {
                    last = t
                    current()
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
        if (sensor != null) sm.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI)
        onDispose { sm.unregisterListener(listener) }
    }
}
