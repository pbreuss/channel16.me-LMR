package com.beamster.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import android.app.Activity;
import android.os.Handler;
import android.util.Log;

import com.beamster.list.FragmentList;
import com.ocpsoft.pretty.time.PrettyTime;
import com.ocpsoft.pretty.time.TimeUnit;
import com.ocpsoft.pretty.time.units.JustNow;
import com.ocpsoft.pretty.time.units.Millisecond;

public class PrettyTimeSingleton {

	private static PrettyTime prettyTime = null;

    public static PrettyTime getInstance(Locale locale) {
        if (prettyTime == null) {
            init(locale);
        }
        return prettyTime;
    }

    private static void init(Locale locale) {
        // init pretty time
        prettyTime = new PrettyTime(locale); // get locale from global settings
		List<TimeUnit> units = new ArrayList<TimeUnit>();
		for( TimeUnit t : prettyTime.getUnits() ) 
		{
			Log.d("BEAMSTER", "TimeUnit added: "+t);
			if(!(t instanceof JustNow) && !(t instanceof Millisecond)) 
			{
				units.add(t);
			}
			else
				Log.d("BEAMSTER", "TimeUnit not added: "+t);
		}
		prettyTime.setUnits(units);	
    }

    public static void destroy() {
    	try
    	{
    		prettyTime = null;
    	}
    	catch (Exception e)
    	{
	    	Log.e("BEAMSTER", "Error PrettyTime destroy: "+e.getMessage());    		
    	}
    }


}