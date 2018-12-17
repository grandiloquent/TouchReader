package euphoria.psycho.library

import android.content.Context
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.view.*
import java.io.File
import java.io.FileFilter

class DirectoryFragment : Fragment() {
    private var mEmptyView: View? = null
    private var mPath: String? = null
    private var mCompositeFilter: CompositeFilter? = null
    private var mEmptyRecyclerView: EmptyRecyclerView? = null
    private var mDirectoryAdapter: DirectoryAdapter? = null
    private var mFileClickListener: FileClickListener? = null
    private fun initializeArgs() {
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

    private fun initializeFilesList() {
        mDirectoryAdapter = DirectoryAdapter(context!!, File(mPath).getFileListByDirPath(mCompositeFilter!!)) { _, p ->
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

    private fun refresh() {
        mDirectoryAdapter?.switchData(File(mPath).getFileListByDirPath(mCompositeFilter!!))
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
        when (item.itemId) {
            R.id.action_delete -> {
                mDirectoryAdapter?.getModel(i.position)?.deletes()
                refresh()
            }
            R.id.action_import_file -> {
                mDirectoryAdapter?.getModel(i.position)?.let {
                    DataProvider.getInstance().importFile(it)
                }
            }
            R.id.action_convert_to_txt -> {
                mDirectoryAdapter?.getModel(i.position)?.let {
                    convertToTextFile(it)
                }
            }
            R.id.action_typeface -> {
                mDirectoryAdapter?.getModel(i.position)?.let {
                    if (it.name.endsWith(".ttf")) {
                        context?.run { preferences.edit().putString(MainActivity.KEY_TYPEFACE, it.absolutePath).apply() }
                    }
                }
            }
        }
        return true
    }

    private fun convertToTextFile(f: File) {
        if (!f.isFile) return
        var htmRegex = Regex("\\.(?:html|htm|xhtml)$")

        if (htmRegex.containsMatchIn(f.name)) {
            val t = File(f.absolutePath.substringBeforeLast('.') + ".txt")
            t.writeText(f.readText().htm2txt())
            refresh()
        }
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