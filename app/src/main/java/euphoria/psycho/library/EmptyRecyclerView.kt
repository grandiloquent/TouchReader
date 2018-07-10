
package euphoria.psycho.library

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.util.AttributeSet
import android.view.ContextMenu
import android.view.View

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


