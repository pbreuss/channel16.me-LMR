package com.beamster.settings;

import me.channel16.lmr.R;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.InflateException;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.beamster.AppConfig;
import com.beamster.AppConfig.TrackerName;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

public class AboutDialogFragment extends DialogFragment {
	
	private static View view;
	
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		Dialog dialog = super.onCreateDialog(savedInstanceState);

		Window window = dialog.getWindow();
		window.setBackgroundDrawableResource(R.color.dialogBackgroundDarkColor);
		window.setTitle(getString(R.string.about_title));
		return dialog;
	}
	
	public static void getOpenFacebookIntent(Context context, String pageid) 
	{
		try {
			Log.d("BEAMSTER", "check if FB is installed ...");
	        context.getPackageManager()
	                .getPackageInfo("com.facebook.katana", 0); //Checks if FB is even installed.
			Log.d("BEAMSTER", "FB is installed ... do intent: "+"fb://page/"+pageid);
			
			final String url = "fb://page/" + pageid;
			Intent facebookAppIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
			facebookAppIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
					
			context.startActivity(facebookAppIntent);
	    } catch (Exception e) {
	    	try
	    	{
	    		((AppConfig)context).trackException(80, e);			
			}
			catch (Exception e2)
			{
    	        Log.e("BEAMSTER", "Failed to report exception 80"+e2);	    			            		    				
			}

			Log.d("BEAMSTER", "FB is NOT installed ...");
			context.startActivity(new Intent(Intent.ACTION_VIEW,
	                Uri.parse("https://www.facebook.com/"+pageid)));
	    }
	}
	
	public static void openGPlus(Context context, String page) {
	    try {
	    	context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://plus.google.com/b/"+page)));
	    } catch(Exception e) {
	    	try
	    	{
	    		((AppConfig)context).trackException(81, e);			
			}
			catch (Exception e2)
			{
    	        Log.e("BEAMSTER", "Failed to report exception 81"+e2);	    			            		    				
			}

			Log.d("BEAMSTER", "G+ is NOT installed ...");
	    }
	}	
	
	public static void openTwitter(Context context, String user_name) {
	    try {
			Intent intent = new Intent(Intent.ACTION_VIEW,
		    Uri.parse("twitter://user?screen_name="+user_name));
			context.startActivity(intent);		
	    } catch(Exception e) {
	    	try
	    	{
	    		((AppConfig)context).trackException(82, e);				    
			}
			catch (Exception e2)
			{
    	        Log.e("BEAMSTER", "Failed to report exception 82"+e2);	    			            		    				
			}

			Log.d("BEAMSTER", "Twitter is NOT installed ...");
			context.startActivity(new Intent(Intent.ACTION_VIEW,
	                Uri.parse("https://twitter.com/"+user_name)));
	    }
	}	
	
	public static void openWWW(Context context, String page) {
	    try {
	    	context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(page)));
	    } catch(Exception e) {
	    	try
	    	{
	    		((AppConfig)context).trackException(83, e);			
			}
			catch (Exception e2)
			{
    	        Log.e("BEAMSTER", "Failed to report exception 83"+e2);	    			            		    				
			}
	    		
			Log.d("BEAMSTER", "www page cannot be opened ...");
	    }
	}		

	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		
		
		// Google Analytics
		Tracker t = ((AppConfig)getActivity().getApplication()).getTracker(TrackerName.APP_TRACKER);

        // Set screen name.
        t.setScreenName("About");

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
			view = inflater.inflate(R.layout.about_dialog, container, false);
			TextView myAboutTextFiew = (TextView) view.findViewById(R.id.aboutTextField);
			myAboutTextFiew.setText(getString(R.string.about_text, AppConfig.getVersionname(), AppConfig.versionCode+(AppConfig.PRODUCTION?"-Prod":"-Stage")));
		} 
		catch (InflateException e) 
		{
			try
			{
				((AppConfig)getActivity().getApplication()).trackException(84, e);			
			}
			catch (Exception e2)
			{
    	        Log.e("BEAMSTER", "Failed to report exception 84"+e2);	    			            		    				
			}

			/* map is already there, just return view as it is */
		}
		
		LinearLayout img = (LinearLayout) view.findViewById(R.id.fbPageLink);
		img.setOnClickListener(new OnClickListener() {
		    public void onClick(View v) {
		    	
		        Tracker t = ((AppConfig)getActivity().getApplication()).getTracker(
			            TrackerName.APP_TRACKER);
			        // Build and send an Event.
			        t.send(new HitBuilders.EventBuilder()
			            .setCategory("About")
			            .setAction("ClickFBFanPageIcon") // actionId
			            .build());			    	
		    	
		       // your code here
			   getOpenFacebookIntent(getActivity(), AppConfig.channel16meFBFanpageId);
		    }
		});	
		
		LinearLayout imgGP = (LinearLayout) view.findViewById(R.id.googlePlusLink);
		imgGP.setOnClickListener(new OnClickListener() {
		    public void onClick(View v) {

		        Tracker t = ((AppConfig)getActivity().getApplication()).getTracker(
			            TrackerName.APP_TRACKER);
			        // Build and send an Event.
			        t.send(new HitBuilders.EventBuilder()
			            .setCategory("About")
			            .setAction("ClickGPlusFanPageIcon") // actionId
			            .build());			    	
		    	
		    	openGPlus(getActivity(), AppConfig.channel16meGooglePlusPageId);
		    }
		});				

		LinearLayout imgTw = (LinearLayout) view.findViewById(R.id.twitterLink);
		imgTw.setOnClickListener(new OnClickListener() {
		    public void onClick(View v) {

		        Tracker t = ((AppConfig)getActivity().getApplication()).getTracker(
			            TrackerName.APP_TRACKER);
			        // Build and send an Event.
			        t.send(new HitBuilders.EventBuilder()
			            .setCategory("About")
			            .setAction("ClickTwitterFanPageIcon") // actionId
			            .build());			    	

			    openTwitter(getActivity(), AppConfig.channel16meTwitterPageId);
		    }
		});				

		LinearLayout imgWeb = (LinearLayout) view.findViewById(R.id.webLink);
		imgWeb.setOnClickListener(new OnClickListener() {
		    public void onClick(View v) {

		        Tracker t = ((AppConfig)getActivity().getApplication()).getTracker(
			            TrackerName.APP_TRACKER);
			        // Build and send an Event.
			        t.send(new HitBuilders.EventBuilder()
			            .setCategory("About")
			            .setAction("ClickWWWPageIcon") // actionId
			            .build());			    	

			    openWWW(getActivity(), AppConfig.channel16meWebPage);
		    }
		});				
		
		
		return view;
	}

}