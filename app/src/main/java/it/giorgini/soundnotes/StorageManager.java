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
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.ImageView;

enum SortMode {
    ALPHABETIC_ASCENDING, ALPHABETIC_DESCENDING, CREATIONTIME_ASCENDING, CREATIONTIME_DESCENDING
}



public class StorageManager {

    private static WeakReference<Activity> activity;
    private static SharedPreferences dataPrefs;

	public static ArrayList<SoundNote> ITEMS;
    public static NoteListAdapter ITEMS_ADAPTER;
	public static HashMap<String, SoundNote> ITEM_MAP;
    public static String textExtensions = ".txt";
    public static String audioExtensions = ".aac";
	public static String currName ;
	public static String currID;
	public static int currPosition = -1;

    public static SortMode SORTOPTION = SortMode.CREATIONTIME_DESCENDING;

	static {
		ITEMS = new ArrayList<>();
		ITEM_MAP = new HashMap<>();
	}

    // Inizializzazione all'avvio dell'activity principale dell'app
    public static void init(Activity act, SharedPreferences dataPreferences) {
        activity = new WeakReference<>(act);
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

    public static void updateCurrRecordingInfo(int position, long length) {
        RecordingsView.Recording rec = getCurrNote().recList.get(position);
        rec.position = position;
        rec.setLenght(length);
    }

    public static SoundNote getCurrNote() {
        return ITEM_MAP.get(currID);
    }

//    public static SoundNote getNoteFromId(String id) {
//        return ITEM_MAP.get(id);
//    }

    public static int getNotePosFromID(String noteID) {
        for (int i = 0; i < ITEMS.size(); i++) {
            if (ITEMS.get(i).id.equals(noteID))
                return i;
        }
        Log.w("SN @@@", "StorageManager getNotePosFromID -- NON TROVATA!!! ritornato 0");
        return 0;
    }

    public static SoundNote getNoteFromPosition(int pos) {
        return ITEMS.get(pos);
    }

    /** Dato il nome di un file di una nota/registrazione ritorna una tripla:
     * - testo: id, nome della nota da mostrare, estensione
     * - registrazione: posizione, lunghezza, estensione
     * esempi:
     * 13-nomeNota.txt --> <13, nomenota, txt>
     * 2-600.txt --> <2, 600, txt>
     * @param p path del file
     * @return una tripla di valori. Se l'estensione non è tra quelle accettate
     * (vedi textExtensions), lancia l'eccezione WrongExtensionException
     */
    private static String[] getTokensFromFilename(String p) throws WrongFileNameException {
        if (p == null)
            throw new IllegalArgumentException("The argument is not initialized");

        int firstMinus = p.indexOf("-");
        int lastDot = p.lastIndexOf(".");
        if (firstMinus == -1 || lastDot == -1)
            throw new WrongFileNameException("Incorrect filename. Should be xx-yyy.ext");

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



//    public static boolean isInteger(String s) {
//        try {
//            Integer.parseInt(s);
//            return true;
//        } catch(NumberFormatException e) {
//            return false;
//        }
//    }
    public static boolean isInteger(String str) {
        if (str == null) {
            return false;
        }
        int length = str.length();
        if (length == 0) {
            return false;
        }
        int i = 0;
        if (str.charAt(0) == '-') {
            if (length == 1) {
                return false;
            }
            i = 1;
        }
        for (; i < length; i++) {
            char c = str.charAt(i);
            if (c <= '/' || c >= ':') {
                return false;
            }
        }
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
                        return (Long.valueOf(n1.date)).compareTo(n2.date);
                        // NOTA SE CERCHERAI DEI BUG: se non ordina bene rimettere n2.date dentro un Long
                    }
                };
                break;

            case CREATIONTIME_DESCENDING:
                comp = new Comparator<SoundNote>() {
                    @Override
                    public int compare(SoundNote n1, SoundNote n2) {
                        return (Long.valueOf(n2.date)).compareTo(n1.date);
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

    private static void addRecToNote(String noteID, String position, String length) {
        SoundNote sn = ITEM_MAP.get(noteID);
        sn.addRec(position, length);
    }

    public static boolean saveAllRecordings() {
        ArrayList<Integer> recLeftToRename = new ArrayList<>();
        ArrayList<RecordingsView.Recording> recList = StorageManager.getCurrNote().recList;
        File recDir = new File(activity.get().getFilesDir(), "/" + currID);
        recDir.mkdir();
//        File recDir = activity.get().getDir(currID, Context.MODE_PRIVATE);

        // ciclo su ogni riga della nota
        for (int i = 0; i < recList.size(); i++) {
            RecordingsView.Recording rec = recList.get(i);
            // rec != null -> nella riga c'è una registrazione
            if (rec != null) {
                // ho modificato la nota e devo rinominare il file in memoria
                if (i != rec.position) {
                    if (!renameRecording(recDir, rec, i)) {
                        // non sono riuscito a rinominare. Aggiungo alla lista delle rec non spostate e riprovo dopo
                        recLeftToRename.add(i);
                        Log.i("SN ###", "StorMan saveAll: non rinominata da pos" + rec.position + "a " + i);
                    }
                    else
                        Log.i("SN ###", "StorMan saveAll: rinominata! da pos" + rec.position + "a " + i + " (giusto che siano uguali");
                }
            }
        }

        int timeout = 0;
        int renamed = 0;
        int len = recLeftToRename.size();
        // ciclo sulle rec non ancora rinominate. Questo while serve per tenere conto di altre possibili collisioni
        while (renamed < len && timeout < 50) {
            // il ciclo delle note da rinominare
            for (int j = len - 1; j >= 0; j--) {
                int newPos = recLeftToRename.get(j);
                if (renameRecording(recDir, recList.get(newPos), newPos)) {
                    recLeftToRename.remove(j);
                    renamed++;
                    Log.i("SN ###", "StorMan saveAll: rinominata dopo collisione! da pos" + recList.get(newPos).position + "a " + newPos + " (giusto che siano uguali");
                    j--; // rimuovo, devo tenere lo stesso indice;
                }
            }
            timeout++;
        }
        Log.i("SN ###", "StorMan saveAll: non rinom dopo collisione! da pos");

        return renamed == len;
    }

    private static boolean renameRecording(File recDir, RecordingsView.Recording rec, int newPos) {
        File oldName = new File(recDir, rec.position + "-" + rec.lenghtMillis + ".aac");
//        oldName.setReadable(true, false);
        File newName = new File(recDir, newPos + "-" + rec.lenghtMillis + ".aac");
        //        newName.setReadable(true, false);
        return oldName.renameTo(newName);
    }

    /** Carico tutti i file dalla memoria interna per mostrarne la lista. Se ritorna false è necessario sotituire il fragment della
     * lista delle note con quello vuoto a mano
     *
     * @return true se ho caricato almeno un elemento
     *         false altrimenti
     */

    public static boolean load() { //TODO: farlo in backgroud
        ITEMS_ADAPTER = new NoteListAdapter(activity.get(), ITEMS);

        String[] files = activity.get().fileList();

        // Carico le note solo se ci sono dei file nella memoria interna
        if (files.length != 0) {
            int counter = 0;
            long date = 0;
            File filesDir = activity.get().getFilesDir();
            String[] id_name_ext;
            StringBuilder text = new StringBuilder();
            ArrayList<File> dirToDo = new ArrayList<>();

            // Itero: per ogni file creo una nuova nota
            for (String path : files) {
                try {
                    Log.w("SN ###", "storman load inizio for sui file - " + path);
                    // popolo l'array di cartelle. Le scorro tutte insieme dopo quando ho già creato le note
//                    //TODO: performance: creare nota con quello che trovo per primo (anche se rec)
                    File d = (new File(path));
                    // perchè nelle cartelle ci sono le registrazioni
                    if (!d.isDirectory()) {
//                        dirToDo.add(d);
//                    else {
                        // Isolo il nome della nota dal nome del file (e ignoro il file se non è uno corretto)
                        id_name_ext = getTokensFromFilename(path);
                        Log.i("SN ###", "StorMan load inizio: " + path);

                        // Devo leggere il file. Può essere la parte testuale o una audio
                        if (textExtensions.contains(id_name_ext[2])) {
                            // Parte testuale: Leggo riga per riga
                            FileInputStream is = activity.get().openFileInput(path);
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
                        }
                    } else {
                        Log.w("SN ###", "StorMan load trovata cartella: " + d.getAbsolutePath());
                    }
//                    else {
//                        //
//                        //TODO: TOGLIERE
//                        //
//                        // Parte audio: // Ho trovato un file audio non vuoto. Ho già suddiviso le parti. Aggiungo.
//                        File f = ew File(filesDir, path);
//                        if (f.length() == 0 || f.getName().equals("temp.aac")) {
//                            if (f.delete())
//                                Log.i("SN ###", "StorMan Load: cancellato file audio: " + path);
//                            else
//                                Log.i("SN ###", "StorMan Load: NON cancellato file audio: " + path);
//                        } else {
//                            Log.i("SN ###", "StorMan Load: trovato file audio: " + path + ", size: " + f.length() + "B");
//                        }
//                    }
//                    }

                } catch (WrongFileNameException e) {
                    // un file col nome diverso... strano
                    Log.i("SN ###", "StorMan load file formato diverso: " + path);
                } catch (OutOfMemoryError e) {
                    e.printStackTrace();
                } catch (FileNotFoundException e) {
                    Log.w("SN ###", "Strange, I scan for file and then read them");
                    e.printStackTrace();
                } catch (UnsupportedEncodingException e) {
                    Log.w("SN ###", "Strange, I save this files myself in UTF-8");
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                // azzero lo StringBuilder
                text.setLength(0);
            }

            // Ho caricato tutte le note:
            // - carico anche le registrazioni
            // - ordino la lista. Se non ci sono elementi carico la vista speciale (??)
            if (counter > 0) {
                for (int i = 0; i < ITEMS.size(); i++) {
                    Log.w("SN ###", "Load entrato nel for di una cartella per i file audio");
                    SoundNote sn = ITEMS.get(i);
                    String id = sn.id;
//                    File dir = activity.get().getDir(id, Context.MODE_PRIVATE);
                    File dir = new File(activity.get().getFilesDir(), File.separator + id);
                    dir.mkdir();
                    File[] fileList = dir.listFiles();
                    // per ogni cartella devi ciclare sui file all'interno. Ogni file è una registrazione da aggiungere
                    // alla nota corrispondente. Ogni rec ha ne nome la posizione nell'array delle registrazioni della nota.
                    for (File rec : fileList) {
                        try {
                            String[] pos_len_ext = getTokensFromFilename(rec.getName());
                            if (audioExtensions.contains(pos_len_ext[2])) {
                                addRecToNote(id, pos_len_ext[0], pos_len_ext[1]);
                                Log.w("SN ###", "Load file audio fatto: id " + id + ", pos "
                                        + pos_len_ext[0] + ", len " + pos_len_ext[1] + "pathdir: " + dir.getPath());
                            } else {
                                // È una registrazione non rinominata correttamente. //TODO: fornire modo per recuperarle e piazzarle
                                // La cancello
                                Log.i("SN ###", "StorMan load file formato diverso: " + rec.getName());
                                File f = new File(rec.getName());
                                if (rec.getName().equals("temp.aac")) {
                                    if (f.delete())
                                        Log.i("SN ###", "StorMan Load: cancellato file audio: " + rec.getName());
                                    else
                                        Log.i("SN ###", "StorMan Load: NON cancellato file audio: " + rec.getName());
                                }
                            }
                        } catch (WrongFileNameException e) {
                            // Non faccio niente, non mi interessa questo file
                            Log.w("SN ###", "Load file ignorato catched wrongfilenamexception");
                        }
                    }
                }

                // metto in ordine la lista delle note.
                sortList(SORTOPTION);
            }

            return true;
        }

        // nessun file caricato
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
                int textLen = newText.length();
                if (textLen != 0) {
                    if (textLen < 100)
                        ITEMS.get(currPosition).preview = newText.substring(0, textLen).replace('\n', ' ');
                    else
                        ITEMS.get(currPosition).preview = newText.substring(0, 100).replace('\n', ' ');
                }
                ITEMS.get(currPosition).text = newText;
                ITEMS_ADAPTER.notifyDataSetChanged();
            } catch (Exception e) {
                Log.d("SN ###", e.toString() + " -- Unable to save the note '" + currName + "'");
                return false;
            }

        }

        // Ora provo a salvare le registrazioni
        return saveAllRecordings();
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
            Log.d("SN ###", "Renaming note, catched WrongFileNameException");
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

    public static void toggleRecState() {
        int pos = getNotePosFromID(RecorderService.getCurrRecNoteID());

        ViewGroup list = ITEMS_ADAPTER.getParentView();
        if (list != null) {
            ViewGroup row = (ViewGroup) list.getChildAt(pos);
            if (row != null) {
                int childs = row.getChildCount();
                if (RecorderService.isRecording() && childs == 1) {
                    Log.d("SN @@@", "row != null - pos: " + pos + " row childs: " + row.getChildCount());
                    ImageView iv = (ImageView) LayoutInflater.from(activity.get()).inflate(R.layout.note_list_rec_image, row, false);
                    row.addView(iv, 1);
                } else if (!RecorderService.isRecording() && childs == 2) {
                    // tolgo l'immagine. C'è ma non sto registrando
                    row.removeViewAt(1);
                }
            }
        }
    }






	/**
	 * Questa classe rappresenta una singola nota
	 */
	public static class SoundNote {
		public String id;
		public String name;
		public String text;
        public String preview = "";
        public ArrayList<RecordingsView.Recording> recList;
        public long date;

		public SoundNote(String id, String name, String text, long date) {
			this.id = id;
			this.name = name;
			this.text = text;
            setPreview();
            this.date = date;
		}

        private void setPreview() {
            int len = this.text.length();
            if (len > 0) {
                len = len < 100 ? len : 100;
                this.preview = text.substring(0, len).replace('\n', ' ');
            }
        }

        public void addRec(String position, String length) {
            try {
                int pos = Integer.parseInt(position);
                int len = Integer.parseInt(length);

                int diff = 0;
                if (recList == null)
                    recList = new ArrayList<>();
                diff = pos - recList.size();
                if (diff >= 0) {
                    for (int i = 0; i <= diff; i++) {
                        recList.add(null);
                    }
                }

                recList.set(pos, new RecordingsView.Recording(pos, len));
            } catch (NumberFormatException e) {
                e.printStackTrace();
                Log.e("SN ###", "StorMan SoundNote addRec: pasteInteger non riucita. Rec non aggiunta");
            }
        }

		@Override
		public String toString() {
			return id + "|||" + name + "|||" + text + "|||" + preview + "|||" + recList.toString() + "|||" + date;
		}
	}
}
