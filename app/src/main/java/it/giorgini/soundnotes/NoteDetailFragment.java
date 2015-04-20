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
import android.widget.Toast;

import java.lang.ref.WeakReference;

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
    private boolean firstStart = true;

    // the actionbar menu that this fragments inflates in onCreateOptionsMenu
    private WeakReference<Menu> menu;
	private StorageManager.SoundNote item;
//    private WeakReference<RecordingsView> recordingsView;

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
        public void initConnections(RichEditText view);
        public void initRecList();
        public void onPlayRequest(MenuItem item);
        public void onPauseRequest(MenuItem item);
        public void onDeleteRecRequest();
        public void onShareCurrRec();
    }

    /**
     * Questo metodo viene chiamato solo se il fragment non è attaccato ad un'activity, altrimenti
     * viene chiamato il metodo dell'activity connessa.
     */
    private static Callbacks defaultCallbacks_DetailActivity = new Callbacks() {
        @Override
        public void returnToList() { }
        public void initConnections(RichEditText view) { }
        public void initRecList() { }
        public void onPlayRequest(MenuItem item) { }
        public void onPauseRequest(MenuItem item) { }
        public void onDeleteRecRequest() { }
        public void onShareCurrRec() { }
    };

	/**
	 * Mandatory empty constructor for the fragment manager to instantiate the
	 * fragment (e.g. upon screen orientation changes).
	 */
	public NoteDetailFragment() { }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        // Activities containing this fragment must implement its callbacks.
        if (!(activity instanceof Callbacks)) {
            throw new IllegalStateException("Activity must implement fragment's callbacks.");
        }
        callbacks_DetailActivity = (Callbacks) activity;
    }

    @Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

        // setto firstStart a true per inizializzare alcuni dati della RecordingView (lo faccio nella onResume)
        firstStart = true;

        // Dico di voler aggiungere dei bottoni nella action bar
        setHasOptionsMenu(true);
//        Log.i("SN ###", "NoteDetailFragment onCreate");
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
//        Log.i("SN ###", "NoteDetailFragment onCreateView");
        View view = inflater.inflate(R.layout.fragment_note_detail, container, false);
//        RichEditText ret = (RichEditText) view;

        // inizializzo certe cose delle mie view: RichEditText e RecordingsView.
        // Solo non tablet perché lì devo aspettare fino alla onActivityCreated
        if (!mTwoPane)
            callbacks_DetailActivity.initConnections((RichEditText) view);

        return view;
	}

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
//        Log.i("SN ###", "NoteDetailFragment onActivityCreated");

        // Show the content as text in the TextView.
        updateCurrItem();

        // inizializzo certe cose delle mie view: RichEditText e RecordingsView
        RichEditText ret = (RichEditText) getView();
        if (ret != null) {
            if (mTwoPane)
                callbacks_DetailActivity.initConnections(ret);

            // Cambio il font (non si può fare da xml)
            Typeface tf = Typeface.createFromAsset(getActivity().getAssets(), "fonts/RobotoSlab-Regular.ttf");
            ret.setTypeface(tf);
            ret.setTextSize(16);
            // Se la nota è vuota apri la tastiera.
            if (StorageManager.getCurrNote().text.equals("")) {
                getActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
            }
        } else {
            Log.e("SN ###", "NoteDetFrag ret = null. MALE");
        }

    }

    @Override
    public void onResume() {
//        Log.i("SN ###", "NoteDetailFragment onResume");
        super.onResume();

        // Se lo stato è recording o playing vuol dire che era già avviato, non serve fargli eseguire la prepare
        if (!RecorderService.isRecording() && !RecorderService.isPlaying()) {
            Intent i = new Intent(getActivity(), RecorderService.class);
            i.setAction(RecorderService.ACTION_PREPARE);
            i.putExtra(RecorderService.EXTRA_NOTEID, item.id);
            getActivity().startService(i);
        }

        if (firstStart) {
            callbacks_DetailActivity.initRecList();
            firstStart = false;
        }
    }

    @Override
	public void onPause() {
//        Log.i("SN ###", "NoteDetailFragment: onPause");
		super.onPause();
	}

    @Override
    public void onStop() {
//        Log.i("SN ###", "NoteDetailFragment: onStop");
        super.onStop();
    }

    @Override
    public void onDetach() {
        super.onDetach();
//        Log.i("SN ###", "NoteDetailFragment: onDetach");
        // Reset the active callbacks interface to the dummy implementation.
        callbacks_DetailActivity = defaultCallbacks_DetailActivity;
    }

    @Override
    public void onDestroy() {
//        Log.i("SN ###", "NoteDetailFragment: onDestroy");
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
        // Ora sistemo la RecordingsView

    }

