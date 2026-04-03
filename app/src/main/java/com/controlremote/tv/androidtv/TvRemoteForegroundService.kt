package com.controlremote.tv.androidtv

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.controlremote.tv.MainActivity
import com.controlremote.tv.R
import kotlin.concurrent.thread

/**
 * Mantiene la sesión TLS de Android TV / Google TV en primer plano para que el socket
 * siga activo aunque el usuario cierre la app (con notificación persistente).
 */
class TvRemoteForegroundService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_DISCONNECT -> {
                disconnectAndStop()
                return START_NOT_STICKY
            }
            ACTION_CONNECT -> {
                val host = intent.getStringExtra(EXTRA_HOST)?.trim().orEmpty()
                startForegroundWithNotification(buildConnectingNotification(host.ifEmpty { "…" }))
                if (host.isEmpty()) {
                    RemoteSessionHolder.setError(getString(R.string.notification_tv_remote_error_no_host))
                    stopForegroundCompat()
                    stopSelf()
                    return START_NOT_STICKY
                }
                val creds = TvCredentialsStore(applicationContext)
                if (!creds.hasCredentials(host)) {
                    RemoteSessionHolder.setError(getString(R.string.notification_tv_remote_error_no_credentials))
                    stopForegroundCompat()
                    stopSelf()
                    return START_NOT_STICKY
                }
                thread(name = "atv-remote-fgs-connect") {
                    try {
                        RemoteSessionHolder.clear()
                        val ssl = AndroidTvSsl.createClientContext(
                            creds.getCertPem(host)!!,
                            creds.getKeyPem(host)!!
                        )
                        val s = RemoteSession(
                            host = host,
                            sslContext = ssl,
                            onTvDeviceLabel = { label ->
                                Handler(Looper.getMainLooper()).post {
                                    creds.setDisplayName(host, label)
                                    RemoteSessionHolder.notifyCredentialsRefresh()
                                }
                            }
                        )
                        s.connect()
                        RemoteSessionHolder.setConnected(s, host)
                        Handler(Looper.getMainLooper()).post {
                            startForegroundWithNotification(buildConnectedNotification(host))
                        }
                    } catch (e: Exception) {
                        RemoteSessionHolder.setError(e.message ?: getString(R.string.notification_tv_remote_error_generic))
                        Handler(Looper.getMainLooper()).post {
                            stopForegroundCompat()
                            stopSelf()
                        }
                    }
                }
            }
        }
        return START_NOT_STICKY
    }

    private fun startForegroundWithNotification(notification: Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceCompat.startForeground(
                this,
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun stopForegroundCompat() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_DETACH)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(false)
        }
    }

    private fun disconnectAndStop() {
        RemoteSessionHolder.clear()
        stopForegroundCompat()
        stopSelf()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.notification_channel_tv_remote_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(R.string.notification_channel_tv_remote_desc)
        }
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(channel)
    }

    private fun buildConnectingNotification(host: String): Notification =
        baseNotificationBuilder(host)
            .setContentText(getString(R.string.notification_tv_remote_connecting, host))
            .build()

    private fun buildConnectedNotification(host: String): Notification =
        baseNotificationBuilder(host)
            .setContentText(getString(R.string.notification_tv_remote_connected, host))
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                getString(R.string.notification_tv_remote_disconnect),
                disconnectPendingIntent()
            )
            .build()

    private fun baseNotificationBuilder(host: String): NotificationCompat.Builder =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher)
            .setContentTitle(getString(R.string.notification_tv_remote_title))
            .setContentText(getString(R.string.notification_tv_remote_connected, host))
            .setOngoing(true)
            .setContentIntent(openAppPendingIntent())
            .setPriority(NotificationCompat.PRIORITY_LOW)

    private fun openAppPendingIntent(): PendingIntent =
        PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    private fun disconnectPendingIntent(): PendingIntent =
        PendingIntent.getService(
            this,
            1,
            Intent(this, TvRemoteForegroundService::class.java).apply { action = ACTION_DISCONNECT },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    companion object {
        const val ACTION_CONNECT = "com.controlremote.tv.action.TV_REMOTE_CONNECT"
        const val ACTION_DISCONNECT = "com.controlremote.tv.action.TV_REMOTE_DISCONNECT"
        const val EXTRA_HOST = "host"

        private const val CHANNEL_ID = "tv_remote_connection"
        private const val NOTIFICATION_ID = 6466

        fun connect(context: Context, host: String) {
            val h = host.trim()
            if (h.isEmpty()) return
            val intent = Intent(context, TvRemoteForegroundService::class.java).apply {
                action = ACTION_CONNECT
                putExtra(EXTRA_HOST, h)
            }
            ContextCompat.startForegroundService(context, intent)
        }

        fun disconnect(context: Context) {
            val intent = Intent(context, TvRemoteForegroundService::class.java).apply {
                action = ACTION_DISCONNECT
            }
            context.startService(intent)
        }
    }
}
