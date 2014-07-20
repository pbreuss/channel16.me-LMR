package com.beamster.list;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Random;

import me.channel16.lmr.R;

import org.jivesoftware.smack.XMPPException;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.FragmentManager;
import android.app.ListFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import com.beamster.AppConfig;
import com.beamster.AppConfig.TrackerName;
import com.beamster.BeamsterMessage;
import com.beamster.ChatActivity;
import com.beamster.LoginActivity;
import com.beamster.android_api.BeamsterAPI;
import com.beamster.audio.AudioVisualizationDialogFragment;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

public class FragmentList extends ListFragment implements OnClickListener, OnTouchListener, TextWatcher {

	FragmentListAdapter adapter;
	EditText text = null;
	Button sendButton = null;
	ImageButton photoButton = null;
	ImageButton recordAudioButton = null;
	static Random rand = new Random();	
	static String sender;
	String jid = null;
	
	AudioVisualizationDialogFragment myAudioVisualizationDialogFragment = null;
		
	
	
    @Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.d("BEAMSTER", "FragmentList onCreate");				
		
		if (getArguments()!=null)
		{
			this.jid = getArguments().getString("jid");
		}
		
		// the public channel
		adapter = new FragmentListAdapter(getActivity(), getMessages());			
		
		setListAdapter(adapter);	
		
	}

    // returns the messages of this FragmentList
    private ArrayList<BeamsterMessage> getMessages()
    {
		if (jid==null || jid.equals(""))
		{
			// this are the group chat messages
			return ((ChatActivity)getActivity()).getMessages();
		}
		else
		{
			// these are the private channel messages
			return ((ChatActivity)getActivity()).getMessages(jid);
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

			if (composing==false)
			{
				// Get tracker.
		        Tracker t = ((AppConfig)getActivity().getApplication()).getTracker(
		            TrackerName.APP_TRACKER);
		        // Build and send an Event.
		        t.send(new HitBuilders.EventBuilder()
		            .setCategory("Message")
		            .setAction("TextMessageSentList") // actionId
		            .setLabel("chat") 
		            .build());								
			}
		}
		else
		{
			// these are the private channel messages
			// send a group chat message
			BeamsterAPI.getInstance().sendMessage(jid+"@", ((ChatActivity)getActivity()).getMyBeamsterUserProfile().getName(), ((ChatActivity)getActivity()).getMyBeamsterUserProfile().getPictureUrl(), "", newMessage, ((ChatActivity)getActivity()).getCurrentCenter().getLatitude(), ((ChatActivity)getActivity()).getCurrentCenter().getLongitude(), composing, composingText);
			
			if (composing==false)
			{
				// Get tracker.
		        Tracker t = ((AppConfig)getActivity().getApplication()).getTracker(
		            TrackerName.APP_TRACKER);
		        // Build and send an Event.
		        t.send(new HitBuilders.EventBuilder()
		            .setCategory("Message")
		            .setAction("TextMessageSentList") // actionId
		            .setLabel("private") 
		            .build());				
			}
		}			    	
    	
    }
    
    
	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
		Log.d("BEAMSTER", "FragmentList onCreateView");				
        
		View rootView = inflater.inflate(R.layout.fragment_list, container, false);        
				
		this.sendButton = (Button) rootView.findViewById(R.id.list_send_button);
		this.sendButton.setOnClickListener(this);		
		
		this.photoButton = (ImageButton) rootView.findViewById(R.id.list_photo_button);
		this.photoButton.setOnClickListener(this);				
		
		this.recordAudioButton = (ImageButton) rootView.findViewById(R.id.list_record_audio_button);
		this.recordAudioButton.setOnTouchListener(this);		

		this.text = (EditText) rootView.findViewById(R.id.text);				
		this.text.addTextChangedListener(this);			            	
        return rootView;
	}
	
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        
		Log.d("BEAMSTER", "FragmentList onActivityCreated");				
    }    
    	
    
    
	@Override
	public void onDestroyView() {
		super.onDestroyView();
		Log.d("BEAMSTER", "FragmentList onDestroyView");				

	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		Log.d("BEAMSTER", "FragmentList onAttach");				

	}

	@Override
	public void onDestroy() {
		super.onDestroy();		
		Log.d("BEAMSTER", "FragmentList nDestroy");				

	}

	@Override
	public void onDetach() {
		super.onDetach();
		Log.d("BEAMSTER", "FragmentList onDetach");				

	}

	@Override
	public void onLowMemory() {
		super.onLowMemory();
		Log.d("BEAMSTER", "FragmentList onLowMemory");				

	}

	@Override
	public void onPause() {
		super.onPause();
		Log.d("BEAMSTER", "FragmentList onPause");						
	}

	@Override
	public void onResume() {
		super.onResume();		
		Log.d("BEAMSTER", "FragmentList onResume");						
		
		try
		{
			if (isAdded() || isHidden()) 
			{		
				getActivity().runOnUiThread(new Runnable() {
		            @Override
		            public void run() {
		          		 getListView().setSelection(getMessages().size()-1);
		            }
		        });		
			} 		
		}
		catch (Exception e)
		{
			try
			{
				((AppConfig)getActivity().getApplication()).trackException(228, e);									
			}
			catch (Exception e2)
			{
    	        Log.e("BEAMSTER", "Failed to report exception 228"+e2);	    			            		    				
			}
		}
		
		// Google Analytics
		Tracker t = ((AppConfig)getActivity().getApplication()).getTracker(TrackerName.APP_TRACKER);

        // Set screen name.
        t.setScreenName("ListView");

        // Send a screen view.
        t.send(new HitBuilders.AppViewBuilder().build());  		
	}

	@Override
	public void onStart() {
		super.onStart();
		Log.d("BEAMSTER", "FragmentList onStart");				
	}

	@Override
	public void onStop() {
		super.onStop();
		Log.d("BEAMSTER", "FragmentList onStop");				
	}

	/**
	 * Updates the list view. If true, it scrolls to the end
	 */
	public void updateMessagesList(final boolean moveToEnd)
	{
		try
		{
			if (isAdded() || isHidden()) 
			{		
				getActivity().runOnUiThread(new Runnable() {
		            @Override
		            public void run() {
		           	 adapter.notifyDataSetChanged();
		           	 if (moveToEnd)
		           	 {
	           			 getListView().setSelection(getMessages().size()-1);
		           	 }
		            }
		        });		
			} 			
		}
		catch (Exception e)
		{
			try
			{
				((AppConfig)getActivity().getApplication()).trackException(269, e);									
			}
			catch (Exception e2)
			{
    	        Log.e("BEAMSTER", "Failed to report exception 269"+e2);	    			            		    				
			}
		}
	}

	@Override
	public void onClick(View v) {
		
		 switch (v.getId()) {
	        case R.id.list_photo_button:
	    		// pick an image from the gallery or cam
	    		((ChatActivity)getActivity()).selectImage(jid, "list");
	        	break;
	        case R.id.list_send_button:
	    		String newMessage = text.getText().toString().trim(); 
	    		if(newMessage.length() > 0)
	    		{
	    			text.setText("");

					// create new messaging object
					BeamsterMessage myBeamsterMessage = new BeamsterMessage(newMessage, true);	    
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
            		
            		// get the name of the current tab
            		ActionBar.Tab selectedTab = ((ChatActivity)getActivity()).getActionBar().getSelectedTab();
            		if (selectedTab.getPosition()<=1)            		
            		{
	            		// add it to list of messages
		    			((ChatActivity)getActivity()).getMessages().add(myBeamsterMessage);										
            		}
            		else
            		{
	            		// add it to list of messages
		    			((ChatActivity)getActivity()).getMessages(jid).add(myBeamsterMessage);					
            		}

            		Log.d("BEAMSTER", "message posted: "+myBeamsterMessage);
            		
	    			
	    			// add message to the adapter	    			
					try {
						
						// send a message (false = composing is false)
						send(newMessage, false, "");

					} catch (XMPPException e) {
						try
						{
							((AppConfig)getActivity().getApplication()).trackException(50, e);			
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
							((AppConfig)getActivity().getApplication()).trackException(51, e);									
		    			}
		    			catch (Exception e2)
		    			{
    		    	        Log.e("BEAMSTER", "Failed to report exception 51"+e2);	    			            		    				
		    			}

						Toast.makeText(getActivity(), "Connection lost ... logging out ... sorry.", 2000).show();
						// if the above crashed ... do this
	    				Intent intent = new Intent(getActivity(), LoginActivity.class);
	    				intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP); 
	    				intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
	    				startActivity(intent);	    				
		        	}

					// add message to list
					updateMessagesList(true);
            		
		        	sendButton.setVisibility(View.GONE);
		        	photoButton.setVisibility(View.VISIBLE);
            		
					// hide soft keyboard
					InputMethodManager imm = (InputMethodManager)getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
					imm.hideSoftInputFromWindow(text.getWindowToken(), 0);				
	    		}
	            break;
		 }		
	}

	@Override
	public void afterTextChanged(Editable s) {}

	@Override
	public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

	@Override
	public void onTextChanged(CharSequence s, int start, int before, int count) {
		try
		{
			// if characters have been deleted, but there are still characters, do nothing
			if (s.length()>0 && s.charAt(s.length() - 1) == '\n') 
			{
	            Log.d("BEAMSTER", "Enter was pressed");	            
	        	sendButton.performClick();
	        }
	        else if (s.length()>0) // send a composing message
			{
	        	sendButton.setVisibility(View.VISIBLE);
	        	photoButton.setVisibility(View.GONE);
	        	
				try {
					// send composing message "typing"
					send("", true, BeamsterAPI.COMPOSING_TYPE_TYPING);
				} catch (XMPPException e) {
					try
					{
						((AppConfig)getActivity().getApplication()).trackException(52, e);			
	    			}
	    			catch (Exception e2)
	    			{
		    	        Log.e("BEAMSTER", "Failed to report exception 52"+e2);	    			            		    				
	    			}

					Log.e("BEAMSTER", "Composing Message could not be sent");
				} catch (Exception e)
	        	{
					try
					{
						((AppConfig)getActivity().getApplication()).trackException(53, e);			
	    			}
	    			catch (Exception e2)
	    			{
		    	        Log.e("BEAMSTER", "Failed to report exception 53"+e2);	    			            		    				
	    			}

					Toast.makeText(getActivity(), "Connection lost ... logging out ... sorry.", 2000).show();
					// if the above crashed ... do this
    				Intent intent = new Intent(getActivity(), LoginActivity.class);
    				intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP); 
    				intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    				startActivity(intent);	    				
	        	}							
			}
	        else
	        {
	        	sendButton.setVisibility(View.GONE);
	        	photoButton.setVisibility(View.VISIBLE);	        	
	        }
		}
        catch (Exception e)
        {
        	try
        	{
        		((AppConfig)getActivity().getApplication()).trackException(54, e);			
			}
			catch (Exception e2)
			{
    	        Log.e("BEAMSTER", "Failed to report exception 54"+e2);	    			            		    				
			}
			Log.e("BEAMSTER", "onTextChanged: "+e.getMessage());	        	
        }
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
	
	long start = 0; 

	@Override
	public boolean onTouch(View arg0, MotionEvent event) {

		FragmentManager fm = getFragmentManager();
		
		switch (event.getAction())		
		{
			case MotionEvent.ACTION_DOWN:
				myAudioVisualizationDialogFragment = new AudioVisualizationDialogFragment(jid, "list");
				myAudioVisualizationDialogFragment.setRetainInstance(true);
				myAudioVisualizationDialogFragment.show(fm, "audioVisualizationDialogFragment");
				start = System.currentTimeMillis();
				break;
			case MotionEvent.ACTION_UP:
				if (myAudioVisualizationDialogFragment!=null)
				{
					myAudioVisualizationDialogFragment.dismiss();
					long timePassed = System.currentTimeMillis()-start;
					Log.d("BEAMSTER", "Time passed: "+timePassed);
					if (timePassed<200)
					{
						getActivity().runOnUiThread(new Runnable() {
				            @Override
				            public void run() {
								//Toast.makeText(getActivity(), R.string.holdtotalk, 7000).show();
								showAlert(R.string.app_name, R.string.holdtotalk);
				            }
				        });		
						
					}
					
				}
				break;
		}
		return false;
	}

	
	
    
}