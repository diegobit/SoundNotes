package it.giorgini.soundnotes;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
//import android.support.v4.app.NavUtils;
import android.support.v7.app.ActionBarActivity;
import android.view.GestureDetector;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.TranslateAnimation;
import android.widget.Toast;

import java.security.InvalidParameterException;
import java.util.ArrayList;

/**
 * An activity representing a single Note detail screen. This activity is only
 * used on handset devices. On tablet-size devices, item details are presented
 * side-by-side with a list of items in a {@link NoteListActivity}.
 * <p>
 * This activity is mostly just a 'shell' activity containing nothing more than
 * a {@link NoteDetailFragment}.
 */
public class NoteDetailActivity extends ActionBarActivity implements NoteDetailFragment.Callbacks {
//    private GestureDetector gestureDetector; // Per ascoltare le gesture del touchpad
//    private View.OnTouchListener gestureListener;

//	private GestureDetector mDetector; 
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_note_detail);

		// Show the Up button in the action bar.
        //noinspection ConstantConditions
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // ombra dell'actionbar
        getSupportActionBar().setElevation(4);

		// savedInstanceState is non-null when there is fragment state
		// saved from previous configurations of this activity
		// (e.g. when rotating the screen from portrait to landscape).
		// In this case, the fragment will automatically be re-added
		// to its container so we don't need to manually add it.
		if (savedInstanceState == null) {
			// Create the detail fragment and add it to the activity
			// using a fragment transaction.
			Bundle arguments = new Bundle();
//			arguments.putString(NoteDetailFragment.ARG_ITEM_ID, getIntent()
//					.getStringExtra(NoteDetailFragment.ARG_ITEM_ID));
			NoteDetailFragment fragment = new NoteDetailFragment();
			fragment.setArguments(arguments);
			getFragmentManager().beginTransaction()
					.add(R.id.note_detail_container, fragment).commit();
		}
		
		// cambio il titolo nella action bar
		setTitle(StorageManager.currName);

//        // Gestione slide per nascondere / mostrare la lista delle note su tablet
//        gestureDetector = new GestureDetector(getParent(), new SwipeGestureListener(this));
//
//        gestureListener = new View.OnTouchListener() {
//            public boolean onTouch(View v, MotionEvent event) {
//                if (gestureDetector.onTouchEvent(event)) {
//                    return true;
//                }
//                return false;
//            }
//        };

        // Animazione apertura e chiusura nota
//        overridePendingTransition(R.anim.push_in_from_right, R.anim.fade_out_stayleft);
	}

    @Override
    protected void onResume() {
        super.onResume();

        // Il service forse potrebbe non essere più attivo (solo tablet)
        Intent i = new Intent(this, RecorderManager.class);
        i.setAction(RecorderManager.ACTION_SERVICE_INIT);
        i.putExtra("mainPath", getFilesDir().getAbsolutePath()); // il percorso principale dove ci sono i miei dati
        startService(i);
    }

    // Opzioni nell'action bar
//	@Override
//	public boolean onCreateOptionsMenu(Menu menu) {
//	    // Inflate the menu items for use in the action bar
//	    MenuInflater inflater = getMenuInflater();
//	    inflater.inflate(R.menu.note_actions, menu);
//	    return super.onCreateOptionsMenu(menu);
//	}
	
//	// Eseguo un'azione a seconda di quale oggetto è stato premuto:
//	// una nota o l'action bar
//	@Override
//	public boolean onOptionsItemSelected(MenuItem item) {
//	    switch (item.getItemId()) {
//	    	case android.R.id.home:
//	    		// Up button
//                returnToList();
//				return true;
//	    	case R.id.action_rec:
//	            toggleRecording(item);
//	            return true;
//	        case R.id.action_share_text:
//	            shareFileAsText(((RichEditText) findViewById(R.id.note_detail)).getText().toString());
//	            return true;
//	        case R.id.action_share_full:
//	            shareFileFull(currFileUri); // TODO: implementare (tasto condivisione)
//	            return true;
//	        default:
//	            return super.onOptionsItemSelected(item);
//	    }
//	}

    @Override
    public void onBackPressed() {
        returnToList();
//        super.onBackPressed();
    }

    // Metodo per tornare alla lista delle note per cellulare (nuovo intent con animazione custom)
    public void returnToList() {
        StorageManager.save(this, ((RichEditText) findViewById(R.id.note_detail)).getText().toString());
//        NavUtils.navigateUpTo(this, new Intent(this, NoteListActivity.class));

        Intent intent = new Intent(this, NoteListActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);

        // Animazione apertura e chiusura lista note
//        overridePendingTransition(R.anim.push_in_from_left, R.anim.fade_out_stayright);
        overridePendingTransition(R.anim.fade_in_stayleft, R.anim.push_out_to_right);
    }

