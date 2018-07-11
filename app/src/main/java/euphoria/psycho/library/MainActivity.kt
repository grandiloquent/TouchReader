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
import android.widget.EditText
import kotlinx.android.synthetic.main.main_activity.*
import java.io.File
import java.util.regex.Pattern

class MainActivity : Activity(), ReaderView.SelectListener {
    var mMenuPanel: MenuPanel? = null
    var mCount = 0
    var mTag: String? = null
    var mSearchList: List<Int>? = null
    var mSearchCount = -1

    var preferenceFontSize: Float by DelegatesExt.preference(this, KEY_FONT_SIZE, DEFAULT_FONT_SIZE)
    var preferenceTypeFace: String by DelegatesExt.preference(this, KEY_TYPEFACE, "")

    private fun initialize() {
        setContentView(R.layout.main_activity)
        readerView.setSelectListener(this)
        initializeToolbar()
        applySettings(preferences)
        loadLasted(null, preferences)
    }

    private fun initializeToolbar() {
        back.setOnClickListener { back() }
        forward.setOnClickListener { forward() }
        menu.setOnClickListener { showMenuPanel() }
        search.setOnClickListener { searchInBooks() }
        search.setOnLongClickListener {
            resetSearch()
            mTag?.let {
                val (count, y) = DataProvider.instance.querySettings(it)
                mCount = count
                renderText(it, count, y)
            }
            true
        }
        showBooklist.setOnClickListener {
            val intent = Intent(App.instance, BookListActivity::class.java)
            startActivityForResult(intent, REQUEST_BOOK_CODE)
        }
    }

    private fun menuFontSizeDecrease() {
        if (preferenceFontSize - .5f > 0f) {
            preferenceFontSize = preferenceFontSize - 0.5f
            readerView.textSize = preferenceFontSize * resources.displayMetrics.scaledDensity
        }
    }

    private fun menuFontSizeIncrease() {

        preferenceFontSize = preferenceFontSize + 0.5f
        readerView.textSize = preferenceFontSize * resources.displayMetrics.scaledDensity
    }

    private fun menuSetPadding() {
        dialog("${preferences.getInt(KEY_PADDING, 0)}", "设置边间距") {
            it.toIntOrNull()?.let {
                readerView.setPadding(it, it, it, it)
                preferences.edit().putInt(KEY_PADDING, it).commit()
            }
        }
    }

    private fun menuSetLineSpace() {
        dialog("${preferences.getInt(KEY_LINESPACINGMULTIPLIER, 0)}", "设置行间距") {
            it.toFloatOrNull()?.let {
                readerView.setLineSpacing(0f, it)
                preferences.edit().putFloat(KEY_LINESPACINGMULTIPLIER, it).commit()
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
        mMenuPanel?.showAtLocation(layout, Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL, 0, 0)
    }

    private fun back() {
        mTag?.let {
            mSearchList?.let {
                if (mSearchCount - 1 > -1) {
                    renderText(mTag, it[--mSearchCount], 0)
                    implementSearchHighLight(preferences.getString(KEY_PATTERN, null))
                    toast("当前位置：${it[mSearchCount]}")
                }
                return
            }
            if (mCount - 1 > 0) {
                renderText(it, --mCount, 0)
            }
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
        mTag?.let {
            dialog("$mCount", "1-${DataProvider.instance.queryCount(it)}") {
                it.toIntOrNull()?.let {
                    renderText(mTag, it, 0)
                    mCount = it
                }
            }
        }

    }

    fun menuSetFontSize() {
        dialog("${preferences.getFloat(KEY_FONT_SIZE, 0f)}", "设置字体大小") {
            it.toFloatOrNull()?.let {
                preferenceFontSize = it
                readerView.textSize = it * resources.displayMetrics.scaledDensity
            }
        }
    }

    private fun searchInBooks() {
        mTag?.let {
            resetSearch()
            dialog(preferences.getString(KEY_PATTERN, null),
                    "使用正则表达式搜索") {
                if (it.isNotBlank()) {
                    try {
                        preferences.edit().putString(KEY_PATTERN, it.trim()).commit()
                        mSearchList = DataProvider.instance.queryMatchesContent(mTag!!, it.trim())
                        if (mSearchList != null) {
                            val y = scrollView.scrollY
                            DataProvider.instance.updateSettings(mTag!!, mCount, y)
                            forward()
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, e.message)
                    }
                }

            };
        }


    }

    private fun implementSearchHighLight(pattern: String) {
        val p = Pattern.compile(pattern)
        val value = readerView.text.toString()
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
        readerView.text = linkifiedText
        readerView.bringPointIntoView(scrollView, offset)
    }

    private fun renderText(tag: String?, count: Int, scrollY: Int) {
        val v = DataProvider.instance.queryContent(tag!!, count)
        readerView.text = v
        if (scrollY > -1)
            scrollView.post { scrollView.scrollTo(0, scrollY) }
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
        typeface?.let {
            var f = File(typeface)
            if (f.isFile) readerView.typeface = Typeface.createFromFile(f)
        }

        readerView.textSize = preferenceFontSize * resources.displayMetrics.scaledDensity
        val padding = sharedPreferences.getInt(KEY_PADDING, 0)
        if (padding > 0) readerView.setPadding(padding, padding, padding, padding)
        val lineSpacingMultiplier = sharedPreferences.getFloat(KEY_LINESPACINGMULTIPLIER, 0f)
        if (lineSpacingMultiplier > 0f) readerView.setLineSpacing(0f, lineSpacingMultiplier)
    }

    fun resetSearch() {
        mSearchList?.let {
            mSearchList = null
            mSearchCount = -1
        }
    }

    override fun onPause() {
        mTag?.let {
            preferences.edit().putString(KEY_TAG, it).commit()
            val y = scrollView.scrollY
            DataProvider.instance.updateSettings(it, mCount, y)
        }
        super.onPause()
    }

    override fun onSelectionChange(value: String?) {
        value?.let {
            DictionaryTask { v -> dicTextView.text = v }.execute(it)
        }
    }

    override fun onClick() {
        dicTextView.text = null
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>?, grantResults: IntArray?) {
        initialize()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_BOOK_CODE && resultCode == RESULT_OK && data != null) {
            resetSearch()
            val tag = data.getStringExtra(KEY_TAG)
            val (count, y) = DataProvider.instance.querySettings(tag)
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
        private const val KEY_FONT_SIZE = "fontsize"
        private const val DEFAULT_FONT_SIZE = 5f
        private const val KEY_PATTERN = "pattern"
        private const val KEY_PADDING = "padding"
        private const val KEY_LINESPACINGMULTIPLIER = "lineSpacingMultiplier"
        const val KEY_TYPEFACE = "typeface"
        const val KEY_TAG = "tag"
    }
}