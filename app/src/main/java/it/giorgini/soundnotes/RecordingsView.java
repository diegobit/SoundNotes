package it.giorgini.soundnotes;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.DragEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

public class RecordingsView extends View {

    RichEditText editText;
    Paint lgrey = new Paint();
    Paint orange = new Paint();
    Paint orangeText = new Paint();
    Paint orangeShadow = new Paint();
    final float leftMargin = getResources().getDimension(R.dimen.rec_left_margin);
    final float smallLeftMargin = getResources().getDimension(R.dimen.rec_left_margin_small);
    final float vertMargin = getResources().getDimension(R.dimen.rec_vertical_margin);
    final float centerWidth = dp2px(23);
    final float rightMargin = dp2px(1);

    private ArrayList<Recording> recList;
    private CoordinatesArray coordinates;

//    boolean isRecording;
    int currRecLine = 0;
    int currRecDevLine = 0;
    int editTextDevLineCount = 0;
//    int editTextLastPosition = 0;
//    int interlinea;
//    int baseline;

    public RecordingsView(Context context) {
        super(context);
        init();
    }

    public RecordingsView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public RecordingsView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

//    public RecordingsView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
//        super(context, attrs, defStyleAttr, defStyleRes);
//        init();
//    }

    private void init() {
//        editText = (RichEditText) findViewById(R.id.note_detail);
//        setBackgroundResource(Color.TRANSPARENT);
        lgrey.setColor(getResources().getColor(R.color.line_lgrey));
        orangeText.setColor(getResources().getColor(R.color.primary));
        orangeText.setAntiAlias(true);
        orangeText.setTextSize(getResources().getDimensionPixelSize(R.dimen.rec_text));
        orangeText.setTextAlign(Paint.Align.CENTER);
        orange.setColor(getResources().getColor(R.color.line_orange));
        orangeShadow.setColor(getResources().getColor(R.color.line_shadow));

        coordinates = new CoordinatesArray(1000);
    }

    // inizializzo l'array recList se serve , coordinates ci pensa da solo
    private void allocateRecList() {
        int diff = editText.countOccurrences('\n', editText.getText().toString(), 0, '\0') - recList.size();
        for (int i = 0; i <= diff; i++) {
            recList.add(null);
        }
    }

    public void setAssociatedEditText(RichEditText ret) {
        editText = ret;
    }

    public void setEditTextDevLineCount(int count) {
        editTextDevLineCount = count;
    }

    public void setCurrRecList() {
        if (StorageManager.getCurrNote().recList == null) {
            StorageManager.getCurrNote().recList = new ArrayList<>();
        }
        recList = StorageManager.getCurrNote().recList;
        allocateRecList();
        Log.i("SN @@@", "RecView recList + " + this.recList + " currNote: " + StorageManager.getCurrNote().id);

        // Devo anche inizializzare coordinatesArray!
        for (int i = 0; i < recList.size(); i++) {
            if (recList.get(i) != null) {
                int devLine = editText.getDeviceLineFromLine(StorageManager.getCurrNote().text, i);
                fillCoordinatesArrayAt(devLine);
            }
        }
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // Separatore Registrazioni|EditText
        canvas.drawLine(leftMargin, vertMargin, leftMargin, getHeight(), lgrey);

        // scorro la lista delle registrazioni della nota e le disegno:
        if (recList != null) {
            for (int i = 0; i < recList.size(); i++) {
                Recording rec = recList.get(i);
                if (rec != null) {
                    int width = getWidth();
                    int devLine = editText.getDeviceLineFromLine(null, i); // FIXME: performance?
                    int interline = coordinates.getInterline(devLine);
                    int baseline = coordinates.getBaseline(devLine);

                    // Disegno la linea
                    canvas.drawLine(smallLeftMargin, interline - 1, width - rightMargin, interline - 1,
                            orangeShadow);
                    canvas.drawLine(smallLeftMargin, interline, width - rightMargin, interline,
                            orangeText);
                    canvas.drawLine(smallLeftMargin, interline + 1, width - rightMargin, interline + 1,
                            orangeShadow);
                    // Scrivo il testo
                    if (rec.hasDuration()) {
                        if (rec.hasHours()) {
                            orangeText.setTextScaleX((float) 0.8);
                            canvas.drawText(rec.lenghtFormatted, centerWidth, baseline, orangeText);
                            orangeText.setTextScaleX((float) 1);
                        } else
                            canvas.drawText(rec.lenghtFormatted, centerWidth, baseline, orangeText);
                    } else {
                        canvas.drawText("REC", centerWidth, baseline, orangeText);
                    }
                }
            }
        } else
            Log.i("SN @@@", "RecView draw recList == null");
    }

