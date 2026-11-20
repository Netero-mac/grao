package com.netero.grao.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.netero.grao.GraoViewModel
import com.netero.grao.film.FilmProcessor
import com.netero.grao.film.Films
import com.netero.grao.ui.theme.GraoColors
import com.netero.grao.ui.theme.MonoFont
import com.netero.grao.ui.theme.TituloFont
import java.io.File

@Composable
fun GalleryScreen(vm: GraoViewModel, modifier: Modifier = Modifier, onEdit: (File) -> Unit = {}) {
    val developed = vm.rolls.filter { it.developed }
    var selectedId by remember { mutableStateOf<String?>(null) }
    val roll = developed.firstOrNull { it.id == selectedId } ?: developed.firstOrNull()
    val photos by produceState(initialValue = emptyList<File>(), roll?.id, roll?.shots) {
        value = (roll?.let { vm.photosOf(it) } ?: emptyList()).reversed()
    }
    var viewing by remember { mutableStateOf<File?>(null) }

    Column(modifier.background(GraoColors.Preto)) {
        Row(
            Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 8.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("Galeria", fontFamily = TituloFont, fontWeight = FontWeight.SemiBold, fontSize = 34.sp, color = GraoColors.Creme)
            if (roll != null) {
                Text(
                    "${Films.byId(roll.filmId).name} · ${roll.shots}".uppercase(),
                    fontFamily = MonoFont, fontSize = 12.sp, color = GraoColors.Ambar,
                )
            }
        }

        if (roll == null) {
            Column(
                Modifier.fillMaxWidth().weight(1f).padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("Nada por aqui ainda", fontSize = 18.sp, color = GraoColors.Creme, fontWeight = FontWeight.SemiBold)
                Text(
                    "Tire fotos no modo Instantâneo ou encha um rolo e espere a revelação.",
                    fontSize = 14.sp, color = GraoColors.TextoClaro, modifier = Modifier.padding(top = 8.dp),
                )
            }
            return@Column
        }

        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(developed, key = { it.id }) { r ->
                Chip(
                    label = if (r.number == 0) "Instantâneas" else "Rolo %02d".format(r.number),
                    selected = r.id == roll.id,
                ) { selectedId = r.id }
            }
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(2.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            items(photos, key = { it.path }) { file -> Thumb(file) { viewing = file } }
        }
    }

    viewing?.let { f ->
        PhotoViewer(f, onEdit = {
            viewing = null
            onEdit(f)
        }) { viewing = null }
    }
}

@Composable
private fun Thumb(file: File, onClick: () -> Unit) {
    val context = LocalContext.current
    AsyncImage(
        model = ImageRequest.Builder(context).data(file).size(360).crossfade(true).build(),
        contentDescription = "Foto",
        contentScale = ContentScale.Crop,
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .background(GraoColors.Grafite)
            .clickable(onClickLabel = "Ver foto", onClick = onClick),
    )
}

@Composable
private fun PhotoViewer(file: File, onEdit: () -> Unit, onClose: () -> Unit) {
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        val zoom = rememberZoomState(file)
        Box(Modifier.fillMaxSize().background(GraoColors.Preto)) {
            Box(Modifier.fillMaxSize().padding(bottom = 96.dp, top = 56.dp).clipToBounds().zoomGestures(zoom)) {
                AsyncImage(
                    model = file,
                    contentDescription = "Foto (pinça para ampliar)",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize().zoomed(zoom),
                )
            }
            TextButton(onClick = onClose, modifier = Modifier.align(Alignment.TopStart).padding(8.dp)) {
                Text("Fechar", color = GraoColors.Creme)
            }
            Text(
                FilmProcessor.dateText(file.lastModified()),
                color = GraoColors.Ambar, fontFamily = MonoFont, fontSize = 13.sp,
                modifier = Modifier.align(Alignment.TopEnd).padding(20.dp),
            )
            Button(
                onClick = onEdit,
                colors = ButtonDefaults.buttonColors(containerColor = GraoColors.Laranja, contentColor = GraoColors.Preto),
                modifier = Modifier.align(Alignment.BottomCenter).padding(28.dp),
            ) { Text("Editar e adicionar efeitos", fontWeight = FontWeight.SemiBold) }
        }
    }
}
