package euphoria.psycho.library

import android.app.Activity
import android.app.AlertDialog
import android.content.ClipboardManager
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.*

class BookListActivity : Activity() {

    private var mList: MutableList<String>? = null
    private var mListView: ListView? = null
    private var mBookAdapter: BookAdapter? = null

    companion object {
        private val MENU_ADD_CLIPBOARD = 0
        private val MENU_CHANGE_TAG = 1
        private val MENU_DELETE_TAG = 2
        private val TAG = "BookListActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initialize()
    }

    override fun onCreateContextMenu(menu: ContextMenu?, v: View?, menuInfo: ContextMenu.ContextMenuInfo?) {
        menu?.add(0, MENU_ADD_CLIPBOARD, 0, R.string.add_from_clipboard)
        menu?.add(0, MENU_CHANGE_TAG, 0, R.string.change_tag)
        menu?.add(0, MENU_DELETE_TAG, 0, R.string.context_menu_delete)

        super.onCreateContextMenu(menu, v, menuInfo)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menu?.add(0, MENU_ADD_CLIPBOARD, 0, R.string.add_from_clipboard)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            MENU_ADD_CLIPBOARD -> {
                addFromClipboard()
                return true;
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onContextItemSelected(item: MenuItem?): Boolean {

        when (item?.itemId) {
            MENU_ADD_CLIPBOARD -> {
                addFromClipboard(mList!![getPosition(item)])
                return true
            }
            MENU_CHANGE_TAG -> {
                changeTag(mList!![getPosition(item)])
                return true
            }
            MENU_DELETE_TAG -> {
                deleteByTag(mList!![getPosition(item)])
                return true
            }
        }
        return super.onContextItemSelected(item)
    }

    fun changeTag(tag: String) {
        var editText = EditText(this)
        editText.setText(tag)

        val builder = AlertDialog.Builder(this)

        builder.setView(editText)
        builder.setNegativeButton("取消") { dialogInterface, i -> dialogInterface.dismiss() }

        builder.setPositiveButton("确定", DialogInterface.OnClickListener { dialogInterface, _ ->
            if (editText.text == null) return@OnClickListener
            DataProvider.getInstance().updateTag(tag, editText.text.toString().trim())
            dialogInterface.dismiss()
            refreshListView()
        })
        builder.show()
    }

    fun deleteByTag(tag: String) {
        DataProvider.getInstance().deleteByTag(tag);
        refreshListView();
    }

    fun refreshListView() {
        mBookAdapter?.switchData(DataProvider.getInstance().listTag())
    }

    fun getPosition(item: MenuItem?): Int {
        val menuInfo = item?.menuInfo as AdapterView.AdapterContextMenuInfo
        return menuInfo.position
    }

    private fun initialize() {
        setContentView(R.layout.booklis_activity)
        mList = DataProvider.getInstance().listTag()
        mListView = findViewById(R.id.listView)
        mBookAdapter = BookAdapter()

        mListView?.adapter = mBookAdapter
        mListView?.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, id ->

            val bundle = Intent()
            bundle.putExtra(MainActivity.KEY_TAG, mList!![position]);
            setResult(RESULT_OK, bundle)
            finish()
        }
        registerForContextMenu(mListView)
    }

    fun toast(context: Context, message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }

    fun addFromClipboard(tag: String) {

        val clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

        if (!clipboardManager.hasPrimaryClip()) return
        if (clipboardManager.primaryClip.itemCount > 0) {
            val text = clipboardManager.primaryClip.getItemAt(0).text;
            if (text != null) {
                DataProvider.getInstance().addFromClipboard(tag, text.toString())
                val message = "成功添加：%s".format(tag);
                toast(this, message)
            }
        }
    }

    fun addFromClipboard() {

        val clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

        if (!clipboardManager.hasPrimaryClip()) return
        if (clipboardManager.primaryClip.itemCount > 0) {
            val text = clipboardManager.primaryClip.getItemAt(0).text;
            if (text != null) {
                DataProvider.getInstance().insertArticle(text.toString())
                toast(this, "成功从剪切板添加.")
            }
        }
    }

    private inner class ViewHolder {
        var textView: TextView? = null
    }


    private inner class BookAdapter : BaseAdapter() {


        override fun getView(p0: Int, p1: View?, p2: ViewGroup?): View? {
            var view = p1
            val viewHolder: ViewHolder
            if (view == null) {
                view = inflater.inflate(R.layout.list_item, p2, false)
                viewHolder = ViewHolder().also {
                    it.textView = view?.findViewById(R.id.title)
                }
                view?.tag = viewHolder
            } else {
                viewHolder = view.tag as ViewHolder
            }
            viewHolder.textView?.text = mList?.get(p0)
            return view
        }

        override fun getItem(p0: Int): String? {
            return mList?.get(p0)
        }

        override fun getItemId(p0: Int): Long {
            return p0.toLong()
        }

        fun switchData(list: List<String>) {
            mList?.run {
                clear()
                addAll(list)
            }

            notifyDataSetChanged()
        }

        override fun getCount(): Int {
            return mList!!.size
        }

    }
}