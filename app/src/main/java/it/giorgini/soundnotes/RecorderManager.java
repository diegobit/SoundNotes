package it.giorgini.soundnotes;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;

import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

enum MRState {
    INITIAL,                // subito dopo OnCreate
    DATASOURCECONFIGURED,   // alla fine di OnStartCommand (service avviato con un intent)
    PREPARED,               // dopo aver preparato il mediarecorder, pronto per registrare
    RECORDING,              // sta registrando
    RELEASED                // prima di chiuderlo
}



public class RecorderManager extends Service {

    public static final String ACTION_START = "it.giorgini.soundnotes.RecorderManager.START";
    public static final String ACTION_STOP = "it.giorgini.soundnotes.RecorderManager.STOP";
    public static final String ACTION_PREPARE = "it.giorgini.soundnotes.RecorderManager.PREPARE";
    public static final String ACTION_SERVICE_INIT = "it.giorgini.soundnotes.RecorderManager";

    private MediaRecorder mr = new MediaRecorder();
    private int[] defaultPrefs = {MediaRecorder.AudioSource.MIC,
                                  MediaRecorder.OutputFormat.MPEG_4,
                                  MediaRecorder.AudioEncoder.AAC_ELD};
    private MRState state;
    private int[] prefs;
    private String mainPath;
    private String currRecPath;
//    private String noteID;

    private long startTime; // Per sapere la lunghezza della registrazione
    private long endTime;

    private NotificationManager notifManager;

    // Unique Identification Number for the Notification. We use it on Notification start, and to cancel it.
    private int NOTIFICATION = R.string.recorder_manager;

    /**
     * Class for clients to access.  Because we know this service always runs in the same process as
     * its clients, we don't need to deal with IPC.
     */
//    public class LocalBinder extends Binder {
//        RecorderManager getService() {
//            return RecorderManager.this;
//        }
//    }

    private WeakReference<Context> ctx;
//    private WeakReference<Handler> handlerUI;

    public RecorderManager() {}

