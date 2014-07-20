package com.beamster.settings;

import me.channel16.lmr.R;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.InflateException;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.RadioButton;
import android.widget.TextView;

import com.beamster.AppConfig;
import com.beamster.AppConfig.TrackerName;
import com.beamster.ChatActivity;
import com.beamster.util.Utils;
import com.getpebble.android.kit.PebbleKit;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

public class PebbleDialogFragment extends DialogFragment implements OnCheckedChangeListener, OnClickListener  {

	private static View view;
	private RadioButton radioButtonPebbleNotificationsEnabled;
	private RadioButton radioButtonPebbleNotificationsDisabled;
	private Button okButton, cancelButton;
	private TextView pebble_onoff;
	private String userName = "";
	
	public PebbleDialogFragment() {
	}
	
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		Dialog dialog = super.onCreateDialog(savedInstanceState);

		Window window = dialog.getWindow();
		window.setBackgroundDrawableResource(R.color.dialogBackgroundDarkColor);
		window.setTitle(getString(R.string.pebble_smartwatch));
		return dialog;
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {

		
		// Google Analytics
		Tracker t = ((AppConfig)getActivity().getApplication()).getTracker(TrackerName.APP_TRACKER);

        // Set screen name.
        t.setScreenName("Pebble");

        // Send a screen view.
        t.send(new HitBuilders.AppViewBuilder().build());  		
		
		if (view != null) 
		{
			ViewGroup parent = (ViewGroup) view.getParent();
			if (parent != null)
				parent.removeView(view);
		}
		
		userName = ((ChatActivity)getActivity()).getMyBeamsterUserProfile().getUserName();		

		SharedPreferences sharedPref = getActivity().getPreferences(Context.MODE_PRIVATE);
		boolean pebbleNotificationEnabled = sharedPref.getBoolean("me.channel16.pebbleNotificationEnabled."+userName, false);		
		
		try 
		{
			view = inflater.inflate(R.layout.pebble_dialog, container, false);
			view.setWillNotDraw(true);
			
			boolean connected = PebbleKit.isWatchConnected(getActivity());
			Log.d("BEAMSTER", "Pebble is " + (connected ? "connected" : "not connected"));			
			
			pebble_onoff = (TextView)view.findViewById(R.id.pebble_onoff);   
			pebble_onoff.setText(getString(R.string.pebble_onOffText, (connected ? getString(R.string.connected) : getString(R.string.notconnected))));
			
			radioButtonPebbleNotificationsEnabled = (RadioButton) view.findViewById(R.id.enable_pebble_notification_yes);
			radioButtonPebbleNotificationsEnabled.setChecked(pebbleNotificationEnabled);
			radioButtonPebbleNotificationsEnabled.setOnCheckedChangeListener(this);			
			
			radioButtonPebbleNotificationsDisabled = (RadioButton) view.findViewById(R.id.enable_pebble_notification_no);
			radioButtonPebbleNotificationsDisabled.setChecked(!pebbleNotificationEnabled);
			radioButtonPebbleNotificationsDisabled.setOnCheckedChangeListener(this);			
			
			radioButtonPebbleNotificationsEnabled.setEnabled(connected);
			radioButtonPebbleNotificationsDisabled.setEnabled(connected);
			
			okButton = (Button)view.findViewById(R.id.options_ok);
			okButton.setOnClickListener(this);

			cancelButton = (Button)view.findViewById(R.id.options_cancel);
			cancelButton.setOnClickListener(this);
			cancelButton.setFocusable(true);				
		} 
		catch (InflateException e) 
		{
			try
			{
				((AppConfig)getActivity().getApplication()).trackException(100, e);						
			}
			catch (Exception e2)
			{
    	        Log.e("BEAMSTER", "Failed to report exception 100"+e2);	    			            		    				
			}
				
			/* map is already there, just return view as it is */
		}
		return view;
	}

	
	@Override
	public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

		boolean on = buttonView.isChecked();
		
		Log.d("BEAMSTER", "Pebble Notification enabled? "+on);

		// prevents the view from scrolling up and put focus in a text field
		view.clearFocus();
		
	}

	@Override
	public void onClick(View button) 
	{
		
		switch (button.getId()) {
		case R.id.options_ok:

			Log.d("BEAMSTER", "Pebble Notification enabled? "+radioButtonPebbleNotificationsEnabled.isChecked());

			SharedPreferences sharedPref = getActivity().getPreferences(Context.MODE_PRIVATE);
			SharedPreferences.Editor editor = sharedPref.edit();
			editor.putBoolean("me.channel16.pebbleNotificationEnabled."+userName, radioButtonPebbleNotificationsEnabled.isChecked());
			editor.commit();			
			
			if (radioButtonPebbleNotificationsEnabled.isChecked())
			{
				Utils.sendAlertToPebble(getActivity(), getString(R.string.pebble_notifications_title), getString(R.string.pebble_notifications_turned_on));
				
				// Get tracker.
		        Tracker t = ((AppConfig)getActivity().getApplication()).getTracker(
		            TrackerName.APP_TRACKER);
		        // Build and send an Event.
		        t.send(new HitBuilders.EventBuilder()
		            .setCategory("Pebble")
		            .setAction("PebbleChanged") // actionId
		            .setLabel("Pebble") 
		            .setValue(1)
		            .build());				
								
			}
			else
			{

				// Get tracker.
		        Tracker t = ((AppConfig)getActivity().getApplication()).getTracker(
		            TrackerName.APP_TRACKER);
		        // Build and send an Event.
		        t.send(new HitBuilders.EventBuilder()
		            .setCategory("Pebble")
		            .setAction("PebbleChanged") // actionId
		            .setLabel("Pebble") 
		            .setValue(0)
		            .build());				
			}
			
			this.dismiss();
			break;
		case R.id.options_cancel:
			Log.d("BEAMSTER", "Do Cancel ...");
			this.dismiss();
			break;
		}

		
	}	
	


}