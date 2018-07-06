package euphoria.psycho.library;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Environment;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class DataProvider extends SQLiteOpenHelper {

    private static DataProvider sDataProvider;

    public static DataProvider getInstance(Context context) {
        if (sDataProvider == null) {
            sDataProvider = new DataProvider(context);
        }
        return sDataProvider;
    }

    public static DataProvider getInstance() {

        return sDataProvider;
    }

    public List<String> listTag() {

        Cursor cursor = getReadableDatabase().rawQuery("SELECT DISTINCT tag FROM document ORDER BY tag", null);

        List<String> list = new ArrayList<>();

        while (cursor.moveToNext()) {
            list.add(cursor.getString(0));
        }
        cursor.close();
        return list;
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        sqLiteDatabase.execSQL("CREATE TABLE IF NOT EXISTS document (tag TEXT ,content TEXT,count INTEGER)");
        sqLiteDatabase.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `idx_tag_count` ON `document` (`tag` ,`count` ASC)");
        sqLiteDatabase.execSQL("CREATE TABLE IF NOT EXISTS settings (tag TEXT ,count INTEGER,scrollY INTEGER)");
        sqLiteDatabase.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `idx_tag` ON `settings` (`tag` )");

    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {

    }

    public String queryContent(String tag, int count) {
        Cursor cursor = getReadableDatabase().rawQuery("SELECT content FROM document WHERE tag = ? AND count = ?", new String[]{tag, Integer.toString(count)});

        String result = "";

        if (cursor.moveToNext()) {
            result = cursor.getString(0);
        }
        cursor.close();
        return result;
    }

    public void updateSettings(String tag, int count, int scrollY) {

        ContentValues initialValues = new ContentValues();
        initialValues.put("tag", tag); // the execution is different if _id is 2
        initialValues.put("count", count);
        initialValues.put("scrollY", scrollY);

        int id = (int) getWritableDatabase().insertWithOnConflict("settings", null, initialValues, SQLiteDatabase.CONFLICT_IGNORE);
        if (id == -1) {
            getWritableDatabase().update("settings", initialValues, "tag=?", new String[]{tag});  // number 1 is the _id here, update to variable for your code
        }
    }

    public DataProvider(Context context) {

        super(context, new File(new File(Environment.getExternalStorageDirectory(), ".readings"), "datas.db").getAbsolutePath(),
                null, 1);
    }
}