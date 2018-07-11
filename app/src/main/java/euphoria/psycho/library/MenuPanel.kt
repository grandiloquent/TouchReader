package euphoria.psycho.library
import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.view.View
import android.view.WindowManager
import android.widget.GridView
import android.widget.PopupWindow
import android.widget.SimpleAdapter
class MenuPanel(val context: Context,
                val iconResources: IntArray,
                val iconNames: Array<String>,
                val listener: (position: Int) -> Unit) : PopupWindow(context) {
    private val mMenuView: View
    private val mGridView: GridView
    init {
        mMenuView = context.inflater.inflate(R.layout.popwindow_choose_icon, null)
        mGridView = mMenuView.findViewById(R.id.gridview)
        val dataList = ArrayList<Map<String, Any>>()
        for (i in (0..iconResources.size - 1) step 1) {
            val map = HashMap<String, Any>()
            map.put("image", iconResources[i])
            map.put("text", iconNames[i])
            dataList.add(map)
        }
        val from = arrayOf("image", "text")
        val to = intArrayOf(R.id.image, R.id.text)
        val simpleAdapter = SimpleAdapter(context, dataList, R.layout.item_pic_gv, from, to)
        mGridView.run {
            adapter = simpleAdapter
            setOnItemClickListener { _, _, p, _ -> listener(p) }
        }
        contentView = mMenuView
        width = WindowManager.LayoutParams.MATCH_PARENT
        height = WindowManager.LayoutParams.WRAP_CONTENT
        isFocusable = true
        animationStyle = R.style.mypopwindow_anim_style
        setBackgroundDrawable(ColorDrawable(0xb0000000.toInt()))
    }
}