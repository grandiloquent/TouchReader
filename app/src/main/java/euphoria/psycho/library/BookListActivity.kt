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
import org.jetbrains.anko.find

class BookListActivity : AppCompatActivity(), ToolbarManager {
    override val toolbar by lazy { find<Toolbar>(R.id.toolbar) }

    var mBookListAdapter: BookListAdapter? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_book)
        initToolbar()

        bookList.layoutManager = LinearLayoutManager(this)
        attachTOScroll(bookList)
        enableHomeAsUp { finish() }
        registerForContextMenu(bookList)
    }

    override fun onCreateContextMenu(menu: ContextMenu?, v: View?, menuInfo: ContextMenu.ContextMenuInfo?) {
        menu?.apply {
            add(0, MENU_ADD_CLIPBOARD, 0, R.string.add_from_clipboard)
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
                DataProvider.instance.addFromClipboard(tag, text.toString())
                val message = "成功添加：%s".format(tag);
                toast(message)
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menu?.add(0, MENU_ADD_CLIPBOARD, 0, R.string.add_from_clipboard)
        return super.onCreateOptionsMenu(menu)
    }

    fun changeTag(tag: String) {
        dialog(tag, "修改书本名") {
            if (it.isNotBlank())
                DataProvider.instance.updateTag(tag, it.trim())
            refreshListView();

        }

    }

    fun deleteByTag(tag: String) {
        DataProvider.instance.deleteByTag(tag);
        refreshListView();
    }

    private fun refreshListView() {
        mBookListAdapter?.switchData(DataProvider.instance.listTag())
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
            MENU_DELETE_TAG -> {
                if (position != -1)
                    deleteByTag(mBookListAdapter!!.getBook(position))
                return true
            }
            else -> return true
        }

    }

    fun addFromClipboard() {
        val clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        if (!clipboardManager.hasPrimaryClip()) return
        if (clipboardManager.primaryClip.itemCount > 0) {
            val text = clipboardManager.primaryClip.getItemAt(0).text;
            if (text != null) {
                DataProvider.instance.insertArticle(text.toString())
                toast("成功从剪切板添加.")
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            MENU_ADD_CLIPBOARD -> {
                addFromClipboard()
                return true;
            }
            else -> return true
        }

    }

    override fun onResume() {
        super.onResume()
        loadBook()
    }

    fun loadBook() {
        mBookListAdapter = BookListAdapter(DataProvider.instance.listTag()) {

            val intent = Intent().apply {
                putExtra(MainActivity.KEY_TAG, it)
            }
            setResult(Activity.RESULT_OK, intent)
            finish()
        }
        bookList.adapter = mBookListAdapter

    }

    companion object {
        private val MENU_ADD_CLIPBOARD = 0
        private val MENU_CHANGE_TAG = 1
        private val MENU_DELETE_TAG = 2
        private val TAG = "BookListActivity"
    }
}

