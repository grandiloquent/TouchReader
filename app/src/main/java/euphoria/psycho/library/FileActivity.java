package euphoria.psycho.library;

import android.app.Activity;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.view.*;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.io.File;
import java.io.FileFilter;
import java.text.Collator;
import java.util.*;
import java.util.regex.Pattern;

public class FileActivity extends Activity implements AdapterView.OnItemClickListener {
    private static final String KEY_DIR = "dir";
    private static final int MENU_CONVERT_TXT = 3;
    private static final int MENU_DELETE = 5;
    private static final int MENU_EXIT = 1;
    private static final int MENU_IMPORT = 6;
    private static final int MENU_SET_FONTFACE = 2;
    private static final String TAG = "BookListActivity";
    private String mCurrentDirectory;
    private FileAdapter mFileAdapter;
    private List<String> mList;
    private ListView mListView;

    private void convert2txt(String path) {
        if (path.endsWith(".epub")) {
            EbookUtils.epub2txt(path);
        }
        refreshListView();
    }

    private List<String> listSupportFiles(String dir) {
        File directory = new File(dir);
        if (!directory.isDirectory()) return new ArrayList<>();
        mCurrentDirectory = dir;
        final Pattern p = Pattern.compile("\\.(?:epub|txt|mobi|ttf)", Pattern.CASE_INSENSITIVE);

        File[] files = directory.listFiles(new FileFilter() {
            @Override
            public boolean accept(File file) {
                if (file.isDirectory() || (file.isFile() && p.matcher(file.getName()).find()))
                    return true;
                return false;
            }
        });
        if (files == null) return new ArrayList<>();

        final Collator collator = Collator.getInstance(Locale.CHINA);
        Arrays.sort(files, new Comparator<File>() {
            @Override
            public int compare(File t1, File t2) {
                if (t1.isDirectory() && t2.isDirectory()) {
                    return collator.compare(t1.getName(), t2.getName());
                } else if (t1.isFile() && t2.isFile()) {
                    return collator.compare(t1.getName(), t2.getName());
                } else if (t1.isDirectory() && t2.isFile()) {
                    return -1;
                } else if (t1.isFile() && t2.isDirectory()) {
                    return 1;
                }
                return 0;
            }
        });

        List<String> list = new ArrayList<>();
        for (File file : files) {
            list.add(file.getName());
        }
        return list;

    }

    private void refreshListView() {
        mFileAdapter.switchData(listSupportFiles(mCurrentDirectory));
    }

    private void setFontface(String path) {
        PreferenceManager.getDefaultSharedPreferences(this).edit().putString("typeface", path).commit();
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        String filename = mFileAdapter.getItem(i);
        File f = new File(mCurrentDirectory, filename);
        if (f.isDirectory()) {
            List<String> files = listSupportFiles(f.getAbsolutePath());
            mFileAdapter.switchData(files);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.booklis_activity);
        String dir = PreferenceManager.getDefaultSharedPreferences(this).getString(KEY_DIR, Environment.getExternalStorageDirectory().getAbsolutePath());

        mList = listSupportFiles(dir);
        mFileAdapter = new FileAdapter();
        mListView = findViewById(R.id.listView);
        mListView.setAdapter(mFileAdapter);
        mListView.setOnItemClickListener(this);
        registerForContextMenu(mListView);

    }

    @Override
    protected void onPause() {

        PreferenceManager.getDefaultSharedPreferences(this).edit().putString(KEY_DIR, mCurrentDirectory).commit();

        super.onPause();
    }

    @Override
    public void onBackPressed() {
        File dir = new File(mCurrentDirectory);
        if (dir.getParentFile() != null) {
            List<String> files = listSupportFiles(dir.getParentFile().getAbsolutePath());
            mFileAdapter.switchData(files);
        } else
            super.onBackPressed();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, MENU_EXIT, 0, R.string.context_menu_exit);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_EXIT:
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        menu.add(0, MENU_SET_FONTFACE, 0, R.string.context_menu_set_fontface);
        menu.add(0, MENU_CONVERT_TXT, 0, R.string.context_menu_convert_txt);
        menu.add(0, MENU_DELETE, 0, R.string.context_menu_delete);
        menu.add(0, MENU_IMPORT, 0, R.string.context_menu_import);
        super.onCreateContextMenu(menu, v, menuInfo);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo menuInfo = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        File file = new File(mCurrentDirectory, mFileAdapter.getItem(menuInfo.position));

        switch (item.getItemId()) {
            case MENU_SET_FONTFACE:
                if (file.isFile()) {
                    setFontface(file.getAbsolutePath());
                }
                return true;
            case MENU_CONVERT_TXT:
                if (file.isFile()) {
                    convert2txt(file.getAbsolutePath());
                }
                return true;
            case MENU_DELETE:
                if (file.isFile()) {
                    file.delete();
                    refreshListView();
                }
                return true;
            case MENU_IMPORT:
                if (file.isFile() && (file.getName().endsWith(".txt") || file.getName().endsWith(".TXT"))) {
                    DataProvider.getInstance().importDocument(file);
                }
                return true;
        }
        return super.onContextItemSelected(item);
    }

    private class ViewHolder {
        public TextView title;
    }

    private class FileAdapter extends BaseAdapter {


        @Override
        public int getCount() {
            return mList.size();
        }

        @Override
        public String getItem(int i) {
            return mList.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            ViewHolder viewHolder;
            if (view == null) {
                view = LayoutInflater.from(FileActivity.this).inflate(R.layout.list_item, viewGroup, false);
                viewHolder = new ViewHolder();
                viewHolder.title = view.findViewById(R.id.title);
                view.setTag(viewHolder);
            } else {

                viewHolder = (ViewHolder) view.getTag();
            }
            viewHolder.title.setText(mList.get(i));
            return view;
        }

        public void switchData(List<String> ls) {
            mList.clear();
            mList.addAll(ls);
            notifyDataSetChanged();
        }
    }
}
