package euphoria.psycho.library

import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import org.jsoup.nodes.Node
import org.jsoup.nodes.TextNode
import org.jsoup.select.NodeTraversor
import org.jsoup.select.NodeVisitor
import java.util.concurrent.TimeUnit

private const val USER_AGENT = "Mozilla/5.0 (Windows NT 6.1; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/60.0.3112.90 Safari/537.36"
private const val ACCEPT = "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8"

fun String.htm2txt(): String {

    val d = Jsoup.parse(this)
    val f = FormattingVisitor()
    NodeTraversor(f).traverse(d?.body())
    return f.toString()
}

fun String.fetchString(): String? {

    val oh = OkHttpClient
            .Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
    val req = Request.Builder()
            .url(this)
            .addHeader("accept", ACCEPT)
            .addHeader("user-agent", USER_AGENT)
            .build()
    val res = oh.newCall(req).execute()
    if (res.isSuccessful) {
        return res.body()?.string()?.htm2txt()
    }
    return null

}

class FormattingVisitor : NodeVisitor {
    override fun tail(node: Node?, depth: Int) {
        if (mT2.contains(node?.nodeName())) {
            mSb.append('\n')
        }
    }

    override fun head(node: Node?, depth: Int) {

        if (node is TextNode) {
            mSb.append(node.text())
        } else if (node?.nodeName() == "li") {
            mSb.append("\n * ");
        } else if (node?.nodeName() == "dt") {
            mSb.append(" ")
        } else if (mT1.contains(node?.nodeName())) {
            mSb.append("\n")
        }

    }

    override fun toString(): String {
        return mSb.toString()
    }

    private val mT1 = arrayOf("p", "h1", "h2", "h3", "h4", "h5", "tr")
    private val mT2 = arrayOf("br", "dd", "dt", "p", "h1", "h2", "h3", "h4", "h5")
    private val mSb = StringBuilder()

}