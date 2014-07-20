package com.beamster.audio;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.security.InvalidParameterException;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Handler;
import android.util.Log;

import com.beamster.AppConfig;
import com.beamster.audio.SimpleLame;

public class RecMicToMp3ToTCP {

	static {
		System.loadLibrary("mp3lame");
	}

	private boolean mIsRecording = false;
	private int mSampleRate;
	private AudioRecord mAudioRecord = null;
	
	private String mServerIP = null;
	private int mSocketPort = 0;
	private Socket mSocketToServer;
	private BufferedOutputStream mStreamToServer = null;

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
	
	/**
	 */
	public RecMicToMp3ToTCP() {		
	}
	
	/**
	 * @param serverIP
	 * @param socketPort
	 * @param sampleRate
	 */
	public RecMicToMp3ToTCP(String serverIP, int socketPort, int sampleRate) {		

		this.setup(serverIP, socketPort, sampleRate);
	}

	/**
	 * @param serverIP
	 * @param socketPort
	 * @param sampleRate
	 * @throws InvalidParameterException 
	 */
	public void setup(String serverIP,  int socketPort, int sampleRate)
			throws InvalidParameterException {
		
		if (sampleRate <= 0) {
			throw new InvalidParameterException(
					"Invalid sample rate specified.");
		}
		this.mServerIP = serverIP;
		this.mSocketPort = socketPort;
		this.mSampleRate = sampleRate;
		
		Log.i("BEAMSTER","New audio recording session setup.");
	}

