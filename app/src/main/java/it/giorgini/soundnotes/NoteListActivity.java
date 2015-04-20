package it.giorgini.soundnotes;

import java.io.File;
import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ShareCompat;
import android.support.v4.content.FileProvider;
import android.support.v4.content.LocalBroadcastManager;
import android.text.Editable;
import android.text.InputType;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.Toast;
import android.support.v7.app.ActionBarActivity;

/**
 * An activity representing a list of Notes. This activity has different
 * presentations for handset and tablet-size devices. On handsets, the activity
 * presents a list of items, which when touched, lead to a
 * {@link NoteDetailActivity} representing item details. On tablets, the
 * activity presents the list of items and item details side-by-side using two
 * vertical panes.
 * <p>
 * The activity makes heavy use of fragments. The list of items is a
 * {@link NoteListFragment} and the item details (if present) is a
 * {@link NoteDetailFragment}.
 * <p>
 * This activity also implements the required {@link NoteListFragment.Callbacks}
 * interface to listen for item selections.
 */
public class NoteListActivity extends ActionBarActivity implements View.OnClickListener, NoteListFragment.Callbacks, NoteDetailFragment.Callbacks {
//    public static String PACKAGE_NAME;
    private static boolean mTwoPane = false; // Modalità doppia per tablet
    private WeakReference<NoteDetailFragment> detailFragment; // Solo se mTwoPane è definito
    private WeakReference<NoteListFragment> listFragment; // fragment per visualizzare una lista di elementi
    private RecordingsView recordingsView;
    private WeakReference<RichEditText> editText;
    private WeakReference<MenuItem> playIcon;

//    private Menu menu; // L'oggetto menu dell'action bar che contiene tutti i bottoni.

    private boolean deletingState = false; // Indica che l'elemento selezionato dal ListFragment è quello attualmente visualizzato.
    // mi serve questa variabile perchè il fragment chiama un metodo di NoteListActivity che deve comportarsi in modi differenti.

    public static final String DATA_PREFS = "DataPreferences";
//    public static final String APP_PREFS = "MyPreferences";
	
//	private NotesStorage ns = new NotesStorage(new WeakReference<NoteListActivity>(this));
		
	// Metodi Inizializzazione
	@Override
	protected void onCreate(Bundle savedInstanceState) {
        Log.i("SN ###", "NoteListActivity onCreate");
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_note_list);

        getSupportActionBar().setDisplayUseLogoEnabled(true);

//        SharedPreferences app_prefs = getSharedPreferences (APP_PREFS, Context.MODE_PRIVATE);
        SharedPreferences data_prefs = getSharedPreferences (DATA_PREFS, Context.MODE_PRIVATE);

        listFragment = new WeakReference<>((NoteListFragment) getFragmentManager().findFragmentById(R.id.note_list));

        if (findViewById(R.id.note_detail_container) != null) {
			// The detail container view will be present only in the large-screen layouts (res/values-xlarge and
			// res/values-sw600dp). If this view is present, then the activity should be in two-pane mode.
			mTwoPane = true;

			// In two-pane mode, list items should be given the 'activated' state when touched.
            listFragment.get().setActivateOnItemClick(true);
		}

        // Aggiorno mtwopane per listFragment
        listFragment.get().setTwoPane(mTwoPane);

//        // Setto l'orientamento di questa activity
//        if (!mTwoPane)
//            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        // Associo la pressione del fab alla creazione di una nuova nota
        ImageButton button = (ImageButton)findViewById(R.id.fab);
        button.setOnClickListener(this);

        // Carico le note dalla memoria interna
        StorageManager.init(this, data_prefs);
        boolean loadedSomething = StorageManager.load();

        // Non ho caricato note dalla memoria interna, sostituisco il fragment con quello vuoto (che istanzio adesso)
        swapVisibleFragment(!loadedSomething);

