package euphoria.psycho.library;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.*;
import android.widget.*;

import java.util.List;


public class BookListActivity extends Activity implements AdapterView.OnItemClickListener {
    private static final int MENU_ADD_CLIPBOARD = 0;
    private static final int MENU_CHANGE_TAG = 1;
    private static final String TAG = "BookListActivity";
    private BookAdapter mBookAdapter;
    private List<String> mList;
    private ListView mListView;

    private void addFromClipboard(String s) {
        ClipboardManager clipboardManager = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);

        ClipData clipData = clipboardManager.getPrimaryClip();
        if (clipData != null && clipData.getItemCount() > 0) {
            CharSequence prefix = clipData.getItemAt(0).getText();
            if (prefix != null) {
                DataProvider.getInstance().addFromClipboard(s, prefix.toString());
                Toast.makeText(this, String.format("成功从剪切板添加%s.", s), Toast.LENGTH_SHORT).show();
                // refreshList();
            }
        }
    }

    private void addFromClipboard() {
        ClipboardManager clipboardManager = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);

        ClipData clipData = clipboardManager.getPrimaryClip();
        if (clipData != null && clipData.getItemCount() > 0) {
            CharSequence prefix = clipData.getItemAt(0).getText();
            if (prefix != null) {
                DataProvider.getInstance().insertArticle(prefix.toString());
                refreshList();
                Toast.makeText(this,"成功从剪切板添加.", Toast.LENGTH_SHORT).show();

            }
        }
    }

    private void changeTag(final String s) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        final EditText editText = new EditText(this);
        editText.setText(s);
        builder.setView(editText);
        builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        });

        builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                if (editText.getText() == null) return;
                DataProvider.getInstance().updateTag(s, editText.getText().toString());
                refreshList();
            }
        });
        builder.show();
    }

    private void refreshList() {
        mBookAdapter.switchData(DataProvider.getInstance().listTag());
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        Intent intent = new Intent();
        intent.putExtra(MainActivity.KEY_TAG, mList.get(i));
        setResult(RESULT_OK, intent);
        finish();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.booklis_activity);
        mList = DataProvider.getInstance().listTag();
        mBookAdapter = new BookAdapter();
        mListView = findViewById(R.id.listView);
        mListView.setAdapter(mBookAdapter);
        mListView.setOnItemClickListener(this);
        registerForContextMenu(mListView);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, MENU_ADD_CLIPBOARD, 0, R.string.add_from_clipboard);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_ADD_CLIPBOARD:
                addFromClipboard();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        menu.add(0, MENU_ADD_CLIPBOARD, 0, R.string.add_from_clipboard);
        menu.add(0, MENU_CHANGE_TAG, 0, R.string.change_tag);
        super.onCreateContextMenu(menu, v, menuInfo);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo menuInfo = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();

        switch (item.getItemId()) {
            case MENU_CHANGE_TAG:
                changeTag(mList.get(menuInfo.position));
                return true;
            case MENU_ADD_CLIPBOARD:
                addFromClipboard(mList.get(menuInfo.position));
                return true;
        }
        return super.onContextItemSelected(item);
    }

    private class ViewHolder {
        public TextView title;
    }

    private class BookAdapter extends BaseAdapter {


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
                view = LayoutInflater.from(BookListActivity.this).inflate(R.layout.list_item, viewGroup, false);
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