	/**
	 */
	public void start() {

		if (mIsRecording) {
			return;
		}

		new Thread() {

			@Override
			public void run() {

				Log.i("BEAMSTER", "Client audio recording thread started.");	
				
				android.os.Process
				.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);

				try {
					
					try {
						mSocketToServer = new Socket(mServerIP, mSocketPort);
						mStreamToServer = new BufferedOutputStream(mSocketToServer.getOutputStream());
																	
					} catch (Exception e) {
						if (mHandler != null) {
							mHandler.sendEmptyMessage(MSG_ERROR_OPEN_CONNECTION);
						}
						return;
					}
					
					if (mSocketToServer.isConnected() == false) {
						if (mHandler != null) {
							mHandler.sendEmptyMessage(MSG_ERROR_OPEN_CONNECTION);
						}	
						return;
					}
					
					final int minBufferSize = AudioRecord.getMinBufferSize(
							mSampleRate, AudioFormat.CHANNEL_IN_MONO,
							AudioFormat.ENCODING_PCM_16BIT);

					if (minBufferSize < 0) {
						if (mHandler != null) {
							mHandler.sendEmptyMessage(MSG_ERROR_GET_MIN_BUFFERSIZE);
						}
						return;
					}

					mAudioRecord = new AudioRecord(
							MediaRecorder.AudioSource.MIC, mSampleRate,
							AudioFormat.CHANNEL_IN_MONO,
							AudioFormat.ENCODING_PCM_16BIT, minBufferSize * 2);

					// PCM buffer size (5sec)
					short[] buffer = new short[mSampleRate * (16 / 8) * 1 * 5]; 
					byte[] mp3buffer = new byte[(int) (7200 + buffer.length * 2 * 1.25)];

					// Lame init
					SimpleLame.init(mSampleRate, 1, mSampleRate, 32);

					// -------------------------------------
					
					mIsRecording = true; 

					sendSocketMsgFileInfo(mStreamToServer);
					
					// -------------------------------------

					try {
						
						mAudioRecord.startRecording(); 
						Log.i("BEAMSTER","New audio recording session started.");
						
					} catch (IllegalStateException e) {
						
						Log.e("BEAMSTER","Audio recording exception: " + e);	
						
						if (mHandler != null) {
							mHandler.sendEmptyMessage(MSG_ERROR_REC_START);
						}
						return;
					}

					if (mHandler != null) {
						mHandler.sendEmptyMessage(MSG_REC_STARTED);
					}

					// ---------------------------------------------------------------
					// Recording loop started

					int readSize = 0;

					while (mIsRecording) {
						
						readSize = mAudioRecord.read(buffer, 0, minBufferSize);

						if (readSize < 0) {
							if (mHandler != null) {
								mHandler.sendEmptyMessage(MSG_ERROR_AUDIO_RECORD);
							}
							break;
						}
						else if (readSize == 0) {;}
						else {

							// encode mic data to mp3
							int encResult = SimpleLame.encode(buffer, buffer, readSize, mp3buffer);

							if (encResult < 0) {
								
								Log.e("BEAMSTER", "Audio recording encode error.");
								
								if (mHandler != null) {
									mHandler.sendEmptyMessage(MSG_ERROR_AUDIO_ENCODE);
								}
								break;
							}

							if (encResult != 0) {
								try {

									mStreamToServer.write(mp3buffer, 0, encResult);	
																	
								} catch (Exception e) {
									
									Log.e("BEAMSTER", "Audio recording send error.");
									
									if (mHandler != null) {
										mHandler.sendEmptyMessage(MSG_ERROR_OPEN_CONNECTION);
									}
									break;
								}
							}
						}
					}

					// Recording loop finished
					// ---------------------------------------------------------------

					int flushResult = SimpleLame.flush(mp3buffer);
					if (flushResult < 0) {
						if (mHandler != null) {
							mHandler.sendEmptyMessage(MSG_ERROR_AUDIO_ENCODE);
						}
					}

					if (flushResult != 0) {
						mStreamToServer.write(mp3buffer, 0, flushResult);
					}

					sendSocketMsg(mStreamToServer, "RECORDING_END");
					Log.i("BEAMSTER","Audio recording session ended.");
					
					// -------------------------------------

				} catch (Exception e) {

					Log.e("BEAMSTER","Exception: " + e);
					
				} finally {
					FinalizeRecording();
				}

				if (mHandler != null) {
					mHandler.sendEmptyMessage(MSG_REC_STOPPED);
				}
			}
			
		}.start();
	}
	
	private void FinalizeRecording() {
		
		mIsRecording = false; 

		try {
			mAudioRecord.stop(); 
		} catch (Exception e) {
			Log.e("BEAMSTER","Exception: " + e);
		}

		try {
			mAudioRecord.release();
		} catch (Exception e) {
			Log.e("BEAMSTER","Exception: " + e);
		}

		try {
			SimpleLame.close();
		} catch (Exception e) {
			Log.e("BEAMSTER","Exception: " + e);
		}

		if (mStreamToServer != null) {
			try {
				mStreamToServer.flush();
			} catch (Exception e) {
				Log.e("BEAMSTER","Exception: " + e);
			}		
		}
		if (mStreamToServer != null) {
			try {
				mStreamToServer.close();
			} catch (Exception e) {
				Log.e("BEAMSTER","Exception: " + e);
			}						
		}
		if (mSocketToServer != null) {
			try {
				mSocketToServer.close();
			} catch (Exception e) {
				Log.e("BEAMSTER","Exception: " + e);
				if (mHandler != null) {
					mHandler.sendEmptyMessage(MSG_ERROR_CLOSE_CONNECTION);
				}
			}
		}
		Log.i("BEAMSTER","Audio recording session finalized.");
	}
	
	/**
	 */
	public void stop() {
		mIsRecording = false;
	}

	/**
	 * @return true/false
	 */
	public boolean isRecording() {
		return mIsRecording;
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
	private void sendSocketMsgFileInfo(OutputStream mStreamToServer) {
		
		String user = "myUserName";
		String timeStamp = String.valueOf(System.currentTimeMillis());
		String fileName = user + "_" + timeStamp + ".mp3";
		String msg = "FILE_NAME:" + fileName;
		
		sendSocketMsg(mStreamToServer, msg);
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
}
