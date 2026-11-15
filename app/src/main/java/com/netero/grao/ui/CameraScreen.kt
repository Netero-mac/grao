package com.netero.grao.ui

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaActionSound
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraEffect
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.UseCase
import androidx.camera.core.UseCaseGroup
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FallbackStrategy
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil.compose.AsyncImage
import com.netero.grao.data.MediaSaver
import com.netero.grao.GraoViewModel
import com.netero.grao.film.Film
import com.netero.grao.film.FilmCategory
import com.netero.grao.film.Films
import com.netero.grao.gl.CameraProcessor
import com.netero.grao.gl.Fx
import com.netero.grao.gl.GraoCameraEffect
import com.netero.grao.ui.theme.GraoColors
import com.netero.grao.ui.theme.MonoFont
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.Executors
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@Composable
fun CameraScreen(vm: GraoViewModel, onOpenGallery: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    var hasCamera by remember { mutableStateOf(granted(context, Manifest.permission.CAMERA)) }
    var hasAudio by remember { mutableStateOf(granted(context, Manifest.permission.RECORD_AUDIO)) }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { hasCamera = it }
    val audioLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { hasAudio = it }
    LaunchedEffect(Unit) { if (!hasCamera) cameraLauncher.launch(Manifest.permission.CAMERA) }

    var lensFacing by remember { mutableIntStateOf(CameraSelector.LENS_FACING_BACK) }
    var videoMode by remember { mutableStateOf(false) }
    var showFx by remember { mutableStateOf(false) }
    var videoAvailable by remember { mutableStateOf(true) }
    var videoWithEffects by remember { mutableStateOf(true) }
    var recording by remember { mutableStateOf<Recording?>(null) }
    var recSeconds by remember { mutableIntStateOf(0) }

    val imageCapture = remember {
        ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY).build()
    }
    val videoCapture = remember {
        val recorder = Recorder.Builder()
            .setQualitySelector(QualitySelector.from(Quality.HD, FallbackStrategy.lowerQualityOrHigherThan(Quality.SD)))
            .build()
        VideoCapture.withOutput(recorder)
    }
    val processor = remember { CameraProcessor() }
    val effect = remember { GraoCameraEffect(processor) }
    val executor = remember { Executors.newSingleThreadExecutor() }
    val sound = remember { MediaActionSound().apply { load(MediaActionSound.SHUTTER_CLICK) } }
    DisposableEffect(Unit) {
        onDispose {
            recording?.stop()
            executor.shutdown()
            sound.release()
            processor.release()
        }
    }
    val flash = remember { Animatable(0f) }

    // O visor ao vivo recebe sempre os parâmetros atuais
    val params = vm.cameraParams
    SideEffect { processor.params = params }

    val previewView = remember {
        PreviewView(context).apply {
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }

    // Liga só o que o modo atual precisa: muitos celulares não aguentam foto + vídeo + efeitos juntos
    LaunchedEffect(lensFacing, hasCamera, videoMode) {
        if (!hasCamera) return@LaunchedEffect
        val provider = context.cameraProvider()
        val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
        val selector = CameraSelector.Builder().requireLensFacing(lensFacing).build()
        if (videoMode) {
            when {
                tryBind(provider, lifecycleOwner, selector, listOf(preview, videoCapture), effect) -> {
                    videoAvailable = true
                    videoWithEffects = true
                }
                tryBind(provider, lifecycleOwner, selector, listOf(preview, videoCapture), null) -> {
                    videoAvailable = true
                    videoWithEffects = false
                    Toast.makeText(context, "Neste celular o vídeo grava sem os efeitos", Toast.LENGTH_LONG).show()
                }
                else -> {
                    videoAvailable = false
                    tryBind(provider, lifecycleOwner, selector, listOf(preview), effect)
                    Toast.makeText(context, "Não foi possível abrir a câmera para vídeo", Toast.LENGTH_LONG).show()
                }
            }
        } else {
            if (!tryBind(provider, lifecycleOwner, selector, listOf(preview, imageCapture), effect)) {
                tryBind(provider, lifecycleOwner, selector, listOf(preview, imageCapture), null)
            }
        }
    }

    val film = vm.activeFilm

    fun shoot() {
        if (!hasCamera) return
        val mirror = lensFacing == CameraSelector.LENS_FACING_FRONT
        val main = ContextCompat.getMainExecutor(context)
        imageCapture.flashMode = if (vm.flashOn) ImageCapture.FLASH_MODE_ON else ImageCapture.FLASH_MODE_OFF
        sound.play(MediaActionSound.SHUTTER_CLICK)
        scope.launch {
            flash.snapTo(0.85f)
            flash.animateTo(0f, tween(260))
        }
        imageCapture.takePicture(executor, object : ImageCapture.OnImageCapturedCallback() {
            override fun onCaptureSuccess(image: ImageProxy) {
                val bitmap = image.toBitmap()
                val rotation = image.imageInfo.rotationDegrees
                image.close()
                main.execute { vm.onCaptured(bitmap, rotation, mirror) }
            }

            override fun onError(exception: ImageCaptureException) {
                Log.e("Grao", "Erro ao tirar foto", exception)
                main.execute { Toast.makeText(context, "Não deu para tirar a foto", Toast.LENGTH_SHORT).show() }
            }
        })
    }

    fun toggleRecord() {
        val current = recording
        if (current != null) {
            current.stop()
            recording = null
            return
        }
        if (!videoAvailable) {
            Toast.makeText(context, "A câmera de vídeo não abriu neste celular", Toast.LENGTH_SHORT).show()
            return
        }
        // Grava primeiro num arquivo do próprio app (sempre funciona) e depois copia para a galeria
        val dir = File(context.getExternalFilesDir(null) ?: context.filesDir, "videos").apply { mkdirs() }
        val file = File(dir, "grao_${System.currentTimeMillis()}.mp4")
        val opts = FileOutputOptions.Builder(file).build()
        var pending = videoCapture.output.prepareRecording(context, opts)
        if (hasAudio) {
            pending = try {
                pending.withAudioEnabled()
            } catch (e: SecurityException) {
                pending
            }
        }
        recSeconds = 0
        recording = try { pending.start(ContextCompat.getMainExecutor(context)) { ev ->
            when (ev) {
                is VideoRecordEvent.Status -> recSeconds = (ev.recordingStats.recordedDurationNanos / 1_000_000_000L).toInt()
                is VideoRecordEvent.Finalize -> {
                    recording = null
                    if (ev.hasError() && ev.error != VideoRecordEvent.Finalize.ERROR_SOURCE_INACTIVE || !file.exists() || file.length() == 0L) {
                        Log.e("Grao", "Erro na gravação: ${ev.error}", ev.cause)
                        Toast.makeText(context, "Erro no vídeo: ${videoErrorText(ev.error)}", Toast.LENGTH_LONG).show()
                        file.delete()
                    } else {
                        scope.launch {
                            val uri = withContext(Dispatchers.IO) { MediaSaver.saveVideoFile(context, file, file.name) }
                            if (uri != null) {
                                file.delete()
                                Toast.makeText(context, "Vídeo salvo na galeria (Movies/Grao)", Toast.LENGTH_LONG).show()
                            } else {
                                Toast.makeText(context, "Vídeo salvo em ${file.parentFile?.path}", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                }
                else -> Unit
            }
        } } catch (e: Exception) {
            Log.e("Grao", "Falha ao iniciar gravação", e)
            Toast.makeText(context, "Não deu para começar a gravar: ${e.message}", Toast.LENGTH_LONG).show()
            null
        }
    }

    // Visor em tela cheia, controles flutuando por cima
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        if (hasCamera) {
            AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
            if (film.polaroidFrame) {
                Box(
                    Modifier
                        .align(Alignment.Center)
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .border(1.dp, GraoColors.Creme.copy(alpha = 0.6f))
                )
            }
        } else {
            Column(
                Modifier.align(Alignment.Center).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("O Grão precisa da câmera para fotografar.", color = GraoColors.Creme)
                TextButton(onClick = { cameraLauncher.launch(Manifest.permission.CAMERA) }) {
                    Text("Permitir câmera", color = GraoColors.Ambar)
                }
            }
        }
        Box(Modifier.fillMaxSize().background(Color.White.copy(alpha = flash.value)))

        // Topo
        Column(
            Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.7f), Color.Transparent)))
                .systemBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            TopBar(vm, recording != null, recSeconds)
            Text(
                film.name.uppercase(),
                color = GraoColors.Creme,
                fontSize = 11.sp,
                letterSpacing = 1.5.sp,
                modifier = Modifier
                    .padding(top = 8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(GraoColors.Preto.copy(alpha = 0.55f))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }

        // Base
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f), Color.Black)))
                .systemBarsPadding()
                .padding(start = 16.dp, end = 16.dp, top = 40.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                if (showFx) {
                    EffectsPanel(
                        effects = vm.cameraEffects,
                        onChange = { fx, v -> vm.setCameraFx(fx, v) },
                        exclude = setOf(Fx.MOTION3D),
                    )
                } else {
                    FilmChips(vm)
                    Text(film.description, color = GraoColors.TextoClaro, fontSize = 12.sp)
                }
            }

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Pill(label = if (showFx) "FILMES" else "FX", active = showFx || vm.cameraEffects.isNotEmpty()) { showFx = !showFx }
                SegmentedTabs(listOf("FOTO", "VÍDEO"), if (videoMode) 1 else 0) { i ->
                    if (recording != null) return@SegmentedTabs
                    videoMode = i == 1
                    if (videoMode && !hasAudio) audioLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }
                Pill(label = if (vm.instantMode) "INSTANT" else "ROLO", active = !vm.instantMode) {
                    if (recording == null) vm.toggleInstant()
                }
            }

            Row(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .border(2.dp, GraoColors.Creme, RoundedCornerShape(12.dp))
                        .background(GraoColors.Grafite)
                        .clickable(onClickLabel = "Abrir galeria") { onOpenGallery() }
                ) {
                    vm.lastPhoto?.let {
                        AsyncImage(
                            model = it,
                            contentDescription = "Última foto",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }

                val label = when {
                    !videoMode -> "Tirar foto"
                    recording != null -> "Parar gravação"
                    else -> "Gravar vídeo"
                }
                Box(
                    Modifier
                        .size(84.dp)
                        .clip(CircleShape)
                        .border(4.dp, Color.White, CircleShape)
                        .clickable(onClickLabel = label) { if (videoMode) toggleRecord() else shoot() }
                        .semantics { contentDescription = label },
                    contentAlignment = Alignment.Center,
                ) {
                    when {
                        !videoMode -> Box(Modifier.size(66.dp).clip(CircleShape).background(Color.White))
                        recording != null -> Box(Modifier.size(32.dp).clip(RoundedCornerShape(6.dp)).background(Color(0xFFFF3B30)))
                        else -> Box(Modifier.size(66.dp).clip(CircleShape).background(Color(0xFFFF3B30)))
                    }
                }

                Box(
                    Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.15f))
                        .clickable(onClickLabel = "Virar câmera") {
                            if (recording != null) return@clickable
                            lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK)
                                CameraSelector.LENS_FACING_FRONT else CameraSelector.LENS_FACING_BACK
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.Refresh, contentDescription = "Virar câmera", tint = Color.White)
                }
            }
        }
    }
}

