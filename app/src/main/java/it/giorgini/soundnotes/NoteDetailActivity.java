package it.giorgini.soundnotes;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
//import android.support.v4.app.NavUtils;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Toast;

import java.lang.ref.WeakReference;

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
    private WeakReference<NoteDetailFragment> detailFragment;
    private WeakReference<RichEditText> editText;
//    RichEditText noteDetailView;
    private RecordingsView recordingsView;
    private WeakReference<MenuItem> playIcon;
    //Your activity will respond to this action String
    public static final String REC_START_REQUEST = "it.giorgini.soundnotes.recstartrequest";
    public static final String REC_STOPPED = "it.giorgini.soundnotes.recstopped";
    public static final String PLAYER_STARTED = "it.giorgini.soundnotes.playerstarted";
    public static final String PLAYER_STOPPED = "it.giorgini.soundnotes.playerstopped";

    private BroadcastReceiver bReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(REC_START_REQUEST)) {
                Log.d("SN @@@", "broadRec - onNewIntent rec started");
                if (recordingsView != null) {
                    Log.d("SN @@@", "recView != null -1-, ok, niente asynctask");
                    boolean canRecord = recordingsView.newRecording();
                    if (canRecord) {
                        // lo dico al service con un intent
                        Intent i = new Intent(context, RecorderService.class);
                        i.setAction(RecorderService.ACTION_START_ACCEPTED);
                        startService(i);
                    } else {
                        // cambio l'icona del registratore nell'action bar
                        detailFragment.get().setRecordingIcon(false);
                    }
                } else {
                    // in questo asynctask aspetto che recordingsView venga inizializzato, poi chiamo
                    // recordingsView.get(). newRecording();
                    Log.d("SN @@@", "recView == null -2- , vado di asynctask");
                    new newRecordingSafe().execute("");
                }
            } else if (intent.getAction().equals((REC_STOPPED))) {
                long recTime = intent.getLongExtra(RecorderService.EXTRA_REC_TIME, 0);
                Log.d("SN @@@", "broadRec - onNewIntent rec stopped - " + recTime);
                recordingsView.stoppedRecording(recTime);
            // In questi due devo solo cambiare l'icona dell'action bar
            } else if (intent.getAction().equals(PLAYER_STARTED)) {
                setPlayerIcon(true, null);
            } else if (intent.getAction().equals(PLAYER_STOPPED)) {
                setPlayerIcon(false, null);
            }
        }
    };

    private class newRecordingSafe extends AsyncTask<String, Void, Void> {
        @Override
        protected Void doInBackground(String... params) {
            try {
                while (recordingsView == null) {
                    Log.d("SN @@@", "recView != null -2.5-, ancora null, richiamo wait");
                    Thread.sleep(500);
                }
                Log.d("SN @@@", "recView != null -3- finalmente!, eseguo newRecording");
                boolean canRecord = recordingsView.newRecording();
                if (canRecord) {
                    // lo dico al service con un intent
                    Intent i = new Intent(getApplicationContext(), RecorderService.class);
                    i.setAction(RecorderService.ACTION_START_ACCEPTED);
                    startService(i);
                } else {
                    // cambio l'icona del registratore nell'action bar
                    detailFragment.get().setRecordingIcon(false);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return null;
        }
        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
        }
    }

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
			detailFragment = new WeakReference<>(new NoteDetailFragment());
            detailFragment.get().setArguments(arguments);
            getFragmentManager().beginTransaction().replace(R.id.note_detail_container, detailFragment.get()).commit();
		}
		
		// cambio il titolo nella action bar
		setTitle(StorageManager.currName);

	}

    @Override
    protected void onStart() {
        super.onStart();

        Log.i("SN @@@", "noteDetAct onStart");

        // Mi registro agli intent del service
        LocalBroadcastManager bManager = LocalBroadcastManager.getInstance(this);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(REC_START_REQUEST);
        intentFilter.addAction(REC_STOPPED);
        intentFilter.addAction(PLAYER_STARTED);
        intentFilter.addAction(PLAYER_STOPPED);
        bManager.registerReceiver(bReceiver, intentFilter);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Il service forse potrebbe non essere più attivo (solo tablet)
        Intent i = new Intent(this, RecorderService.class);
//        i.putExtra("mTwoPane", mTwoPane);
//        i.putExtra(RecorderService.EXTRA_MAIN_PATH, getFilesDir().getAbsolutePath()); // il percorso principale dove ci sono i miei dati
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
//            if (RecorderService.getState() != MRState.RECORDING) {
//                Log.d("SN ###", "stopService called from NoteDetailActivity");
//                stopService(new Intent(this, RecorderService.class));
//            }
//        }
    }

    @Override
    protected void onStop() {
        Log.i("SN ###", "NoteDetailActivity onStop");
        super.onStop();

//        // L'app non è più visibile: nessuna activity è visibile (nemmeno sotto un'altra).
//        // posso rilasciare il MediaRecorder (se non sto registrando)
//        if (!LifecycleHandler.isApplicationVisible() && RecorderService.getState() != MRState.RECORDING) {
//            Log.d("SN ###", "stopService called from NoteDetailActivity");
//            stopService(new Intent(this, RecorderService.class));
//        }

        // Tolgo la registrazione al broacast receiver
        LocalBroadcastManager bManager = LocalBroadcastManager.getInstance(this);
        bManager.unregisterReceiver(bReceiver);

        // L'app non è più visibile: nessuna activity è visibile (nemmeno sotto un'altra). Due cose:
        // - salvo la nota corrente
        // - posso rilasciare il MediaRecorder (se non sto registrando)
        if (!LifecycleHandler.isApplicationVisible()) {
            saveCurrentNote();

            if (!RecorderService.isRecording() && !RecorderService.isPlaying()) {
                Log.d("SN ###", "stopService called from NoteDetailActivity onStop");
                stopService(new Intent(this, RecorderService.class));
            }
        }
    }

    @Override
    protected void onDestroy() {
        Log.d("SN ###", "NoteDetailActivity onDestroy");

        // Vengo chiamato per esempio quando chiudo l'app dalla lista delle recenti.
        // Devo terminare il service
        if (!LifecycleHandler.isApplicationVisible()) {
            Log.d("SN ###", "stopService called from NoteDetailActivity onDestroy");
            stopService(new Intent(this, RecorderService.class));
        }

        recordingsView.clear();

        super.onDestroy();
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
            if (RecorderService.isRecording())
                detailFragment.get().setRecordingIcon(true);
            else
                detailFragment.get().setRecordingIcon(false);

            if (RecorderService.isPlaying())
                setPlayerIcon(true, null);
            else
                setPlayerIcon(false, null);
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

    @Override
    public void initConnections(RichEditText editText) {
//        noteDetailView = editText;
        recordingsView = (RecordingsView) findViewById(R.id.rec_view);
        if (recordingsView != null) {
            Log.d("SN @@@", "recView != null -0- inizial da initCOnnections");
        }
        this.editText = new WeakReference<RichEditText>(editText);
        editText.setRecView(recordingsView);
        recordingsView.setAssociatedEditText(editText);

        // I also set the RichEditText's reference to this context and recView's edittextlinecount
        editText.setContext(this);
        editText.initRecViewDevLineCount();

        // Imposterei le note alla recordingsView ma lo faccio nella initReclist (ora i layout non sono definiti)
//        recView.setCurrRecList();
    }

    public void initRecList() {
//        recordingsView.setCurrRecList();
    }

    public void onPlayRequest(MenuItem item) {
        int line = editText.get().getCurrLine();
        int recLine = recordingsView.lineBelongsToRecording(line);
        if (recLine != -1) {
            Intent i = new Intent(this, RecorderService.class);
            i.setAction(RecorderService.ACTION_PLAYER_START);
            // calcolo il path della registrazione da riprodurre e la linea della rec (prima di spostarla... forse voglio
            // andare in pausa, potrei aver inserito/cancellato linee, devo sapere quella prima di queste azioni
            RecordingsView.Recording rec = StorageManager.getCurrNote().recList.get(recLine);
            String path = rec.position + "-" + rec.lenghtMillis + ".aac";
            i.putExtra("line", rec.position);
            i.putExtra("path", path);
            startService(i);

            // cambio l'icona nella action bar nel tasto della pausa
            setPlayerIcon(true, item);
        } else {
            Toast.makeText(this, R.string.rec_forbidden_play_empty, Toast.LENGTH_LONG).show();
        }
    }

    public void onPauseRequest(MenuItem item) {
        Intent i = new Intent(this, RecorderService.class);
        i.setAction(RecorderService.ACTION_PLAYER_PAUSE);
        i.putExtra("stop", false);
        startService(i);

        // cambio l'icona nella action bar nel tasto play
        setPlayerIcon(false, item);
    }

    public void setPlayerIcon(boolean playing, MenuItem playerNewItem) {
        // Aggiorno l'icona per le prossime volte
        if (playerNewItem != null)
            playIcon = new WeakReference<>(playerNewItem);

        // cambio l'icona nella action bar nel tasto play o pausa
        if (playIcon != null) {
            if (playing)
                playIcon.get().setIcon(R.drawable.ic_action_pause);
            else
                playIcon.get().setIcon(R.drawable.ic_action_play);
        } else
            Log.w("SN @@@", "NoteDetAct richiesto cambio di icona play ma playicon = null");
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
