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
    };

	/**
	 * Mandatory empty constructor for the fragment manager to instantiate the
	 * fragment (e.g. upon screen orientation changes).
	 */
	public NoteListFragment() {
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// TODO: replace with a real list adapter.
//		setListAdapter(new ArrayAdapter<NotesStorage.SoundNote>(getActivity(),
//				android.R.layout.simple_list_item_activated_1,
//				android.R.id.text1, NotesStorage.ITEMS));
	}

//	@Override
//	public View onCreateView(LayoutInflater inflater,
//			ViewGroup container, Bundle savedInstanceState) {
//		view.setOnTouchListener(new View.OnTouchListener() {
//            public boolean onTouch(View v, MotionEvent event) {
//
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

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);

		// Activities containing this fragment must implement its callbacks.
		if (!(activity instanceof Callbacks)) {
			throw new IllegalStateException("Activity must implement fragment's callbacks.");
		}

		mCallbacks = (Callbacks) activity;
	}

	@Override
	public void onDetach() {
		super.onDetach();
		// Reset the active callbacks interface to the dummy implementation.
		mCallbacks = sStorageCallbacks;
	}

    // Metodo chiamato quando l'activity a cui è collegato viene creata. Serve per fare cose quando
    // tutto è inizializzato oppure se volgio mantenerlo anche quando lo stacco da un'activity
    @Override
    public void onActivityCreated(Bundle savedState) {
        super.onActivityCreated(savedState);

        setListAdapter(NotesStorage.ITEMS_ADAPTER);

        registerForContextMenu(getListView());

//        getListView().setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
//
//            @Override
//            public boolean onItemLongClick(AdapterView<?> adapterView, View view, int position, long id) {
//
//                // NOtifico l'interfaccia dei callback (l'activity, se il fragment è
//                // connesso ad una) che un oggetto è stato premuto a lungo
//                mCallbacks.onItemLongPressed(NotesStorage.getNoteFromPosition(position).id, position);
//
//                return true;
//            }
//        });
    }

    @Override
    public void onListItemClick(ListView listView, View view, int position, long id) {
        super.onListItemClick(listView, view, position, id);

        // Notify the active callbacks interface (the activity, if the
        // fragment is attached to one) that an item has been selected.
        mCallbacks.onItemSelected(NotesStorage.getNoteFromPosition(position).id, position);

    }

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
                boolean deleted = NotesStorage.delete(getActivity(), currentPressedItem);

                // Non è riuscito a cancellarlo, notifico l'utente
                if (!deleted)
                    (Toast.makeText(getActivity(), R.string.action_delete_fail, Toast.LENGTH_LONG)).show();

                // se ora la lista è vuota sostituisco il fragment
                if (NotesStorage.ITEMS.isEmpty())
                    mCallbacks.swapVisibleFragment(true);

                // faccio un click su quell'elemento della lista

                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		if (mActivatedPosition != ListView.INVALID_POSITION) {
			// Serialize and persist the activated item position.
			outState.putInt(STATE_ACTIVATED_POSITION, mActivatedPosition);
		}
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
	
}
