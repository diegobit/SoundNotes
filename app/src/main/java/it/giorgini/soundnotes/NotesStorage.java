package it.giorgini.soundnotes;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.widget.ArrayAdapter;

enum SortMode {
    ALPHABETIC_ASCENDING, ALPHABETIC_DESCENDING, CREATIONTIME_ASCENDING, CREATIONTIME_DESCENDING
}

class WrongExtensionException extends Exception {

    public WrongExtensionException(String s) {
        super(s);
    }
}

public class NotesStorage {

    private static WeakReference<Activity> currActivity;
    private static SharedPreferences dataPrefs;

	public static ArrayList<SoundNote> ITEMS;
    public static ArrayAdapter<SoundNote> ITEMS_ADAPTER;
	public static HashMap<String, SoundNote> ITEM_MAP;
    public static String textExtensions = "txt";
	public static String currName;
	public static String currID;
	public static int currPosition = -1;

    public static SortMode SORTOPTION = SortMode.CREATIONTIME_DESCENDING;

//	private File internalDir;
	
//	private WeakReference<NoteListActivity> ctx;
	
	static {
//		this.ctx = ctx;
		ITEMS = new ArrayList<SoundNote>();
		ITEM_MAP = new HashMap<String, SoundNote>();
//		internalDir = ctx.get().getFilesDir();
//		create("11", "text11", System.currentTimeMillis());
//		create("vuota", "", System.currentTimeMillis());
	}
	
//	public NotesStorage(WeakReference<NoteListActivity> ctx) {
//		this.ctx = ctx;
//		ITEMS = new ArrayList<SoundNote>();
//		ITEM_MAP = new HashMap<String, SoundNote>();
//		internalDir = ctx.get().getFilesDir();
//	}

    public static void init(Activity act, SharedPreferences dataPreferences) {
        currActivity = new WeakReference<Activity>(act);
        dataPrefs = dataPreferences;
    }

	public static void updateCurrItem(int position) {
//        Log.d("DEBUG", "----------updatePos: curr:" + currPosition + " new: " + position);
        SoundNote n = getNoteFromPosition(position);
		currID = n.id;
		currName = n.name;
		currPosition = position;
	}
//	public static void updateCurrItem(int id, int position) {
//		currID = String.valueOf(id);
//		currName = ITEM_MAP.get(currID).name;
//		currPosition = position;
//	}
//    private static void init(boolean isFirst) {
//        ITEMS = null;
//        ITEM_MAP = new HashMap<String, SoundNote>();
//
//        if (!isFirst) {
//            currName = null;
//            currID = null;
//            currPosition = -1;
//            nextID = 1;
//        }
//    }

    public static SoundNote getCurrNote() {
        return ITEM_MAP.get(currID);
    }

    public static SoundNote getNoteFromId(String id) {
        return ITEM_MAP.get(id);
    }

    public static SoundNote getNoteFromPosition(int pos) {
        return ITEMS.get(pos);
    }

    private static void sortList(SortMode st) {
        Comparator<SoundNote> comp;
        switch (st) {
            case ALPHABETIC_ASCENDING:
                comp = new Comparator<SoundNote>() {
                    @Override
                    public int compare(SoundNote n1, SoundNote n2) {
                        return n1.name.compareTo(n2.name);
                    }
                };
                break;

            case ALPHABETIC_DESCENDING:
                comp = new Comparator<SoundNote>() {
                    @Override
                    public int compare(SoundNote n1, SoundNote n2) {
                        return n2.name.compareTo(n1.name);
                    }
                };
                break;

            case CREATIONTIME_ASCENDING:
                comp = new Comparator<SoundNote>() {
                    @Override
                    public int compare(SoundNote n1, SoundNote n2) {
                        return (new Long(n1.date)).compareTo(n2.date); // TODO: Se non ordina bene rimettere n2.date dentro un Long
                    }
                };
                break;

            case CREATIONTIME_DESCENDING:
                comp = new Comparator<SoundNote>() {
                    @Override
                    public int compare(SoundNote n1, SoundNote n2) {
                        return (new Long(n2.date)).compareTo(n1.date);
                    }
                };
                break;

            default:
                throw new IllegalArgumentException("Unknown SortType");
        }

        Collections.sort(ITEMS, comp);
        ITEMS_ADAPTER.notifyDataSetChanged();
//        ITEMS.sort(comp);
    }

