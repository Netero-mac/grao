package com.netero.grao.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.ColorMatrixColorFilter
import android.media.MediaActionSound
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil.compose.AsyncImage
import com.netero.grao.GraoViewModel
import com.netero.grao.film.Film
import com.netero.grao.film.FilmProcessor
import com.netero.grao.film.Films
import com.netero.grao.ui.theme.GraoColors
import com.netero.grao.ui.theme.MonoFont
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.Executors
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.math.hypot
import kotlin.random.Random
import android.graphics.Paint as AndroidPaint

@Composable
fun CameraScreen(vm: GraoViewModel, onOpenGallery: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        hasPermission = it
    }
    LaunchedEffect(Unit) { if (!hasPermission) launcher.launch(Manifest.permission.CAMERA) }

    var lensFacing by remember { mutableIntStateOf(CameraSelector.LENS_FACING_BACK) }
    val imageCapture = remember {
        ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY).build()
    }
    val executor = remember { Executors.newSingleThreadExecutor() }
    val sound = remember { MediaActionSound().apply { load(MediaActionSound.SHUTTER_CLICK) } }
    DisposableEffect(Unit) {
        onDispose {
            executor.shutdown()
            sound.release()
        }
    }
    val flash = remember { Animatable(0f) }

    val film = vm.activeFilm

    fun shoot() {
        if (!hasPermission) return
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

    Column(
        Modifier
            .fillMaxSize()
            .background(GraoColors.Preto)
            .systemBarsPadding()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        TopBar(vm)

        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(6.dp))
                .border(6.dp, Color(0xFF0A0908), RoundedCornerShape(6.dp))
                .background(Color.Black)
        ) {
            if (hasPermission) {
                Viewfinder(film = film, dateOn = vm.dateOn, imageCapture = imageCapture, lensFacing = lensFacing)
            } else {
                Column(
                    Modifier.align(Alignment.Center).padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text("O Grão precisa da câmera para fotografar.", color = GraoColors.Creme)
                    TextButton(onClick = { launcher.launch(Manifest.permission.CAMERA) }) {
                        Text("Permitir câmera", color = GraoColors.Ambar)
                    }
                }
            }
            Box(Modifier.fillMaxSize().background(Color.White.copy(alpha = flash.value)))
        }

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            FilmChips(vm)
            Text(film.description, color = GraoColors.TextoClaro, fontSize = 12.sp)
        }

        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .border(2.dp, GraoColors.Creme, RoundedCornerShape(8.dp))
                    .background(GraoColors.Grafite)
                    .clickable(onClickLabel = "Abrir galeria") { onOpenGallery() }
            ) {
                vm.lastPhoto?.let {
                    AsyncImage(
                        model = it,
                        contentDescription = "Última foto revelada",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }

            Box(
                Modifier
                    .size(84.dp)
                    .clip(CircleShape)
                    .border(4.dp, GraoColors.Creme, CircleShape)
                    .background(GraoColors.Preto)
                    .clickable(onClickLabel = "Tirar foto") { shoot() }
                    .semantics { contentDescription = "Tirar foto" },
                contentAlignment = Alignment.Center,
            ) {
                Box(Modifier.size(64.dp).clip(CircleShape).background(GraoColors.Laranja))
            }

            Box(
                Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .border(1.dp, GraoColors.Marrom, CircleShape)
                    .background(GraoColors.Grafite)
                    .clickable(onClickLabel = "Virar câmera") {
                        lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK)
                            CameraSelector.LENS_FACING_FRONT else CameraSelector.LENS_FACING_BACK
                    },
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Refresh, contentDescription = "Virar câmera", tint = GraoColors.Creme)
            }
        }
    }
}

