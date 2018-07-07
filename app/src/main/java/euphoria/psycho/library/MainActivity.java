package euphoria.psycho.library;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.SpannableStringBuilder;
import android.text.style.BackgroundColorSpan;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.*;

import java.io.File;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends Activity implements ReaderView.SelectListener, Translator.AsyncTaskListener {

    private static final String KEY_FONTSIZE = "fontsize";
    private static final String KEY_LANGUAGE = "is_chinese";
    private static final String KEY_LINESPACINGMULTIPLIER = "lineSpacingMultiplier";
    private static final String KEY_PADDING = "padding";
    private static final String KEY_PATTERN = "pattern";
    private static final int REQUEST_BOOK_CODE = 1;
    private static final int REQUEST_FILE_CODE = 2;
    private static final int REQUEST_PERMISSIONS_CODE = 342;
    private static final String TAG = "MainActivity";
    private int mCount = 1;
    private TextView mDicTextView;
    private boolean mIsChinese = true;
    private ReaderView mReaderView;
    private ScrollView mScrollView;
    private int mSearchCount = -1;
    private List<Integer> mSearchList;
    private SelectPicPopupWindow mSelectPicPopupWindow;
    private SharedPreferences mSharedPreferences;
    private String mTag;
    public static final String KEY_TAG = "tag";
    public static final String KEY_TYPEFACE = "typeface";

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

    private void applySettings() {

        mIsChinese = mSharedPreferences.getBoolean(KEY_LANGUAGE, true);
        String tag = mSharedPreferences.getString("tag", null);
        if (tag == null) return;
        int count = 1;
        int scrollY = 0;

        int[] settings = DataProvider.getInstance().querySettings(tag);

        if (settings.length > 1) {
            count = settings[0];
            scrollY = settings[1];
        }

        renderText(tag, count, scrollY);
        mTag = tag;
        mCount = count;
    }

    private void back() {
        if (mTag == null) return;
        if (mSearchList != null) {
            if (mSearchCount - 1 > -1) {

                renderText(mTag, mSearchList.get(--mSearchCount), 0);
                String p = mSharedPreferences.getString(KEY_PATTERN, null);
                if (p != null) {
                    implementSearchHighLight(p);
                    Toast.makeText(this, String.format("当前页面位置:%d.", mSearchList.get(mSearchCount)), Toast.LENGTH_SHORT).show();
                }
            }
            return;
        }
        if (mCount - 1 > 0) {
            --mCount;
            renderText(mTag, mCount, 0);
        }
    }

    private void forward() {
        if (mTag == null) return;
        if (mSearchList != null) {
            if (mSearchCount + 1 < mSearchList.size()) {

                renderText(mTag, mSearchList.get(++mSearchCount), 0);
                String p = mSharedPreferences.getString(KEY_PATTERN, null);
                if (p != null) {
                    implementSearchHighLight(p);
                    Toast.makeText(this, String.format("当前页面位置:%d.", mSearchList.get(mSearchCount)), Toast.LENGTH_SHORT).show();
                }
            }
            return;
        }
        ++mCount;
        renderText(mTag, mCount, 0);
    }

    private void implementSearchHighLight(String pattern) {
        Pattern p = Pattern.compile(pattern);

        String value = mReaderView.getText().toString();

        SpannableStringBuilder linkifiedText = new SpannableStringBuilder(value);


        Matcher matcher = p.matcher(value);
        int offset = 0;
        while (matcher.find()) {
            if (offset == 0)
                offset = matcher.start();
            int start = matcher.start();
            int end = matcher.end();
            BackgroundColorSpan span = new BackgroundColorSpan(Color.YELLOW);
            linkifiedText.setSpan(span, start, end, 0);
        }
        mReaderView.setText(linkifiedText);
        if (offset > 0) {
            bringPointIntoView(mReaderView, mScrollView, offset);
        }

    }

    private void implementSearchInBooks(String pattern) {
        Pattern p = Pattern.compile(pattern);
        mSearchList = DataProvider.getInstance().queryMatchesContent(mTag, p);

    }

    private void initialize() {

        DataProvider.getInstance(this);
        mDicTextView = findViewById(R.id.dicTextView);
        mReaderView = findViewById(R.id.readerView);
        mReaderView.setSelectListener(this);
        mScrollView = findViewById(R.id.scrollView);

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
                    int[] settings = DataProvider.getInstance().querySettings(mTag);

                    if (settings.length > 1) {
                        renderText(mTag, settings[0], settings[1]);
                        mCount = settings[0];
                    } else {
                        renderText(mTag, mCount, 0);

                    }
                }
                return true;
            }
        });
        findViewById(R.id.menu).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                showMenus();
            }
        });
        applyReaderViewSetting();
        applySettings();
    }

    private void menuChangeDictionary() {
        mIsChinese = !mIsChinese;

        mSharedPreferences.edit().putBoolean(KEY_LANGUAGE, mIsChinese).commit();

    }

    private void menuFontSizeDecrease() {

        float fontSize = mSharedPreferences.getFloat(KEY_FONTSIZE, 0f);
        if (fontSize == 0f || fontSize - 0.5f < 0f) return;
        fontSize = fontSize - 0.5f;
        mSharedPreferences.edit().putFloat(KEY_FONTSIZE, fontSize).commit();
        mReaderView.setTextSize(fontSize * getResources().getDisplayMetrics().scaledDensity);
    }

    private void menuFontSizeIncrease() {
        float fontSize = mSharedPreferences.getFloat(KEY_FONTSIZE, 0f);

        fontSize = fontSize + 0.5f;
        mSharedPreferences.edit().putFloat(KEY_FONTSIZE, fontSize).commit();
        mReaderView.setTextSize(fontSize * getResources().getDisplayMetrics().scaledDensity);
    }

    private void menuJumpTo() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        final EditText editText = new EditText(this);
        builder.setView(editText);
        builder.setTitle("1-" + DataProvider.getInstance().queryCount(mTag));
        builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        });
        editText.setText(Integer.toString(mCount));
        builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

                CharSequence c = editText.getText();
                if (c == null || mTag == null) return;
                int count = str2int(c.toString());
                if (count > 0) {
                    renderText(mTag, count, 0);
                    mCount = count;
                }
            }
        });
        builder.show();
    }

    private void menuOpenFileList() {
        Intent intent = new Intent(this, FileActivity.class);
        startActivityForResult(intent, REQUEST_FILE_CODE);
    }

    private void menuSetFontSize() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        final EditText editText = new EditText(this);
        builder.setView(editText);
        builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        });
        editText.setText(Float.toString(mSharedPreferences.getFloat(KEY_FONTSIZE, 0f)));
        builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

                CharSequence c = editText.getText();
                if (c == null) return;
                float fontSize = str2float(c.toString());
                if (fontSize > 0) {
                    mSharedPreferences.edit().putFloat(KEY_FONTSIZE, fontSize).commit();
                    mReaderView.setTextSize(fontSize * getResources().getDisplayMetrics().scaledDensity);
                }
            }
        });
        builder.show();
    }

    private void menuSetLineSpace() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        final EditText editText = new EditText(this);
        builder.setView(editText);
        builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        });
        editText.setText(Float.toString(mSharedPreferences.getFloat(KEY_LINESPACINGMULTIPLIER, 0f)));
        builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

                CharSequence c = editText.getText();
                if (c == null) return;
                float fontSize = str2float(c.toString());
                if (fontSize > 0) {
                    mSharedPreferences.edit().putFloat(KEY_LINESPACINGMULTIPLIER, fontSize).commit();
                    mReaderView.setLineSpacing(0f, fontSize);
                }
            }
        });
        builder.show();
    }

    private void menuSetPadding() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        final EditText editText = new EditText(this);
        builder.setView(editText);
        builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        });
        editText.setText(Integer.toString(mSharedPreferences.getInt(KEY_PADDING, 0)));
        builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

                CharSequence c = editText.getText();
                if (c == null || mTag == null) return;
                int count = str2int(c.toString());
                if (count > 0) {
                    mReaderView.setPadding(count, count, count, count);
                    mSharedPreferences.edit().putInt(KEY_PADDING, count).commit();
                }
            }
        });
        builder.show();
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
        final String pattern = mSharedPreferences.getString(KEY_PATTERN, null);
        if (pattern != null) {
            editText.setText(pattern);
        }
        builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                if (editText.getText() == null) return;
                String matchPattern = editText.getText().toString().trim();
                try {
                    implementSearchInBooks(matchPattern);
                    mSharedPreferences.edit().putString(KEY_PATTERN, matchPattern).commit();
                    if (mSearchList != null) {
                        DataProvider.getInstance().updateSettings(mTag, mCount, mScrollView.getScrollY());
                        forward();
                    }
                } catch (Exception e) {
                    Log.e(TAG, e.getMessage());
                }
            }
        });
        builder.show();
    }

    private void showMenus() {

        Resources resources = getResources();
        mSelectPicPopupWindow = new SelectPicPopupWindow(this,
                new int[]{
                        R.mipmap.ic_arrow_forward_black_18dp,
                        R.mipmap.ic_format_size_black_18dp,
                        R.mipmap.ic_apps_black_18dp,
                        R.mipmap.ic_format_size_black_18dp,
                        R.mipmap.ic_format_size_black_18dp,
                        R.mipmap.ic_format_size_black_18dp,
                        R.mipmap.ic_format_size_black_18dp,
                        R.mipmap.ic_format_size_black_18dp,

                },
                new String[]{
                        resources.getString(R.string.menu_jump_to),
                        resources.getString(R.string.menu_font_size),
                        resources.getString(R.string.menu_file_list),
                        resources.getString(R.string.menu_font_decrease),
                        resources.getString(R.string.menu_font_increase),
                        resources.getString(R.string.menu_change_dictionary),
                        resources.getString(R.string.menu_set_padding),
                        resources.getString(R.string.menu_set_line)

                },
                new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                        mSelectPicPopupWindow.dismiss();
                        if (i == 0) {
                            menuJumpTo();
                        } else if (i == 1) {
                            menuSetFontSize();
                        } else if (i == 2) {
                            menuOpenFileList();
                        } else if (i == 3) {
                            menuFontSizeDecrease();
                        } else if (i == 4) {
                            menuFontSizeIncrease();
                        } else if (i == 5) {
                            menuChangeDictionary();
                        } else if (i == 6) {
                            menuSetPadding();
                        } else if (i == 7) {
                            menuSetLineSpace();
                        }
                    }
                });
