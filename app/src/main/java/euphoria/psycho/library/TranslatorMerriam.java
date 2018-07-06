package euphoria.psycho.library;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.os.Environment;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import org.json.JSONArray;
import org.json.JSONObject;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

public class TranslatorMerriam {


    private static final String QUERY_ADRESS = "http://www.dictionaryapi.com/api/v1/references/learners/xml/";
    private final DataProvider mDataProvider;

    ;
    private final Translator.AsyncTaskListener mListener;

    private final RequestQueue mRequestQueue;
    private static TranslatorMerriam sTranslator;

    private JsonObjectRequest buildJsonRequest(final String value) {
        return new JsonObjectRequest
                (Request.Method.GET, "https://translate.google.cn/translate_a/single?client=gtx&sl=en&tl=zh&dt=t&dt=bd&ie=UTF-8&oe=UTF-8&dj=1&source=icon&q=" + Uri.encode(value), null, new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(JSONObject response) {
                        String strValue = parseJson(response);
                        if (strValue.matches("^[a-zA-Z\n']+$")) {
                            mListener.onPostExecute(value);
                            return;
                        }
                        if (strValue != null && strValue.trim().length() > 0) {
                            mDataProvider.insert(value.toLowerCase(), strValue);
                        }
                        if (mListener != null) {
                            mListener.onPostExecute(value + "   " + strValue);
                        }

                    }
                }, new Response.ErrorListener() {

                    @Override
                    public void onErrorResponse(VolleyError error) {

                        if (mListener != null) {
                            mListener.onError(error.toString());
                        }
                    }
                }) {
            @Override
            public Map<String, String> getHeaders() {
                HashMap<String, String> headers = new HashMap<String, String>();
                headers.put("accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8");
                headers.put("user-agent", "Mozilla/5.0 (Windows NT 6.1; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/60.0.3112.90 Safari/537.36");
                return headers;
            }
        };

    }

    private StringRequest buildRequest(final String value) {

        // QUERY_ADRESS + Uri.encode(value) + "?key=eec2a287-c274-4066-b4c2-3f6cc014d77f"
        return new StringRequest
                (Request.Method.GET, "https://cn.bing.com/dict/search?q=" + value, new Response.Listener<String>() {

                    @Override
                    public void onResponse(String response) {
                        String strValue = YoudaoUtils.extractEnglishFromBing(response);


                        if (strValue != null && strValue.length() > 0) {
                            mDataProvider.insert(value.toLowerCase(), strValue);
                        }
                        if (mListener != null) {
                            mListener.onPostExecute(value + "   " + strValue);
                        }
//
//                        if (response == null || response.length() < 1) {
//                            mListener.onPostExecute(value);
//                            return;
//                        }
//                        String strValue = parseXMLBySAXParser(response);
//
//                        if (strValue == null || strValue.length() < 1) {
//                            mRequestQueue.add(buildJsonRequest(value));
//                            return;
//                        }
//                        if (strValue != null && strValue.trim().length() > 0) {
//                            mDataProvider.insert(value.toLowerCase(), strValue);
//                        }
//                        if (mListener != null) {
//                            mListener.onPostExecute(value + "   " + strValue);
//                        }

                    }
                }, new Response.ErrorListener() {

                    @Override
                    public void onErrorResponse(VolleyError error) {

                        if (mListener != null) {
                            mListener.onError(error.toString());
                        }
                    }
                }) {
            @Override
            public Map<String, String> getHeaders() {
                HashMap<String, String> headers = new HashMap<String, String>();
                headers.put("accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8");
                headers.put("user-agent", "Mozilla/5.0 (Windows NT 6.1; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/60.0.3112.90 Safari/537.36");
                return headers;
            }
        };

    }

    private String parseJson(JSONObject jsonObject) {

        StringBuilder sb = new StringBuilder();

        try {
            JSONArray dictArray = jsonObject.getJSONArray("dict");
            if (dictArray != null || dictArray.length() > 0) {
                JSONArray termsArray = dictArray.getJSONObject(0).getJSONArray("terms");

                for (int i = 0; i < termsArray.length(); i++) {
                    sb.append(termsArray.getString(i)).append(';');
                }

            }
        } catch (Exception e) {

        }
        try {
            JSONArray sentencesArray = jsonObject.getJSONArray("sentences");

            if (sentencesArray != null || sentencesArray.length() > 0) {
                String trans = sentencesArray.getJSONObject(0).getString("trans");
                if (trans != null)
                    sb.append(trans).append('\n');
            }


        } catch (Exception e) {

        }

        return sb.toString();


    }

    private String parseXMLBySAXParser(String value) {
        SAXParserFactory SAXfactory = SAXParserFactory.newInstance();
        try {
            SAXParser saxParser = SAXfactory.newSAXParser();
            InputStream stream = new ByteArrayInputStream(value.trim().getBytes(Charset.forName("utf8")));

            MerriamHandler handler = new MerriamHandler();
            saxParser.parse(stream, handler);

            return handler.toString();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public void addRequestQueue(String value) {

        String result = mDataProvider.query(value.toLowerCase());
        if (result != null && mListener != null) {
            mListener.onPostExecute(value + "   " + result);
        } else {
            mRequestQueue.add(buildRequest(value));
        }
    }

    public static TranslatorMerriam getInstance(Context context, Translator.AsyncTaskListener listener) {
        if (sTranslator == null) {
            sTranslator = new TranslatorMerriam(listener, context);
        }
        return sTranslator;
    }

    private class MerriamHandler extends DefaultHandler {
        private final StringBuilder mStringBuilder = new StringBuilder();
        private boolean mIsDt;

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
            if (qName == "dt") {
                mIsDt = true;
            }
        }

        @Override
        public void characters(char[] ch, int start, int length) throws SAXException {
            if (mIsDt) {

                String value = new String(ch, start, length);
                if (!value.equals(":"))
                    mStringBuilder.append(value);
                mIsDt = false;
            }
        }

        @Override
        public String toString() {
            String value = mStringBuilder.toString();
            value = value.replaceAll(":+", "\n");

            return value.trim();
        }
    }

    public TranslatorMerriam(Translator.AsyncTaskListener listener, Context context) {
        mListener = listener;

        mDataProvider = new DataProvider(context);
        mRequestQueue = Volley.newRequestQueue(context);

    }

    private class DataProvider extends SQLiteOpenHelper {


        public void insert(String key, String value) {
            ContentValues contentValues = new ContentValues();
            contentValues.put("key", key);
            contentValues.put("word", value);
            getWritableDatabase().insertWithOnConflict("dic", null, contentValues, SQLiteDatabase.CONFLICT_IGNORE);
        }

        @Override
        public void onCreate(SQLiteDatabase sqLiteDatabase) {
            sqLiteDatabase.execSQL("CREATE TABLE IF NOT EXISTS dic (key TEXT ,word TEXT)");
            sqLiteDatabase.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `key_UNIQUE` ON `dic` (`key` ASC)");
        }

        @Override
        public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {

        }

        public String query(String key) {

            Cursor cursor = getReadableDatabase().rawQuery("select word from dic where key = ?", new String[]{key});

            String result = null;
            if (cursor.moveToNext()) {
                result = cursor.getString(0);
            }
            cursor.close();
            return result;
        }

        public DataProvider(Context context) {

            super(context, new File(new File(Environment.getExternalStorageDirectory(), ".readings"), "psycho_en.db").getAbsolutePath(),
                    null, 1);
        }
    }
}
