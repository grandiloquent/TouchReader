package euphoria.psycho.library

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.text.TextUtils
import android.view.Menu
import android.view.MenuItem
import android.widget.TextView
import java.io.File
import java.lang.reflect.Field
import java.util.*

class FileActivity : AppCompatActivity(), FileClickListener {
    var mToolbar: Toolbar? = null
    var mStartPath: String? = Environment.getExternalStorageDirectory().absolutePath
    var mCurrentPath: String? = mStartPath
    var mTitle: String? = null
    override fun onClick(f: File?) {
        Handler().postDelayed({
            run {
                f?.let {
                    if (it.isDirectory) {
                        mCurrentPath = it.absolutePath
                        addFragmentToBackStack(mCurrentPath!!)
                        updateTitle()
                    }
                }
            }
        }, 150L)
    }

    fun updateTitle() {
        supportActionBar?.let {
            var titlePath: String? = null
            if (mCurrentPath.isNullOrEmpty()) {
                titlePath = "/"
            } else {
                titlePath = mCurrentPath
            }
            if (mTitle.isNullOrEmpty()) {
                supportActionBar?.title = titlePath
            } else {
                supportActionBar?.subtitle = titlePath
            }
        }
    }

    fun checkPermission(permission: String, requestPermissionCode: Int, onFailure: () -> Unit, onSuccess: () -> Unit) {
        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, permission)) {
                onFailure()
            } else {
                ActivityCompat.requestPermissions(this, arrayOf(permission), requestPermissionCode);
            }
        } else onSuccess()
    }

    private fun initialize() {
        setContentView(R.layout.file_activity)
        mToolbar = findViewById(R.id.toolbar)
        setSupportActionBar(mToolbar)
        supportActionBar?.let { it.setDisplayHomeAsUpEnabled(true) }
        try {
            var field: Field? = null
            if (mTitle.isNullOrEmpty())
                field = mToolbar?.javaClass?.getDeclaredField("mTitleTextView")
            else field = mToolbar?.javaClass?.getDeclaredField("mSubtitleTextView")
            field?.isAccessible = true
            val textView = field?.get(mToolbar) as TextView
            textView.ellipsize = TextUtils.TruncateAt.START
        } catch (ex: Exception) {
        }
        if (!mTitle.isNullOrEmpty()) setTitle(mTitle)
        updateTitle()
        initBackStackState()
        supportFragmentManager.beginTransaction()
                .replace(R.id.container, DirectoryFragment.getInstance(
                        mCurrentPath!!
                )).addToBackStack(null)
                .commit()
    }

    fun initBackStackState() {
        var pathToAdd = mCurrentPath
        val separatedPaths = ArrayList<String>()
        while (pathToAdd != mStartPath) {
            pathToAdd = FileUtils.cutLastSegmentOfPath(pathToAdd)
            separatedPaths.add(pathToAdd)
        }
        Collections.reverse(separatedPaths)
        for (path in separatedPaths) {
            addFragmentToBackStack(path);
        }
    }

    fun addFragmentToBackStack(path: String) {
        supportFragmentManager.beginTransaction()
                .replace(R.id.container, DirectoryFragment.getInstance(
                        path
                )).addToBackStack(null)
                .commit()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu, menu)
        menu?.findItem(R.id.action_close)?.setVisible(true)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            android.R.id.home -> onBackPressed()
            R.id.action_close -> finish()
            R.id.action_combine_safari -> {
                File(mCurrentPath).combineSafariBookDirectory()
                
            }
        }
        return true
    }

    override fun onPause() {
        super.onPause()
        preferences.edit().putString(STATE_START_PATH, mStartPath)
                .putString(STATE_CURRENT_PATH, mCurrentPath).commit()
    }

    override fun onBackPressed() {
        if (mCurrentPath != mStartPath) {
            supportFragmentManager.popBackStack()
            mCurrentPath = FileUtils.cutLastSegmentOfPath(mCurrentPath)
            updateTitle()
        } else {
            finish()
        }
    }

    //    override fun onSaveInstanceState(outState: Bundle?) {
//        super.onSaveInstanceState(outState)
//        Log.e(TAG, "[onSaveInstanceState]");
//        outState?.putString(STATE_CURRENT_PATH, mCurrentPath)
//        outState?.putString(STATE_START_PATH, mStartPath)
//    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        savedInstanceState?.let {
//            mStartPath = it.getString(STATE_START_PATH)
//            mCurrentPath = it.getString(STATE_CURRENT_PATH)
//        }
        val startPath = preferences.getString(STATE_START_PATH, null);
        if (startPath != null) mStartPath = startPath
        val currentPath = preferences.getString(STATE_CURRENT_PATH, null);
        if (currentPath != null) mCurrentPath = currentPath
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
            ), REQUEST_PERMISSIONS_CODE);
        } else initialize()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        initialize()
    }

    companion object {
        private const val REQUEST_PERMISSIONS_CODE = 1;
        private const val STATE_CURRENT_PATH = "state_current_path"
        private const val STATE_START_PATH = "state_start_path"
        private const val TAG = "FileActivity"
    }
}