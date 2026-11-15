package com.netero.grao.ui

import android.graphics.Bitmap
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.netero.grao.EditSource
import com.netero.grao.GraoViewModel
import com.netero.grao.data.MediaSaver
import com.netero.grao.film.FilmCategory
import com.netero.grao.film.Films
import com.netero.grao.gl.Adj
import com.netero.grao.gl.EffectParams
import com.netero.grao.gl.Frame
import com.netero.grao.gl.Fx
import com.netero.grao.gl.FxCategory
import com.netero.grao.gl.GlEngine
import com.netero.grao.gl.TextPos
import com.netero.grao.gl.TextStyleKind
import com.netero.grao.ui.theme.GraoColors
import com.netero.grao.ui.theme.TituloFont
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun EditorScreen(vm: GraoViewModel, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) vm.editSource = EditSource.FromUri(uri)
    }
    fun pick() = picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))

    val source = vm.editSource
    if (source == null) {
        Column(
            modifier.background(GraoColors.Preto).padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("Editor", fontFamily = TituloFont, fontWeight = FontWeight.SemiBold, fontSize = 36.sp, color = GraoColors.Creme)
            Text(
                "Escolha uma foto do celular e aplique filmes, efeitos, ajustes, texto e molduras.",
                color = GraoColors.TextoClaro, fontSize = 14.sp, modifier = Modifier.padding(top = 8.dp, bottom = 24.dp),
            )
            Button(
                onClick = { pick() },
                colors = ButtonDefaults.buttonColors(containerColor = GraoColors.Laranja, contentColor = GraoColors.Preto),
                modifier = Modifier.height(52.dp),
            ) { Text("Escolher foto", fontWeight = FontWeight.SemiBold) }
        }
        return
    }

    // Imagens base: original (para salvar), média (para o preview) e pequena (miniaturas)
    var full by remember(source) { mutableStateOf<Bitmap?>(null) }
    var previewBase by remember(source) { mutableStateOf<Bitmap?>(null) }
    var thumbBase by remember(source) { mutableStateOf<Bitmap?>(null) }
    var loadError by remember(source) { mutableStateOf(false) }
    LaunchedEffect(source) {
        try {
            val b = withContext(Dispatchers.IO) {
                when (source) {
                    is EditSource.FromUri -> MediaSaver.load(context, source.uri)
                    is EditSource.FromFile -> MediaSaver.load(source.file)
                }
            }
            full = b
            previewBase = GlEngine.fit(b, 1080)
            thumbBase = GlEngine.fit(b, 200)
        } catch (t: Throwable) {
            Log.e("Grao", "Não abriu a foto", t)
            loadError = true
        }
    }

    var params by remember(source) { mutableStateOf(EffectParams(seed = System.currentTimeMillis() % 100_000)) }
    var rendered by remember(source) { mutableStateOf<ImageBitmap?>(null) }
    var showOriginal by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(params, previewBase) {
        val base = previewBase ?: return@LaunchedEffect
        delay(35)
        try {
            rendered = GlEngine.render(base, params, maxSide = 1080).asImageBitmap()
        } catch (t: Throwable) {
            Log.e("Grao", "Falha no preview", t)
        }
    }

    val thumbs = remember(source) { mutableStateMapOf<String, ImageBitmap>() }
    LaunchedEffect(thumbBase) {
        val base = thumbBase ?: return@LaunchedEffect
        for (f in Films.all) {
            try {
                thumbs[f.id] = GlEngine.render(base, EffectParams(filmId = f.id, seed = 3L), maxSide = 200, withFrame = false).asImageBitmap()
            } catch (t: Throwable) {
                break
            }
        }
    }

    val fxThumbs = remember(source) { mutableStateMapOf<Fx, ImageBitmap>() }
    LaunchedEffect(thumbBase) {
        val base = thumbBase ?: return@LaunchedEffect
        for (fx in Fx.values()) {
            try {
                val p = EffectParams(filterMix = 0f, seed = 5L).withFx(fx, if (fx == Fx.MIRROR) 1f else 0.75f)
                fxThumbs[fx] = GlEngine.render(base, p, maxSide = 200, time = 1.3f, withFrame = false).asImageBitmap()
            } catch (t: Throwable) {
                break
            }
        }
    }

    fun export(share: Boolean) {
        val img = full ?: return
        busy = true
        scope.launch {
            try {
                val motion = params.fx(Fx.MOTION3D)
                if (motion > 0f) {
                    val file = File(context.cacheDir, "grao_3d.mp4")
                    GlEngine.renderMotion(img, params.withFx(Fx.MOTION3D, 0f), file, amount = 0.4f + motion)
                    val uri = withContext(Dispatchers.IO) { MediaSaver.saveVideoFile(context, file) }
                    if (uri == null) toast(context, "Não deu para salvar o vídeo")
                    else if (share) MediaSaver.share(context, uri, "video/mp4")
                    else toast(context, "Vídeo 3D salvo em Movies/Grao")
                } else {
                    val out = GlEngine.render(img, params, maxSide = 3000)
                    val uri = withContext(Dispatchers.IO) { MediaSaver.saveImage(context, out) }
                    if (uri == null) toast(context, "Não deu para salvar")
                    else if (share) MediaSaver.share(context, uri)
                    else toast(context, "Foto salva em Pictures/Grao")
                }
            } catch (t: Throwable) {
                Log.e("Grao", "Falha ao exportar", t)
                toast(context, "Erro ao salvar: ${t.message}")
            } finally {
                busy = false
            }
        }
    }

    var tab by remember { mutableStateOf(0) }

    Column(modifier.background(GraoColors.Preto)) {
        // Barra superior
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = { pick() }) { Text("Trocar foto", color = GraoColors.TextoClaro) }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = { export(share = false) }, enabled = !busy) { Text("Salvar", color = GraoColors.Creme) }
                Button(
                    onClick = { export(share = true) },
                    enabled = !busy,
                    colors = ButtonDefaults.buttonColors(containerColor = GraoColors.Laranja, contentColor = GraoColors.Preto),
                ) { Text("Compartilhar", fontWeight = FontWeight.SemiBold) }
            }
        }

        // Preview: segure para ver a original
        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .pointerInput(Unit) {
                    detectTapGestures(onPress = {
                        showOriginal = true
                        tryAwaitRelease()
                        showOriginal = false
                    })
                },
            contentAlignment = Alignment.Center,
        ) {
            val base = previewBase
            val shown = if (showOriginal) base?.asImageBitmap() else rendered ?: base?.asImageBitmap()
            when {
                loadError -> Text("Não foi possível abrir esta foto.", color = GraoColors.TextoClaro)
                shown == null -> CircularProgressIndicator(color = GraoColors.Laranja)
                else -> Image(shown, contentDescription = "Foto editada", contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize())
            }
            Text(
                if (showOriginal) "ORIGINAL" else "segure para comparar",
                color = GraoColors.Creme,
                fontSize = 11.sp,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(GraoColors.Preto.copy(alpha = 0.6f))
                    .padding(horizontal = 8.dp, vertical = 3.dp),
            )
            if (params.fx(Fx.MOTION3D) > 0f) {
                Text(
                    "3D: vira vídeo ao salvar",
                    color = GraoColors.Ambar,
                    fontSize = 11.sp,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(GraoColors.Preto.copy(alpha = 0.6f))
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                )
            }
            if (busy) CircularProgressIndicator(color = GraoColors.Laranja)
        }

        // Painel da aba atual
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            when (tab) {
                0 -> FiltersTab(vm, params, thumbs) { params = it }
                1 -> EffectsTab(params, fxThumbs) { params = it }
                2 -> AdjustTab(params) { params = it }
                3 -> TextTab(params) { params = it }
                else -> FrameTab(params) { params = it }
            }
        }

        LazyRow(
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            val tabs = listOf("Filtros", "Efeitos", "Ajustes", "Texto", "Moldura")
            items(tabs.size) { i ->
                SegmentedTabs(listOf(tabs[i]), if (tab == i) 0 else -1) { tab = i }
            }
        }
    }
}

