package it.giorgini.soundnotes;

import java.lang.ref.WeakReference;
import android.os.Bundle;
import android.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

/**
 * A fragment representing a single Note detail screen. This fragment is either
 * contained in a {@link NoteListActivity} in two-pane mode (on tablets) or a
 * {@link NoteDetailActivity} on handsets.
 */
public class NoteDetailFragment extends Fragment {
//	/**
//	 * The fragment argument representing the item ID that this fragment represents.
//	 */
//	public static final String ARG_ITEM_ID = "item_id";

	private NotesStorage.SoundNote item;
	
//	/**
//	 * Il contenitore delle note
//	 */
//	public WeakReference<NotesStorage> ns;
	
//	private GestureDetector mDetector; 

	/**
	 * Mandatory empty constructor for the fragment manager to instantiate the
	 * fragment (e.g. upon screen orientation changes).
	 */
	public NoteDetailFragment() { }

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

        // Dico di voler aggiungere dei botoni nella action bar
        setHasOptionsMenu(true);

//		if (getArguments().containsKey(NotesStorage.currID)) {
			// Load the dummy content specified by the fragment
			// arguments. In a real-world scenario, use a Loader
			// to load content from a content provider.
//        Log.d("DEBUG", "----------NoteDetailFragment onCreate");

//		}

//		// Gestione slide per nascondere / mostrare la lista delle note su tablet
////		OnGestureListener gl = new GestureDetector.SimpleOnGestureListener();
////		GestureDetector gd = new GestureDetector(this, GestureDetector.)
//        // Instantiate the gesture detector with the
//        // application context and an implementation of
//        // GestureDetector.OnGestureListener
//        mDetector = new GestureDetector(this.getActivity(), new SwipeGestureListener(this.getActivity()));
//        Log.d("DEBUG", "START");
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
//		View rootView = inflater.inflate(R.layout.fragment_note_detail,
//				container, false);

		// Show the content as text in the TextView.
//        updateCurrItem();

//		// Registro un listener sugli eventi touch che passerò all'activity
//		getView().setOnTouchListener(new View.OnTouchListener() {
//			@Override
//            public boolean onTouch(View v, MotionEvent event) {
//            	getActivity().onTouchEvent(event);
//                return false;
//            }
//		});
		
		return inflater.inflate(R.layout.fragment_note_detail, container, false);
	}

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // Show the content as text in the TextView.
        updateCurrItem();
    }

    @Override
	public void onPause() {
        Log.d("DEBUG", "##### NoteDetailFragment: onPause");
//        NotesStorage.save(getActivity(), ((RichEditText) (getView().findViewById(R.id.note_detail))).getText().toString());
		super.onPause();
	}

    @Override
    public void onStop() {
        Log.d("DEBUG", "##### NoteDetailFragment: onStop");
        super.onStop();
    }

    @Override
    public void onDetach() {
        Log.d("DEBUG", "##### NoteDetailFragment: onDetach");
        super.onDetach();
    }

    @Override
    public void onDestroy() {
        Log.d("DEBUG", "##### NoteDetailFragment: onDestroy");
        super.onDestroy();
    }

    // Aggiorna elemento visualizzato dal fragment
    public void updateCurrItem() {
        item = NotesStorage.getCurrNote();
        if (getView() != null && item != null) {
            ((RichEditText) getView().findViewById(R.id.note_detail)).setText(item.text);
        }
//            edt.setSelection(mItem.text.length()); //TODO: Implementare impostazione: edit all'inizio/fine/mai
    }

    // Anche il fragment contribuisce agli elementi dell'action bar.
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.note_actions, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    //	public void saveCurrentFile() {
//		String text = ((RichEditText) getView()).getText().toString();
//		NotesStorage.save(getActivity(), text);
////		SoundNote newNote = new SoundNote(ARG_ITEM_ID, NotesStorage.ITEM_MAP.get(ARG_ITEM_ID).name, text);
////		NotesStorage.ITEM_MAP.put(getArguments().getString(ARG_ITEM_ID), newNote);
//	}
	
//	@Override
//	public boolean onInterceptTouchEvent(MotionEvent ev) {
//		
//	}
}
