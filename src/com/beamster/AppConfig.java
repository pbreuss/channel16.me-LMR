package com.beamster;

import java.util.HashMap;

import android.app.Application;
import android.util.Log;

import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.StandardExceptionParser;
import com.google.android.gms.analytics.Tracker;

public class AppConfig extends Application {
	
	// same for stage/prod
	public final static boolean PRODUCTION = false;
	final static String api_key = "khfkglwrtj45"; // enter your API key here - you can get your own api key be sending an email to info@channel16.me
	final static String secret = "78539647"; // enter your API secret here
	final static String CLIENT_ID = "Channel16.me LMR/Android";
	public static int versionCode = 0; 
	public static String versionName = ""; 
	
    // The following line should be changed to include the correct property id.
    private static final String PROPERTY_ID = "UA-49617456-3"; // Channel16.me LRM Tracking ID
    public static final String channel16meFBFanpageId = "174923596048420";
    public static final String channel16meGooglePlusPageId = "109882967958105142007/+Channel16Me_info";
    public static final String channel16meTwitterPageId = "channel16me";
    public static final String channel16meWebPage = "http://www.channel16.me";
    public static final String channel16TermsWebPage = "http://www.beamster.com/wordpress/channel16_terms.html";    
    public static final String channel16TranslationEmail = "translate@channel16.me";
    
    // add ISO3 language code here. If listed, the "please help translate" will not be displayed
    public static final String[] translatedLanguages = {"eng", "deu", "ger", "pol", "jpn", "nld", "rus", "ces", "ita", "fra", "spa", "msa", "por"};
    
    public static int GENERAL_TRACKER = 0;

    public enum TrackerName {
        APP_TRACKER, // Tracker used only in this app.
        GLOBAL_TRACKER, // Tracker used by all the apps from a company. eg: roll-up tracking.
        ECOMMERCE_TRACKER, // Tracker used by all ecommerce transactions from a company.
    }

    HashMap<TrackerName, Tracker> mTrackers = new HashMap<TrackerName, Tracker>();

    public AppConfig() {
        super();
    }

    public synchronized Tracker getTracker(TrackerName trackerId) {
        if (!mTrackers.containsKey(trackerId)) {

            GoogleAnalytics analytics = GoogleAnalytics.getInstance(this);
            Tracker t = analytics.newTracker(PROPERTY_ID);
            t.enableAutoActivityTracking(true); // to report unhandled exceptions
            mTrackers.put(trackerId, t);

        }
        return mTrackers.get(trackerId);
    }
	
	public static int getVersioncode() {
		return versionCode;
	}

	public static String getVersionname() {
		return versionName;
	}
	
	public void trackException(int id, Exception e) {
		  
		try
		{
	        Tracker t = getTracker(TrackerName.APP_TRACKER);
	 	  	  
		    // Build and send exception.
		    t.send(new HitBuilders.ExceptionBuilder()
	        	.setDescription(new StandardExceptionParser(this, null).getDescription(id+": "+Thread.currentThread().getName(), e))
	            .setFatal(false)
	            .build()
		    );
		}
		catch (Exception e2)
		{
	        Log.e("BEAMSTER", "Exception in trackException: "+2);			
		}
	}
	
    
}