private fun tryBind(
    provider: ProcessCameraProvider,
    owner: LifecycleOwner,
    selector: CameraSelector,
    useCases: List<UseCase>,
    effect: CameraEffect?,
): Boolean = try {
    provider.unbindAll()
    val group = UseCaseGroup.Builder().apply {
        useCases.forEach { addUseCase(it) }
        effect?.let { addEffect(it) }
    }.build()
    provider.bindToLifecycle(owner, selector, group)
    true
} catch (e: Exception) {
    Log.w("Grao", "Combinação de câmera não suportada: ${useCases.size} casos, efeito=${effect != null}", e)
    false
}

private fun videoErrorText(code: Int): String = when (code) {
    VideoRecordEvent.Finalize.ERROR_INSUFFICIENT_STORAGE -> "sem espaço no celular"
    VideoRecordEvent.Finalize.ERROR_NO_VALID_DATA -> "nenhum quadro foi gravado (grave por mais de 1 segundo)"
    VideoRecordEvent.Finalize.ERROR_ENCODING_FAILED -> "o codificador de vídeo falhou"
    VideoRecordEvent.Finalize.ERROR_SOURCE_INACTIVE -> "a câmera parou durante a gravação"
    VideoRecordEvent.Finalize.ERROR_INVALID_OUTPUT_OPTIONS -> "não deu para criar o arquivo"
    VideoRecordEvent.Finalize.ERROR_FILE_SIZE_LIMIT_REACHED -> "limite de tamanho atingido"
    VideoRecordEvent.Finalize.ERROR_RECORDER_ERROR -> "erro do gravador"
    else -> "código $code"
}

