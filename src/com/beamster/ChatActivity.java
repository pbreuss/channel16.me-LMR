package com.beamster;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;
import java.util.Set;

import me.channel16.lmr.R;

import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.PacketExtension;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.Presence.Type;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smackx.chatstates.packet.ChatStateExtension;

import android.app.ActionBar;
import android.app.ActionBar.Tab;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.location.Location;
import android.location.LocationManager;
import android.media.ExifInterface;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.ParcelFileDescriptor;
import android.os.PowerManager;
import android.provider.MediaStore;
import android.provider.Settings;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.beamster.AppConfig.TrackerName;
import com.beamster.android_api.BeamsterAPI;
import com.beamster.android_api.BeamsterAPI.BeamsterPacketListener;
import com.beamster.android_api.BeamsterGetMessagesIQ;
import com.beamster.android_api.BeamsterGetMessagesIQ.BeamsterMessageItem;
import com.beamster.android_api.BeamsterGetRosterIQ;
import com.beamster.android_api.BeamsterRosterItem;
import com.beamster.android_api.BeamsterRosterPresence;
import com.beamster.android_api.BeamsterSecurityException;
import com.beamster.android_api.BeamsterSetLocationIQ;
import com.beamster.android_api.BeamsterUserProfile;
import com.beamster.android_api.ISO8601;
import com.beamster.audio.Utilities;
import com.beamster.list.FragmentList;
import com.beamster.map.FragmentMap;
import com.beamster.people.PeopleDialogFragment;
import com.beamster.photo.PhotoHelper;
import com.beamster.settings.AboutDialogFragment;
import com.beamster.settings.AccountsDialogFragment;
import com.beamster.settings.HelpTranslateDialogFragment;
import com.beamster.settings.PebbleDialogFragment;
import com.beamster.settings.SettingsDialogFragment;
import com.beamster.util.MessageUpdateHandler;
import com.beamster.util.Utils;
import com.getpebble.android.kit.PebbleKit;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.maps.LocationSource;