        // Cambio il colore dell'header nella schermata della app recenti perché altrimenti android scrive di nero su arancione :(
        //FIXME: colore testo app recenti da cambiare (colore testo app recenti)
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
//            Bitmap icon = BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher);
//            this.setTaskDescription(new ActivityManager.TaskDescription(null, null, R.color.primaryDark));
//        }
	}

    @Override
    protected void onStart() {
        super.onStart();

        // Mi registro agli intent del service
        if (mTwoPane) {
            LocalBroadcastManager bManager = LocalBroadcastManager.getInstance(this);
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(RecorderService.REC_START_REQUEST);
            intentFilter.addAction(RecorderService.REC_STOPPED);
            intentFilter.addAction(RecorderService.PLAYER_STARTED);
            intentFilter.addAction(RecorderService.PLAYER_STOPPED);
            bManager.registerReceiver(bReceiver, intentFilter);
        }
    }

    @Override
    protected void onResume() {
        Log.i("SN ###", "NoteListActivity onResume");
        super.onResume();

        // Il service forse potrebbe non essere più attivo (solo tablet).
        if (mTwoPane) {
            Intent i = new Intent(this, RecorderService.class);
            i.putExtra("mTwoPane", mTwoPane);
            i.putExtra("mainPath", getFilesDir().getAbsolutePath()); // il percorso principale dove ci sono i miei dati
            startService(i);
        }

        // aggiorno l'icona del registratore nella lista delle note. Per cellulare
        StorageManager.toggleRecState();
    }

    private BroadcastReceiver bReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(RecorderService.REC_START_REQUEST)) {
                Log.d("SN @@@", "broadRec - onNewIntent rec started");
                if (recordingsView != null) {
                    Log.d("SN @@@", "recView != null -1-, ok, niente asynctask");
                    boolean canRecord = recordingsView.newRecording();
                    if (canRecord) {
                        // lo dico al service con un intent
                        Intent i = new Intent(context, RecorderService.class);
                        i.setAction(RecorderService.ACTION_START_ACCEPTED);
                        startService(i);
                        StorageManager.toggleRecState();
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
            } else if (intent.getAction().equals((RecorderService.REC_STOPPED))) {
                long recTime = intent.getLongExtra(RecorderService.EXTRA_REC_TIME, 0);
                Log.d("SN @@@", "broadRec - onNewIntent rec stopped - " + recTime);
                recordingsView.stoppedRecording(recTime);
                // In questi due devo solo cambiare l'icona dell'action bar
            } else if (intent.getAction().equals(RecorderService.PLAYER_STARTED)) {
                setPlayerIcon(true, null);
            } else if (intent.getAction().equals(RecorderService.PLAYER_STOPPED)) {
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

    // Metodi per gestione dell'action bar
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
        // ombra della actionbar
        getSupportActionBar().setElevation(5);
//        // Inflate the menu items for use in the action bar
//	    MenuInflater inflater = getMenuInflater();
//	    inflater.inflate(R.menu.notelist_actions, menu);
	    return super.onCreateOptionsMenu(menu);
	}

    @Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    // Devo gestire la pressione dei tasti principali dell'action bar. Altri saranno gestiti dal fragment
        switch (item.getItemId()) {
//            case R.id.action_search:
//                openSearch();
//                return true;
//            case R.id.action_settings:
//                openSettings();
//                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
	}

    @Override
    protected void onStop() {
        Log.i("SN ###", "NoteListActivity onStop");
        super.onStop();

        // Tolgo la registrazione al broacast receiver
        if (mTwoPane) {
            LocalBroadcastManager bManager = LocalBroadcastManager.getInstance(this);
            bManager.unregisterReceiver(bReceiver);
        }

        // L'app non è più visibile: nessuna activity è visibile (nemmeno sotto un'altra):
        // - se sono su tablet e ho una nota da salvare lo faccio
        // - se non sto registrando rilascio il MediaRecorder
        if (!LifecycleHandler.isApplicationVisible()) {
            if (mTwoPane) {
                saveCurrentNote();
            }
            if (!RecorderService.isRecording() && !RecorderService.isPlaying()) {
                Log.d("SN ###", "stopService called from NoteListActivity");
                stopService(new Intent(this, RecorderService.class));
            }
        }
    }

    @Override
    protected void onDestroy() {
        Log.d("SN ###", "NoteListActivity onDestroy");
        StorageManager.clearNotesList();
        super.onDestroy();

        // Vengo chiamato per esempio quando chiudo l'app dalla lista delle recenti.
        // Devo terminare il service
        if (!LifecycleHandler.isApplicationVisible()) {
            Log.d("SN ###", "stopService called from NoteDetailActivity onDestroy");
            stopService(new Intent(this, RecorderService.class));
        }

        if (recordingsView != null)
            recordingsView.clear();
    }

//    @Override
//    public void onConfigurationChanged(Configuration newConfig) {
//        super.onConfigurationChanged(newConfig);
//
//        // Checks the orientation of the screen
//        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
//            Log.i("SN ###", "Service: rotated in landscape");
//        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT){
//            Log.i("SN ###", "Service: rotated in portrait");
//        }
//    }


    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        Log.i("SN ###", "NoteListActivity onWindowFocusChanged");
        super.onWindowFocusChanged(hasFocus);

        // controllo se ha il focus, allora aggiorno certi elementi dell'interfaccia (solo tablet, non tablet lo faccio nelle notedetailactivity)
        if (mTwoPane && hasFocus) {
            if (RecorderService.isRecording()) {
                if (detailFragment != null)
                    detailFragment.get().setRecordingIcon(true);
            } else {
                if (detailFragment != null)
                    detailFragment.get().setRecordingIcon(false);
            }

            if (RecorderService.isPlaying())
                setPlayerIcon(true, null);
            else
                setPlayerIcon(false, null);
        }
    }

    // salva la nota corrente.
    // NB: codice uguale alla metodo saveCurrentNote di NoteDetailActivity
    public void saveCurrentNote() {
        Log.i("SN ###", "NoteListActivity saveCurrentNote called");
        RichEditText ret = (RichEditText) findViewById(R.id.note_detail);
        if (ret != null) {
            String s = ret.getText().toString();
            StorageManager.save(this, s);
        } else {
            Log.d("SN ###", "NoteListActivity saveCurrentNote: EditText nota = null, non salvo nulla (forse è giusto, non sempre c'è una nota aperta su tablet");
        }
    }

    /** Questo metodo cambia il fragment visibile: lista di note / schermata vuota */
    public void swapVisibleFragment(boolean emptyList) {
        if (emptyList) {
            // sostituisco il fragment con la lista delle note con quello vuoto speciale
            if (mTwoPane) {
//                findViewById(R.id.note_detail_container).setVisibility(View.GONE);
                RelativeLayout recViewContainer = (RelativeLayout) findViewById(R.id.rec_view_container);
                recViewContainer.setVisibility(View.GONE);
            }
            findViewById(R.id.note_list).setVisibility(View.GONE);
            findViewById(R.id.empty_list).setVisibility(View.VISIBLE);
        } else {
            if (mTwoPane) {
//                findViewById(R.id.note_detail_container).setVisibility(View.VISIBLE);
                RelativeLayout recViewContainer = (RelativeLayout) findViewById(R.id.rec_view_container);
                recViewContainer.setVisibility(View.VISIBLE);
            }
            findViewById(R.id.empty_list).setVisibility(View.GONE);
            findViewById(R.id.note_list).setVisibility(View.VISIBLE);
        }

    }

    public void setDetailNoteMenuItems(boolean visibility) {
        detailFragment.get().setHasOptionsMenu(visibility);
        invalidateOptionsMenu();
    }

    public void updateDetailFragmentContent() {
        detailFragment.get().updateCurrItem();
    }

    private boolean save() {
        if (!StorageManager.ITEMS.isEmpty()) {
            String newText = ((RichEditText) this.findViewById(R.id.note_detail)).getText().toString();
            return StorageManager.save(this, newText);
        } else
            return false;
    }

    public void newNote() {
		// ALERT: chiedo titolo nuova nota
		AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setTitle(R.string.new_note_alert_title)
               .setMessage(R.string.new_note_alert_message);

        // creo campo input testo, lo imposto alla data corrente e lo seleziono per creare il titolo facilmente
        final EditText input = new EditText(this);
        String timeStamp = new SimpleDateFormat("dd MMM yyyy '" + getString(R.string.time_preposition) + "' HH:mm:ss", Locale.getDefault()).format(Calendar.getInstance().getTime());
        input.setSingleLine(true);
        input.setInputType(InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        input.setText(timeStamp);
        input.selectAll();

        builder.setView(input);

        // Imposto le azioni dell'alert
        builder.setPositiveButton(R.string.alert_key_ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                // funzione che salva la nota, crea la nuova (chiede all'utente il nome) e la apre
                NewNote_OKButton(input, null);
            }
        });

        builder.setNegativeButton(R.string.alert_key_cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                // nulla da fare
            }
        });

        // Registro un listener sul tasto invio che crea la nota col testo scritto (evito che si
        // possa andare accapo nel titolo
        builder.setOnKeyListener(new DialogInterface.OnKeyListener() {
            @Override
            public boolean onKey(DialogInterface dialogInterface, int keyCode, KeyEvent keyEvent) {
                if (keyEvent.getAction() == KeyEvent.ACTION_DOWN) {
                    switch(keyCode) {
                        case KeyEvent.KEYCODE_DPAD_CENTER:
                        case KeyEvent.KEYCODE_ENTER:
                        case KeyEvent.KEYCODE_NUMPAD_ENTER:
                            // funzione che salva la nota, crea la nuova (chiede all'utente il nome) e la apre
                            NewNote_OKButton(input, dialogInterface);
                            break;
                        default:
                            return false;
                    }

                    return true;
                }
                return false;
            }
        });

		// mostro l'alert
        AlertDialog alert = builder.create();
        alert.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
		alert.show();
	}

    // funzione che gestisce la creazione di una nuova nota se l'utente ha inserito un nome e premuto ok
    public void NewNote_OKButton(EditText input, DialogInterface dialog) {
        // Salvo la nota corrente prima di creare la successiva (solo su tablet)
        if (mTwoPane)
            save();

        Editable value = input.getText();
        // Creo la nuova nota col titolo inserito o con quello di default
//        int id;
        if (value.length() != 0)
            StorageManager.add(value.toString(), "", System.nanoTime());
        else
            StorageManager.add(getResources().getString(R.string.new_note_name), "", System.nanoTime());

        // aggiorno la lista delle note
        // updateListAdapter(); // non dovrebbe più servire

        // Se ora c'è una sola nota devo ripristinare il fragment che visualizza la lista
        // e aggiungo di nuovo i tasti nella action bar se è la prima nota nella lista
        if (StorageManager.ITEMS.size() == 1) {
            swapVisibleFragment(false);
            if (detailFragment != null)
                setDetailNoteMenuItems(true);
        }

        // chiudo l'alert
        if (dialog != null)
            dialog.cancel();

        // faccio un click su quell'elemento della lista
        ListView lv = listFragment.get().getListView();
        ListAdapter la = lv.getAdapter();
        lv.performItemClick(la.getView(0, null, null), 0, la.getItemId(0));
    }