    /** dato il nome di un file di una nota ritorna una coppia: id e nome della nota da mostrare
     * 13-nomeNota.txt --> <13, nomenota>
     * @param p path del file
     * @return una coppia di valori, l'id della nota e il nome isolato dall'ID e dall'estensione. Se l'estensione
     * non è tra quelle accettate (vedi textExtensions), lancia l'eccezione WrongExtensionException
     */
    private static String[] getTokensFromPath(String p) throws WrongExtensionException {
        if (p == null)
            throw new IllegalArgumentException("The argument is not initialized");

        String fullname = (new File(p)).getName();

        // Voglio solo il nome del file senza l'estensione. Lancio un'eccezione se
        // l'estensione non è corretta
        String[] tokens = fullname.split("\\.(?=[^\\.]+$)");
        if (tokens.length < 2 || !textExtensions.contains(tokens[1]))
            throw new IllegalArgumentException("The argument is not initialized");

        // Voglio solo il nome della nota, rimuovo l'ID.
        String[] parts = tokens[0].split("-");

//        Log.d("DEBUG", "#### fullname:" + fullname + "####");
//        Log.d("DEBUG", "#### tokens:" + tokens[0] + "_" + tokens[1] + "####");
//        Log.d("DEBUG", "#### parts:" + parts[0] + "_" + parts[1] + "####");

        return parts;

//        int i = fullname.lastIndexOf(".");
//        if (i <= 0)
//            throw new WrongExtensionException("Invalid extension");
//        String id_name = fullname.substring(0, i - 1);
//        String ext = fullname.substring(i, fullname.length() - 1);
//        if (!textExtensions.contains(ext))
//            throw new WrongExtensionException("Invalid extension");
//
//        // Voglio solo il nome della nota, rimuovo l'ID.
//        String[] parts = id_name.split("-");

//        Log.d("DEBUG", "#### fullname:" + fullname + "####");
//        Log.d("DEBUG", "#### id_name:" + id_name + "####");
//        Log.d("DEBUG", "#### ext:" + ext + "####");
//        return parts[0] + "-" + parts[1]; // FIXME: lasciare solo parts[1]
    }

	public static int add(String name, String text, long date) {
        // recupero l'id univoco della prossima nota
        int nextID = dataPrefs.getInt("nextID", 1);

        // creo la nota e aggiorno l'adapter
        create(String.valueOf(nextID), name, text, date, true);

        // Aggiorno l'id univoco della prossima nota.
        SharedPreferences.Editor edit = dataPrefs.edit();
        edit.putInt("nextID", nextID + 1);
        edit.apply();

		return nextID;
	}

    private static void create(String id, String name, String text, long date, boolean notifyChangesToAdapter) {
        SoundNote n = new SoundNote(id, name, text, date);

        if (SORTOPTION == SortMode.CREATIONTIME_DESCENDING) {
            ITEMS.add(0, n);
            // TODO: altri casi
            ITEM_MAP.put(id, n);
        }

        if (notifyChangesToAdapter)
            ITEMS_ADAPTER.notifyDataSetChanged();
    }

    public static void clear() {
        ITEMS.clear();
        ITEM_MAP.clear();
        ITEMS_ADAPTER.notifyDataSetChanged();
        currName = null;
        currID = null;
        currPosition = -1;
    }

    /** Carico tutti i file dalla memoria interna per mostrarne la lista. Se ritorna false è necessario sotituire il fragment della
     * lista delle note con quello vuoto a mano
     *
     * @return true se ho caricato almeno un elemento
     *         false altrimenti
     */