public class ChatActivity extends Activity implements OnCompletionListener, 
			GooglePlayServicesClient.ConnectionCallbacks, 
			GooglePlayServicesClient.OnConnectionFailedListener,
			LocationListener, LocationSource {
		
	// Declare Tab Variable
    private static ActionBar.Tab Tab1, Tab2;
    
	private static BeamsterPacketListener myPacketListener = null;
	private static BeamsterUserProfile myBeamsterUserProfile = null;
	private static ArrayList<BeamsterRosterItem> people;
	private static int selectedPerson = 0; // maybe not needed
	
	private static boolean beamed = false; // tells if beamed; if user moved the map before beaming, beaming is true, if user clicked true location, beaming is false
	private static boolean loginMessage = true; // if user just logged in (if true, display welcome message next time all_messages are loaded)
	private static boolean profileSaved = false; // this variable is set to true, when the profile has been saved, which means that users should be reloaded. if false, user should only be reloaded if the users move by 1000m
	
	private static boolean doNotify = false;
	private static boolean pebbleNotificationEnabled = false;
    private static NotificationManager mNotificationManager;
    NotificationCompat.Builder builder;
    private static int numNotificationMessages = 0;
	
 // Global variable to hold the current location
    private static Location mCurrentLocation = null;
    private static Location mCurrentCenterLastUpdateRoster = null;
    private static OnLocationChangedListener mapLocationListener=null;
	
	// this list stores the current chat messages
	private static ArrayList<BeamsterMessage> messages;
	
	// in this Hashtable we are storing the the messages of the closed Channels
	private static Hashtable<String,ArrayList<BeamsterMessage>> closedChannels = null;
	private static boolean reconnecting = false;
		
	ActionBar bar = null;
	// Media Player
	public  MediaPlayer mp;
	// Handler to update UI timer, progress bar etc,.
	private static Handler mHandler = new Handler();
	private Utilities utils;	
	private ImageButton buttonPlay = null; // this is always the current play button that needs to be changed if in OnCompletion
	ProgressDialog dialog = null;
	LocationClient mLocationClient = null;
	// Define an object that holds accuracy and frequency parameters
    LocationRequest mLocationRequest;
    final boolean LOCATION_UPDATES = true;
    PeopleDialogFragment peopleDialog = null;
	private static AlertDialog privateChannelDialog = null;
	
	// Global constants
    /*
     * Define a request code to send to Google Play services
     * This code is returned in Activity.onActivityResult
     */
    private final static int CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;
    private static final int REQUEST_CODE_RECOVER_PLAY_SERVICES = 1001;
    // Milliseconds per second
    private static final int MILLISECONDS_PER_SECOND = 1000;
    // Update frequency in seconds
    public static final int UPDATE_INTERVAL_IN_SECONDS = 10;
    // Update frequency in milliseconds
    private static final long UPDATE_INTERVAL =
            MILLISECONDS_PER_SECOND * UPDATE_INTERVAL_IN_SECONDS;
    // The fastest update frequency, in seconds
    private static final int FASTEST_INTERVAL_IN_SECONDS = 1;
    // A fast frequency ceiling in milliseconds
    private static final long FASTEST_INTERVAL =
            MILLISECONDS_PER_SECOND * FASTEST_INTERVAL_IN_SECONDS;    
    private static final int SMALLES_DISPLACEMENT_METERS = 10;
	
	@Override
    public void onCreate(Bundle savedInstanceState) 
	{
        super.onCreate(savedInstanceState);
        setContentView(R.layout.chat_activity);
        this.dialog = ProgressDialog.show(ChatActivity.this, getString(R.string.connecting), getString(R.string.please_wait), false);
        
        // setConnectionErrorCallback
        try
        {
            BeamsterAPI.getInstance().setConnectionErrorCallback(myConnectionErrorCallback);        	
        }
        catch (Exception e)
        {
        	// if connection has been null and BeamsterAPI.getInstance() return null
	  		try
	  		{
				Intent intent = new Intent(ChatActivity.this, LoginActivity.class);
				intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP); 
				intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				startActivity(intent);	    	          	
	  		}
	  		catch (Exception e2)
	  		{
				try
				{
					((AppConfig)getApplication()).trackException(2002, e2);
    			}
    			catch (Exception e3)
    			{
	    	        Log.e("BEAMSTER", "Failed to report exception 2002"+e3);	    			            		    				
    			}	  			
	  		}
        	
        }
        // TODO fix nullpointer exception here
        
        bar = getActionBar();
        bar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        bar.setDisplayShowTitleEnabled(true);
        bar.setDisplayUseLogoEnabled(true);
        bar.setHomeButtonEnabled(true);
        
        bar.addTab(bar.newTab()
                .setText(R.string.tab_title_list)
                .setTabListener(new TabListener<FragmentList>(this, "list", FragmentList.class)));
        bar.addTab(bar.newTab()
                .setText(R.string.tab_title_map)
                .setTabListener(new TabListener<FragmentMap>(this, "map", FragmentMap.class)));

        if (savedInstanceState != null) {
            bar.setSelectedNavigationItem(savedInstanceState.getInt("tab", 0));
        }
        
		Intent intent = getIntent();
        
		// set beamed
		ChatActivity.beamed = intent.getBooleanExtra("com.beamster.beamed", false);  
		ChatActivity.myBeamsterUserProfile = (BeamsterUserProfile) intent.getSerializableExtra("com.beamster.BeamsterAPI.BeamsterUserProfile");
		
		// TODO pass profile in here
		try
		{
			//Log.d("BEAMSTER", "com.beamster.BeamsterAPI.BeamsterUserProfile: "+intent.getSerializableExtra("com.beamster.BeamsterAPI.BeamsterUserProfile"));
			//Log.d("BEAMSTER", "this.myBeamsterUserProfile: "+this.myBeamsterUserProfile);
			
			Log.i("BEAMSTER", "Opening connection to: "+ChatActivity.myBeamsterUserProfile.getName()+ "/" + intent.getStringExtra("beamsterUsername"));			
		}
		catch (Exception e)
		{
			try
			{
				((AppConfig)getApplication()).trackException(1, e);
			}
			catch (Exception e2)
			{
    	        Log.e("BEAMSTER", "Failed to report exception 1"+e2);	    			            		    				
			}
				
			Log.e("BEAMSTER", "in ChatActivity create: "+e.getMessage());			
		}
                        		
        mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		
		// set notifications
        builder =
                new NotificationCompat.Builder(ChatActivity.this)
                .setSmallIcon(R.drawable.ic_stat_notification)
                .setDefaults(Notification.DEFAULT_ALL);
        numNotificationMessages = 0;
		
		// create list of messages - all messages old and new are going to be added here
		messages = new ArrayList<BeamsterMessage>();
		closedChannels = new Hashtable<String,ArrayList<BeamsterMessage>>();
		people = new ArrayList<BeamsterRosterItem>();
		
		// start a handler the runs every minute to update dates in the list view
		MessageUpdateHandler.getHandler(ChatActivity.this);	
		
		// Create the LocationRequest object
        mLocationRequest = LocationRequest.create();
        // Use high accuracy
        mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
        // Set the update interval to 5 seconds
        mLocationRequest.setInterval(UPDATE_INTERVAL);
        // Set the fastest update interval to 1 second
        mLocationRequest.setFastestInterval(FASTEST_INTERVAL);
        
        // this causes location on the map to be find way too late
        //mLocationRequest.setSmallestDisplacement(SMALLES_DISPLACEMENT_METERS);		
        
        /*
         * Create a new location client, using the enclosing class to
         * handle callbacks.
         */
        mLocationClient = new LocationClient(this, this, this);        
                
    }
	
    /*
     * Called when the Activity becomes visible.
     */
    @Override
    protected void onStart() {
        super.onStart();

        // Connect the client.
        mLocationClient.connect();
    }	
	    
	/**
	 * To add a message to a private channel tab
	 * 
	 * @param userName
	 * @param name
	 */
	public void addMessageToTab(ArrayList<BeamsterMessage> closedChannel,
			BeamsterMessage fBeamsterMessage, String jid) {

		// just to make sure we check if not null
		if (closedChannel != null) {
			// before we add a message (regardless if composing or not), check
			// if we have a composing message of this username in the list
			ListIterator<BeamsterMessage> messagesIt = closedChannel
					.listIterator(closedChannel.size());
			while (messagesIt.hasPrevious()) {
				BeamsterMessage aMessage = messagesIt.previous();
				if (aMessage.getUsername() != null
						&& aMessage.getUsername().equals(jid)
						&& aMessage.isComposing()) {
					// remove this message
					closedChannel.remove(aMessage);
					break;
				}
			}

        	closedChannel.add(fBeamsterMessage);    			

			// add message to list
			FragmentList fragmentList = (FragmentList) getFragmentManager()
					.findFragmentByTag(jid);
			if (fragmentList != null) {
				if (!fragmentList.isVisible()
						&& !fBeamsterMessage.isComposing()) {
					// change title +1
					updateTitle(fBeamsterMessage.getUsername());
				}
				fragmentList.updateMessagesList(true);

			} else {
				// update title+1 if tab has not been created yet
				updateTitle(fBeamsterMessage.getUsername());
			}

		}
	}
    
    
	/**
	 * To add a private channel tab
	 * @param userName
	 * @param name
	 */
	public void addTab(String userName, final String name, final String pictureUrl, final boolean select, final boolean createdBySelf)
	{
		// peopleDialog
		if (peopleDialog!=null && peopleDialog.isVisible())
		{
			peopleDialog.dismiss();
			peopleDialog = null;
		}
		
		// first check if tab already exists
		boolean found = false;
		int num = bar.getTabCount();
		for (int i = 2; i<num; i++)
		{
			if (bar.getTabAt(i).getTag()!=null && bar.getTabAt(i).getTag().equals(userName))
			{
				found = true;
		        break;
			}
		}
				
		// create new privat channel
		if (!found)
		{
			// Supply num input as an argument.
		    Bundle args = new Bundle();
		    args.putString("jid", userName);
		    args.putString("name", name);
		    args.putBoolean("closedChannel", true);
			
		    // now initialize the message list
		    ArrayList<BeamsterMessage> myBeamsterMessageList = null;
		    if (!closedChannels.containsKey(userName))
		    	closedChannels.put(userName, myBeamsterMessageList = new ArrayList<BeamsterMessage>());
		    else
		    	myBeamsterMessageList = closedChannels.get(userName);
		    
		    BeamsterMessage myBeamsterMessage = null;
		    if (createdBySelf)
		    	myBeamsterMessage = new BeamsterMessage(getString(R.string.new_private_channel, name), true);
		    else
		    	myBeamsterMessage = new BeamsterMessage(getString(R.string.user_initiated_private_channel, name), true);
		    	
			myBeamsterMessage.setStatusMessage(true);
			myBeamsterMessage.setPictureurl(pictureUrl);

			// add it to list of messages
			myBeamsterMessageList.add(myBeamsterMessage);					
		    		    
			final Tab newTab = bar.newTab()
	        .setText(name)
	        .setTag(userName)
	        .setTabListener(new TabListener<FragmentList>(this, userName, FragmentList.class, true, args));
						
			this.runOnUiThread(new Runnable() {
			    public void run() {
			        bar.addTab(newTab);	
			        
			        if (select)
			        	bar.selectTab(newTab);
			    }
			});			
			
		}
	}
	
	/**
	 * To update a channel tab when a message comes in and the tab is not visible
	 * @param userName
	 * @param name
	 */
	public void updateTitle(String userName)
	{
		// first check if tab already exists
		int num = bar.getTabCount();
		for (int i = 0; i<num; i++)
		{
			if (bar.getTabAt(i).getTag()!=null && bar.getTabAt(i).getTag().equals(userName))
			{
				final Tab myTab = bar.getTabAt(i);
				this.runOnUiThread(new Runnable() {
				    public void run() {
				    	String text = (String)myTab.getText();
				    	int a=0, b=0, num=0;
				    	if ((a=text.indexOf("("))>-1)
				    	{
					    	if ((b=text.indexOf(")", a+1))>-1)
					    	{
					    		num = Integer.parseInt(text.substring(a+1,b));
					    		text = text.substring(b+2); // take rest of string after space
					    	}
				    	}
			    		num++;

						myTab.setText("("+num+") "+text);
				    }
				});	
				
			}
			
		}				
	}
	
	/**
	 * To update a channel tab when a message comes in and the tab is not visible
	 * @param userName
	 * @param name
	 */
	public void updateTitleList()
	{
		final Tab myTab = bar.getTabAt(0);
		this.runOnUiThread(new Runnable() {
		    public void run() {
		    	String text = (String)myTab.getText();
		    	int a=0, b=0, num=0;
		    	if ((a=text.indexOf("("))>-1)
		    	{
			    	if ((b=text.indexOf(")", a+1))>-1)
			    	{
			    		num = Integer.parseInt(text.substring(a+1,b));
			    		text = text.substring(b+2); // take rest of string after space
			    	}
		    	}
	    		num++;

				myTab.setText("("+num+") "+text);
		    }
		});	
	}	
	
	/**
	 * clear the numbers in the title when tab becomes visible
	 * @param userName
	 * @param name
	 */
	public void clearTitleNotification(String userName)
	{
		// first check if tab already exists
		int num = bar.getTabCount();
		for (int i = 0; i<num; i++)
		{
			if (bar.getTabAt(i).getTag()!=null && bar.getTabAt(i).getTag().equals(userName))
			{
				final Tab myTab = bar.getTabAt(i);
				this.runOnUiThread(new Runnable() {
				    public void run() {
				    	String text = (String)myTab.getText();
				    	int b=0;
				    	if ((b=text.indexOf(")"))>-1)
				    	{
				    		text = text.substring(b+2); // take rest of string after space
							myTab.setText(text);
				    	}
				    }
				});	
				
			}
			
		}				
	}	
	
	/**
	 * To add a private channel tab
	 * @param userName
	 * @param name
	 */
	public void removeTab(final Tab tab)
	{
    	// Add Dialog
		DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
		    @Override
		    public void onClick(DialogInterface dialog, int which) {
		        switch (which){
		        case DialogInterface.BUTTON_POSITIVE:
		            //Yes button clicked
		            bar.removeTab(tab);
		            break;

		        case DialogInterface.BUTTON_NEGATIVE:
		            //No button clicked
		            break;
		        }
		    }
		};		        	
    	
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage("Close this private/working channel?")
		    .setNegativeButton(R.string.no, dialogClickListener)
		    .setPositiveButton(R.string.yes, dialogClickListener).show();
	}	
	        
  	@Override
  	public boolean onCreateOptionsMenu(Menu menu) {
  		getMenuInflater().inflate(R.menu.chat_activity_menu, menu);
  		
  		try
  		{
	        // enable translation menu, if new language has been detected
	        String isoL = Locale.getDefault().getISO3Language();     
	        Log.i("BEAMSTER", "XXXXXXXXXXXXXX isoL: "+isoL);
	        Set<String> languages = new HashSet<String>(Arrays.asList(AppConfig.translatedLanguages));
	        if (!languages.contains(isoL))
	        {
	    		MenuItem item = menu.findItem(R.id.help_translate);  
	    		item.setVisible(true);
	    		//invalidateOptionsMenu(); // thisabled this pbre20140417
	        }
	        else
	        {
	    		MenuItem item = menu.findItem(R.id.help_translate);  
	    		item.setVisible(false);
	    		//invalidateOptionsMenu(); // thisabled this pbre20140417
	        }
  		}
  		catch (Exception e)
  		{
  			try
  			{
  				((AppConfig)getApplication()).trackException(300, e);
			}
			catch (Exception e2)
			{
    	        Log.e("BEAMSTER", "Failed to report exception 300"+e2);	    			            		    				
			}

			Log.e("BEAMSTER", "onCreateOptionsMenu: "+e);  			
  		}
        
  	    return true;
  	}
  	
  	private class ConnectTask extends AsyncTask<String, Void, String> {

		@Override
  	    protected String doInBackground(String... urls) {

  			long start = System.currentTimeMillis();
  			// Create a connection
  			try {
  				myPacketListener = new MyPacketListener();
  				
  				
  				BeamsterAPI.getInstance().connect(myPacketListener);
  				
  				
  			} catch (Exception e) {
  				try
  				{
  					((AppConfig)getApplication()).trackException(2, e);
    			}
    			catch (Exception e2)
    			{
	    	        Log.e("BEAMSTER", "Failed to report exception 2"+e2);	    			            		    				
    			}

  				Log.e("BEAMSTER", (System.currentTimeMillis()-start)+"ChatActivity.connect: "+e);
  			
  				Intent intent = new Intent(ChatActivity.this, LoginActivity.class);
  				intent.putExtra("com.beamster.error", e.getMessage());
  				intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP); 
  				intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
  				startActivity(intent);	    				  				
  			}
  	    	
  	    	return "";
  	    }

  	    @Override
  	    protected void onPostExecute(String result) {
			Log.d("BEAMSTER", "onPostExecute ...");
  	    }
  	}  	
  	
    class MyPacketListener implements BeamsterPacketListener {

		@Override
		public void processPacket(Packet packet) 
		{
			if (packet instanceof Message && ((Message) packet).getType()==Message.Type.headline) 
			{				
	            Log.d("BEAMSTER","MyPacketListener Headline Message: "+packet.toXML());
	            
				Message message = (Message) packet;
    			BeamsterMessage myBeamsterMessage = new BeamsterMessage(message.getBody(), true);
    			myBeamsterMessage.setStatusMessage(true);

				// add it to list of messages
	           	messages.add(myBeamsterMessage);									
	           	
				// add message to list
				FragmentList fragmentList = (FragmentList)getFragmentManager().findFragmentByTag("list");
				if (fragmentList!=null)
					fragmentList.updateMessagesList(true);
	           	
			}
			else if (packet instanceof Message && ((Message) packet).getType()==Message.Type.chat) 
			{
	            Log.d("BEAMSTER","MyPacketListener Message: "+packet.toXML());
	            
				Message message = (Message) packet;
								
				ChatStateExtension composing = (ChatStateExtension)message.getExtension("composing", "http://jabber.org/protocol/chatstates");
				ChatStateExtension active = (ChatStateExtension)message.getExtension("active", "http://jabber.org/protocol/chatstates");
				
				if (composing!=null)
				{
					// display composing message
					Log.d("BEAMSTER", "composing message received ...");
				}
				else if (active!=null)
				{
					// display real message
					Log.d("BEAMSTER", "active message received ...");
				}
				
				BeamsterMessage myBeamsterMessage = null;
				// check if text message
				if (message.getBody() != null) {
					String fromName = StringUtils.parseBareAddress(message.getFrom());
					Log.i("BEAMSTER", "Text Received " + message.getBody()+ " from " + fromName );

					// create new messaging object
					myBeamsterMessage = new BeamsterMessage((composing==null?message.getBody():getString(R.string.isTyping)), false);
				}
				// check if composing
				else if (composing!=null)
				{
					String composingType = (String)message.getProperty("composingType");
					if (composingType==null)
					{
						myBeamsterMessage = new BeamsterMessage(getString(R.string.isTyping), false);						
					}
					else if (composingType.equals(BeamsterAPI.COMPOSING_TYPE_TYPING))
					{
						myBeamsterMessage = new BeamsterMessage(getString(R.string.isTyping), false);						
					}
					else if (composingType.equals(BeamsterAPI.COMPOSING_TYPE_SPEAKING))
					{
						myBeamsterMessage = new BeamsterMessage(getString(R.string.isSpeaking), false);						
					}					

					// create new messaging object
					myBeamsterMessage.setComposing(true);
				}
				else
				{
					Log.e("BEAMSTER", "Invalid message type...");
					return;
				}
				
				String username = (String)message.getProperty("username");
									
				String pictureUrl = (String)message.getProperty("pictureUrl");
				if (pictureUrl!=null)
					myBeamsterMessage.setPictureurl(pictureUrl);
					
				myBeamsterMessage.setBeamed((String)message.getProperty("beamed"));
        		myBeamsterMessage.setBearing((String)message.getProperty("bearing"));
        		
    			// find this one from the roster and display message to user
        		boolean found = false;
        		Iterator<BeamsterRosterItem> peopleIterator = people.iterator();
        		while (peopleIterator.hasNext())
        		{
        			BeamsterRosterItem myBeamsterRosterItem = peopleIterator.next();
        			if (username.equals(myBeamsterRosterItem.getJid()))
        			{
                		myBeamsterMessage.setClientId(myBeamsterRosterItem.getClientId());
                		try
                		{
                			myBeamsterMessage.setDistanceAway(Double.parseDouble(myBeamsterRosterItem.getDistanceAway()));
        					//Log.e("BEAMSTER", "Error parsing distance away...");
                		}
                		catch (Exception e)
                		{
                			try
                			{
                				((AppConfig)getApplication()).trackException(3, e);
    		    			}
    		    			catch (Exception e2)
    		    			{
        		    	        Log.e("BEAMSTER", "Failed to report exception 3"+e2);	    			            		    				
    		    			}

                			myBeamsterMessage.setDistanceAway(0);                			
                		}
                		found = true;
                		break;
        			}
        		}    
        		
        		if (!found)
        		{
        			// calculate distance
        			
        		}
        		
        		try
        		{
            		myBeamsterMessage.setLat(String.valueOf(message.getProperty("lat")));
            		myBeamsterMessage.setLon(String.valueOf(message.getProperty("lon")));
            		myBeamsterMessage.setMessagedate(Calendar.getInstance());
            		myBeamsterMessage.setName((String)message.getProperty("name"));
            		myBeamsterMessage.setSpeed((String)message.getProperty("speed"));
            		myBeamsterMessage.setUsername(username);     
            		
            		if (myBeamsterMessage.isAudio() && !myBeamsterMessage.isComposing())
            			myBeamsterMessage.setStillNeedsToBePlayed(true);
        		}
        		catch (Exception e)
        		{
        			try
        			{
        				((AppConfig)getApplication()).trackException(4, e);        			
	    			}
	    			catch (Exception e2)
	    			{
		    	        Log.e("BEAMSTER", "Failed to report exception 4"+e2);	    			            		    				
	    			}
        				
					Log.e("BEAMSTER", "Error parsing message properties "+e.getMessage());        			
        		}

        		// now check from to decide if private or public message
        		if (message.getFrom().startsWith("chat@"))
        		{
		           	// before we add a message (regardless if composing or not), check if we have a composing message of this username in the list
	        		ListIterator<BeamsterMessage> messagesIt = messages.listIterator(messages.size());
	        		while(messagesIt.hasPrevious()) 
	        		{
	        			BeamsterMessage aMessage = messagesIt.previous();
	        			if (aMessage.getUsername()!=null 
	        					&& aMessage.getUsername().equals(username) 
	        					&& aMessage.isComposing())
	        			{
	        				// remove this message
	    					messages.remove(aMessage);
	    					break;
	        			}
	        		}        			  
	        		
					// add it to list of messages only if there was no composing before and 
					messages.add(myBeamsterMessage);		

					// add message to list
					FragmentList fragmentList = (FragmentList)getFragmentManager().findFragmentByTag("list");
					if (fragmentList!=null)
					{					
						if (!fragmentList.isVisible() && !myBeamsterMessage.isComposing())
						{
							// change title +1
							updateTitleList();
						}
					
						fragmentList.updateMessagesList(true);
					}

					FragmentMap fragmentMap = (FragmentMap)getFragmentManager().findFragmentByTag("map");
					if (fragmentMap!=null)
					{
						if (myBeamsterMessage.isAudio())
						{
							fragmentMap.updateMessagesMap(true, myBeamsterMessage.getUsername(), myBeamsterMessage.getName(), getString(R.string.sent_a_voice_message));						
						}
						else
						{
							fragmentMap.updateMessagesMap(true, myBeamsterMessage.getUsername(), myBeamsterMessage.getName(), myBeamsterMessage.getMessage());													
						}
					}
	        		
        		}
        		else
        		{
        			// check if it is a private message
					final int k = message.getFrom().indexOf("@");
					if (k > -1) {
						// determine jid from message
						final String jid = message.getFrom().substring(0, k);

						// first check if tab already exists
						ArrayList<BeamsterMessage> closedChannel = closedChannels
								.get(jid);
						if (closedChannel != null) {
							boolean foundTab = false;
							int num = bar.getTabCount();
							for (int i = 2; i < num; i++) {
								if (bar.getTabAt(i).getTag() != null
										&& bar.getTabAt(i).getTag().equals(jid)) {
									foundTab = true;
									break;
								}
							}
							if (!foundTab) {
								final BeamsterMessage fBeamsterMessage = myBeamsterMessage;
								addTab(jid, fBeamsterMessage.getName(),
										fBeamsterMessage.getPictureurl(),
										false, false);
							}

							addMessageToTab(closedChannel, myBeamsterMessage,
									jid);
						} else {
							// its a private message
							final BeamsterMessage fBeamsterMessage = myBeamsterMessage;
							final AlertDialog.Builder builder1 = new AlertDialog.Builder(
									ChatActivity.this);
							builder1.setTitle(R.string.user_initiated_private_channel_titel);
							builder1.setMessage(getString(
									R.string.user_initiated_private_channel,
									myBeamsterMessage.getName()));
							builder1.setCancelable(false);
							builder1.setNeutralButton(android.R.string.ok,
									new DialogInterface.OnClickListener() {

										public void onClick(
												DialogInterface dialog, int id) {
											dialog.cancel();
											privateChannelDialog = null;

											addTab(jid, fBeamsterMessage
													.getName(),
													fBeamsterMessage
															.getPictureurl(),
													false, false);

											ArrayList<BeamsterMessage> closedChannel = closedChannels
													.get(jid);

											addMessageToTab(closedChannel,
													fBeamsterMessage, jid);
										}
									});

							try {
								runOnUiThread(new Runnable() {
									public void run() {
										if (privateChannelDialog == null) {
											privateChannelDialog = builder1
													.create();
											privateChannelDialog.show();
										}
									}
								});
							} catch (Exception e) {
								Log.e("BEAMSTER", e.getMessage());
							}
						} // end else foundTab

					}

        			        			
        		}
        		        		
				// notification
        		Log.d("BEAMSTER", "Checking notifications: doNotify: "+doNotify+" composing: "+composing+" "+(doNotify && composing==null));
				if (doNotify && composing==null) // composing==null means we are considering only real messages, not the composing messages
				{
	        		Log.d("BEAMSTER", "Building notification message ... "+myBeamsterMessage.getMessage());
					
					// creating and sending a notification
					Intent intent = new Intent(ChatActivity.this, NotificationActivity.class);
					PendingIntent pIntent = PendingIntent.getActivity(ChatActivity.this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

			        // Constructs the Builder object.
			        builder.setContentText(myBeamsterMessage.getMessage())
			        .setContentTitle(getString(R.string.notification_title)+" "+myBeamsterMessage.getName())
			        .setNumber(++numNotificationMessages)					
			        .setAutoCancel(true);

			        builder.setContentIntent(pIntent);

			        // Including the notification ID allows you to update the notification later on.
			        mNotificationManager.notify(016, builder.build());
			        
			        // pebble
			        if (pebbleNotificationEnabled)
			        {
			        	// send only if screen is on
			        	PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
			        	boolean isScreenOn = pm.isScreenOn();
			        	if (!isScreenOn)
			        	{
				        	// check fist if pebble is connected
							boolean connected = PebbleKit.isWatchConnected(getApplicationContext());
							if (connected)
							{
								if (myBeamsterMessage.isAudio())
								{
									Utils.sendAlertToPebble(ChatActivity.this, getString(R.string.pebble_notifications_title_from)+" "+myBeamsterMessage.getName(), getString(R.string.sent_a_voice_message));																
								}
								else
								{
									Utils.sendAlertToPebble(ChatActivity.this, getString(R.string.pebble_notifications_title_from)+" "+myBeamsterMessage.getName(), myBeamsterMessage.getMessage());																									
								}
							}			        		
			        	}			        	
			        }
			        
				}
								
				
			} 
			else if (packet instanceof Presence) 
	        {
				try
				{
		            Presence myPresence = (Presence)packet;
		            Iterator<PacketExtension> it = myPresence.getExtensions().iterator();
		            // loop through PacketExtensions
		            while (it.hasNext())
		            {
		            	BeamsterRosterPresence myBeamsterRosterPresence = (BeamsterRosterPresence)it.next();
		            	List<BeamsterRosterItem> list = myBeamsterRosterPresence.getItems();
		            	for (BeamsterRosterItem item : list)
		            	{
		            		if (item.getStatus().equals(BeamsterAPI.PRESENCE_STATUS_LOGGED_OUT))
		            		{
		            			// remove this one from the roster and display message to user
			            		Iterator<BeamsterRosterItem> peopleIterator = people.iterator();
			            		while (peopleIterator.hasNext())
			            		{
			            			BeamsterRosterItem myBeamsterRosterItem = peopleIterator.next();
			            			if (item.getJid().equals(myBeamsterRosterItem.getJid()))
			            			{
			            				people.remove(myBeamsterRosterItem);
			            				
										// add message to list
										FragmentList fragmentList = (FragmentList)getFragmentManager().findFragmentByTag("list");
										if (fragmentList!=null)
											fragmentList.updateMessagesList(true);
										
										// add message to map
										FragmentMap fragmentMap = (FragmentMap)getFragmentManager().findFragmentByTag("map");
										if (fragmentMap!=null)
											fragmentMap.updateMessagesMap(myBeamsterRosterItem, "remove");
			            							            					            							            				
				            			BeamsterMessage myBeamsterMessage = new BeamsterMessage(getString(R.string.user_x_has_logged_out, myBeamsterRosterItem.getName()), true);
				            			myBeamsterMessage.setStatusMessage(true);
				        				String pictureUrl = (String)item.getPictureUrl();
				        				if (pictureUrl!=null)
				        					myBeamsterMessage.setPictureurl(pictureUrl);

										// add it to list of messages
							           	messages.add(myBeamsterMessage);					
						            			            											
				            			break;
			            			} // end if			            						            			
			            		} // end while
		            		}
		            		else if (item.getStatus().equals(BeamsterAPI.PRESENCE_STATUS_USER_MOVE_OUT_OF_SIGHT))
		            		{
		            			// remove this one from the roster and display message to user
			            		Iterator<BeamsterRosterItem> peopleIterator = people.iterator();
			            		while (peopleIterator.hasNext())
			            		{
			            			BeamsterRosterItem myBeamsterRosterItem = peopleIterator.next();
			            			if (item.getJid().equals(myBeamsterRosterItem.getJid()))
			            			{
			            				people.remove(myBeamsterRosterItem);
			            				
				            			BeamsterMessage myBeamsterMessage = new BeamsterMessage(getString(R.string.user_x_moved_out_of_sight, myBeamsterRosterItem.getName()), true);
				            			myBeamsterMessage.setStatusMessage(true);
				        				String pictureUrl = (String)item.getPictureUrl();
				        				if (pictureUrl!=null)
				        					myBeamsterMessage.setPictureurl(pictureUrl);

										// add it to list of messages
							           	messages.add(myBeamsterMessage);					
						            			            	
										// add message to list
										FragmentList fragmentList = (FragmentList)getFragmentManager().findFragmentByTag("list");
										if (fragmentList!=null)
											fragmentList.updateMessagesList(true);
														
										// add message to map
										FragmentMap fragmentMap = (FragmentMap)getFragmentManager().findFragmentByTag("map");
										if (fragmentMap!=null)
											fragmentMap.updateMessagesMap(myBeamsterRosterItem, "update");
										
				            			break;
			            			} // end if			            						            			
			            		} // end while
		            		}
		            		else if (item.getStatus().equals(BeamsterAPI.PRESENCE_STATUS_MOVE_IN_SIGHT))
		            		{
		            			// add this one to roster and display message to user
		            			// remove this one from the roster and display message to user
		            			boolean found = false;
			            		Iterator<BeamsterRosterItem> peopleIterator = people.iterator();
			            		while (peopleIterator.hasNext())
			            		{
			            			BeamsterRosterItem myBeamsterRosterItem = peopleIterator.next();
			            			if (item.getJid().equals(myBeamsterRosterItem.getJid()))
			            			{
				            			// update the data
				            			myBeamsterRosterItem.copyData(item);

				            			// in case it was already in, remove
			            				found = true;
			            			}
			            		}

			            		if (!found)
			            		{
				            		// add person again
			            			people.add(item);			            			
			            		}
		            		
		            			BeamsterMessage myBeamsterMessage = new BeamsterMessage(getString(R.string.user_x_came_in_sight, item.getName()), true);
		            			myBeamsterMessage.setStatusMessage(true);
		        				String pictureUrl = (String)item.getPictureUrl();
		        				if (pictureUrl!=null)
		        					myBeamsterMessage.setPictureurl(pictureUrl);

								// add it to list of messages
					           	messages.add(myBeamsterMessage);					
				            			            	
								// add message to list
								FragmentList fragmentList = (FragmentList)getFragmentManager().findFragmentByTag("list");
								if (fragmentList!=null)
									fragmentList.updateMessagesList(true);
								
								// add message to map
								FragmentMap fragmentMap = (FragmentMap)getFragmentManager().findFragmentByTag("map");
								if (fragmentMap!=null)
									fragmentMap.updateMessagesMap(item, "add");
								
								// notification
				        		Log.d("BEAMSTER", "Checking notifications: doNotify for presence "+doNotify);
				        		if (doNotify) // active means we are considering only real messages, not the composing messages
								{
					        		Log.d("BEAMSTER", "Building notification message ... "+myBeamsterMessage.getMessage());
									
									// creating and sending a notification
									Intent intent = new Intent(ChatActivity.this, NotificationActivity.class);
									PendingIntent pIntent = PendingIntent.getActivity(ChatActivity.this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

							        // Constructs the Builder object.
							        builder.setContentText(myBeamsterMessage.getMessage())
							        .setContentTitle(getString(R.string.notification_title)+" "+myBeamsterMessage.getName())
							        .setNumber(++numNotificationMessages)					
							        .setAutoCancel(true);

							        builder.setContentIntent(pIntent);

							        // Including the notification ID allows you to update the notification later on.
							        mNotificationManager.notify(016, builder.build());							        							        
								}																
								
			            		
		            		} 
		            		else if (item.getStatus().equals(BeamsterAPI.PRESENCE_STATUS_USER_UPDATED))
		            		{
		            			// add this one to roster and display message to user
		            			// remove this one from the roster and display message to user
		            			boolean found = false;
			            		Iterator<BeamsterRosterItem> peopleIterator = people.iterator();
			            		BeamsterRosterItem myBeamsterRosterItem = null;
			            		while (peopleIterator.hasNext())
			            		{
			            			myBeamsterRosterItem = peopleIterator.next();
			            			if (item.getJid().equals(myBeamsterRosterItem.getJid()))
			            			{
				            			// update the data
				            			myBeamsterRosterItem.copyData(item);

				            			// in case it was already in, remove
			            				found = true;
			            			}
			            		}

			            		if (!found)
			            		{
				            		// add person again
			            			people.add(myBeamsterRosterItem);			            			
			            		}
			            		
			            		// display message to user
			            		/* pbre20140401 do not report simple status updates to the user.
		            			BeamsterMessage myBeamsterMessage = new BeamsterMessage(getString(R.string.user_x_updated_status, item.getName()), true);
		            			myBeamsterMessage.setStatusMessage(true);
		        				String pictureUrl = (String)item.getPictureUrl();
		        				if (pictureUrl!=null)
		        					myBeamsterMessage.setPictureurl(pictureUrl);

								// add it to list of messages
					           	messages.add(myBeamsterMessage);					
				            			            	
								// add message to list
								FragmentList fragmentList = (FragmentList)getFragmentManager().findFragmentByTag("list");
								if (fragmentList!=null)
									fragmentList.updateMessagesList(true);
								
								// add message to map
								FragmentMap fragmentMap = (FragmentMap)getFragmentManager().findFragmentByTag("map");
								if (fragmentMap!=null)
									fragmentMap.updateMessagesMap(myBeamsterRosterItem, "update");
								*/
		            		}
		            	}
		            }	        								
				}
				catch (Exception e)
				{
					try
					{
						((AppConfig)getApplication()).trackException(5, e);					
	    			}
	    			catch (Exception e2)
	    			{
		    	        Log.e("BEAMSTER", "Failed to report exception 5"+e2);	    			            		    				
	    			}
		            Log.e("BEAMSTER","(packet instanceof Presence): "+packet+" Error: "+e.getMessage());					
				}
	        } else if (packet instanceof IQ) {
	        	// 11b
	            Log.d("BEAMSTER","MyPacketListener IQ: "+packet.getPacketID()+" XML: "+packet.toXML());
				
	            if (packet.getPacketID().equals("get_messages"))
	            {
		            Log.d("BEAMSTER","handle IQ: get_messages: "+packet.getPacketID());

		            try
	            	{
	            		if (dialog!=null)
	              			dialog.dismiss();		
	            		
	            		// each time we reload the messages, clear the ArrayList
	            		messages.clear();

		            	BeamsterGetMessagesIQ myBeamsterGetMessagesIQ = ((BeamsterGetMessagesIQ) packet);
		            	List<BeamsterMessageItem> myItems = myBeamsterGetMessagesIQ.getItems();
		            	
		            	for (BeamsterMessageItem item : myItems)
		            	{
			            	try
			            	{
			            		BeamsterMessage myBeamsterMessage = new BeamsterMessage(item.getMessage(), (BeamsterAPI.getInstance().getUsername().equals(item.getUsername())));
			            		myBeamsterMessage.setBeamed(item.getBeamed());
			            		myBeamsterMessage.setBearing(item.getBearing());
			            		myBeamsterMessage.setClientId(item.getClientId());
			            		try
			            		{
				            		myBeamsterMessage.setDistanceAway(Double.parseDouble(item.getDistanceAway()));
			            		}
			            		catch (Exception e)
			            		{
			            			try
			            			{
			            				((AppConfig)getApplication()).trackException(6, e);			            			
            		    			}
            		    			catch (Exception e2)
            		    			{
                		    	        Log.e("BEAMSTER", "Failed to report exception 6"+e2);	    			            		    				
            		    			}
				            		myBeamsterMessage.setDistanceAway(0);
			            			Log.e("BEAMSTER", "Parse Error: "+item.getDistanceAway()+": "+e.getMessage());
			            		}
			            		myBeamsterMessage.setLat(item.getLat());
			            		myBeamsterMessage.setLon(item.getLon());
			            		try
			            		{
			            			myBeamsterMessage.setMessagedate(ISO8601.toCalendar(item.getMessagedate()));
			            		}
			            		catch (Exception e)
			            		{
			            			try
			            			{
			            				((AppConfig)getApplication()).trackException(7, e);			            			
            		    			}
            		    			catch (Exception e2)
            		    			{
                		    	        Log.e("BEAMSTER", "Failed to report exception 7"+e2);	    			            		    				
            		    			}
			            			myBeamsterMessage.setMessagedate(Calendar.getInstance());
			            			Log.e("BEAMSTER", "Parse Error: "+item.getMessagedate()+": "+e.getMessage());
			            		}
			            		myBeamsterMessage.setName(item.getName());
			            		myBeamsterMessage.setPictureurl(item.getPictureurl());
			            		myBeamsterMessage.setSpeed(item.getSpeed());
			            		myBeamsterMessage.setUsername(item.getUsername());
			            		
				        		messages.add(myBeamsterMessage);		            		
			            	}
			            	catch (Exception e)
			            	{
			            		try
			            		{
			            			((AppConfig)getApplication()).trackException(8, e);
        		    			}
        		    			catch (Exception e2)
        		    			{
            		    	        Log.e("BEAMSTER", "Failed to report exception 8"+e2);	    			            		    				
        		    			}

			    	            Log.d("BEAMSTER","Error loading item: "+item);	            		
			            	}
		            	}
		            	
						// create new status message
		            	BeamsterMessage myBeamsterMessage = getStatusMessage();
		            	if (myBeamsterMessage!=null)
		            	{
	    					myBeamsterMessage.setPictureurl(myBeamsterUserProfile.getPictureUrl());

							// add it to list of messages
				           	messages.add(myBeamsterMessage);							            		
		            	}
		            			            	
						// add message to list
						FragmentList fragmentList = (FragmentList)getFragmentManager().findFragmentByTag("list");
						if (fragmentList!=null)
							fragmentList.updateMessagesList(true);
	            		
	            	}
	            	catch (Exception e)
	            	{
	            		try
	            		{
	            			((AppConfig)getApplication()).trackException(9, e);
		    			}
		    			catch (Exception e2)
		    			{
    		    	        Log.e("BEAMSTER", "Failed to report exception 9"+e2);	    			            		    				
		    			}
	    	            Log.d("BEAMSTER","Error receiving BeamsterGetMessagesIQ: "+e.getMessage());	            		
	            	}
	            }
	            else if (packet.getPacketID().equals("get_roster"))
	            {
		            Log.d("BEAMSTER","handle IQ: get_roster: "+packet.getPacketID());
	            	
	            	try
	            	{
		            	BeamsterGetRosterIQ myBeamsterGetRosterIQ = ((BeamsterGetRosterIQ) packet);
	            		people = (ArrayList<BeamsterRosterItem>)myBeamsterGetRosterIQ.getItems();
	            		
        	            Log.d("BEAMSTER","Now updating the map ..."); 	
	            		
	    				// this basically clears the map and draws users again on it
	    				FragmentMap fragmentMap = (FragmentMap)getFragmentManager().findFragmentByTag("map");
	    				if (fragmentMap!=null)
	    					fragmentMap.updateMessagesMap(null, "update");
	    					            		
	            	}
	            	catch (Exception e)
	            	{
	            		try
	            		{
	            			((AppConfig)getApplication()).trackException(10, e);
		    			}
		    			catch (Exception e2)
		    			{
    		    	        Log.e("BEAMSTER", "Failed to report exception 10"+e2);	    			            		    				
		    			}
	    	            Log.d("BEAMSTER","Error receiving get_roster: "+e.getMessage());	            		
	            	}
	            }
	            else if (packet.getPacketID().equals("get_profile"))
	            {
    	            Log.d("BEAMSTER","IQ get_profile - Not yet implemented");	            		
	            }	            
	            else if (packet.getPacketID().equals("set_profile"))
	            {
    	            Log.d("BEAMSTER","IQ set_profile - Not yet implemented");	   
	            }	            
	            else if (packet.getPacketID().equals("set_location"))
	            {
    	            Log.d("BEAMSTER","IQ set_location - get roster, last messages");	  
    	            Log.d("BEAMSTER","IQ currentCenter: "+mCurrentLocation);	  
    	            Log.d("BEAMSTER","IQ currentCenterLastUpdateRoster: "+mCurrentCenterLastUpdateRoster);	  
    	            Log.d("BEAMSTER","IQ profileSaved: "+profileSaved);	  

    	            // just to debug
    	            /*
    	            if (currentCenterLastUpdateRoster!=null)
    	            {
        	            Log.d("BEAMSTER","IQ computing distance: "+Utils.distance(currentCenterLastUpdateRoster.latitude, currentCenterLastUpdateRoster.longitude, currentCenter.latitude, currentCenter.longitude)); 	
        	            ChatActivity.this.runOnUiThread(new Runnable() {
        				    public void run() {
                	            Toast.makeText(getApplicationContext(), "IQ computing distance: "+Utils.distance(currentCenterLastUpdateRoster.latitude, currentCenterLastUpdateRoster.longitude, currentCenter.latitude, currentCenter.longitude), Toast.LENGTH_LONG).show();   
        				    }
        				});	
    	            }*/

    	            // do update only if we moved by at least 1000m
    	            // after the location has been stored at the server, we retrieve the roster and get Messages, but only initially (if currentCenterLastUpdateRoster==null, or if we moved by at least 100m)
    	            if (mCurrentCenterLastUpdateRoster==null || Utils.distance(mCurrentCenterLastUpdateRoster.getLatitude(), mCurrentCenterLastUpdateRoster.getLongitude(), mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude())>1000
    	            	|| profileSaved)
    	            {
	    	            Log.d("BEAMSTER","Calling roster and messages update ...");	            		

	    	            mCurrentCenterLastUpdateRoster = mCurrentLocation;  // remember the initial location here

        	            // get roster
        				BeamsterAPI.getInstance().getRoster(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude());					

        				// get all latest messages
        				BeamsterAPI.getInstance().getLastMessages(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude());							    							    	            	
        				
        				// set to false such that we dont do the reload at next location change
    	            	profileSaved = false;
    	            }
    	            else
    	            {
        	            Log.d("BEAMSTER","Updating the map with new location of self..."+packet); 	
        	            BeamsterSetLocationIQ myBeamsterSetLocationIQ = ((BeamsterSetLocationIQ) packet);
	            		List<BeamsterRosterItem> meList = (ArrayList<BeamsterRosterItem>)myBeamsterSetLocationIQ.getItems();
	            		
	            		// there is only one item in this list
	            		BeamsterRosterItem me = meList.get(0);
	            		if (me!=null)
	            		{
		            		if (people!=null)
		            		{
		            			Iterator<BeamsterRosterItem> peopleIterator = people.iterator();
		            			BeamsterRosterItem myBeamsterRosterItem = null;
		            			while (peopleIterator.hasNext())
		            			{
		            				myBeamsterRosterItem = peopleIterator.next();
		            				if (myBeamsterUserProfile!=null && myBeamsterUserProfile.getUserName().equals(myBeamsterRosterItem.getJid()))
		            				{
		            					myBeamsterRosterItem.setLat(""+me.getLat());
		            					myBeamsterRosterItem.setLon(""+me.getLon());		            					
		            				}
		            			}			
		            		}	            			
	            		}
   	            	
	    				// this basically clears the map and draws users again on it
	    				FragmentMap fragmentMap = (FragmentMap)getFragmentManager().findFragmentByTag("map");
	    				if (fragmentMap!=null)
	    					fragmentMap.updateMessagesMap(null, "update");
    	            }

	            } // end if packet.getPacketID()   
			} // end if (packet instanceof))		
		} // end processPacket
        
    }	
  	
    public void updateFragments(String fragmentName)
    {
		// add message to list
    	if (fragmentName!=null && fragmentName.equals("list"))
    	{
			// add message to list
			FragmentList fragmentList = (FragmentList)getFragmentManager().findFragmentByTag("list");
			if (fragmentList!=null)
				fragmentList.updateMessagesList(true);
    	}
    	else if (fragmentName!=null && fragmentName.equals("map"))
    	{
			// add message to map
			FragmentMap fragmentMap = (FragmentMap)getFragmentManager().findFragmentByTag("map");
			if (fragmentMap!=null)
				// add message to list
				fragmentMap.updateMessagesMap(true, getMyBeamsterUserProfile().getUserName(), getMyBeamsterUserProfile().getName(), getString(R.string.sentAudioMessage));
    	}
    	else if (fragmentName!=null)
    	{
			// add message to list
			FragmentList fragmentList = (FragmentList)getFragmentManager().findFragmentByTag(fragmentName);
			if (fragmentList!=null)
				fragmentList.updateMessagesList(true);
    	}
    	// TODO add cam
    }
    
    
    
    public boolean isProfileSaved() {
		return profileSaved;
	}

	public void setProfileSaved(boolean profileSaved) {
		ChatActivity.profileSaved = profileSaved;
	}

	/**
     * This function displays a status message after a user logged in or changed profile 
     */
  	private BeamsterMessage getStatusMessage()
  	{
  		String message = "";
  		String messageAboutPeopleinTheArea = "";
  		/*
        var otherUsersNum = contacts.length-1;
        
        if (otherUsersNum<0)
        	otherUsersNum = 0;
        
        if (otherUsersNum == 0)
        {
        	userMessage = l("js.thereareno", formatDistance(Gab.radius, Gab.radiusUnit))+" "+l("js.trytoincrease", "<a href=\"#options\" data-rel=\"dialog\" data-position-to=\"window\" data-transition=\"pop\">"+l("options.privacysettings")+"</a>");  
        }
        else if (otherUsersNum == 1)
    	{
        	userMessage = l('js.thereisone', formatDistance(Gab.radius, Gab.radiusUnit))+l("js.trytoincrease", "<a href=\"#options\" data-rel=\"dialog\" data-position-to=\"window\" data-transition=\"pop\">"+l("options.privacysettings")+"</a>");          	
    	}
        else
    	{
        	userMessage = l('js.therearemore', "<a href=\"#onlineusers\" data-rel=\"dialog\">"+otherUsersNum+" users</a>", formatDistance(Gab.radius, Gab.radiusUnit));              	        	
    	} */           

  		// TODO decode address here
  		
        if (loginMessage)
    	{
        	message = getString(R.string.welcome)+" ";
            // set to false not to display login message again
            loginMessage = false;
    	}        

        if (beamed)
    	{
        	message = message+String.format(getString(R.string.beamed), String.format("%.2f", mCurrentLocation.getLatitude())+"/"+String.format("%.2f", mCurrentLocation.getLongitude()))+messageAboutPeopleinTheArea+" "+getString(R.string.starttyping, Utils.getFormattedDistance(ChatActivity.myBeamsterUserProfile.getRadius(), ChatActivity.myBeamsterUserProfile.getRadiusUnit()));
    	}
        else
    	{
        	message = message+getString(R.string.trueloc, String.format("%.2f", mCurrentLocation.getLatitude())+"/"+String.format("%.2f", mCurrentLocation.getLongitude()))+" "+getString(R.string.starttyping, Utils.getFormattedDistance(ChatActivity.myBeamsterUserProfile.getRadius(), ChatActivity.myBeamsterUserProfile.getRadiusUnit()));            	
    	}
        
        // set to false not to display login message again
        loginMessage = false;
        
		BeamsterMessage myBeamsterMessage = new BeamsterMessage(message, true);
		myBeamsterMessage.setStatusMessage(true);
		return myBeamsterMessage;         		            
  		
  	}
    
      	
    public BeamsterUserProfile getMyBeamsterUserProfile() {
		return myBeamsterUserProfile;
	}

	public void setMyBeamsterUserProfile(BeamsterUserProfile myBeamsterUserProfile) {
		ChatActivity.myBeamsterUserProfile = myBeamsterUserProfile;
	}

	public boolean isBeamed() {
		return beamed;
	}

	public void setBeamed(boolean beamed) {
		ChatActivity.beamed = beamed;
	}

	public Location getCurrentCenter() {
		return mCurrentLocation;
	}

	public void setCurrentCenter(Location currentCenter) {
		ChatActivity.mCurrentLocation = currentCenter;
	}

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("tab", getActionBar().getSelectedNavigationIndex());
    }

    public static class TabListener<T extends Fragment> implements ActionBar.TabListener {
        private final Activity mActivity;
        private final String mTag;
        private final Class<T> mClass;
        private final Bundle mArgs;
        private Fragment mFragment;
        private boolean closedChannel = false;

        public boolean isPrivatChat() {
			return closedChannel;
		}

		public void setPrivatChat(boolean privatChat) {
			this.closedChannel = privatChat;
		}

		public TabListener(Activity activity, String tag, Class<T> clz) {
            this(activity, tag, clz, null);
        }
		
		public TabListener(Activity activity, String tag, Class<T> clz, boolean closedChannel) {
            this(activity, tag, clz, null);
            this.closedChannel = closedChannel;
        }		

		public TabListener(Activity activity, String tag, Class<T> clz, boolean closedChannel, Bundle args) {
            this(activity, tag, clz, args);
            this.closedChannel = closedChannel;
        }		

		public TabListener(Activity activity, String tag, Class<T> clz, Bundle args) {
            mActivity = activity;
            mTag = tag;
            mClass = clz;
            mArgs = args;

            // Check to see if we already have a fragment for this tab, probably
            // from a previously saved state.  If so, deactivate it, because our
            // initial state is that a tab isn't shown.
            mFragment = mActivity.getFragmentManager().findFragmentByTag(mTag);
            if (mFragment != null && !mFragment.isDetached()) {
                FragmentTransaction ft = mActivity.getFragmentManager().beginTransaction();
                ft.detach(mFragment);
                ft.commit();
            }
        }

        public void onTabSelected(Tab tab, FragmentTransaction ft) {
            if (mFragment == null) {
                mFragment = Fragment.instantiate(mActivity, mClass.getName(), mArgs);
                ft.add(android.R.id.content, mFragment, mTag);            
            } else {
                ft.attach(mFragment);
                
                // clear the numbers in case there are any (1) ...
		    	String text = (String)tab.getText();
		    	int b=0;
		    	if ((b=text.indexOf(")"))>-1)
		    	{
		    		text = text.substring(b+2); // take rest of string after space
		    		tab.setText(text);
		    	}
            }
        }

        public void onTabUnselected(Tab tab, FragmentTransaction ft) {
            if (mFragment != null) {
                ft.detach(mFragment);
            }
        }

        public void onTabReselected(Tab tab, FragmentTransaction ft) {
        	if (closedChannel)
        	{
        		((ChatActivity)mActivity).removeTab(tab);
        	}
        }
        
        
    }

	public ArrayList<BeamsterMessage> getMessages() {
		return messages;
	}

	public void setMessages(ArrayList<BeamsterMessage> messages) {
		ChatActivity.messages = messages;
	}

	public ArrayList<BeamsterMessage> getMessages(String jid) {
		if (closedChannels!=null)
		{
			return closedChannels.get(jid);			
		}	
		else
		{
			return null; // this should not happen
		}
			
	}

	
	@Override
	protected void onResume() {
		super.onResume();
		doNotify = false;
		
		// set this counter to 0 again; this is the number of messages displayed in the notification bar
		numNotificationMessages= 0;
		
		Log.d("BEAMSTER", "ChatActivity Resume ... doNotify: "+doNotify);
		MessageUpdateHandler.resumeMyHandler();
	}

	@Override
	protected void onPause() {
		super.onPause();
		doNotify = true;

		try
		{
			// every time we pause, we check if pebble notification is enabled
			SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);
			pebbleNotificationEnabled = sharedPref.getBoolean("me.channel16.pebbleNotificationEnabled."+ChatActivity.myBeamsterUserProfile.getUserName() , false);				
		}
		catch (Exception e)
		{
			try
			{
				((AppConfig)getApplication()).trackException(11, e);
			}
			catch (Exception e2)
			{
    	        Log.e("BEAMSTER", "Failed to report exception 11"+e2);	    			            		    				
			}

			Log.e("BEAMSTER", "Exception when trying to access sharedpred: "+e.getMessage());			
		}
		
		Log.d("BEAMSTER", "ChatActivity Pause ... doNotify: "+doNotify);
		MessageUpdateHandler.pauseMyHandler();
	}

	@Override
	protected void onStop() {
		
		// If the client is connected
        if (mLocationClient.isConnected()) {
            /*
             * Remove location updates for a listener.
             * The current Activity is the listener, so
             * the argument is "this".
             */
        	if (LOCATION_UPDATES && mLocationClient!=null)
        		mLocationClient.removeLocationUpdates(this);
        	
            /*
             * After disconnect() is called, the client is
             * considered "dead".
             */
            mLocationClient.disconnect();        
        	
        }
        
		super.onStop();
		Log.d("BEAMSTER", "ChatActivity Stop ...");
		MessageUpdateHandler.stopMyHandler();
	}
	
	@Override
	public void onBackPressed() {
		// do something on back.
		Log.d("BEAMSTER", "back pressed ...");
		
		DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
		    @Override
		    public void onClick(DialogInterface dialog, int which) {
		        switch (which){
		        case DialogInterface.BUTTON_POSITIVE:
		            //Yes button clicked
		    		signOut();
		            break;

		        case DialogInterface.BUTTON_NEGATIVE:
		            //No button clicked
		            break;
		        }
		    }
		};

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(R.string.signOutDialog).setPositiveButton(R.string.yes, dialogClickListener)
		    .setNegativeButton(R.string.no, dialogClickListener).show();
		return;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    // Handle item selection
    	FragmentManager fm = getFragmentManager();

	    switch (item.getItemId()) {
	        case R.id.action_signoff:
	    		DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
	    		    @Override
	    		    public void onClick(DialogInterface dialog, int which) {
	    		        switch (which){
	    		        case DialogInterface.BUTTON_POSITIVE:
	    		            //Yes button clicked
	    		    		signOut();
	    		            break;

	    		        case DialogInterface.BUTTON_NEGATIVE:
	    		            //No button clicked
	    		            break;
	    		        }
	    		    }
	    		};

	    		AlertDialog.Builder builder = new AlertDialog.Builder(this);
	    		builder.setMessage(R.string.signOutDialog)
	    		    .setNegativeButton(R.string.no, dialogClickListener)
	    		    .setPositiveButton(R.string.yes, dialogClickListener).show();
	        	
	            return true;
	        case R.id.accounts:
	        	AccountsDialogFragment accountsDialog = new AccountsDialogFragment(ChatActivity.myBeamsterUserProfile.getUserName());
	        	accountsDialog.setRetainInstance(true);
	        	accountsDialog.show(fm, "accounts");

	            return true;
	        case R.id.action_settings:
	            SettingsDialogFragment optionsDialog = new SettingsDialogFragment();
	            optionsDialog.setRetainInstance(true);
	            optionsDialog.show(fm, "options");

	            return true;
	        case R.id.people_in_your_area:
	        		
	            peopleDialog = new PeopleDialogFragment();
	            peopleDialog.setRetainInstance(false);
	            peopleDialog.show(fm, "people_in_the_area");
	            return true;
	        case R.id.pebble_smartwatch:
        		
	            PebbleDialogFragment pebbleDialog = new PebbleDialogFragment();
	            pebbleDialog.setRetainInstance(true);
	            pebbleDialog.show(fm, "pebble");
	        	
	            return true;	            
	        case R.id.about:
        		
	            AboutDialogFragment aboutDialog = new AboutDialogFragment();
	            aboutDialog.setRetainInstance(true);
	            aboutDialog.show(fm, "about");
	        	
	            return true;	            

	        case R.id.help_translate:
        		
	        	HelpTranslateDialogFragment helpTranslate = new HelpTranslateDialogFragment();
	        	helpTranslate.setRetainInstance(true);
	        	helpTranslate.show(fm, "help_translate");
	        	
	            return true;	            
	        
	        default:
	            aboutDialog = new AboutDialogFragment();
	            aboutDialog.setRetainInstance(true);
	            aboutDialog.show(fm, "about");
	        	
	            return true;	            
	    }
	}

	/**
	 * Signs a current user out and move back to MainActivity
	 */
	public void signOut()
	{
        Tracker t = ((AppConfig)getApplication()).getTracker(
	            TrackerName.APP_TRACKER);
        // Build and send an Event.
        t.send(new HitBuilders.EventBuilder()
            .setCategory("SignOut")
            .setAction("SignOutAccount") // actionId
            .setLabel(myBeamsterUserProfile.getIdType()) 
            .build());	
		
		final ProgressDialog dialog = ProgressDialog.show(this, getString(R.string.disconnecting), getString(R.string.please_wait), false);

		Thread thread = new Thread(new Runnable() {

			@Override
			public void run() {

				try
				{
					BeamsterAPI.getInstance().sendPresence(Type.unavailable);
					BeamsterAPI.getInstance().destroy();
							    						    				
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						try
						{
							((AppConfig)getApplication()).trackException(12, e);
		    			}
		    			catch (Exception e2)
		    			{
			    	        Log.e("BEAMSTER", "Failed to report exception 12"+e2);	    			            		    				
		    			}
	
						Log.d("BEAMSTER", "ChatActivity onOptionsItemSelected InterruptedException: "+e.getMessage());
					}
					dialog.dismiss();		
					
					runOnUiThread(new Runnable() {
			            @Override
			            public void run() {
			            	
			            	ChatActivity.this.finish();     	
			            	
			            	// go to the home activity
			            	Intent intent = new Intent(Intent.ACTION_MAIN);
			            	intent.addCategory(Intent.CATEGORY_HOME);
			            	intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			            	startActivity(intent);		            	
			            	
			            }
			        });		
				}
				catch (Exception e)
				{
					try
					{
						((AppConfig)getApplication()).trackException(1739, e);
	    			}
	    			catch (Exception e2)
	    			{
		    	        Log.e("BEAMSTER", "Failed to report exception 1739"+e2);	    			            		    				
	    			}

					Log.d("BEAMSTER", "ChatActivity onOptionsItemSelected InterruptedException: "+e.getMessage());					
				}
			}
		});
		thread.start();
		dialog.show();		
	}
    
	public ArrayList<BeamsterRosterItem> getPeople() {
		return people;
	}

	public void setTasks(ArrayList<BeamsterRosterItem> people) {
		ChatActivity.people = people;
	}	
	
	public int getSelectedPerson() {
		return selectedPerson;
	}

	public void setSelectedPerson(int selectedPerson) {
		ChatActivity.selectedPerson = selectedPerson;
	}

	@Override
	public void onCompletion(MediaPlayer mp) {
		if (buttonPlay!=null)
			buttonPlay.setImageResource(R.drawable.btn_play);

		// check if there is another message to be played
		ArrayList<BeamsterMessage> messages = getMessages();
		Iterator<BeamsterMessage> myMessagesIt = messages.iterator();
		while (myMessagesIt.hasNext())
		{
			BeamsterMessage message = myMessagesIt.next();
			if (message.isStillNeedsToBePlayed())
			{
				// Resume song						
				playSong(message, message.getPlayButton(), message.getSeekBar(), message.getCurrentDurationLabel(), message.getTotalDurationLabel(), message.getMessage());
				break;
			}
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (mp!=null)
			mp.release();
	}		

	/**
	 * Function to play a song
	 * @param songIndex - index of song
	 * */
	public void  playSong(BeamsterMessage message, ImageButton playButton, SeekBar songProgressBar, TextView currentDurationLabel, TextView totalDurationLabel, String path){
		
		// Play song
		if (mp!=null && mp.isPlaying())
			return;
		message.setStillNeedsToBePlayed(false);
		
		// set global play button
		buttonPlay = playButton;
		
		try {
			if (mp!=null)
			{
	        	mp.reset();
				mp.setDataSource(path);
				mp.prepare();
				mp.start();				
			}
			
        	// Changing Button Image to pause image
			playButton.setImageResource(R.drawable.btn_pause);
			
			// set Progress bar values
			songProgressBar.setProgress(0);
			songProgressBar.setMax(100);
			
			// Updating progress bar
			updateProgressBar(songProgressBar, currentDurationLabel, totalDurationLabel);
		} catch (IllegalArgumentException e) {
			try
			{
				((AppConfig)getApplication()).trackException(13, e);
			}
			catch (Exception e2)
			{
    	        Log.e("BEAMSTER", "Failed to report exception 13"+e2);	    			            		    				
			}

	        Log.e("BEAMSTER", "IllegalArgumentException 13"+e);	    			            		    				
		} catch (IllegalStateException e) {
			try
			{
		    	((AppConfig)getApplication()).trackException(14, e);
			}
			catch (Exception e2)
			{
		        Log.e("BEAMSTER", "Failed to report exception 13"+e2);	    			            		    				
			}
	        Log.e("BEAMSTER", "IllegalStateException 14"+e);	    			            		    				
		} catch (IOException e) {
			try
			{
				((AppConfig)getApplication()).trackException(15, e);
			}
			catch (Exception e2)
			{
		        Log.e("BEAMSTER", "Failed to report exception 13"+e2);	    			            		    				
			}
	        Log.e("BEAMSTER", "IllegalStateException 15"+e);	    			            		    				
		}
	}
	
	/**
	 * Update timer on seekbar
	 * */
	public void updateProgressBar(SeekBar songProgressBar, TextView currentDurationLabel, TextView totalDurationLabel) {
        mHandler.postDelayed(new UpdateTimeTask(songProgressBar, currentDurationLabel, totalDurationLabel), 100);        
    }	
		
	class UpdateTimeTask implements Runnable {

		SeekBar songProgressBar = null;
		TextView currentDurationLabel = null, totalDurationLabel = null;
		
		public UpdateTimeTask(SeekBar songProgressBar, TextView currentDurationLabel, TextView totalDurationLabel) 
		{
			// store parameter for later user
			this.songProgressBar = songProgressBar;
			this.currentDurationLabel = currentDurationLabel;
			this.totalDurationLabel = totalDurationLabel;
		}

	   public void run() {
		   long totalDuration = mp.getDuration();
		   long currentDuration = mp.getCurrentPosition();
		  
		   // Displaying Total Duration time
		   totalDurationLabel.setText(""+utils.milliSecondsToTimer(totalDuration));
		   // Displaying time completed playing
		   currentDurationLabel.setText(""+utils.milliSecondsToTimer(currentDuration));
		   
		   // Updating progress bar
		   int progress = (int)(utils.getProgressPercentage(currentDuration, totalDuration));

		   //Log.d("Progress", ""+progress);
		   songProgressBar.setProgress(progress);
		   Log.d("BEAMSTER", "isPLaying: "+mp.isPlaying());
		   
		   // Running this thread after 100 milliseconds
		   if (mp.isPlaying())
			   mHandler.postDelayed(this, 100);
	   }
	}




	@Override
	public void onConnectionFailed(ConnectionResult connectionResult) {
        /*
         * Google Play services can resolve some errors it detects.
         * If the error has a resolution, try sending an Intent to
         * start a Google Play services activity that can resolve
         * error.
         */
        if (connectionResult.hasResolution()) {
            try {
                // Start an Activity that tries to resolve the error
                connectionResult.startResolutionForResult(
                        this,
                        CONNECTION_FAILURE_RESOLUTION_REQUEST);
                /*
                * Thrown if Google Play services canceled the original
                * PendingIntent
                */
            } catch (IntentSender.SendIntentException e) {
            	try
            	{
            		((AppConfig)getApplication()).trackException(16, e);
    			}
    			catch (Exception e2)
    			{
        	        Log.e("BEAMSTER", "Failed to report exception 16"+e2);	    			            		    				
    			}

                // Log the error
    	        Log.e("BEAMSTER", "IntentSender.SendIntentException 16"+e);	    			            		    				
            }
        } else {
            /*
             * If no resolution is available, display a dialog to the
             * user with the error.
             */
            showErrorDialog(connectionResult.getErrorCode());
        }
	}

	void showErrorDialog(int code) {
		  GooglePlayServicesUtil.getErrorDialog(code, this, 
		      REQUEST_CODE_RECOVER_PLAY_SERVICES).show();
	}	
	
	@Override
	public void onConnected(Bundle arg0) {
		try {

			Log.d("BEAMSTER", "onConnected LocationService ... has already Beamster Connection: "+BeamsterAPI.getInstance().isConnected());
		
			if (BeamsterAPI.getInstance().isConnected())
			{
				Log.d("BEAMSTER", "in onConnected LocationService is already connected to xmpp server...");
				return;					
			}
			
			// Get Location Manager and check for GPS & Network location services
			LocationManager lm = (LocationManager) getSystemService(LOCATION_SERVICE);
			Log.d("BEAMSTER", "GPS_PROVIDER enabled?: "+lm.isProviderEnabled(LocationManager.GPS_PROVIDER));
			Log.d("BEAMSTER", "NETWORK_PROVIDER enabled?: "+lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER));
				
			if(!lm.isProviderEnabled(LocationManager.GPS_PROVIDER) &&
	          !lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) 
			{
				// Build the alert dialog
				AlertDialog.Builder builder = new AlertDialog.Builder(ChatActivity.this);
			    builder.setTitle(R.string.gpssettings);
			    builder.setMessage(R.string.gpsnotanabled);
			    builder.setPositiveButton("OK", new DialogInterface.OnClickListener() 
			    {
			    	public void onClick(DialogInterface dialogInterface, int i) 
			    	{
				        // Show location settings when the user acknowledges the alert dialog
				        Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
				        startActivity(intent);
			    	}
			    });
		      Dialog alertDialog = builder.create();
		      alertDialog.setCanceledOnTouchOutside(false);
		      alertDialog.show();	
			}
			else
			{			
		    	mCurrentLocation = mLocationClient.getLastLocation();
				Log.d("BEAMSTER", "getLastLocation - mCurrentLocation: "+mCurrentLocation);
	
		    	// Mediaplayer
				mp = new MediaPlayer();
				utils = new Utilities();	
				mp.setOnCompletionListener(ChatActivity.this); // Important        
		    	
		    	if (mCurrentLocation!=null)
		    	{
					ConnectTask task = new ConnectTask();
				    task.execute();	
		    	}
	
		    	// start location updates
		    	if (LOCATION_UPDATES && mLocationClient!=null)
		    		mLocationClient.requestLocationUpdates(mLocationRequest, this);
			}
			
		} catch (Exception e1) {
	  		Log.d("BEAMSTER", "connectFailed ..."+e1);
	  		try
	  		{
				Intent intent = new Intent(ChatActivity.this, LoginActivity.class);
				intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP); 
				intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				startActivity(intent);	    	          	
	  		}
	  		catch (Exception e2)
	  		{
				try
				{
					((AppConfig)getApplication()).trackException(2043, e2);
    			}
    			catch (Exception e3)
    			{
	    	        Log.e("BEAMSTER", "Failed to report exception 2043"+e3);	    			            		    				
    			}	  			
	  		}
		}			
	}

	@Override
	public void onDisconnected() {
	}

	@Override
	public void onLocationChanged(Location location) {
		
		if (mCurrentLocation==null)				
		{
			ConnectTask task = new ConnectTask();
			task.execute();	
		}
		
		if (mapLocationListener != null) {
		      mapLocationListener.onLocationChanged(location);
		}
		
        // Report to the UI that the location was updated		
        String msg = "Updated Location: " +
                Double.toString(location.getLatitude()) + "," +
                Double.toString(location.getLongitude());
        //Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
        
		mCurrentLocation = location; 
		
		// find the user in roster and update location
		/*
		if (people!=null)
		{
			Iterator<BeamsterRosterItem> peopleIterator = people.iterator();
			BeamsterRosterItem myBeamsterRosterItem = null;
			while (peopleIterator.hasNext())
			{
				myBeamsterRosterItem = peopleIterator.next();
				if (this.myBeamsterUserProfile!=null && this.myBeamsterUserProfile.getUserName().equals(myBeamsterRosterItem.getJid()))
				{
					myBeamsterRosterItem.setLat(""+location.getLatitude());
					myBeamsterRosterItem.setLon(""+location.getLongitude());
					
					// this basically clears the map and draws users again on it
					FragmentMap fragmentMap = (FragmentMap)getFragmentManager().findFragmentByTag("map");
					if (fragmentMap!=null)
						fragmentMap.updateMessagesMap(myBeamsterRosterItem, "update");				
				}
			}			
		}*/
		
		// save a location on server
		BeamsterAPI.getInstance().setLocation(location.getLatitude(), 
				location.getLongitude(),
				location.getBearing(),
				location.getSpeed(),
				beamed);			
     }

	@Override
	  public void activate(OnLocationChangedListener listener) {
	    ChatActivity.mapLocationListener=listener;
	    
		Log.d("BEAMSTER", "SETTING LOCATION TO LOCATION LISTENER ..."+mapLocationListener+" mCurrentLocation: "+mCurrentLocation);
		if (mapLocationListener != null && mCurrentLocation!=null) {
		      mapLocationListener.onLocationChanged(mCurrentLocation);
		}	    
	  }

	  @Override
	  public void deactivate() {
	    ChatActivity.mapLocationListener=null;
	  }
	  
	  /**
	   * This callback is called by the BeamsterAPI after all users have been loaded
	   */
	  private BeamsterAPI.ConnectionErrorCallback myConnectionErrorCallback = new BeamsterAPI.ConnectionErrorCallback()
	  {

		  	@Override
			public void authenticated(XMPPConnection arg0) {
		  		Log.d("BEAMSTER", "authenticated ...");
		  		
			}

			@Override
			public void connected(XMPPConnection con) {

				Log.d("BEAMSTER", "xmpp connected ...");

  				try {
					BeamsterAPI.getInstance().performLogin();

		  	    	 // Get Location Manager and check for GPS & Network location services
		  	        LocationManager lm = (LocationManager) getSystemService(LOCATION_SERVICE);
					Log.d("BEAMSTER", "GPS_PROVIDER enabled?: "+lm.isProviderEnabled(LocationManager.GPS_PROVIDER));
					Log.d("BEAMSTER", "NETWORK_PROVIDER enabled?: "+lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER));  	        
		  	        
		  	        if(!lm.isProviderEnabled(LocationManager.GPS_PROVIDER) &&
		  	              !lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
		  	          // Build the alert dialog
		  	          AlertDialog.Builder builder = new AlertDialog.Builder(ChatActivity.this);
		  	          builder.setTitle(R.string.gpssettings);
		  	          builder.setMessage(R.string.gpsnotanabled);
		  	          builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
		  	        	  public void onClick(DialogInterface dialogInterface, int i) 
			  	          {
			  	            // Show location settings when the user acknowledges the alert dialog
			  	            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
			  	            startActivity(intent);
			  	          }
		  	          });
		  	          Dialog alertDialog = builder.create();
		  	          alertDialog.setCanceledOnTouchOutside(false);
		  	          alertDialog.show();
		  	        }        
		  	        else
		  	        {
		  	        	try
		  	        	{
			  	        	if (mLocationClient!=null)
			  	        	{
			  			    	mCurrentLocation = mLocationClient.getLastLocation();
			  	        	}
		  	        	}
		  	        	catch (Exception e)
		  	        	{
		        			try
		        			{
		        				((AppConfig)getApplication()).trackException(580, e);
		      	        		Log.d("BEAMSTER", "Exception580: "+e.getMessage());
			    			}
			    			catch (Exception e2)
			    			{
				    	        Log.e("BEAMSTER", "Failed to report exception 580"+e2);	    			            		    				
			    			}
		  	        		
		  	        	}

		  	        	try
		  	        	{			  	        		
			  	        	if (mCurrentLocation!=null)
			  	        	{	
					  	        // re-init this
				  	        	mCurrentCenterLastUpdateRoster = null;		  	  		        
					  	        	
								// save a location on server
								BeamsterAPI.getInstance().setLocation(mCurrentLocation.getLatitude(), 
										mCurrentLocation.getLongitude(),
										mCurrentLocation.getBearing(),
										mCurrentLocation.getSpeed(),
										beamed);		
			  	        	}
		  	        	}
		  	        	catch (Exception e)
		  	        	{
		        			try
		        			{
		        				((AppConfig)getApplication()).trackException(580, e);
		      	        		Log.d("BEAMSTER", "Exception581: "+e.getMessage());
			    			}
			    			catch (Exception e2)
			    			{
				    	        Log.e("BEAMSTER", "Failed to report exception 581"+e2);	    			            		    				
			    			}
		  	        		
		  	        	}
		  	        }		  		
					
				} catch (BeamsterSecurityException e1) {
			  		Log.d("BEAMSTER", "loginFailed ..."+e1);
		            Toast.makeText(getApplicationContext(), getString(R.string.error_invalid_password), Toast.LENGTH_LONG).show();   
			  		try
			  		{
	    				Intent intent = new Intent(ChatActivity.this, LoginActivity.class);
	    				intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP); 
	    				intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
	    				startActivity(intent);	    	          	
			  		}
			  		catch (Exception e2)
			  		{
						try
						{
							((AppConfig)getApplication()).trackException(2002, e2);
		    			}
		    			catch (Exception e3)
		    			{
			    	        Log.e("BEAMSTER", "Failed to report exception 2002"+e3);	    			            		    				
		    			}	  			
			  		}
				}
		  		
			}

			@Override
			public void connectionClosed() {
		  		Log.d("BEAMSTER", "connectionClosed ...");
			}

			@Override
			public void connectionClosedOnError(Exception ex) {
		  		Log.d("BEAMSTER", "connectionClosedOnError ..."+ex+" ChatActivity.reconnecting: "+ChatActivity.reconnecting+" check1: "+("org.jivesoftware.smack.SmackException$NotConnectedException".equals(ex.toString())));
		  		if(!ChatActivity.reconnecting && ("org.jivesoftware.smack.SmackException$NotConnectedException".equals(ex.toString())))
		  		{
			  		Log.d("BEAMSTER", "going to login...");
			  		try
			  		{
	    				Intent intent = new Intent(ChatActivity.this, LoginActivity.class);
	    				intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP); 
	    				intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
	    				startActivity(intent);	    	          			  	
			  		}
			  		catch (Exception e)
			  		{
						try
						{
							((AppConfig)getApplication()).trackException(2001, e);
		    			}
		    			catch (Exception e2)
		    			{
			    	        Log.e("BEAMSTER", "Failed to report exception 2001"+e2);	    			            		    				
		    			}	  			
			  		}		  			
		  		}
			}

			@Override
			public void reconnectingIn(int arg0) {
		  		Log.d("BEAMSTER", "reconnectingIn ..."+arg0);
		  		ChatActivity.reconnecting = true;
			}

			@Override
			public void reconnectionFailed(Exception e) {
		  		Log.d("BEAMSTER", "reconnectionFailed ..."+e);
		  		ChatActivity.reconnecting = false;
		  		try
		  		{
    				Intent intent = new Intent(ChatActivity.this, LoginActivity.class);
    				intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP); 
    				intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    				startActivity(intent);	    	          	
		  		}
		  		catch (Exception e2)
		  		{
					try
					{
						((AppConfig)getApplication()).trackException(2002, e2);
	    			}
	    			catch (Exception e3)
	    			{
		    	        Log.e("BEAMSTER", "Failed to report exception 2002"+e3);	    			            		    				
	    			}	  			
		  		}
			}

			@Override
			public void reconnectionSuccessful() {
		  		ChatActivity.reconnecting = false;
		  		Log.d("BEAMSTER", "reconnectionSuccessful ...");
			}		

	    
	};	
	
	static String privateJid = null;
	static String launchedFrom = null;
	
	// this is set if the message to be sent  
	public void selectImage(String jid, String launchedFromLocal) {

		// remember jid (in case its a private message, jid is set) 
		privateJid = jid;
		launchedFrom = launchedFromLocal;
		
		final CharSequence[] options = { getString(R.string.take_photo), getString(R.string.choose_from_gallery), getString(R.string.button_cancel) };

		AlertDialog.Builder builder = new AlertDialog.Builder(ChatActivity.this);
		builder.setTitle(R.string.send_a_photo);
		builder.setItems(options, new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialogChooser, int item) {
				if (options[item].equals(getString(R.string.take_photo)))
				{
					Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
					File f = new File(android.os.Environment.getExternalStorageDirectory(), "temp.jpg");
					intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(f));
					startActivityForResult(intent, 1);
				}
				else if (options[item].equals(getString(R.string.choose_from_gallery)))
				{
		            // in onCreate or any event where your want the user to
		            // select a file
					Intent intent = new Intent(Intent.ACTION_GET_CONTENT, null);
					intent.setType("image/*");
		            startActivityForResult(Intent.createChooser(intent,
		                    getString(R.string.send_a_photo)), 2);					
				}
				else if (options[item].equals(getString(R.string.button_cancel))) 
				{
					dialogChooser.dismiss();
				}
			}
		});

		builder.show();
	} // selectImage
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {

		super.onActivityResult(requestCode, resultCode, data);
 		if (resultCode == RESULT_OK) 
		{
 			this.dialog = ProgressDialog.show(ChatActivity.this, getString(R.string.send), getString(R.string.please_wait), false);
			if (requestCode == 1) 
			{
				File f = new File(Environment.getExternalStorageDirectory()
						.toString());

				for (File temp : f.listFiles()) 
				{
					if (temp.getName().equals("temp.jpg")) 
					{
						f = temp;
						break;
					}
				}

				try 
				{
					Bitmap bitmap;
					BitmapFactory.Options bitmapOptions = new BitmapFactory.Options();
					bitmap = BitmapFactory.decodeFile(f.getAbsolutePath(),
					bitmapOptions);

					ExifInterface exif = new ExifInterface(f.getAbsolutePath());
					int exifOrientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);

			        int rotate = 0;
			        switch (exifOrientation) 
			        {
			          	case ExifInterface.ORIENTATION_ROTATE_90:
			          		rotate = 90;
			          		break; 

			          	case ExifInterface.ORIENTATION_ROTATE_180:
			          		rotate = 180;
			          		break;

			          	case ExifInterface.ORIENTATION_ROTATE_270:
			          		rotate = 270;
			          		break;
			        }

			        if (rotate != 0) 
			        {
			        	int w = bitmap.getWidth();
			        	int h = bitmap.getHeight();

			        	// Setting pre rotate
			        	Matrix mtx = new Matrix();
			        	mtx.preRotate(rotate);

			        	// Rotating Bitmap & convert to ARGB_8888, required by tess
			        	bitmap = Bitmap.createBitmap(bitmap, 0, 0, w, h, mtx, false);
			        	bitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);					
			        }					
					
			        String jid = privateJid;
					privateJid = null;
					sendPhoto(bitmap, jid, launchedFrom);
					
					// Add some parameters to the image that will be stored in the Image ContentProvider
					int UNIQUE_BUCKET_ID = 998;
					ContentValues values = new ContentValues(7);
					long num = System.currentTimeMillis();
					values.put(MediaStore.Images.Media.DISPLAY_NAME,"Channel16.me_photo_"+num+".jpg");
					values.put(MediaStore.Images.Media.TITLE,"Channel16.me_photo_"+num+".jpg");
					values.put(MediaStore.Images.Media.DESCRIPTION, "Channel16.me photo");
					values.put(MediaStore.Images.Media.BUCKET_DISPLAY_NAME,"Channel16.me"); 
					values.put(MediaStore.Images.Media.BUCKET_ID,UNIQUE_BUCKET_ID);
					values.put(MediaStore.Images.Media.DATE_TAKEN,System.currentTimeMillis());
					values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");

					// Inserting the image meta data inside the content provider
					Uri uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

					// Filling the real data returned by the picture callback function into the content provider
					try {
					    OutputStream outStream = getContentResolver().openOutputStream(uri);
						bitmap.compress(Bitmap.CompressFormat.JPEG, 85, outStream);
						outStream.flush();
						outStream.close();
					}catch (Exception e) {
					    Log.e("BEAMSTER", "Exception while writing image", e);
						try
						{
							((AppConfig)getApplication()).trackException(2463, e);
		    			}
		    			catch (Exception e2)
		    			{
			    	        Log.e("BEAMSTER", "Failed to report exception 2463"+e2);	    			            		    				
		    			}	
					}	
				} 
				catch (Exception e) 
				{
					try
					{
						((AppConfig)getApplication()).trackException(2475, e);
	    			}
	    			catch (Exception e2)
	    			{
		    	        Log.e("BEAMSTER", "Failed to report exception 2475"+e2);	    			            		    				
	    			}	
				}

			} 
			else if (requestCode == 2) 
			{
				Uri selectedImageUri = data.getData();
                String selectedImagePath = getPath(selectedImageUri);				
				if (selectedImagePath!=null)
				{
	                Bitmap bitmap = BitmapFactory.decodeFile(selectedImagePath);
					sendPhoto(bitmap, privateJid, launchedFrom);						
				}		                
				else
				{
					new LoadImage(privateJid, launchedFrom).execute(selectedImageUri);										
				}
			}
		}
	} // end onActivityResult
	
	 /**
     * helper to retrieve the path of an image URI
     */
    public String getPath(Uri uri) {
            // just some safety built in 
            if( uri == null ) {
                // TODO perform some logging or show user feedback
                return null;
            }
            // try to retrieve the image from the media store first
            // this will only work for images selected from gallery
            String[] projection = { MediaStore.Images.Media.DATA };
            //Cursor cursor = managedQuery(uri, projection, null, null, null); // deprecated
            Cursor cursor = getContentResolver().query(uri, projection, null, null, null);
            if( cursor != null ){
                int column_index = cursor
                .getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                cursor.moveToFirst();
                return cursor.getString(column_index);
            }
            // this is our fallback here
            return uri.getPath();
    }

	private class LoadImage extends AsyncTask<Uri, Void, Bitmap> {

		String privateJid = null;
		String launchedFrom = null;
        public LoadImage(String jid, String launchedFrom) {
			super();
			this.privateJid = jid;
			this.launchedFrom = launchedFrom;
		}

		@Override
        protected Bitmap doInBackground(Uri... selectedImageUri) {
	        ParcelFileDescriptor parcelFileDescriptor;
	        Bitmap image = null;
			try {
				parcelFileDescriptor = getContentResolver().openFileDescriptor(selectedImageUri[0], "r");
		        FileDescriptor fileDescriptor = parcelFileDescriptor.getFileDescriptor();
		        image = BitmapFactory.decodeFileDescriptor(fileDescriptor);
		        parcelFileDescriptor.close();
			} catch (FileNotFoundException e) {
				Log.e("BEAMSTER", "Load image failed: "+e.getMessage());
				try
				{
					((AppConfig)getApplication()).trackException(2532, e);
    			}
    			catch (Exception e3)
    			{
	    	        Log.e("BEAMSTER", "Failed to report exception 2532"+e3);	    			            		    				
    			}	  			

			} catch (IOException e) {
				try
				{
					((AppConfig)getApplication()).trackException(2542, e);
    			}
    			catch (Exception e3)
    			{
	    	        Log.e("BEAMSTER", "Failed to report exception 2542"+e3);	    			            		    				
    			}	  			
			}
	        return image;
        }

        @Override
        protected void onPostExecute(Bitmap image) {
			sendPhoto(image, this.privateJid, launchedFrom);	
		}

        @Override
        protected void onPreExecute() {
        }

        @Override
        protected void onProgressUpdate(Void... values) {
        }
    }			
	
	protected void sendPhoto(Bitmap myPhoto, String privateJid, String launchedFrom)
	{
		Log.i("BEAMSTER", "sendPhoto: "+myPhoto.getWidth()+" "+myPhoto.getHeight());
		
		// 1) resize image
		float wh = ((float)myPhoto.getHeight()/((float)myPhoto.getWidth())); 
		int bitmapWidth = 640;
		int bitmapHeight = (int)(bitmapWidth*wh);
		Bitmap resized = Bitmap.createScaledBitmap(myPhoto, bitmapWidth, bitmapHeight, true);

		// 1a) upload image
		PhotoHelper myPhotoHelper = new PhotoHelper(privateJid, launchedFrom, this);
		myPhotoHelper.uploadPhoto(resized);
	}	
	
	public void dismissDialog()
	{
		if (this.dialog!=null)
			this.dialog.dismiss();
	}
	
}