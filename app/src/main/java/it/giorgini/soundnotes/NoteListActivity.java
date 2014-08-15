package it.giorgini.soundnotes;

import java.security.InvalidParameterException;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.util.Log;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.TranslateAnimation;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.Toast;

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
public class NoteListActivity extends Activity implements NoteListFragment.Callbacks {
	private boolean mTwoPane = false; // Modalità doppia per tablet
    private NoteDetailFragment detailFragment; // Solo se mTwoPane è definito // FIXME: e se lo mettessi in un WeakReference?
    private NoteListFragment listFragment; // fragment per visualizzare una lista di elementi

    private Menu menu; // L'oggetto menu dell'action bar che contiene tutti i bottoni.

    private boolean deletingState = false; // Indica che l'elemento selezionato dal ListFragment è quello attualmente visualizzato.
    // mi serve questa variabile perchè il fragment chiama un metodo di NoteListActivity che deve comportarsi in modi differenti.

	private GestureDetector mDetector; // Per ascoltare le gesture del touchpad
    public static final String DATA_PREFS = "DataPreferences";
    public static final String APP_PREFS = "MyPreferences";
	
//	private NotesStorage ns = new NotesStorage(new WeakReference<NoteListActivity>(this));
		
	// Metodi Inizializzazione
	@Override
	protected void onCreate(Bundle savedInstanceState) {
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

        // Carico le note dalla memoria interna
        NotesStorage.init(this, data_prefs);
        boolean loadedSomething = NotesStorage.load();

        // Non ho caricato note dalla memoria interna, sostituisco il fragment con quello vuoto (che istanzio adesso)
        swapVisibleFragment(!loadedSomething);

		// Gestione slide per nascondere / mostrare la lista delle note su tablet
        mDetector = new GestureDetector(getParent(), new SwipeGestureListener(this));
	}

