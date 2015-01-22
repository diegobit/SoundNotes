package it.giorgini.soundnotes;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.ScrollView;

public class ScrollRecordingsView extends ScrollView {
    public ScrollRecordingsView(Context context) {
        super(context);
    }

    public ScrollRecordingsView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ScrollRecordingsView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

//    public ScrollRecordingsView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
//        super(context, attrs, defStyleAttr, defStyleRes);
//    }


    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
//        Log.i("SN @@@", "Scroll - dispatch");
        super.dispatchTouchEvent(ev);
        return false;
    }



}
