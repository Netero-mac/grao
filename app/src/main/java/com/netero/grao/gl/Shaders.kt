package com.netero.grao.gl

/**
 * Um único shader faz tudo: filtro do filme, ajustes e efeitos.
 * A mesma lógica roda no visor da câmera (textura OES), no editor e na foto final (textura 2D).
 */
object Shaders {

    const val VERTEX = """
attribute vec4 aPos;
attribute vec2 aUv;
varying vec2 vUv;
void main() {
    gl_Position = aPos;
    vUv = aUv;
}
"""

    fun fragment(oes: Boolean): String {
        val header = if (oes) {
            "#extension GL_OES_EGL_image_external : require\n"
        } else ""
        val sampler = if (oes) "uniform samplerExternalOES uTex;" else "uniform sampler2D uTex;"
        return header + PRECISION + sampler + "\n" + BODY
    }

    private const val PRECISION = """
#ifdef GL_FRAGMENT_PRECISION_HIGH
precision highp float;
#else
precision mediump float;
#endif
"""

    private const val BODY = """
varying vec2 vUv;
uniform mat4 uTexMatrix;
uniform mat4 uColorMat;
uniform vec4 uColorOff;
uniform float uFilterMix;
uniform float uExposure;
uniform float uContrast;
uniform float uSaturation;
uniform float uTemp;
uniform float uTint;
uniform float uFade;
uniform float uSharpen;
uniform float uVignette;
uniform float uGrain;
uniform float uChroma;
uniform float uHalation;
uniform vec3 uHalationColor;
uniform float uGlow;
uniform float uSparkle;
uniform float uPrism;
uniform float uGlitch;
uniform float uScanlines;
uniform float uLeak;
uniform vec2 uLeakPos;
uniform vec3 uLeakColor;
uniform vec3 uSplitS;
uniform vec3 uSplitH;
uniform float uSplitAmt;
uniform float uSoft;
uniform float uTime;
uniform float uSeed;
uniform vec2 uRes;
uniform float uYDown;
uniform sampler2D uDecal;
uniform float uDecalOn;
uniform vec2 uDepthOffset;
uniform float uGlitter;
uniform float uParty;
uniform float uSideLight;
uniform float uRainbow;
uniform float uDream;
uniform float uPixel;
uniform float uFisheye;
uniform float uMirror;
uniform float uKaleido;
uniform float uComic;
uniform float uHalftone;
uniform float uThermal;
uniform float uDuotone;

vec4 S(vec2 p) {
    return texture2D(uTex, (uTexMatrix * vec4(clamp(p, 0.0, 1.0), 0.0, 1.0)).xy);
}

float hash(vec2 p) {
    return fract(sin(dot(p, vec2(12.9898, 78.233))) * 43758.5453);
}

float lum(vec3 c) {
    return dot(c, vec3(0.299, 0.587, 0.114));
}

vec3 hue(float h) {
    return clamp(abs(mod(h * 6.0 + vec3(0.0, 4.0, 2.0), 6.0) - 3.0) - 1.0, 0.0, 1.0);
}

vec3 screen(vec3 a, vec3 b) {
    return 1.0 - (1.0 - clamp(a, 0.0, 1.0)) * (1.0 - clamp(b, 0.0, 1.0));
}

vec3 grade(vec3 c) {
    vec3 f = (uColorMat * vec4(c, 1.0)).rgb + uColorOff.rgb;
    c = mix(c, clamp(f, 0.0, 1.0), uFilterMix);

    float L0 = lum(clamp(c, 0.0, 1.0));
    c = mix(c, c + (uSplitS - 0.5) * 0.45, (1.0 - L0) * uSplitAmt);
    c = mix(c, c + (uSplitH - 0.5) * 0.45, L0 * uSplitAmt);

    c *= pow(2.0, uExposure);
    c = (c - 0.5) * (1.0 + uContrast) + 0.5;
    float l = lum(c);
    c = mix(vec3(l), c, 1.0 + uSaturation);
    c.r += uTemp * 0.08;
    c.b -= uTemp * 0.08;
    c.g += uTint * 0.06;
    c = c * (1.0 - 0.22 * uFade) + 0.22 * uFade;
    return c;
}

void main() {
    vec2 uv = vUv;
    float aspect = uRes.x / uRes.y;
    float imgY = mix(1.0 - uv.y, uv.y, uYDown);

    // Lentes: olho de peixe, espelho, caleidoscópio, pixel
    if (uFisheye > 0.0) {
        vec2 p = (uv - 0.5) * vec2(aspect, 1.0);
        float r = length(p);
        float k = 1.0 + uFisheye * 0.9;
        p *= (1.0 - uFisheye * 0.35) * (1.0 + uFisheye * 0.6 * r * r) / k * 1.25;
        uv = p / vec2(aspect, 1.0) + 0.5;
    }
    if (uMirror > 0.5) {
        uv.x = uv.x < 0.5 ? uv.x : 1.0 - uv.x;
    }
    if (uKaleido > 0.0) {
        vec2 p = (uv - 0.5) * vec2(aspect, 1.0);
        float seg = floor(4.0 + uKaleido * 6.0);
        float ang = atan(p.y, p.x);
        float r = length(p);
        float s = 6.28318 / seg;
        ang = mod(ang, s);
        ang = abs(ang - s * 0.5);
        p = vec2(cos(ang), sin(ang)) * r;
        uv = p / vec2(aspect, 1.0) + 0.5;
    }
    if (uPixel > 0.0) {
        float px = 4.0 + uPixel * 40.0;
        vec2 cells = uRes / px;
        uv = (floor(uv * cells) + 0.5) / cells;
    }

    // Glitch: faixas horizontais deslocadas
    if (uGlitch > 0.0) {
        float band = floor(imgY * 28.0);
        float tick = floor(uTime * 6.0);
        float n = hash(vec2(band, tick + uSeed));
        float on = step(1.0 - 0.4 * uGlitch, n);
        uv.x += on * (hash(vec2(band + 7.0, tick + uSeed)) - 0.5) * 0.14 * uGlitch;
    }

    // Movimento 3D: profundidade estimada (centro e parte de baixo mais perto)
    if (uDepthOffset.x != 0.0 || uDepthOffset.y != 0.0) {
        vec2 c = (uv - 0.5) * vec2(aspect, 1.0);
        float d = 0.55 * imgY + 0.45 * smoothstep(0.85, 0.0, length(c));
        uv += uDepthOffset * (d - 0.5);
    }

    vec2 t = 1.0 / uRes;
    vec3 col;
    vec2 off = vec2(uChroma * 0.004, 0.0) + (uv - 0.5) * uPrism * 0.03;
    if (uPrism > 0.0) {
        // 5 amostras coloridas: vermelho, amarelo, verde, ciano, azul
        vec3 p = S(uv + off).rgb * vec3(1.0, 0.0, 0.0)
               + S(uv + off * 0.5).rgb * vec3(0.5, 0.5, 0.0)
               + S(uv).rgb * vec3(0.0, 1.0, 0.0)
               + S(uv - off * 0.5).rgb * vec3(0.0, 0.5, 0.5)
               + S(uv - off).rgb * vec3(0.0, 0.0, 1.0);
        col = p / vec3(1.5, 2.0, 1.5);
    } else if (uChroma > 0.0) {
        col.r = S(uv + off).r;
        col.g = S(uv).g;
        col.b = S(uv - off).b;
    } else {
        col = S(uv).rgb;
    }

    // Imagem macia (resolução de fita)
    if (uSoft > 0.0) {
        vec3 b = col;
        for (int i = 1; i <= 3; i++) {
            float o = float(i) * 1.6;
            b += S(uv + vec2(t.x * o, 0.0)).rgb + S(uv - vec2(t.x * o, 0.0)).rgb;
        }
        col = mix(col, b / 7.0, uSoft);
    }

    if (uSharpen > 0.0) {
        vec3 b = (S(uv + vec2(t.x, 0.0)).rgb + S(uv - vec2(t.x, 0.0)).rgb
                + S(uv + vec2(0.0, t.y)).rgb + S(uv - vec2(0.0, t.y)).rgb) * 0.25;
        col += (col - b) * uSharpen * 1.5;
    }

    // Sonho: desfoque suave misturado
    if (uDream > 0.0) {
        vec3 b = vec3(0.0);
        for (int i = 0; i < 8; i++) {
            float a = float(i) * 0.7854;
            b += S(uv + vec2(cos(a), sin(a)) * vec2(1.0 / aspect, 1.0) * 0.008).rgb;
        }
        b /= 8.0;
        col = mix(col, screen(col, b * 0.6), uDream * 0.8);
    }

    col = grade(col);

    // Duotone: duas cores, uma nas sombras e outra nas luzes
    if (uDuotone > 0.0) {
        col = mix(col, mix(uSplitS, uSplitH, smoothstep(0.05, 0.95, lum(col))), uDuotone);
    }

    // Térmica: mapa de calor pela luminância
    if (uThermal > 0.0) {
        float l = lum(clamp(col, 0.0, 1.0));
        vec3 th = l < 0.25 ? mix(vec3(0.05, 0.0, 0.3), vec3(0.45, 0.0, 0.7), l * 4.0)
                : l < 0.5 ? mix(vec3(0.45, 0.0, 0.7), vec3(0.95, 0.1, 0.2), (l - 0.25) * 4.0)
                : l < 0.75 ? mix(vec3(0.95, 0.1, 0.2), vec3(1.0, 0.75, 0.0), (l - 0.5) * 4.0)
                : mix(vec3(1.0, 0.75, 0.0), vec3(1.0, 1.0, 0.9), (l - 0.75) * 4.0);
        col = mix(col, th, uThermal);
    }

    // Quadrinho: cores chapadas + contorno
    if (uComic > 0.0) {
        float levels = 6.0 - uComic * 2.0;
        vec3 poster = floor(clamp(col, 0.0, 1.0) * levels + 0.5) / levels;
        vec2 e = t * 1.5;
        float lx = lum(S(uv + vec2(e.x, 0.0)).rgb) - lum(S(uv - vec2(e.x, 0.0)).rgb);
        float ly = lum(S(uv + vec2(0.0, e.y)).rgb) - lum(S(uv - vec2(0.0, e.y)).rgb);
        float edge = smoothstep(0.08, 0.25, length(vec2(lx, ly)));
        poster = mix(poster, poster * 1.2, 0.3);
        col = mix(col, poster * (1.0 - edge * 0.9), uComic);
    }

    // Retícula: pontos de impressão
    if (uHalftone > 0.0) {
        float cell = max(5.0, uRes.y / 140.0);
        vec2 pp = uv * uRes;
        mat2 rot = mat2(0.7071, -0.7071, 0.7071, 0.7071);
        vec2 q = rot * pp / cell;
        vec2 f = fract(q) - 0.5;
        float l = lum(clamp(col, 0.0, 1.0));
        float r = sqrt(1.0 - l) * 0.62;
        float dotm = 1.0 - smoothstep(r - 0.06, r + 0.06, length(f));
        vec3 paper = vec3(0.97, 0.95, 0.9);
        vec3 ink = col * 0.55;
        col = mix(col, mix(paper, ink, dotm), uHalftone);
    }

    // Brilho e halação: luzes fortes "vazam" em volta
    float glowAmt = uGlow + uHalation;
    if (glowAmt > 0.0) {
        vec3 acc = vec3(0.0);
        for (int i = 0; i < 12; i++) {
            float a = float(i) * 0.5236;
            vec2 dir = vec2(cos(a), sin(a)) * vec2(1.0 / aspect, 1.0);
            acc += max(S(uv + dir * 0.010).rgb - 0.68, 0.0);
            acc += max(S(uv + dir * 0.022).rgb - 0.68, 0.0) * 0.7;
        }
        acc /= 20.0;
        float a = lum(acc) * 3.2;
        col += acc * 2.6 * uGlow;
        col += uHalationColor * a * uHalation * 1.4;
    }

    // Diamante: estrelinhas a partir dos pontos mais claros
    if (uSparkle > 0.0) {
        float s = 0.0;
        for (int i = 1; i <= 7; i++) {
            float r = float(i) * 0.006;
            float fall = 1.0 - float(i) / 8.0;
            vec2 h = vec2(r / aspect, 0.0);
            vec2 v = vec2(0.0, r);
            vec2 d1 = vec2(r / aspect, r) * 0.7;
            vec2 d2 = vec2(r / aspect, -r) * 0.7;
            float b = 0.0;
            b += max(lum(S(uv + h).rgb) - 0.88, 0.0) + max(lum(S(uv - h).rgb) - 0.88, 0.0);
            b += max(lum(S(uv + v).rgb) - 0.88, 0.0) + max(lum(S(uv - v).rgb) - 0.88, 0.0);
            b += (max(lum(S(uv + d1).rgb) - 0.9, 0.0) + max(lum(S(uv - d1).rgb) - 0.9, 0.0)) * 0.6;
            b += (max(lum(S(uv + d2).rgb) - 0.9, 0.0) + max(lum(S(uv - d2).rgb) - 0.9, 0.0)) * 0.6;
            s += b * fall * fall;
        }
        vec2 cell = floor(uv * uRes / 6.0);
        float tw = step(0.997, hash(cell + uSeed)) * step(0.55, lum(col));
        col += vec3(1.0, 0.97, 0.92) * (s * 2.2 + tw * 0.8) * uSparkle;
    }

    // Glitter: purpurina piscando
    if (uGlitter > 0.0) {
        vec2 cell = floor(uv * uRes / 3.0);
        float h = hash(cell + uSeed * 1.3);
        float blink = 0.5 + 0.5 * sin(uTime * 9.0 + h * 50.0);
        float on = step(0.985 - uGlitter * 0.02, h) * smoothstep(0.3, 0.7, lum(col));
        col += mix(vec3(1.0), hue(h * 7.0), 0.35) * on * blink * (0.9 + uGlitter);
    }

    // Festa: luzes coloridas flutuando
    if (uParty > 0.0) {
        vec3 lights = vec3(0.0);
        for (int i = 0; i < 6; i++) {
            float fi = float(i);
            vec2 c = vec2(
                0.5 + 0.45 * sin(uTime * (0.35 + fi * 0.07) + fi * 2.1 + uSeed),
                0.5 + 0.45 * cos(uTime * (0.3 + fi * 0.05) + fi * 1.7)
            );
            vec2 d = (uv - c) * vec2(aspect, 1.0);
            float g = smoothstep(0.38, 0.0, length(d));
            lights += hue(fract(fi / 6.0 + uTime * 0.05)) * g * g;
        }
        col = screen(col, lights * uParty * 0.85);
    }

    // Luz lateral: rosa e laranja entrando pela esquerda
    if (uSideLight > 0.0) {
        float g = pow(clamp(1.0 - uv.x * 1.3, 0.0, 1.0), 1.6);
        vec3 lc = mix(vec3(1.0, 0.25, 0.55), vec3(1.0, 0.6, 0.2), imgY);
        col = screen(col, lc * g * uSideLight * 0.9);
    }

    // Arco-íris diagonal
    if (uRainbow > 0.0) {
        float d = (uv.x + imgY) * 0.5;
        float band = smoothstep(0.35, 0.5, d) * smoothstep(0.75, 0.5, d);
        col = screen(col, hue(fract(d * 2.2)) * band * uRainbow * 0.6);
    }

    // Light leak (mistura "screen")
    if (uLeak > 0.0) {
        vec2 p = (vec2(uv.x, imgY) - uLeakPos) * vec2(aspect, 1.0);
        float g = smoothstep(1.1, 0.0, length(p));
        col = 1.0 - (1.0 - clamp(col, 0.0, 1.0)) * (1.0 - uLeakColor * g * uLeak);
    }

    // Vinheta
    if (uVignette > 0.0) {
        vec2 vc = (uv - 0.5) * vec2(1.0, 1.0);
        float v = smoothstep(0.85, 0.3, length(vc) * 1.15);
        col *= mix(1.0, v, clamp(uVignette, 0.0, 1.0));
    }

    // Grão (mais forte nos meios-tons)
    if (uGrain > 0.0) {
        float gs = max(1.0, uRes.y / 1400.0);
        float n = hash(floor(uv * uRes / gs) + fract(uTime * 0.37) * 91.0 + uSeed) - 0.5;
        float l = lum(col);
        col += n * uGrain * 0.24 * (1.0 - abs(l - 0.5) * 1.2);
    }

    // Linhas de scan do VHS
    if (uScanlines > 0.0) {
        float step3 = max(3.0, uRes.y / 360.0);
        float line = step(0.5, fract(imgY * uRes.y / step3));
        col *= 1.0 - line * 0.2 * uScanlines;
    }

    col = clamp(col, 0.0, 1.0);

    // Decalque: poeira, data, texto
    if (uDecalOn > 0.5) {
        vec4 d = texture2D(uDecal, vec2(vUv.x, mix(1.0 - vUv.y, vUv.y, uYDown)));
        col = mix(col, d.rgb / max(d.a, 0.001), d.a);
    }

    gl_FragColor = vec4(col, 1.0);
}
"""
}
