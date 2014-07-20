package com.beamster.audio;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Arrays;
import java.util.Calendar;

import me.channel16.lmr.R;

import org.jivesoftware.smack.XMPPException;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.InflateException;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.beamster.AppConfig;
import com.beamster.AppConfig.TrackerName;
import com.beamster.BeamsterMessage;
import com.beamster.ChatActivity;
import com.beamster.LoginActivity;
import com.beamster.android_api.BeamsterAPI;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

public class AudioVisualizationDialogFragment extends DialogFragment {

	static {
		System.loadLibrary("mp3lame");
	}

	private static View view;
	private Audio audio;
	private Scope scope; 
	private String launchedFrom = ""; // either list, map, cam
			
	// this is the jod the message is supposed to be to.
	// if null, send to group
	private String jid = null; 

	public AudioVisualizationDialogFragment() {
	}	
	
	public AudioVisualizationDialogFragment(String jid, String launchedFrom) {
		this.jid = jid;
		this.launchedFrom = launchedFrom;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		
		if (view != null) 
		{
			ViewGroup parent = (ViewGroup) view.getParent();
			if (parent != null)
				parent.removeView(view);
		}

		try 
		{
			view = inflater.inflate(R.layout.audio_visualization_dialog, container, false);
			scope = (Scope) view.findViewById(R.id.scope);

			// Create audio

			audio = new Audio(jid, launchedFrom);

			// Connect views to audio
			if (scope != null)
				scope.audio = audio;

		} 
		catch (InflateException e) 
		{
			try
			{
				((AppConfig)getActivity().getApplication()).trackException(30, e);			
			}
			catch (Exception e2)
			{
    	        Log.e("BEAMSTER", "Failed to report exception 30"+e2);	    			            		    				
			}

			/* map is already there, just return view as it is */
		}
		return view;
	}

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		Dialog dialog = super.onCreateDialog(savedInstanceState);

