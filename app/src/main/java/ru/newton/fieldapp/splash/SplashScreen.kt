package ru.newton.fieldapp.splash

import android.media.MediaPlayer
import android.net.Uri
import android.widget.VideoView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import ru.newton.fieldapp.R

/**
 * Plays the brand intro animation (`res/raw/main_animation.mp4`) once on app
 * launch, then calls [onFinished] so the host can swap to the main UI.
 *
 * UX details:
 *  - Background is solid white to match the brand intro reel — the video
 *    content is composed for a white frame, black would show as a hard edge.
 *  - Tap anywhere skips — surveyors who've seen it dozens of times shouldn't
 *    have to wait through it every cold start.
 *  - We use the built-in [VideoView] instead of ExoPlayer to avoid pulling in
 *    a Media3 dependency just for a one-shot intro. Sufficient for an MP4
 *    bundled in `res/raw`.
 */
@Composable
fun SplashScreen(onFinished: () -> Unit) {
    val context = LocalContext.current
    val videoUri = remember {
        Uri.parse("android.resource://${context.packageName}/${R.raw.main_animation}")
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .clickable(onClick = onFinished),
        contentAlignment = Alignment.Center,
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                VideoView(ctx).apply {
                    setVideoURI(videoUri)
                    setOnPreparedListener { mp ->
                        // Looping off; the splash plays exactly once.
                        mp.isLooping = false
                        // Some devices add a default audio focus claim — we
                        // intentionally don't change volume here so a silent
                        // device stays silent.
                        start()
                    }
                    setOnCompletionListener { onFinished() }
                    setOnErrorListener { _: MediaPlayer?, _: Int, _: Int ->
                        // If playback can't start (missing codec, broken file),
                        // don't strand the user on a black screen.
                        onFinished()
                        true
                    }
                }
            },
        )
        Text(
            text = "Пропустить",
            color = Color.Black.copy(alpha = 0.5f),
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp),
        )
    }
}
