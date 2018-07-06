package euphoria.psycho.library;

import android.content.Context;
import android.content.pm.PackageManager;
import android.util.AttributeSet;
import android.view.*;
import android.widget.TextView;

import java.text.BreakIterator;

public class ReaderView extends android.support.v7.widget.AppCompatTextView implements View.OnTouchListener {
    private int mMinTouchOffset, mMaxTouchOffset;
    private boolean mIsTouchscreenMultiTouchDistinct;
    private WordIterator mWordIterator;
    private static final int ID_ANNOTATION = 731;

    private boolean mIsSelected = true;

    public interface SelectListener {

        void onSelectionChange(String value);

        void onClick();
    }

    private SelectListener mSelectListener;

    private long blockTime = 0L;

    public ReaderView(Context context) {
        super(context);
        initialize();
    }

    public void setSelectListener(SelectListener selectListener) {
        mSelectListener = selectListener;

    }

    public ReaderView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize();
    }


    public ReaderView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initialize();
    }

    public static boolean isTouchscreenMultiTouchDistinct(Context context) {
        return context.getPackageManager().hasSystemFeature(
                PackageManager.FEATURE_TOUCHSCREEN_MULTITOUCH_DISTINCT);
    }

    public void switchSelectable() {
        setTextIsSelectable(this.mIsSelected);
        this.mIsSelected = !this.mIsSelected;


    }


    private void initialize() {


        /*setContentText(getText(), true);*/

        // this.setOnEditorActionListener(null);
        this.setOnTouchListener(this);
        setTextIsSelectable(!mIsSelected);

        mWordIterator = new WordIterator();
        mWordIterator.setCharSequence(getText(), 0, getText().length());
        mIsTouchscreenMultiTouchDistinct = isTouchscreenMultiTouchDistinct(this.getContext());
    }

    public static long packRangeInLong(int start, int end) {
        return (((long) start) << 32) | end;
    }

    public static int unpackRangeStartFromLong(long range) {
        return (int) (range >>> 32);
    }

    public static int unpackRangeEndFromLong(long range) {
        return (int) (range & 0x00000000FFFFFFFFL);
    }

    private void updateMinAndMaxOffsets(MotionEvent event) {
        if (System.currentTimeMillis() > blockTime) {
            int pointerCount = event.getPointerCount();
            for (int index = 0; index < pointerCount; index++) {
                int offset = getOffsetForPosition(event.getX(index), event.getY(index));
                if (offset < mMinTouchOffset) mMinTouchOffset = offset;
                if (offset > mMaxTouchOffset) mMaxTouchOffset = offset;
            }
        }
    }

    private String selectCurrentWord() {

        int stringLength = this.getText().length();

        long lastTouchOffsets = packRangeInLong(mMinTouchOffset, mMaxTouchOffset);
        final int minOffset = unpackRangeStartFromLong(lastTouchOffsets);
        final int maxOffset = unpackRangeEndFromLong(lastTouchOffsets);
        if (minOffset < 0 || minOffset >= stringLength) return null;
        if (maxOffset < 0 || maxOffset >= stringLength) return null;
        int selectionStart, selectionEnd;

        mWordIterator.setCharSequence(getText(), minOffset, maxOffset);
        selectionStart = mWordIterator.getBeginning(minOffset);
        selectionEnd = mWordIterator.getEnd(maxOffset);
        if (selectionStart == BreakIterator.DONE || selectionEnd == BreakIterator.DONE ||
                selectionStart == selectionEnd) {
            long range = getCharRange(minOffset);
            selectionStart = unpackRangeStartFromLong(range);
            selectionEnd = unpackRangeEndFromLong(range);
        }

        return this.getText().subSequence(selectionStart, selectionEnd).toString();
    }

    private long getCharRange(int offset) {
        int stringLength = this.getText().length();

        if (offset + 1 < stringLength) {
            final char currentChar = getText().charAt(offset);
            final char nextChar = getText().charAt(offset + 1);
            if (Character.isSurrogatePair(currentChar, nextChar)) {
                return packRangeInLong(offset, offset + 2);
            }
        }
        if (offset < stringLength) {
            return packRangeInLong(offset, offset + 1);
        }
        if (offset - 2 >= 0) {
            final char previousChar = getText().charAt(offset - 1);
            final char previousPreviousChar = getText().charAt(offset - 2);
            if (Character.isSurrogatePair(previousPreviousChar, previousChar)) {
                return packRangeInLong(offset - 2, offset);
            }
        }
        if (offset - 1 >= 0) {
            return packRangeInLong(offset - 1, offset);
        }
        return packRangeInLong(offset, offset);
    }

    @Override
    public boolean onTouch(View view, MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                final float x = event.getX();
                final float y = event.getY();
                mMinTouchOffset = mMaxTouchOffset = getOffsetForPosition(x, y);
                return mIsSelected;
            case MotionEvent.ACTION_POINTER_UP:
                if (mIsTouchscreenMultiTouchDistinct) {
                    updateMinAndMaxOffsets(event);
                }

                return mIsSelected;

            case MotionEvent.ACTION_UP:

                String value = selectCurrentWord();
                if (value == null) return mIsSelected;
                value = value.trim();
                if (value != null && value.length() > 0 && mSelectListener != null) {
                    if (value.contains(" ")) {
                        value = value.split(" ")[0];
                    }
                    mSelectListener.onSelectionChange(value);
                } else {
                    mSelectListener.onClick();
                }
                return mIsSelected;

        }
        return false;
    }
}
