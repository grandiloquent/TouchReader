package euphoria.psycho.library

import android.app.IntentService
import android.app.Notification
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.IBinder


class ClipboardService : IntentService("euphoria.psycho.library.ClipboardService") {
    override fun onHandleIntent(p0: Intent?) {

    }

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
        startForeground(ID_FOREGROUND, Notification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        mClipboardManager?.addPrimaryClipChangedListener(mListener)

        // Don't let this service restart automatically if it has been stopped by the OS.
        return START_NOT_STICKY;
    }

    override fun onDestroy() {
        super.onDestroy()
        mClipboardManager?.removePrimaryClipChangedListener(mListener)
    }

    companion object {
        private const val ID_FOREGROUND = 0x1
    }
}