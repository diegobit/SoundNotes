package it.giorgini.soundnotes;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

enum MRState {
    INITIAL,                // subito dopo OnCreate
    DATASOURCECONFIGURED,   // alla fine di OnStartCommand (service avviato con un intent)
    PREPARED,               // dopo aver preparato il mediarecorder, pronto per registrare
    RECORDING,              // sta registrando
    RELEASED                // prima di chiuderlo
}



public class RecorderService extends Service implements MediaPlayer.OnPreparedListener,
        MediaPlayer.OnInfoListener, MediaPlayer.OnErrorListener {

    public static final String ACTION_START = "it.giorgini.soundnotes.recordermanager.START";
    public static final String ACTION_START_ACCEPTED = "it.giorgini.soundnotes.recordermanager.START.ACCEPTED";
    public static final String ACTION_STOP = "it.giorgini.soundnotes.recordermanager.STOP";
    public static final String ACTION_PREPARE = "it.giorgini.soundnotes.recordermanager.PREPARE";
    public static final String ACTION_PLAYER_START = "it.giorgini.soundnotes.recordermanager.PLAYER.START";
    public static final String ACTION_PLAYER_PAUSE = "it.giorgini.soundnotes.recordermanager.PLAYER.PAUSE";

    // stringhe per il broadcast receiver che riceverà le richieste del recorder
    public static final String REC_START_REQUEST = "it.giorgini.soundnotes.recstartrequest";
    public static final String REC_STOPPED = "it.giorgini.soundnotes.recstopped";
    public static final String PLAYER_STARTED = "it.giorgini.soundnotes.playerstarted";
    public static final String PLAYER_STOPPED = "it.giorgini.soundnotes.playerstopped";

//    public static final String EXTRA_MAIN_PATH = "extra_mainPath";
    public static final String EXTRA_TWO_PANE = "extra_mTwoPane";
    public static final String EXTRA_NOTEID = "extra_noteID";
    public static final String EXTRA_REC_TIME = "extra_recTime";

    private MediaRecorder mr;
    private MediaPlayer mp;
    private int[] defaultPrefs = {MediaRecorder.AudioSource.MIC,
                                  MediaRecorder.OutputFormat.AAC_ADTS,
                                  MediaRecorder.AudioEncoder.AAC};
    private boolean hasToStop = false;
    private static MRState state;
    private static String recNoteID;
    private static String recNoteName;
    private static int recNoteLine;
    private int[] prefs;
    private String filesDir;
    private String currRecDir;
    private String currRecRelPath;
    private long currRecLength;
    private static boolean playing = false;
    private static int currPlayingLine = -1;

    private long startTime; // Per sapere la lunghezza della registrazione
    private long endTime;

    // Unique Identification Number for the Notification. We use it on Notification start, and to cancel it.
    private int notifID = 1;


    /**
     * Class for clients to access.  Because we know this service always runs in the same process as
     * its clients, we don't need to deal with IPC.
     */
//    public class LocalBinder extends Binder {
//        RecorderService getService() {
//            return RecorderService.this;
//        }
//    }

    public RecorderService() {}

    @Override
    public void onCreate() {
        Log.i("SN ###", "Service onCreate");
        super.onCreate();

        currPlayingLine = -1;
        prefs = defaultPrefs;
        filesDir = getApplicationContext().getFilesDir().getAbsolutePath();
        mr = new MediaRecorder();
        state = MRState.INITIAL;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Questo metodo viene chiamato ogni volta che arriva uno startintent al service.
        // cellulare: NoteDetailActivity - tablet: NoteListActivity
        Log.d("SN ###", "Service: id " + startId + ": " + intent);
        String action;

        // intent == null significa che il service è stato riavviato dal sistema.
        if (intent == null) {
            Log.w("SN ###", "Service intent == null PROBLEMA, non dovrebbe succedere");
        }
        // Action == null significa che ho appena avviato una activity. Il service forse sta già registrando.
        else if ((action = intent.getAction()) == null) {
            if (!isRecording() && !isPlaying()) {
                // qui il service non era rimasto in background a registrare...
                Log.i("SN ###", "Service Started");
//                if (intent.hasExtra(EXTRA_MAIN_PATH))
//                    filesDir = intent.getStringExtra(EXTRA_MAIN_PATH);
//                mTwoPane = intent.getBooleanExtra(EXTRA_TWO_PANE, false);
            } else {
                Log.i("SN ###", "Service NOT started, already recording or playing");
            }
        }
        // Action != null ho qualcosa da fare...
        else {
            // ...processo l'azione richiesta (avvio registrazione, stop, prepara registratore)
            switch (action) {
                case ACTION_START:
                    Log.i("SN ###", "Service: recording maybe starting. Asking to RecordingsView...");
                    start();
                    break;
                case ACTION_START_ACCEPTED:
                    Log.i("SN ###", "Service: recording started!!");
                    startAccepted();
                    break;
                case ACTION_STOP:
                    Log.i("SN ###", "Service: recording stopped");
                    // questo arriva solo dalla notifica. Se l'app è visiblie stoppo, se no rilascio.
                    if (LifecycleHandler.isApplicationVisible()) {
                        stop();
                    } else {
                        hasToStop = true;
                        release();
                    }
                    break;
                case ACTION_PREPARE:
                    Log.i("SN ###", "Service prepare started");
                    setRecPath(intent.getStringExtra(EXTRA_NOTEID));
                    if (state == MRState.INITIAL)
                        setParameters(prefs[0], prefs[1], prefs[2]);
                    if (state == MRState.DATASOURCECONFIGURED)
                        prepare();
                    break;
                case ACTION_PLAYER_START:
                    startPlaying(intent.getStringExtra("path"),
                                 intent.getIntExtra("line", 0));
                    break;
                case ACTION_PLAYER_PAUSE:
                    pausePlaying();
                    break;
                default:
                    Log.w("SN ###", "Service: it shouldn't arrive here");
                    break;
            }
        }

        // non voglio essere riavviato quando vengo killato
        return START_NOT_STICKY;
    }


    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

//    // This is the object that receives interactions from clients.  See
//    // RemoteService for a more complete example.
//    private final IBinder mBinder = new LocalBinder();

    public static MRState getState() {
        return state;
    }

    public static String getCurrRecNoteID() {
        return recNoteID;
    }

    public static String getCurrRecNoteName() {
        return recNoteName;
    }

    public static void setCurrRecNoteName(String newName) {
        recNoteName = newName;
    }

    public static int getCurrRecNoteLine() {
        return recNoteLine;
    }

    public static void setCurrRecNoteLine(int line) {
        recNoteLine = line;
    }

    public static boolean isRecording() {
        return state == MRState.RECORDING;
    }

    public static boolean isPlaying() {
        return playing;
    }

    // Per cambiare le impostazioni della registrazione
    public void setRecPath(String id) {
//        currRecDir = getDir(id, Context.MODE_PRIVATE).getPath();
        File dir = new File(getFilesDir(), File.separator + id);
        dir.mkdir();
        currRecDir = dir.getPath();
        currRecRelPath = "temp.aac";  // -temp lo rinomino a fine registrazione nel tempo della registrazione. Per
                                      // ora mi va bene così
        Log.d("SN ###", "RecMan filesDir: " + filesDir + "; currRecDir: " + currRecDir + "; relpath: " + currRecRelPath);
    }

    // Per cambiare le impostazioni della registrazione in futuro
	public void setParameters(int AudioSource, int OutputSource, int AudioEncoder) throws IllegalStateException {
        if (isRecording() || isPlaying()) {
            Log.w("SN ###", "!!! setParameters chiamata mentre stai registrando o riproducendo... mmmh");
        } else {
            if (state != MRState.INITIAL) {
                mr.reset();
            }
            state = MRState.INITIAL;
            mr.setAudioSource(AudioSource);
            mr.setOutputFormat(OutputSource);
            mr.setAudioEncoder(AudioEncoder);
            mr.setOutputFile(currRecDir + File.separator + currRecRelPath);
            state = MRState.DATASOURCECONFIGURED;
        }
	}
	
	public void prepare() {
        Log.d("SN @@@", "RecServ prepare middle");
        if (state == MRState.DATASOURCECONFIGURED) {
            try {
//                AsyncTask<Integer, Float, Boolean> prepAsync = new PrepareAsync();
//                prepAsync.execute(0);     //TODO: usare questo. Crasha a volte perché viene chiamato start prima della fine della prepare.
                mr.prepare();
                state = MRState.PREPARED;
            } catch (IllegalStateException e) {
                throw new IllegalStateException ("MediaRecorder is in an incorrect state (" + state + "), should be MRState.DATASOURCECONFIGURED");
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            throw new IllegalStateException("MediaRecorder is in an incorrect state (" + state + "), should be MRState.DATASOURCECONFIGURED");
        }
	}

//    private class PrepareAsync extends AsyncTask<Integer, Float, Boolean> {
//
//        public PrepareAsync() { }
//
//        @Override
//        protected Boolean doInBackground(Integer... params) {
//            try {
//                mr.prepare();
//                state = MRState.PREPARED;
//                Log.d("SN @@@", "RecServ prepareBG - PREPARED!");
//            } catch (IllegalStateException e) {
//                throw new IllegalStateException ("MediaRecorder is in an incorrect state (" + state + "), should be MRState.DATASOURCECONFIGURED");
//            } catch (IOException e) {
//                e.printStackTrace();
//                return false;
//            }
//
//            return true;
//        }
//    }

    public void start() {
        Log.d("SN @@@", "RecServ start1 - state: " + state);
        if (state != MRState.PREPARED) {
            Log.i("SN @@@", "recServ start - rec non era ancora pronto");
            if (state == MRState.INITIAL)
                setParameters(prefs[0], prefs[1], prefs[2]);
            if (state == MRState.DATASOURCECONFIGURED)
                prepare();
        }
        createRecOnText();
    }

    private void startAccepted() {
        try {
            // rilascio il player se serve
            if (mp != null) {
                mp.release();
                mp = null;
            }

            mr.start();
            startTime = System.currentTimeMillis();
            recNoteID = StorageManager.currID;
            recNoteName = StorageManager.currName;
            state = MRState.RECORDING;
            // Diventa un service foreground e crea la notifica
            startForeground(1, createNotification());
        } catch (IllegalStateException e) {
            throw new IllegalStateException("MediaRecorder is in an incorrect state (" + state + "), should be MRState.PREPARED");
        } catch (RuntimeException e) {
            Toast.makeText(getApplicationContext(), R.string.action_rec_startfail, Toast.LENGTH_LONG).show();
            e.printStackTrace();
            mr.reset();
        }
    }

    public void stop() {
        try {
            // Fermo la registrazione e salvo la nota
            mr.stop();
            // Aggiorno il momento della fine della registrazione
            endTime = System.currentTimeMillis();
            // notifico l'activity
            setRecLenghtOnText();
            // salvo la nota correttamente
            saveRecording();
            // smetto di essere un service in background e rimuovo la notifica
            stopForeground(true);

            Log.d("SN ###", "RecMan stop OK");
        } catch (IllegalStateException e) {
            Toast.makeText(getApplicationContext(), R.string.action_rec_stopfail, Toast.LENGTH_LONG).show();
            Log.d("SN ###", "MediaRecorder is in an incorrect state (" + state + "), it should be MRState.RECORDING (rarely INITIAL)");
            e.printStackTrace();
        } catch (RuntimeException e) {
            e.printStackTrace();
            File file = new File(currRecDir, currRecRelPath);
            boolean deleted = file.delete();
            if (!deleted) {
                Log.d("SN ###", "RecMan stop: stop exception. File not deleted");
            } else {
                Log.d("SN ###", "RecMan stop: stop exception. File deleted");
            }
            mr.reset();
        }

        // resetto il mediarecorder
        recNoteName = "I'm not recording";
        state = MRState.INITIAL;

        // rimuovo l'iconcina della registrazione dalla lista
        StorageManager.toggleRecState();
    }


    /** Metodo per portare il recorder manager in stato di "release": tutto deallocato */
    public void release() {
        try {
            if (isRecording()) {
                stop();
            } else if (state != MRState.INITIAL) {
                mr.reset();
            }
            mr.release();
            mr = null;
        } catch (IllegalStateException e) {
            throw new IllegalStateException("MediaRecorder is in an incorrect state (" + state + ")");
        } catch (RuntimeException e) {
            File file = new File(currRecDir, currRecRelPath);
            boolean deleted = file.delete();
            if (!deleted)
                Log.i("SN @@@", "recServ stop rec non riuscito e temp non eliminato... temp verrà però sovrascritto se registro ancora nella stessa nota");
        }
        Log.i("SN ###", "Service release ok");
        if (hasToStop) {
            stopSelf();
        }
    }

    // line è la linea della registrazione corrente. Non è la linea corrente, ma quella salvata
    // dentro la registrazione.
    public void startPlaying(String path, int line) {
        // significa che ho riprodotto un pezzo di registrazione
        if (mp != null && !isPlaying() && line == currPlayingLine) {
            Log.i("SN @@@", "recServ startPlaying start dopo pausa: ");
            finallyStartPlaying();
        }
        else {
            if (mp == null) {
                Log.i("SN @@@", "recServ startPlaying start inizializzando mp: ");
                mp = new MediaPlayer();
                mp.setOnPreparedListener(this);
                mp.setOnInfoListener(this);
                mp.setOnErrorListener(this);
                mp.setOnCompletionListener(new MediaPlayerOnCompletionListener());

                // Wake lock per evitare che la cpu vada in sleep
                mp.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);

                mp.setLooping(false);
                mp.setAudioStreamType(AudioManager.STREAM_MUSIC);
            } else {
                Log.i("SN @@@", "recServ startPlaying start mp da resettare: ");
                if (isPlaying())
                    mp.stop();
                mp.reset();
            }

            try {
                File f = new File(new File(getFilesDir(), StorageManager.getCurrNote().id), path);
                ArrayList<RecordingsView.Recording> recList = StorageManager.getCurrNote().recList;
                mp.setDataSource((new FileInputStream(f)).getFD());
                mp.prepareAsync();
                currPlayingLine = line;
            } catch (IOException e) {
                Log.i("SN @@@", "recServ startPlaying errore. path " + path + ", line " + line + " completo: " + new File(new File(getFilesDir(), StorageManager.getCurrNote().id), path).getPath());
                e.printStackTrace();
                releasePlayer(); // chiamo stopforeground qui dentro anche sae non ho chiamato startForeground... problemi?
            }
        }
    }

    public void finallyStartPlaying() {
        mp.start();
        playing = true;
        // divento un service in foreground, attivo la notifica e lo dico all'activity
        startForeground(1, createNotification());
        sendPlayPauseStatusToActivity();

//        // devo fermare la riproduzione prima della fine per evitare errori del mediaplayer :/
//        long duration = StorageManager.getCurrNote().recList.get(currPlayingLine).lenghtMillis;
//        long durationFixed = duration > 500 ? duration - 300 : duration / 2;
//        Log.i("SN @@@", "recServ player duration: " + duration + "dur fix: " + durationFixed);
//        if (handler != null)
//            handler.removeCallbacksAndMessages(null);
//        handler = new Handler();
//        handler.postDelayed(stopPlayerTask, durationFixed);
    }

    public void sendPlayPauseStatusToActivity() {
        Intent i;
        if (isPlaying())
            i = new Intent(PLAYER_STARTED);
        else
            i = new Intent(PLAYER_STOPPED);
        LocalBroadcastManager.getInstance(this).sendBroadcast(i);
    }

    /** Called when MediaPlayer is ready */
    public void onPrepared(MediaPlayer player) {
        Log.i("SN @@@", "recServ startPlaying prepared");
        finallyStartPlaying();
    }

    private class MediaPlayerOnCompletionListener implements MediaPlayer.OnCompletionListener {
        public void onCompletion(MediaPlayer mp) {
            Log.i("SN @@@", "recServ player playback completed");
            if (mp != null && isPlaying())
                mp.stop();
            releasePlayer();
            sendPlayPauseStatusToActivity();
        }
    }

    Runnable stopPlayerTask = new Runnable(){
        @Override
        public void run() {
            Log.i("SN @@@", "recServ player playback stopped with runnable");
            releasePlayer();
        }};

//    @Override
//    public void onCompletion(MediaPlayer mp) {
//        Log.i("SN @@@", "recServ player playback completed");
//        mp.stop();
//        releasePlayer();
//        sendPlayPauseStatusToActivity();
//    }

    @Override
    public boolean onInfo(MediaPlayer mp, int what, int extra) {
        switch (what) {
            case MediaPlayer.MEDIA_INFO_UNKNOWN:
                Log.i("SN @@@", "recServ startPlaying info: what MEDIA_INFO_UNKNOWN (" + what + "), extra " + extra);
                break;
            case MediaPlayer.MEDIA_INFO_BAD_INTERLEAVING:
                Log.i("SN @@@", "recServ startPlaying info: what MEDIA_INFO_BAD_INTERLEAVING (" + what + "), extra " + extra);
                break;
            case MediaPlayer.MEDIA_INFO_BUFFERING_END:
                Log.i("SN @@@", "recServ startPlaying info: what MEDIA_INFO_BUFFERING_END (" + what + "), extra " + extra);
                break;
            case MediaPlayer.MEDIA_INFO_BUFFERING_START:
                Log.i("SN @@@", "recServ startPlaying info: what MEDIA_INFO_BUFFERING_START (" + what + "), extra " + extra);
                break;
            case MediaPlayer.MEDIA_INFO_METADATA_UPDATE:
                Log.i("SN @@@", "recServ startPlaying info: what MEDIA_INFO_METADATA_UPDATE (" + what + "), extra " + extra);
                break;
            case MediaPlayer.MEDIA_INFO_NOT_SEEKABLE:
                Log.i("SN @@@", "recServ startPlaying info: what MEDIA_INFO_NOT_SEEKABLE (" + what + "), extra " + extra);
                break;
            case MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START:
                Log.i("SN @@@", "recServ startPlaying info: what MEDIA_INFO_VIDEO_RENDERING_START (" + what + "), extra " + extra);
                break;
            case MediaPlayer.MEDIA_INFO_VIDEO_TRACK_LAGGING:
                Log.i("SN @@@", "recServ startPlaying info: what MEDIA_INFO_VIDEO_TRACK_LAGGING (" + what + "), extra " + extra);
                break;
            case MediaPlayer.MEDIA_INFO_UNSUPPORTED_SUBTITLE:
                Log.i("SN @@@", "recServ startPlaying info: what MEDIA_INFO_UNSUPPORTED_SUBTITLE (" + what + "), extra " + extra);
                break;
            case MediaPlayer.MEDIA_INFO_SUBTITLE_TIMED_OUT:
                Log.i("SN @@@", "recServ startPlaying info: what MEDIA_INFO_SUBTITLE_TIMED_OUT (" + what + "), extra " + extra);
                break;
        }
        return true;
    }

    @SuppressLint("InlinedApi")
    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        switch (what) {
            case MediaPlayer.MEDIA_ERROR_UNKNOWN:
                Log.i("SN @@@", "recServ startPlaying error: what MEDIA_ERROR_UNKNOWN");
                break;
            case MediaPlayer.MEDIA_ERROR_SERVER_DIED:
                Log.i("SN @@@", "recServ startPlaying error: what MEDIA_ERROR_SERVER_DIED");
                break;
        }
        if (NoteListActivity.deviceApiIsAtLeast(Build.VERSION_CODES.JELLY_BEAN_MR1)) {
            switch (extra) {
                case MediaPlayer.MEDIA_ERROR_IO:
                    Log.i("SN @@@", "recServ startPlaying error: extra MEDIA_ERROR_IO");
                    break;
                case MediaPlayer.MEDIA_ERROR_MALFORMED:
                    Log.i("SN @@@", "recServ startPlaying error: extra MEDIA_ERROR_MALFORMED");
                    break;
                case MediaPlayer.MEDIA_ERROR_UNSUPPORTED:
                    Log.i("SN @@@", "recServ startPlaying error: extra MEDIA_ERROR_UNSUPPORTED");
                    break;
                case MediaPlayer.MEDIA_ERROR_TIMED_OUT:
                    Log.i("SN @@@", "recServ startPlaying error: extra MEDIA_ERROR_TIMED_OUT");
                    break;
            }
        }

        // Gestisco l'errore resettando il media player e dicendolo all'activity
        releasePlayer();
        sendPlayPauseStatusToActivity();

        return false;
    }

    public void pausePlaying() {
        if (mp != null) {
//            if (handler != null)
//                handler.removeCallbacksAndMessages(null);
            mp.pause();
            stopForeground(true);
        } else {
            Log.w("SN @@@", "recServ pausePlaying chiamato ma mp == null");
        }
        playing = false;
    }

    public void releasePlayer() {
        if (mp != null) {
            // rimuovo handler se ci sono
//            if (handler != null) {
//                handler.removeCallbacksAndMessages(null);
//                handler = null;
//            }
            if (mp.isPlaying())
                mp.stop();
            mp.reset();
            mp.release();
            mp = null;
            currPlayingLine = -1;
            playing = false;
            // smetto di essere un service in foreground e rimuovo la notifica
            stopForeground(true);
        }
    }

    private void saveRecording() {
//        String id = getCurrRecNoteID();
        String newName = getCurrRecNoteLine() + "-" + currRecLength + ".aac";
        File f = new File(currRecDir, currRecRelPath);
        Log.w("SN @@@", "RecServ save file (" + f.getPath() + ") CAN be read, l: " + f.length());
        File f2 = new File(currRecDir, newName);
        boolean renamed = f.renameTo(f2);
        if (renamed) {
            StorageManager.updateCurrRecordingInfo(getCurrRecNoteLine(), currRecLength);
        }
        else {
            // assesto la situazione salvando le altre note
            StorageManager.saveAllRecordings();
            Log.w("SN @@@", "RecServ save - NOT renamed. Retrying...");
            renamed = f.renameTo(f2);
        }
        if (!renamed)
            Log.w("SN @@@", "RecServ save - 2th NOT renamed");
        else
            Log.w("SN @@@", "RecServ save - 2th RENAMED - file (" + f2.getPath() + ") CAN be read, l: " + f2.length());

//        startPlayingDummy(rec);
    }

    @Override
    public void onDestroy() {
        Log.d("SN ###", "Service onDestroy forst");
        // se sto registrando salvo prima di release
        if (isRecording()) {
            stop();
        } else if (isPlaying()) {
            mp.stop();
        }
        if (mr != null) {
            mr.release();
            mr = null;
        }
        releasePlayer();
        Log.d("SN ###", "Service onDestroy after");
        super.onDestroy();
    }

    public void createRecOnText() {
        // lo dico alla detailactivity così crea la nota nella RecordingsView
        Intent i = new Intent(REC_START_REQUEST);
        LocalBroadcastManager.getInstance(this).sendBroadcast(i);
    }

    public void setRecLenghtOnText() {
        Log.d("SN @@@", "RecMan setRecLenghtOnText");
        currRecLength = endTime - startTime;
        Intent i = new Intent(REC_STOPPED);
        i.putExtra(EXTRA_REC_TIME, currRecLength);

        LocalBroadcastManager.getInstance(this).sendBroadcast(i);
    }

    public static boolean isInTheSameNoteAsRecording() {
        String id = RecorderService.getCurrRecNoteID();
        // null, non ci sono problemi
        return id == null || RecorderService.getCurrRecNoteID().equals(StorageManager.currID);
    }

    @SuppressLint("InlinedApi")
    public Notification createNotification() {
        // Intent quando clicco sulla notifica
        Intent intent = new Intent(getApplicationContext(), NoteListActivity.class);
        intent.setAction(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, intent, 0);

        // Intent quando clicco su stop
        Intent stopIntent = new Intent(this, RecorderService.class);
        if (isRecording()) {
            stopIntent.setAction(RecorderService.ACTION_STOP);
        } else {
            stopIntent.setAction(RecorderService.ACTION_PLAYER_PAUSE);
            stopIntent.putExtra("stop", true);
        }
        PendingIntent pendingStopIntent = PendingIntent.getService(this, 0, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        String noteName = StorageManager.currName;

        // creo la notifica
        NotificationCompat.Builder nBuilder =
                new NotificationCompat.Builder(getApplicationContext())
                        .setSmallIcon(R.drawable.ic_stat_notify)
                        .setColor(getResources().getColor(R.color.primary))
                        .setContentText(noteName)
                        .setStyle(new NotificationCompat.BigTextStyle().bigText(noteName))
                        .setOngoing(true)
                        .setContentIntent(pendingIntent);

        // se sto registrando o riproducendo
        if (isRecording()) {
            nBuilder.setContentTitle(getString(R.string.notif_rec_title))
                    .addAction(R.drawable.ic_action_stop, getString(R.string.notif_rec_stop), pendingStopIntent);
        } else {
            nBuilder.setContentTitle(getString(R.string.notif_play_title))
                    .addAction(R.drawable.ic_action_stop, getString(R.string.notif_play_stop), pendingStopIntent);
        }

        // la categoria per lollipop
        if (NoteListActivity.deviceApiIsAtLeast(Build.VERSION_CODES.LOLLIPOP)) {
            nBuilder.setCategory(Notification.CATEGORY_SERVICE)
                    .setVisibility(Notification.VISIBILITY_PRIVATE);
        }

        return nBuilder.build();
    }
}
