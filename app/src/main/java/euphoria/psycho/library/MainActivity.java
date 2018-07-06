package euphoria.psycho.library;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;

import java.io.File;

public class MainActivity extends Activity implements ReaderView.SelectListener {

    private static final String KEY_FONTSIZE = "fontsize";
    private static final String KEY_LINESPACINGMULTIPLIER = "lineSpacingMultiplier";
    private static final String KEY_PADDING = "padding";
    private static final String KEY_TYPEFACE = "typeface";
    private static final int REQUEST_BOOK_CODE = 1;
    private static final int REQUEST_PERMISSIONS_CODE = 342;
    private int mCount = 1;
    private ReaderView mReaderView;
    private SharedPreferences mSharedPreferences;
    private View mShowBookList;
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
        mReaderView = findViewById(R.id.readerView);
        mReaderView.setSelectListener(this);
        applyReaderViewSetting();

        mShowBookList = findViewById(R.id.showBooklist);

        mShowBookList.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openBookListActivity();
            }
        });

    }

    private void openBookListActivity() {
        Intent intent = new Intent(this, BookListActivity.class);
        startActivityForResult(intent, REQUEST_BOOK_CODE);
    }

    @Override
    public void onSelectionChange(String value) {

    }

    @Override
    public void onClick() {

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
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        initialize();
    }


    private void renderText(String s, int count, int y) {
        mReaderView.setText(DataProvider.getInstance().queryContent(s, count));
        mCount = count;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_BOOK_CODE && resultCode == RESULT_OK) {
            String tag = data.getStringExtra(KEY_TAG);

            renderText(tag, 1, 0);
        }
        super.onActivityResult(requestCode, resultCode, data);

    }
}
