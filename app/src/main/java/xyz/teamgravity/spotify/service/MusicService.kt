package xyz.teamgravity.spotify.service

import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import androidx.media.MediaBrowserServiceCompat
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector
import com.google.android.exoplayer2.ext.mediasession.TimelineQueueNavigator
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import xyz.teamgravity.spotify.FirebaseMusicSource
import xyz.teamgravity.spotify.MusicNotificationManager
import xyz.teamgravity.spotify.helper.callback.MusicPlaybackPreparer
import xyz.teamgravity.spotify.helper.callback.MusicPlayerEventListener
import xyz.teamgravity.spotify.helper.callback.MusicPlayerNotificationListener
import javax.inject.Inject

private const val SERVICE_TAG = "music_service"

private const val MEDIA_ROOT_ID = "media_root_id"

@AndroidEntryPoint
class MusicService : MediaBrowserServiceCompat() {

    @Inject
    lateinit var dataSourceFactory: DefaultDataSourceFactory

    @Inject
    lateinit var exoPlayer: SimpleExoPlayer

    @Inject
    lateinit var firebaseMusicSource: FirebaseMusicSource

    // special coroutine scope for service
    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)

    private lateinit var musicNotificationManager: MusicNotificationManager

    private lateinit var mediaSession: MediaSessionCompat
    private lateinit var mediaSessionConnector: MediaSessionConnector

    private lateinit var musicPlayerEventListener: MusicPlayerEventListener

    var isForegroundService = false
    private var isPlayerInitialized = false

    private var currentPlayingSong: MediaMetadataCompat? = null

    companion object {
        var currentSongDuration = 0L
            private set
    }

    override fun onCreate() {
        super.onCreate()

        serviceScope.launch {
            firebaseMusicSource.fetchMediaData()
        }

        val activityIntent = packageManager?.getLaunchIntentForPackage(packageName)?.let {
            PendingIntent.getActivity(this, 0, it, 0)
        }

        mediaSession = MediaSessionCompat(this, SERVICE_TAG).apply {
            setSessionActivity(activityIntent)
            isActive = true
        }

        sessionToken = mediaSession.sessionToken

        musicNotificationManager = MusicNotificationManager(
            this,
            mediaSession.sessionToken,
            MusicPlayerNotificationListener(this),
        ) {
            // current song switches
            currentSongDuration = exoPlayer.duration
        }


        val musicPlaybackPreparer = MusicPlaybackPreparer(firebaseMusicSource) {
            // every time user chooses new song
            currentPlayingSong = it
            preparePlayer(firebaseMusicSource.songs, it, true)
        }

        mediaSessionConnector = MediaSessionConnector(mediaSession).apply {
            setPlaybackPreparer(musicPlaybackPreparer)
            setQueueNavigator(MusicQueueNavigator())
            setPlayer(exoPlayer)
        }

        musicPlayerEventListener = MusicPlayerEventListener(this)

        exoPlayer.addListener(musicPlayerEventListener)

        musicNotificationManager.showNotification(exoPlayer)
    }

    private fun preparePlayer(songs: List<MediaMetadataCompat>, itemToPlay: MediaMetadataCompat?, playNow: Boolean) {
        val currentSongIndex = if (currentPlayingSong == null) 0 else songs.indexOf(itemToPlay)

        exoPlayer.prepare(firebaseMusicSource.asMediaSource(dataSourceFactory))
        exoPlayer.seekTo(currentSongIndex, 0L)
        exoPlayer.playWhenReady = playNow
    }

    override fun onGetRoot(clientPackageName: String, clientUid: Int, rootHints: Bundle?): BrowserRoot? {
        return BrowserRoot(MEDIA_ROOT_ID, null)
    }

    override fun onLoadChildren(parentId: String, result: Result<MutableList<MediaBrowserCompat.MediaItem>>) {
        when (parentId) {
            MEDIA_ROOT_ID -> {
                val resultsSent = firebaseMusicSource.whenReady { initiliazed ->
                    if (initiliazed) {
                        result.sendResult(firebaseMusicSource.asMediaItems())
                        if (!isPlayerInitialized && firebaseMusicSource.songs.isNotEmpty()) {
                            preparePlayer(firebaseMusicSource.songs, firebaseMusicSource.songs[0], false)
                            isPlayerInitialized = true
                        }
                    } else {
                        result.sendResult(null)
                    }
                }

                if (!resultsSent) {
                    result.detach()
                }
            }
        }
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        exoPlayer.stop()
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()

        exoPlayer.removeListener(musicPlayerEventListener)
        exoPlayer.release()
    }

    private inner class MusicQueueNavigator : TimelineQueueNavigator(mediaSession) {
        override fun getMediaDescription(player: Player, windowIndex: Int): MediaDescriptionCompat {
            return firebaseMusicSource.songs[windowIndex].description
        }
    }
}