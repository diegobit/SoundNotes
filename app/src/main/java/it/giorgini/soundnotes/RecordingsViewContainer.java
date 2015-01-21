package it.giorgini.soundnotes;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;

public class RecordingsViewContainer extends FrameLayout {
    public RecordingsViewContainer(Context context) {
        super(context);
    }

    public RecordingsViewContainer(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public RecordingsViewContainer(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

//    public Recordings_view_container(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
//        super(context, attrs, defStyleAttr, defStyleRes);
//    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
//        return super.dispatchTouchEvent(ev);
        boolean consumed = false;
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);

            MotionEvent eventcopy = MotionEvent.obtain(event);
            eventcopy.offsetLocation(-child.getLeft(), -child.getTop());
            // consumato se questo figlio ha consumato l'evento o se lo ha consumato l'altro figlio prima
            consumed = child.dispatchTouchEvent(eventcopy) || consumed;
//            eventcopy.recycle();
//            return consumed;



//            if (child.viewMetrics.containsPoint(ev.getX(), ev.getY())) {
//                child.dispatchTouchEvent(ev);
//                return true;
//            }
        }
        return consumed;
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
//        super.dispatchDraw(canvas);
//        boolean consumed = false;
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            child.invalidate();
            child.draw(canvas);
        }
//        return consumed;
    }
}
