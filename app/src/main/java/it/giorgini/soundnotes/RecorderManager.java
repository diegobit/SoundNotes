package it.giorgini.soundnotes;

import java.io.IOException;

import android.annotation.SuppressLint;
import android.media.MediaRecorder;
import android.os.AsyncTask;

public class RecorderManager {
	
	private MediaRecorder mr = new MediaRecorder();
	
	@SuppressLint("InlinedApi")
	public RecorderManager(String path) {
		mr.setAudioSource(MediaRecorder.AudioSource.MIC);
//		if (Build.VERSION.SDK_INT == 15)
		mr.setOutputFormat(MediaRecorder.OutputFormat.DEFAULT);
//		else
//			mr.setOutputFormat(MediaRecorder.OutputFormat.AAC_ADTS);
		mr.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
		mr.setOutputFile(path);
	}
	
	public void setParameters(int AudioSource, int OutputSource, int AudioEncoder, String path) {
		mr.setAudioSource(AudioSource);
		mr.setOutputFormat(OutputSource);
		mr.setAudioEncoder(AudioEncoder);
		mr.setOutputFile(path);
	}
	
	
	private class PrepareBG extends AsyncTask<Integer, Float, Boolean> {
		private boolean error = false;
		
		@Override
		protected Boolean doInBackground(Integer... params) {
			try {
				mr.prepare();
			} catch (IOException e) {
				e.printStackTrace();
				error = true;
			}
			return null;
		}
		
		@Override
		protected void onProgressUpdate(Float... values) {
			super.onProgressUpdate(values);
//			if (error)
				
		}
	}
	
	public static void prepare() {
//		AsyncTask<Integer, Float, Boolean> PBG = new PrepareBG().execute(1, 2);
	}
	
}
