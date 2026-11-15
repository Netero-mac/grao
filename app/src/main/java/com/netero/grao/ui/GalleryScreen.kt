package com.netero.grao.ui

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
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
        value = roll?.let { vm.photosOf(it) } ?: emptyList()
    }
    var viewing by remember { mutableStateOf<File?>(null) }

    Column(modifier) {
        Column(Modifier.padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 8.dp)) {
            Text("Grão", fontFamily = TituloFont, fontWeight = FontWeight.SemiBold, fontSize = 40.sp, color = GraoColors.Marrom)
            Text("Seus rolos revelados", fontSize = 14.sp, color = GraoColors.MarromClaro)
        }

        if (roll == null) {
            Column(
                Modifier.fillMaxWidth().weight(1f).padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("Nenhum rolo revelado ainda", fontSize = 18.sp, color = GraoColors.Marrom, fontWeight = FontWeight.SemiBold)
                Text(
                    "Fotografe até encher o rolo (ou rebobine) e espere a revelação.",
                    fontSize = 14.sp,
                    color = GraoColors.MarromClaro,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            return@Column
        }

        LazyRow(
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(developed, key = { it.id }) { r ->
                val selected = r.id == roll.id
                Box(
                    Modifier
                        .height(44.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(if (selected) GraoColors.Marrom else GraoColors.Creme)
                        .border(1.dp, if (selected) GraoColors.Marrom else GraoColors.Borda, RoundedCornerShape(22.dp))
                        .clickable { selectedId = r.id }
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        if (r.number == 0) "Instantâneas" else "Rolo %02d".format(r.number),
                        color = if (selected) GraoColors.Creme else GraoColors.Marrom,
                        fontSize = 13.sp,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                    )
                }
            }
        }

        Row(
            Modifier.fillMaxWidth().padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                "${Films.byId(roll.filmId).name} · ${roll.shots} poses".uppercase(),
                fontSize = 12.sp,
                letterSpacing = 1.5.sp,
                color = GraoColors.MarromClaro,
            )
            Text(FilmProcessor.dateText(roll.createdAt), fontFamily = MonoFont, fontSize = 14.sp, color = GraoColors.Ferrugem)
        }

        // Grade compacta: 3 colunas de miniaturas pequenas
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(photos, key = { it.path }) { file ->
                Thumb(file) { viewing = file }
            }
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
    Box(
        Modifier
            .shadow(2.dp)
            .background(GraoColors.Papel)
            .clickable(onClickLabel = "Ver foto", onClick = onClick)
            .padding(start = 4.dp, top = 4.dp, end = 4.dp, bottom = 12.dp)
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context).data(file).size(300).crossfade(true).build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxWidth().aspectRatio(1f),
        )
    }
}

@Composable
private fun PhotoViewer(file: File, onEdit: () -> Unit, onClose: () -> Unit) {
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(
            Modifier
                .fillMaxSize()
                .background(GraoColors.Preto)
                .clickable(onClickLabel = "Fechar", onClick = onClose),
            contentAlignment = Alignment.Center,
        ) {
            AsyncImage(
                model = file,
                contentDescription = "Foto revelada",
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize().padding(16.dp),
            )
            androidx.compose.material3.Button(
                onClick = onEdit,
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = GraoColors.Laranja,
                    contentColor = GraoColors.Preto,
                ),
                modifier = Modifier.align(Alignment.BottomCenter).padding(32.dp),
            ) { Text("Editar esta foto") }
        }
    }
}
