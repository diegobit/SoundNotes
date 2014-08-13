package it.giorgini.soundnotes;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.widget.EditText;

public class RichEditText extends EditText {

	private Paint lgrey;

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
		lgrey = new Paint();
//		lgrey.setARGB(255, 140, 140, 140);
        lgrey.setARGB(255, 200, 200, 200);
	}
	
	@Override
	protected void onDraw(@NonNull Canvas canvas) {
        canvas.drawLine(0, 5, 0, canvas.getHeight()-5, lgrey);
		super.onDraw(canvas);
	}
	
}
