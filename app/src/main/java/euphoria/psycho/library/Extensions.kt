package euphoria.psycho.library

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Environment
import android.preference.PreferenceManager
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import java.io.File
import java.io.FileFilter
import java.text.DateFormat
import java.util.*

/*
Context
 */
val Context.screenWidth: Int
    get() = applicationContext.resources.displayMetrics.widthPixels
val Context.screenHeight: Int
    get() = applicationContext.resources.displayMetrics.heightPixels
val Context.preferences: SharedPreferences
    get() = PreferenceManager.getDefaultSharedPreferences(this)
val Context.inflater: LayoutInflater
    get() = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

inline fun <reified T : Any> Context.getIntent() = Intent(this, T::class.java)
inline fun <reified T : Any> Context.startActivity() = startActivity(getIntent<T>())
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

inline fun Context.dialog(strValue: String, title: String?, crossinline listener: (v: String) -> Unit) {
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

/*
TextView
 */
val TextView.trimmedText: String
    get() = text.toString().trim()

fun TextView.bringPointIntoView(scrollView: ScrollView, offset: Int) {
    val line = this.layout.getLineForOffset(offset).toFloat()
    val y = ((line + 0.5) * this.lineHeight).toInt()
    scrollView.post { scrollView.scrollTo(0, y - scrollView.height / 2) }
}

/*
View
 */
fun View.slideExit() {
    if (translationY == 0f) animate().translationY(-height.toFloat())
}

fun View.slideEnter() {
    if (translationY < 0f) animate().translationY(0f)
}

/*String*/
//fun String.toIntSafe(): Int {
//    val r = Regex("[0-9]+")
//    val m = r.find(this)
//    return if (m != null) {
//        return m.value.toInt()
//    } else -1
//}
//
//fun String.toFloatSafe(): Float {
//    val r = Regex("[0-9\\.]+")
//    val m = r.find(this)
//    return if (m != null) {
//        return m.value.toFloat()
//    } else -1f
//}
/*
File
 */
fun File.deletes() {
    if (isDirectory)
        walkBottomUp().forEach { it.delete() }
    else
        delete()
}

fun File.getFileListByDirPath(filter: FileFilter): ArrayList<File> {

    val arrayList = ArrayList<File>()
    if (isFile || !exists()) return arrayList
    val files = listFiles(filter)
    if (files == null) arrayList
    for (f in files) arrayList.add(f)
    arrayList.sortWith(compareBy<File> { it.isFile }.thenBy { it.name })
    return arrayList
}

/*
Log
*/
fun Throwable.logToFile() {
    val fileName = File(Environment.getExternalStorageDirectory(), "errors.log");
    this.message?.let {
        fileName.appendText(it)
    }
    fileName.appendText(this.stackTrace.joinToString("\n"))
}

/*
DateTime
 */
fun Long.toDateString(dateFormat: Int = DateFormat.MEDIUM): String {
    val df = DateFormat.getDateInstance(dateFormat, Locale.getDefault())
    return df.format(this)
}
