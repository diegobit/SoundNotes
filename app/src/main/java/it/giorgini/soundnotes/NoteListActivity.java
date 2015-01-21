package it.giorgini.soundnotes;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.preference.PreferenceScreen;
import android.text.Editable;
import android.text.InputType;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListAdapter;
import android.widget.ListView;
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
public class NoteListActivity extends ActionBarActivity implements View.OnClickListener, NoteListFragment.Callbacks {
    public static String PACKAGE_NAME;
    private static boolean mTwoPane = false; // Modalità doppia per tablet
    private NoteDetailFragment detailFragment; // Solo se mTwoPane è definito // FIXME: e se lo mettessi in un WeakReference?
    private NoteListFragment listFragment; // fragment per visualizzare una lista di elementi

//    private Menu menu; // L'oggetto menu dell'action bar che contiene tutti i bottoni.

    private boolean deletingState = false; // Indica che l'elemento selezionato dal ListFragment è quello attualmente visualizzato.
    // mi serve questa variabile perchè il fragment chiama un metodo di NoteListActivity che deve comportarsi in modi differenti.

    public static final String DATA_PREFS = "DataPreferences";
    public static final String APP_PREFS = "MyPreferences";
	
//	private NotesStorage ns = new NotesStorage(new WeakReference<NoteListActivity>(this));
		
	// Metodi Inizializzazione
	@Override
	protected void onCreate(Bundle savedInstanceState) {
        Log.i("SN ###", "NoteListActivity onCreate");
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_note_list);

        SharedPreferences app_prefs = getSharedPreferences (APP_PREFS, Context.MODE_PRIVATE);
        SharedPreferences data_prefs = getSharedPreferences (DATA_PREFS, Context.MODE_PRIVATE);

        listFragment = (NoteListFragment) getFragmentManager().findFragmentById(R.id.note_list);

        if (findViewById(R.id.note_detail_container) != null) {
			// The detail container view will be present only in the large-screen layouts (res/values-large and
			// res/values-sw600dp). If this view is present, then the activity should be in two-pane mode.
			mTwoPane = true;

			// In two-pane mode, list items should be given the 'activated' state when touched.
            listFragment.setActivateOnItemClick(true);
		}

        listFragment.setTwoPane(mTwoPane);

        // Associo la pressione del fab alla creazione di una nuova nota
        ImageButton button = (ImageButton)findViewById(R.id.fab);
        button.setOnClickListener(this);

        // Carico le note dalla memoria interna
        StorageManager.init(this, data_prefs);
        boolean loadedSomething = StorageManager.load();

        // Non ho caricato note dalla memoria interna, sostituisco il fragment con quello vuoto (che istanzio adesso)
        swapVisibleFragment(!loadedSomething);

        // Cambio il colore dell'header nella schermata della app recenti perché altrimenti android scrive di nero su arancione :(
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
//            Bitmap icon = BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher);
//            this.setTaskDescription(new ActivityManager.TaskDescription(null, null, R.color.primaryDark));
//        }
	}

    @Override
    protected void onResume() {
        Log.i("SN ###", "NoteListActivity onResume");
        super.onResume();

        // Il service forse potrebbe non essere più attivo (solo tablet).
        if (mTwoPane) {
            Intent i = new Intent(this, RecorderManager.class);
            i.putExtra("mTwoPane", mTwoPane);
            i.putExtra("mainPath", getFilesDir().getAbsolutePath()); // il percorso principale dove ci sono i miei dati
            // TODO: devo ancora aggiornare l'id della nota... (?)
            startService(i);
        }

//        // Controllo se sono stato avviato da una notifica per andare ad una nota precisa in quel caso
//        // apro quella nota. Lo faccio nella onResume per assicurarmi di aver fatto tutto
//        Intent i = getIntent();
//        int pos = i.getIntExtra("openedNotePosition", -1);
//        if (pos != -1) {
//            // chiamo onItemSelected come se avessi selezionato davvero quella nota nella lista... mah
//            //TODO: cambiare modo di accedere alla nota dalla notifica
//            onItemSelected(null, pos);
//        }
    }

    // Metodi per gestione dell'action bar
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
        // ombra della actionbar
        getSupportActionBar().setElevation(4);
        // Inflate the menu items for use in the action bar
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.notelist_actions, menu);
//        this.menu = menu; // aggiorno la variabile menu per poter disabilitare alcuni tasti
	    return super.onCreateOptionsMenu(menu);
	}

    @Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    // Devo gestire la pressione dei tasti principali dell'action bar. Altri saranno gestiti dal fragment
        switch (item.getItemId()) {
            case R.id.action_search:
                openSearch();
                return true;
            case R.id.action_settings:
                openSettings();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
	}

//    @Override
//    protected void onStart() {
//        super.onStart();
//    }

//    @Override
//    protected void onResume() {
//        super.onResume();
//    }