    // Aggiunge esteticamente una registrazione nella nota se non sto già registrando e se non sto registrando su un'altra rec
    public boolean newRecording() {
        // sto già registrando, non ne inizio una nuova
        if (RecorderService.isRecording()) {
            Toast.makeText(getContext(), R.string.rec_forbidden_start, Toast.LENGTH_LONG).show();
            return false;
        }

//        Log.i("SN @@@", "RecordingsView newRec - prima currRecLine: " + currRecLine);
        currRecLine = editText.getCurrLine();
        RecorderService.setCurrRecNoteLine(currRecLine);
        currRecDevLine = editText.getDeviceLineFromLine(null, currRecLine);
//        Log.i("SN @@@", "RecordingsView newRec - dopo currRecLine: " + currRecLine);

        // alloco e riempio gli array fino a currRecLine
        allocateRecList();
        fillCoordinatesArrayAt(currRecDevLine);

        // aggiungo una registrazione all'array di recordings
        Recording rec = new Recording(currRecLine);
        try {
            if (recList.get(currRecLine) == null) {
                recList.set(currRecLine, rec);
                invalidate();
                return true;
            } else {
//                recList.set(currRecLine, rec);
//                invalidate();
                Toast.makeText(getContext(), R.string.rec_forbidden_position, Toast.LENGTH_LONG).show();
                return false;
            }
        } catch (IndexOutOfBoundsException e) {
            Log.e("SN @@@", "RecView newRec: indexOutOfBound !!!!!!!!");
            return false;
        }

        //TODO: implementare (anche notelistactivity)
    }

    public void stoppedRecording(long recTime) {
        recList.get(currRecLine).setLenght(recTime);
//        rec.setLenght(recTime);
//        recList.set(currRecLine, rec);
        if (RecorderService.isInTheSameNoteAsRecording()) {
            invalidate();
        }
    }

    public void fillCoordinatesArrayAt(int devLine) {
        if (!coordinates.hasValues(devLine)) {
            // calcolo le coordinate della dev-linea corrente per poter disegnarci intorno
            int baseline = editText.getLayout().getLineBaseline(devLine);
            int interlinea;
            if (devLine > 0) {
                int baselinePrev = editText.getLayout().getLineBaseline(devLine - 1);
                int descentPrev = editText.getLayout().getLineDescent(devLine - 1);
                interlinea = baselinePrev + descentPrev + Math.round(dp2px(1));
            } else {
                interlinea = Math.round(dp2px(2));
            }
            coordinates.set(devLine, baseline, interlinea, true);
        }
    }

//    public void onCharAdded(CharSequence addedText) {
//        if (addedText.equals("\n")) {
//            insertNewline();
//        } else {
//            int newDevLineCount = editText.get().getLineCount();
//            int diff = newDevLineCount - editTextDevLineCount;
//            editTextDevLineCount = newDevLineCount;
//            if (diff > 0) {
//                int currDevLine = editText.get().getCurrDevLine();
//                for (int i =  currDevLine + 1; i < newDevLineCount; i++) {
//                    fillCoordinatesArrayAt(i);
//                }
//                if (RecorderService.getState() == MRState.RECORDING && currRecDevLine > currDevLine)
//                    currRecDevLine += diff;
//
//                invalidate();
//            }
//        }
//    }

