package com.beamster.util;

import java.util.HashMap;
import java.util.Map;

import me.channel16.lmr.R;

import org.json.JSONArray;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class Utils {

	public Utils() {
	}

	/**
	Displays the radius in a formatted way. 
	Parameters:
	distanceAway: the distance to be formatted
	radiusUnit: the unit of the distance
	*/
	public static String getFormattedDistance(Context ctx, double distanceAway, String radiusUnit, boolean beamed, String clientId) {
		
		String beamedStr = "", clientIdStr = "";
		if (clientId!=null && !clientId.equals(""))
		clientIdStr = ", "+ctx.getString(R.string.via_client, clientId);
				
		if (beamed == true)
			beamedStr = ", Beamed";
				
		if (radiusUnit !=null && radiusUnit.equals("km"))
		{
			if (distanceAway<0.5)
				return " ("+ctx.getString(R.string.distance_away, (Math.round(distanceAway*1000)), "m")+beamedStr+clientIdStr+")";			
			else
				return " ("+ctx.getString(R.string.distance_away, (Math.round(distanceAway*10)/10), "km")+beamedStr+clientIdStr+")";			
		}	
		else
		{
			if (distanceAway<0.2)
				return " ("+ctx.getString(R.string.distance_away, (Math.round(distanceAway*5280)), "ft")+beamedStr+clientIdStr+")";			
			else
				return " ("+ctx.getString(R.string.distance_away, (Math.round(distanceAway*10)/10), "mi")+beamedStr+clientIdStr+")";			
		}	
	}	
	
	/**
	Displays the radius in a formatted way. 
	Parameters:
	distanceAway: the distance to be formatted
	radiusUnit: the unit of the distance
	*/
	public static String getFormattedDistance(double distanceAway, String radiusUnit) {

		if (radiusUnit !=null && radiusUnit.equals("km"))
		{
			if (distanceAway<0.5)
				return (Math.round(distanceAway*1000))+"m";			
			else
				return (Math.round(distanceAway*10)/10)+"km";			
		}	
		else
		{
			if (distanceAway<0.2)
				return (Math.round(distanceAway*5280))+"ft";			
			else
				return (Math.round(distanceAway*10)/10)+"mi";			
		}	
	}
		
	
	public static double distance (double lat_a, double lng_a, double lat_b, double lng_b ) 
	{
	    double earthRadius = 3958.75;
	    double latDiff = Math.toRadians(lat_b-lat_a);
	    double lngDiff = Math.toRadians(lng_b-lng_a);
	    double a = Math.sin(latDiff /2) * Math.sin(latDiff /2) +
	    Math.cos(Math.toRadians(lat_a)) * Math.cos(Math.toRadians(lat_b)) *
	    Math.sin(lngDiff /2) * Math.sin(lngDiff /2);
	    double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
	    double distance = earthRadius * c;

	    double meterConversion = 1609d;

	    return distance * meterConversion;
	}	
	
	/**
	 * Sending an message to Pebble
	 * @param myActivity
	 * @param title
	 * @param msg
	 */
	public static void sendAlertToPebble(Activity myActivity, String title, String msg) {
	    final Intent i = new Intent("com.getpebble.action.SEND_NOTIFICATION");

	    final Map<String,String> data = new HashMap<String,String>();
	    data.put("title", title);
	    data.put("body", msg);
	    final JSONObject jsonData = new JSONObject(data);
	    final String notificationData = new JSONArray().put(jsonData).toString();

	    i.putExtra("messageType", "PEBBLE_ALERT");
	    i.putExtra("sender", myActivity.getString(R.string.pebble_notifications_title));
	    i.putExtra("notificationData", notificationData);

	    Log.d("BEAMSTER", "About to send a modal alert to Pebble: "+notificationData);
	    myActivity.sendBroadcast(i);
	}		
}
