package com.toffaha.halal

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin

// 🕌 Islamic palette: emerald + gold on warm cream
val Emerald = Color(0xFF0B6B43)
val EmeraldDeep = Color(0xFF053B26)
val Gold = Color(0xFFD4AF37)
val GoldSoft = Color(0xFFE9D9A6)
val Cream = Color(0xFFFBF7EC)

/** kept for older references */
val Green = Emerald

@Composable
fun HalalTheme(content: @Composable () -> Unit) {
    val scheme = if (isSystemInDarkTheme())
        darkColorScheme(
            primary = Color(0xFF7BD8A6), onPrimary = EmeraldDeep,
            secondary = Gold, onSecondary = EmeraldDeep,
            secondaryContainer = Color(0xFF3A3114), onSecondaryContainer = GoldSoft,
            background = Color(0xFF07170F), surface = Color(0xFF0C2218),
            surfaceVariant = Color(0xFF14301F), onSurfaceVariant = Color(0xFFC9D6CC),
        )
    else
        lightColorScheme(
            primary = Emerald, onPrimary = Color.White,
            secondary = Color(0xFFA8841E), onSecondary = Color.White,
            secondaryContainer = GoldSoft, onSecondaryContainer = EmeraldDeep,
            background = Cream, surface = Cream,
            surfaceVariant = Color(0xFFF1EADA), onSurfaceVariant = Color(0xFF4F5B52),
        )
    MaterialTheme(colorScheme = scheme, content = content)
}

/** 8-point star (Rub el Hizb): two squares rotated 45° */
fun DrawScope.star8(center: Offset, r: Float, color: Color, stroke: Float? = null) {
    for (rot in listOf(0.0, Math.PI / 4)) {
        val p = Path()
        for (i in 0 until 4) {
            val a = rot + i * Math.PI / 2 + Math.PI / 4
            val x = center.x + r * cos(a).toFloat()
            val y = center.y + r * sin(a).toFloat()
            if (i == 0) p.moveTo(x, y) else p.lineTo(x, y)
        }
        p.close()
        drawPath(p, color, style = if (stroke != null) Stroke(stroke) else Fill)
    }
}

/** Tiled geometric star pattern — use as a background layer */
@Composable
fun IslamicPattern(modifier: Modifier = Modifier, color: Color = Gold.copy(alpha = 0.18f), cell: Dp = 34.dp) {
    Canvas(modifier) {
        val c = cell.toPx()
        val r = c * 0.36f
        var y = 0f
        var row = 0
        while (y < size.height + c) {
            var x = if (row % 2 == 0) 0f else c / 2
            while (x < size.width + c) {
                star8(Offset(x, y), r, color, stroke = 1.2.dp.toPx())
                star8(Offset(x, y), r * 0.35f, color.copy(alpha = color.alpha * 0.8f))
                x += c
            }
            y += c * 0.75f
            row++
        }
    }
}

/** Small gold star used as divider ornament */
@Composable
fun StarOrnament(size: Dp = 12.dp, color: Color = Gold) {
    Canvas(Modifier.size(size)) { star8(center, this.size.minDimension / 2, color) }
}

/** ── ✦ ── gold divider */
@Composable
fun GoldDivider(modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.weight(1f).height(1.dp).background(Brush.horizontalGradient(listOf(Color.Transparent, Gold))))
        Spacer(Modifier.width(6.dp))
        StarOrnament(10.dp)
        Spacer(Modifier.width(6.dp))
        Box(Modifier.weight(1f).height(1.dp).background(Brush.horizontalGradient(listOf(Gold, Color.Transparent))))
    }
}

/** Header: emerald gradient, gold star pattern, Arabic + Latin title */
@Composable
fun IslamicHeader() {
    Box(
        Modifier.fillMaxWidth()
            .background(Brush.verticalGradient(listOf(EmeraldDeep, Emerald)))
    ) {
        IslamicPattern(Modifier.matchParentSize(), Gold.copy(alpha = 0.16f), 30.dp)
        Column(
            Modifier.fillMaxWidth().statusBarsPadding().padding(top = 10.dp, bottom = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                StarOrnament(14.dp)
                Spacer(Modifier.width(10.dp))
                Text("حلال", color = Gold, fontSize = 26.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Serif)
                Spacer(Modifier.width(10.dp))
                Text("Halal Scanner", color = Color.White, fontSize = 20.sp, fontFamily = FontFamily.Serif, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.width(10.dp))
                StarOrnament(14.dp)
            }
            Text(
                "بسم الله الرحمن الرحيم",
                color = GoldSoft.copy(alpha = 0.9f), fontSize = 12.sp, fontFamily = FontFamily.Serif
            )
        }
        Box(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(2.dp)
                .background(Brush.horizontalGradient(listOf(Color.Transparent, Gold, Color.Transparent)))
        )
    }
}

/** Section title with star: ✦ Title */
@Composable
fun SectionTitle(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        StarOrnament(12.dp, MaterialTheme.colorScheme.secondary)
        Spacer(Modifier.width(8.dp))
        Text(text, fontWeight = FontWeight.Bold, fontSize = 17.sp, fontFamily = FontFamily.Serif, color = MaterialTheme.colorScheme.primary)
    }
}
