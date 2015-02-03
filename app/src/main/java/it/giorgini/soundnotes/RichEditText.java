package it.giorgini.soundnotes;

import android.content.ClipData;
import android.content.ClipDescription;
import android.content.ClipboardManager;
import android.content.Context;
import android.text.Editable;
import android.text.Layout;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.Log;
import android.view.DragEvent;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.widget.EditText;

import java.lang.ref.WeakReference;

public class RichEditText extends EditText {

    private WeakReference<RecordingsView> recView;
    private WeakReference<Context> ctx;
    private int lastDevLineCount = 0;
    private int lastDevLine = 0;

	// COSTRUTTORI
	public RichEditText(Context context) {
		super(context);
		initTextWatcher();
	}

	public RichEditText(Context context, AttributeSet attrs) {
		super(context, attrs);
		initTextWatcher();
	}

	public RichEditText(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		initTextWatcher();
	}

    // Con questo oggetto intercetto l'input che arriva alla RichEditText e lo manipolo prima di passarglielo
    @Override
    public InputConnection onCreateInputConnection(@SuppressWarnings("NullableProblems") EditorInfo outAttrs) {
        RecInputConnectionWrapper ti = new RecInputConnectionWrapper(super.onCreateInputConnection(outAttrs), true);
        // imposto un collegamento tra recView e l'inputconnection.
        ti.setViews(recView.get(), this);
        return ti;
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        // inizializzo la lista delle registrazioni della recView
        recView.get().setCurrRecList();
    }

    public void setRecView(RecordingsView recView) {
        this.recView = new WeakReference<>(recView);
    }

    public void setContext(Context ctx) {
        this.ctx = new WeakReference<>(ctx);
    }

    public void initRecViewDevLineCount() {
        recView.get().setEditTextDevLineCount(getLineCount());
    }

//    Inizializzazione TextWatcher
	private void initTextWatcher() {
        addTextChangedListener(new TextWatcher() {
            // Controllo carattere per carattere (non \n che lo controllo in InputConnectionWrapper commitChanges
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
//                Log.d("SN $$$", "RET textwatcher beforeTextChanged - start: " + start + " count: " + count + " after: " + after);

                // ho scritto un carattere. Se > 1 allora ho incollato o è l'inizializzazione.
                // (FIXME: potrebbe esserci solo un carattere nella nota e aprirla)
                if (after - count == 1) {
                    lastDevLineCount = getLineCount();
                    lastDevLine = getCurrDevLine();
                }
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
//                Log.d("SN $$$", "RET textwatcher ontTextChanged - start: " + start + " count: " + count + " before: " + before);
                if (count - before == 1) {
                    recView.get().onCharAdded(lastDevLineCount, lastDevLine);
                } else if (count - before == -1) {
                    // sto cancellando un carattere, controllo se si è ridotto il numero di righe device
                    recView.get().onCharDeleted(lastDevLineCount, lastDevLine);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
//                Log.d("SN $$$", "RET textwatcher afterTextChanged");
//                // TODO Auto-generated method stub
            }
        });
    }

//    @Override
//    public boolean onDragEvent(@SuppressWarnings("NullableProblems") DragEvent event) {
//        switch (event.getAction()) {
//            case DragEvent.ACTION_DRAG_STARTED:
//                if (recView.onDragStarted(DragEvent event)) {
//                    return true;
//                }
//                return false;
//                break;
//            default:
//                return super.onDragEvent(event);
//                break;
//        }
//    }

    //	@Override
//	protected void onDraw(@NonNull Canvas canvas) {
//		super.onDraw(canvas);
//	}

//    @Override
//    public boolean onTouchEvent(MotionEvent event) {
//        return super.onTouchEvent(event);
//    }

    public int getCurrLine() {
        String startTextToSelection = getText().toString().substring(0, getSelectionStart());
        return countOccurrences('\n', startTextToSelection, 0, '\0');
    }

    public int getCurrDevLine() {
        return getDeviceLineForOffset(getSelectionStart());
    }

    /** Conta le occorrenze di c nella stringa s partendo dall'indice startIndex.
     * Se trova stopChar si ferma prima del termine di s e ritorna le occorrenze trovate.
     * Per cercase fino alla fine della stringa passare \0 come stopChar */
    public int countOccurrences(char c, String s, int startIndex, char stopChar) {
        int counter = 0;
        if (stopChar == '\0') {
            for (int i = startIndex; i < s.length(); i++) {
                if (c == s.charAt(i)) {
                    counter++;
                }
            }
        } else {
            boolean stop = false;
            for (int i = startIndex; i < s.length() || !stop; i++) {
                if (c == s.charAt(i)) {
                    counter++;
                } else if (c == stopChar) {
                    stop = true;
                }
            }
        }
        return counter;
    }

    public int getDeviceLineForOffset(int index) {
        Layout lay = getLayout(); //FIXME: strano strano bug... creo nota con UN (1) carattere. QUando la apro lay == null
        if (lay != null) {
            return getLayout().getLineForOffset(index);
        } else {
            Log.w("SN @@@", "RET getDeviceLineFroOffset layout NULL!!!");
            return 0;
        }
    }

    public int getLineForOffset(int index) {
        String textToIntex = getText().toString().substring(0, index);
        return countOccurrences('\n', textToIntex, 0, '\0');
    }