private fun granted(context: Context, permission: String) =
    ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

@Composable
private fun TopBar(vm: GraoViewModel, recording: Boolean, recSeconds: Int) {
    Row(
        Modifier.fillMaxWidth().height(48.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Pill(label = if (vm.flashOn) "FLASH ON" else "FLASH OFF", active = vm.flashOn) {
            vm.flashOn = !vm.flashOn
        }
        Column(
            Modifier.clickable(enabled = !vm.rollLocked && !vm.instantMode, onClickLabel = "Trocar entre 24 e 36 poses") {
                vm.toggleCapacity()
            },
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            when {
                recording -> {
                    Text(
                        "REC %02d:%02d".format(recSeconds / 60, recSeconds % 60),
                        color = Color(0xFFFF5A4A),
                        fontFamily = MonoFont,
                        fontSize = 22.sp,
                    )
                    Text("gravando com efeitos", color = GraoColors.TextoClaro, fontSize = 11.sp)
                }
                vm.instantMode -> {
                    Text("INST", color = GraoColors.Ambar, fontFamily = MonoFont, fontSize = 22.sp, letterSpacing = 2.sp)
                    Text("fotos na hora", color = GraoColors.TextoClaro, fontSize = 11.sp)
                }
                else -> {
                    Text(
                        "%02d/%02d".format(vm.shotsTaken, vm.capacity),
                        color = GraoColors.Ambar,
                        fontFamily = MonoFont,
                        fontSize = 22.sp,
                        letterSpacing = 2.sp,
                    )
                    Text(
                        if (vm.rollLocked) "restam ${vm.capacity - vm.shotsTaken} poses" else "toque: 24 ou 36 poses",
                        color = GraoColors.TextoClaro,
                        fontSize = 11.sp,
                    )
                }
            }
        }
        Pill(label = if (vm.dateOn) "DATA ON" else "DATA OFF", active = vm.dateOn) {
            vm.dateOn = !vm.dateOn
        }
    }
}

@Composable
private fun Pill(label: String, active: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .height(44.dp)
            .clip(RoundedCornerShape(22.dp))
            .border(1.dp, if (active) GraoColors.Ambar else Color.White.copy(alpha = 0.2f), RoundedCornerShape(22.dp))
            .background(Color.Black.copy(alpha = 0.45f))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            color = if (active) GraoColors.Ambar else GraoColors.TextoClaro,
            fontFamily = MonoFont,
            fontSize = 12.sp,
        )
    }
}

