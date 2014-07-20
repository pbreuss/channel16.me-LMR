package com.beamster.settings;

import me.channel16.lmr.R;
import android.app.Dialog;
import android.app.DialogFragment;
import android.os.Bundle;
import android.util.Log;
import android.view.InflateException;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;

import com.beamster.AppConfig;
import com.beamster.AppConfig.TrackerName;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

public class PrivacyPolicyDialogFragment extends DialogFragment {
	
	private static View view;
	WebView myWebView=null;
	//The button that closes the dialog  
    private Button btClose;  
	
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		Dialog dialog = super.onCreateDialog(savedInstanceState);

		Window window = dialog.getWindow();
		window.setBackgroundDrawableResource(R.color.dialogBackgroundDarkColor);
		window.setTitle(getString(R.string.privacy_policy));
		return dialog;
	}
		
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		
		
		// Google Analytics
		Tracker t = ((AppConfig)getActivity().getApplication()).getTracker(TrackerName.APP_TRACKER);

        // Set screen name.
        t.setScreenName("PrivacyPolicy");

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
			view = inflater.inflate(R.layout.privacy_policy_dialog, container, false);
			myWebView = (WebView) view.findViewById(R.id.webview);
			
			//Initialize the Button object with the data from the 'webviewdialog.xml' file  
	        btClose = (Button) view.findViewById(R.id.bt_close);  
	        //Define what should happen when the close button is pressed.  
	        btClose.setOnClickListener(new OnClickListener()  
	        {  
	            @Override  
	            public void onClick(View v)  
	            {  
	                //Dismiss the dialog  
	            	PrivacyPolicyDialogFragment.this.dismiss();  
	            }  
	        });  
	        
	        //Scroll bars should not be hidden  
	        myWebView.setScrollbarFadingEnabled(false);  
	        //Disable the horizontal scroll bar  
	        myWebView.setHorizontalScrollBarEnabled(false);  
	        //Set the user agent  
	        myWebView.getSettings().setUserAgentString("AndroidWebView");  
	        //Clear the cache  
	        myWebView.setWebViewClient(new WebViewClient());
	        
	        myWebView.clearCache(true);  
	        //Make the webview load the specified URL  
	        myWebView.loadUrl(AppConfig.channel16TermsWebPage);  	        
			
		} 
		catch (InflateException e) 
		{
			try
			{
				((AppConfig)getActivity().getApplication()).trackException(200, e);			
			}
			catch (Exception e2)
			{
    	        Log.e("BEAMSTER", "Failed to report exception 200"+e2);	    			            		    				
			}

			/* map is already there, just return view as it is */
		}
				
		return view;
	}
	


}