package com.example.jetourcluster

import android.content.ComponentName
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.SystemClock
import android.service.notification.NotificationListenerService

/**
 * Needs "notification access" (see README). Having it lets us read the active media session
 * without any privileged permission.
 */
class MediaListenerService : NotificationListenerService() {

    private var msm: MediaSessionManager? = null
    private var controller: MediaController? = null

    private val cb = object : MediaController.Callback() {
        override fun onMetadataChanged(metadata: MediaMetadata?) = push()
        override fun onPlaybackStateChanged(state: PlaybackState?) = push()
        override fun onSessionDestroyed() = pick(msm?.getActiveSessions(component()))
    }

    private val sessionsListener = MediaSessionManager.OnActiveSessionsChangedListener { pick(it) }

    private fun component() = ComponentName(this, MediaListenerService::class.java)

    override fun onListenerConnected() {
        val m = getSystemService(MediaSessionManager::class.java)
        msm = m
        try {
            m.addOnActiveSessionsChangedListener(sessionsListener, component())
            pick(m.getActiveSessions(component()))
        } catch (_: SecurityException) { }
    }

    override fun onListenerDisconnected() {
        try { msm?.removeOnActiveSessionsChangedListener(sessionsListener) } catch (_: Exception) { }
        controller?.unregisterCallback(cb)
        controller = null
        MediaBridge.publish(null)
    }

    private fun pick(list: List<MediaController>?) {
        controller?.unregisterCallback(cb)
        controller = list?.firstOrNull { it.playbackState?.state == PlaybackState.STATE_PLAYING }
            ?: list?.firstOrNull()
        controller?.registerCallback(cb)
        push()
    }

    private fun push() {
        val c = controller
        val md = c?.metadata
        if (c == null || md == null) { MediaBridge.publish(null); return }
        val st = c.playbackState
        val app = try {
            packageManager.getApplicationLabel(packageManager.getApplicationInfo(c.packageName, 0)).toString()
        } catch (_: Exception) { c.packageName }
        MediaBridge.publish(
            MediaInfo(
                title = md.getString(MediaMetadata.METADATA_KEY_TITLE) ?: "Unknown",
                artist = md.getString(MediaMetadata.METADATA_KEY_ARTIST)
                    ?: md.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST) ?: "",
                app = app,
                art = md.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART) ?: md.getBitmap(MediaMetadata.METADATA_KEY_ART),
                playing = st?.state == PlaybackState.STATE_PLAYING,
                position = st?.position ?: 0L,
                duration = md.getLong(MediaMetadata.METADATA_KEY_DURATION),
                updatedAt = SystemClock.elapsedRealtime()
            )
        )
    }
}
