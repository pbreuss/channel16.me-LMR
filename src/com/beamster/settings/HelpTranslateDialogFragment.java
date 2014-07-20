package com.beamster.settings;

import java.util.Locale;

import me.channel16.lmr.R;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.Intent;
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

public class HelpTranslateDialogFragment extends DialogFragment {
	
	private static View view;
	
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		Dialog dialog = super.onCreateDialog(savedInstanceState);

		Window window = dialog.getWindow();
		window.setBackgroundDrawableResource(R.color.dialogBackgroundDarkColor);
		window.setTitle(getString(R.string.help_translate));
		return dialog;
	}
	
	public static void openEmail(Context context, String email, String language) {
	    try {
	    	Intent intent = new Intent(Intent.ACTION_SEND);
	    	intent.setType("text/html");
	    	intent.putExtra(Intent.EXTRA_EMAIL, new String[]{email});
	    	intent.putExtra(Intent.EXTRA_SUBJECT, "Translation "+language);
	    	intent.putExtra(Intent.EXTRA_TEXT, "Send me the English channel16.me texts, I'll see what I can do ...");

	    	context.startActivity(Intent.createChooser(intent, "Send Email"));	    	
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
        t.setScreenName("Help_Translate");

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
			view = inflater.inflate(R.layout.help_translate, container, false);
			TextView myHelpTranslateTextFiew = (TextView) view.findViewById(R.id.helpTranslateTextField);
			myHelpTranslateTextFiew.setText(getString(R.string.help_translate_text, Locale.getDefault().getDisplayLanguage()));
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
		
		LinearLayout imgWeb = (LinearLayout) view.findViewById(R.id.emailLink);
		imgWeb.setOnClickListener(new OnClickListener() {
		    public void onClick(View v) {

		        Tracker t = ((AppConfig)getActivity().getApplication()).getTracker(
			            TrackerName.APP_TRACKER);
			        // Build and send an Event.
			        t.send(new HitBuilders.EventBuilder()
			            .setCategory("Help_Translate")
			            .setAction("ClickTranslateEmailIcon") // actionId
			            .build());			    	

			    openEmail(getActivity(), AppConfig.channel16TranslationEmail, Locale.getDefault().getDisplayLanguage());
		    }
		});				
		
		
		return view;
	}

}