package euphoria.psycho.library

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.preference.PreferenceManager
import android.view.View
import android.widget.ScrollView
import android.widget.TextView
import java.io.File

class MainActivity : Activity(), ReaderView.SelectListener, TaskListener {


    var mReaderView: ReaderView? = null
    var mCount = 0
    var mTag: String? = null
    var mScrollView: ScrollView? = null
    var mSearchList: List<Int>? = null
    var mSearchCount = -1
    var mDicTextView: TextView? = null

    private fun initialize() {
        // Initialize the database
        DataProvider.getInstance(this);

        setContentView(R.layout.main_activity)

        mDicTextView = findViewById(R.id.dicTextView)
        mScrollView = findViewById(R.id.scrollView)

        mReaderView = findViewById(R.id.readerView)
        mReaderView?.setSelectListener(this)

        initializeToolbar()

        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)

        applySettings(sharedPreferences)
        loadLasted(null, sharedPreferences)
    }

    private fun initializeToolbar() {
        findViewById<View>(R.id.back).setOnClickListener { _ -> }
        findViewById<View>(R.id.showBooklist).setOnClickListener { _ ->
            val intent = Intent(App.instance, BookListActivity::class.java)
            startActivityForResult(intent, REQUEST_BOOK_CODE)
        }
    }


    private fun back() {
        if (mTag == null) return

    }

    private fun forward() {
        if (mTag == null) return
        ++mCount
        renderText(mTag, mCount, 0)
    }

    private fun renderText(tag: String?, count: Int, scrollY: Int) {
        val v = DataProvider.getInstance().queryContent(tag, count)
        mReaderView?.text = v;
        if (scrollY > -1)
            mScrollView?.post { mScrollView?.scrollTo(0, scrollY) }
    }

    fun loadLasted(tag: String?, sharedPreferences: SharedPreferences) {

        val t = tag ?: sharedPreferences.getString(KEY_TAG, null) ?: return
        mTag = t;

        val lastedPosition = DataProvider.getInstance().querySettings(t)
        var count = 1;
        var y = 0
        if (lastedPosition.size > 1) {
            count = lastedPosition[0]
            y = lastedPosition[1]
        }
        mCount = count
        renderText(t, count, y)
    }

    fun applySettings(sharedPreferences: SharedPreferences) {

        // Setting typeface
        val typeface = sharedPreferences.getString(KEY_TYPEFACE, null)
        if (typeface != null) {
            val tf = File(typeface)
            if (tf.isFile)
                mReaderView?.setTypeface(Typeface.createFromFile(tf))
        }

    }

    fun queryLastedPosition(tag: String): Pair<Int, Int> {

        var r = DataProvider.getInstance().querySettings(tag)
        return Pair(r[0], r[1])
    }

    fun resetSearch() {
        if (mSearchList != null) {
            mSearchList = null
            mCount = -1
        }
    }

    override fun onPostExecute(result: String?) {
        mDicTextView?.setText(result)
    }

    override fun onPause() {
        if (mTag != null) {
            PreferenceManager.getDefaultSharedPreferences(this).edit().putString(KEY_TAG, mTag).commit()
            val y = mScrollView?.scrollY ?: 0
            DataProvider.getInstance().updateSettings(mTag, mCount, y)
        }
        super.onPause()
    }

    override fun onSelectionChange(value: String?) {
        if (!value.isNullOrEmpty()) {
            DictionaryTask(this).execute(value)
        }
    }

    override fun onClick() {
        mDicTextView?.setText(null)
    }


    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>?, grantResults: IntArray?) {
        initialize()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_BOOK_CODE && resultCode == RESULT_OK && data != null) {
            resetSearch()
            val tag = data.getStringExtra(KEY_TAG)
            val (count, y) = queryLastedPosition(tag)
            mTag = tag
            mCount = count
            renderText(tag, count, y)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.INTERNET),
                    REQUEST_PERMISSIONS_CODE)
        } else initialize()
    }


    companion object {
        private const val REQUEST_PERMISSIONS_CODE = 1
        private const val REQUEST_BOOK_CODE = 2
        const val KEY_TYPEFACE = "typeface"
        const val KEY_TAG = "tag"
    }
}
