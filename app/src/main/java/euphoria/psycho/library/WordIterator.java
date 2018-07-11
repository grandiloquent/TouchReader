package euphoria.psycho.library;
import java.text.BreakIterator;
import java.util.Locale;
/**
 * Created by Administrator on 2015/1/24.
 */
public class WordIterator {
    private BreakIterator nBreakIterator;
    private static final int WINDOW_WIDTH = 50;
    private int nOffsetShift;
    private String nString;
    public WordIterator() {
        this(Locale.getDefault());
    }
    public WordIterator(Locale locale) {
        nBreakIterator = BreakIterator.getWordInstance(locale);
    }
    public int getEnd(int offset) {
        final int shiftedOffset = offset - nOffsetShift;
        checkOffsetIsValid(shiftedOffset);
        if (isAfterLetterOrDigit(shiftedOffset)) {
            if (nBreakIterator.isBoundary(shiftedOffset)) {
                return shiftedOffset + nOffsetShift;
            } else {
                return nBreakIterator.following(shiftedOffset) + nOffsetShift;
            }
        } else {
            if (isOnLetterOrDigit(shiftedOffset)) {
                return nBreakIterator.following(shiftedOffset) + nOffsetShift;
            }
        }
        return BreakIterator.DONE;
    }
    public void setCharSequence(CharSequence charSequence, int start, int end) {
        nOffsetShift = Math.max(0, start - WINDOW_WIDTH);
        final int windowEnd = Math.min(charSequence.length(), end + WINDOW_WIDTH);
        nString = charSequence.subSequence(nOffsetShift, windowEnd).toString();
        nBreakIterator.setText(nString);
    }
    public int getBeginning(int offset) {
        final int shiftedOffset = offset - nOffsetShift;
        checkOffsetIsValid(shiftedOffset);
        if (isOnLetterOrDigit(shiftedOffset)) {
            if (nBreakIterator.isBoundary(shiftedOffset)) {
                return shiftedOffset + nOffsetShift;
            } else {
                return nBreakIterator.preceding(shiftedOffset) + nOffsetShift;
            }
        } else {
            if (isAfterLetterOrDigit(shiftedOffset)) {
                return nBreakIterator.preceding(shiftedOffset) + nOffsetShift;
            }
        }
        return BreakIterator.DONE;
    }
    private boolean isAfterLetterOrDigit(int shiftedOffset) {
        if (shiftedOffset >= 1 && shiftedOffset <= nString.length()) {
            final int codePoint = nString.codePointBefore(shiftedOffset);
            if (Character.isLetterOrDigit(codePoint)) return true;
        }
        return false;
    }
    private boolean isOnLetterOrDigit(int shiftedOffset) {
        if (shiftedOffset >= 0 && shiftedOffset < nString.length()) {
            final int codePoint = nString.codePointAt(shiftedOffset);
            if (Character.isLetterOrDigit(codePoint)) return true;
        }
        return false;
    }
    private void checkOffsetIsValid(int shiftedOffset) {
        if (shiftedOffset < 0 || shiftedOffset > nString.length()) {
            throw new IllegalArgumentException("Invalid offset: " + (shiftedOffset + nOffsetShift) +
                    ". Valid range is [" + nOffsetShift + ", " + (nString.length() + nOffsetShift) +
                    "]");
        }
    }
}