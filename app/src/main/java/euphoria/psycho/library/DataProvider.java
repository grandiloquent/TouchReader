package euphoria.psycho.library;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Environment;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class DataProvider extends SQLiteOpenHelper {

    private static DataProvider sDataProvider;

    public void addFromClipboard(String tag, String context) {
        Cursor cursor = getReadableDatabase().query("document", new String[]{"count"}, "tag=?", new String[]{tag}, null, null, "count DESC");
        int count = 1;
        if (cursor.moveToNext()) {
            count = cursor.getInt(0) + 1;
        }
        cursor.close();
        ContentValues contentValues = new ContentValues();
        contentValues.put("tag", tag);
        contentValues.put("count", count);
        contentValues.put("content", context);
        getWritableDatabase().insert("document", null, contentValues);
    }

    public void deleteByTag(String tag) {
        //DELETE FROM COMPANY WHERE ID = 7;
        getWritableDatabase().delete("document", "tag=?", new String[]{tag});

    }

    public String exportDocument(String tag) {

        Cursor cursor = getReadableDatabase().rawQuery("SELECT content FROM document WHERE tag = ? ORDER BY count ", new String[]{tag});
        StringBuilder builder = new StringBuilder();
        while (cursor.moveToNext()) {

            // ,count    int i=cursor.getInt(1);
            builder.append(cursor.getString(0)).append("\n\n");
        }

        cursor.close();
        return builder.toString();
    }

    public static DataProvider getInstance(Context context) {
        if (sDataProvider == null) {
            sDataProvider = new DataProvider(context);
        }
        return sDataProvider;
    }

    public static DataProvider getInstance() {

        return sDataProvider;
    }

    public void importDocument(File file) {
        try {

            int threshold = 8000;
            FileInputStream in = new FileInputStream(file);

            InputStreamReader reader = new InputStreamReader(in, Charset.forName("utf8"));
            BufferedReader bufferedReader = new BufferedReader(reader);

            StringBuilder sb = new StringBuilder(threshold);
            String l;
            String tag = FileUtils.getFileNameWithoutExtension(file.getName());
            int count = 0;
            while ((l = bufferedReader.readLine()) != null) {
                if (l.trim().length() > 0)
                    sb.append(l).append("\n\n");
                if (sb.length() > threshold) {

                    insert(tag, sb.toString(), ++count);
                    sb = new StringBuilder(threshold);
                }
            }

            if (sb.length() > 0) {
                insert(tag, sb.toString(), ++count);
            }
            FileUtils.closeSilently(reader);
            FileUtils.closeSilently(bufferedReader);
            FileUtils.closeSilently(in);
//            File dir = new File(Files.getExternalStorageDirectoryPath(".readings"), ".imported");
//            dir.mkdirs();
//            File targetFile = new File(dir, file.getName());
//            if (!targetFile.isFile())
//                file.renameTo(targetFile);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void insert(String tag, String content, int count) {
        ContentValues contentValues = new ContentValues();
        contentValues.put("tag", tag);
        contentValues.put("content", content);
        contentValues.put("count", count);
        getWritableDatabase().insert("document", null, contentValues);
    }

    public void insertArticle(String context) {

        Cursor cursor = getReadableDatabase().query("document", new String[]{"count"}, "tag=?", new String[]{"Stories From Clipboard"}, null, null, "count DESC");
        int count = 1;
        if (cursor.moveToNext()) {
            count = cursor.getInt(0) + 1;
        }
        cursor.close();
        ContentValues contentValues = new ContentValues();
        contentValues.put("tag", "Stories From Clipboard");
        contentValues.put("count", count);
        contentValues.put("content", context);
        getWritableDatabase().insert("document", null, contentValues);
    }

    public int queryCount(String t) {
        Cursor cursor = getReadableDatabase().query("document", new String[]{"count"}, "tag=?", new String[]{t}, null, null, "count DESC");
        int count = 0;
        if (cursor.moveToNext()) {
            count = cursor.getInt(0);
        }
        return count;
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

    public List<Integer> queryMatchesContent(String tag, Pattern pattern) {
        Cursor cursor = getReadableDatabase().rawQuery("SELECT count,content FROM document WHERE tag = ?", new String[]{tag});
        List<Integer> list = new ArrayList<>();


        while (cursor.moveToNext()) {
            if (pattern.matcher(cursor.getString(1)).find()) {
                list.add(cursor.getInt(0));
            }
        }
        cursor.close();
        if (list.size() < 1) return null;
        return list;
    }

    public int[] querySettings(String tag) {
        int[] settings = new int[]{
                1, 0
        };
        Cursor cursor = getReadableDatabase().rawQuery("SELECT count,scrollY FROM settings WHERE tag = ?", new String[]{tag});

        if (cursor.moveToNext()) {
            settings[0] = cursor.getInt(0);
            settings[1] = cursor.getInt(1);
        }
        cursor.close();

        return settings;
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

    public void updateTag(String tag, String newTag) {

        ContentValues initialValues = new ContentValues();
        initialValues.put("tag", newTag); // the execution is different if _id is 2


        getWritableDatabase().update("document", initialValues, "tag=?", new String[]{tag});  // number 1 is the _id here, update to variable for your code
    }

    public DataProvider(Context context) {

        super(context, new File(new File(Environment.getExternalStorageDirectory(), ".readings"), "datas.db").getAbsolutePath(),
                null, 1);
    }
}