//	public void openSearch() {
//		//TODO: implementare ricerca
//		Toast toast = Toast.makeText(this, "tasto search", Toast.LENGTH_SHORT);
//		toast.show();
//	}
//
//	public void openSettings() {
//		//TODO: implementare impostazioni
//		Toast.makeText(this, "tasto settings", Toast.LENGTH_SHORT).show();
////        Object aa = PreferenceScreen.createPreferenceScreen(this);
//	}
	
	/**
     * Callback method from {@link NoteListFragment.Callbacks} indicating that
     * the item with the given ID was selected.
     */
	@Override
	public void onItemSelected(String id, int position) {
		if (mTwoPane) {
			// In two-pane mode, show the detail view in this activity by adding or replacing the detail fragment using a
			// fragment transaction.
            if (detailFragment == null) {
                Log.d("SN ###", "currFragment == null");
                StorageManager.updateCurrItem(position);
                detailFragment = new WeakReference<>(new NoteDetailFragment());
                detailFragment.get().setTwoPane(true);
                // lo piazzo
                getFragmentManager().beginTransaction().replace(R.id.note_detail_container, detailFragment.get()).commit();
            } else {
                // ho premuto su un'altra nota, salvo quella corrente e carico la successiva
//                if (!deletingState && StorageManager.getCurrNote() != null)
                if (!deletingState && StorageManager.getCurrNote() != null)
                    save();
                StorageManager.updateCurrItem(position);
                updateDetailFragmentContent();
                // Aggiorno la recView
                recordingsView.updateCurrItem(position);
            }

		} else {
			// In single-pane mode, simply start the detail activity
			// for the selected item ID after updating the current note in NotesStorage
            StorageManager.updateCurrItem(position);
			Intent detailIntent = new Intent(this, NoteDetailActivity.class);
            detailIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
            detailIntent.putExtra(RecorderService.EXTRA_TWO_PANE, mTwoPane);
			startActivity(detailIntent);
            overridePendingTransition(R.anim.push_in_from_right, R.anim.fade_out_stayleft);
		}
	}

    public void setDeletingState(boolean state) {
        deletingState = state;
    }

    @Override
    public void onBackPressed() {
        // Se sono su tablet il tasto back chiude l'app. Se non ho ancora salvato la nota lo faccio qui.
        if (mTwoPane)
            save();

        super.onBackPressed();
    }

    // Implement the OnClickListener callback
    public void onClick(View v) {
        newNote();
    }

    // returns true if current Android OS on device is >= verCode
    public static boolean deviceApiIsAtLeast(int verCode) {
        if (android.os.Build.VERSION.RELEASE.startsWith("4.1"))
            return 16 >= verCode;
        else if (android.os.Build.VERSION.RELEASE.startsWith("4.2")) {
            return 17 >= verCode;
        } else if (android.os.Build.VERSION.RELEASE.startsWith("4.3")) {
            return 18 >= verCode;
        } else if (android.os.Build.VERSION.RELEASE.startsWith("4.4")) {
            return 19 >= verCode;
        } else if (android.os.Build.VERSION.RELEASE.startsWith("5.")) {
            return 21 >= verCode;
        } else {
            Log.i("SN", "Application: device minimum must be upgraded for the newer build");
            return true;
        }
    }

    @Override
    public void returnToList() {

    }

    @Override
    public void initConnections(RichEditText ret) {
        if (mTwoPane) {
            recordingsView = (RecordingsView) findViewById(R.id.rec_view);
            this.editText = new WeakReference<>(ret);
            ret.setRecView(recordingsView);
            recordingsView.setAssociatedEditText(ret);

            // I also set the RichEditText's reference to this context and recView's edittextlinecount
            ret.setContext(this);
            ret.initRecViewDevLineCount();
        }
    }

    public void initRecList() {
        recordingsView.setCurrRecList();
    }

    @Override
    public void onPlayRequest(MenuItem item) {
        Log.i("SN ###", "NoteListActivity onPlayRequest");
        if (mTwoPane) {
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
    }

    @Override
    public void onPauseRequest(MenuItem item) {
        Log.i("SN ###", "NoteListActivity onPauseRequest");
        if (mTwoPane) {
            Intent i = new Intent(this, RecorderService.class);
            i.setAction(RecorderService.ACTION_PLAYER_PAUSE);
            i.putExtra("stop", false);
            startService(i);

            // cambio l'icona nella action bar nel tasto play
            setPlayerIcon(false, item);
        }
    }

    // Attenzione: metodo duplicato: FIXARE
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
        }
    }

    @Override
    public void onDeleteRecRequest() {
        Log.i("SN ###", "NoteListActivity onDeleteRequest");
        if (mTwoPane) {
            int line = editText.get().getCurrLine();
            final int recLine = recordingsView.lineBelongsToRecording(line);

            if (recLine != -1) {
                final RecordingsView.Recording rec = StorageManager.getCurrNote().recList.get(recLine);
                String recLineText = editText.get().getLineText(recLine);
                // ALERT: chiedo se vuole eliminare
                AlertDialog.Builder builder = new AlertDialog.Builder(this);

                builder.setTitle(R.string.delete_rec_title)
                        .setMessage(rec.lenghtFormatted + " - " + recLineText);

                // Imposto le azioni dell'alert
                builder.setPositiveButton(R.string.alert_key_delete, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        recordingsView.removeRecAt(rec, recLine);
                    }
                });

                builder.setNegativeButton(R.string.alert_key_cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        // nulla da fare
                    }
                });

                // mostro l'alert
                AlertDialog alert = builder.create();
                alert.show();
            } else {
                Toast.makeText(this, R.string.rec_forbidden_del_rec, Toast.LENGTH_LONG).show();
            }
        }
    }

    @SuppressLint("InlinedApi")
    @SuppressWarnings("deprecation")
    @Override
    public void onShareCurrRec() {
        Log.i("SN ###", "NoteListActivity onShareCurrRec");
        if (mTwoPane) {
            final int line = editText.get().getCurrLine();
            final int recLine = recordingsView.lineBelongsToRecording(line);

            if (recLine != -1) {
                final StorageManager.SoundNote currNote = StorageManager.getCurrNote();
                final RecordingsView.Recording rec = currNote.recList.get(recLine);
                final File file = new File(new File(this.getFilesDir(),
                        StorageManager.currID),
                        rec.position + "-" + rec.lenghtMillis + ".aac");
                // l'uri ottenuta da un file provider affinché l'app che apre il file condiviso abbia i permessi per farlo
                final Uri uri = FileProvider.getUriForFile(this, "it.giorgini.soundnotes.FileProvider", file);

                final Intent intent = ShareCompat.IntentBuilder.from(this)
                        .setType("audio/aac")
                        .setSubject(currNote.name)
                        .setStream(uri)
                        .setChooserTitle(R.string.action_share_curr_rec_chooser)
                        .createChooserIntent()
                        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                if (NoteListActivity.deviceApiIsAtLeast(Build.VERSION_CODES.LOLLIPOP))
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT);
                else
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);

                startActivity(intent);
            } else {
                Toast.makeText(this, R.string.rec_forbidden_share_curr_rec, Toast.LENGTH_LONG).show();
            }
        }
    }
}