//    @Override
//    protected void onPause() {
//        super.onPause();
//    }

    @Override
    protected void onStop() {
        Log.i("SN ###", "NoteListActivity onStop");
        super.onStop();

        // L'app non è più visibile: nessuna activity è visibile (nemmeno sotto un'altra):
        // - se sono su tablet e ho una nota da salvare lo faccio
        // - se non sto registrando rilascio il MediaRecorder
        if (!LifecycleHandler.isApplicationVisible()) {
            if (mTwoPane) {
                saveCurrentNote();
            }
            if (RecorderManager.getState() != MRState.RECORDING) {
                Log.d("SN ###", "stopService called from NoteListActivity");
                stopService(new Intent(this, RecorderManager.class));
            }
        }
    }

    @Override
    protected void onDestroy() {
        StorageManager.clear();
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

//    @Override
//    protected void onNewIntent(Intent intent) {
//        Log.i("SN ###", "onNewIntent");
//        super.onNewIntent(intent);
//
//        // Controllo se sono stato avviato da una notifica per andare ad una nota precisa in quel caso
//        // apro quella nota. Lo faccio nella onResume per assicurarmi di aver fatto tutto
//        int pos = intent.getIntExtra("openedNotePosition", -1);
//        if (pos != -1) {
//            // chiamo onItemSelected come se avessi selezionato davvero quella nota nella lista... mah
//            //TODO: cambiare modo di accedere alla nota dalla notifica
//            onItemSelected(null, pos);
//        }
//    }

    //    @Override
//    protected void onRestart() {
//        super.onRestart();
//    }

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

    /** Questo metodo cambia il fragment visibile: lista di note / schermata vuota
     */
    public void swapVisibleFragment(boolean emptyList) {
        if (emptyList) {
            // sostituisco il fragment con la lista delle note con quello vuoto speciale
            if (mTwoPane)
                findViewById(R.id.note_detail_container).setVisibility(View.GONE);
            findViewById(R.id.note_list).setVisibility(View.GONE);
            findViewById(R.id.empty_list).setVisibility(View.VISIBLE);
        } else {
            if (mTwoPane)
                findViewById(R.id.note_detail_container).setVisibility(View.VISIBLE);
            findViewById(R.id.empty_list).setVisibility(View.GONE);
            findViewById(R.id.note_list).setVisibility(View.VISIBLE);
        }

    }

    public void setDetailNoteMenuItems(boolean visibility) {
        detailFragment.setHasOptionsMenu(visibility);
        invalidateOptionsMenu();
    }

    public void updateDetailFragmentContent() {
        detailFragment.updateCurrItem();
    }

    private boolean save() {
        if (!StorageManager.ITEMS.isEmpty()) {
            String newText = ((RichEditText) this.findViewById(R.id.note_detail)).getText().toString();
            return StorageManager.save(this, newText);
        } else
            return false;
    }

    public void newNote() {

//        Display display = getWindowManager().getDefaultDisplay();
//        DisplayMetrics outMetrics = new DisplayMetrics();
//        display.getMetrics(outMetrics);
//
//        float density  = getResources().getDisplayMetrics().density;
//        float dpHeight = outMetrics.heightPixels / density;
//        float dpWidth  = outMetrics.widthPixels / density;
//
//        Log.d("DEBUG","••••• dens:" + density + " - dpH:" + dpHeight + " - dpW:" + dpWidth);

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
        //                       updateListAdapter(); // non dovrebbe più servire
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
        ListView lv = listFragment.getListView();
        ListAdapter la = lv.getAdapter();
        lv.performItemClick(la.getView(0, null, null), 0, la.getItemId(0));
    }

	public void openSearch() {
		//TODO: implementare (ricerca)
		Toast toast = Toast.makeText(this, "tasto search", Toast.LENGTH_SHORT);
		toast.show();
	}
	
	public void openSettings() {
		//TODO: implementare (impostazioni)
		Toast.makeText(this, "tasto settings", Toast.LENGTH_SHORT).show();

//        Object aa = PreferenceScreen.createPreferenceScreen(this);
	}
	
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
                detailFragment = new NoteDetailFragment();
                detailFragment.setTwoPane(true);
                getFragmentManager().beginTransaction().replace(R.id.note_detail_container, detailFragment).commit();
            } else {
                Log.d("SN ###", "currFragment != null - testoCorrente: '" + ((RichEditText) this.findViewById(R.id.note_detail)).getText().toString() + "'");
                // ho premuto su un'altra nota, salvo quella corrente e carico la successiva
                if (!deletingState && StorageManager.getCurrNote() != null)
                    save();
                StorageManager.updateCurrItem(position);
                updateDetailFragmentContent();
            }

		} else {
			// In single-pane mode, simply start the detail activity
			// for the selected item ID after updating the current note in NotesStorage
            StorageManager.updateCurrItem(position);
			Intent detailIntent = new Intent(this, NoteDetailActivity.class);
            detailIntent.putExtra("mTwoPane", mTwoPane);
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
}