@Composable
private fun TopBar(vm: GraoViewModel) {
    Row(
        Modifier.fillMaxWidth().height(48.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Pill(label = if (vm.flashOn) "FLASH ON" else "FLASH OFF", active = vm.flashOn) {
            vm.flashOn = !vm.flashOn
        }
        Column(
            Modifier.clickable(enabled = !vm.rollLocked, onClickLabel = "Trocar entre 24 e 36 poses") {
                vm.toggleCapacity()
            },
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
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
            .border(1.dp, if (active) GraoColors.Ambar else GraoColors.Marrom, RoundedCornerShape(22.dp))
            .background(GraoColors.Grafite)
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
    val films = if (vm.rollLocked) listOf(vm.activeFilm) else Films.all
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(vertical = 2.dp)) {
        items(films, key = { it.id }) { f ->
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
                    .clickable { vm.selectFilm(f) }
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
        if (vm.rollLocked) {
            item {
                Box(Modifier.height(44.dp), contentAlignment = Alignment.Center) {
                    Text("filme carregado até o fim do rolo", color = GraoColors.TextoClaro, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun Viewfinder(film: Film, dateOn: Boolean, imageCapture: ImageCapture, lensFacing: Int) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember {
        PreviewView(context).apply {
            // COMPATIBLE usa TextureView, o que permite aplicar o filtro de cor ao vivo
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }

    LaunchedEffect(lensFacing) {
        val provider = context.cameraProvider()
        val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
        val selector = CameraSelector.Builder().requireLensFacing(lensFacing).build()
        try {
            provider.unbindAll()
            provider.bindToLifecycle(lifecycleOwner, selector, preview, imageCapture)
        } catch (e: Exception) {
            Log.e("Grao", "Não deu para abrir a câmera", e)
        }
    }

    Box(Modifier.fillMaxSize()) {
        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize(),
            update = { v ->
                val paint = AndroidPaint().apply { colorFilter = ColorMatrixColorFilter(film.matrix) }
                v.setLayerType(View.LAYER_TYPE_HARDWARE, paint)
            },
        )
        VignetteOverlay(film.vignette)
        GrainOverlay(film.grain)

        if (film.polaroidFrame) {
            Box(
                Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .border(1.dp, GraoColors.Creme.copy(alpha = 0.6f))
            )
        }

        Text(
            film.name.uppercase(),
            color = GraoColors.Creme,
            fontSize = 11.sp,
            letterSpacing = 1.5.sp,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(12.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(GraoColors.Preto.copy(alpha = 0.55f))
                .padding(horizontal = 8.dp, vertical = 4.dp),
        )

        if (film.vhs) {
            ScanlinesOverlay()
            Text(
                "PLAY ▶",
                color = Color.White,
                fontFamily = MonoFont,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                style = TextStyle(shadow = Shadow(color = Color.Black, offset = Offset(2f, 2f), blurRadius = 2f)),
                modifier = Modifier.align(Alignment.TopEnd).padding(16.dp),
            )
        } else if (dateOn) {
            Text(
                FilmProcessor.dateText(),
                color = GraoColors.Ambar,
                fontFamily = MonoFont,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                style = TextStyle(shadow = Shadow(color = GraoColors.Ambar.copy(alpha = 0.8f), blurRadius = 12f)),
                modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
            )
        }
    }
}

@Composable
private fun VignetteOverlay(strength: Float) {
    Box(
        Modifier
            .fillMaxSize()
            .drawWithCache {
                val r = hypot(size.width, size.height) / 2f
                val brush = Brush.radialGradient(
                    0f to Color.Transparent,
                    0.55f to Color.Transparent,
                    1f to Color.Black.copy(alpha = (strength * 0.9f).coerceIn(0f, 1f)),
                    center = Offset(size.width / 2f, size.height / 2f),
                    radius = r,
                )
                onDrawBehind { drawRect(brush) }
            }
    )
}

@Composable
private fun ScanlinesOverlay() {
    Canvas(Modifier.fillMaxSize()) {
        val step = 3.dp.toPx()
        val line = step / 2f
        var y = 0f
        while (y < size.height) {
            drawRect(Color.Black.copy(alpha = 0.18f), topLeft = Offset(0f, y), size = Size(size.width, line))
            y += step
        }
    }
}

@Composable
private fun GrainOverlay(amount: Float) {
    if (amount <= 0f) return
    val brush = remember {
        val tile = FilmProcessor.noiseTile(256, 42L).asImageBitmap()
        ShaderBrush(ImageShader(tile, TileMode.Repeated, TileMode.Repeated))
    }
    var offset by remember { mutableStateOf(Offset.Zero) }
    LaunchedEffect(Unit) {
        while (true) {
            offset = Offset(Random.nextFloat() * 256f, Random.nextFloat() * 256f)
            delay(90)
        }
    }
    Canvas(Modifier.fillMaxSize()) {
        translate(-offset.x, -offset.y) {
            drawRect(
                brush = brush,
                topLeft = Offset.Zero,
                size = Size(size.width + 256f, size.height + 256f),
                alpha = amount * 0.2f,
            )
        }
    }
}

private suspend fun Context.cameraProvider(): ProcessCameraProvider = suspendCoroutine { cont ->
    val future = ProcessCameraProvider.getInstance(this)
    future.addListener({ cont.resume(future.get()) }, ContextCompat.getMainExecutor(this))
}
