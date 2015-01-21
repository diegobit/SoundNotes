package it.giorgini.soundnotes;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class RecordingsView extends View {

    RichEditText editText;
    Paint orange = new Paint();
    Paint orangeShadow = new Paint();

    public RecordingsView(Context context) {
        super(context);
        init();
    }

    public RecordingsView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public RecordingsView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

//    public RecordingsView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
//        super(context, attrs, defStyleAttr, defStyleRes);
//        init();
//    }

    private void init() {
        editText = (RichEditText) findViewById(R.id.note_detail);
        setBackgroundResource(Color.TRANSPARENT);
        if (editText != null) {
            orange.setColor(getResources().getColor(R.color.line_orange));
            orange.setAntiAlias(true);
            orange.setTextSize(12);
            orange.setTextAlign(Paint.Align.CENTER);
            orangeShadow.setColor(getResources().getColor(R.color.line_shadow));
        }
    }
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        String timeStamp = new SimpleDateFormat("dd/MM/yy HH:mm", Locale.getDefault()).format(Calendar.getInstance().getTime());

//        if (editText != null) {
//            Log.i("SN @@@", "RecView draw 1");
//            canvas.drawText(timeStamp, 23, editText.bottomDescent, orange);
//        } else {
//            Log.i("SN @@@", "RecView draw 2");
//            editText = (RichEditText) findViewById(R.id.note_detail);
//            orange.setColor(getResources().getColor(R.color.line_orange));
////            orange.setAntiAlias(true);
////            orange.setTextSize(12);
////            orange.setTextAlign(Paint.Align.CENTER);
//            orangeShadow.setColor(getResources().getColor(R.color.line_shadow));
//        }


        canvas.drawLine(0, 0, getWidth(), getHeight(), orange);




        int x = getWidth();
        int y = getHeight();
        int radius;
        radius = 100;
        Paint paint = new Paint();
        // Use Color.parseColor to define HTML colors
        paint.setColor(Color.parseColor("#CD5C5C"));
        canvas.drawCircle(x / 2, y / 2, radius, paint);
    }

}
