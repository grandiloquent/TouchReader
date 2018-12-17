package euphoria.psycho.library

import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import org.jsoup.nodes.Node
import org.jsoup.nodes.TextNode
import org.jsoup.select.NodeTraversor
import org.jsoup.select.NodeVisitor
import java.io.File
import java.util.concurrent.TimeUnit

private const val USER_AGENT = "Mozilla/5.0 (Windows NT 6.1; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/60.0.3112.90 Safari/537.36"
private const val ACCEPT = "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8"


fun File.combineSafariBookDirectory() {
    if (!this.isDirectory) return
    val tocFile = File(this, "目录.html")
    if (!tocFile.isFile) return
    val hd = Jsoup.parse(tocFile.readText())
    val links = hd.select("a")
    val linksList = ArrayList<String>()
    if (links.isNotEmpty()) {
        for (l in links) {
            val fileName = l.attr("href").substringBefore('#')
            if (!linksList.contains(fileName))
                linksList.add(fileName)
        }
    }
    val sb = StringBuilder()
    sb.append("<!DOCTYPE html> <html lang=\"en\"> <head> <meta charset=\"utf-8\"> <meta content=\"IE=edge\" http-equiv=\"X-UA-Compatible\"> <meta content=\"width=device-width,initial-scale=1\" name=\"viewport\"><link href=\"style.css\" rel=\"stylesheet\"></head>")
    for (f in linksList) {
        val filePath = File(this, f)
        if (filePath.isFile)
            sb.append(Jsoup.parse(filePath.readText()).body().html())
    }
    sb.append("</body></html>")
}

fun String.htm2txt(special: Boolean = false): String {

    val d = Jsoup.parse(this)
    val f = FormattingVisitor()
    if (special) {
        NodeTraversor(f).traverse(d?.body()?.select(".b-story-body-x")?.first())
        return (d?.body()?.select(".b-story-header h1")?.first()?.text()
                ?: "") + "\n" + f.toString()
    } else {
        NodeTraversor(f).traverse(d?.body())
        return f.toString()
    }

}

fun String.fetchString(special: Boolean = false): String? {

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
        return res.body()?.string()?.htm2txt(special)
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