package it.giorgini.soundnotes;

import android.app.Activity;
import android.os.Bundle;
import android.app.ListFragment;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.Toast;

/**
 * A list fragment representing a list of Notes. This fragment also supports
 * tablet devices by allowing list items to be given an 'activated' state upon
 * selection. This helps indicate which item is currently being viewed in a
 * {@link NoteDetailFragment}.
 * <p>
 * Activities containing this fragment MUST implement the {@link Callbacks}
 * interface.
 */
public class NoteListFragment extends ListFragment {

    private int currentPressedItem = -1;

	/**
	 * The serialization (saved instance state) Bundle key representing the
	 * activated item position. Only used on tablets.
	 */
	private static final String STATE_ACTIVATED_POSITION = "activated_position";

	/**
	 * The fragment's current callback object, which is notified of list item
	 * clicks.
	 */
	private Callbacks mCallbacks = sStorageCallbacks;

	/**
	 * The current activated item position. Only used on tablets.
	 */
	private int mActivatedPosition = ListView.INVALID_POSITION;

    private boolean mTwoPane; // Modalità doppia per tablet

	/**
	 * A callback interface that all activities containing this fragment must
	 * implement. This mechanism allows activities to be notified of item
	 * selections.
	 */
	public interface Callbacks {
		/**
		 * Callback for when an item has been selected.
		 */
		public void onItemSelected(String id, int position);
        public void swapVisibleFragment(boolean emptyList);
        public void updateDetailFragmentContent();
        public void setDeletingState(boolean state);
        public void setDetailNoteMenuItems(boolean visibility);
	}

	/**
	 * Questo metodo viene chiamato solo se il fragment non è attaccato ad un'activity, altrimenti
     * viene chiamato il metodo dell'activity connessa.
	 */
	private static Callbacks sStorageCallbacks = new Callbacks() {
		@Override
        public void onItemSelected(String id, int position) {
        }

        @Override
        public void swapVisibleFragment(boolean emptyList) {
        }

        @Override
        public void updateDetailFragmentContent() {
        }

        @Override
        public void setDeletingState(boolean state) {
        }

        @Override
        public void setDetailNoteMenuItems(boolean visibility) {
        }
    };

	/**
	 * Mandatory empty constructor for the fragment manager to instantiate the
	 * fragment (e.g. upon screen orientation changes).
	 */
	public NoteListFragment() {
	}



    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        // Activities containing this fragment must implement its callbacks.
        if (!(activity instanceof Callbacks)) {
            throw new IllegalStateException("Activity must implement fragment's callbacks.");
        }

        mCallbacks = (Callbacks) activity;
    }

//	@Override
//	public void onCreate(Bundle savedInstanceState) {
//		super.onCreate(savedInstanceState);
//	}

