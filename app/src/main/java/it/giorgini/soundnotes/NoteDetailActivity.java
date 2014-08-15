package it.giorgini.soundnotes;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
//import android.support.v4.app.NavUtils;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

/**
 * An activity representing a single Note detail screen. This activity is only
 * used on handset devices. On tablet-size devices, item details are presented
 * side-by-side with a list of items in a {@link NoteListActivity}.
 * <p>
 * This activity is mostly just a 'shell' activity containing nothing more than
 * a {@link NoteDetailFragment}.
 */
public class NoteDetailActivity extends Activity {
	private Uri currFileUri;
	private boolean isRecording = false;
	
//	private GestureDetector mDetector; 
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_note_detail);

		// Show the Up button in the action bar.
		getActionBar().setDisplayHomeAsUpEnabled(true);

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
		setTitle(NotesStorage.currName);

        // Animazione apertura e chiusura nota
//        overridePendingTransition(R.anim.push_in_from_right, R.anim.fade_out_stayleft);
	}

	// Opzioni nell'action bar
//	@Override
//	public boolean onCreateOptionsMenu(Menu menu) {
//	    // Inflate the menu items for use in the action bar
//	    MenuInflater inflater = getMenuInflater();
//	    inflater.inflate(R.menu.note_actions, menu);
//	    return super.onCreateOptionsMenu(menu);
//	}
	
	// Eseguo un'azione a seconda di quale oggetto è stato premuto:
	// una nota o l'action bar
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    switch (item.getItemId()) {
	    	case android.R.id.home:
	    		// Up button
                returnToList();
				return true;
	    	case R.id.action_rec:
	            toggleRecording(item);
	            return true;
	        case R.id.action_share_text:
	            shareFileAsText(((RichEditText) findViewById(R.id.note_detail)).getText().toString());
	            return true;
	        case R.id.action_share_full:
	            shareFileFull(currFileUri); // TODO: implementare (tasto condivisione)
	            return true;
	        default:
	            return super.onOptionsItemSelected(item);
	    }
	}

    @Override
    public void onBackPressed() {
        returnToList();
//        super.onBackPressed();
    }

    public void returnToList() {
        NotesStorage.save(this, ((RichEditText) findViewById(R.id.note_detail)).getText().toString());
//        NavUtils.navigateUpTo(this, new Intent(this, NoteListActivity.class));

        Intent intent = new Intent(this, NoteListActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);

        // Animazione apertura e chiusura lista note
//        overridePendingTransition(R.anim.push_in_from_left, R.anim.fade_out_stayright);
        overridePendingTransition(R.anim.fade_in_stayleft, R.anim.push_out_to_right);
    }

    // Funzione per iniziare (e fermare) la registrazione
	public void toggleRecording(MenuItem item) {
		//TODO: implementare (registrazione)
		
		isRecording = !isRecording;
		// cambio l'icona
		if(isRecording)
			item.setIcon(R.drawable.ic_action_mic_active);
		else
			item.setIcon(R.drawable.ic_action_mic);
		
		Toast toast = Toast.makeText(this, "tasto search", Toast.LENGTH_SHORT);
		toast.show();
	}
	
	// Funzione ricerca all'interno della nota
	public void openSearch() {
		//TODO: implementare (ricerca)
		Toast toast = Toast.makeText(this, "tasto search", Toast.LENGTH_SHORT);
		toast.show();
	}
	
	// Funzione che condivide il file corrente
	public void shareFileAsText(String text) {
		Intent sendIntent = new Intent();
		sendIntent.setAction(Intent.ACTION_SEND);
		sendIntent.putExtra(Intent.EXTRA_TEXT, text);
		sendIntent.setType("text/plain");
		startActivity(Intent.createChooser(
				sendIntent, getResources().getText(R.string.action_share_text_chooser)));
	}
	
	// Funzione che condivide il file corrente
	public void shareFileFull(Uri fileUri) {
		//TODO: implementare (condivisione file)
		Intent sendIntent = new Intent();
		sendIntent.setAction(Intent.ACTION_SEND);
		sendIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
		sendIntent.setType("multipart/");
		startActivity(Intent.createChooser(
				sendIntent, getResources().getText(R.string.action_share_full_chooser)));
	}
	
	
//	// METODI EVENTI TOUCH
//	@Override 
//    public boolean onTouchEvent(MotionEvent event) { 
//		findViewById(R.id.note_list).onTouchEvent(event);
//        return super.onTouchEvent(event);
//    }
//	
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
