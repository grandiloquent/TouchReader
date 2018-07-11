package euphoria.psycho.library

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.text.style.BackgroundColorSpan
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.AdapterView
import android.widget.EditText
import android.widget.ScrollView
import android.widget.TextView
import java.io.File
import java.util.regex.Pattern

class MainActivity : Activity(), ReaderView.SelectListener {

    //var mSelectPicPopupWindow: SelectPicPopupWindow? = null

    var mMenuPanel: MenuPanel? = null
    var mReaderView: ReaderView? = null
    var mCount = 0
    var mTag: String? = null
    var mScrollView: ScrollView? = null
    var mSearchList: List<Int>? = null
    var mSearchCount = -1
    var mDicTextView: TextView? = null

    private fun initialize() {

        setContentView(R.layout.main_activity)

        mDicTextView = findViewById(R.id.dicTextView)
        mScrollView = findViewById(R.id.scrollView)

        mReaderView = findViewById(R.id.readerView)
        mReaderView?.let {
            it.setSelectListener(this)
        }

        initializeToolbar()



        applySettings(preferences)
        loadLasted(null, preferences)
    }

    private fun initializeToolbar() {
        findViewById<View>(R.id.back).setOnClickListener { back() }
        findViewById<View>(R.id.forward).setOnClickListener { _ -> forward() }
        findViewById<View>(R.id.menu).setOnClickListener { _ -> showMenuPanel() }
        findViewById<View>(R.id.search).setOnClickListener { _ -> searchInBooks() }
        findViewById<View>(R.id.search).setOnLongClickListener { _ ->
            resetSearch()
            if (mTag != null) {
                val (count, y) = queryLastedPosition(mTag!!)
                mCount = count
                renderText(mTag, count, y)
            }
            true
        }
        findViewById<View>(R.id.showBooklist).setOnClickListener { _ ->
            val intent = Intent(App.instance, BookListActivity::class.java)
            startActivityForResult(intent, REQUEST_BOOK_CODE)
        }
    }

    private fun menuFontSizeDecrease() {

        var fontSize = preferences.getFloat(KEY_FONTSIZE, 0f)
        if (fontSize == 0f || fontSize - 0.5f < 0f) return
        fontSize = fontSize - 0.5f
        preferences.edit().putFloat(KEY_FONTSIZE, fontSize).commit()
        mReaderView!!.textSize = fontSize * resources.displayMetrics.scaledDensity
    }

    private fun menuFontSizeIncrease() {

        var fontSize = preferences.getFloat(KEY_FONTSIZE, 0f)

        fontSize = fontSize + 0.5f
        preferences.edit().putFloat(KEY_FONTSIZE, fontSize).commit()
        mReaderView!!.textSize = fontSize * resources.displayMetrics.scaledDensity
    }

    private fun menuSetPadding() {

        dialog("${preferences.getInt(KEY_PADDING, 0)}", "设置边间距") {
            val v = it.toIntSafe()
            if (v > 0) {
                mReaderView?.setPadding(v, v, v, v)
                preferences.edit().putInt(KEY_PADDING, v).commit()
            }
        }
    }

    private fun menuSetLineSpace() {

        dialog("${preferences.getInt(KEY_LINESPACINGMULTIPLIER, 0)}", "设置行间距") {
            val v = it.toFloatSafe()
            if (v > 0) {
                mReaderView?.setLineSpacing(0f, v)
                preferences.edit().putFloat(KEY_LINESPACINGMULTIPLIER, v).commit()
            }
        }
    }

    private fun showMenuPanel() {

        mMenuPanel = MenuPanel(this,
                intArrayOf(R.mipmap.ic_arrow_forward_black_18dp, R.mipmap.ic_format_size_black_18dp, R.mipmap.ic_apps_black_18dp, R.mipmap.ic_format_size_black_18dp, R.mipmap.ic_format_size_black_18dp, R.mipmap.ic_format_size_black_18dp, R.mipmap.ic_format_size_black_18dp, R.mipmap.ic_format_size_black_18dp),
                arrayOf(resources.getString(R.string.menu_jump_to), resources.getString(R.string.menu_font_size), resources.getString(R.string.menu_file_list), resources.getString(R.string.menu_font_decrease), resources.getString(R.string.menu_font_increase), resources.getString(R.string.menu_change_dictionary), resources.getString(R.string.menu_set_padding), resources.getString(R.string.menu_set_line))) {
            mMenuPanel?.dismiss()
            if (it == 0) {
                menuJumpTo()
            } else if (it == 1) {
                menuSetFontSize()
            } else if (it == 2) {
                val intent = Intent(MainActivity@ this, FileActivity::class.java)
                startActivity(intent)
            } else if (it == 3) {
                menuFontSizeDecrease()

            } else if (it == 4) {
                menuFontSizeIncrease()
            } else if (it == 5) {

            } else if (it == 6) {
                menuSetPadding()
            } else if (it == 7) {
                menuSetLineSpace()
            }

        }
        mMenuPanel?.showAtLocation(this.findViewById(R.id.layout), Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL, 0, 0)
        //显示窗口,设置layout在PopupWindow中显示的位置
        //mSelectPicPopupWindow!!.showAtLocation(this.findViewById(R.id.layout), Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL, 0, 0)

    }

    private fun back() {
        if (mTag == null) return

        if (mSearchList != null) {
            if (mSearchCount - 1 > -1) {
                renderText(mTag, mSearchList!![--mSearchCount], 0)
                implementSearchHighLight(preferences.getString(KEY_PATTERN, null))
                toast("当前位置：${mSearchList!![mSearchCount]}")
            }
            return
        }
        if (mCount - 1 > 0) {
            renderText(mTag, --mCount, 0)
        }
    }