		// request a window without the title
		dialog.getWindow().setTitle(getString(R.string.pushtotalk));
		return dialog;
	}

	// Show alert

	@Override
	public void onDestroy() {
		super.onDestroy();
		audio = null;
	}

	@Override
	public void onPause() {
		super.onPause();
		audio.stop();
	}

	@Override
	public void onResume() {
		super.onResume();
		audio.start();
	}

	void showAlert(int appName, int errorBuffer) {
		// Create an alert dialog builder

		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

		// Set the title, message and button

		builder.setTitle(appName);
		builder.setMessage(errorBuffer);
		builder.setNeutralButton(android.R.string.ok,
				new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
			}
		});

		// Create the dialog
		AlertDialog dialog = builder.create();

		// Show it
		dialog.show();
	}



	// Audio

	protected class Audio implements Runnable {

		/**
		 * Used for sending text messages back to user interface
		 */
		private Handler mHandler;

		public static final int MSG_REC_STARTED = 0;
		public static final int MSG_REC_STOPPED = 1;
		public static final int MSG_OPEN_CONNECTION = 2;
		public static final int MSG_ERROR_GET_MIN_BUFFERSIZE = 3;
		public static final int MSG_ERROR_REC_START = 4;
		public static final int MSG_ERROR_AUDIO_RECORD = 5;
		public static final int MSG_ERROR_AUDIO_ENCODE = 6;
		public static final int MSG_ERROR_OPEN_CONNECTION = 7;
		public static final int MSG_ERROR_CLOSE_CONNECTION = 8;

		// sending

		private boolean mIsRecording = false;

		private Socket mSocketToServer;
		private BufferedOutputStream mStreamToServer = null;

		// mp3 encoding
		byte[] mp3buffer = null;

		// ////////////////////////////////////////////////////////////////////////////

		// Preferences

		protected int input;

		protected boolean lock;
		protected boolean zoom;
		protected boolean filter;
		protected boolean screen;
		protected boolean strobe;
		protected boolean multiple;
		protected boolean downsample;

		protected double reference;
		protected double mSampleRate; // sample;
		protected int mMinBufferSize = 0;

		// Data

		protected Thread thread;
		protected double bufferVisual[];
		protected short bufferMic[];
		//protected short data[];

		// Output data

		protected double lower;
		protected double higher;
		protected double nearest;
		protected double frequency;
		protected double difference;
		protected double cents;
		protected double fps;

		protected int count;
		protected int note;

		// Private data

		private long timer;
		private int divisor = 1;

		private AudioRecord audioRecord;

		private static final int MAXIMA = 8;
		private static final int OVERSAMPLE = 16;
		private static final int SAMPLES = 11025; //16384;
		private static final int RANGE = SAMPLES * 3 / 8;
		private static final int STEP = SAMPLES / OVERSAMPLE;
		private static final int SIZE = 4096;

		private static final int C5_OFFSET = 57;
		private static final long TIMER_COUNT = 24;
		private static final double MIN = 0.5;

		private static final double G = 3.023332184e+01;
		private static final double K = 0.9338478249;

		private double xv[];
		private double yv[];

		private Complex x;

		protected float signal;

		protected Maxima maxima;

		protected double xa[];

		private double xp[];
		private double xf[];
		private double dx[];

		private double x2[];
		private double x3[];
		private double x4[];
		private double x5[];
		
		private String jid = null; // this is the jid the audio message is sent to
		private String sentFrom = "";
		
		// Constructor

		protected Audio(String jid, String sentFrom) {

			this.sentFrom = sentFrom;
			bufferVisual = new double[SAMPLES]; // new double[SAMPLES];
			//data = new short[STEP];

			// /////////////////////////////////////////////////////////////////

			// PCM buffer size (5sec)
			// short[] bufferMic = new short[mSampleRate * (16 / 8) * 1 * 5]; 
			//byte[] mp3buffer = new byte[(int) (7200 + bufferMic.length * 2 * 1.25)];

			// /////////////////////////////////////////////////////////////////

			xv = new double[2];
			yv = new double[2];

			x = new Complex(SAMPLES);

			maxima = new Maxima(MAXIMA);

			xa = new double[RANGE];
			xp = new double[RANGE];
			xf = new double[RANGE];
			dx = new double[RANGE];

			x2 = new double[RANGE / 2];
			x3 = new double[RANGE / 3];
			x4 = new double[RANGE / 4];
			x5 = new double[RANGE / 5];
			
			this.jid = jid;
		}

		/**
		 * @param handler
		 * Only used for sending text feedback messages to user
		 */
		public void setHandle(Handler handler) {
			this.mHandler = handler;
		}

		/**
		 * @param OutputStream
		 * Send user info and time as part of filename for the recording
		 */
		private String sendSocketMsgFileInfo(OutputStream mStreamToServer) {

			String user = BeamsterAPI.getInstance().getUsername();
			String timeStamp = String.valueOf(System.currentTimeMillis());
			String fileName = user + "_" + timeStamp + ".mp3";
			String msg = "FILE_NAME:" + fileName;

			sendSocketMsg(mStreamToServer, msg);
			return BeamsterAPI.getInstance().getAudioPath() + fileName;
		}

		/**
		 * @param OutputStream
		 * @param msg
		 * Send a string message from client to server/stream
		 */
		private void sendSocketMsg(OutputStream mStreamToServer, String msg) {

			PrintWriter out = new PrintWriter(
					new BufferedWriter(
							new OutputStreamWriter(mStreamToServer)
							)
					);
			out.println(msg);
			out.flush();

			Log.i("BEAMSTER", "Client socket msg sent: " + msg);	
		}

		// Start audio

		protected void start() {
			// Start the thread

			thread = new Thread(this, "Audio");
			thread.start();
		}

		// Run

		@Override
		public void run() {
			processAudio();
		}

		// Stop

		protected void stop() {
			Thread t = thread;
			thread = null;

			// Wait for the thread to exit
			while (t != null && t.isAlive())
				Thread.yield();
		}

		// Process Audio

		protected void processAudio() {

			Log.d("BEAMSTER", "Begin: processAudio");    
			
			// /////////////////////////////////////////////////////////////////

			// Sample rates to try
			Resources resources = getResources();
			int rates[] = resources.getIntArray(R.array.sample_rates);

			for (int rate : rates) {

				mSampleRate = rate;
				mMinBufferSize = AudioRecord.getMinBufferSize((int) mSampleRate,
						AudioFormat.CHANNEL_IN_MONO,
						AudioFormat.ENCODING_PCM_16BIT);

				if (mMinBufferSize > 0)
					break;

				if (mMinBufferSize == AudioRecord.ERROR_BAD_VALUE)
					continue;

				if (mMinBufferSize == AudioRecord.ERROR) {
					getActivity().runOnUiThread(new Runnable() {

						@Override
						public void run() {
							showAlert(R.string.app_name, R.string.error_buffer);
						}
					});

					thread = null;
					return;
				}
			}

			Log.d("BEAMSTER", "sample rate: " + mSampleRate);  
			Log.d("BEAMSTER", "min mic audio buffer size: " + mMinBufferSize);  

			// Set divisor according to sample rate

			// If you change the sample rates, make sure that this code
			// still works correctly, as both arrays get sorted as there
			// is no array.getIndexOf()

			Arrays.sort(rates);
			int index = Arrays.binarySearch(rates, (int) mSampleRate);
			int divisors[] = resources.getIntArray(R.array.divisors);
			Arrays.sort(divisors);
			divisor = divisors[index];

			// Calculate fps

			fps = (mSampleRate / divisor) / SAMPLES;
			final double expect = 2.0 * Math.PI * STEP / SAMPLES;

			// /////////////////////////////////////////////////////////////////
			// setup server connection

			Log.d("BEAMSTER", "Begin socket connection.");  

			try {
				
				mSocketToServer = new Socket(BeamsterAPI.getInstance().getServer(), 
						BeamsterAPI.getInstance().getServerUploadPort());
				
				mStreamToServer = new BufferedOutputStream(mSocketToServer.getOutputStream());

			} catch (Exception e) {
				try
				{				
					((AppConfig)getActivity().getApplication()).trackException(31, e);			
    			}
    			catch (Exception e2)
    			{
	    	        Log.e("BEAMSTER", "Failed to report exception 31"+e2);	    			            		    				
    			}

				if (mHandler != null) {
					mHandler.sendEmptyMessage(MSG_ERROR_OPEN_CONNECTION);
				}
				Log.e("BEAMSTER", "Error socket connection: " + e);  
				
				getActivity().runOnUiThread(new Runnable() {

			        public void run() {
						Toast.makeText(getActivity(), "Error Recording Audio - Could not connect to Server!", Toast.LENGTH_LONG).show();
			        }
			    });				
				return;
			}

			if (mSocketToServer.isConnected() == false) {
				if (mHandler != null) {
					mHandler.sendEmptyMessage(MSG_ERROR_OPEN_CONNECTION);
				}	
				Log.e("BEAMSTER", "Socket connection could not be opened!");  
				return;
			}

			// /////////////////////////////////////////////////////////////////

			Log.d("BEAMSTER", "Create AudioRecord object."); 

			//			audioRecord = new AudioRecord(input, (int) mSampleRate,
			//					AudioFormat.CHANNEL_IN_MONO,
			//					AudioFormat.ENCODING_PCM_16BIT, SIZE * divisor);

			audioRecord = new AudioRecord(input, (int) mSampleRate,
					AudioFormat.CHANNEL_IN_MONO,
					AudioFormat.ENCODING_PCM_16BIT, mMinBufferSize * 2);

			// Check state

			int state = audioRecord.getState();

			if (state != AudioRecord.STATE_INITIALIZED) {
				getActivity().runOnUiThread(new Runnable() {
					@Override
					public void run() {
						showAlert(R.string.app_name, R.string.error_init);
					}
				});

				audioRecord.release();
				thread = null;
				return;
			}

			// /////////////////////////////////////////////////////////////////

			// Lame init
			SimpleLame.init((int) mSampleRate, 1, (int) mSampleRate, 32);

			Log.d("BEAMSTER", "Lame (mp3) library initialized."); 

			String filename = sendSocketMsgFileInfo(mStreamToServer);

			// /////////////////////////////////////////////////////////////////

			// PCM buffer size (5sec)
			bufferMic = new short[(int)mSampleRate * (16 / 8) * 1 * 5]; 
			mp3buffer = new byte[(int) (7200 + bufferMic.length * 2 * 1.25)]; 
			
			// Start recording

			Log.d("BEAMSTER", "Start audio recording."); 
			
			audioRecord.startRecording();

			Log.d("BEAMSTER", "Audio recording started."); 
			
			long startTime = System.currentTimeMillis(); 
			Log.i("BEAMSTER","startTime: " + startTime);
			boolean sentSpeakingMessage = false;
			
			// Max data
			double dmax = 0.0;

			// Continue until the thread is stopped
			int readSize = 0;

			while (thread != null) {
				// Read a buffer of data
				if (!sentSpeakingMessage && System.currentTimeMillis()-startTime>200)
				{
					try {
						send("", true, BeamsterAPI.COMPOSING_TYPE_SPEAKING);
					} catch (XMPPException e) {
						try
						{
							((AppConfig)getActivity().getApplication()).trackException(32, e);			
		    			}
		    			catch (Exception e2)
		    			{
			    	        Log.e("BEAMSTER", "Failed to report exception 32"+e2);	    			            		    				
		    			}

						Log.e("BEAMSTER", "Composing Message could not be sent");
					} 
					catch (Exception e)
		        	{
						try
						{
							((AppConfig)getActivity().getApplication()).trackException(33, e);			
		    			}
		    			catch (Exception e2)
		    			{
			    	        Log.e("BEAMSTER", "Failed to report exception 33"+e2);	    			            		    				
		    			}

						Toast.makeText(getActivity(), "Connection lost ... logging out ... sorry.", 2000).show();
						// if the above crashed ... do this
						Intent intent = new Intent(getActivity(), LoginActivity.class);
						intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP); 
						intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
						startActivity(intent);	    				
		        	}			
					sentSpeakingMessage = true;
				}
				
				
				Log.d("BEAMSTER", "Audio recording: try audioRecord.read"); 

				readSize = audioRecord.read(bufferMic, 0, mMinBufferSize);

				Log.d("BEAMSTER", "Audio recording: audioRecord.read"); 

				//readSize = audioRecord.read(bufferMic, 0, STEP * divisor);

				// Stop the thread if no data
				if (readSize == 0) {
					Log.d("BEAMSTER", "Audio recording: no data received. Stop."); 
					thread = null;
					break;
				}

				// /////////////////////////////////////////////////////////////////

				Log.d("BEAMSTER", "Audio recording: try mp3 encode."); 

				// encode mic data to mp3
				int encResult = SimpleLame.encode( bufferMic, bufferMic, readSize, mp3buffer);

				Log.d("BEAMSTER", "Audio recording: mp3 encoding done."); 

				if (encResult < 0) {

					Log.e("BEAMSTER", "Audio recording encode error.");

					if (mHandler != null) {
						mHandler.sendEmptyMessage(MSG_ERROR_AUDIO_ENCODE);
					}
					break;
				}

				if (encResult != 0) {
					try {

						Log.d("BEAMSTER", "Audio recording: write stream data to server.");

						mStreamToServer.write(mp3buffer, 0, encResult);	

						Log.d("BEAMSTER", "Audio recording: data written to server.");

					} catch (Exception e) {
						try
						{
							((AppConfig)getActivity().getApplication()).trackException(34, e);			
		    			}
		    			catch (Exception e2)
		    			{
    		    	        Log.e("BEAMSTER", "Failed to report exception 34"+e2);	    			            		    				
		    			}

						Log.e("BEAMSTER", "Audio recording send error.");

						if (mHandler != null) {
							mHandler.sendEmptyMessage(MSG_ERROR_OPEN_CONNECTION);
						}
						break;
					}
				}

				// /////////////////////////////////////////////////////////////////

				try {

					// If display not locked update scope

					if (scope != null && !lock)
						scope.postInvalidate();

					// Move the main data buffer up

					System.arraycopy(bufferVisual, STEP, bufferVisual, 0, SAMPLES - STEP);

					// Max signal

					double rm = 0;

					// Butterworth filter, 3dB/octave

					for (int i = 0; i < STEP; i++) {
						xv[0] = xv[1];
						xv[1] = bufferMic[i * divisor] / G;

						yv[0] = yv[1];
						yv[1] = (xv[0] + xv[1]) + (K * yv[0]);

						// Choose filtered/unfiltered data

						bufferVisual[(SAMPLES - STEP) + i] = (short) (audio.filter ? yv[1]
								: bufferMic[i * divisor]);

						// Find root mean signal

						double v = bufferMic[i * divisor] / 32768.0;
						rm += v * v;
					}

					// Signal value

					rm /= STEP;
					signal = (float) Math.sqrt(rm);

					// Maximum value

					if (dmax < 4096.0)
						dmax = 4096.0;

					// Calculate normalising value

					double norm = dmax;

					dmax = 0.0;

					// Copy data to FFT input arrays for tuner

					for (int i = 0; i < SAMPLES; i++) {
						// Find the magnitude

						if (dmax < Math.abs(bufferVisual[i]))
							dmax = Math.abs(bufferVisual[i]);

						// Calculate the window

						double window = 0.5 - 0.5 * Math.cos(2.0 * Math.PI * i
								/ SAMPLES);

						// Normalise and window the input data

						x.r[i] = bufferVisual[i] / norm * window;
					}

					// do FFT for tuner

					fftr(x);

					// Process FFT output for tuner

					for (int i = 1; i < RANGE; i++) {
						double real = x.r[i];
						double imag = x.i[i];

						xa[i] = Math.hypot(real, imag);

						// Do frequency calculation

						double p = Math.atan2(imag, real);
						double dp = xp[i] - p;

						xp[i] = p;

						// Calculate phase difference

						dp -= i * expect;

						int qpd = (int) (dp / Math.PI);

						if (qpd >= 0)
							qpd += qpd & 1;

						else
							qpd -= qpd & 1;

						dp -= Math.PI * qpd;

						// Calculate frequency difference

						double df = OVERSAMPLE * dp / (2.0 * Math.PI);

						// Calculate actual frequency from slot frequency plus
						// frequency difference and correction value

						xf[i] = i * fps + df * fps;

						// Calculate differences for finding maxima

						dx[i] = xa[i] - xa[i - 1];
					}

					// Downsample

					if (downsample) {
						// x2 = xa << 2

						for (int i = 0; i < RANGE / 2; i++) {
							x2[i] = 0.0;

							for (int j = 0; j < 2; j++)
								x2[i] += xa[(i * 2) + j] / 2.0;
						}

						// x3 = xa << 3

						for (int i = 0; i < RANGE / 3; i++) {
							x3[i] = 0.0;

							for (int j = 0; j < 3; j++)
								x3[i] += xa[(i * 3) + j] / 3.0;
						}

						// x4 = xa << 4

						for (int i = 0; i < RANGE / 4; i++) {
							x4[i] = 0.0;

							for (int j = 0; j < 4; j++)
								x2[i] += xa[(i * 4) + j] / 4.0;
						}

						// x5 = xa << 5

						for (int i = 0; i < RANGE / 5; i++) {
							x5[i] = 0.0;

							for (int j = 0; j < 5; j++)
								x5[i] += xa[(i * 5) + j] / 5.0;
						}

						// Add downsamples

						for (int i = 1; i < RANGE; i++) {
							if (i < RANGE / 2)
								xa[i] += x2[i];

							if (i < RANGE / 3)
								xa[i] += x3[i];

							if (i < RANGE / 4)
								xa[i] += x4[i];

							if (i < RANGE / 5)
								xa[i] += x5[i];

							// Recalculate differences

							dx[i] = xa[i] - xa[i - 1];
						}
					}

					// Maximum FFT output

					double max = 0.0;

					count = 0;
					int limit = RANGE - 1;

					// Find maximum value, and list of maxima

					for (int i = 1; i < limit; i++) {
						if (xa[i] > max) {
							max = xa[i];
							frequency = xf[i];
						}

						// If display not locked, find maxima and add to list

						if (!lock && count < MAXIMA && xa[i] > MIN
								&& xa[i] > (max / 4.0) && dx[i] > 0.0
								&& dx[i + 1] < 0.0) {
							maxima.f[count] = xf[i];

							// Cents relative to reference

							double cf = -12.0 * log2(reference / xf[i]);

							// Reference note

							maxima.r[count] = reference
									* Math.pow(2.0, Math.round(cf) / 12.0);

							// Note number

							maxima.n[count] = (int) (Math.round(cf) + C5_OFFSET);

							// Don't use if negative

							if (maxima.n[count] < 0) {
								maxima.n[count] = 0;
								continue;
							}

							// Set limit to octave above

							if (!downsample && (limit > i * 2))
								limit = i * 2 - 1;

							count++;
						}
					}

					// Found flag

					boolean found = false;

					// Do the note and cents calculations

					if (max > MIN) {
						found = true;

						// Frequency

						if (!downsample)
							frequency = maxima.f[0];

						// Cents relative to reference

						double cf = -12.0 * log2(reference / frequency);

						// Don't count silly values

						if (Double.isNaN(cf))
							continue;

						// Reference note

						nearest = audio.reference
								* Math.pow(2.0, Math.round(cf) / 12.0);

						// Lower and upper freq

						lower = reference
								* Math.pow(2.0, (Math.round(cf) - 0.55) / 12.0);
						higher = reference
								* Math.pow(2.0, (Math.round(cf) + 0.55) / 12.0);

						// Note number

						note = (int) Math.round(cf) + C5_OFFSET;

						if (note < 0) {
							note = 0;
							found = false;
						}
						// Find nearest maximum to reference note

						double df = 1000.0;

						for (int i = 0; i < count; i++) {
							if (Math.abs(maxima.f[i] - nearest) < df) {
								df = Math.abs(maxima.f[i] - nearest);
								frequency = maxima.f[i];
							}
						}

						// Cents relative to reference note

						cents = -12.0 * log2(nearest / frequency) * 100.0;

						// Ignore silly values

						if (Double.isNaN(cents)) {
							cents = 0.0;
							found = false;
						}

						// Ignore if not within 50 cents of reference note

						if (Math.abs(cents) > 50.0) {
							cents = 0.0;
							found = false;
						}

						// Difference

						difference = frequency - nearest;
					}

					// Found

					if (found) {

						// Reset count;

						timer = 0;
					}

					else {
						// If display not locked

						if (!lock) {
							if (timer > TIMER_COUNT) {
								difference = 0.0;
								frequency = 0.0;
								nearest = 0.0;
								higher = 0.0;
								lower = 0.0;
								cents = 0.0;
								count = 0;
								note = 0;

							}
						}
					}

				} catch (Exception e) {
					try
					{
						((AppConfig)getActivity().getApplication()).trackException(35, e);
	    			}
	    			catch (Exception e2)
	    			{
		    	        Log.e("BEAMSTER", "Failed to report exception 35"+e2);	    			            		    				
	    			}
					Log.i("BEAMSTER","Audio recording: exception running visualization! " + e);					
				}

				timer++;
			}

			// Stop and release the audio recorder

			if (audioRecord != null) {
				audioRecord.stop();
				audioRecord.release();
			}

			// Recording loop finished
			// ---------------------------------------------------------------

			boolean recordingCancelled = false;
			
			int flushResult = SimpleLame.flush(mp3buffer);
			
			if (flushResult < 0) {
				if (mHandler != null) {
					mHandler.sendEmptyMessage(MSG_ERROR_AUDIO_ENCODE);
				}
			} else if (flushResult != 0) {
				try {
					mStreamToServer.write(mp3buffer, 0, flushResult);
					
					long estimatedTime = System.currentTimeMillis() - startTime;
					
					Log.i("BEAMSTER","currentTimeMillis: " + System.currentTimeMillis() );
					Log.i("BEAMSTER","estimatedTime: " + estimatedTime);
					
					if (estimatedTime < 200) { // 0.2 sec
					
						recordingCancelled = true;
						sendSocketMsg(mStreamToServer, "RECORDING_CANCEL");
						Log.i("BEAMSTER","Audio recording session cancelled.");
												
					} else {	
					
						// = 0 - ok
						// send message now!
						Log.i("BEAMSTER","Send message to "+filename+" flushResult: "+flushResult);
						
						// create new messaging object
						BeamsterMessage myBeamsterMessage = new BeamsterMessage(filename, true);	    
	            		myBeamsterMessage.setBeamed(((ChatActivity)getActivity()).isBeamed());
	            		myBeamsterMessage.setClientId(BeamsterAPI.getInstance().getClientId());
	            		myBeamsterMessage.setDistanceAway(0);
	            		myBeamsterMessage.setLat(""+((ChatActivity)getActivity()).getCurrentCenter().getLatitude());
	            		myBeamsterMessage.setLon(""+((ChatActivity)getActivity()).getCurrentCenter().getLongitude());
	            		myBeamsterMessage.setMessagedate(Calendar.getInstance());
	            		myBeamsterMessage.setName(((ChatActivity)getActivity()).getMyBeamsterUserProfile().getName());
	            		String pictureUrl = ((ChatActivity)getActivity()).getMyBeamsterUserProfile().getPictureUrl();
	            		if (pictureUrl!=null && !pictureUrl.equals(""))            			
	            			myBeamsterMessage.setPictureurl(pictureUrl);
	            		myBeamsterMessage.setSpeed("");
	            		myBeamsterMessage.setUsername(BeamsterAPI.getInstance().getUsername());					
	            		myBeamsterMessage.setStillNeedsToBePlayed(false);
	            		
	            		// get the name of the current tab
	            		if (jid==null)            		
	            		{
		            		// add it to list of messages
			    			((ChatActivity)getActivity()).getMessages().add(myBeamsterMessage);										

			    			// update list or map						
							((ChatActivity)getActivity()).updateFragments(sentFrom);
	            		}
	            		else
	            		{
		            		// add it to list of messages
			    			((ChatActivity)getActivity()).getMessages(jid).add(myBeamsterMessage);					

			    			// update list or map						
							((ChatActivity)getActivity()).updateFragments(jid);
	            		}
	        		            		            		
	    				//BeamsterAPI.getInstance().sendMessage("chat@beamster.", ((ChatActivity)getActivity()).getMyBeamsterUserProfile().getName(), ((ChatActivity)getActivity()).getMyBeamsterUserProfile().getPictureUrl(), "", filename, ((ChatActivity)getActivity()).getCurrentCenter().latitude, ((ChatActivity)getActivity()).getCurrentCenter().longitude, false, "");
						send(filename, false, "");
												

					} // end real message
					
  			
				} catch (IOException e) {
					try
					{
						((AppConfig)getActivity().getApplication()).trackException(36, e);
	    			}
	    			catch (Exception e2)
	    			{
		    	        Log.e("BEAMSTER", "Failed to report exception 36"+e2);	    			            		    				
	    			}

					Log.e("BEAMSTER","IOException when finalizing message: "+e.getMessage());
				} catch (XMPPException e) {
					try
					{
						((AppConfig)getActivity().getApplication()).trackException(37, e);			
	    			}
	    			catch (Exception e2)
	    			{
		    	        Log.e("BEAMSTER", "Failed to report exception 37"+e2);	    			            		    				
	    			}

					Log.e("BEAMSTER","XMPPException when finalizing message: "+e.getMessage());
				}
			}

			FinalizeRecording();

			if (!recordingCancelled) {
				sendSocketMsg(mStreamToServer, "RECORDING_END");
				Log.i("BEAMSTER","Audio recording session ended.");				
			}
			
			
		}

	    /**
	     * Sending a message
	     */
	    private void send(String newMessage, boolean composing, String composingText) throws XMPPException
	    {
			if (jid==null || jid.equals(""))
			{
				// send a group chat message
				BeamsterAPI.getInstance().sendMessage("chat@beamster.", ((ChatActivity)getActivity()).getMyBeamsterUserProfile().getName(), ((ChatActivity)getActivity()).getMyBeamsterUserProfile().getPictureUrl(), "", newMessage, ((ChatActivity)getActivity()).getCurrentCenter().getLatitude(), ((ChatActivity)getActivity()).getCurrentCenter().getLongitude(), composing, composingText);
				
				if (this.sentFrom!=null && this.sentFrom.equals("map") && composing==false)
				{
					// Get tracker.
			        Tracker t = ((AppConfig)getActivity().getApplication()).getTracker(
			            TrackerName.APP_TRACKER);
			        // Build and send an Event.
			        t.send(new HitBuilders.EventBuilder()
			            .setCategory("Message")
			            .setAction("AudioMessageSentMap") // actionId
			            .setLabel("chat") 
			            .build());										
				}
				else if (this.sentFrom!=null && this.sentFrom.equals("list") && composing==false)
				{
					// Get tracker.
			        Tracker t = ((AppConfig)getActivity().getApplication()).getTracker(
			            TrackerName.APP_TRACKER);
			        // Build and send an Event.
			        t.send(new HitBuilders.EventBuilder()
			            .setCategory("Message")
			            .setAction("AudioMessageSentList") // actionId
			            .setLabel("chat") 
			            .build());										
				}
				else if (this.sentFrom!=null && this.sentFrom.equals("cam") && composing==false)
				{
					// Get tracker.
			        Tracker t = ((AppConfig)getActivity().getApplication()).getTracker(
			            TrackerName.APP_TRACKER);
			        // Build and send an Event.
			        t.send(new HitBuilders.EventBuilder()
			            .setCategory("Message")
			            .setAction("AudioMessageSentCam") // actionId
			            .setLabel("chat") 
			            .build());										
				}
					
			}
			else
			{
				// these are the private channel messages
				// send a group chat message
				BeamsterAPI.getInstance().sendMessage(jid+"@", ((ChatActivity)getActivity()).getMyBeamsterUserProfile().getName(), ((ChatActivity)getActivity()).getMyBeamsterUserProfile().getPictureUrl(), "", newMessage, ((ChatActivity)getActivity()).getCurrentCenter().getLatitude(), ((ChatActivity)getActivity()).getCurrentCenter().getLongitude(), composing, composingText);

				if (this.sentFrom!=null && this.sentFrom.equals("map") && composing==false)
				{
					// Get tracker.
			        Tracker t = ((AppConfig)getActivity().getApplication()).getTracker(
			            TrackerName.APP_TRACKER);
			        // Build and send an Event.
			        t.send(new HitBuilders.EventBuilder()
			            .setCategory("Message")
			            .setAction("AudioMessageSentMap") // actionId
			            .setLabel("private") 
			            .build());										
				}
				else if (this.sentFrom!=null && this.sentFrom.equals("list") && composing==false)
				{
					// Get tracker.
			        Tracker t = ((AppConfig)getActivity().getApplication()).getTracker(
			            TrackerName.APP_TRACKER);
			        // Build and send an Event.
			        t.send(new HitBuilders.EventBuilder()
			            .setCategory("Message")
			            .setAction("AudioMessageSentList") // actionId
			            .setLabel("private") 
			            .build());										
				}
				else if (this.sentFrom!=null && this.sentFrom.equals("cam") && composing==false)
				{
					// Get tracker.
			        Tracker t = ((AppConfig)getActivity().getApplication()).getTracker(
			            TrackerName.APP_TRACKER);
			        // Build and send an Event.
			        t.send(new HitBuilders.EventBuilder()
			            .setCategory("Message")
			            .setAction("AudioMessageSentCam") // actionId
			            .setLabel("private") 
			            .build());										
				}				
			
			}			    	
	    	
	    }
		
		private void FinalizeRecording() {

			mIsRecording = false; 

			try {
				SimpleLame.close();
			} catch (Exception e) {
				try
				{
					((AppConfig)getActivity().getApplication()).trackException(38, e);			
    			}
    			catch (Exception e2)
    			{
	    	        Log.e("BEAMSTER", "Failed to report exception 38"+e2);	    			            		    				
    			}

				Log.e("BEAMSTER","3Exception: " + e);
			}
			
			if (mStreamToServer != null) {
				try {
					mStreamToServer.flush();
				} catch (Exception e) {
					try
					{
						((AppConfig)getActivity().getApplication()).trackException(39, e);			
	    			}
	    			catch (Exception e2)
	    			{
		    	        Log.e("BEAMSTER", "Failed to report exception 39"+e2);	    			            		    				
	    			}

					Log.e("BEAMSTER","4Exception: " + e);
				}		
			}
			if (mStreamToServer != null) {
				try {
					mStreamToServer.close();
				} catch (Exception e) {
					try
					{
						((AppConfig)getActivity().getApplication()).trackException(40, e);			
	    			}
	    			catch (Exception e2)
	    			{
		    	        Log.e("BEAMSTER", "Failed to report exception 40"+e2);	    			            		    				
	    			}

					Log.e("BEAMSTER","5Exception: " + e);
				}						
			}
			if (mSocketToServer != null) {
				try {
					mSocketToServer.close();
				} catch (Exception e) {
					try
					{
						((AppConfig)getActivity().getApplication()).trackException(41, e);			
	    			}
	    			catch (Exception e2)
	    			{
		    	        Log.e("BEAMSTER", "Failed to report exception 41"+e2);	    			            		    				
	    			}

					Log.e("BEAMSTER","6Exception: " + e);
					if (mHandler != null) {
						mHandler.sendEmptyMessage(MSG_ERROR_CLOSE_CONNECTION);
					}
				}
			}
			Log.i("BEAMSTER","Audio recording session finalized.");
		}

		// Real to complex FFT, ignores imaginary values in input array

		private void fftr(Complex a) {
			final int n = a.r.length;
			final double norm = Math.sqrt(1.0 / n);

			for (int i = 0, j = 0; i < n; i++) {
				if (j >= i) {
					double tr = a.r[j] * norm;

					a.r[j] = a.r[i] * norm;
					a.i[j] = 0.0;

					a.r[i] = tr;
					a.i[i] = 0.0;
				}

				int m = n / 2;
				while (m >= 1 && j >= m) {
					j -= m;
					m /= 2;
				}
				j += m;
			}

			for (int mmax = 1, istep = 2 * mmax; mmax < n; mmax = istep, istep = 2 * mmax) {
				double delta = (Math.PI / mmax);
				for (int m = 0; m < mmax; m++) {
					double w = m * delta;
					double wr = Math.cos(w);
					double wi = Math.sin(w);

					for (int i = m; i < n; i += istep) {
						int j = i + mmax;
						double tr = wr * a.r[j] - wi * a.i[j];
						double ti = wr * a.i[j] + wi * a.r[j];
						a.r[j] = a.r[i] - tr;
						a.i[j] = a.i[i] - ti;
						a.r[i] += tr;
						a.i[i] += ti;
					}
				}
			}
		}

		// Copy to clipboard

		@SuppressLint("DefaultLocale")
		protected void copyToClipboard() {

			if (multiple) {
				for (int i = 0; i < count; i++) {
					// Calculate cents

					double cents = -12.0 * log2(maxima.r[i] / maxima.f[i])
							* 100.0;
					// Ignore silly values

					if (Double.isNaN(cents))
						continue;
				}
			}
		}
	}

	// Log2

	protected double log2(double d) {
		return Math.log(d) / Math.log(2.0);
	}

	// These two objects replace arrays of structs in the C version
	// because initialising arrays of objects in Java is, IMHO, barmy

	// Complex

	private class Complex {
		double r[];
		double i[];

		private Complex(int l) {
			r = new double[l];
			i = new double[l];
		}
	}

	// Maximum

	protected class Maxima {
		double f[];
		double r[];
		int n[];

		protected Maxima(int l) {
			f = new double[l];
			r = new double[l];
			n = new int[l];
		}
	}	
	
}