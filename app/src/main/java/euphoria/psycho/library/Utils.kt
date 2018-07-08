package euphoria.psycho.library

import android.content.Context
import java.util.regex.Pattern


val Context.screenWidth: Int
    get() {
        return applicationContext.resources.displayMetrics.widthPixels
    }

fun Context.dp2x(dp: Float): Int {
    val scale = applicationContext.resources.displayMetrics.density
    return (dp * scale + .5f).toInt()
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
