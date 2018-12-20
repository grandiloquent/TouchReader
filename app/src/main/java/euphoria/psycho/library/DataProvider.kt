package euphoria.psycho.library

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.os.Environment
import java.io.File

class DataProvider(context: Context = App.instance) : SQLiteOpenHelper(
        context, File(File(Environment.getExternalStorageDirectory(), ".readings"), "datas.db").absolutePath,
        null,
        DataProvider.DATABASE_VERSION
) {
    fun importFile(file: File) {
        val tag = file.nameWithoutExtension
        val list = file.readLines()
        var sb = StringBuilder()
        val length = 8000
        var count = 0;
        for (line in list) {
            if (line.isNotBlank())
                sb.append(line).append('\n')
            if (sb.length > length) {
                insert(tag, sb.toString(), ++count)
                sb = StringBuilder()
            }
        }
        if (sb.isNotEmpty()) {
            insert(tag, sb.toString(), ++count)
        }
    }

    fun insert(tag: String, content: String, count: Int) {
        val v = ContentValues()
        v.put("tag", tag)
        v.put("content", content)
        v.put("count", count)
        writableDatabase.insert("document", null, v)
    }

    fun addFromClipboard(tag: String, context: String) {
        val cursor = readableDatabase.query("document", arrayOf("count"), "tag=?", arrayOf(tag), null, null, "count DESC")
        var count = 1
        if (cursor.moveToNext()) {
            count = cursor.getInt(0) + 1
        }
        cursor.close()
        val contentValues = ContentValues()
        contentValues.put("tag", tag)
        contentValues.put("count", count)
        contentValues.put("content", context)
        writableDatabase.insert("document", null, contentValues)
    }

    fun queryMatchesContent(tag: String, pattern: String): List<Int> {
        val cursor = readableDatabase.rawQuery("SELECT count,content FROM document WHERE tag = ?", arrayOf(tag));
        val list = ArrayList<Int>()
        var regex = Regex(pattern)
        while (cursor.moveToNext()) {
            if (regex.containsMatchIn(cursor.getString(1))) {
                list.add(cursor.getInt(0))
            }
        }
        cursor.close()
        return list
    }

    fun listTag(): ArrayList<String> {
        val cursor = readableDatabase.rawQuery("SELECT DISTINCT tag FROM document ORDER BY tag", null);
        val list = ArrayList<String>();
        while (cursor.moveToNext()) {
            list.add(cursor.getString(0));
        }
        cursor.close();
        return list;
    }

    fun queryCount(tag: String): Int {
        val cursor = readableDatabase.query("document", arrayOf("count"), "tag=?", arrayOf(tag), null, null, "count DESC");
        var count = 0;
        if (cursor.moveToNext()) {
            count = cursor.getInt(0);
        }
        cursor.close()
        return count;
    }

    fun insertArticle(context: String) {
        val cursor = readableDatabase.query("document", arrayOf("count"), "tag=?", arrayOf("Stories From Clipboard"), null, null, "count DESC");
        var count = 1;
        if (cursor.moveToNext()) {
            count = cursor.getInt(0) + 1;
        }
        cursor.close();
        val contentValues = ContentValues();
        contentValues.put("tag", "Stories From Clipboard");
        contentValues.put("count", count);
        contentValues.put("content", context);
        writableDatabase.insert("document", null, contentValues);
    }

    fun queryContent(tag: String, count: Int): String {
        val cursor = readableDatabase.rawQuery("SELECT content FROM document WHERE tag = ? AND count = ?", arrayOf(tag, Integer.toString(count)))
        var result = ""
        if (cursor.moveToNext())
            result = cursor.getString(0)
        cursor.close()
        return result
    }

    fun updateSettings(tag: String, count: Int, scrollY: Int) {
        val v = ContentValues()
        v.put("tag", tag)
        v.put("count", count)
        v.put("scrollY", scrollY)
        writableDatabase.insertWithOnConflict("settings", null, v, SQLiteDatabase.CONFLICT_REPLACE);
    }

    fun querySettings(tag: String): Pair<Int, Int> {
        var count = 1
        var y = 0
        val cursor = readableDatabase.rawQuery("SELECT count,scrollY FROM settings WHERE tag = ?", arrayOf(tag));
        if (cursor.moveToNext()) {
            count = cursor.getInt(0)
            y = cursor.getInt(1)
        }
        cursor.close()
        return count to y
    }

    fun updateDocument(tag: String, count: Int, content: String) {
        val values = ContentValues()

        values.put("content", content)
        writableDatabase.update("document", values, "tag=? and count=?", arrayOf(tag, count.toString()));
    }

    fun updateTag(tag: String, newTag: String) {
        val values = ContentValues()
        values.put("tag", newTag)
        writableDatabase.update("document", values, "tag=?", arrayOf(tag));
    }

    fun deleteByTag(tag: String) {
        writableDatabase.execSQL("VACUUM")
        writableDatabase.delete("document", "tag=?", arrayOf(tag))
    }

    override fun onCreate(sqLiteDatabase: SQLiteDatabase?) {

        sqLiteDatabase?.run {
            execSQL("CREATE TABLE IF NOT EXISTS document (tag TEXT ,content TEXT,count INTEGER)");
            execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `idx_tag_count` ON `document` (`tag` ,`count` ASC)");
            execSQL("CREATE TABLE IF NOT EXISTS settings (tag TEXT ,count INTEGER,scrollY INTEGER)");
            execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `idx_tag` ON `settings` (`tag` )");
        }
    }

    override fun onUpgrade(p0: SQLiteDatabase?, p1: Int, p2: Int) {
    }

    companion object {
        private const val DATABASE_VERSION = 1

        private var instance: DataProvider? = null

        fun getInstance() = instance ?: DataProvider().also { instance = it }


    }
}