//显示窗口,设置layout在PopupWindow中显示的位置
        mSelectPicPopupWindow.showAtLocation(this.findViewById(R.id.layout), Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, 0);

        //为弹出窗口实现监听类
    }

    public static void bringPointIntoView(TextView textView,
                                          final ScrollView scrollView, int offset) {
        float line = textView.getLayout().getLineForOffset(offset);
        final int y = (int) ((line + 0.5) * textView.getLineHeight());
        scrollView.post(new Runnable() {
            @Override
            public void run() {
                scrollView.scrollTo(0, y - scrollView.getHeight() / 2);
            }
        });
        //scrollView.smoothScrollTo(0, y - scrollView.getHeight() / 2);
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

    @Override
    public void onSelectionChange(String value) {
        if (mIsChinese)
            Translator.getInstance(this, this).addRequestQueue(value.trim());
        else
            TranslatorMerriam.getInstance(this, this).addRequestQueue(value.trim());
    }

    @Override
    public void onClick() {
        mDicTextView.setText(null);
    }

    public float str2float(String s) {

        Pattern pattern = Pattern.compile("[0-9\\.]+");
        Matcher matcher = pattern.matcher(s);
        if (matcher.find()) {
            return Float.parseFloat(matcher.group());
        }
        return -1f;
    }

    public int str2int(String s) {

        Pattern pattern = Pattern.compile("[0-9]+");
        Matcher matcher = pattern.matcher(s);
        if (matcher.find()) {
            return Integer.parseInt(matcher.group());
        }
        return -1;
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
            if (mSearchList != null) {
                mSearchList = null;
                mSearchCount = -1;
            }
            String tag = data.getStringExtra(KEY_TAG);
            int[] settings = DataProvider.getInstance().querySettings(tag);
            int y = 0;
            int count = 1;
            if (settings.length > 1) {
                count = settings[0];
                y = settings[1];
            }
            mTag = tag;
            mCount = count;
            if (count < 1) count = 1;
            renderText(tag, count, y);
        }
        super.onActivityResult(requestCode, resultCode, data);

    }
}
