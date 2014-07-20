package com.beamster.settings;

import java.util.ArrayList;
import java.util.List;

import me.channel16.lmr.R;
import android.app.Dialog;
import android.app.DialogFragment;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.InflateException;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;

import com.beamster.AppConfig;
import com.beamster.AppConfig.TrackerName;
import com.beamster.ChatActivity;
import com.beamster.android_api.BeamsterAPI;
import com.beamster.android_api.BeamsterUserProfile;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

public class SettingsDialogFragment extends DialogFragment implements OnSeekBarChangeListener, OnCheckedChangeListener, OnClickListener, OnItemSelectedListener {

	private static View view;
	private RadioButton milesMeterSwitchSmall, milesMeterSwitchLarge, unitOfLengthKm, unitOfLengthMiles;
	private TextView textProgressRange, textProgressRangeValue, textProgressLocationBlurring, showyourSelfAsText, optionsHideOnMapText, showFBFriendsOnlyText, blockAllAnonymousUsersText, blockAllBeamedUsersText;
	private SeekBar rangeToBeseenSeekBar, locationBlurring; // declare seekbar object variable
	private Switch optionsHideOnMap, showFBFriendsOnly, blockAllAnonymousUsers, blockAllBeamedUsers; // declare seekbar object variable
	private Button okButton, cancelButton; // declare seekbar object variable
	private Spinner showYourSelfAsSpinner;
	private EditText nickName, status;
	private View settingsAdvancedList;

	// temp values
	BeamsterUserProfile myBeamsterUserProfile = null;