    @Override
    public void onCreate() {
        Log.d("DEBUG", "### Service onCreate");
        super.onCreate();

        this.ctx = new WeakReference<>(getApplicationContext());
        prefs = defaultPrefs;
        state = MRState.INITIAL;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Questo metodo viene chiamato ogni volta che arriva uno startintent al service.
        // cellulare: NoteDetailActivity - tablet: NoteListActivity
        Log.d("DEBUG", "### Service: id " + startId + ": " + intent);
        String action;

        // Se intent == null significa che il service è stato riavviato dal sistema
        if (intent == null) {
            Log.d("DEBUG", "### Service restarted");
        } else {
            action = intent.getAction();

            // inizializzo o processo l'azione richiesta (avvio registrazione, stop, prepara registratore)
            switch (action) {
                case ACTION_SERVICE_INIT:
                    Log.d("DEBUG", "### Service Started");
                    if (intent.hasExtra("mainPath")) {
                        mainPath = intent.getStringExtra("mainPath");
                    }
                    break;
                case ACTION_START:
                    start();
                    Log.d("DEBUG", "### Service: recording started");
                    break;
                case ACTION_STOP:
                    stop();
                    Log.d("DEBUG", "### Service: recording stopped");
                    break;
                case ACTION_PREPARE:
                    Log.d("DEBUG", "### Service prepared");
                    setRecPath(intent.getStringExtra("noteID"));
                    setParameters(prefs[0], prefs[1], prefs[2], currRecPath);

                    prepare();
                    break;
            }
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        // Tell the user we stopped.
        Log.d("DEBUG", "### Service destroyed");
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

//    // This is the object that receives interactions from clients.  See
//    // RemoteService for a more complete example.
//    private final IBinder mBinder = new LocalBinder();

    // Per cambiare le impostazioni della registrazione
    public void setRecPath(String id) {
        currRecPath = mainPath + "/" + id + "-temp.m4a/"; // -temp lo rinomino a fine registrazione nel tempo della registrazione. Per
                                                              // ora mi va bene così
        Log.d("DEBUG", "#### RecMan currNoteFolder: " + currRecPath);
    }

    // Per cambiare le impostazioni della registrazione in futuro
	public void setParameters(int AudioSource, int OutputSource, int AudioEncoder, String path) throws IllegalStateException {
        if (state == MRState.RECORDING) {
            Log.d("DEBUG", "### !!! Prepare chiamate mentre stai registrando !!!");
        } else {
            if (state != MRState.INITIAL) {
                mr.reset();
            }
            mr.setAudioSource(AudioSource);
            mr.setOutputFormat(OutputSource);
            mr.setAudioEncoder(AudioEncoder);
            mr.setOutputFile(path);
            state = MRState.DATASOURCECONFIGURED;
        }
	}
	
	public void prepare() {
        if (state == MRState.DATASOURCECONFIGURED) {
            AsyncTask<Integer, Float, Boolean> pbg = new PrepareBG();
            pbg.execute(0);
        } else {
            throw new IllegalStateException("MediaRecorder is in an incorrect state (" + state + "), should be MRState.DATASOURCECONFIGURED");
        }
	}

    public void start() {
        if (state == MRState.PREPARED) {
            mr.start();
            startTime = System.currentTimeMillis();

        } else {
            throw new IllegalStateException("MediaRecorder is in an incorrect state (" + state + "), should be MRState.PREPARED");
        }
    }

    public void stop() {
        try {
            // Fermo la registrazione e salvo la nota
            mr.stop();
            save();

            // Aggiorno il momento della fine della registrazione
            endTime = System.currentTimeMillis();

            Log.d("DEBUG", "### RecMan stop OK");
        } catch (IllegalStateException e) {
            throw new IllegalStateException("MediaRecorder is in an incorrect state (" + state + "), it should be MRState.RECORDING (rarely INITIAL)");
        } catch (RuntimeException e) {
            File file = new File(currRecPath);
            boolean deleted = file.delete();
            if (!deleted) {
                Log.d("DEBUG", "#### RecMan stop: stop exception. File not deleted");
            } else {
                Log.d("DEBUG", "#### RecMan stop: stop exception. File deleted");
            }
        }

        // Reinizializzo il mediarecorder
        setParameters(prefs[0], prefs[1], prefs[2], currRecPath);
        prepare();
    }

    /** Metodo per portare il recorder manager in stato di "release": tutto deallocato
     *
     */
    public void release() { // TODO: service deve terminare quando chiudo l'app e non sto registrando
        try {
            if (state == MRState.RECORDING) {
                stop();
            } else {
                mr.reset();
            }
            mr.release();
        } catch (IllegalStateException e) {
            throw new IllegalStateException("MediaRecorder is in an incorrect state (" + state + ")");
        } catch (RuntimeException e) {
            File file = new File(currRecPath);
            boolean deleted = file.delete();
            if (!deleted) (Toast.makeText(ctx.get(), "", Toast.LENGTH_LONG)).show(); //TODO serve toast?
        }
        Log.d("DEBUG", "#### RecMan release ok");
    }

    private boolean save() {
        // TODO: implementare save?
        return false;
    }



    private class PrepareBG extends AsyncTask<Integer, Float, Boolean> {
//        private WeakReference<Context> ctx;

//        public PrepareBG(WeakReference<Context> ctx) {
//            this.ctx = ctx;
//        }
        public PrepareBG() { }

        @Override
        protected Boolean doInBackground(Integer... params) {
            try {
                mr.prepare();
                state = MRState.PREPARED;
            } catch (IllegalStateException e) {
                throw new IllegalStateException ("MediaRecorder is in an incorrect state (" + state + "), should be MRState.DATASOURCECONFIGURED");
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }

            return true;
        }

//        @Override
//        protected void onPostExecute(Boolean result) {
//            if (!result)
//                (Toast.makeText(ctx.get(), R.string.rec_prepare_error, Toast.LENGTH_LONG)).show();
//            else {
//                (Toast.makeText(ctx.get(), "prepare terminata", Toast.LENGTH_LONG)).show();
//            }
//            super.onPostExecute(result);
//        }
    }
}