	// Metodi per gestione dell'action bar
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
	    // Inflate the menu items for use in the action bar
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.notelist_actions, menu);
        this.menu = menu; // aggiorno la variabile menu per poter disabilitare alcuni tasti
	    return super.onCreateOptionsMenu(menu);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    // Devo gestire la pressione dei tasti dell'action bar distinguendo se sono su tablet o no.
        if (mTwoPane) {
            switch (item.getItemId()) {
                case R.id.action_new:
                    newNote();
                    return true;
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
        else {
            // Non tablet. Cellulare
            switch (item.getItemId()) {
                case R.id.action_new:
                    newNote();
                    return true;
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
////        NotesStorage.save(this, ((RichEditText) findViewById(R.id.note_detail)).getText().toString());
//        super.onPause();
//    }

//    @Override
//    protected void onStop() {
//        super.onStop();
//    }

    @Override
    protected void onDestroy() {
        NotesStorage.clear();
        super.onDestroy();
    }

//    @Override
//    protected void onRestart() {
//        super.onRestart();
//    }


    /** Questo metodo cambia il fragment visibile: lista di note / schermata vuota
     */
    public void swapVisibleFragment(boolean emptyList) {
        if (emptyList) {
            // sostituisco il fragment con la lista delle note...
//            getFragmentManager().beginTransaction().detach(getFragmentManager().findFragmentById(R.id.note_list));

            // ...con quello vuoto speciale
            if (mTwoPane)
                findViewById(R.id.note_detail_container).setVisibility(View.GONE);
            findViewById(R.id.note_list).setVisibility(View.GONE);
            Log.d("DEBUG", "#### Prima di attach nuovo fragment vuoto");
//            getFragmentManager().beginTransaction().attach(getFragmentManager().findFragmentById(R.id.empty_list));
            findViewById(R.id.empty_list).setVisibility(View.VISIBLE);
        } else {
//            getFragmentManager().beginTransaction().detach(getFragmentManager().findFragmentById(R.id.empty_list));
//            getFragmentManager().beginTransaction().attach(getFragmentManager().findFragmentById(R.id.note_list));
            if (mTwoPane)
                findViewById(R.id.note_detail_container).setVisibility(View.VISIBLE);
            findViewById(R.id.empty_list).setVisibility(View.GONE);
            findViewById(R.id.note_list).setVisibility(View.VISIBLE);
        }

    }

    public void setDetailNoteMenuItems(boolean visibility) {
        detailFragment.setHasOptionsMenu(visibility);
        invalidateOptionsMenu();
//        menu.findItem(R.menu.note_actions).setVisible(visibility);
    }

    public void updateDetailFragmentContent() {
        detailFragment.updateCurrItem();
    }

    private boolean save() {
        if (!NotesStorage.ITEMS.isEmpty())
            return NotesStorage.save(this, ((RichEditText) this.findViewById(R.id.note_detail)).getText().toString());
        else
            return false;
    }

    public void newNote() {
		// ALERT: chiedo titolo nuova nota
		AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setTitle(R.string.new_note_alert_title)
               .setMessage(R.string.new_note_alert_message);

        // creo campo input testo
        final EditText input = new EditText(this);
//        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input);

        // Imposto le azioni dell'alert
        builder.setPositiveButton(R.string.new_note_alert_ok, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    // Salvo la nota corrente prima di creare la successiva
                    save();

                    Editable value = input.getText();
                    // Creo la nuova nota col titolo inserito o con quelo di default
                    int id;
                    if (value.length() != 0)
                        id = NotesStorage.add(value.toString(), "", System.currentTimeMillis());
                    else
                        id = NotesStorage.add(getResources().getString(R.string.new_note_name), "", System.currentTimeMillis());

                    // aggiorno la lista delle note
         //                       updateListAdapter(); // non dovrebbe più servire
                    // Se ora c'è una sola nota devo ripristinare il fragment che visualizza la lista
                    // e aggiungo di nuovo i tasti nella action bar se è la prima nota nella lista
                    if (NotesStorage.ITEMS.size() == 1) {
                        swapVisibleFragment(false);
                        if (detailFragment != null)
                            setDetailNoteMenuItems(true);
                    }

                    // faccio un click su quell'elemento della lista
                    ListView lv = listFragment.getListView();
                    ListAdapter la = lv.getAdapter();
                    lv.performItemClick(la.getView(0, null, null), 0, la.getItemId(0));
                }
            });

//        builder.setNegativeButton(R.string.new_note_alert_cancel, new DialogInterface.OnClickListener() {
//                    public void onClick(DialogInterface dialog, int whichButton) { } });

        // Registro un listener sul tasto invio che crea la nota col testo scritto (evito che si
        // possa andare accapo nel titolo
        builder.setOnKeyListener(new DialogInterface.OnKeyListener() {
            @Override
            public boolean onKey(DialogInterface dialogInterface, int keyCode, KeyEvent keyEvent) {
                if (keyEvent.getAction() == KeyEvent.ACTION_DOWN) {
                    switch(keyCode) {
                        case KeyEvent.KEYCODE_ENTER:
                        case KeyEvent.KEYCODE_DPAD_CENTER:
                            //TODO: Codice duplicato 15 righe sopra
                            // Salvo la nota corrente prima di creare la successiva
                            save();

                            Editable value = input.getText();
                            // Creo la nuova nota col titolo inserito o con quello di default
                            int id;
                            if (value.length() != 0)
                                id = NotesStorage.add(value.toString(), "", System.currentTimeMillis());
                            else
                                id = NotesStorage.add(getResources().getString(R.string.new_note_name), "", System.currentTimeMillis());

                            // aggiorno la lista delle note
//                            updateListAdapter(); // non dovrebbe più servire

                            // Se ora c'è una sola nota devo ripristinare il fragment che visualizza la lista
                            // e aggiungo di nuovo i tasti nella action bar se è la prima nota nella lista
                            if (NotesStorage.ITEMS.size() == 1) {
                                swapVisibleFragment(false);
                                if (detailFragment != null)
                                    setDetailNoteMenuItems(true);
                            }

                            // chiudo l'alert
                            dialogInterface.cancel();

                            // faccio un click su quell'elemento della lista
                            ListView lv = listFragment.getListView();
                            ListAdapter la = lv.getAdapter();
                            lv.performItemClick(la.getView(0, null, null), 0, la.getItemId(0));

//                            // salvo la nota apena creata così da assicurarmi che sia stata creata correttamente e per
//                            // far funzionare subito altre funzioni, come la cancellazione
//                            save();
// FIXME: bug: se cancello una nota appena creata senza toccare nulla, non riesce, quindi devo prima salvarla ma così crasha
// (chiamata NotesStorage.save - nullpointerexc)
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
		alert.show();
	}
	
	public void openSearch() {
		//TODO: implementare (ricerca)
		Toast toast = Toast.makeText(this, "tasto search", Toast.LENGTH_SHORT);
		toast.show();
	}
	
	public void openSettings() {
		//TODO: implementare (impostazioni)
		Toast toast = Toast.makeText(this, "tasto settings", Toast.LENGTH_SHORT);
		toast.show();
	}
	
//	public void updateListAdapter() {
//		// aggiorno la lista delle note
//		ArrayAdapter <NotesStorage.SoundNote> ad = new ArrayAdapter<NotesStorage.SoundNote>(
//				this,
//				android.R.layout.simple_list_item_activated_1,
//				android.R.id.text1, NotesStorage.ITEMS);
//		((NoteListFragment) getFragmentManager().findFragmentById(R.id.note_list)).setListAdapter(ad);
//	}
	
	/**
     * Callback method from {@link NoteListFragment.Callbacks} indicating that
     * the item with the given ID was selected.
     */
	@Override
	public void onItemSelected(String id, int position) {
        // Salvo il contenuto della nota corrente.
//        RichEditText det = (RichEditText) findViewById(R.id.note_detail);
//        if (mTwoPane && NotesStorage.currPosition >= 0)
//            NotesStorage.save(this, ((RichEditText) findViewById(R.id.note_detail)).getText().toString());

        // Aggiorno lo stato interno di NotesStorage
//        NotesStorage.updateCurrItem(position);

		if (mTwoPane) {
            Log.d("DEBUG", "##### mTwoPane");
			// In two-pane mode, show the detail view in this activity by
			// adding or replacing the detail fragment using a
			// fragment transaction.
            if (detailFragment == null) {
                Log.d("DEBUG", "##### currFragment == null");
                NotesStorage.updateCurrItem(position);
//                Bundle arguments = new Bundle();
//			    arguments.putString(id);
                detailFragment = new NoteDetailFragment();
//			    fragment.setArguments(arguments);
                getFragmentManager().beginTransaction().replace(R.id.note_detail_container, detailFragment).commit();
            } else {
                Log.d("DEBUG", "##### currFragment != null - testoCorrente: '" + ((RichEditText) this.findViewById(R.id.note_detail)).getText().toString() + "'");
                // ho premuto su un'altra nota, salvo quella corrente e carico la successiva
                if (!deletingState && NotesStorage.getCurrNote() != null)
                    save();
                NotesStorage.updateCurrItem(position);
                updateDetailFragmentContent();
            }

		} else {
			// In single-pane mode, simply start the detail activity
			// for the selected item ID after updating the current note in NotesStorage
            NotesStorage.updateCurrItem(position);
			Intent detailIntent = new Intent(this, NoteDetailActivity.class);
//			detailIntent.putExtra(NoteDetailFragment.ARG_ITEM_ID, id);
			startActivity(detailIntent);
            overridePendingTransition(R.anim.push_in_from_right, R.anim.fade_out_stayleft);
		}
	}

    public void setDeletingState(boolean state) {
        deletingState = state;
    }



	// METODI EVENTI TOUCH
	@Override 
    public boolean onTouchEvent(MotionEvent event) { 
        if (mTwoPane)
        	this.mDetector.onTouchEvent(event);
        return super.onTouchEvent(event);
    }
	
	// Nasconde la ListView in modalità tablet
	public void slideToLeft(View view, int newVisibility) {
        // FIXME: funziona solo in una striscetta. SlideToRight non funziona più da quanto ho aggiunto il fragment vuota
		if (newVisibility == View.VISIBLE || newVisibility == View.INVISIBLE ||
				newVisibility == View.GONE) {
			TranslateAnimation animate = new TranslateAnimation(0,-view.getWidth(),0,0);
			animate.setDuration(200);
			animate.setFillAfter(true);
			view.startAnimation(animate);
			view.setVisibility(newVisibility);
		}
		else throw new InvalidParameterException("The new visibility specified is invalid");
	}
	
	// Riattiva la ListView in modalità tablet
	public void slideToRight(View view, int newVisibility) {
		if (newVisibility == View.VISIBLE || newVisibility == View.INVISIBLE ||
				newVisibility == View.GONE) {
			view.setVisibility(View.VISIBLE);
			TranslateAnimation animate = new TranslateAnimation(-view.getWidth(),0,0,0);
			animate.setDuration(200);
			animate.setFillAfter(true);
			view.startAnimation(animate);
		}
		else {
			throw new InvalidParameterException("The new visibility specified is invalid");
		}
	}
}
