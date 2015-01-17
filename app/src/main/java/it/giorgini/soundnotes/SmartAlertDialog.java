package it.giorgini.soundnotes;

import android.app.AlertDialog;
import android.content.Context;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

/**
 * Created by diego on 17/01/15.
 */
public class SmartAlertDialog extends AlertDialog{
    protected SmartAlertDialog(Context context) {
        super(context);
    }

    public void show(EditText input) {
        show();

        InputMethodManager imm = (InputMethodManager) getOwnerActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.showSoftInput(input, InputMethodManager.SHOW_IMPLICIT);
    }
}