private fun toast(context: android.content.Context, msg: String) =
    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()

@Composable
private fun FiltersTab(
    vm: GraoViewModel,
    params: EffectParams,
    thumbs: Map<String, ImageBitmap>,
    onChange: (EffectParams) -> Unit,
) {
    // null = todos; "fav" = favoritos
    var category by remember { mutableStateOf<Any?>(null) }
    val films = Films.all.filter {
        when (val c = category) {
            null -> true
            "fav" -> it.id in vm.favorites
            is FilmCategory -> it.category == c
            else -> true
        }
    }

    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        item { Chip("Todos", category == null) { category = null } }
        item { Chip("Favoritos", category == "fav") { category = "fav" } }
        items(FilmCategory.values().toList()) { c -> Chip(c.label, category == c) { category = c } }
    }

    if (films.isEmpty()) {
        Box(Modifier.fillMaxWidth().height(96.dp), contentAlignment = Alignment.Center) {
            Text("Segure um filtro para favoritar", color = GraoColors.TextoClaro, fontSize = 12.sp)
        }
    } else {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(films, key = { it.id }) { f ->
                val selected = f.id == params.filmId
                Column(
                    Modifier
                        .width(72.dp)
                        .pointerInput(f.id) {
                            detectTapGestures(
                                onTap = { onChange(params.copy(filmId = f.id, dateOn = f.dateByDefault)) },
                                onLongPress = { vm.toggleFavorite(f.id) },
                            )
                        },
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .border(2.dp, if (selected) GraoColors.Laranja else Color.Transparent, RoundedCornerShape(8.dp))
                            .background(GraoColors.Grafite),
                    ) {
                        thumbs[f.id]?.let {
                            Image(it, contentDescription = f.name, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                        }
                        if (f.id in vm.favorites) {
                            Box(
                                Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(4.dp)
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(GraoColors.Ambar)
                            )
                        }
                    }
                    Text(
                        f.name,
                        color = if (selected) GraoColors.Laranja else GraoColors.Creme,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        }
    }
    AmountSlider("Intensidade do filtro", params.filterMix) { onChange(params.copy(filterMix = it)) }
}

@Composable
private fun AdjustTab(params: EffectParams, onChange: (EffectParams) -> Unit) {
    var selected by remember { mutableStateOf(Adj.EXPOSURE) }
    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        items(Adj.values().toList()) { a ->
            Chip(a.label, a == selected, dot = params.adj(a) != 0f) { selected = a }
        }
    }
    AmountSlider(
        label = selected.label,
        value = params.adj(selected),
        range = if (selected.bipolar) -1f..1f else 0f..1f,
    ) { onChange(params.withAdj(selected, it)) }
    TextButton(onClick = { onChange(params.copy(adjust = emptyMap())) }) {
        Text("Zerar ajustes", color = GraoColors.TextoClaro)
    }
}

