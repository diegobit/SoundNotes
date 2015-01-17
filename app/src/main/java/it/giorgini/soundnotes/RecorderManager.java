package it.giorgini.soundnotes;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Handler;
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
//                      	        if (Build.VERSION.SDK_INT == 15)
                                  MediaRecorder.OutputFormat.MPEG_4,
//	    	                        else
//			                            mr.setOutputFormat(MediaRecorder.OutputFormat.AAC_ADTS);
                                  MediaRecorder.AudioEncoder.AAC_ELD};
    private MRState state;
    private int[] prefs;
    private String mainPath;
    private String currRecPath;
    private String noteID;
//    private String nextRecPath;

    private long startTime; // Per sapere la lunghezza della registrazione
    private long endTime;

    private NotificationManager notifManager;

    // Unique Identification Number for the Notification. We use it on Notification start, and to cancel it.
    private int NOTIFICATION = R.string.recorder_manager;

    /**
     * Class for clients to access.  Because we know this service always runs in the same process as
     * its clients, we don't need to deal with IPC.
     */
    public class LocalBinder extends Binder {
        RecorderManager getService() {
            return RecorderManager.this;
        }
    }

    private WeakReference<Context> ctx;
//    private WeakReference<Handler> handlerUI;

    public RecorderManager() {}

//	@SuppressLint("InlinedApi")
//	public RecorderManager(Context ctx, Handler handlerUI, String path, String noteID) {
//        state = MRState.INITIAL;
//        this.ctx = new WeakReference<Context>(ctx);
//        this.handlerUI = new WeakReference<Handler>(handlerUI);
//
//        prefs = defaultPrefs;
//        mainPath = path;
//        setRecPath(noteID);
//        setParameters(prefs[0], prefs[1], prefs[2], currRecPath);
//	}

    @Override
    public void onCreate() {
        super.onCreate();

        this.ctx = new WeakReference<Context>(getApplicationContext());
//        this.handlerUI = new WeakReference<Handler>(handlerUI);
        prefs = defaultPrefs;
        state = MRState.INITIAL;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Questo metodo viene chiamato ogni volta che arriva uno startintent al service.
        // cellulare: NoteDetailActivity - tablet: NoteListActivity
        Log.i("LocalService", "Received start id " + startId + ": " + intent);
        String action;

        // Se intent == null significa che il service è stato riavviato dal sistema
        if (intent == null) {
            action = "restarted";
            Log.d("LocalService", "restarted");
        } else {
            action = intent.getAction();

            // inizializzo o processo l'azione richiesta (avvio registrazione, stop, prepara registratore)
            switch (action) {
                case ACTION_SERVICE_INIT:
                    if (intent.hasExtra("mainPath")) {
                        mainPath = intent.getStringExtra("mainPath");
                    }
                    Toast.makeText(this, "onStartCommand - start service", Toast.LENGTH_SHORT).show();
                    break;
                case ACTION_START:
                    start();
                    Toast.makeText(this, "onStartCommand - start", Toast.LENGTH_SHORT).show();
                    break;
                case ACTION_STOP:
                    pause();
                    Toast.makeText(this, "onStartCommand - pause", Toast.LENGTH_SHORT).show();
                    break;
                case ACTION_PREPARE:
                    setRecPath(intent.getStringExtra("noteID"));
                    setParameters(prefs[0], prefs[1], prefs[2], currRecPath);

                    notifManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                    // Display a notification about us starting.  We put an icon in the status bar.
                    showNotification();

                    prepare();
                    Toast.makeText(this, "onStartCommand - prepare", Toast.LENGTH_SHORT).show();
                    break;
            }
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        // Cancel the persistent notification.
        notifManager.cancel(NOTIFICATION);

        // Tell the user we stopped.
        Toast.makeText(this, R.string.recorder_manager, Toast.LENGTH_SHORT).show();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

//    // This is the object that receives interactions from clients.  See
//    // RemoteService for a more complete example.
//    private final IBinder mBinder = new LocalBinder();

    /**
     * Show a notification while this service is running.
     */
    private void showNotification() {
        CharSequence text = getText(R.string.recorder_manager);

        // Set the icon, scrolling text and timestamp
        Notification.Builder nB = new Notification.Builder(this);
        Notification notif = nB.build();

        // The PendingIntent to launch our activity if the user selects this notification
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, NoteListActivity.class), 0);

//        notif.setLatestEventInfo(this, getText(R.string.local_service_label),
//                text, contentIntent);

        // Send the notification.
        notifManager.notify(NOTIFICATION, notif);
    }

    // Per cambiare le impostazioni della registrazione
    public void setRecPath(String id) {
        noteID = id;
        currRecPath = mainPath + "/" + noteID + "-temp.m4a/"; // -temp lo rinomino a fine registrazione nel tempo della registrazione. Per
                                                              // ora mi va bene così
        Log.d("DEBUG", "#### RecMan currNoteFolder: " + currRecPath);
    }

