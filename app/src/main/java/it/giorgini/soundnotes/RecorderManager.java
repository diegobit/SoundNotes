package it.giorgini.soundnotes;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.Iterator;
import java.util.List;

import android.app.ActivityManager;
import android.app.FragmentManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.TaskStackBuilder;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Build;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
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
    public static final String ACTION_SERVICE_NAME = "it.giorgini.soundnotes.RecorderManager";

    private MediaRecorder mr = new MediaRecorder();
    private int[] defaultPrefs = {MediaRecorder.AudioSource.MIC,
                                  MediaRecorder.OutputFormat.MPEG_4,
                                  MediaRecorder.AudioEncoder.AAC_ELD};
    private boolean mTwoPane;
    private static MRState state;
    private int[] prefs;
    private String mainPath;
    private String currRecPath;
    private String currNote;
//    private String noteID;

    private long startTime; // Per sapere la lunghezza della registrazione
    private long endTime;

    // Unique Identification Number for the Notification. We use it on Notification start, and to cancel it.
    private int notifID = 1;

//    private Callbacks callbacks_DetailFragment = defaultCallbacks_DetailFragment;

//    public interface Callbacks {
//        public void setRecordingIcon(boolean isRecording);
//    }

    /**
     * Chiamati solo se non trova i metodi del fragment
     */
