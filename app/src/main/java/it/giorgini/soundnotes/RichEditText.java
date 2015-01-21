package it.giorgini.soundnotes;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.support.annotation.NonNull;
import android.text.Layout;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.EditText;

public class RichEditText extends EditText {

//	private int lgrey;
//    private int orange;
//    private Paint paint;
    private Paint lgrey = new Paint();
    private Paint orange = new Paint();
    private Paint orangeShadow = new Paint();
    private int bottomDescent = 0;

    private final int defWidth = getWidth();
    private final float leftMargin = getResources().getDimension(R.dimen.rec_left_margin);
    private final float upperMargin = getResources().getDimension(R.dimen.rec_upper_margin);
//    private float recBarWidth;
    // px = dp * density
    // dp = px / density
//    private float density;

//    RecordingsView recView;

//    private Callbacks callbacks_RecordingsView = defaultCallbacks_RecordingsView;

//    public interface Callbacks {
//        /**
//         * Callback for when an item has been selected.
//         */
//        public void scrollBy(int x, int y);
////        public void saveCurrentNote();
////        public void slideToLeft(int newVisibility);
////        public void slideToRight(int newVisibility);
//    }

//    private static Callbacks defaultCallbacks_RecordingsView= new Callbacks() {
//        @Override
//        public void scrollBy(int x, int y) { }
//    };


	// COSTRUTTORI
	public RichEditText(Context context) {
		super(context);
		defaultInit();
	}

	public RichEditText(Context context, AttributeSet attrs) {
		super(context, attrs);
		defaultInit();
	}

	public RichEditText(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		defaultInit();
	}

	
	// Inizializzazione comune
	private void defaultInit() {
//        DisplayMetrics dm = new DisplayMetrics();
//        ((Activity) getContext()).getWindowManager().getDefaultDisplay().getMetrics(new DisplayMetrics());
//        density = dm.density;
//        recBarWidth = recBarWidthDP * density;

        setWidth(defWidth - (int) leftMargin);

        lgrey.setColor(getResources().getColor(R.color.line_lgrey));
        orange.setColor(getResources().getColor(R.color.line_orange));
        orangeShadow.setColor(getResources().getColor(R.color.line_shadow));
//        recView = (RecordingsView) findViewById(R.id.rec_view);
	}
	
	@Override
	protected void onDraw(@NonNull Canvas canvas) {
//        Log.i("SN @@@", "RichEditText draw. dens: " + density + " recDP: " + recBarWidthDP + " rec: " + recBarWidth);

        canvas.translate(leftMargin, upperMargin);
		super.onDraw(canvas);
        canvas.translate(-leftMargin, upperMargin);

//        paint.setColor(lgrey);

//        paint.setColor(orange);
        canvas.drawLine(leftMargin, 5, leftMargin, canvas.getHeight() - 5, lgrey);

        canvas.drawLine(10, bottomDescent - 1, getWidth() - 30, bottomDescent - 1, orangeShadow);
        canvas.drawLine(10, bottomDescent, getWidth() - 30, bottomDescent, orange);
        canvas.drawLine(10, bottomDescent + 1, getWidth() - 30, bottomDescent + 1, orangeShadow);
	}



    @Override
    public boolean onTouchEvent(MotionEvent event) {
        event.offsetLocation(-leftMargin, -upperMargin);
        boolean ret = super.onTouchEvent(event);
        event.offsetLocation(leftMargin, upperMargin);

        if (event.getAction() == MotionEvent.ACTION_UP) {
            int line = getLayout().getLineForOffset(getSelectionStart());
            if (line > 0) {
                int bottom = getLayout().getLineBottom(line - 1);
                int descent = getLayout().getLineDescent(line - 1);
                bottomDescent = bottom + descent;
            } else {
                bottomDescent = 0;
            }
//            Log.i("SN @@@", "RichEditText line: " + line + " y: " + y);
        }

        return ret;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        int newW = w - (int) leftMargin;
        setWidth(newW);
    }

    //    @Override
//    protected void onScrollChanged(int horiz, int vert, int oldHoriz, int oldVert) {
//        super.onScrollChanged(horiz, vert, oldHoriz, oldVert);
//
//        if (recView == null) {
//            recView = (RecordingsView) findViewById(R.id.rec_view);
//            Log.i("SN @@@", "RichEditText onscroll 1");
//            }
//        else {
//            Log.i("SN @@@", "RichEditText onscroll 2");
//            recView.scrollBy(horiz - oldHoriz, vert - oldVert);
//        }
//    }
}