	public SettingsDialogFragment() {
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {

		
		// Google Analytics
		Tracker t = ((AppConfig)getActivity().getApplication()).getTracker(TrackerName.APP_TRACKER);

        // Set screen name.
        t.setScreenName("Settings");

        // Send a screen view.
        t.send(new HitBuilders.AppViewBuilder().build());  		
		
		if (view != null) 
		{
			ViewGroup parent = (ViewGroup) view.getParent();
			if (parent != null)
				parent.removeView(view);
		}

		try 
		{
			view = inflater.inflate(R.layout.settings_dialog, container, false);
			view.setWillNotDraw(true);
			
			settingsAdvancedList = (View)view.findViewById(R.id.settingsAdvancedList);  
			
			// make text label for progress value
			textProgressRange = (TextView)view.findViewById(R.id.textViewProgress);   
			textProgressRangeValue = (TextView)view.findViewById(R.id.textViewProgressValue);   

			// get fields
			rangeToBeseenSeekBar = (SeekBar)view.findViewById(R.id.rangeToBeseenSeekBar); // make seekbar object
			rangeToBeseenSeekBar.setOnSeekBarChangeListener(this); // set seekbar listener.

			milesMeterSwitchSmall = (RadioButton) view.findViewById(R.id.options_milesMeterSwitch_small);
			milesMeterSwitchSmall.setOnCheckedChangeListener(this);

			milesMeterSwitchLarge = (RadioButton) view.findViewById(R.id.options_milesMeterSwitch_large);
			milesMeterSwitchLarge.setOnCheckedChangeListener(this);            

			showyourSelfAsText = (TextView) view.findViewById(R.id.showyourselfas_text);
			showYourSelfAsSpinner = (Spinner)view.findViewById(R.id.showyourselfas_spinner);
			showYourSelfAsSpinner.setOnItemSelectedListener(this);

			List<String> list = new ArrayList<String>();
			list.add(getString(R.string.options_nicknameandanonymousimage));
			list.add(getString(R.string.options_firstnameandyourfbimage));
			list.add(getString(R.string.options_fullnameandfbimage));
			ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_spinner_item, list);
			dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
			showYourSelfAsSpinner.setAdapter(dataAdapter);            

			nickName = (EditText) view.findViewById(R.id.nickname_edittext);

			optionsHideOnMapText = (TextView)view.findViewById(R.id.options_hideonmap_text);
			optionsHideOnMap = (Switch)view.findViewById(R.id.options_hideonmap);
			optionsHideOnMap.setOnCheckedChangeListener(this);

			showFBFriendsOnlyText = (TextView)view.findViewById(R.id.options_showfbfriendsonly_text);
			showFBFriendsOnly = (Switch)view.findViewById(R.id.options_showfbfriendsonly);
			showFBFriendsOnly.setOnCheckedChangeListener(this);

			blockAllAnonymousUsersText = (TextView)view.findViewById(R.id.options_blockallanonymoususers_text);
			blockAllAnonymousUsers = (Switch)view.findViewById(R.id.options_blockallanonymoususers);
			blockAllAnonymousUsers.setOnCheckedChangeListener(this);

			blockAllBeamedUsersText = (TextView)view.findViewById(R.id.options_blockallbeamedusers_text);
			blockAllBeamedUsers = (Switch)view.findViewById(R.id.options_blockallbeamedusers);
			blockAllBeamedUsers.setOnCheckedChangeListener(this);            

			textProgressLocationBlurring = (TextView)view.findViewById(R.id.optionsText_locationblurring);   

			locationBlurring = (SeekBar)view.findViewById(R.id.options_locationblurring); // make seekbar object
			locationBlurring.setOnSeekBarChangeListener(this); // set seekbar listener.

			status = (EditText) view.findViewById(R.id.options_yourstatus);            

			unitOfLengthKm = (RadioButton) view.findViewById(R.id.unit_of_length_km);
			unitOfLengthKm.setOnCheckedChangeListener(this);

			unitOfLengthMiles = (RadioButton) view.findViewById(R.id.unit_of_length_miles);
			unitOfLengthMiles.setOnCheckedChangeListener(this);

			okButton = (Button)view.findViewById(R.id.options_ok);
			okButton.setOnClickListener(this);

			cancelButton = (Button)view.findViewById(R.id.options_cancel);
			cancelButton.setOnClickListener(this);
			cancelButton.setFocusable(true);

			// initials option values
			try {
				myBeamsterUserProfile = (BeamsterUserProfile) ((ChatActivity)getActivity()).getMyBeamsterUserProfile().clone();

				Log.d("BEAMSTER", "myBeamsterUserProfile retrieved ..."+myBeamsterUserProfile.toString());

				// if anonymous, set several fields to disabled
				if (myBeamsterUserProfile.isAnonymous())
				{
					settingsAdvancedList.setVisibility(View.GONE);	
				}
				else
				{
					settingsAdvancedList.setVisibility(View.VISIBLE);
				}

				// debug
				//settingsAdvancedList.setVisibility(View.VISIBLE);
				
				// set unitSwitch meter/km or feet/miles
				if (myBeamsterUserProfile.getRadiusUnit().equals("km"))
				{
					milesMeterSwitchSmall.setText(R.string.options_meters);
					milesMeterSwitchLarge.setText(R.string.options_km);
				}
				else
				{
					milesMeterSwitchSmall.setText(R.string.options_feet);
					milesMeterSwitchLarge.setText(R.string.options_miles);
				}

				// if range is <1 use smaller unit and display as meter or feet
				if (myBeamsterUserProfile.getRadius()<1)
				{
					milesMeterSwitchSmall.setChecked(true);

					int smallUnitNumber = 1000;    		    
					if (myBeamsterUserProfile.getRadiusUnit().equals("miles"))   		
						smallUnitNumber = 5280;    		    		

					// set initial values
					textProgressRange.setText(getString(R.string.options_rangetobeseen));		    	
					textProgressRangeValue.setText(String.valueOf(Math.round(myBeamsterUserProfile.getRadius())));		    	
					rangeToBeseenSeekBar.setProgress((int)(myBeamsterUserProfile.getRadius()*smallUnitNumber));  
				}
				else
				{
					milesMeterSwitchLarge.setChecked(true);

					// set initial values
					textProgressRange.setText(getString(R.string.options_rangetobeseen));		    	
					textProgressRangeValue.setText(String.valueOf(Math.round(myBeamsterUserProfile.getRadius())));		    	
					rangeToBeseenSeekBar.setProgress((int)myBeamsterUserProfile.getRadius());  
				}					

				showYourSelfAsSpinner.setSelection(myBeamsterUserProfile.getPrivacy()-1);

				nickName.setText(myBeamsterUserProfile.getNickName());

				optionsHideOnMap.setChecked( myBeamsterUserProfile.getHideOnMap()==0?false:true); 
				showFBFriendsOnly.setChecked( myBeamsterUserProfile.getShowFbFriendsOnly()==0?false:true); 
				blockAllAnonymousUsers.setChecked( myBeamsterUserProfile.getBlockAllAnonymousUsers()==0?false:true); 
				blockAllBeamedUsers.setChecked( myBeamsterUserProfile.getBlockBeamedUsers()==0?false:true); 

				textProgressLocationBlurring.setText(getString(R.string.options_locationblurring, (myBeamsterUserProfile.getRadiusUnit().equals("km")?getString(R.string.options_meters):getString(R.string.options_feet)))+" "+Math.round(myBeamsterUserProfile.getLocationBlurring()));				
				locationBlurring.setProgress((int)myBeamsterUserProfile.getLocationBlurring());    
				status.setText(myBeamsterUserProfile.getStatusMessage());

				if (myBeamsterUserProfile.getRadiusUnit().equals("km"))
					unitOfLengthKm.setChecked(true);
				else
					unitOfLengthMiles.setChecked(true);

			} catch (CloneNotSupportedException e) {
				try
				{
					((AppConfig)getActivity().getApplication()).trackException(101, e);			
    			}
    			catch (Exception e2)
    			{
	    	        Log.e("BEAMSTER", "Failed to report exception 101"+e2);	    			            		    				
    			}

				Log.e("BEAMSTER", "myBeamsterUserProfile could not be cloned ..."+e.getMessage());
			}
		} 
		catch (InflateException e) 
		{
			try
			{
				((AppConfig)getActivity().getApplication()).trackException(102, e);			
			}
			catch (Exception e2)
			{
    	        Log.e("BEAMSTER", "Failed to report exception 102"+e2);	    			            		    				
			}

			/* map is already there, just return view as it is */
		}
		return view;
	}

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		Dialog dialog = super.onCreateDialog(savedInstanceState);