//    public void computeNextRecPath() {
//        //TODO: implementare
//        nextRecPath = currRecPath + "1.aac";
//    }

    // Per cambiare le impostazioni della registrazione in futuro
	public void setParameters(int AudioSource, int OutputSource, int AudioEncoder, String path) throws IllegalStateException {
        Log.d("DEBUG", "#### RecMan state: " + state + "; setParameters: " + AudioSource + "-" + OutputSource + "-" + AudioEncoder + "; " + path);
        if (state == MRState.RECORDING) {
            Toast.makeText(this, "!!!!!! setParameters mentre recording!!!!", Toast.LENGTH_SHORT).show();
            // TODO: togliere toast e mettere log o togliere tutto
        } else {
            if (state != MRState.INITIAL) {
                mr.reset();
            }
            mr.setAudioSource(AudioSource);
            mr.setOutputFormat(OutputSource);
            mr.setAudioEncoder(AudioEncoder);
            mr.setOutputFile(path);
            state = MRState.DATASOURCECONFIGURED;
//        else {
//            throw new IllegalStateException("MediaRecorder is in an incorrect state (" + state + "), should be MRState.INITIAL");
//        }
        }
	}
	
	public void prepare() {
        if (state == MRState.DATASOURCECONFIGURED) {
            AsyncTask<Integer, Float, Boolean> pbg = new PrepareBG(ctx);
            pbg.execute(0);
        } else {
            throw new IllegalStateException("MediaRecorder is in an incorrect state (" + state + "), should be MRState.DATASOURCECONFIGURED");
        }
	}

    public void start() {
//        handlerUI.get().post(new Runnable() {
//            @Override
//            public void run() {
//                mr.start();
//            }
//        });
        if (state == MRState.PREPARED) {
            mr.start();
            startTime = System.currentTimeMillis();
        } else {
            throw new IllegalStateException("MediaRecorder is in an incorrect state (" + state + "), should be MRState.PREPARED");
        }
        Log.d("DEBUG", "#### RecMan start");
    }

    public void pause() {
        try {
            // Fermo la registrazione e salvo la nota
            mr.stop();
            save();

            // Aggiorno il momento della fine della registrazione
            endTime = System.currentTimeMillis();

            Log.d("DEBUG", "#### RecMan pause: stop ok");
        } catch (IllegalStateException e) {
            throw new IllegalStateException("MediaRecorder is in an incorrect state (" + state + "), it should be MRState.RECORDING (rarely INITIAL)");
        } catch (RuntimeException e) {
            File file = new File(currRecPath);
            boolean deleted = file.delete();
            if (!deleted) {
                Log.d("DEBUG", "#### RecMan pause: stop exception. File not deleted");
                (Toast.makeText(ctx.get(), "", Toast.LENGTH_LONG)).show(); //TODO serve toast?
            } else {
                Log.d("DEBUG", "#### RecMan pause: stop exception. File deleted?");
            }
        }

        // Reinizializzo il mediarecorder
        setParameters(prefs[0], prefs[1], prefs[2], currRecPath);
        prepare();
    }

    public void release() {
        try {
            mr.reset();
            mr.release();
        } catch (IllegalStateException e) {
            throw new IllegalStateException("MediaRecorder is in an incorrect state (" + state + ")");
        } catch (RuntimeException e) {
            File file = new File(currRecPath);
            boolean deleted = file.delete();
            if (!deleted) (Toast.makeText(ctx.get(), "", Toast.LENGTH_LONG)).show(); //TODO serve toast?
        }
        Log.d("DEBUG", "#### RecMan stop: stop");
    }



    private boolean save() {
        // TODO: implementare
        return false;
    }




    private class PrepareBG extends AsyncTask<Integer, Float, Boolean> {
        private WeakReference<Context> ctx;

        public PrepareBG(WeakReference<Context> ctx) {
            this.ctx = ctx;
        }

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

        @Override
        protected void onPostExecute(Boolean result) {
            if (!result)
                (Toast.makeText(ctx.get(), R.string.rec_prepare_error, Toast.LENGTH_LONG)).show();
            else {
                (Toast.makeText(ctx.get(), "prepare terminata", Toast.LENGTH_LONG)).show();
            }
            //FIXME: per ora creo toast per segnalare la fine di prepare. ma è da rimuovere.
            super.onPostExecute(result);
        }
    }
}
