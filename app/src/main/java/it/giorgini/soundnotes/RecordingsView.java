package it.giorgini.soundnotes;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Pair;
import android.view.View;

public class RecordingsView extends View {

    RichEditText editText;
    Paint lgrey = new Paint();
    Paint orange = new Paint();
    Paint orangeText = new Paint();
    Paint orangeShadow = new Paint();
    Paint recPaint;
    int recX;
    int recY;
    final float leftMargin = getResources().getDimension(R.dimen.rec_left_margin);
    final float smallLeftMargin = getResources().getDimension(R.dimen.rec_left_margin_small);
    final float vertMargin = getResources().getDimension(R.dimen.rec_vertical_margin);
    final float centerWidth = dp2px(23);
    final float rightMargin = dp2px(1);

    int currRecLine = 0;
    String deleteString;
    int interlinea;
    int baseline;
    int interlineaNext;

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
//        editText = (RichEditText) findViewById(R.id.note_detail);
//        setBackgroundResource(Color.TRANSPARENT);
        lgrey.setColor(getResources().getColor(R.color.line_lgrey));
        orangeText.setColor(getResources().getColor(R.color.primary));
        orangeText.setAntiAlias(true);
        orangeText.setTextSize(getResources().getDimensionPixelSize(R.dimen.rec_text));
        orangeText.setTextAlign(Paint.Align.CENTER);
        orange.setColor(getResources().getColor(R.color.line_orange));
        orangeShadow.setColor(getResources().getColor(R.color.line_shadow));
    }
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // linea grigia che separa il testo dalle registrazioni a sinistra
        canvas.drawLine(leftMargin, vertMargin, leftMargin, canvas.getHeight(), lgrey);

        // TESTO DI PROVA
//        if (recPaint != null) {
//            canvas.drawText("REC", centerWidth, interlinea, orangeText);
//            canvas.drawText("REC", recX, recY, recPaint);
//        canvas.drawText("REC", centerWidth, (interlineaNext+interlinea)/2, orangeText);
        canvas.drawText("REC", centerWidth, baseline, orangeText);
//        }

        // Linea PROVA SOPRA TESTO SELEZIONATO
        canvas.drawLine(smallLeftMargin, interlinea - 1, getWidth() - rightMargin, interlinea - 1, orangeShadow);
        canvas.drawLine(smallLeftMargin, interlinea, getWidth() - rightMargin, interlinea, orangeText);
        canvas.drawLine(smallLeftMargin, interlinea + 1, getWidth() - rightMargin, interlinea + 1, orangeShadow);




//        int x = getWidth();
//        int y = getHeight();
//        int radius;
//        radius = 100;
//        Paint paint = new Paint();
//        // Use Color.parseColor to define HTML colors
//        paint.setColor(Color.parseColor("#C8CD5C5C"));
//        canvas.drawCircle(x / 2, y / 2, radius, paint);
    }

    // Aggiunge esteticamente una registrazione nella nota
    public void newRecording() {
        Log.i("SN @@@", "RecordingsView newRec");
        currRecLine = editText.getCurrLine();
        if (currRecLine > 0) {
            int baselinePrev = editText.getLayout().getLineBaseline(currRecLine - 1);
            int descentPrev = editText.getLayout().getLineDescent(currRecLine - 1);
            interlinea = baselinePrev + descentPrev + Math.round(dp2px(1));

            baseline = editText.getLayout().getLineBaseline(currRecLine);
            int descent = editText.getLayout().getLineDescent(currRecLine);
            interlineaNext = baseline + descent + Math.round(dp2px(1));
            Integer x = 0;
            Integer y = 0;
            recPaint = computeStringCoord("REC", 0, interlinea, getWidth(), interlineaNext - interlinea, x, y);
            recX = x;
            recY = y;
        } else {
            interlinea = Math.round(dp2px(1));
        };
        //TODO: implementare (anche notelistactivity)
        invalidate();
    }

    public float dp2px(int dp) {
        float scale = getContext().getResources().getDisplayMetrics().density;
        Log.d("SN @@@", "RecView density: " + scale);
        return dp * scale + 0.5f;
    }

    public float px2dp(int px) {
        float scale = getContext().getResources().getDisplayMetrics().density;
        return (px - 0.5f) / scale;
    }

    public Paint computeStringCoord(String s, int topLeftX, int topLeftY, int width, int height, Integer resultX, Integer resultY) {
        Paint p = new Paint();
        // height * 0,7 mi piace
        int fontHeight = (int)(height * 0.7);

        p.setTextSize(fontHeight);
        p.setColor(getResources().getColor(R.color.primary));
        p.setTextAlign(Paint.Align.CENTER);
        String textToDraw = s;
        Rect bounds = new Rect();
        p.getTextBounds(textToDraw, 0, textToDraw.length(), bounds);
        resultX = topLeftX + width / 2;
        resultY = topLeftY + height / 2 + (bounds.bottom - bounds.top) / 2;
        return p;
    }

}