//    public void setRecView(RecordingsView recView) {
//        recordingsView = new WeakReference<>(recView);
//    }

    // Anche il fragment contribuisce agli elementi dell'action bar.
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
//        Log.i("SN ###", "NoteDetailFragment onCreateOptionsMenu");
        this.menu = new WeakReference<>(menu);
        inflater.inflate(R.menu.note_actions, menu);

        super.onCreateOptionsMenu(menu, inflater);
    }

    // Funzione per iniziare (e fermare) la registrazione
    public void toggleRecording() {
        if (!RecorderService.isPlaying()) {
            Intent i = new Intent(getActivity(), RecorderService.class);

            if (!RecorderService.isRecording()) {
                // Non sto registrando, devo avviarlo
                setRecordingIcon(true);
                i.setAction(RecorderService.ACTION_START);
            } else if (RecorderService.isInTheSameNoteAsRecording()) {
                // Sto già registrando, stoppo. Sono nella stessa nota della registrazione
                setRecordingIcon(false);
                i.setAction(RecorderService.ACTION_STOP);
            } else {
                // non stoppo
                Toast.makeText(getActivity(), R.string.rec_forbidden_stop, Toast.LENGTH_LONG).show();

            }

            getActivity().startService(i);
        } else {
            Toast.makeText(getActivity(), R.string.rec_forbidden_rec_playing, Toast.LENGTH_LONG).show();
        }
    }

    public void togglePlayPause(MenuItem item) {
        if (!RecorderService.isRecording()) {
            if (!RecorderService.isPlaying()) {
                // dico di riprodurre
                callbacks_DetailActivity.onPlayRequest(item);
            } else {
                // lo fermo
                callbacks_DetailActivity.onPauseRequest(item);
            }
        } else
            Toast.makeText(getActivity(), R.string.rec_forbidden_play, Toast.LENGTH_LONG).show();
    }

    // Setta l'icona del registratore attiva/spenta
    public void setRecordingIcon(boolean recording) {
        if (menu != null) {
            MenuItem i = menu.get().findItem(R.id.action_rec);

            if (recording) {
                i.setIcon(R.drawable.ic_action_mic_active);
                AnimationDrawable icon = (AnimationDrawable) i.getIcon();
                icon.start();
            } else {
                i.setIcon(R.drawable.ic_action_mic);
            }
        } else {
//            Log.i("SN ###", "NoteDetFrag setRecIcon menu = null.- Forse non un probl, metodo chiamato su tablet");
        }
    }

    public void askDeleteRecording() {
        callbacks_DetailActivity.onDeleteRecRequest();
    }

    public void shareFileAsText(String text) {
        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, text);
        sendIntent.setType("text/plain");
        startActivity(Intent.createChooser(
                sendIntent, getResources().getText(R.string.action_share_text_chooser)));
    }

    public void shareCurrRec() {
        callbacks_DetailActivity.onShareCurrRec();
    }

//    public void shareFileFull(Uri fileUri) {
//        //TODO: implementare condivisione file
//        Intent sendIntent = new Intent();
//        sendIntent.setAction(Intent.ACTION_SEND);
//        sendIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
//        sendIntent.setType("multipart/");
//        startActivity(Intent.createChooser(
//                sendIntent, getResources().getText(R.string.action_share_full_chooser)));
//    }

    // Eseguo un'azione a seconda di quale oggetto è stato premuto:
    // una nota o l'action bar
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                // Up button. Solo su cellulare
                callbacks_DetailActivity.returnToList();
                return true;
            case R.id.action_play:
                togglePlayPause(item);
                return true;
            case R.id.action_rec:
                toggleRecording();
                return true;
            case R.id.action_del:
                askDeleteRecording();
                return true;
//            case R.id.action_hide_note_list:
//                toggleNotesListVisibility(item);
//                return true;
            case R.id.action_share_text:
                RichEditText ret = (RichEditText) getActivity().findViewById(R.id.note_detail);
                shareFileAsText((ret.getText().toString()));
                return true;
            case R.id.action_share_curr_rec:
                shareCurrRec();
                return true;
            case R.id.action_share_full:
//                shareFileFull(); // CONDIVISIONE FILE TASTO DA IMPLEMENTARE
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
