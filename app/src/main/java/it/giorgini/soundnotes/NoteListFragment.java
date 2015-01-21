package it.giorgini.soundnotes;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.app.ListFragment;
import android.text.InputType;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.EditText;
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
	 * The fragment's current callback object, which is notified of list item clicks.
	 */
	private Callbacks callbacks_ListActivity = defaultCallbacks_ListActivity;

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
	private static Callbacks defaultCallbacks_ListActivity = new Callbacks() {
		@Override
        public void onItemSelected(String id, int position) { }

        @Override
        public void swapVisibleFragment(boolean emptyList) { }

        @Override
        public void updateDetailFragmentContent() { }

        @Override
        public void setDeletingState(boolean state) { }

        @Override
        public void setDetailNoteMenuItems(boolean visibility) { }
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

        callbacks_ListActivity = (Callbacks) activity;
    }

    // onCreate
    // onCreateView

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

        setListAdapter(StorageManager.ITEMS_ADAPTER);

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
		// Reset the active callbacks interface
		callbacks_ListActivity = defaultCallbacks_ListActivity;
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
        callbacks_ListActivity.onItemSelected(StorageManager.getNoteFromPosition(position).id, position);

    }

    // Creazione menu contestuale long press su nota in lista
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);

        // Mi salvo l'elemento premuto per saperlo quando l'utente sceglierà qualcosa dal menu contestuale
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
        currentPressedItem = info.position;

        // Titolo del menu e aggiungo le opzioni
        StorageManager.SoundNote n = StorageManager.getNoteFromPosition(info.position);
        menu.setHeaderTitle(n.name);
        menu.add(Menu.NONE, R.id.action_rename, Menu.NONE, R.string.action_rename);
        menu.add(Menu.NONE, R.id.action_delete, Menu.NONE, R.string.action_delete);
    }

    // E' stato premuto un elemento del menu contestuale.
    @Override
    public boolean onContextItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_rename:
                renameNote();
                return true;
            case R.id.action_delete:
                deleteNote();
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }



    // Rinomina la nota selezionata nel menu contestuale
    private void renameNote() {
        // ALERT: chiedo nuovo titolo per la nota
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        builder.setTitle(R.string.renaming_title)
               .setMessage(R.string.renaming_message);

        // creo campo input testo, lo imposto al nome corrente
        final EditText input = new EditText(getActivity());
        input.setSingleLine(true);
        input.setInputType(InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        final String oldName = StorageManager.getNoteFromPosition(currentPressedItem).name;
        input.setText(oldName);
        input.selectAll();

        builder.setView(input);

        // Imposto le azioni dell'alert
        builder.setPositiveButton(R.string.alert_key_ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                renameNote_OKButton(input, oldName, dialog);
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
                            renameNote_OKButton(input, oldName, dialogInterface);
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

    // Metodo che si occupa di rinominare la nota (fa poco, chiama un metodo di StorageManager
    public void renameNote_OKButton(EditText input, String oldName, DialogInterface dialog) {
        String newName = input.getText().toString();

        // Se ho cambiato qualcosa allora rinomino, altrimenti non faccio nulla.
        if (!newName.equals(oldName)) {
            boolean renamed = StorageManager.rename(getActivity(), currentPressedItem, newName);
            if (!renamed)
                Toast.makeText(getActivity(), R.string.action_rename_fail, Toast.LENGTH_LONG).show();
        }

        // chiudo l'alert
        if (dialog != null)
            dialog.cancel();
    }

    // Cancella la nota SELEZIONATA nel MENU CONTESTUALE chiamato tenendo premuto su una nota.
    private void deleteNote() {
        boolean deleted = StorageManager.delete(getActivity(), currentPressedItem);

        // Non è riuscito a cancellarlo, notifico l'utente
        if (!deleted) {
            (Toast.makeText(getActivity(), R.string.action_delete_fail, Toast.LENGTH_LONG)).show();
        } else if (StorageManager.ITEMS.isEmpty()) {
            // se ho cancellato e la lista è vuota
            mActivatedPosition = ListView.INVALID_POSITION;
            StorageManager.updateCurrItem(-1);
            currentPressedItem = -1;
            // Aggiorno il contenuto del fragment (solo su tablet) e sistemo i bottoni dell'action bar
            if (mTwoPane) {
                callbacks_ListActivity.updateDetailFragmentContent();
                // Devo rimuovere dall'action bar i bottoni relativi alla nota aperta.
                callbacks_ListActivity.setDetailNoteMenuItems(false);
//                MenuItem detailNoteMenus = MenuItem.findItem(R.id.note_actions); refreshItem.setVisible(false);
            }
            //  sostituisco il fragment della nota aperta con quello vuoto
            callbacks_ListActivity.swapVisibleFragment(true);
        } else {
            // Se ho cancellato una nota e ci sono note nella lista aggiorno la nota corrente nel NotesStorage.
            // Ma SOLO se la nota corrente è quella che l'utente sta cancellando.
            if (StorageManager.currPosition == currentPressedItem) {
                callbacks_ListActivity.setDeletingState(true);
                int pos = currentPressedItem == StorageManager.ITEMS.size() ? currentPressedItem - 1 : currentPressedItem;
                StorageManager.updateCurrItem(pos);
                // su tablet faccio un click sull'elemento sopra a quello eliminato
                if (mTwoPane) {
                    ListAdapter la = getListView().getAdapter();
                    getListView().performItemClick(la.getView(pos, null, null), pos, la.getItemId(pos));
                }
                callbacks_ListActivity.setDeletingState(false);
            }
        }
        // Aggiorno il contenuto del fragment (solo su tablet)
        if (mTwoPane)
            callbacks_ListActivity.updateDetailFragmentContent();
    }
}
