package euphoria.psycho.library
import android.net.Uri
import android.os.AsyncTask
import android.text.TextUtils
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit
class DictionaryTask(val listener: (v: String?) -> Unit) : AsyncTask<String, Void, String>() {
    companion object {
        private const val USER_AGENT = "Mozilla/5.0 (Windows NT 6.1; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/60.0.3112.90 Safari/537.36"
        private const val ACCEPT = "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8"
        private const val CHINESE_PATTERN = "[\u4E00-\u9FA5]"
    }
    override fun doInBackground(vararg query: String?): String? {
        val word = query.get(0) ?: return null
        var r = Regex(CHINESE_PATTERN)
        var m = r.find(word);
        if (m != null && m.groups.size > 0) {
            return queryFromBing(word)
        } else {
            return queryFromYouDao(word)
        }
    }
    override fun onPostExecute(result: String?) {
        listener(result)
    }
    fun queryFromBing(word: String): String? {
        var result = DictionaryProvider.instance.query(word)
        if (result != null) return "$word $result"
        val url = "https://cn.bing.com/dict/search?q=${Uri.encode(word)}"
        val okHttpClient = OkHttpClient
                .Builder()
                .connectTimeout(3, TimeUnit.SECONDS)
                .readTimeout(5, TimeUnit.SECONDS)
                .build()
        val req = Request.Builder()
                .url(url)
                .addHeader("accept", ACCEPT)
                .addHeader("user-agent", USER_AGENT)
                .build()
        val res = okHttpClient.newCall(req).execute()
        if (res.isSuccessful) {
            val str = res.body()?.string()
            val v = YoudaoUtils.extractEnglishFromBing(str)
            if (TextUtils.isEmpty(v)) return word
            else {
                DictionaryProvider.instance.insert(word, v)
                return "$word $v"
            }
        } else return null
    }
    fun queryFromYouDao(word: String): String? {
        var result = DictionaryProvider.instance.query(word)
        if (result != null) return "$word $result"
        val url = YoudaoUtils.generateRequestUrl(word)
        val okHttpClient = OkHttpClient
                .Builder()
                .connectTimeout(3, TimeUnit.SECONDS)
                .readTimeout(5, TimeUnit.SECONDS)
                .build()
        val req = Request.Builder()
                .url(url)
                .addHeader("accept", ACCEPT)
                .addHeader("user-agent", USER_AGENT)
                .build()
        val res = okHttpClient.newCall(req).execute()
        if (res.isSuccessful) {
            val str = res.body()?.string()
            val v = YoudaoUtils.extractJSON(JSONObject(str), word)
            if (TextUtils.isEmpty(v)) return word
            else {
                DictionaryProvider.instance.insert(word, v)
                return "$word $v"
            }
        } else return null
    }
}