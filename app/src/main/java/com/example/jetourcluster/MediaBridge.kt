package com.example.jetourcluster

import android.graphics.Bitmap
import java.util.concurrent.CopyOnWriteArraySet

data class MediaInfo(
    val title: String,
    val artist: String,
    val app: String,
    val art: Bitmap?,
    val playing: Boolean,
    val position: Long,       // ms at [updatedAt]
    val duration: Long,       // ms, 0 if unknown
    val updatedAt: Long       // SystemClock.elapsedRealtime()
)

/** Tiny pub/sub between [MediaListenerService] (producer) and [MediaWidget] (consumer). */
object MediaBridge {
    @Volatile var info: MediaInfo? = null
        private set
    private val listeners = CopyOnWriteArraySet<(MediaInfo?) -> Unit>()

    fun subscribe(l: (MediaInfo?) -> Unit) { listeners.add(l); l(info) }
    fun unsubscribe(l: (MediaInfo?) -> Unit) { listeners.remove(l) }
    fun publish(i: MediaInfo?) { info = i; listeners.forEach { it(i) } }
}