    public void onCharAdded(int lastDevLineCount, int lastDevLine) {
        if (RecorderService.isInTheSameNoteAsRecording()) {
            int newDevLineCount = editText.getLineCount();

            // il numero di righe device è aumentato
            int diff = newDevLineCount - lastDevLineCount;
            if (diff > 0) {

                for (int i = lastDevLine + 1; i < newDevLineCount; i++) {
                    fillCoordinatesArrayAt(i);
                }
                if (RecorderService.isRecording() &&
                        RecorderService.isInTheSameNoteAsRecording() &&
                        currRecDevLine > lastDevLine)
                    currRecDevLine += diff; // diff dovrebbe sempre essere 1, ma vabbè, meglio diff
            }
            invalidate();
        }
    }

    public void onCharDeleted(int lastDevLineCount, int lastDevLine) {
        if(RecorderService.isInTheSameNoteAsRecording()) {
            int newDevLineCount = editText.getLineCount();

            // il numero di righe device è diminuito, diff < 0
            int diff = newDevLineCount - lastDevLineCount;
            if (diff < 0) {

                for (int i = lastDevLine; i < newDevLineCount; i++) {
                    fillCoordinatesArrayAt(i);
                }
                if (RecorderService.isRecording() &&
                        RecorderService.isInTheSameNoteAsRecording() &&
                        currRecDevLine >= lastDevLine)
                    currRecDevLine += diff; // diff dovrebe sempre essere 1, ma vabbè, meglio diff
            }
            invalidate();
        }
    }

    // Con il cursore posizionato alla linea line l'utente ha premuto invio. Devo spostare le registrazioni
    public void insertNewline() {
        if (RecorderService.isInTheSameNoteAsRecording()) {
            int line = editText.getCurrLine();
            allocateRecList();
            recList.add(line + 1, null);
            // controllo che ci siano coordinate per le linee successive a line
            for (int i = editText.getDeviceLineFromLine(null, line) + 1; i < editText.getLineCount(); i++) {
                fillCoordinatesArrayAt(i);
            }
            // aggiorno currRecLine e currRecDevLine
            if (RecorderService.isRecording()) {
                if (currRecLine > line) {
                    currRecLine++;
                    RecorderService.setCurrRecNoteLine(currRecLine);
                }
                if (currRecDevLine > editText.getCurrDevLine())
                    currRecDevLine++;
            }
            invalidate();
        }
    }

    // Inserisce n linee dopo prevSelPos. prev perchè viene chiamato dopo aver incollato dalla clipboard
    public void insertNewline(int n, int prevSelPos, int prevDevLineCount) {
        if (RecorderService.isInTheSameNoteAsRecording()) {
            int line = editText.getLineForOffset(prevSelPos);
            int devLine = editText.getDeviceLineForOffset(prevSelPos);
            allocateRecList();
            for (int i = 0; i < n; i++) {
                recList.add(line + 1, null);
            }
            // controllo che ci siano coordinate per le linee successive a line
            for (int i = devLine; i < editText.getLineCount(); i++) {
                fillCoordinatesArrayAt(i);
            }
            // aggiorno currRecLine e currRecDevLine
            if (RecorderService.isRecording()) {
                if (currRecLine > line) {
                    for (int i = 0; i < n; i++) {
                        currRecLine++;
                    }
                    RecorderService.setCurrRecNoteLine(currRecLine);
                }
                if (currRecDevLine > devLine) {
                    int diff = editText.getLineCount() - prevDevLineCount;
                    currRecDevLine += diff;
                }
            }

            invalidate();
        }
    }

