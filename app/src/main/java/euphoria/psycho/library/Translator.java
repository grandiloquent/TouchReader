package euphoria.psycho.library;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.os.Environment;
import android.text.TextUtils;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.RetryPolicy;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class Translator {
    private final AsyncTaskListener mListener;

    private final RequestQueue mRequestQueue;

    private final DataProvider mDataProvider;
    private static Translator sTranslator;


    public static Translator getInstance(Context context, AsyncTaskListener listener) {
        if (sTranslator == null) {
            sTranslator = new Translator(context, listener);

        }
        return sTranslator;
    }

    public Translator(Context context, AsyncTaskListener listener) {
        mListener = listener;
        mDataProvider = new DataProvider(context);
        mRequestQueue = Volley.newRequestQueue(context);

    }

    public interface AsyncTaskListener {
        void onPostExecute(String value);

        void onPreExecute();

        void onError(String value);
    }

    public void addRequestQueue(String value) {

        String result = mDataProvider.query(value.toLowerCase());
        if (result != null && mListener != null) {
            mListener.onPostExecute(value + "   " + result);
        } else {
//            JsonObjectRequest jsonObjectRequest = buildRequest(value);
//            jsonObjectRequest.setRetryPolicy(new RetryPolicy() {
//                @Override
//                public int getCurrentTimeout() {
//                    return 10000;
//                }
//
//                @Override
//                public int getCurrentRetryCount() {
//                    return 2;
//                }
//
//                @Override
//                public void retry(VolleyError error) throws VolleyError {
//
//                }
//            });
//            mRequestQueue.add(jsonObjectRequest);

            StringRequest request = buildStringRequest(value);
            request.setRetryPolicy(new RetryPolicy() {
                @Override
                public int getCurrentTimeout() {
                    return 3000;
                }

                @Override
                public int getCurrentRetryCount() {
                    return 2;
                }

                @Override
                public void retry(VolleyError error) throws VolleyError {

                }
            });
            mRequestQueue.add(request);
        }
    }


    private StringRequest buildStringRequest(final String query) {
        return new StringRequest(Request.Method.GET, "https://cn.bing.com/dict/search?q=" + Uri.encode(query), new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                String strValue = YoudaoUtils.extractEnglishFromBing(response);

                if (!TextUtils.isEmpty(strValue)) {

                    mDataProvider.insert(query.toLowerCase(), strValue);
                    mListener.onPostExecute(query + " " + strValue);
                } else {
                    mListener.onPostExecute(query);
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

    // QUERY_ADRESS + Uri.encode(value)
    private JsonObjectRequest buildRequest(final String value) {
        return new JsonObjectRequest
                (Request.Method.GET, YoudaoUtils.generateRequestUrl(value), null, new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(JSONObject response) {
                        String strValue = YoudaoUtils.extractJSON(response, value);

                        if (!TextUtils.isEmpty(strValue)) {

                            mDataProvider.insert(value.toLowerCase(), strValue);
                            mListener.onPostExecute(value + " " + strValue);
                        } else {
                            mListener.onPostExecute(value);
                        }
//                        String strValue = parseJson(response);
//                        if (strValue.matches("^[a-zA-Z\n']+$")) {
//                            mListener.onPostExecute(value);
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

    private static final String QUERY_ADRESS = "https://translate.google.cn/translate_a/single?client=gtx&sl=en&tl=zh&dt=t&dt=bd&ie=UTF-8&oe=UTF-8&dj=1&source=icon&q=";


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

    private String query(String query) {

        try {
            URL url = new URL(QUERY_ADRESS + Uri.encode(query));
            final HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            connection.setConnectTimeout(3000);
            connection.setReadTimeout(5000);
            connection.setRequestMethod("GET");

            // connection.setUseCaches(false);
            // connection.setDoOutput(true);
            // connection.setInstanceFollowRedirects(true);
            connection.connect();
            BufferedReader br = new BufferedReader(new InputStreamReader(url.openStream()));
            StringBuilder sb = new StringBuilder();

            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line + "\n");
            }
            br.close();

            return sb.toString();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private class DataProvider extends SQLiteOpenHelper {


        public DataProvider(Context context) {

            super(context, new File(new File(Environment.getExternalStorageDirectory(), ".readings"), "psycho.db").getAbsolutePath(),
                    null, 1);
        }

        @Override
        public void onCreate(SQLiteDatabase sqLiteDatabase) {
            sqLiteDatabase.execSQL("CREATE TABLE \"dic\" ( \"key\" varchar , \"word\" varchar, \"learned\" INTEGER)");
            sqLiteDatabase.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `key_UNIQUE` ON `dic` (`key` ASC)");
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

        public void insert(String key, String value) {
            ContentValues contentValues = new ContentValues();
            contentValues.put("key", key);
            contentValues.put("word", value);
            getWritableDatabase().insert("dic", null, contentValues);
        }

        @Override
        public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {

        }
    }
}
