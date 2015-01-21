package it.giorgini.soundnotes;

import android.content.Intent;
import android.content.res.Configuration;
import android.media.MediaRecorder;
import android.os.Bundle;
//import android.support.v4.app.NavUtils;
import android.support.v7.app.ActionBarActivity;
import android.text.Editable;
import android.util.Log;
import android.widget.Toast;

/**
 * An activity representing a single Note detail screen. This activity is only
 * used on handset devices. On tablet-size devices, item details are presented
 * side-by-side with a list of items in a {@link NoteListActivity}.
 * <p>
 * This activity is mostly just a 'shell' activity containing nothing more than
 * a {@link NoteDetailFragment}.
 */
public class NoteDetailActivity extends ActionBarActivity implements NoteDetailFragment.Callbacks {
//    public boolean mTwoPane;
    NoteDetailFragment detailFragment;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_note_detail);

		// Show the Up button in the action bar.
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // ombra dell'actionbar
        getSupportActionBar().setElevation(4);

//        mTwoPane = getIntent().getExtras().getBoolean("mTwoPane");

		// savedInstanceState is non-null when there is fragment state saved from previous configurations of this activity
		// (e.g. when rotating the screen from portrait to landscape). In this case, the fragment will automatically be re-added
		// to its container so we don't need to manually add it.
		if (savedInstanceState == null) {
			// Create the detail fragment and add it to the activity using a fragment transaction.
			Bundle arguments = new Bundle();
			detailFragment = new NoteDetailFragment();
            detailFragment.setArguments(arguments);
			getFragmentManager().beginTransaction()
					.add(R.id.note_detail_container, detailFragment).commit();
		}
		
		// cambio il titolo nella action bar
		setTitle(StorageManager.currName);
	}

    @Override
    protected void onResume() {
        super.onResume();

        // Il service forse potrebbe non essere più attivo (solo tablet)
        Intent i = new Intent(this, RecorderManager.class);
//        i.putExtra("mTwoPane", mTwoPane);
        i.putExtra("mainPath", getFilesDir().getAbsolutePath()); // il percorso principale dove ci sono i miei dati
        startService(i);
    }

    @Override
    protected void onPause() {
        Log.i("SN ###", "NoteDetailActivity onPause");
        super.onPause();

//        // L'app non è più visibile: nessuna activity è visibile (nemmeno sotto un'altra). Due cose:
//        // - salvo la nota corrente
//        // - posso rilasciare il MediaRecorder (se non sto registrando)
//        if (!LifecycleHandler.isApplicationVisible()) {
//            saveCurrentNote();
//
//            if (RecorderManager.getState() != MRState.RECORDING) {
//                Log.d("SN ###", "stopService called from NoteDetailActivity");
//                stopService(new Intent(this, RecorderManager.class));
//            }
//        }
    }

    @Override
    protected void onStop() {
        Log.i("SN ###", "NoteDetailActivity onStop");
        super.onStop();

//        // L'app non è più visibile: nessuna activity è visibile (nemmeno sotto un'altra).
//        // posso rilasciare il MediaRecorder (se non sto registrando)
//        if (!LifecycleHandler.isApplicationVisible() && RecorderManager.getState() != MRState.RECORDING) {
//            Log.d("SN ###", "stopService called from NoteDetailActivity");
//            stopService(new Intent(this, RecorderManager.class));
//        }
        // L'app non è più visibile: nessuna activity è visibile (nemmeno sotto un'altra). Due cose:
        // - salvo la nota corrente
        // - posso rilasciare il MediaRecorder (se non sto registrando)
        if (!LifecycleHandler.isApplicationVisible()) {
            saveCurrentNote();

            if (RecorderManager.getState() != MRState.RECORDING) {
                Log.d("SN ###", "stopService called from NoteDetailActivity");
                stopService(new Intent(this, RecorderManager.class));
            }
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        // Checks the orientation of the screen
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            Log.i("SN ###", "Service: rotated in landscape");
        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT){
            Log.i("SN ###", "Service: rotated in portrait");
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        Log.i("SN ###", "NoteDetailActivity onWindowFocusChanged");
        super.onWindowFocusChanged(hasFocus);

        if (hasFocus) {
            if (RecorderManager.getState() == MRState.RECORDING)
                detailFragment.setRecordingIcon(true);
            else
                detailFragment.setRecordingIcon(false);
        }
    }

    @Override
    public void onBackPressed() {
        returnToList();
    }

    // Metodo per tornare alla lista delle note per cellulare (nuovo intent con animazione custom)
    public void returnToList() {
        saveCurrentNote();

        Intent intent = new Intent(this, NoteListActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);

        // Animazione apertura e chiusura lista note
        overridePendingTransition(R.anim.fade_in_stayleft, R.anim.push_out_to_right);
    }

    // salva la nota corrente.
    // NB: codice uguale alla metodo saveCurrentNote di NoteListActivity
    public void saveCurrentNote() {
        Log.i("SN ###", "NoteDetailActivity saveCurrentNote called");
        RichEditText ret = (RichEditText) findViewById(R.id.note_detail);
        if (ret != null) {
            String s = ret.getText().toString();
            StorageManager.save(this, s);
        } else {
            Log.d("SN ###", "NoteDetailActivity saveCurrentNote: EditText nota = null, non salvo nulla");
        }
    }
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
}