    public int[] getLineForEachOffset(int startI, int endI) {
        String text = getText().subSequence(0, endI + 1).toString();
        int len = text.length();
        if (startI < 0 || endI < 0 || startI > len || endI > len)
            throw new IndexOutOfBoundsException("startI: " + startI + " endI: " + endI + " string len: " + len);

        int counter = 0;
        int[] lines = new int[2];
        for (int i = 0; i < len; i++) {
            if (text.charAt(i) == '\n')
                counter++;
            if (i == startI) {
                lines[0] = counter;
            }
            if (i == endI) {
                lines[1] = counter;
                return lines;
            }
        }
        Log.w("SN @@@", "uscito dal for ma NON VA BENE!!!");
        return lines;
    }

    // Passo una stringa per chiamarla quando la RET non è ancora inizializzata bene
    public int getDeviceLineFromLine(String text, int line) {
        String s = getText().toString();
//        if (text == null)
//            s = getText().toString();
//        else
//            s = text;
        boolean found = false;
        int counter = 0;
        int i = 0;

        // ciclo: appena trovo il line-esimo '\n' allora il carattere successivo è il primo della
        // line-esima linea. Con quello trovo la "device line", cioè quella che si basa sul layout
        while (!found && i < s.length()) {
            if (s.charAt(i) == '\n') {
                counter++;
            }
            if (line == counter)
                found = true;
            i++;
        }

        if (found) {
            return getDeviceLineForOffset(i);
        }
        else {
            Log.w("SN @@@", "RET getDeviceLineFromLine(" + line + ") ha ritornato 0 - strlen: " + s.length() + " counter: " + counter);
            return -0;
        }
    }

    public String getStringIncludedInDeviceLines(int startLine, int endLine) {
        int startPos = getLayout().getLineStart(startLine);
        int endPos = getLayout().getLineEnd(endLine);

        return getText().toString().substring(startPos, endPos);
    }

    public String getLineText(int line) {
        String s = getText().toString();
        int counter = 0;
        int i = 0;
        boolean found = false;
        int startPos = 0;
        int endPos = 0;

        if (line != 0) {
            while (!found && i < s.length()) {
                if (s.charAt(i) == '\n') {
                    counter++;
                }
                if (counter == line) {
                    if (line != 1)
                        startPos = i + 1;
                    found = true;
                }
                i++;
            }
        }

        found = false;

        while (!found && i < s.length()) {
            if (s.charAt(i) == '\n') {
                counter++;
            }
            if (counter == line + 1) {
                endPos = i;
                found = true;
            }
            i++;
        }

        return getText().toString().substring(startPos, endPos);
    }

    public String getDeviceLineText(int line) {
        int startPos = getLayout().getLineStart(line);
        int endPos = getLayout().getLineEnd(line);

        return getText().toString().substring(startPos, endPos);
    }

    public int countDevLinesInLine(int line) {
        String s = getText().toString();
        int counter = 0;
        int i = 0;
        boolean found = false;
        int startPos = 0;
        int endPos = 0;

        if (line > 0) {
            while (!found && i < s.length()) {
                if (s.charAt(i) == '\n') {
                    counter++;
                }
                if (counter == line - 1) {
                    if (line != 1)
                        startPos = i + 1;
                    found = true;
                }
                i++;
            }
        }

        found = false;

        while (!found && i < s.length()) {
            if (s.charAt(i) == '\n') {
                counter++;
            }
            if (counter == line) {
                endPos = i;
                found = true;
            }
            i++;
        }

        return getDeviceLineForOffset(endPos) - getDeviceLineForOffset(startPos) + 1;
    }



    // Quando premo su taglia/copia voglio essere notificato
    @Override
    public boolean onTextContextMenuItem(int id) {
        boolean consumed = false;

        switch (id) {
            case android.R.id.cut:
                if (onTextCut()) {
                    consumed = super.onTextContextMenuItem(id);
                    recView.get().invalidate();
                }
                break;
            case android.R.id.paste:
                int[] textInfo = beforeTextPaste();
                consumed = super.onTextContextMenuItem(id);
                onTextPaste(textInfo);
                break;
            default:
                consumed = super.onTextContextMenuItem(id);
                break;
        }

        return consumed;
    }

    public boolean onTextCut() {
        int startSel = getSelectionStart();
        int endSel = getSelectionEnd();
//        Log.d("SN $$$", "RET ontextcut - startSel: " + startSel + " endSel: " + endSel);
        return recView.get().removeComplexNewline(startSel, endSel);
    }

    public int[] beforeTextPaste() {
        int[] textInfo = new int[2];
        textInfo[0] = getSelectionStart();
        textInfo[1] = getLineCount();
        return textInfo;
    }

    public void onTextPaste(int[] textInfo) {
        int startSelPos = textInfo[0];
        int prevDevLineCount = textInfo[1];
        ClipboardManager clipboard = (ClipboardManager) ctx.get().getSystemService(Context.CLIPBOARD_SERVICE);

        String pasteData = "";

        // Controllo che la clipboard contenga qualcosa e che sia testo incollabile
        if (clipboard.hasPrimaryClip() &&
                clipboard.getPrimaryClipDescription().hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN)) {
            ClipData.Item item = clipboard.getPrimaryClip().getItemAt(0);
            pasteData = item.getText().toString();
            if (pasteData != null) {
                int linesAdded = countOccurrences('\n', pasteData, 0, '\0');


                recView.get().insertNewline(linesAdded, startSelPos, prevDevLineCount);
            }
        }
    }

    @Override
    protected void onScrollChanged(int horiz, int vert, int oldHoriz, int oldVert) {
        super.onScrollChanged(horiz, vert, oldHoriz, oldVert);

        recView.get().scrollBy(horiz - oldHoriz, vert - oldVert);
    }
}

