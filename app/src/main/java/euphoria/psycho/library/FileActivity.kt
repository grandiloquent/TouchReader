package euphoria.psycho.library

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.support.v4.app.ActivityCompat
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.Toolbar
import android.text.TextUtils
import android.util.AttributeSet
import android.util.Log
import android.view.*
import android.widget.ImageView
import android.widget.TextView
import java.io.File
import java.io.FileFilter
import java.io.Serializable
import java.lang.reflect.Field
import java.util.*
import kotlin.math.log


class FileActivity : AppCompatActivity(), FileClickListener {

    var mToolbar: Toolbar? = null
    var mStartPath: String? = Environment.getExternalStorageDirectory().absolutePath
    var mCurrentPath: String? = mStartPath

    var mTitle: String? = null

    override fun onClick(f: File?) {

        Handler().postDelayed({
            run {
                f?.let {
                    if (f.isDirectory) {
                        mCurrentPath = f.absolutePath

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

class CompositeFilter(val filters: ArrayList<FileFilter>) : FileFilter, Serializable {
    override fun accept(f: File?): Boolean {

        for (filter in filters) {
            if (!filter.accept(f)) {
                return false
            }
        }

        return true
    }

}

class EmptyRecyclerView(context: Context,
                        attributeSet: AttributeSet?,
                        defStyle: Int) : RecyclerView(context, attributeSet, defStyle) {
    constructor(context: Context, attributeSet: AttributeSet?) : this(context, attributeSet, 0)
    constructor(context: Context) : this(context, null, 0)

    var mRecyclerViewContextMenuInfo: RecyclerViewContextMenuInfo? = null


    var mEmptyView: View? = null
    val mObserver = object : AdapterDataObserver() {
        override fun onChanged() {
            super.onChanged()
            checkIfEmpty()
        }
    }

    fun setEmptyView(emptyView: View?) {
        mEmptyView = emptyView
        checkIfEmpty()
    }

    fun checkIfEmpty() {
        mEmptyView?.let {
            if (adapter?.itemCount!! > 0)
                it.visibility = View.GONE
            else
                it.visibility = View.VISIBLE
        }
    }

    override fun showContextMenuForChild(originalView: View?): Boolean {

        val longPressPosition = getChildLayoutPosition(originalView!!)
        if (longPressPosition >= 0) {
            val longPressId = adapter?.getItemId(longPressPosition)
            mRecyclerViewContextMenuInfo = RecyclerViewContextMenuInfo(longPressPosition, longPressId!!)
            return super.showContextMenuForChild(originalView)
        }
        return false


    }

    override fun getContextMenuInfo(): ContextMenu.ContextMenuInfo {
        return mRecyclerViewContextMenuInfo!!
    }

    override fun setAdapter(adapter: Adapter<*>?) {
        val o = getAdapter()
        o?.let {
            it.unregisterAdapterDataObserver(mObserver)
        }
        super.setAdapter(adapter)
        adapter?.let {
            it.registerAdapterDataObserver(mObserver)
        }
    }

    class RecyclerViewContextMenuInfo(val position: Int, val id: Long) : ContextMenu.ContextMenuInfo {

    }
}

class DirectoryAdapter(private val context: Context,
                       val list: ArrayList<File>,
                       val listener: (v: View, position: Int) -> Unit) : RecyclerView.Adapter<DirectoryAdapter.DirectoryViewHolder>() {
    fun getModel(index: Int): File {
        return list[index]
    }

    fun switchData(l: List<File>) {
        list.clear()
        list.addAll(l)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DirectoryViewHolder {
        val view = context.inflater.inflate(R.layout.item_file, parent, false)
        return DirectoryViewHolder(view, listener)
    }

    override fun getItemCount(): Int {
        return list.size
    }

    override fun onBindViewHolder(viewHolder: DirectoryViewHolder, position: Int) {
        val currentFile = list[position]
        val fileType = FileTypeUtils.getFileType(currentFile)
        viewHolder.run {
            fileImageView?.setImageResource(fileType.icon)
            fileSubTitle?.setText(fileType.description)
            fileTitle?.text = currentFile.name
        }
    }


    class DirectoryViewHolder(view: View,
                              val listener: (v: View, position: Int) -> Unit) : RecyclerView.ViewHolder(view) {
        var fileImageView: ImageView? = null
        var fileTitle: TextView? = null
        var fileSubTitle: TextView? = null

        init {
            fileImageView = view.findViewById(R.id.item_file_image)
            fileTitle = view.findViewById(R.id.item_file_title)
            fileSubTitle = view.findViewById(R.id.item_file_subtitle)
            view.setOnClickListener { v -> listener(v, adapterPosition) }
            view.setOnLongClickListener { v -> v.showContextMenu() }
        }
    }
}

interface FileClickListener {
    fun onClick(f: File?)

}

class DirectoryFragment : Fragment() {
    var mEmptyView: View? = null
    var mPath: String? = null
    var mCompositeFilter: CompositeFilter? = null
    var mEmptyRecyclerView: EmptyRecyclerView? = null
    var mDirectoryAdapter: DirectoryAdapter? = null
    var mFileClickListener: FileClickListener? = null


    fun initializeArgs() {
        mCompositeFilter = CompositeFilter(arrayListOf(object : FileFilter {
            override fun accept(f: File?): Boolean {
                return true
            }
        }))
        arguments?.let {
            //mCompositeFilter = it.getSerializable(ARG_FILTER) as CompositeFilter?
            it.getString(ARG_FILE_PATH)?.also {
                mPath = it
            }
        }
    }

    fun initializeFilesList() {
        mDirectoryAdapter = DirectoryAdapter(context!!, FileUtils.getFileListByDirPath(mPath, mCompositeFilter)) { _, p ->
            mFileClickListener?.let {
                it.onClick(mDirectoryAdapter?.getModel(p))
            }
        }

        mEmptyRecyclerView?.run {
            layoutManager = LinearLayoutManager(activity)
            adapter = mDirectoryAdapter
            setEmptyView(mEmptyView)
        }

    }

    fun refresh() {
        mDirectoryAdapter?.switchData(FileUtils.getFileListByDirPath(mPath, mCompositeFilter))
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initializeArgs()
        initializeFilesList()
        registerForContextMenu(mEmptyRecyclerView)
    }


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_directory, container, false)
        mEmptyRecyclerView = view.findViewById(R.id.directory_recycler_view)
        mEmptyView = view.findViewById(R.id.directory_empty_view)
        return view
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        mFileClickListener = context as FileClickListener
    }

    override fun onCreateContextMenu(menu: ContextMenu?, v: View?, menuInfo: ContextMenu.ContextMenuInfo?) {
        activity?.menuInflater?.inflate(R.menu.menu_file_context, menu)
        super.onCreateContextMenu(menu, v, menuInfo)
    }

    override fun onContextItemSelected(item: MenuItem?): Boolean {

        val i = item?.menuInfo as EmptyRecyclerView.RecyclerViewContextMenuInfo
        when (item?.itemId) {

            R.id.action_delete -> {
                mDirectoryAdapter?.getModel(i.position)?.deletes()
                refresh()

            }
        }
        return super.onContextItemSelected(item)
    }

    override fun onDetach() {
        super.onDetach()
        mFileClickListener = null
    }


    companion object {
        private const val ARG_FILE_PATH = "arg_file_path"
        private const val ARG_FILTER = "arg_filter"

        private var instance: DirectoryFragment? = null

        fun getInstance(path: String): DirectoryFragment {

            return DirectoryFragment().also {
                val args = Bundle()
                args.putString(ARG_FILE_PATH, path)
                //args.putSerializable(ARG_FILTER, filter)
                it.arguments = args

            }
        }

//        private var instance: DirectoryFragment? = null
//
//        fun getInstance(path: String, filter: CompositeFilter): DirectoryFragment {
//            return instance ?: synchronized(this) {
//                DirectoryFragment().also {
//                    val args = Bundle()
//                    args.putString(ARG_FILE_PATH, path)
//                    args.putSerializable(ARG_FILTER, filter)
//                    it.arguments = args
//                    instance = it
//                }
//            }
//        }
    }
}