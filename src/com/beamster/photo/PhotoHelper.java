package com.beamster.photo;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Calendar;

import org.jivesoftware.smack.XMPPException;

import android.app.ActionBar;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import com.beamster.AppConfig;
import com.beamster.AppConfig.TrackerName;
import com.beamster.BeamsterMessage;
import com.beamster.ChatActivity;
import com.beamster.LoginActivity;
import com.beamster.android_api.BeamsterAPI;
import com.beamster.list.FragmentList;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

public class PhotoHelper {

	private String launchedFrom = ""; // either list, map, cam
	private String jid = null;
	private Context context = null;

	public PhotoHelper() {
	}

	public PhotoHelper(String jid, String launchedFrom, Context context) {
		this.jid = jid;
		this.launchedFrom = launchedFrom;
		this.context = context;
	}
	
	public void uploadPhoto(Bitmap myPhoto) {
		AsyncPhotoUpload myClientTask = new AsyncPhotoUpload();		
		myClientTask.execute(myPhoto);
	}	

	private class AsyncPhotoUpload extends AsyncTask<Bitmap, Void, String> {

		private Socket mSocketToServer;
		private BufferedOutputStream mStreamToServer = null;

		/**
		 * @param OutputStream
		 * @param msg
		 *            Send a string message from client to server/stream
		 */
		private void sendSocketMsg(OutputStream mStreamToServer, String msg) {

			PrintWriter out = new PrintWriter(new BufferedWriter(
					new OutputStreamWriter(mStreamToServer)));
			out.println(msg);
			out.flush();

			Log.i("BEAMSTER", "Client socket msg sent: " + msg);
		}		
		
		/**
		 * @param OutputStream
		 *            Send user info and time as part of filename for the
		 *            recording
		 */
		private String sendSocketMsgFileInfo(OutputStream mStreamToServer) {

			String user = BeamsterAPI.getInstance().getUsername();
			String timeStamp = String.valueOf(System.currentTimeMillis());
			String fileName = user + "_" + timeStamp + ".jpg";
			String msg = "FILE_NAME:" + fileName;

			sendSocketMsg(mStreamToServer, msg);
			return BeamsterAPI.getInstance().getAudioPath() + fileName;
		}		
		
		protected String doInBackground(Bitmap ... bitmaps) {
			String filename = null;
			try {
				mSocketToServer = new Socket(BeamsterAPI.getInstance().getServer(),
						BeamsterAPI.getInstance().getServerUploadPort());
				
				mStreamToServer = new BufferedOutputStream(
						mSocketToServer.getOutputStream());
				
				int count = bitmaps.length;
		        long totalSize = 0;
		        for (int i = 0; i < count; i++) 
		        {
		        	Bitmap myBitmap = bitmaps[i];
		            totalSize += myBitmap.getByteCount();

					filename = sendSocketMsgFileInfo(mStreamToServer);

					myBitmap.compress(CompressFormat.JPEG, 85, mStreamToServer);

					sendSocketMsg(mStreamToServer, "RECORDING_END");

					if (mStreamToServer != null) {
							mStreamToServer.flush();
							mStreamToServer.close();
							mSocketToServer.close();
					}
					Log.i("BEAMSTER", "Audio recording session finalized.");
		            
		            
		            publishProgress((int) ((i / (float) count) * 100));
		            
		            // Escape early if cancel() is called
		            if (isCancelled()) break;
		        }
		        return filename;				
				
			} catch (UnknownHostException e) {
				try
				{
					((AppConfig)((ChatActivity)context).getApplication()).trackException(130, e);			
				}
				catch (Exception e2)
				{
	    	        Log.e("BEAMSTER", "Failed to report exception 130"+e2);	    			            		    				
				}

				Log.e("BEAMSTER", "message not sent! "+e);
			} catch (IOException e) {
				try
				{
					((AppConfig)((ChatActivity)context).getApplication()).trackException(141, e);			
				}
				catch (Exception e2)
				{
	    	        Log.e("BEAMSTER", "Failed to report exception 141"+e2);	    			            		    				
				}

				Log.e("BEAMSTER", "message not sent! "+e);
			}

			return null;// returns what you want to pass to the onPostExecute()
		}
		
		protected void publishProgress(int progress) {
			Log.i("BEAMSTER", "Upload progress "+progress);
		}

