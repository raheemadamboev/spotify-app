package xyz.teamgravity.spotify.helper.callback

import android.app.Notification
import android.content.Intent
import androidx.core.content.ContextCompat
import com.google.android.exoplayer2.ui.PlayerNotificationManager
import xyz.teamgravity.spotify.helper.util.MusicNotificationManager
import xyz.teamgravity.spotify.service.MusicService

class MusicPlayerNotificationListener(
    private val musicService: MusicService
) : PlayerNotificationManager.NotificationListener {


    override fun onNotificationCancelled(notificationId: Int, dismissedByUser: Boolean) {
        super.onNotificationCancelled(notificationId, dismissedByUser)
        musicService.apply {
            stopForeground(true)
            isForegroundService = false
            stopSelf()
        }
    }

    override fun onNotificationPosted(notificationId: Int, notification: Notification, ongoing: Boolean) {
        super.onNotificationPosted(notificationId, notification, ongoing)
        musicService.apply {
            if (ongoing && !isForegroundService) {
                ContextCompat.startForegroundService(this, Intent(applicationContext, this::class.java))
            }
            startForeground(MusicNotificationManager.NOTIFICATION_ID, notification)
            isForegroundService = true
        }
    }
}