    public static boolean load() { //TODO: farlo in backgroud
        ITEMS_ADAPTER = new ArrayAdapter<SoundNote>(currActivity.get(),
                    android.R.layout.simple_list_item_activated_1,
                    android.R.id.text1,
                    ITEMS);

        String[] files = currActivity.get().fileList();

        // Carico le note solo se ci sono dei file nella memoria interna
        if (files.length != 0) {
            int counter = 0;
            long date = 0;
            String[] id_and_name;
            StringBuilder text = new StringBuilder();

            // Itero: per ogni file creo una nuova nota
            for (String path : files) {
                try {
                    // Isolo il nome della nota dal nome del file (e ignoro il file se non è uno corretto)
                    id_and_name = getTokensFromPath(path);

                    // Leggo dal file
                    FileInputStream is = currActivity.get().openFileInput(path);
                    BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
                    String line = reader.readLine();
//                    while (line != null && !line.equals("null")) {
                    while (line != null) {
                        // prima linea del file: se date == 0 allora sto controllando la prima linea
                        if (date == 0)
                            date = Long.parseLong(line);
                            // Aggiorno il contenuto della nota
                        else
                            text.append(line).append("\n");

                        line = reader.readLine();
                    }
                    is.close();

                    // creo la nota da mostrare nella lista e aggiorno il contatore
                    create(id_and_name[0], id_and_name[1], text.toString(), date, false);
                    counter++;

                    // azzero la data, sto per controllare una nuova nota
                    date = 0;

                } catch (WrongExtensionException e) {
                    // non faccio nulla perchè voglio solamente ignorare il file corrente
                } catch (OutOfMemoryError e) {
                    e.printStackTrace();
                } catch (FileNotFoundException e) {
                    Log.d("DEBUG", "Strange, I scan for file and then read them");
                    e.printStackTrace();
                } catch (UnsupportedEncodingException e) {
                    Log.d("DEBUG", "Strange, I save this files myself in UTF-8");
                } catch (IOException e) {
                    e.printStackTrace();
                }

                // azzero lo StringBuilder
                text.setLength(0);
            }

            // Ho caricato tutte le note: le ordino. Se non ci sono elementi carico la vista speciale
            if (counter > 0) {
                sortList(SORTOPTION);
                return true;
            }
        }

        return false;
    }
	
	public static boolean save(Context ctx, String newText) {
        Log.d("DEBUG", "#####save: " + newText);
//        for (SoundNote n : ITEMS)
//            Log.d("DEBUG", String.format("------LISTA-PRIMA: %s", n.toString2()));
        // salvo solo se ci sono delle modifiche alla nota. Questo metodo viene chiamato anche ogni
        // volta che su tablet si seleziona una nuova nota nella lista
        SoundNote currNote = getCurrNote();

        boolean textHasChanged = !currNote.text.equals(newText);
        // Salvo se Il testo è stato modificato oppure se sono entrambi vuoti
        // (voglio evitare di salvare più volte lo stesso testo se visualizzo senza modificare)
        if (textHasChanged || (!textHasChanged && newText.equals(""))) {
            FileOutputStream outStream;

            try {
                outStream = ctx.openFileOutput(makeFilename(currID, currName, ".txt"), Context.MODE_PRIVATE);
                outStream.write((Long.toString(currNote.date) + "\n" + newText).getBytes());
                outStream.close();
                ITEM_MAP.get(currID).text = newText;
                ITEMS.get(currPosition).text = newText;
                ITEMS_ADAPTER.notifyDataSetChanged();
            } catch (Exception e) {
                Log.d("DEBUG", "Unable to save the note '" + currName + "'");
                return false;
            }

//            for (SoundNote n : ITEMS)
//                Log.d("DEBUG", String.format("------LISTA-DOPO: %s", n.toString2()));
            return true;
        }

        return true;
	}

	public static boolean delete(Context ctx, int position) {
        Log.d("DEBUG", "@@@@ delete: pos: " + position + " - " + NotesStorage.getNoteFromPosition(position).name);
        String id = ITEMS.get(position).id;
        File file = new File(ctx.getFilesDir(), getFilenameFromID(id));
        Log.d("DEBUG", "@@@@ delete: file: " + file.getAbsolutePath());
        boolean deleted = file.delete();
        File[] files = ctx.getFilesDir().listFiles();
        for (File f : files) {
            Log.d("DEBUG", "@@@@ for: " + f.getAbsolutePath());
        }

        if (deleted) {
            ITEM_MAP.remove(id);
//            ITEMS.remove(getNoteFromId(id));
            ITEMS.remove(position);
            ITEMS_ADAPTER.notifyDataSetChanged();
        }

        return deleted;
    }

    private static String getFilenameFromID (String id) {
        return id + "-" + ITEM_MAP.get(id).name + ".txt";
    }

    private static String makeFilename (String id, String name, String extension) {
        return id + "-" + name + extension;
    }



	/**
	 * Questa classe rappresenta una singola nota
	 */
	public static class SoundNote {
		public String id;
		public String name;
		public String text;
        public long date;

		public SoundNote(String id, String name, String text, long date) {
			this.id = id;
			this.name = name;
			this.text = text;
            this.date = date;
		}

		@Override
		public String toString() {
			return name; // Metterei la rappresentazione completa del dato ma l'Array Adapter che
                         // uso per la lista di note visualizza il valore di ritorno di toString
		}

//        public String toString2() { //TODO: RIMUOVI!
//            return String.format("%s:%s:%s", id, name, text);
//        }
	}
}