@Composable
private fun TextTab(params: EffectParams, onChange: (EffectParams) -> Unit) {
    val t = params.text
    OutlinedTextField(
        value = t.text,
        onValueChange = { onChange(params.copy(text = t.copy(text = it.take(80)))) },
        label = { Text("Texto na foto") },
        singleLine = false,
        maxLines = 3,
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = GraoColors.Creme,
            unfocusedTextColor = GraoColors.Creme,
            focusedBorderColor = GraoColors.Laranja,
            unfocusedBorderColor = GraoColors.Marrom,
            focusedLabelColor = GraoColors.Laranja,
            unfocusedLabelColor = GraoColors.TextoClaro,
        ),
        modifier = Modifier.fillMaxWidth(),
    )
    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        items(TextPos.values().toList()) { p -> Chip(p.label, t.pos == p) { onChange(params.copy(text = t.copy(pos = p))) } }
        items(TextStyleKind.values().toList()) { s -> Chip(s.label, t.style == s) { onChange(params.copy(text = t.copy(style = s))) } }
        item { Chip("Data", params.dateOn) { onChange(params.copy(dateOn = !params.dateOn)) } }
    }
    val colors = listOf(
        android.graphics.Color.rgb(242, 232, 213),
        android.graphics.Color.rgb(244, 167, 44),
        android.graphics.Color.rgb(226, 104, 60),
        android.graphics.Color.WHITE,
        android.graphics.Color.rgb(20, 18, 16),
    )
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        colors.forEach { c ->
            Box(
                Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color(c))
                    .border(2.dp, if (t.color == c) GraoColors.Laranja else GraoColors.Marrom, CircleShape)
                    .clickable { onChange(params.copy(text = t.copy(color = c))) }
            )
        }
    }
}

@Composable
private fun FrameTab(params: EffectParams, onChange: (EffectParams) -> Unit) {
    Text("A moldura aparece no preview e na foto salva.", color = GraoColors.TextoClaro, fontSize = 12.sp)
    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        items(Frame.values().toList()) { f -> Chip(f.label, params.frame == f) { onChange(params.copy(frame = f)) } }
    }
}


@Composable
private fun EffectsTab(params: EffectParams, thumbs: Map<Fx, ImageBitmap>, onChange: (EffectParams) -> Unit) {
    var category by remember { mutableStateOf<FxCategory?>(null) }
    var selected by remember { mutableStateOf<Fx?>(null) }
    val active = params.effects.filterValues { it > 0f }.keys
    val list = Fx.values().filter { category == null || it.category == category }

    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        item { Chip("Todos", category == null) { category = null } }
        items(FxCategory.values().toList()) { c -> Chip(c.label, category == c) { category = c } }
        if (active.isNotEmpty()) {
            item { Chip("Limpar (${active.size})", false) { onChange(params.copy(effects = emptyMap())) } }
        }
    }

    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(list, key = { it.name }) { fx ->
            val on = fx in active
            Column(
                Modifier
                    .width(72.dp)
                    .clickable {
                        if (selected == fx && on) {
                            onChange(params.withFx(fx, 0f))
                        } else {
                            selected = fx
                            if (!on) onChange(params.withFx(fx, if (fx == Fx.MIRROR) 1f else 0.6f))
                        }
                    },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .border(
                            2.dp,
                            when {
                                fx == selected -> GraoColors.Laranja
                                on -> GraoColors.Ambar
                                else -> Color.Transparent
                            },
                            RoundedCornerShape(8.dp),
                        )
                        .background(GraoColors.Grafite),
                ) {
                    thumbs[fx]?.let {
                        Image(it, contentDescription = fx.label, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                    }
                    if (on) {
                        Box(
                            Modifier
                                .align(Alignment.TopEnd)
                                .padding(4.dp)
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(GraoColors.Laranja)
                        )
                    }
                }
                Text(
                    fx.label,
                    color = if (on) GraoColors.Laranja else GraoColors.Creme,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }

    val sel = selected
    if (sel != null) {
        AmountSlider("${sel.label}: ${sel.description}", params.fx(sel)) { onChange(params.withFx(sel, it)) }
    } else {
        Text("Toque num efeito para ligar. Toque de novo para desligar. Dá para empilhar vários.", color = GraoColors.TextoClaro, fontSize = 12.sp)
    }
}
