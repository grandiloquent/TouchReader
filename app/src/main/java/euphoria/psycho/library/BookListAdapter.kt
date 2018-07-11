package euphoria.psycho.library

import android.support.v7.widget.RecyclerView
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.item_book.*
import kotlinx.android.extensions.LayoutContainer

class BookListAdapter(private val books: MutableList<String>,
                      private val itemClick: (String) -> Unit)
    : RecyclerView.Adapter<BookListAdapter.ViewHolder>() {

    private var currentPostion = -1
    var position: Int
        get() = currentPostion
        set(value) {
            currentPostion = value
        }

    fun getBook(p: Int): String = books[p]

    fun switchData(l: ArrayList<String>) {
        with(books) {
            clear()
            addAll(l)
            notifyDataSetChanged()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, p1: Int): ViewHolder {

        val view = parent.context.inflater.inflate(R.layout.item_book, parent, false)
        return ViewHolder(view, itemClick)
    }

    override fun onBindViewHolder(holder: ViewHolder, p: Int) {
        holder.bindBook(books[p])
        holder.itemView.setOnLongClickListener {
            position = p
            it.showContextMenu()
            true
        }
    }

    override fun onViewRecycled(holder: ViewHolder) {
        holder.itemView.setOnLongClickListener(null)
        super.onViewRecycled(holder)
    }

    override fun getItemCount(): Int = books.size

    class ViewHolder(override val containerView: View,
                     private val itemClick: (String) -> Unit) :
            RecyclerView.ViewHolder(containerView), LayoutContainer {
        fun bindBook(bookName: String) {

            with(bookName) {
                textView.text = this
                itemView.setOnClickListener { itemClick(this) }

            }
        }
    }
}