    // ero in una posizione e ho premuto backspace e sto per cancellare newline.
    // Ritorno false se non posso farlo, altrimenti true e sposto le rec
    public boolean removeNewlineBeforeCurrPos() {
        if (RecorderService.isInTheSameNoteAsRecording()) {
            // non permetto di cancellare se nella stessa linea c'è una registrazione e:
            // - in quella prima c'è del testo
            // - oppure una registrazione
            int line = editText.getCurrLine();
            allocateRecList();
            if (recList.get(line) != null &&                                    // devo essere proprio alla testa della rec. Dopo permetto sempre
                    (editText.getLineText(line - 1).trim().length() > 0)/* ||      // tolgo gli spazi
                (lineBelongsToRecording(line - 1)))*/) {

                if (lineBelongsToRecording(line - 1) != -1) {
                    // oltre al testo c'è anche una registrazione. Lo comunico all'utente
                    Toast.makeText(getContext(), R.string.rec_forbidden_backspace_rec_before, Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(getContext(), R.string.rec_forbidden_backspace, Toast.LENGTH_LONG).show();
                }
                return false;
            }

            if (recList.get(line) == null) {
                recList.remove(line);
            } else {
                recList.remove(line - 1);
            }

            // controllo che ci siano coordinate per le linee successive a line
            for (int i = editText.getDeviceLineFromLine(null, line - 1); i < editText.getLineCount(); i++) {
                fillCoordinatesArrayAt(i);
            }

            // aggiorno currRecLine e currRecDevLine
            if (RecorderService.isRecording()) {
                if (currRecLine >= line) {
                    currRecLine--;
                    RecorderService.setCurrRecNoteLine(currRecLine);
                }
                if (currRecDevLine > editText.getCurrDevLine())
                    currRecDevLine--;
            }

            //  non invalido perché lo devo fare dopo. Lo faccio da RecInputConnectionWrapper
            Log.d("SN %%%", "1.5 true - currRedDevLine(hoTolro1): " + currRecDevLine + " currRecLine(hoTolto1): " + currRecLine);
            return true;
        } else
            return true;
    }

    // ho premuto backspace con un bel testo selezionato. Controllo se ho cancellato qualche \n
    // true se faccio proseguire con l'azione, false altrimenti
    public boolean removeComplexNewline(int startSel, int endSel) {
        if (RecorderService.isInTheSameNoteAsRecording()) {
//            int startLine = editText.getLineForOffset(startSel);
//            int endLine = editText.getLineForOffset(endSel);
            allocateRecList();
            String selectedText = editText.getText().toString().substring(startSel, endSel);

            // Se startSel e endSel sono nella stessa linea allora non ci sono problemi
            int startLine = editText.getLineForOffset(startSel);
            int endLine = editText.getLineForOffset(endSel);
            if (startLine == endLine) {
                Log.d("SN %%%", "2.0 true - startL=endL - startLine: " + startLine + " endLine: " + endLine);
                return true;
            } else {
                // (controllo per debug) //TODO: togliere se non ci sono problemi
                if (!selectedText.contains("\n")) {
                    Log.w("SN $$$", "RecInputConnectionWrapper NON DOVREBBE ESSERCI \\n !!!! invece c'è. Sbagliato come conto le linee prima");
                    return true;
                }
                // Linee adiacenti. Dintinguo 2 casi:
                // - non ci sono registrazioni oppure ce n'è una nella riga sopra -> lascio cancellare
                // - ci sono due rec oppure ce n'è una sotto -> blocco la cancellazione
                if (endLine - startLine == 1) {

                    if (recList.get(endLine) != null) {
                        // - rec nella seconda(non mi interessa la prima) , blocco
                        Toast.makeText(getContext(), R.string.rec_forbidden_del_middle_rec, Toast.LENGTH_LONG).show();
                        Log.d("SN %%%", "2.5 false - endL-startL = 1 - currRecDevLine: " + currRecDevLine + " currRecLine: " + currRecLine);
                        return false;
                    } else {
                        // - rec nella prima e non nella seconda, permetto e shifto (togliendo la endline)
                        // - entrambe senza rec, permetto e shifto (tolgo la endline)
                        recList.remove(endLine);

                        // controllo che ci siano coordinate per le linee successive a line
                        for (int i = editText.getDeviceLineFromLine(null, startLine); i < editText.getLineCount(); i++) {
                            fillCoordinatesArrayAt(i);
                        }
                        if (RecorderService.isRecording()) {
                            if (currRecLine > endLine) {
                                currRecLine--;
                            }
                            if (currRecDevLine > editText.getCurrDevLine())
                                currRecDevLine--;
                        }
                        Log.d("SN %%%", "2.8 true - endL-startL = 1 - currRecDevLine(hoTolto1): " + currRecDevLine + " currRecLine(hoTolto1): " + currRecLine);
                        return true;
                    }

                } else {
                    // differenza numero linea > 1: sto cancellando almeno tra 3 linee, ne sto cancellando
                    // almeno una interamente.
                    int middleRecCount = countRecordingsBetweenLines(startLine + 1, endLine - 1);
                    if (middleRecCount > 0) {
                        // ce n'è almeno una centrale con una rec, se la cancello perdo la rec. Blocco
                        Toast.makeText(getContext(), R.string.rec_forbidden_del_middle_rec, Toast.LENGTH_LONG).show();
                        return false;
                    } else if (recList.get(endLine) != null) {
                        // anche se nell'ultima linea c'è una rec blocco.
                        Toast.makeText(getContext(), R.string.rec_forbidden_del_middle_rec, Toast.LENGTH_LONG).show();
                        return false;
                    } else {
                        // 2 casi: - rec nella prima e non delle altre;
                        // - nessuna rec in tutte le linee.
                        // Allora conto quanti '\n' cancello e shifto di quel numero
                        int newlineDeleted = endLine - startLine;
                        for (int i = 0; i < newlineDeleted; i++) {
                            recList.remove(startLine + 1);
                        }
                        // controllo che ci siano coordinate per le linee successive a devLine
                        for (int i = editText.getDeviceLineFromLine(null, startLine); i < editText.getLineCount(); i++) {
                            fillCoordinatesArrayAt(i);
                        }
                        // aggiorno currRecLine e currRecDevLine
                        if (RecorderService.isRecording()) {
                            if (currRecLine > startLine) {
                                for (int i = 0; i < newlineDeleted; i++) {
                                    currRecLine--;
                                    RecorderService.setCurrRecNoteLine(currRecLine);
                                }
                            }
                            if (currRecDevLine > editText.getCurrDevLine()) {
                                currRecDevLine = editText.getDeviceLineFromLine(null, currRecLine);
                            }
                        }
                        Log.d("SN %%%", "3.8 true - recPrima o nientePrimaESeconda - currRecDevLine(tolto1): " + currRecDevLine + " currRecLine(tolto1): " + currRecLine);
                        return true;
                    }
                }
            }
        } else
            return true;
    }

//    public boolean onDragStarted(DragEvent event) {
//        event.getClipDescription()
//        return true;
//    }

    public int countRecordingsBetweenLines(int startLine, int endLine) {
        int counter = 0;
        for (int i = startLine; i <= endLine; i++) {
            if (recList.get(i) != null)
                counter++;
        }
        return counter;
    }

    /** Metodo che ritorna true se nella linea line c'è una registrazione. Siccome per me ogni linea appartiene alla
     * registrazione sopra di essa, allora cerco rec anche nelle linee precedenti */
    public int lineBelongsToRecording(int line) {
        allocateRecList();
        for (int i = line; i >= 0; i--) {
            if (recList.get(i) != null)
                return i;
        }
        return -1;
    }

//    // ero nella linea line e ho premuto backspace e sto per cancellare newline.
//    // Ritorno false se non posso farlo, altrimenti true e sposto le rec
//    public boolean removeNewlineBefore(int line) {
//        // non permetto di cancellare se nella stessa linea c'è una registrazione e:
//        // - in quella prima c'è del testo
//        // - oppure una registrazione
//        allocateRecList();
////        Log.d("SN $$$", "line:'" + editText.getLineText(line).trim() + "'" + );
//        if (recList.get(line) != null &&
//                (editText.getLineText(line - 1).trim().length() > 0 ||      // tolgo gli spazi
//                        (recList.get(line - 1) != null))) {
//            Toast.makeText(getContext(), R.string.rec_forbidden_backspace, Toast.LENGTH_SHORT).show();
//            return false;
//        }
//
//        if (recList.get(line) == null)
//            recList.remove(line);
//        else
//            recList.remove(line - 1);
//
//        // controllo che ci siano coordinate per le linee successive a line
//        for (int i = line; i < recList.size(); i++) {
//            if (recList.get(i) != null)
//                fillCoordinatesArrayAt(i);
//        }
//
//        if (currRecLine >= line)
//            currRecLine--;
//
//        // non invalido perché lo devo fare dopo. Lo faccio da RecInputConnectionWrapper
//        return true;
//    }

    // Rimuovo una linea dalle registrazioni dopo la cancellazione premendo backspace di un \n
//    public void removeNewlineBefore(int line) {
//        // non c'è nulla di importante nella linea prima, proseguo
//        if (recList.get(line) == null)
//            recList.remove(line);
//        else
//            recList.remove(line - 1);
//
//        if (currRecLine >= line)
//            currRecLine--;
//
//        invalidate();
//        }

//        allocateRecList();
//        recList.add(line + 1, null);
//        // controllo che ci siano coordinate per le linee successive a line
//        for (int i = line; i < recList.size(); i++) {
//            if (recList.get(i) != null)
//                fillCoordinatesArrayAt(i);
//        }
//        // aggiorno currRecLine
//        if (currRecLine > line)
//            currRecLine++;
//
//        invalidate();

    public float dp2px(int dp) {
        float scale = getContext().getResources().getDisplayMetrics().density;
        return dp * scale + 0.5f;
    }

    public float px2dp(int px) {
        float scale = getContext().getResources().getDisplayMetrics().density;
        return (px - 0.5f) / scale;
    }

//    public Paint computeStringCoord(String s, int topLeftX, int topLeftY, int width, int height, Integer resultX, Integer resultY) {
//        Paint p = new Paint();
//        // height * 0,7 mi piace
//        int fontHeight = (int)(height * 0.7);
//
//        p.setTextSize(fontHeight);
//        p.setColor(getResources().getColor(R.color.primary));
//        p.setTextAlign(Paint.Align.CENTER);
//        Rect bounds = new Rect();
//        p.getTextBounds(s, 0, s.length(), bounds);
//        resultX = topLeftX + width / 2;
//        resultY = topLeftY + height / 2 + (bounds.bottom - bounds.top) / 2;
//        return p;
//    }

    public void clear() {
        if (!RecorderService.isRecording()) {
            currRecLine = 0;
            currRecDevLine = 0;
        }
    }












    /**
     * Questa classe rappresenta una singola nota
     */
    public static class Recording {
//        private int id;
        public int position;
        public long lenghtMillis;
        public String lenghtFormatted = null;

        public Recording(int position) {
//            this.id = id;
            this.position = position;
        }

        public Recording(int position, long lenghtMillis) {
//            this.id = id;
            this.position = position;
            this.lenghtMillis = lenghtMillis;
            this.lenghtFormatted = formattedStringFromMilliseconds(lenghtMillis);
        }



        public void setLenght(long lenghtMillis) {
            this.lenghtMillis = lenghtMillis;
            this.lenghtFormatted = formattedStringFromMilliseconds(lenghtMillis);
        }

//        public String getDuration() {
//            return this.lenghtFormatted;
//        }



        public boolean hasDuration() {
            return lenghtFormatted != null;
        }

        public boolean hasHours() {
            return lenghtFormatted.length() > 5;
        }



        public String toString() {
            if (hasDuration())
                return position + ", " + lenghtMillis + ", " + lenghtFormatted;
            else
                return position + ", " + lenghtMillis;
        }



        private String formattedStringFromMilliseconds(long millis) {
            long hours = TimeUnit.MILLISECONDS.toHours(millis);
            if (hours == 0) {
                return String.format("%02d:%02d",
                        TimeUnit.MILLISECONDS.toMinutes(millis) % TimeUnit.HOURS.toMinutes(1),
                        TimeUnit.MILLISECONDS.toSeconds(millis) % TimeUnit.MINUTES.toSeconds(1));
            } else if (hours <= 9) {
                return String.format("%2d:%02d:%02d",
                        hours,
                        TimeUnit.MILLISECONDS.toMinutes(millis) % TimeUnit.HOURS.toMinutes(1),
                        TimeUnit.MILLISECONDS.toSeconds(millis) % TimeUnit.MINUTES.toSeconds(1));
            } else {
                return String.format("%02d:%02d:%02d",
                        hours,
                        TimeUnit.MILLISECONDS.toMinutes(millis) % TimeUnit.HOURS.toMinutes(1),
                        TimeUnit.MILLISECONDS.toSeconds(millis) % TimeUnit.MINUTES.toSeconds(1));
            }
        }
    }

}