		protected void onPostExecute(String photoUrl) {
			Log.i("BEAMSTER", "Upload done "+photoUrl);
			if (photoUrl!=null && !photoUrl.equals(""))
			{
				//Log.i("BEAMSTER", "Image uploaded: "+filename);

				// 2) construct message

				// create new messaging object
				BeamsterMessage myBeamsterMessage = new BeamsterMessage(photoUrl, true);	    
				myBeamsterMessage.setBeamed(((ChatActivity)context).isBeamed());
				myBeamsterMessage.setClientId(BeamsterAPI.getInstance().getClientId());
				myBeamsterMessage.setDistanceAway(0);
				myBeamsterMessage.setLat(""+((ChatActivity)context).getCurrentCenter().getLatitude());
				myBeamsterMessage.setLon(""+((ChatActivity)context).getCurrentCenter().getLongitude());
				myBeamsterMessage.setMessagedate(Calendar.getInstance());
				myBeamsterMessage.setName(((ChatActivity)context).getMyBeamsterUserProfile().getName());
				String pictureUrl = ((ChatActivity)context).getMyBeamsterUserProfile().getPictureUrl();
				if (pictureUrl!=null && !pictureUrl.equals(""))            			
					myBeamsterMessage.setPictureurl(pictureUrl);
				myBeamsterMessage.setSpeed("");
				myBeamsterMessage.setUsername(BeamsterAPI.getInstance().getUsername());
				
				// 3) add message to list of messages
				
				// get the name of the current tab
				ActionBar.Tab selectedTab = ((ChatActivity)context).getActionBar().getSelectedTab();
				if (selectedTab.getPosition()<=1)            		
				{
		    		// add it to list of messages
					((ChatActivity)context).getMessages().add(myBeamsterMessage);										
				}
				else
				{
		    		// add it to list of messages
					((ChatActivity)context).getMessages(jid).add(myBeamsterMessage);					
				}

				Log.d("BEAMSTER", "message posted: "+myBeamsterMessage);
				
				
				// 4) send the message
				
				try {
					
					// send a message (false = composing is false)
					send(photoUrl, false, "");

					// add message to list
					FragmentList fragmentList = (FragmentList)((ChatActivity)context).getFragmentManager().findFragmentByTag("list");
					if (fragmentList!=null)
						fragmentList.updateMessagesList(true);
					
				} catch (XMPPException e) {
					try
					{
						((AppConfig)((ChatActivity)context).getApplication()).trackException(50, e);			
					}
					catch (Exception e2)
					{
		    	        Log.e("BEAMSTER", "Failed to report exception 50"+e2);	    			            		    				
					}

					Log.e("BEAMSTER", "message not sent! "+e);
				} catch (Exception e)
		    	{
					try
					{
						((AppConfig)((ChatActivity)context).getApplication()).trackException(51, e);									
					}
					catch (Exception e2)
					{
		    	        Log.e("BEAMSTER", "Failed to report exception 51"+e2);	    			            		    				
					}

					Toast.makeText(context, "Connection lost ... logging out ... sorry.", 2000).show();
					// if the above crashed ... do this
					Intent intent = new Intent(context, LoginActivity.class);
					intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP); 
					intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
					context.startActivity(intent);	    				
		    	}		
				
			}
			
			// dismiss dialog
			((ChatActivity)context).dismissDialog();
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
			BeamsterAPI.getInstance().sendMessage("chat@beamster.", ((ChatActivity)context).getMyBeamsterUserProfile().getName(), ((ChatActivity)context).getMyBeamsterUserProfile().getPictureUrl(), "", newMessage, ((ChatActivity)context).getCurrentCenter().getLatitude(), ((ChatActivity)context).getCurrentCenter().getLongitude(), composing, composingText);

			if (composing==false)
			{
				// Get tracker.
		        Tracker t = ((AppConfig)((ChatActivity)context).getApplication()).getTracker(
		            TrackerName.APP_TRACKER);
		        // Build and send an Event.
		        t.send(new HitBuilders.EventBuilder()
		            .setCategory("Message")
		            .setAction("PhotoMessageSent"+this.launchedFrom) // actionId
		            .setLabel("chat") 
		            .build());								
			}
		}
		else
		{
			// these are the private channel messages
			// send a group chat message
			BeamsterAPI.getInstance().sendMessage(jid+"@", ((ChatActivity)context).getMyBeamsterUserProfile().getName(), ((ChatActivity)context).getMyBeamsterUserProfile().getPictureUrl(), "", newMessage, ((ChatActivity)context).getCurrentCenter().getLatitude(), ((ChatActivity)context).getCurrentCenter().getLongitude(), composing, composingText);
			
			if (composing==false)
			{
				// Get tracker.
		        Tracker t = ((AppConfig)((ChatActivity)context).getApplication()).getTracker(
		            TrackerName.APP_TRACKER);
		        // Build and send an Event.
		        t.send(new HitBuilders.EventBuilder()
		            .setCategory("Message")
		            .setAction("PhotoMessageSent"+this.launchedFrom) // actionId
		            .setLabel("private") 
		            .build());				
			}
		}	
    	
    }


}