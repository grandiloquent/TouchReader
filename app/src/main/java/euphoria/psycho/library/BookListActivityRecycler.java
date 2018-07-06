package euphoria.psycho.library;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.*;
import android.widget.TextView;

import java.util.List;


public class BookListActivityRecycler extends Activity implements OnItemClickListener {
    private static final String TAG = "BookListActivity";
    private BookAdapter mBookAdapter;
    private RecyclerView mRecyclerView;

    private static final int MENU_CHANGE_TAG = 0;

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        int p = mBookAdapter.getPosition();
        switch (item.getItemId()) {
            case MENU_CHANGE_TAG:
                Log.e(TAG, mBookAdapter.getItem(p));
                return true;
        }
        return super.onContextItemSelected(item);
    }

    @Override
    public void onItemClick(String value) {

        Intent intent = new Intent();
        intent.putExtra(MainActivity.KEY_TAG, value);
        setResult(Activity.RESULT_OK, intent);
        this.finish();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.booklis_activity);
        // https://github.com/codepath/android_guides/wiki/Using-the-RecyclerView
        //mRecyclerView = findViewById(R.id.recyclerView);

        mBookAdapter = new BookAdapter(DataProvider.getInstance().listTag(), this, this);
        mRecyclerView.setAdapter(mBookAdapter);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        registerForContextMenu(mRecyclerView);
    }


    public class BookAdapter extends RecyclerView.Adapter<BookAdapter.ViewHolder> {
        private final List<String> books;
        private final LayoutInflater inflater;
        private final OnItemClickListener listener;
        private int position;

        public int getPosition() {
            return position;
        }

        public void setPosition(int position) {
            this.position = position;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

            View v = inflater.inflate(R.layout.textview_item, parent, false);


            ViewHolder viewHolder = new ViewHolder(v);
            return viewHolder;
        }

        @Override
        public void onBindViewHolder(final ViewHolder holder, int position) {

            String book = books.get(position);
            holder.textView.setText(book);
            holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    setPosition(holder.getPosition());
                    return false;
                }
            });
        }

        public String getItem(int p) {
            return books.get(p);
        }

        @Override
        public int getItemCount() {
            return books.size();
        }

        @Override
        public void onViewRecycled(ViewHolder holder) {
            holder.itemView.setOnLongClickListener(null);
            super.onViewRecycled(holder);
        }

        public BookAdapter(List books, Context context, OnItemClickListener listener) {
            this.books = books;
            this.listener = listener;
            inflater = LayoutInflater.from(context);
        }

        public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnCreateContextMenuListener {
            private TextView textView;

            @Override
            public void onClick(View view) {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    listener.onItemClick(books.get(position));
                }
            }

            @Override
            public void onCreateContextMenu(ContextMenu contextMenu, View view, ContextMenu.ContextMenuInfo contextMenuInfo) {
                contextMenu.add(Menu.NONE, MENU_CHANGE_TAG,
                        Menu.NONE, R.string.change_tag);

            }

            public ViewHolder(View itemView) {
                super(itemView);
                textView = (TextView) itemView.findViewById(R.id.textView);
                textView.setOnClickListener(this);
                itemView.setOnCreateContextMenuListener(this);
            }
        }

    }
}
