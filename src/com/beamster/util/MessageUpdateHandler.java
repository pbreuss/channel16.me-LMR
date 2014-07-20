package com.beamster.util;

import android.app.Activity;
import android.content.Intent;
import android.os.Handler;
import android.util.Log;

import com.beamster.AppConfig;
import com.beamster.LoginActivity;
import com.beamster.android_api.BeamsterAPI;
import com.beamster.list.FragmentList;

public class MessageUpdateHandler {
    private static Handler handler;
    private static Activity myContext;
    private static Runnable myRunnable=null;

    public static Handler getHandler(Activity context) {
        if (handler == null) {
        	myContext = context;
            initHandler();
        }
        return handler;
    }

    private static void initHandler() {
        handler = new Handler();
    }

    public static void stopMyHandler() {
    	try
    	{
	    	if (myRunnable!=null)
	    	{
	            handler.removeCallbacksAndMessages(null);
		    	Log.d("BEAMSTER", "Handler null paused/removed ...");
	    	}			
    	}
    	catch (Exception e)
    	{
    		try
    		{
    			((AppConfig)myContext.getApplication()).trackException(110, e);			
			}
			catch (Exception e2)
			{
    	        Log.e("BEAMSTER", "Failed to report exception 110"+e2);	    			            		    				
			}

	    	Log.e("BEAMSTER", "Error removeCallbacksAndMessages Handler: "+e.getMessage());    		
    	}
    }

    public static void pauseMyHandler() {
    	try
    	{
	    	if (myRunnable!=null)
	    	{
	    		handler.removeCallbacksAndMessages(myRunnable);    		
		    	Log.d("BEAMSTER", "Handler paused/removed ...");
	    	}			
    	}
    	catch (Exception e)
    	{
    		try
    		{
    			((AppConfig)myContext.getApplication()).trackException(111, e);			
			}
			catch (Exception e2)
			{
    	        Log.e("BEAMSTER", "Failed to report exception 111"+e2);	    			            		    				
			}

	    	Log.e("BEAMSTER", "Error removing Handler: "+e.getMessage());    		
    	}
    }

    public static void resumeMyHandler() {
    	try
    	{
			handler.postDelayed( myRunnable = new Runnable() {
	
			    @Override
			    public void run() {
			    	Log.d("BEAMSTER", "Handler started ...");
			    	// check connection
			    	if (!BeamsterAPI.getInstance().isConnected() || !BeamsterAPI.getInstance().isAuthenticated())
			    	{
			    		// need to reconnect
				  		try
				  		{
		    				Intent intent = new Intent(myContext, LoginActivity.class);
		    				intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP); 
		    				intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		    				myContext.startActivity(intent);	    	          	
				  		}
				  		catch (Exception e2)
				  		{
							try
							{
								((AppConfig)myContext.getApplication()).trackException(102, e2);
			    			}
			    			catch (Exception e3)
			    			{
				    	        Log.e("BEAMSTER", "Failed to report exception 102"+e3);	    			            		    				
			    			}	  			
				  		}
			    	}
			    	
					// add message to list
					FragmentList fragmentList = (FragmentList)myContext.getFragmentManager().findFragmentByTag("list");
					if (fragmentList!=null)
						fragmentList.updateMessagesList(false);
	
			        handler.postDelayed( myRunnable = this, 60 * 1000 );
			    }
			}, 60 * 1000 );             
    	}
    	catch (Exception e)
    	{
    		try
    		{
    			((AppConfig)myContext.getApplication()).trackException(112, e);			
			}
			catch (Exception e2)
			{
    	        Log.e("BEAMSTER", "Failed to report exception 112"+e2);	    			            		    				
			}

			Log.e("BEAMSTER", "Error resuming Handler: "+e.getMessage());    		
    	}
		
    }
}