@Composable
private fun FilmChips(vm: GraoViewModel) {
    var pending by remember { mutableStateOf<Film?>(null) }

    // Abas de categoria em cima, filmes daquela categoria embaixo
    val groups = buildList {
        val favs = Films.all.filter { it.id in vm.favorites }
        if (favs.isNotEmpty()) add("Favoritos" to favs)
        FilmCategory.values().forEach { c ->
            val list = Films.all.filter { it.category == c }
            if (list.isNotEmpty()) add(c.label to list)
        }
    }
    var chosen by remember { mutableStateOf<String?>(null) }
    val defaultTab = groups.indexOfFirst { g -> g.first != "Favoritos" && g.second.any { it.id == vm.activeFilm.id } }.coerceAtLeast(0)
    val tab = groups.indexOfFirst { it.first == chosen }.takeIf { it >= 0 } ?: defaultTab
    CategoryTabs(groups.map { it.first to it.second.size }, tab) { chosen = groups[it].first }

    // Com o rolo em uso, trocar de filme pede confirmação.
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(vertical = 2.dp)) {
        items(groups.getOrNull(tab)?.second ?: emptyList(), key = { it.id }) { f ->
            val selected = f.id == vm.activeFilm.id
            Box(
                Modifier
                    .height(44.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(if (selected) GraoColors.Laranja else GraoColors.Grafite)
                    .border(
                        BorderStroke(1.dp, if (selected) GraoColors.Laranja else GraoColors.Marrom),
                        RoundedCornerShape(22.dp),
                    )
                    .clickable {
                        when {
                            selected -> Unit
                            vm.rollLocked -> pending = f
                            else -> vm.selectFilm(f)
                        }
                    }
                    .padding(horizontal = 14.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    f.name,
                    color = if (selected) GraoColors.Preto else GraoColors.Creme,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                    fontSize = 13.sp,
                )
            }
        }
    }

    pending?.let { f ->
        AlertDialog(
            onDismissRequest = { pending = null },
            title = { Text("Trocar para ${f.name}?") },
            text = {
                Text(
                    "O rolo atual (${vm.shotsTaken}/${vm.capacity} · ${vm.activeFilm.name}) vai ser " +
                        "rebobinado e enviado para revelar. Depois você começa um rolo novo com ${f.name}."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.switchFilm(f)
                    pending = null
                }) { Text("Trocar filme") }
            },
            dismissButton = {
                TextButton(onClick = { pending = null }) { Text("Cancelar") }
            },
        )
    }
}

private suspend fun Context.cameraProvider(): ProcessCameraProvider = suspendCoroutine { cont ->
    val future = ProcessCameraProvider.getInstance(this)
    future.addListener({ cont.resume(future.get()) }, ContextCompat.getMainExecutor(this))
}