//    @Override
//    public void setOnTouchEventListenerTo() {
//        View cont = findViewById(R.id.note_detail_container);
//        View det = findViewById(R.id.note_detail);
//        cont.setOnTouchListener(gestureListener);
//        det.setOnTouchListener(gestureListener);
////        for (View view : views) {
////            view.setOnTouchListener(gestureListener);
////        }
//    }

//	// Funzione ricerca all'interno della nota
//	public void openSearch() {
//		//TODO: implementare (ricerca)
//		Toast toast = Toast.makeText(this, "tasto search", Toast.LENGTH_SHORT);
//		toast.show();
//	}
	
//	// METODI EVENTI TOUCH
//	@Override 
//    public boolean onTouchEvent(MotionEvent event) { 
//		findViewById(R.id.note_list).onTouchEvent(event);
//        return super.onTouchEvent(event);
//    }
//

//	// Nasconde la ListView in modalità tablet
//	public void slideToLeft(int newVisibility) {
//        // FIXME: funziona solo in una striscetta. SlideToRight non funziona più da quanto ho aggiunto il fragment vuota
//		if (newVisibility == View.VISIBLE || newVisibility == View.INVISIBLE ||
//				newVisibility == View.GONE) {
//
//            // Animo e nascondo la lista
//            View v = findViewById(R.id.note_list);
//            TranslateAnimation animate = new TranslateAnimation(0,-v.getWidth(),0,0);
//            animate.setDuration(200);
//            animate.setFillAfter(true);
//            v.startAnimation(animate);
//			v.setVisibility(newVisibility);
//		}
//		else throw new InvalidParameterException("The new visibility specified is invalid");
//	}
//
//	// Riattiva la ListView in modalità tablet
//	public void slideToRight(int newVisibility) {
//		if (newVisibility == View.VISIBLE || newVisibility == View.INVISIBLE ||
//				newVisibility == View.GONE) {
//
//            // Animo e mostro la lista
//            View v = findViewById(R.id.note_list);
//            TranslateAnimation animate = new TranslateAnimation(-v.getWidth(),0,0,0);
//            animate.setDuration(200);
//            animate.setFillAfter(true);
//            v.startAnimation(animate);
//            v.setVisibility(newVisibility);
//		}
//		else {
//			throw new InvalidParameterException("The new visibility specified is invalid");
//		}
//	}


//	// Nasconde la ListView
//	public void slideToLeft(View view){
//		TranslateAnimation animate = new TranslateAnimation(0,-view.getWidth(),0,0);
//		animate.setDuration(500);
//		animate.setFillAfter(true);
//		view.startAnimation(animate);
//		view.setVisibility(View.GONE);
//		}
//
//	// Riattiva la ListView
//	public void slideToRight(View view){
//		view.setVisibility(View.VISIBLE);
//		TranslateAnimation animate = new TranslateAnimation(0,view.getWidth(),0,0);
//		animate.setDuration(500);
//		animate.setFillAfter(true);
//		view.startAnimation(animate);
//	}
	
//	@Override
//	public boolean onDown(MotionEvent e) {
//		Log.d("TouchEvent", "onDown: " + e.toString());
//		return true;
//	}
//
//	@Override
//	public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
//			float velocityY) {
//		return false;
//	}
//
//	@Override
//	public void onLongPress(MotionEvent e) {
//	}
//
//	@Override
//	public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX,
//			float distanceY) {
//		if (distanceX > 10) {
//			Log.d("TouchEvent", "onFling: " + e1.toString()+e2.toString());
//		}
//		return true;
//	}
//
//	@Override
//	public void onShowPress(MotionEvent e) {
//	}
//
//	@Override
//	public boolean onSingleTapUp(MotionEvent e) {
//		return false;
//	}
	
}