		Window window = dialog.getWindow();
		window.setBackgroundDrawableResource(R.color.dialogBackgroundDarkColor);
		window.setTitle(getString(R.string.button_privacysettings));
		return dialog;
	}

	// implemented functions of OnSeekBarChangeListener

	@Override
	public void onProgressChanged(SeekBar seekbar, int value, boolean arg2) {
		switch (seekbar.getId()) {
		case R.id.rangeToBeseenSeekBar:
			// change progress text label with current seekbar value
			textProgressRange.setText(getString(R.string.options_rangetobeseen));
			textProgressRangeValue.setText(String.valueOf(Math.round(value)));
			myBeamsterUserProfile.setRadius(value);
			break;
		case R.id.options_locationblurring:
			// change progress text label with current seekbar value
			textProgressLocationBlurring.setText(getString(R.string.options_locationblurring, (myBeamsterUserProfile.getRadiusUnit().equals("km")?getString(R.string.options_meters):getString(R.string.options_feet)))+" "+Math.round(value));
			myBeamsterUserProfile.setLocationBlurring(value);
		}
		// prevents the view from scrolling up and put focus in a text field
		view.clearFocus();
	}

	@Override
	public void onStartTrackingTouch(SeekBar seekBar) {
	}

	@Override
	public void onStopTrackingTouch(SeekBar seekBar) {
		seekBar.setSecondaryProgress(seekBar.getProgress()); // set the shade of the previous value.
	}

	// implemented functions of OnCheckedChangeListener

	@Override
	public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

		// Is the toggle on?
		if (buttonView instanceof Switch) 
		{
			boolean on = ((Switch) buttonView).isChecked();

			switch (buttonView.getId()) {
			case R.id.options_hideonmap:
				if (on) {
					myBeamsterUserProfile.setHideOnMap(1);
				} else {
					myBeamsterUserProfile.setHideOnMap(0);
				}
				break;
			case R.id.options_showfbfriendsonly:
				if (on) {
					myBeamsterUserProfile.setShowFbFriendsOnly(1);
				} else {
					myBeamsterUserProfile.setShowFbFriendsOnly(0);
				}
				break;
			case R.id.options_blockallanonymoususers:
				if (on) {
					myBeamsterUserProfile.setBlockAllAnonymousUsers(1);
				} else {
					myBeamsterUserProfile.setBlockAllAnonymousUsers(0);
				}
				break;
			case R.id.options_blockallbeamedusers:
				if (on) {
					myBeamsterUserProfile.setBlockBeamedUsers(1);
				} else {
					myBeamsterUserProfile.setBlockBeamedUsers(0);
				}
				break;	    		
			} // end of switch

		}
		else if (buttonView instanceof RadioButton) 
		{
			boolean checked = ((RadioButton) buttonView).isChecked();

			switch (buttonView.getId()) {
			case R.id.unit_of_length_km:
				if (checked)
				{
					// change all the numbers to miles/km
					double radius = myBeamsterUserProfile.getRadius();
					double locationBlurring = myBeamsterUserProfile.getLocationBlurring();
					String lastSetting = myBeamsterUserProfile.getRadiusUnit();

					// recalculate range, locationBlurring and set Seekbar
					if (lastSetting.equals("miles")) {
						// calculate miles to km
						radius = radius*1.609344;	
						locationBlurring = locationBlurring/3.2808399; 
						myBeamsterUserProfile.setRadiusUnit("km");

						milesMeterSwitchSmall.setText(R.string.options_meters);
						milesMeterSwitchLarge.setText(R.string.options_km);
					} 

					textProgressRange.setText(getString(R.string.options_rangetobeseen));
					textProgressRangeValue.setText(" "+String.valueOf(Math.round(radius)));
					myBeamsterUserProfile.setRadius(radius);

					textProgressLocationBlurring.setText(getString(R.string.options_locationblurring, (myBeamsterUserProfile.getRadiusUnit().equals("km")?getString(R.string.options_meters):getString(R.string.options_feet)))+" "+Math.round(locationBlurring));
					myBeamsterUserProfile.setLocationBlurring(locationBlurring);		        		
				}
				break;	
			case R.id.unit_of_length_miles:
				if (checked)
				{
					// change all the numbers to miles/km
					double radius = myBeamsterUserProfile.getRadius();
					double locationBlurring = myBeamsterUserProfile.getLocationBlurring();
					String lastSetting = myBeamsterUserProfile.getRadiusUnit();

					if (lastSetting.equals("km")) {
						// calculate km to miles
						radius = radius*0.62137119223733;		
						locationBlurring = locationBlurring*3.2808399; 
						myBeamsterUserProfile.setRadiusUnit("miles");

						milesMeterSwitchSmall.setText(R.string.options_feet);
						milesMeterSwitchLarge.setText(R.string.options_miles);
					}	    	    

					textProgressRange.setText(getString(R.string.options_rangetobeseen));
					textProgressRangeValue.setText(String.valueOf(Math.round(radius)));				    	
					myBeamsterUserProfile.setRadius(radius);

					textProgressLocationBlurring.setText(getString(R.string.options_locationblurring, (myBeamsterUserProfile.getRadiusUnit().equals("km")?getString(R.string.options_meters):getString(R.string.options_feet)))+" "+Math.round(locationBlurring));
					myBeamsterUserProfile.setLocationBlurring(locationBlurring);
				}
				break;	
			} // end of switch

		}

		// prevents the view from scrolling up and put focus in a text field
		view.clearFocus();
		
	}	

	// implemented functions of OnClickListener

	@Override
	public void onClick(View button) {

		switch (button.getId()) {
		case R.id.options_ok:

			myBeamsterUserProfile.setNickName(nickName.getText().toString());
			myBeamsterUserProfile.setStatusMessage(status.getText().toString());

			Log.d("BEAMSTER", "Do Save ..."+myBeamsterUserProfile.toString());

			BeamsterAPI.getInstance().setProfile(	myBeamsterUserProfile.getUserName(),
					myBeamsterUserProfile.getStatusMessage(), 
					myBeamsterUserProfile.getNickName(), 
					(int)myBeamsterUserProfile.getRadius(), 
					myBeamsterUserProfile.getRadiusUnit(), 
					myBeamsterUserProfile.getPrivacy(), 
					myBeamsterUserProfile.getShowFbFriendsOnly(), 
					myBeamsterUserProfile.getBlockAllAnonymousUsers(), 
					myBeamsterUserProfile.getBlockBeamedUsers(), 
					myBeamsterUserProfile.getHideOnMap(), 
					myBeamsterUserProfile.getLocationBlurring(), 
					"", // TODO FBFriends
					true);

			// now set the profile to the one in memory
			((ChatActivity)getActivity()).setMyBeamsterUserProfile(myBeamsterUserProfile);

			((ChatActivity)getActivity()).setProfileSaved(true);
			
			Location gps = ((ChatActivity)getActivity()).getCurrentCenter();
			
			// save a location on server
			BeamsterAPI.getInstance().setLocation(gps.getLatitude(), 
					gps.getLongitude(),
					gps.getBearing(),
					gps.getSpeed(),
					false);			
			
            Tracker t = ((AppConfig)getActivity().getApplication()).getTracker(
    	            TrackerName.APP_TRACKER);
    	        // Build and send an Event.
    	        t.send(new HitBuilders.EventBuilder()
    	            .setCategory("Settings")
    	            .setAction("SaveSettings") // actionId
    	            .setLabel("Privacy"+myBeamsterUserProfile.getPrivacy()) 
    	            .build());	

    	        // Build and send an Event.
    	        t.send(new HitBuilders.EventBuilder()
    	            .setCategory("Settings")
    	            .setAction("SaveSettings") // actionId
    	            .setLabel("HideOnMap"+(myBeamsterUserProfile.getHideOnMap()==0?"Off":"On"))
    	            .build());	
    	        
    	        // Build and send an Event.
    	        if (myBeamsterUserProfile.getLocationBlurring()==0)
    	        {
        	        t.send(new HitBuilders.EventBuilder()
    	            .setCategory("Settings")
    	            .setAction("SaveSettings") // actionId
    	            .setLabel("LocationBlurringOff") 
    	            .build());	    	        	
    	        }
    	        else
    	        {
        	        t.send(new HitBuilders.EventBuilder()
    	            .setCategory("Settings")
    	            .setAction("SaveSettings") // actionId
    	            .setLabel("LocationBlurringOn") 
    	            .setValue((long)myBeamsterUserProfile.getLocationBlurring()) 
    	            .build());	    	        	
    	        }

    	        // Build and send an Event.
    	        t.send(new HitBuilders.EventBuilder()
    	            .setCategory("Settings")
    	            .setAction("SaveSettings") // actionId
    	            .setLabel("Radius"+myBeamsterUserProfile.getRadiusUnit()) 
    	            .setValue((long)myBeamsterUserProfile.getRadius()) 
    	            .build());	    	        
    	        
			this.dismiss();
			break;
		case R.id.options_cancel:
			Log.d("BEAMSTER", "Do Cancel ...");
			this.dismiss();
			break;
		}

	}


	// implemented functions of OnItemSelectedListener	

	@Override
	public void onItemSelected(AdapterView<?> adapterView, View arg1, int pos,
			long id) {
		switch (adapterView.getId()) {
		case R.id.showyourselfas_spinner:
			Log.d("BEAMSTER", "selected ... pos: "+pos+" id: "+id);

			// set privacy
			myBeamsterUserProfile.setPrivacy(pos+1);

			break;

		}
	}

	@Override
	public void onNothingSelected(AdapterView<?> arg0) {

	}




}