package euphoria.psycho.library;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.File;
import java.util.List;
import java.util.regex.Pattern;

public class MainActivity extends Activity implements ReaderView.SelectListener, Translator.AsyncTaskListener {

    private static final String KEY_FONTSIZE = "fontsize";
    private static final String KEY_LINESPACINGMULTIPLIER = "lineSpacingMultiplier";
    private static final String KEY_PADDING = "padding";
    private static final String KEY_TYPEFACE = "typeface";
    private static final String KEY_PATTERN = "pattern";

    private static final int REQUEST_BOOK_CODE = 1;
    private static final int REQUEST_PERMISSIONS_CODE = 342;
    private int mCount = 1;
    private int mSearchCount = -1;
    private TextView mDicTextView;
    private boolean mIsChinese = true;
    private ReaderView mReaderView;
    private ScrollView mScrollView;
    private SharedPreferences mSharedPreferences;
    private List<Integer> mSearchList;
    private String mTag;
    public static final String KEY_TAG = "tag";

    private void applyReaderViewSetting() {
        if (mSharedPreferences == null)
            mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);


        String typeface = mSharedPreferences.getString(KEY_TYPEFACE, null);
        if (typeface != null) {
            File typefaceFile = new File(typeface);
            if (typefaceFile.isFile()) {
                mReaderView.setTypeface(Typeface.createFromFile(typeface));
            }
        }


        float fontsize = mSharedPreferences.getFloat(KEY_FONTSIZE, 0f);
        if (fontsize > 0f) {
            mReaderView.setTextSize(fontsize * getResources().getDisplayMetrics().scaledDensity);
        }
        int padding = mSharedPreferences.getInt(KEY_PADDING, 0);
        if (padding > 0) {
            mReaderView.setPadding(padding, padding, padding, padding);
        }

        float lineSpacingMultiplier = mSharedPreferences.getFloat(KEY_LINESPACINGMULTIPLIER, 0f);
        if (lineSpacingMultiplier > 0) {
            mReaderView.setLineSpacing(0f, lineSpacingMultiplier);
        }
    }

    private void initialize() {

        DataProvider.getInstance(this);
        mDicTextView = findViewById(R.id.dicTextView);
        mReaderView = findViewById(R.id.readerView);
        mReaderView.setSelectListener(this);
        mScrollView = findViewById(R.id.scrollView);
        applyReaderViewSetting();

        findViewById(R.id.forward).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                forward();
            }
        });

        findViewById(R.id.back).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                back();
            }
        });

        findViewById(R.id.showBooklist).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openBookListActivity();
            }
        });

        findViewById(R.id.search).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mSearchList == null)
                    searchInBooks();
            }
        });
        findViewById(R.id.search).setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                mSearchList = null;
                mSearchCount = -1;
                if (mTag != null) {
                    renderText(mTag, mCount, 0);
                }
                return true;
            }
        });
    }

    private void openBookListActivity() {
        Intent intent = new Intent(this, BookListActivity.class);
        startActivityForResult(intent, REQUEST_BOOK_CODE);
    }

    private void renderText(String s, int count, final int y) {
        mReaderView.setText(DataProvider.getInstance().queryContent(s, count));

        if (y > -1) {
            mScrollView.post(new Runnable() {
                @Override
                public void run() {
                    mScrollView.scrollTo(0, y);
                }
            });
        }
    }

    @Override
    public void onPostExecute(String value) {
        mDicTextView.setText(value);
    }

    @Override
    public void onPreExecute() {

    }

    @Override
    public void onError(String value) {

    }

    private void forward() {
        if (mTag == null) return;
        if (mSearchList != null) {
            if (mSearchCount + 1 < mSearchList.size()) {

                renderText(mTag, mSearchList.get(++mSearchCount), 0);
            }
            return;
        }
        ++mCount;
        renderText(mTag, mCount, 0);
    }

    private void back() {
        if (mTag == null) return;
        if (mSearchList != null) {
            if (mSearchCount - 1 > -1) {

                renderText(mTag, mSearchList.get(--mSearchCount), 0);
            }
            return;
        }
        if (mCount - 1 > 0) {
            --mCount;
            renderText(mTag, mCount, 0);
        }
    }

    @Override
    public void onSelectionChange(String value) {
        if (mIsChinese)
            Translator.getInstance(this, this).addRequestQueue(value.trim());
        else
            TranslatorMerriam.getInstance(this, this).addRequestQueue(value.trim());
    }

    private void searchInBooks() {
        if (mTag == null) return;
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        final EditText editText = new EditText(this);
        builder.setView(editText);
        builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        });
        String pattern = mSharedPreferences.getString(KEY_PATTERN, null);
        if (pattern != null) {
            editText.setText(pattern);
        }
        builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                if (editText.getText() == null) return;
                String matchPattern = editText.getText().toString().trim();

                implementSearchInBooks(matchPattern);
            }
        });
        builder.show();
    }

    private void implementSearchInBooks(String pattern) {
        Pattern p = Pattern.compile(pattern);
        mSearchList = DataProvider.getInstance().queryMatchesContent(mTag, p);

    }

    @Override
    public void onClick() {
        mDicTextView.setText(null);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.INTERNET}
                    , REQUEST_PERMISSIONS_CODE);
        } else {
            initialize();

        }
    }

    @Override
    protected void onPause() {
        if (mTag != null) {
            mSharedPreferences.edit().putString("tag", mTag).putInt("count", mCount).commit();
            int y = mScrollView.getScrollY();
            if (y > 0)
                DataProvider.getInstance().updateSettings(mTag, mCount, y);
        }
        super.onPause();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        initialize();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_BOOK_CODE && resultCode == RESULT_OK) {
            String tag = data.getStringExtra(KEY_TAG);
            mTag = tag;
            mCount = 1;
            renderText(tag, 1, 0);
        }
        super.onActivityResult(requestCode, resultCode, data);

    }
}
