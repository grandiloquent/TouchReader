package euphoria.psycho.library

import android.app.Activity
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.Toolbar
import android.view.ContextMenu
import android.view.Menu
import android.view.MenuItem
import android.view.View
import kotlinx.android.synthetic.main.activity_book.*
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.async
import org.jetbrains.anko.coroutines.experimental.bg
import org.jetbrains.anko.find

class BookListActivity : AppCompatActivity(), ToolbarManager {
    override val toolbar by lazy { find<Toolbar>(R.id.toolbar) }

    var mBookListAdapter: BookListAdapter? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val unCaughtExceptionHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler(object : Thread.UncaughtExceptionHandler {
            override fun uncaughtException(p0: Thread?, p1: Throwable?) {
                p1?.logToFile()
                if (unCaughtExceptionHandler != null) {
                    unCaughtExceptionHandler.uncaughtException(p0, p1)
                } else System.exit(2)
            }

        })
        setContentView(R.layout.activity_book)
        toolbar.inflateMenu(R.menu.menu_main)
        toolbar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.action_add_from_clipboard -> {
                    addFromClipboard()
                    refreshListView()
                }
                else -> App.instance.toast("Unknown option")
            }
            true
        }

        bookList.layoutManager = LinearLayoutManager(this)
        attachTOScroll(bookList)
        enableHomeAsUp { finish() }
        registerForContextMenu(bookList)
    }

    override fun onCreateContextMenu(menu: ContextMenu?, v: View?, menuInfo: ContextMenu.ContextMenuInfo?) {
        menu?.apply {
            add(0, MENU_ADD_CLIPBOARD, 0, R.string.add_from_clipboard)
            add(0, MENU_ADD_FROM_URL, 0, "从网络添加")
            add(0, MENU_REPLACE_FROM_CLIPBOARD, 0, "从剪切板替换")
            add(0, MENU_CHANGE_TAG, 0, R.string.change_tag)
            add(0, MENU_DELETE_TAG, 0, R.string.context_menu_delete)
        }

        super.onCreateContextMenu(menu, v, menuInfo)
    }

    fun addFromClipboard(tag: String) {
        val clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        if (!clipboardManager.hasPrimaryClip()) return
        if (clipboardManager.primaryClip.itemCount > 0) {
            val text = clipboardManager.primaryClip.getItemAt(0).text;
            if (text != null) {
                DataProvider.getInstance().addFromClipboard(tag, text.toString())
                val message = "成功添加：%s".format(tag);
                toast(message)
            }
        }
    }

    fun replaceFromClipboard(tag: String) {
        dialog("", "输入相应位置") {
            it.toIntOrNull()?.let {
                val clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                if (!clipboardManager.hasPrimaryClip()) return@let
                if (clipboardManager.primaryClip.itemCount > 0) {
                    val text = clipboardManager.primaryClip.getItemAt(0).text;
                    if (text != null) {
                        DataProvider.getInstance().updateDocument(tag, it, text.toString())
                        val message = "成功添加：%s".format(tag);
                        toast(message)
                    }
                }
            }
        }
    }

    fun changeTag(tag: String) {
        dialog(tag, "修改书本名") {
            if (it.isNotBlank())
                DataProvider.getInstance().updateTag(tag, it.trim())
            refreshListView();

        }

    }

    fun deleteByTag(tag: String) {
        DataProvider.getInstance().deleteByTag(tag);
        refreshListView();
    }

    private fun refreshListView() {
        mBookListAdapter?.switchData(DataProvider.getInstance().listTag())
    }

    override fun onContextItemSelected(item: MenuItem?): Boolean {

        var position = -1
        try {
            position = mBookListAdapter!!.position


        } catch (ex: Exception) {
        }
        when (item?.itemId) {
            MENU_ADD_CLIPBOARD -> {
                if (position != -1)
                    addFromClipboard(mBookListAdapter!!.getBook(position))
                return true
            }
            MENU_CHANGE_TAG -> {
                if (position != -1)
                    changeTag(mBookListAdapter!!.getBook(position))
                return true
            }
            MENU_REPLACE_FROM_CLIPBOARD -> {
                if (position != -1)
                    replaceFromClipboard(mBookListAdapter!!.getBook(position))
                return true
            }
            MENU_DELETE_TAG -> {
                if (position != -1)
                    deleteByTag(mBookListAdapter!!.getBook(position))
                return true
            }
            MENU_ADD_FROM_URL -> {
                addFromURL(mBookListAdapter!!.getBook(position))
                return true
            }
            else -> return true
        }

    }

    fun addFromURL(tag: String) {

        dialog("", "输入相应的网址") {
            addFromURLImplement(it, tag)
        }

    }

    fun addFromURLImplement(url: String, tag: String) {
        async(UI) {
            var result = bg {
                if (url.contains("literotica"))
                    url.fetchString(true)
                else
                    url.fetchString()

            }
            updateDatabase(tag, result.await())
            toast("成功添加：$url")
        }
    }

    fun updateDatabase(tag: String, content: String?) {

        content?.let {
            DataProvider.getInstance().addFromClipboard(tag, content)
        }

    }

    fun addFromClipboard() {
        val clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        if (!clipboardManager.hasPrimaryClip()) return
        if (clipboardManager.primaryClip.itemCount > 0) {
            val text = clipboardManager.primaryClip.getItemAt(0).text;
            if (text != null) {
                DataProvider.getInstance().insertArticle(text.toString())
                toast("成功从剪切板添加.")
            }
        }
    }


    override fun onResume() {
        super.onResume()
        loadBook()
    }

    fun loadBook() {
        mBookListAdapter = BookListAdapter(DataProvider.getInstance().listTag()) {

            val intent = Intent().apply {
                putExtra(MainActivity.KEY_TAG, it)
            }
            setResult(Activity.RESULT_OK, intent)
            finish()
        }
        bookList.adapter = mBookListAdapter

    }

    companion object {
        private val MENU_ADD_FROM_URL = 3
        private val MENU_REPLACE_FROM_CLIPBOARD = 5
        private val MENU_ADD_CLIPBOARD = 0
        private val MENU_CHANGE_TAG = 1
        private val MENU_DELETE_TAG = 2
        private val TAG = "BookListActivity"
    }
}

