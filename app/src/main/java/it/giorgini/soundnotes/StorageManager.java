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
import android.widget.Toast;

enum SortMode {
    ALPHABETIC_ASCENDING, ALPHABETIC_DESCENDING, CREATIONTIME_ASCENDING, CREATIONTIME_DESCENDING
}



public class StorageManager {

    private static WeakReference<Activity> currActivity;
    private static SharedPreferences dataPrefs;

	public static ArrayList<SoundNote> ITEMS;
    public static RichArrayAdapter<SoundNote> ITEMS_ADAPTER;
	public static HashMap<String, SoundNote> ITEM_MAP;
    public static String textExtensions = ".txt";
    public static String audioExtensions = ".m4a";
	public static String currName ;
	public static String currID;
	public static int currPosition = -1;

    public static SortMode SORTOPTION = SortMode.CREATIONTIME_DESCENDING;

	static {
		ITEMS = new ArrayList<SoundNote>();
		ITEM_MAP = new HashMap<String, SoundNote>();
	}

    // Inizializzazione all'avvio dell'activity principale dell'app
    public static void init(Activity act, SharedPreferences dataPreferences) {
        currActivity = new WeakReference<Activity>(act);
        dataPrefs = dataPreferences;
    }

    public static void clear() {
        ITEMS.clear();
        ITEM_MAP.clear();
        ITEMS_ADAPTER.notifyDataSetChanged();
        currName = null;
        currID = null;
        currPosition = -1;
    }

    public static void updateCurrItem(int position) {
        // Se position >= 0 significa che c'è almeno una nota.
        if (position >= 0) {
            SoundNote n = getNoteFromPosition(position);
            currID = n.id;
            currName = n.name;
            currPosition = position;
        } else {
            currID = "";
            currName = "";
            currPosition = position;
        }

    }



    public static SoundNote getCurrNote() {
        return ITEM_MAP.get(currID);
    }

    public static SoundNote getNoteFromId(String id) {
        return ITEM_MAP.get(id);
    }

    public static SoundNote getNoteFromPosition(int pos) {
        return ITEMS.get(pos);
    }

    /** Dato il nome di un file di una nota ritorna una tripla: id, nome della nota da mostrare, estensione
     * 13-nomeNota.txt --> <13, nomenota, txt>
     * @param p path del file
     * @return una coppia di valori, l'id della nota e il nome isolato dall'ID e dall'estensione. Se l'estensione
     * non è tra quelle accettate (vedi textExtensions), lancia l'eccezione WrongExtensionException
     */
    private static String[] getTokensFromFilename(String p) throws WrongFileNameException {
        if (p == null)
            throw new IllegalArgumentException("The argument is not initialized");

        // Cerco il primo "-" che divide id da nome, e l'ultimo "." che segnala l'estensione.
        // Se non ne trovo uno lancio subito un'eccezione, i file devono sempre avere nomi ben formati.
        int firstMinus = p.indexOf("-");
        int lastDot = p.lastIndexOf(".");
        if (firstMinus == 0 || lastDot == 0)
            throw new WrongFileNameException("Incorrect filename. Should be id-name.ext");

        // Divido la stringa in tre
        String[] tokens = { "", "", "" };
        tokens[0] = p.substring(0, firstMinus);
        tokens[1] = p.substring(firstMinus + 1, lastDot);
        tokens[2] = p.substring(lastDot + 1);

        // controllo che l'id sia un numero e che l'estensione sia tra quelle previste.
        if (!isInteger(tokens[0]) || textExtensions.contains(tokens[0]) || audioExtensions.contains(tokens[0]))
            throw new WrongFileNameException("Incorrect filename. Should be id-name.ext");

        return tokens;
    }

    private static String getFilenameFromID (String id) {
        return id + "-" + ITEM_MAP.get(id).name + ".txt";
    }

    private static String makeFilename (String id, String name, String extension) throws WrongFileNameException{
        if (textExtensions.contains(extension) || audioExtensions.contains(extension)) {
            boolean hasDot = extension.contains(".");
            if (hasDot && extension.length() == 4) {
                return id + "-" + name + extension;
            }
            else if (!hasDot && extension.length() == 3) {
                return id + "-" + name + "." + extension;
            }
        }
        throw new WrongFileNameException("The extension given is wrong/unsupported");
    }



