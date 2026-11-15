# Grão

App Android de câmera com efeitos de filme analógico. Você fotografa com um "rolo" (24 ou 36 poses), não vê as fotos na hora e espera a revelação.

## O que já funciona

- **Visor ao vivo com o filme aplicado**: cor do filme, vinheta, grão animado e data laranja opcional
- **35 filtros** em categorias (Filme, P&B, Vintage, Vídeo, Tons, Arte): Portra, Gold, Ektar, CineStill, Huji, CPM35, VHS, Teal & Orange, Miami, Y2K, Kidcore, Indie, Vapor, Cyber, Pôr do sol, Anos 70, Duotones e mais
- **22 efeitos empilháveis** com intensidade: Brilho, Diamante, Glitter, Festa, Luz lateral, Arco-íris, Sonho, Halação, Grão, Light leak, Poeira, VHS, Glitch, RGB Split, Prisma, Pixel, Olho de peixe, Espelho, Caleidoscópio, Quadrinho, Retícula, Térmica e 3D
- **Editor**: foto do celular, antes/depois, miniaturas, favoritos, ajustes, texto, molduras, salvar e compartilhar
- **Câmera com efeitos ao vivo na GPU**, gravação de vídeo e modo Instantâneo/Rolo
- **Modo rolo**: 24 ou 36 poses (toque no contador antes da primeira foto). O filme fica travado até o fim do rolo
- **Revelação**: quando o rolo enche, ele leva 60 minutos para revelar. Agitar (botão ou chacoalhar o celular) adianta 5 minutos. Também dá para rebobinar e revelar um rolo pela metade
- **Galeria compacta**: 3 colunas de miniaturas por rolo; toque para ver a foto em tela cheia
- As fotos reveladas também vão para a galeria do celular, em `Pictures/Grao`

## Como gerar o APK

1. Suba o projeto para um repositório no GitHub (branch `main`)
2. O workflow `.github/workflows/build.yml` roda sozinho a cada push (ou manualmente em *Actions → Gerar APK → Run workflow*)
3. Baixe o APK direto pelo link fixo: https://github.com/Netero-mac/grao/releases/latest/download/grao.apk

Localmente: `gradle assembleDebug` (Gradle 8.7 + JDK 17), ou abra no Android Studio.

## Estrutura

```
app/src/main/java/com/netero/grao/
├── MainActivity.kt        navegação entre Câmera, Galeria e Revelar
├── GraoViewModel.kt       estado do app (rolo atual, filme, flash, data)
├── film/
│   ├── Film.kt            definição dos filmes (cor, grão, vinheta, leaks)
│   └── FilmProcessor.kt   aplica o efeito na foto final
├── data/
│   └── RollRepository.kt  rolos e fotos em arquivos + exportação para a galeria
└── ui/
    ├── CameraScreen.kt    visor (CameraX) e controles
    ├── GalleryScreen.kt   galeria em grade
    ├── DevelopScreen.kt   câmara escura / revelação
    ├── BottomNav.kt
    └── theme/Theme.kt     paleta e fontes
```

## Ajustes rápidos

- Tempo de revelação: `RollRepository.DEVELOP_MINUTES` e `SHAKE_MINUTES`
- Criar um filme novo: adicione um `Film(...)` em `Films.all` com `look(saturation, contrast, warmth, fade, r, g, b)`
- Fontes: coloque `fraunces.ttf` e `share_tech_mono.ttf` em `res/font` e troque `TituloFont`/`MonoFont` em `Theme.kt`

## Próximas ideias

- LUTs `.cube` para emular filmes com mais fidelidade
- Shader OpenGL para o efeito completo ao vivo (light leak e aberração cromática no visor)
- Efeito VHS (linhas de scan, "REC ●") e câmera de daguerreótipo
- Importar fotos da galeria e aplicar um filme
- Compartilhar foto direto do app
