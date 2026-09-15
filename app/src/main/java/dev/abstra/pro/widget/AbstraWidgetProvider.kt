package dev.abstra.pro.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.view.View
import android.widget.RemoteViews
import dev.abstra.pro.MainActivity
import dev.abstra.pro.R
import dev.abstra.pro.service.MusicPlaybackService

class AbstraWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (id in appWidgetIds) {
            updateWidget(context, appWidgetManager, id)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == "dev.abstra.pro.WIDGET_UPDATE" || intent.action == AppWidgetManager.ACTION_APPWIDGET_UPDATE) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, AbstraWidgetProvider::class.java)
            val ids = appWidgetManager.getAppWidgetIds(componentName)
            for (id in ids) {
                updateWidget(context, appWidgetManager, id)
            }
        }
    }

    companion object {
        fun updateWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val service = MusicPlaybackService.instance
            val engine = service?.audioEngine
            val song = engine?.currentSong?.value
            val isPlaying = engine?.isPlaying?.value ?: false

            val views = RemoteViews(context.packageName, R.layout.abstra_widget_layout)

            val openIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            }
            val openPendingIntent = PendingIntent.getActivity(
                context, 0, openIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            views.setOnClickPendingIntent(R.id.widget_root, openPendingIntent)

            if (song != null) {
                views.setTextViewText(R.id.widget_title, song.title)
                views.setTextViewText(R.id.widget_artist, song.artist)

                views.setImageViewResource(
                    R.id.widget_play_pause,
                    if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play_arrow
                )

                views.setImageViewResource(R.id.widget_output_icon, getOutputIcon(context))
                views.setTextViewText(R.id.widget_output_text, getOutputName(context))

                val art = loadArtwork(context, song.uriString)
                if (art != null) {
                    views.setImageViewBitmap(R.id.widget_cover, art)
                } else {
                    views.setImageViewResource(R.id.widget_cover, R.drawable.ic_launcher_foreground)
                }
            } else {
                views.setTextViewText(R.id.widget_title, "Abstra Pro")
                views.setTextViewText(R.id.widget_artist, "No track playing")
                views.setImageViewResource(R.id.widget_play_pause, R.drawable.ic_play_arrow)
                views.setImageViewResource(R.id.widget_cover, R.drawable.ic_launcher_foreground)
            }

            views.setOnClickPendingIntent(
                R.id.widget_play_pause,
                getServicePendingIntent(context, if (isPlaying) MusicPlaybackService.ACTION_PAUSE else MusicPlaybackService.ACTION_PLAY)
            )
            views.setOnClickPendingIntent(
                R.id.widget_prev,
                getServicePendingIntent(context, MusicPlaybackService.ACTION_PREVIOUS)
            )
            views.setOnClickPendingIntent(
                R.id.widget_next,
                getServicePendingIntent(context, MusicPlaybackService.ACTION_NEXT)
            )

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        private fun getServicePendingIntent(context: Context, action: String): PendingIntent {
            val intent = Intent(context, MusicPlaybackService::class.java).apply {
                this.action = action
            }
            return PendingIntent.getService(
                context, action.hashCode(), intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
        }

        private fun getOutputIcon(context: Context): Int {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
            for (dev in devices) {
                when (dev.type) {
                    AudioDeviceInfo.TYPE_BLUETOOTH_A2DP, AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> return R.drawable.ic_bluetooth
                    AudioDeviceInfo.TYPE_WIRED_HEADPHONES, AudioDeviceInfo.TYPE_WIRED_HEADSET, AudioDeviceInfo.TYPE_USB_HEADSET -> return R.drawable.ic_headphones
                    else -> {}
                }
            }
            return R.drawable.ic_speaker
        }

        private fun getOutputName(context: Context): String {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
            for (dev in devices) {
                when (dev.type) {
                    AudioDeviceInfo.TYPE_BLUETOOTH_A2DP, AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> return "Bluetooth"
                    AudioDeviceInfo.TYPE_WIRED_HEADPHONES, AudioDeviceInfo.TYPE_WIRED_HEADSET, AudioDeviceInfo.TYPE_USB_HEADSET -> return "Headphones"
                    else -> {}
                }
            }
            return "Speaker"
        }

        private fun loadArtwork(context: Context, uriString: String?): Bitmap? {
            if (uriString.isNullOrBlank() || uriString.startsWith("demo_")) return null
            return try {
                val retriever = MediaMetadataRetriever()
                retriever.setDataSource(context, Uri.parse(uriString))
                val art = retriever.embeddedPicture
                retriever.release()
                if (art != null) {
                    val opts = BitmapFactory.Options().apply { inSampleSize = 2 }
                    BitmapFactory.decodeByteArray(art, 0, art.size, opts)
                } else null
            } catch (_: Exception) {
                null
            }
        }
    }
}
