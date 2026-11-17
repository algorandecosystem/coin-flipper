package io.github.algorandecosystem.example.coinflipper

import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import org.koin.mp.KoinPlatform

internal actual fun openUrl(url: String?) {
    val uri = url?.toUri() ?: return
    val context: Context = KoinPlatform.getKoin().get()
    context.startActivity(Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}