//    private static Callbacks defaultCallbacks_DetailFragment = new Callbacks() {
//        @Override
//        public void setRecordingIcon(boolean isRecording) { }
//    };

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
        Log.i("SN ###", "Service onCreate");
        super.onCreate();

        this.ctx = new WeakReference<>(getApplicationContext());
        prefs = defaultPrefs;
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
            Log.d("SN ###", "Service restarted");
        }
        // Action == null significa che ho appena avviato una activity. Il service forse sta già registrando.
        else if ((action = intent.getAction()) == null) {
            if (state != MRState.RECORDING) {
                // qui il service non era rimasto in background a registrare...
                Log.i("SN ###", "Service Started");
                if (intent.hasExtra("mainPath"))
                    mainPath = intent.getStringExtra("mainPath");
                mTwoPane = intent.getBooleanExtra("mTwoPane", false);
            } else {
                Log.i("SN ###", "Service NOT started, already recording");
            }
        }
        // Action != null ho qualcosa da fare...
        else {
            // ...processo l'azione richiesta (avvio registrazione, stop, prepara registratore)
            switch (action) {
                case ACTION_START:
                    Log.i("SN ###", "Service: recording started");
                    start();
                    break;
                case ACTION_STOP:
                    Log.i("SN ###", "Service: recording stopped");
                    stop();
                    break;
                case ACTION_PREPARE:
                    Log.i("SN ###", "Service prepared");
                    setRecPath(intent.getStringExtra("noteID"));
                    setParameters(prefs[0], prefs[1], prefs[2], currRecPath);

                    prepare();
                    break;
                default:
                    Log.w("SN ###", "Service: it shouldn't arrive here");
                    break;
            }
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        // Tell the user we stopped.
        release();
        Log.d("SN ###", "Service destroyed");
        super.onDestroy();
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

    // Per cambiare le impostazioni della registrazione
    public void setRecPath(String id) {
        currRecPath = mainPath + "/" + id + "-temp.m4a/"; // -temp lo rinomino a fine registrazione nel tempo della registrazione. Per
                                                              // ora mi va bene così
        Log.d("SN ###", "RecMan currNoteFolder: " + currRecPath);
    }

    // Per cambiare le impostazioni della registrazione in futuro
	public void setParameters(int AudioSource, int OutputSource, int AudioEncoder, String path) throws IllegalStateException {
        if (state == MRState.RECORDING) {
            Log.w("SN ###", "!!! setParameters chiamata mentre stai registrando !!!");
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
        try {
            mr.start();
            startTime = System.currentTimeMillis();
            currNote = StorageManager.currName;
            state = MRState.RECORDING;

            createRecOnText();

            createNotification();
        } catch (IllegalStateException e) {
            throw new IllegalStateException("MediaRecorder is in an incorrect state (" + state + "), should be MRState.PREPARED");
        } catch (RuntimeException e) {
            Toast.makeText(ctx.get(), R.string.action_rec_startfail, Toast.LENGTH_LONG).show();
            mr.reset();
            state = MRState.INITIAL;
            setParameters(prefs[0], prefs[0], prefs[1], currRecPath);
            prepare();
        }
    }

    public void stop() {
        try {
            // Fermo la registrazione e salvo la nota
            mr.stop();
            // Aggiorno il momento della fine della registrazione
            endTime = System.currentTimeMillis();
            // salvo la nota correttamente
            save();

            Log.d("SN ###", "RecMan stop OK");
        } catch (IllegalStateException e) {
            throw new IllegalStateException("MediaRecorder is in an incorrect state (" + state + "), it should be MRState.RECORDING (rarely INITIAL)");
        } catch (RuntimeException e) {
            File file = new File(currRecPath);
            boolean deleted = file.delete();
            if (!deleted) {
                Log.d("SN ###", "RecMan stop: stop exception. File not deleted");
            } else {
                Log.d("SN ###", "RecMan stop: stop exception. File deleted");
            }
            mr.reset();
        }

        // rimuovo la notifica
        removeNotification();

        // resetto il mediarecorder
        currNote = "I'm not recording";
        state = MRState.INITIAL;
        setParameters(prefs[0], prefs[1], prefs[2], currRecPath);
        prepare();
    }

    /** Metodo per portare il recorder manager in stato di "release": tutto deallocato */
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
        Log.i("SN ###", "RecMan release ok");
    }

    private boolean save() {
        // TODO: implementare save?
        return false;
    }

    public void createRecOnText() {


    }

    public void SetRecLenghtOnText() {
        //TODO: implementare
    }


    public void createNotification() {
        // AZIONI
        Intent stopIntent = new Intent(this, RecorderManager.class);
        stopIntent.setAction(RecorderManager.ACTION_STOP);
        PendingIntent pendingStopIntent = PendingIntent.getService(this, 0, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder nBuilder =
                new NotificationCompat.Builder(ctx.get())
                        .setSmallIcon(R.drawable.ic_stat_notify)
                        .setColor(getResources().getColor(R.color.primary))
//                        .setContentTitle(getString(R.string.notif_title))
//                        .setContentText(getString(R.string.notif_text))
                        .setContentTitle(getString(R.string.notif_title))
                        .setContentText(StorageManager.currName)
                        .setOngoing(true)
                        .addAction(R.drawable.ic_action_stop, getString(R.string.notif_stop), pendingStopIntent);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            nBuilder.setCategory(Notification.CATEGORY_SERVICE)
                    .setVisibility(Notification.VISIBILITY_PRIVATE);
        }

        Intent intent = new Intent(ctx.get(), NoteListActivity.class);
        intent.setAction(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        PendingIntent pendingIntent = PendingIntent.getActivity(ctx.get(), 0, intent, 0);

        nBuilder.setContentIntent(pendingIntent);
        NotificationManager nManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // INVIO la notifica
        nManager.notify(notifID, nBuilder.build());
    }

    public void removeNotification() {
        isMainActivityRunning(getApplicationContext().getPackageName());

        NotificationManager nManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        nManager.cancel(notifID);
    }

    public boolean isMainActivityRunning(String packageName) {
        ActivityManager manager = (ActivityManager) getSystemService (Context.ACTIVITY_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            List<ActivityManager.AppTask> tasksList = manager.getAppTasks();
            try {
                ActivityManager.RecentTaskInfo taskInfo = tasksList.get(0).getTaskInfo();
                CharSequence d = taskInfo.description;
                int id = taskInfo.id;
                String st = taskInfo.toString();
                Log.d("SN ###", "desc: " + d + " - id: " + id + " - st: " + st);
//                ComponentName activity = taskInfo.origActivity;
//                Log.d("SN ###", "activity orig name: " + activity.getPackageName());
//                if (activity.getPackageName().equals(NoteListActivity.PACKAGE_NAME))
//                    return true;
                return false;
            } catch (IndexOutOfBoundsException e) {
                return false;
            }
        }
//        else {
//            @SuppressWarnings
//                    ("deprecation") List< ActivityManager.RunningTaskInfo > runningTaskInfo = manager.getRunningTasks(1);
//            ComponentName topActivity = runningTaskInfo.get(0).topActivity;
//            ComponentName baseActivity = runningTaskInfo.get(0).baseActivity;
//            Log.d("SN ###", "activity base: '" + baseActivity.getPackageName() + "' top: '" + topActivity.getPackageName() + "'");
//            if (baseActivity.getPackageName().equals(NoteListActivity.PACKAGE_NAME))
//                return true;
//        }

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
