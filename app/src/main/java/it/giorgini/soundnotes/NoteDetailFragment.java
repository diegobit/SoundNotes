package it.giorgini.soundnotes;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.app.Fragment;
import android.os.Handler;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

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

    /**
     * The fragment's current callback object, which is notified of list item
     * clicks.
     */
    private Callbacks mCallbacks = sStorageCallbacks;

    private boolean mTwoPane = false;

	private StorageManager.SoundNote item;

    private Handler handlerUI; // Mi serve per aspettare un secondo in stop()

//    private RecorderManager recorderManager;
    private Uri currFileUri;
    private boolean isRecording = false;
//    private boolean isListVisible = true;

    /**
     * A callback interface that all activities containing this fragment must
     * implement. This mechanism allows activities to be notified of item
     * selections.
     */
    public interface Callbacks {
        /**
         * Callback for when an item has been selected.
         */
        public void returnToList();
//        public void slideToLeft(int newVisibility);
//        public void slideToRight(int newVisibility);
        /* Per controllare lo swipe */
//        public void setOnTouchEventListenerTo();
    }

    /**
     * Questo metodo viene chiamato solo se il fragment non è attaccato ad un'activity, altrimenti
     * viene chiamato il metodo dell'activity connessa.
     */
    private static Callbacks sStorageCallbacks = new Callbacks() {
        @Override
        public void returnToList() { }
//        public void slideToLeft(int newVisibility) { }
//        public void slideToRight(int newVisibility) { }
//        @Override
//        public void setOnTouchEventListenerTo() { }
    };

	/**
	 * Mandatory empty constructor for the fragment manager to instantiate the
	 * fragment (e.g. upon screen orientation changes).
	 */
	public NoteDetailFragment() { }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        // Solo per cellulare perchè su tablet non ho bisogno dei Callback (cambiare activity per esempio)
        if (!mTwoPane) {
            // Activities containing this fragment must implement its callbacks.
            if (!(activity instanceof Callbacks)) {
                throw new IllegalStateException("Activity must implement fragment's callbacks.");
            }
            mCallbacks = (Callbacks) activity;
        }
    }

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

        // Cambio il font (non si può fare da xml)
        RichEditText ret = (RichEditText) getActivity().findViewById(R.id.note_detail);
        Typeface tf = Typeface.createFromAsset(getActivity().getAssets(), "fonts/RobotoSlab-Regular.ttf");
        ret.setTypeface(tf);
        ret.setTextSize(16);
        // Se la nota è vuota apri la tastiera.
        if (StorageManager.getCurrNote().text.equals("")) {
            getActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        }

        // Creo l'handler UI
        handlerUI = new Handler();

//        // Mi registro agli eventi touch attraverso un callback con l'activity
//        mCallbacks.setOnTouchEventListenerTo();

//        // Istanzio un recorder manager per gestire le registrazioni audio.
//        recorderManager = new RecorderManager(getActivity(), handlerUI, getActivity().getFilesDir().getAbsolutePath(), item.id);
        // **** SPOSTATO IN NOTELISTACTIVITY e NOTEDETAILACTIVITY

//        recorderManager.prepare();
    }

    @Override
    public void onResume() {
        super.onResume();

        Intent i = new Intent(getActivity(), RecorderManager.class);
        i.setAction(RecorderManager.ACTION_PREPARE);
        i.putExtra("noteID", item.id);
        getActivity().startService(i);
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
        super.onDetach();
        Log.d("DEBUG", "##### NoteDetailFragment: onDetach");
        // Reset the active callbacks interface to the dummy implementation.
        mCallbacks = sStorageCallbacks;
    }

    @Override
    public void onDestroy() {
        Log.d("DEBUG", "##### NoteDetailFragment: onDestroy");
        super.onDestroy();
    }

    public void setTwoPane(boolean twoPane) {
        mTwoPane = twoPane;
    }

    // Aggiorna elemento visualizzato dal fragment
    public void updateCurrItem() {
        item = StorageManager.getCurrNote();
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

    // Funzione per iniziare (e fermare) la registrazione
    public void toggleRecording(MenuItem item) {

        isRecording = !isRecording;
        // cambio l'icona
        Intent i = new Intent(getActivity(), RecorderManager.class);
        if(isRecording) {
            item.setIcon(R.drawable.ic_action_mic_active);
            i.setAction(RecorderManager.ACTION_START);
            getActivity().startService(i);
//            recorderManager.start();
        } else {
            item.setIcon(R.drawable.ic_action_mic);
            i.setAction(RecorderManager.ACTION_STOP);
            getActivity().startService(i);
//            recorderManager.pause();
        }
    }

//    public void toggleNotesListVisibility(MenuItem item) {
//        if (mTwoPane) {
//            Toast.makeText(getActivity(), "DENTRO TOGGLE", Toast.LENGTH_SHORT).show();
//            isListVisible = !isListVisible;
//            if (isListVisible) {
//                mCallbacks.slideToRight(View.VISIBLE);
//                item.setIcon(R.drawable.ic_action_full_screen);
//            } else {
//                mCallbacks.slideToLeft(View.GONE);
//                item.setIcon(R.drawable.ic_action_return_from_full_screen);
//            }
//        } else {
//            Log.d("DEBUG", "#### tablet noteslist hide button pressed on a cellphone...");
//        }
//    }

    // Se non sto registrando
    public void releaseRecorder() {
        Log.d("DEBUG", "#### RecMan releaseRecorder: called");
        
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

    // Eseguo un'azione a seconda di quale oggetto è stato premuto:
    // una nota o l'action bar
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                // Up button. Solo su cellulare
                mCallbacks.returnToList();
                return true;
            case R.id.action_rec:
                toggleRecording(item);
                return true;
//            case R.id.action_hide_note_list:
//                toggleNotesListVisibility(item);
//                return true;
            case R.id.action_share_text:
                shareFileAsText(((RichEditText) getView().findViewById(R.id.note_detail)).getText().toString());
                return true;
            case R.id.action_share_full:
                shareFileFull(currFileUri); // TODO: implementare (tasto condivisione)
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
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
