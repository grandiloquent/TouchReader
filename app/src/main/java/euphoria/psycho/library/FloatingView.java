package euphoria.psycho.library;

import android.content.Context;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.os.Build;
import android.util.Log;
import android.view.*;
import android.widget.TextView;

public class FloatingView {

    private final Context mContext;
    private TextView mTextView;
    private WindowManager mWindowManager;
    private View mView;

    public FloatingView(Context context) {
        mContext = context;
        initialize();
    }

    private void initialize() {

        mView = LayoutInflater.from(mContext).inflate(R.layout.floating_window, null);

        mView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mWindowManager.removeView(mView);
            }
        });
        mTextView = mView.findViewById(R.id.tv);
        mTextView.setTextIsSelectable(true);

        mWindowManager = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);


        Point point = new Point();
        mWindowManager.getDefaultDisplay().getSize(point);
        int LAYOUT_FLAG;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            LAYOUT_FLAG = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            LAYOUT_FLAG = WindowManager.LayoutParams.TYPE_PHONE;
        }

        final WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                LAYOUT_FLAG,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,

                PixelFormat.TRANSLUCENT);

       // params.height = (int) (point.y * .2f);
        // params.gravity = Gravity.BOTTOM;
        mWindowManager.addView(mView, params);
    }

    private int dp2px(Context context, float dp) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dp * scale + 0.5f);
    }

   public void setText(String s) {
        mTextView.setText(s);
    }
}
