package it.giorgini.soundnotes;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.graphics.drawable.AnimationDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

/**
 * A fragment representing a single Note detail screen. This fragment is either
 * contained in a {@link NoteListActivity} in two-pane mode (on tablets) or a
 * {@link NoteDetailActivity} on handsets.
 */
public class NoteDetailFragment extends Fragment {
    /**
     * The fragment's current callback object, which is notified of list item
     * clicks.
     */
    private Callbacks callbacks_DetailActivity = defaultCallbacks_DetailActivity;

    private boolean mTwoPane = false;

    // the actionbar menu that this fragments inflates in onCreateOptionsMenu
    private Menu menu;
	private StorageManager.SoundNote item;

    private Uri currFileUri;
//    private boolean isRecording = false;
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
        public void setDetailFragmentView(RichEditText view);
//        public void saveCurrentNote();
//        public void slideToLeft(int newVisibility);
//        public void slideToRight(int newVisibility);
    }

    /**
     * Questo metodo viene chiamato solo se il fragment non è attaccato ad un'activity, altrimenti
     * viene chiamato il metodo dell'activity connessa.
     */
    private static Callbacks defaultCallbacks_DetailActivity = new Callbacks() {
        @Override
        public void returnToList() { }
        public void setDetailFragmentView(RichEditText view) { }
//        public void saveCurrentNote() { }
//        public void slideToLeft(int newVisibility) { }
//        public void slideToRight(int newVisibility) { }
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
            callbacks_DetailActivity = (Callbacks) activity;
        }
    }

    @Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

        // Dico di voler aggiungere dei bottoni nella action bar
        setHasOptionsMenu(true);
        Log.i("SN ###", "NoteDetailFragment onCreate");
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
        Log.i("SN ###", "NoteDetailFragment onCreateView");
        View view = inflater.inflate(R.layout.fragment_note_detail, container, false);
        callbacks_DetailActivity.setDetailFragmentView((RichEditText) view);

        return view;
	}

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Log.i("SN ###", "NoteDetailFragment onActivityCreated");

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
    }

    @Override
    public void onResume() {
        Log.i("SN ###", "NoteDetailFragment onResume");
        super.onResume();

        // Se lo stato è recording vuol dire che era già avviato, non serve fargli eseguire la prepare
        if (RecorderManager.getState() != MRState.RECORDING) {
            Intent i = new Intent(getActivity(), RecorderManager.class);
            i.setAction(RecorderManager.ACTION_PREPARE);
            i.putExtra("noteID", item.id);
            getActivity().startService(i);
        }
    }

    @Override
	public void onPause() {
        Log.i("SN ###", "NoteDetailFragment: onPause");
		super.onPause();
	}

    @Override
    public void onStop() {
        Log.i("SN ###", "NoteDetailFragment: onStop");
        super.onStop();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        Log.i("SN ###", "NoteDetailFragment: onDetach");
        // Reset the active callbacks interface to the dummy implementation.
        callbacks_DetailActivity = defaultCallbacks_DetailActivity;
    }

    @Override
    public void onDestroy() {
        Log.i("SN ###", "NoteDetailFragment: onDestroy");
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
    }


    // Anche il fragment contribuisce agli elementi dell'action bar.
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        Log.i("SN ###", "NoteDetailFragment onCreateOptionsMenu");
        this.menu = menu;
        inflater.inflate(R.menu.note_actions, menu);

        // Controllo se sto registrando, in quel caso setto l'icona appropriata
        // Se no, non devo fare nulla, c'è già quella giusta
//        if (RecorderManager.getState() == MRState.RECORDING) {
//            MenuItem menuItem = menu.findItem(R.id.action_rec);
//            setRecordingIcon(menuItem, true);
//        }

        super.onCreateOptionsMenu(menu, inflater);
    }

    // Funzione per iniziare (e fermare) la registrazione
    public void toggleRecording(MenuItem item) {
        Intent i = new Intent(getActivity(), RecorderManager.class);

        // Non sto registrando, devo avviarlo
        if(RecorderManager.getState() != MRState.RECORDING) {
            setRecordingIcon(true);
            i.setAction(RecorderManager.ACTION_START);
        // Sto già registrando, stoppo
        } else {
            setRecordingIcon(false);
            i.setAction(RecorderManager.ACTION_STOP);
        }

        getActivity().startService(i);
    }

    // Setta l'icona del registratore attiva/spenta
    public void setRecordingIcon(boolean recording) {
        MenuItem i = menu.findItem(R.id.action_rec);

        if (recording) {
            i.setIcon(R.drawable.ic_action_mic_active);
            AnimationDrawable icon = (AnimationDrawable) i.getIcon();
            icon.start();
        } else {
            i.setIcon(R.drawable.ic_action_mic);
        }
    }

//    public void toggleNotesListVisibility(MenuItem item) {
//        if (mTwoPane) {
//            Toast.makeText(getActivity(), "DENTRO TOGGLE", Toast.LENGTH_SHORT).show();
//            isListVisible = !isListVisible;
//            if (isListVisible) {
//                callbacks_DetailActivity.slideToRight(View.VISIBLE);
//                item.setIcon(R.drawable.ic_action_full_screen);
//            } else {
//                callbacks_DetailActivity.slideToLeft(View.GONE);
//                item.setIcon(R.drawable.ic_action_return_from_full_screen);
//            }
//        } else {
//            Log.d("DEBUG", "#### tablet noteslist hide button pressed on a cellphone...");
//        }
//    }

//    // Se non sto registrando
//    public void releaseRecorder() {
//        Log.d("SN ###", "RecMan releaseRecorder: called");
//        //TODO: Implementare releaseRecorder?
//    }

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
                callbacks_DetailActivity.returnToList();
                return true;
            case R.id.action_rec:
                toggleRecording(item);
                return true;
//            case R.id.action_hide_note_list:
//                toggleNotesListVisibility(item);
//                return true;
            case R.id.action_share_text:
                RichEditText ret = (RichEditText) getActivity().findViewById(R.id.note_detail);
                shareFileAsText((ret.getText().toString()));
                return true;
            case R.id.action_share_full:
                shareFileFull(currFileUri); // TODO: implementare (tasto condivisione)
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
