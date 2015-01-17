//package it.giorgini.soundnotes;
//
//import java.lang.ref.WeakReference;
//
//import android.content.Context;
//import android.util.Log;
//import android.view.GestureDetector;
//import android.view.MotionEvent;
//import android.view.View;
//
//class SwipeGestureListener extends GestureDetector.SimpleOnGestureListener {
//    private static final String DEBUG_TAG = "Gestures";
//    private WeakReference<NoteListActivity> activity;
//    private WeakReference<View> view_list;
//
//    private float scrollDistance = 0;
////    private WeakReference<View> view_detailActivity = null;
////    private WeakReference<View> view_detailFragment = null;
//
//    public SwipeGestureListener(Context myActivity) {
//		activity = new WeakReference<NoteListActivity>((NoteListActivity) myActivity);
//		view_list = new WeakReference<View>(activity.get().findViewById(R.id.note_list_container));
////		view_detailFragment = new WeakReference<View>(activity.get().findViewById(R.id.note_detail));
////		view_detailActivity = new WeakReference<View>(activity.get().findViewById(R.id.note_detail_container));
//	}
//
//    @Override
//    public boolean onDown(MotionEvent event) {
////        scrollDistance = 0;
////        return true;
//        return false;
//    }
//
//    @Override
//    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX,
//            float distanceY) {
//        scrollDistance += distanceX;
//        Log.d(DEBUG_TAG, "onScroll: " + "x:" + distanceX + ", y:" + distanceY + ", dist:" + scrollDistance + ", state:" + view_list.get().getVisibility());
//        if (view_list.get().getVisibility() == View.VISIBLE && scrollDistance > 100) {
//        	Log.d(DEBUG_TAG, "onScroll: " + "activated!");
//        	activity.get().slideToLeft(view_list.get(), View.GONE);
//            scrollDistance = 0;
//            return true;
//        } else if (view_list.get().getVisibility() == View.GONE && scrollDistance < -100) {
//        	Log.d(DEBUG_TAG, "onScroll: " + "deactivated!");
//        	activity.get().slideToRight(view_list.get(), View.VISIBLE);
//            scrollDistance = 0;
//            return true;
//        }
//        return super.onScroll(e1, e2, distanceX, distanceY);
//    }
//
//    //    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
////                           float velocityY) {
////        try {
////            if (Math.abs(e1.getY() - e2.getY()) > SWIPE_MAX_OFF_PATH)
////                return false;
////// right to left swipe
////
////            if (e1.getX() - e2.getX() > SWIPE_MIN_DISTANCE
////                    && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY
////                    && curPage < _totPages - 1) {
////
//////// Logic
////            } else if (e2.getX() - e1.getX() > SWIPE_MIN_DISTANCE
////                    && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY
////                    && curPage > 0) {
//////// Logic
////            }
////
////        } catch (Exception e) {
////// nothing
////        }
////        return false;
////    }
//}