//	@Override
//	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
//		view.setOnTouchListener(new View.OnTouchListener() {
//            public boolean onTouch(View v, MotionEvent event) {
//                if(event.getAction() == MotionEvent.ACTION_MOVE){
//                    //do something
//                }
//                return true;
//            }
//    });
//	}

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Restore the previously serialized activated item position.
        if (savedInstanceState != null && savedInstanceState.containsKey(STATE_ACTIVATED_POSITION)) {
            setActivatedPosition(savedInstanceState.getInt(STATE_ACTIVATED_POSITION));
        }

        // Registro un listener sugli eventi touch che passerò all'activity
        view.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                getActivity().onTouchEvent(event);
                return true;
            }
        });
    }

    // Metodo chiamato quando l'activity a cui è collegato viene creata. Serve per fare cose quando
    // tutto è inizializzato oppure se voglio mantenerlo anche quando lo stacco da un'activity
    @Override
    public void onActivityCreated(Bundle savedState) {
        super.onActivityCreated(savedState);

        setListAdapter(NotesStorage.ITEMS_ADAPTER);

        // registra un menu contestuale da visualizzare (quando su una nota avviene un logpress)
        registerForContextMenu(getListView());
    }

    // onViewStateRestored
    // onStart
    // onResume
    // onPause

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mActivatedPosition != ListView.INVALID_POSITION) {
            // Serialize and persist the activated item position.
            outState.putInt(STATE_ACTIVATED_POSITION, mActivatedPosition);
        }
    }

    // onStop
    // onDestroyView
    // onDestroy

	@Override
	public void onDetach() {
		super.onDetach();
		// Reset the active callbacks interface to the dummy implementation.
		mCallbacks = sStorageCallbacks;
	}



    public void setTwoPane(boolean twoPane) {
        mTwoPane = twoPane;
    }

    /**
     * Turns on activate-on-click mode. When this mode is on, list items will be
     * given the 'activated' state when touched.
     */
    public void setActivateOnItemClick(boolean activateOnItemClick) {
        // When setting CHOICE_MODE_SINGLE, ListView will automatically
        // give items the 'activated' state when touched.
        getListView().setChoiceMode(activateOnItemClick ? ListView.CHOICE_MODE_SINGLE : ListView.CHOICE_MODE_NONE);
    }

    private void setActivatedPosition(int position) {
        if (position == ListView.INVALID_POSITION) {
            getListView().setItemChecked(mActivatedPosition, false);
        } else {
            getListView().setItemChecked(position, true);
        }

        mActivatedPosition = position;
    }



    // Click su nota nella lista
    @Override
    public void onListItemClick(ListView listView, View view, int position, long id) {
        super.onListItemClick(listView, view, position, id);

        // Notify the active callbacks interface (the activity, if the
        // fragment is attached to one) that an item has been selected.
        mCallbacks.onItemSelected(NotesStorage.getNoteFromPosition(position).id, position);

    }

    // Creazione menu contestuale long press su nota in lista
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);

        // Mi salvo l'elemento premuto per saperlo quando l'utente sceglierà qualcosa dal menu contestuale
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
        currentPressedItem = info.position;

        // Titolo del menu e aggiungo le opzioni
        NotesStorage.SoundNote n = NotesStorage.getNoteFromPosition(info.position);
        menu.setHeaderTitle(n.name);
        menu.add(Menu.NONE, R.id.action_delete, Menu.NONE, R.string.action_delete);
    }

    // E' stato premuto un elemento del menu contestuale.
    @Override
    public boolean onContextItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_delete:
                deleteNote();
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }



    // Cancella la nota SELEZIONATA nel MENU CONTESTUALE chiamato tenendo premuto su una nota.
    private void deleteNote() {
        boolean deleted = NotesStorage.delete(getActivity(), currentPressedItem);

        // Non è riuscito a cancellarlo, notifico l'utente
        if (!deleted) {
            (Toast.makeText(getActivity(), R.string.action_delete_fail, Toast.LENGTH_LONG)).show();
        } else if (NotesStorage.ITEMS.isEmpty()) {
            // se ho cancellato e la lista è vuota
            mActivatedPosition = ListView.INVALID_POSITION;
            NotesStorage.updateCurrItem(-1);
            currentPressedItem = -1;
            // Aggiorno il contenuto del fragment (solo su tablet) e sistemo i bottoni dell'action bar
            if (mTwoPane) {
                mCallbacks.updateDetailFragmentContent();
                // Devo rimuovere dall'action bar i bottoni relativi alla nota aperta.
                mCallbacks.setDetailNoteMenuItems(false);
//                MenuItem detailNoteMenus = MenuItem.findItem(R.id.note_actions); refreshItem.setVisible(false);
            }
            //  sostituisco il fragment della nota aperta con quello vuoto
            mCallbacks.swapVisibleFragment(true);
        } else {
            // Se ho cancellato una nota e ci sono note nella lista aggiorno la nota corrente nel NotesStorage.
            // Ma SOLO se la nota corrente è quella che l'utente sta cancellando.
            if (NotesStorage.currPosition == currentPressedItem) {
                mCallbacks.setDeletingState(true);
                int pos = currentPressedItem == NotesStorage.ITEMS.size() ? currentPressedItem - 1 : currentPressedItem;
                NotesStorage.updateCurrItem(pos);
                // su tablet faccio un click sull'elemento sopra a quello eliminato
                if (mTwoPane) {
                    ListAdapter la = getListView().getAdapter();
                    getListView().performItemClick(la.getView(pos, null, null), pos, la.getItemId(pos));
                }
                mCallbacks.setDeletingState(false);
            }
        }
// Aggiorno il contenuto del fragment (solo su tablet)
        if (mTwoPane)
            mCallbacks.updateDetailFragmentContent();
    }

}
