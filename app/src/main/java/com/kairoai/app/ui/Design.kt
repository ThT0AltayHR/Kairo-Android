package com.kairoai.app.ui

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.kairoai.app.R
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.request.ImageRequest

val Ink = Color(0xFF08090D)
val Panel = Color(0xFF11131A)
val Panel2 = Color(0xFF171A23)
val Violet = Color(0xFFB39BFF)
val Mint = Color(0xFF85E6C2)
val Coral = Color(0xFFFF987F)
val Muted = Color(0xFF9699A8)
val KairoSans = FontFamily(Font(R.font.kairo_sans))

@Composable
fun kairoColorScheme() = androidx.compose.material3.darkColorScheme(
    primary = Violet,
    secondary = Mint,
    tertiary = Coral,
    background = Ink,
    surface = Panel,
    surfaceVariant = Panel2,
    onPrimary = Ink,
    onBackground = Color(0xFFF3F3F8),
    onSurface = Color(0xFFF3F3F8),
    onSurfaceVariant = Muted
)

@Composable
fun Mascot(modifier: Modifier = Modifier, mood: String = "idle") {
    val context = LocalContext.current
    val imageLoader = remember(context) {
        ImageLoader.Builder(context).components {
            if (Build.VERSION.SDK_INT >= 28) add(ImageDecoderDecoder.Factory()) else add(GifDecoder.Factory())
        }.build()
    }
    val safeMood = mood.takeIf { it in setOf("idle", "thinking", "done", "error", "question") } ?: "idle"
    Box(modifier.clip(RoundedCornerShape(28.dp)).background(Brush.radialGradient(listOf(Color(0x223E3B66), Color.Transparent)))) {
        AsyncImage(
            model = ImageRequest.Builder(context).data("file:///android_asset/mascot_${safeMood}.gif").crossfade(true).build(),
            imageLoader = imageLoader,
            contentDescription = "Kairo'nun ${safeMood} animasyonu",
            modifier = Modifier.fillMaxSize()
        )
    }
}