    public static boolean isInteger(String s) {
        try {
            Integer.parseInt(s);
        } catch(NumberFormatException e) {
            return false;
        }
        // Arrivo qui solo se è possibile fare il parse della stringa s in un intero.
        return true;
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
                        return (new Long(n1.date)).compareTo(n2.date);
                        // NOTA SE CERCHERAI DEI BUG: se non ordina bene rimettere n2.date dentro un Long
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

    /** Carico tutti i file dalla memoria interna per mostrarne la lista. Se ritorna false è necessario sotituire il fragment della
     * lista delle note con quello vuoto a mano
     *
     * @return true se ho caricato almeno un elemento
     *         false altrimenti
     */

    public static boolean load() { //TODO: farlo in backgroud
        ITEMS_ADAPTER = new RichArrayAdapter<SoundNote>(currActivity.get(),
                                                        android.R.layout.simple_list_item_activated_1,
                                                        android.R.id.text1,
                                                        ITEMS);

        String[] files = currActivity.get().fileList();

        // Carico le note solo se ci sono dei file nella memoria interna
        if (files.length != 0) {
            int counter = 0;
            long date = 0;
            String[] id_name_ext;
            StringBuilder text = new StringBuilder();

            // Itero: per ogni file creo una nuova nota
            for (String path : files) {
                try {
                    // Isolo il nome della nota dal nome del file (e ignoro il file se non è uno corretto)
                    id_name_ext = getTokensFromFilename(path);

                    // Devo leggere il file. Può essere la parte testuale o una audio
                    if (textExtensions.contains(id_name_ext[2])) {
                        // Parte testuale: Leggo riga per riga
                        FileInputStream is = currActivity.get().openFileInput(path);
                        BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
                        String line = reader.readLine();
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
                        create(id_name_ext[0], id_name_ext[1], text.toString(), date, false);
                        counter++;

                        // azzero la data, sto per controllare una nuova nota
                        date = 0;
                    } else {
                        // Parte audio:

                        //
                        //TODO: IMPLEMENTARE
                        //
                    }

                } catch (WrongFileNameException e) {
                    // Non faccio nulla perchè voglio solamente ignorare il file corrente se non riconosco il formato del nome.
                    // Non voglio che crashi o vengano caricare note non consistenti
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
        // salvo solo se ci sono delle modifiche alla nota. Questo metodo viene chiamato anche ogni
        // volta che su tablet si seleziona una nuova nota nella lista
        SoundNote currNote = getCurrNote();

        boolean textHasChanged = !currNote.text.equals(newText);
        // Salvo se Il testo è stato modificato oppure se sono entrambi vuoti
        // (voglio evitare di salvare più volte lo stesso testo se visualizzo senza modificare)
        if (textHasChanged || newText.equals("")) {
            FileOutputStream outStream;

            try {
                String fn = makeFilename(currID, currName, ".txt");
                outStream = ctx.openFileOutput(fn, Context.MODE_PRIVATE);
                outStream.write((Long.toString(currNote.date) + "\n" + newText).getBytes());
                outStream.close();
                ITEM_MAP.get(currID).text = newText;
                ITEMS.get(currPosition).text = newText;
                ITEMS_ADAPTER.notifyDataSetChanged();
            } catch (Exception e) {
                Log.d("DEBUG", e.toString() + " -- Unable to save the note '" + currName + "'");
                return false;
            }

            return true;
        }

        return true;
	}

    public static boolean rename(Context ctx, int position, String newName) {
        boolean renamed = false;
        String id = getNoteFromPosition(position).id;
        String filename = getFilenameFromID(id);
        File file = new File(ctx.getFilesDir(), filename);

        try {
            String[] tokens = getTokensFromFilename(filename);
            String newFilename = makeFilename(id, newName, tokens[2]);
            File newFile = new File(ctx.getFilesDir(), newFilename);

            renamed = file.renameTo(newFile);

            if (renamed) {
                SoundNote sn = ITEM_MAP.get(id);
                SoundNote newSn = new SoundNote(id, newName, sn.text, sn.date);

                ITEM_MAP.put(id, newSn);
                ITEMS.set(position, newSn);
                ITEMS_ADAPTER.notifyDataSetChanged();
            }
        } catch (WrongFileNameException e) {
            Log.d("DEBUG", "Renaming note, catched WrongFileNameException");
        }

        return renamed;
    }

	public static boolean delete(Context ctx, int position) {
        String id = getNoteFromPosition(position).id;
        File file = new File(ctx.getFilesDir(), getFilenameFromID(id));
        boolean deleted = file.delete();

        if (deleted) {
            ITEM_MAP.remove(id);
            ITEMS.remove(position);
            ITEMS_ADAPTER.notifyDataSetChanged();
        }

        return deleted;
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
			return "    " + name; // Metterei la rappresentazione completa del dato ma l'Array Adapter che
                         // uso per la lista di note visualizza il valore di ritorno di toString
		}
	}
}
