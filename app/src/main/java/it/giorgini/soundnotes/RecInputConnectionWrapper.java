package it.giorgini.soundnotes;

import android.util.Log;
import android.view.KeyEvent;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputConnectionWrapper;

import java.lang.ref.WeakReference;

public class RecInputConnectionWrapper extends InputConnectionWrapper {
    WeakReference<RecordingsView> recView;
    WeakReference<RichEditText> editText;

    public RecInputConnectionWrapper(InputConnection target, boolean mutable) {
        super(target, mutable);
    }

    public void setViews(RecordingsView rv, RichEditText et) {
        recView = new WeakReference<>(rv);
        editText = new WeakReference<>(et);
    }

    // controllo solo se è stato aggiunto un \n, non i caratteri singoli perché questo metodo viene chiamato
    // solo alla fine della parola ma mi serve controllare carattere per carattere
    @Override
    public boolean commitText(CharSequence text, int newCursorPosition) {
//        Log.i("SN $$$", "RecInputConnectionWrapper commitText: " + text);
//        recView.get().onCharAdded(text);
        if (text.equals("\n")) {
            recView.get().insertNewline();
        }

        return super.commitText(text, newCursorPosition);
    }

    @Override
    public boolean sendKeyEvent(KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            switch (event.getKeyCode()) {
                case KeyEvent.KEYCODE_DEL:
                    int startSel = editText.get().getSelectionStart();
                    int endSel = editText.get().getSelectionEnd();
                    if (startSel == endSel) {
                        if (startSel == 0)
                            return true;
//                        String deletedChar = editText.getText().toString().substring(startSel - 1, startSel);
                        char deletedChar = editText.get().getText().charAt(startSel - 1);
                        if (deletedChar == '\n') {
                            if (recView.get().removeNewlineBeforeCurrPos()) {
                                boolean ret = super.sendKeyEvent(event);
                                recView.get().invalidate();
                                return ret;
                            } else
                                return true;
                        }
                    } else {
                        // ho premuto backspace ma sto cancellando una riga complessa
                        if (recView.get().removeComplexNewline(startSel, endSel)) {
                            boolean ret = super.sendKeyEvent(event);
                            recView.get().invalidate();
                            return ret;
                        } else
                            return true;
                    }
                    break;
                case KeyEvent.KEYCODE_FORWARD_DEL:
                    //TODO: gestire tasto canc tastiere fisiche
                    break;
                case KeyEvent.KEYCODE_ENTER:
                case KeyEvent.KEYCODE_NUMPAD_ENTER:
                    recView.get().insertNewline();
                    break;
                default:
                    break;
            }
        }
        return super.sendKeyEvent(event);
    }

//    @Override
//    public boolean setComposingText(CharSequence text, int newCursorPosition) {
//        Log.d("SN $$$", "RecInputConnectionWrapper setComposingText: " + text);
//        return super.setComposingText(text, newCursorPosition);
//    }

//    @Override
//    public boolean performContextMenuAction(int id) {
//        switch (id) {
//            case android.R.id.selectAll:
//                Log.d("SN $$$", "RecInputConnectionWrapper performContextMenuAction: selectAll");
//                break;
//            case android.R.id.startSelectingText:
//                Log.d("SN $$$", "RecInputConnectionWrapper performContextMenuAction: startSelectingText");
//                break;
//            case android.R.id.stopSelectingText:
//                Log.d("SN $$$", "RecInputConnectionWrapper performContextMenuAction: stopSelectingText");
//                break;
//            case android.R.id.cut:
//                Log.d("SN $$$", "RecInputConnectionWrapper performContextMenuAction: cut");
//                break;
//            case android.R.id.copy:
//                Log.d("SN $$$", "RecInputConnectionWrapper performContextMenuAction: copy");
//                break;
//            case android.R.id.paste:
//                Log.d("SN $$$", "RecInputConnectionWrapper performContextMenuAction: paste");
//                break;
//            case android.R.id.copyUrl:
//                Log.d("SN $$$", "RecInputConnectionWrapper performContextMenuAction: copyUrl");
//                break;
//            case android.R.id.switchInputMethod:
//                Log.d("SN $$$", "RecInputConnectionWrapper performContextMenuAction: switchInputMethod");
//                break;
//            default:
//                Log.d("SN $$$", "RecInputConnectionWrapper performContextMenuAction: " + id);
//                break;
//        }
//        return super.performContextMenuAction(id);
//    }

    @Override
    public boolean deleteSurroundingText(int beforeLength, int afterLength) {
        int startSel;
        int endSel = editText.get().getSelectionEnd();
        // se seleziono un testo normalmente e premo backspace android lo implementa spostando il cursore in endSel e cancellando tante
        // volte quante serve. Se io a codice seleziono un testo android lo cancella insieme, quindi non posso fidarmi di beforeLength
        if (beforeLength == 1)
            startSel = editText.get().getSelectionStart();
        else
            startSel = endSel - beforeLength;

        if (startSel == endSel) {
            if (startSel == 0)
                return super.deleteSurroundingText(beforeLength, afterLength);

            // ho premuto semplicemente backspace
//            String deletedChar = editText.getText().toString().substring(startSel - 1, startSel);
            char deletedChar = editText.get().getText().charAt(endSel - 1);
            if (deletedChar == '\n') {
                // se non posso cancellare...
                if(recView.get().removeNewlineBeforeCurrPos()) {
                    boolean ret = super.deleteSurroundingText(beforeLength, afterLength);
                    recView.get().invalidate();
                    return ret;
                } else {
                    return true;
                }
            }
        } else {
            // ho premuto backspace ma sto cancellando una riga complessa
            if (recView.get().removeComplexNewline(startSel, endSel)) {
                boolean ret = super.deleteSurroundingText(beforeLength, afterLength);
                recView.get().invalidate();
                return ret;
            } else {
                editText.get().setSelection(startSel, endSel);
                return true;
            }
        }
        return super.deleteSurroundingText(beforeLength, afterLength);
    }
}