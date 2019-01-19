package euphoria.psycho.library

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.IBinder
import android.support.annotation.RequiresApi


class ClipboardService : Service() {

    private var mClipboardManager: ClipboardManager? = null
    private var mWord: String? = null
    private val mListener = object : ClipboardManager.OnPrimaryClipChangedListener {
        override fun onPrimaryClipChanged() {

            mClipboardManager?.let {
                if (it.primaryClip.itemCount > 0) {
                    it.primaryClip.getItemAt(0).text?.let {
                        val word = it.toString()
                        if (!word.equals(mWord)) {
                            val f = FloatingView(applicationContext)
                            DictionaryTask { f.setText(it) }.execute(word)
                            mWord = word
                        }
                    }
                }
            }
        }

    }

    override fun onBind(p0: Intent?): IBinder? = null
    override fun onCreate() {
        super.onCreate()
        mClipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel("Default","Default")
            startForeground(ID_FOREGROUND, Notification.Builder(this, "Default").build())
        }else{
            startForeground(ID_FOREGROUND, Notification())
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        mClipboardManager?.addPrimaryClipChangedListener(mListener)

        // Don't let this service restart automatically if it has been stopped by the OS.
        return START_NOT_STICKY;
    }
    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel(channelId: String, channelName: String): String{
        val chan = NotificationChannel(channelId,
                channelName, NotificationManager.IMPORTANCE_NONE)
        chan.lightColor = Color.BLUE
        chan.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
        val service = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        service.createNotificationChannel(chan)
        return channelId
    }
    override fun onDestroy() {
        super.onDestroy()
        mClipboardManager?.removePrimaryClipChangedListener(mListener)
    }

    companion object {
        private const val ID_FOREGROUND = 0x1
    }
}