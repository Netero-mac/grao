package com.netero.grao

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.lifecycle.viewmodel.compose.viewModel
import com.netero.grao.ui.BottomNav
import com.netero.grao.ui.CameraScreen
import com.netero.grao.ui.DevelopScreen
import com.netero.grao.ui.EditorScreen
import com.netero.grao.ui.GalleryScreen
import com.netero.grao.ui.theme.GraoColors
import com.netero.grao.ui.theme.GraoTheme

enum class Screen { Camera, Gallery, Editor, Develop }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        com.netero.grao.ui.theme.AppFonts.load(applicationContext)
        enableEdgeToEdge()
        setContent {
            GraoTheme { GraoApp() }
        }
    }
}

@Composable
fun GraoApp(vm: GraoViewModel = viewModel()) {
    var screen by rememberSaveable { mutableStateOf(Screen.Camera) }

    LaunchedEffect(vm.justFilled) {
        if (vm.justFilled) {
            screen = Screen.Develop
            vm.justFilled = false
        }
    }
    LaunchedEffect(screen) { vm.refresh() }
    BackHandler(enabled = screen != Screen.Camera) { screen = Screen.Camera }

    when (screen) {
        Screen.Camera -> CameraScreen(vm, onOpenGallery = { screen = Screen.Gallery })
        else -> {
            val dark = true
            Column(
                Modifier
                    .fillMaxSize()
                    .background(
                        when (screen) {
                            Screen.Gallery -> GraoColors.Preto
                            Screen.Editor -> GraoColors.Preto
                            else -> GraoColors.CamaraEscura
                        }
                    )
                    .systemBarsPadding()
            ) {
                Box(Modifier.weight(1f)) {
                    when (screen) {
                        Screen.Gallery -> GalleryScreen(vm, Modifier.fillMaxSize()) { file ->
                            vm.editSource = EditSource.FromFile(file)
                            screen = Screen.Editor
                        }
                        Screen.Editor -> EditorScreen(vm, Modifier.fillMaxSize())
                        else -> DevelopScreen(vm, Modifier.fillMaxSize())
                    }
                }
                BottomNav(current = screen, dark = dark, onSelect = { screen = it })
            }
        }
    }
}
