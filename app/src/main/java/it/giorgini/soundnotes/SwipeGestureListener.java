package it.giorgini.soundnotes;

import java.lang.ref.WeakReference;

import android.content.Context;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

class SwipeGestureListener extends GestureDetector.SimpleOnGestureListener {
    private static final String DEBUG_TAG = "Gestures"; 
    private WeakReference<NoteListActivity> activity = null;
    private WeakReference<View> view_list = null;
    
    private float scrollDistance = 0;
//    private WeakReference<View> view_detailActivity = null;
//    private WeakReference<View> view_detailFragment = null;
    
    public SwipeGestureListener(Context myActivity) {
		activity = new WeakReference<NoteListActivity>((NoteListActivity) myActivity);
		view_list = new WeakReference<View>(activity.get().findViewById(R.id.note_list));
//		view_detailFragment = new WeakReference<View>(activity.get().findViewById(R.id.note_detail));
//		view_detailActivity = new WeakReference<View>(activity.get().findViewById(R.id.note_detail_container));
	}
    
    @Override
    public boolean onDown(MotionEvent event) { 
        Log.d(DEBUG_TAG,"onDown: " + event.toString());
        scrollDistance = 0;
        return true;
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX,
            float distanceY) {
        Log.d(DEBUG_TAG, "onScroll: " + "x:" + distanceX + ", y:" + distanceY);
        scrollDistance += distanceX;
        if (view_list.get().getVisibility() == View.VISIBLE && scrollDistance > 100) {
        	Log.d(DEBUG_TAG, "onScroll: " + "activated!");
        	activity.get().slideToLeft(view_list.get(), View.GONE);
        } else if (view_list.get().getVisibility() == View.GONE && scrollDistance < -100) {
        	Log.d(DEBUG_TAG, "onScroll: " + "deactivated!");
        	activity.get().slideToRight(view_list.get(), View.VISIBLE);
        }
        return true;
    }
    
}