package euphoria.psycho.library

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import java.io.File
import java.util.*


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