    private fun forward() {
        if (mTag == null) return
        if (mSearchList != null) {
            if (mSearchCount + 1 < mSearchList!!.size) {
                renderText(mTag, mSearchList!![++mSearchCount], 0)
                implementSearchHighLight(preferences.getString(KEY_PATTERN, null))
                toast("当前位置：${mSearchList!![mSearchCount]}")
            }
            return
        }
        ++mCount
        renderText(mTag, mCount, 0)
    }

    fun menuJumpTo() {
        if (mTag == null) return
        dialog("$mCount", "1-${DataProvider.instance.queryCount(mTag!!)}") {
            val count = it.toIntSafe()
            renderText(mTag, count, 0)
            mCount = count
        }
    }

    fun menuSetFontSize() {

        dialog("${preferences.getFloat(KEY_FONTSIZE, 0f)}", "设置字体大小") {
            var s = it.toFloatSafe()
            if (s > 0f) {
                preferences.edit().putFloat(KEY_FONTSIZE, s).commit()
                mReaderView?.textSize = s * resources.displayMetrics.scaledDensity
            }
        }
    }

    private fun searchInBooks() {
        if (mTag == null) return
        resetSearch()

        val et = EditText(this)
        et.setText(preferences.getString(KEY_PATTERN, null))

        val b = AlertDialog.Builder(this)
                .setView(et)
                .setNegativeButton("取消") { d, _ -> d.dismiss() }
                .setPositiveButton("确定") { d, _ ->
                    val v = et.text.toString()
                    if (v.isNullOrBlank()) return@setPositiveButton
                    try {
                        preferences.edit().putString(KEY_PATTERN, v.trim()).commit()
                        mSearchList = DataProvider.instance.queryMatchesContent(mTag!!, v.trim())
                        if (mSearchList != null) {
                            val y = mScrollView?.scrollY ?: 0
                            DataProvider.instance.updateSettings(mTag!!, mCount, y)
                            forward()
                        }

                    } catch (e: Exception) {
                        Log.e(TAG, e.message)
                    }
                }

        b.show()

    }

    private fun implementSearchHighLight(p: String) {
        val p = Pattern.compile(p)

        val value = mReaderView!!.text.toString()

        val linkifiedText = SpannableStringBuilder(value)


        val matcher = p.matcher(value)
        var offset = 0
        while (matcher.find()) {
            if (offset == 0)
                offset = matcher.start()
            val start = matcher.start()
            val end = matcher.end()
            val span = BackgroundColorSpan(Color.YELLOW)
            linkifiedText.setSpan(span, start, end, 0)
        }
        mReaderView?.text = linkifiedText
        mReaderView?.bringPointIntoView(mScrollView!!, offset)

    }

    private fun renderText(tag: String?, count: Int, scrollY: Int) {
        val v = DataProvider.instance.queryContent(tag!!, count)
        mReaderView?.text = v
        if (scrollY > -1)
            mScrollView?.post { mScrollView?.scrollTo(0, scrollY) }
    }

    fun loadLasted(tag: String?, sharedPreferences: SharedPreferences) {

        val t = tag ?: sharedPreferences.getString(KEY_TAG, null) ?: return
        mTag = t

        val (count, y) = DataProvider.instance.querySettings(t)

        mCount = count
        renderText(t, count, y)
    }

    fun applySettings(sharedPreferences: SharedPreferences) {

        // Setting typeface
        val typeface = sharedPreferences.getString(KEY_TYPEFACE, null)
        if (typeface != null) {
            val tf = File(typeface)
            if (tf.isFile)
                mReaderView?.typeface = Typeface.createFromFile(tf)
        }

        val fontSize = sharedPreferences.getFloat(KEY_FONTSIZE, 0f)
        if (fontSize > 0f) mReaderView?.textSize = fontSize * resources.displayMetrics.scaledDensity

        val padding = sharedPreferences.getInt(KEY_PADDING, 0)
        if (padding > 0) mReaderView?.setPadding(padding, padding, padding, padding)


        val lineSpacingMultiplier = sharedPreferences.getFloat(KEY_LINESPACINGMULTIPLIER, 0f)
        if (lineSpacingMultiplier > 0f) mReaderView?.setLineSpacing(0f, lineSpacingMultiplier)

    }

    fun queryLastedPosition(tag: String): Pair<Int, Int> {

        return DataProvider.instance.querySettings(tag)

    }

    fun resetSearch() {
        if (mSearchList != null) {
            mSearchList = null
            mSearchCount = -1
        }
    }


    override fun onPause() {
        if (mTag != null) {
            preferences.edit().putString(KEY_TAG, mTag).commit()
            val y = mScrollView?.scrollY ?: 0
            DataProvider.instance.updateSettings(mTag!!, mCount, y)
        }
        super.onPause()
    }

    override fun onSelectionChange(value: String?) {
        if (!value.isNullOrEmpty()) {
            DictionaryTask { v -> mDicTextView?.text = v }.execute(value)
        }
    }

    override fun onClick() {
        mDicTextView?.apply {
            text = null
        }
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
        private const val TAG = "MainActivity"
        private const val KEY_FONTSIZE = "fontsize"
        private const val KEY_PATTERN = "pattern"
        private const val KEY_PADDING = "padding"
        private const val KEY_LINESPACINGMULTIPLIER = "lineSpacingMultiplier"
        const val KEY_TYPEFACE = "typeface"
        const val KEY_TAG = "tag"
    }
}
