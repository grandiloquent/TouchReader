package euphoria.psycho.library

import android.app.AlertDialog
import android.content.Context
import android.util.Log
import android.widget.EditText
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast


val Context.screenWidth: Int
    get() {
        return applicationContext.resources.displayMetrics.widthPixels
    }

fun Context.dp2x(dp: Float): Int {
    val scale = applicationContext.resources.displayMetrics.density
    return (dp * scale + .5f).toInt()
}

fun Context.toast(message: String, isShort: Boolean = true) {
    if (isShort)
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    else
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()

}

fun Context.dialog(strValue: String, title: String?, listener: (v: String) -> Unit) {
    val et = EditText(this)
    et.setText(strValue)

    val b = AlertDialog.Builder(this)
            .setView(et)
            .setNegativeButton("取消") { d, _ -> d.dismiss() }
            .setPositiveButton("确定") { d, _ ->
                listener(et.text.toString())
                d.dismiss()
            }
    if (title != null) {
        b.setTitle(title)
    }
    b.show()
}

fun TextView.bringPointIntoView(scrollView: ScrollView, offset: Int) {
    val line = this.layout.getLineForOffset(offset).toFloat()
    val y = ((line + 0.5) * this.lineHeight).toInt()
    scrollView.post { scrollView.scrollTo(0, y - scrollView.height / 2) }
}
/*String*/

fun String.toIntSafe(): Int {
    val r = Regex("[0-9]+")
    val m = r.find(this)
    return if (m != null) {
        return m.value.toInt()
    } else -1
}

fun String.toFloatSafe(): Float {
    val r = Regex("[0-9\\.]+")
    val m = r.find(this)
    return if (m != null) {
        return m.value.toFloat()
    } else -1f
}
