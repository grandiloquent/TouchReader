package euphoria.psycho.library
import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.os.Environment
import java.io.File
class DictionaryProvider(context: Context = App.instance) : SQLiteOpenHelper(
        context,
        File(File(Environment.getExternalStorageDirectory(), ".readings"), "psycho.db").absolutePath,
        null,
        DictionaryProvider.DB_VERSION) {
    fun query(key: String?): String? {
        if (key == null) return null
        val cursor = readableDatabase.rawQuery("select word from dic where key = ?", arrayOf(key))
        var result: String? = null
        if (cursor.moveToNext()) {
            result = cursor.getString(0)
        }
        cursor.close()
        return result
    }
    fun insert(key: String, value: String) {
        val contentValues = ContentValues()
        contentValues.put("key", key)
        contentValues.put("word", value)
        writableDatabase.insert("dic", null, contentValues)
    }
    companion object {
        const val DB_VERSION = 1;
        val instance by lazy { DictionaryProvider() }
    }
    override fun onCreate(sqLiteDatabase: SQLiteDatabase?) {
        sqLiteDatabase?.execSQL("CREATE TABLE \"dic\" ( \"key\" varchar , \"word\" varchar, \"learned\" INTEGER)")
        sqLiteDatabase?.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `key_UNIQUE` ON `dic` (`key` ASC)")
    }
    override fun onUpgrade(p0: SQLiteDatabase?, p1: Int, p2: Int) {
    }
}