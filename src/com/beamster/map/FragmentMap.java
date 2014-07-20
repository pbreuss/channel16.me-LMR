package com.beamster.map;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;

import me.channel16.lmr.R;

import org.jivesoftware.smack.XMPPException;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.FragmentManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.beamster.AppConfig;
import com.beamster.AppConfig.TrackerName;
import com.beamster.BeamsterMessage;
import com.beamster.ChatActivity;
import com.beamster.LoginActivity;
import com.beamster.android_api.BeamsterAPI;
import com.beamster.android_api.BeamsterRosterItem;
import com.beamster.audio.AudioVisualizationDialogFragment;
import com.beamster.list.FragmentListAdapter;
import com.beamster.util.MultiDrawable;
import com.beamster.util.Person;
import com.beamster.util.RoundedImageView;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.maps.android.clustering.Cluster;
import com.google.maps.android.clustering.ClusterManager;
import com.google.maps.android.clustering.view.DefaultClusterRenderer;
import com.google.maps.android.ui.IconGenerator;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.SimpleImageLoadingListener;

public class FragmentMap extends MapFragment implements OnClickListener, OnTouchListener, TextWatcher,  ClusterManager.OnClusterClickListener<Person>, ClusterManager.OnClusterItemClickListener<Person> {

    GoogleMap mapView;
	EditText text = null;
	Button sendButton = null;  
	ImageButton photoButton = null;
    private ClusterManager<Person> mClusterManager;
    FragmentListAdapter adapter = null;
    ListView list = null;
    Hashtable<String,Marker> myMarkerHashtable;
    ImageButton recordAudioButton = null;
	AudioVisualizationDialogFragment myAudioVisualizationDialogFragment = null;
	boolean initialDraw = false;
	
    @Override
    public void onCreate(Bundle arg0) {
        super.onCreate(arg0);
        
        Log.d("BEAMSTER", "FragmentMap onCreate");				
        
        initialDraw = true;
    }

	/**
	 * MAP CLUSTERING CODE xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx   
	 */
    
    /**
     * Draws profile photos inside markers (using IconGenerator).
     * When there are multiple people in the cluster, draw multiple photos (using MultiDrawable).
     */
    private class PersonRenderer extends DefaultClusterRenderer<Person> {
        private final IconGenerator mIconGenerator = new IconGenerator(getActivity());
        private final IconGenerator mClusterIconGenerator = new IconGenerator(getActivity());
        private final ImageView mImageView;
        private final ImageView mClusterImageView;
        private final int mDimension;

        public PersonRenderer() {
            super(getActivity(), mapView, mClusterManager);

            View multiProfile = getActivity().getLayoutInflater().inflate(R.layout.multi_profile, null);
            mClusterIconGenerator.setContentView(multiProfile);
            mClusterImageView = (ImageView) multiProfile.findViewById(R.id.image);

            mImageView = new ImageView(getActivity());
            mDimension = (int) getResources().getDimension(R.dimen.custom_profile_image);
            mImageView.setLayoutParams(new ViewGroup.LayoutParams(mDimension, mDimension));
            int padding = (int) getResources().getDimension(R.dimen.custom_profile_padding);
            mImageView.setPadding(padding, padding, padding, padding);
            mIconGenerator.setContentView(mImageView);
            mIconGenerator.setStyle(8);
        }

        @Override
        protected void onBeforeClusterItemRendered(final Person person, final MarkerOptions markerOptions) {
            // Draw a single person.
            // Set the info window to show their name.
            final int width = mDimension;
            final int height = mDimension;
        	
            ImageLoader.getInstance().loadImage(person.getPictureUrl(), new SimpleImageLoadingListener(){

                @Override
                public void onLoadingComplete(String imageUri, View view,
                        Bitmap loadedImage) {
	                super.onLoadingComplete(imageUri, view, loadedImage);
	
	        		int w = loadedImage.getWidth();

	        		Bitmap roundBitmap = RoundedImageView.getCroppedBitmap(loadedImage, w);        
	                
	                Drawable d = new BitmapDrawable(getResources(), roundBitmap);
	                d.setBounds(0, 0, width, height);	                
	                
	                mImageView.setImageDrawable(d);	                
	                
	                Bitmap icon = mIconGenerator.makeIcon();
	                markerOptions.icon(BitmapDescriptorFactory.fromBitmap(icon)).title(person.getName());
                }

            });            
            
        }

        @Override
        protected void onBeforeClusterRendered(final Cluster<Person> cluster, final MarkerOptions markerOptions) {
            // Draw multiple people.
            // Note: this method runs on the UI thread. Don't spend too much time in here (like in this example).
            final List<Drawable> profilePhotos = new ArrayList<Drawable>(Math.min(4, cluster.getSize()));
            final int width = mDimension;
            final int height = mDimension;

            for (Person p : cluster.getItems()) {
                // Draw 4 at most.
                if (profilePhotos.size() == 4) break;
                ImageLoader.getInstance().loadImage(p.getPictureUrl(), new SimpleImageLoadingListener(){

                    @Override
                    public void onLoadingComplete(String imageUri, View view,
                            Bitmap loadedImage) {
    	                super.onLoadingComplete(imageUri, view, loadedImage);
    	                Drawable d = new BitmapDrawable(getResources(), loadedImage);
    	                d.setBounds(0, 0, width, height);
    	                profilePhotos.add(d);
                    }

                });                   
            }
            MultiDrawable multiDrawable = new MultiDrawable(profilePhotos);
            multiDrawable.setBounds(0, 0, width, height);

            mClusterImageView.setImageDrawable(multiDrawable);
            Bitmap icon = mClusterIconGenerator.makeIcon(String.valueOf(cluster.getSize()));
            markerOptions.icon(BitmapDescriptorFactory.fromBitmap(icon));
        }

        @Override
        protected boolean shouldRenderAsCluster(Cluster<Person> cluster) {
            // Always render clusters.
            return cluster.getSize() > 1;
        }
        
        @Override
        protected void onClusterRendered(Cluster<Person> cluster, Marker marker) {
            super.onClusterRendered(cluster, marker);            
            for (Person p : cluster.getItems())
            {
                myMarkerHashtable.put(p.getJid(), marker);
            }            
        }        
        
        @Override
        protected void onClusterItemRendered(Person person, Marker marker) {
            super.onClusterItemRendered(person, marker);
            myMarkerHashtable.put(person.getJid(), marker);
        }
        
    }

    @Override
    public boolean onClusterClick(Cluster<Person> cluster) {
        // Show a toast with some info when the cluster is clicked.
    	
    	LatLngBounds.Builder bounds = new LatLngBounds.Builder();   
    	for (Person p : cluster.getItems())
    	{
    		//use .include to put add each point to be included in the bounds   
            bounds.include(new LatLng(p.getPosition().latitude, p.getPosition().longitude));
    	}
    	
    	//set bounds with all the map points
        mapView.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds.build(), 50));
        
        //String firstName = cluster.getItems().iterator().next().name;
        //Toast.makeText(getActivity(), cluster.getSize() + " (including " + firstName + ")", Toast.LENGTH_SHORT).show();
        return true;
    }

    @Override
    public boolean onClusterItemClick(final Person item) {

    	// check if it is not me
    	if (!item.getJid().equals(((ChatActivity)getActivity()).getMyBeamsterUserProfile().getUserName()))
    	{
        	// Add Dialog
    		DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
    		    @Override
    		    public void onClick(DialogInterface dialog, int which) {
    		        switch (which){
    		        case DialogInterface.BUTTON_POSITIVE:
    		            //Yes button clicked
    		        	((ChatActivity)getActivity()).addTab(item.getJid(), item.getName(), item.getPictureUrl(), true, true);
    		        	
    		            break;

    		        case DialogInterface.BUTTON_NEGATIVE:
    		            //No button clicked
    		            break;
    		        }
    		    }
    		};		        	
        	
    		if (!((ChatActivity)getActivity()).getMyBeamsterUserProfile().isAnonymous())
    		{
        		boolean found  = false;
        		
    			// remove this one from the roster and display message to user
        		Iterator<BeamsterRosterItem> peopleIterator = ((ChatActivity)getActivity()).getPeople().iterator();
        		while (peopleIterator.hasNext())
        		{
        			BeamsterRosterItem myBeamsterRosterItem = peopleIterator.next();
        			if (item.getJid().equals(myBeamsterRosterItem.getJid()))
        			{
        				found = true;
            			break;
        			} // end if			            						            			
        		} // end while

        		if (found)
        		{
            		
            		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            		builder.setMessage(getActivity().getString(R.string.dialog_startPrivateChannel, item.getName()))
            		    .setNegativeButton(R.string.no, dialogClickListener)
            		    .setPositiveButton(R.string.yes, dialogClickListener).show();          			
        		}
        		else
        		{
    				Toast.makeText(getActivity(), R.string.is_offline, Toast.LENGTH_LONG).show();
        		} 		
    		}
    		else
    		{
				Toast.makeText(getActivity(), R.string.anonymous_not_working_channel, Toast.LENGTH_LONG).show();			    			
    		}    		
    		  		

    	}
    	return false;
    }
    
    protected void showInfoWindow(String jid, String name, String message)
    {
    	Marker myMarker = myMarkerHashtable.get(jid);
    	if (myMarker!=null)
    	{
        	myMarker.setTitle(name);
        	myMarker.setSnippet(message);
        	myMarker.showInfoWindow();    		
    	}
    }    
    
	/**
	 * /MAP CLUSTERING CODE xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx   
	 */    
    
    @Override
    public View onCreateView(LayoutInflater mInflater, ViewGroup arg1,
            Bundle arg2) {
    	
        Log.d("BEAMSTER", "FragmentMap onCreateView");				
    	
    	// get the layout
    	View view = mInflater.inflate(R.layout.fragment_map, arg1, false);        

		this.sendButton = (Button) view.findViewById(R.id.map_send_button);
		this.sendButton.setOnClickListener(this);		
		
		this.photoButton = (ImageButton) view.findViewById(R.id.map_photo_button);
		this.photoButton.setOnClickListener(this);		
		
		this.recordAudioButton = (ImageButton) view.findViewById(R.id.map_record_audio_button);
		this.recordAudioButton.setOnTouchListener(this);		

		this.text = (EditText) view.findViewById(R.id.textMap);				
		this.text.addTextChangedListener(this);		    	
		
    	// get the mapview from the MapFragment
        View myMapView = super.onCreateView(mInflater, arg1, arg2);

        // generate the layout programmatically ...
        android.widget.RelativeLayout.LayoutParams rl = new RelativeLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT);        
        rl.addRule(RelativeLayout.ABOVE, R.id.bottom_write_barMap);

        // and add it to the layout
        ((ViewGroup) view).addView(myMapView, rl);

        list = (ListView) view.findViewById(R.id.listMap);
        
        adapter = new FragmentListAdapter(getActivity(), ((ChatActivity)getActivity()).getMessages());
		list.setAdapter(adapter);
        
        list.bringToFront();

        // return the view
        return view;        
    }

    @Override
    public void onInflate(Activity arg0, AttributeSet arg1, Bundle arg2) {
        super.onInflate(arg0, arg1, arg2);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);        
        Log.d("BEAMSTER", "FragmentMap onActivityCreated");				

        if (initialDraw)
        {
            mapView = getMap();
            mapView.getUiSettings().setZoomControlsEnabled(false);	   
            //This is how you register the LocationSource
            mapView.setMyLocationEnabled(true);
            mapView.setLocationSource(((ChatActivity)getActivity()));
	        
            mClusterManager = new ClusterManager<Person>(getActivity(), mapView);
            myMarkerHashtable = new Hashtable<String,Marker>();
            mClusterManager.setRenderer(new PersonRenderer());
            mapView.setOnCameraChangeListener(mClusterManager);
            mapView.setOnMarkerClickListener(mClusterManager);
            mClusterManager.setOnClusterClickListener(this);
            mClusterManager.setOnClusterItemClickListener(this);
            
            Log.d("BEAMSTER", "getting mapView: "+mapView);	    			

            mapView.clear();
        
	    	ArrayList<BeamsterRosterItem> myArrayList = ((ChatActivity)getActivity()).getPeople();
	    	for (BeamsterRosterItem user : myArrayList)	
	    	{
	    		try
	    		{
					LatLng myLatLng = new LatLng(Double.parseDouble(user.getLat()), Double.parseDouble(user.getLon()));
					
					// show only users with lat/lon not 0 - these are users who have their location hidden
					if (myLatLng.latitude != 0 && myLatLng.longitude != 0)
					{
						Person newPerson = new Person(getResources(), user.getJid(), myLatLng, user.getName(), user.getPictureUrl());
				        mClusterManager.addItem(newPerson);
		
				        // save a reference to the marker within the BeamsterRosterItem object, such that we can find the marker based on user id
				        user.setPerson(newPerson);			
						
					}					
	    		}
	    		catch (Exception e)
	    		{
	    			try
	    			{
	    				((AppConfig)getActivity().getApplication()).trackException(60, e);			
	    			}
	    			catch (Exception e2)
	    			{
		    	        Log.e("BEAMSTER", "Failed to report exception 60"+e2);	    			            		    				
	    			}

	    	        Log.e("BEAMSTER", "FragmentMap onActivityCreated - error creating marker: "+user+" error: "+e.getMessage());	    			
	    		}
	    	} 
	    	    	
	        mClusterManager.cluster();
	        
	        Location center = ((ChatActivity)getActivity()).getCurrentCenter();	        
	        if (center!=null)
	        {
		        CameraUpdate centerLatLng = CameraUpdateFactory.newLatLng(new LatLng(center.getLatitude(), center.getLongitude()));
		        mapView.moveCamera(centerLatLng);	        	
	        }
	        	        
	        initialDraw = false;
        }
    }
    
	/**
	 * Updates the map view.
	 */
	public void updateMessagesMap(final boolean moveToEnd, final String jid, final String name, final String text)
	{
		try
		{
			if (isAdded() || isHidden()) 
			{		
				getActivity().runOnUiThread(new Runnable() {
		            @Override
		            public void run() {
		            	// show message in map
		            	showInfoWindow(jid, name, text);
		            	
		            	// update list view when message comes in
		            	if (adapter!=null && list!=null)
		            	{
			   	           	 adapter.notifyDataSetChanged();
				           	 if (moveToEnd)
				           	 {
				           		list.setSelection(((ChatActivity)getActivity()).getMessages().size()-1);
				           	 }	            		
		            	}
		            }
		        });		
			}			
		}
		catch (Exception e)
		{
			try
			{
				((AppConfig)getActivity().getApplication()).trackException(426, e);									
			}
			catch (Exception e2)
			{
    	        Log.e("BEAMSTER", "Failed to report exception 426"+e2);	    			            		    				
			}
		}
			
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		Log.d("BEAMSTER", "FragmentMap onDestroy");				
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		Log.d("BEAMSTER", "FragmentMap onDestroyView");				
	}

	@Override
	public void onPause() {
		super.onPause();
		Log.d("BEAMSTER", "FragmentMap onPause");				
	}

	@Override
	public void onResume() {
		super.onResume();
		Log.d("BEAMSTER", "FragmentMap onResume");	
		
		try
		{
			if (isAdded() || isHidden()) 
			{
				getActivity().runOnUiThread(new Runnable() {
		            @Override
		            public void run() {
		            	// update list view when message comes in
		            	if (list!=null)
		            	{
			   	           	 list.setSelection(((ChatActivity)getActivity()).getMessages().size()-1);
		            	}
		            }
		        });		
			}
		}
		catch (Exception e)
		{
			try
			{
				((AppConfig)getActivity().getApplication()).trackException(479, e);									
			}
			catch (Exception e2)
			{
    	        Log.e("BEAMSTER", "Failed to report exception 479"+e2);	    			            		    				
			}
		}
		
		// Google Analytics
		Tracker t = ((AppConfig)getActivity().getApplication()).getTracker(TrackerName.APP_TRACKER);

        // Set screen name.
        t.setScreenName("MapView");

        // Send a screen view.
        t.send(new HitBuilders.AppViewBuilder().build());        
        		
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		Log.d("BEAMSTER", "FragmentMap onAttach");				
	}

	@Override
	public void onStart() {
		super.onStart();
		Log.d("BEAMSTER", "FragmentMap onStart");				
	}

	/**
	 * Adds 
	 */
	public void updateMessagesMap(final BeamsterRosterItem user, final String type)
	{
		if (isAdded() || isHidden()) 
		{				
			getActivity().runOnUiThread(new Runnable() {
	            @Override
	            public void run() {
	            	
            		try
            		{
            			if (type.equals("add") && user!=null)
            			{
		    				LatLng myLatLng = new LatLng(Double.parseDouble(user.getLat()), Double.parseDouble(user.getLon()));
							// show only users with lat/lon not 0 - these are users who have their location hidden
							if (myLatLng.latitude != 0 && myLatLng.longitude != 0)
							{
			    				Person newPerson = new Person(getResources(), user.getJid(), myLatLng, user.getName(), user.getPictureUrl());
			    		        mClusterManager.addItem(newPerson);
			    		   
			    		        user.setPerson(newPerson);
							}

							mClusterManager.cluster();
						} 
            			else if (type.equals("remove") || type.equals("update"))
            			{
            				mClusterManager.clearItems();
            		    	ArrayList<BeamsterRosterItem> myArrayList = ((ChatActivity)getActivity()).getPeople();
            		    	for (BeamsterRosterItem user : myArrayList)	
            		    	{
            		    		try
            		    		{
            						LatLng myLatLng = new LatLng(Double.parseDouble(user.getLat()), Double.parseDouble(user.getLon()));
            						
            						// show only users with lat/lon not 0 - these are users who have their location hidden
            						if (myLatLng.latitude != 0 && myLatLng.longitude != 0)
            						{	            						
	            						Person newPerson = new Person(getResources(), user.getJid(), myLatLng, user.getName(), user.getPictureUrl());
	            				        mClusterManager.addItem(new Person(getResources(), user.getJid(), myLatLng, user.getName(), user.getPictureUrl()));

	            				        user.setPerson(newPerson);
            						}
            						else
            							Log.d("BEAMSTER", "User not drawn on map because 0/0"+myLatLng);	    			
            					}
            		    		catch (Exception e)
            		    		{
            		    			try
            		    			{
            		    				((AppConfig)getActivity().getApplication()).trackException(61, e);			            		    			
            		    			}
            		    			catch (Exception e2)
            		    			{
                		    	        Log.e("BEAMSTER", "Failed to report exception 61"+e2);	    			            		    				
            		    			}
            		    	        Log.e("BEAMSTER", "FragmentMap onActivityCreated - error creating marker: "+user+" error: "+e.getMessage());	    			
            		    		}
            		    	} 
            		    	
            		        mClusterManager.cluster();
            				
            			} 
            			else if (type.equals("update"))
            			{
                			Log.e("BEAMSTER", "Updating POIS ...");
                			if (user!=null)
                			{
                				Person existingPerson = user.getPerson();
                				LatLng myLatLng = new LatLng(Double.parseDouble(user.getLat()), Double.parseDouble(user.getLon()));
                				existingPerson.setPosition(myLatLng);
    		    		        mClusterManager.cluster();                				
                			}
            			}            			
            		}
            		catch (Exception e)
            		{
            			try
            			{
            				((AppConfig)getActivity().getApplication()).trackException(62, e);			
		    			}
		    			catch (Exception e2)
		    			{
    		    	        Log.e("BEAMSTER", "Failed to report exception 62"+e2);	    			            		    				
		    			}
            			Log.e("BEAMSTER", "Error in updateMessagesMap: "+user+" type: "+type+" Error: "+e.getMessage());
            		}
	            }
	        });		
		}
			
	}	
    
	
	@Override
	public void onClick(View v) {
		
		 switch (v.getId()) {
	        case R.id.map_photo_button:
	    		// pick an image from the gallery or cam
	    		((ChatActivity)getActivity()).selectImage(null, "map");
	        	break;		 
	        case R.id.map_send_button:
	    		String newMessage = text.getText().toString().trim(); 
	    		if(newMessage.length() > 0)
	    		{
	    			text.setText("");

					// create new messaging object
					BeamsterMessage myBeamsterMessage = new BeamsterMessage(newMessage, true);	    
            		myBeamsterMessage.setBeamed(((ChatActivity)getActivity()).isBeamed());
            		myBeamsterMessage.setClientId(BeamsterAPI.getInstance().getClientId());
            		myBeamsterMessage.setDistanceAway(0);
            		myBeamsterMessage.setLat(""+((ChatActivity)getActivity()).getCurrentCenter().getLatitude());
            		myBeamsterMessage.setLon(""+((ChatActivity)getActivity()).getCurrentCenter().getLongitude());
            		myBeamsterMessage.setMessagedate(Calendar.getInstance());
            		myBeamsterMessage.setName(((ChatActivity)getActivity()).getMyBeamsterUserProfile().getName());
            		String pictureUrl = ((ChatActivity)getActivity()).getMyBeamsterUserProfile().getPictureUrl();
            		if (pictureUrl!=null && !pictureUrl.equals(""))            			
            			myBeamsterMessage.setPictureurl(pictureUrl);
            		myBeamsterMessage.setSpeed("");
            		myBeamsterMessage.setUsername(BeamsterAPI.getInstance().getUsername());
            		
            		// add it to list of messages
	    			((ChatActivity)getActivity()).getMessages().add(myBeamsterMessage);					

					Log.d("BEAMSTER", "message posted: "+myBeamsterMessage);

	    			
					// add message to map
					updateMessagesMap(true, ((ChatActivity)getActivity()).getMyBeamsterUserProfile().getUserName(), ((ChatActivity)getActivity()).getMyBeamsterUserProfile().getName(), newMessage);
					
	    			// add message to the adapter	    			
					try {
						
						// send a message (false = composing is false)
						BeamsterAPI.getInstance().sendMessage("chat@beamster.", ((ChatActivity)getActivity()).getMyBeamsterUserProfile().getName(), ((ChatActivity)getActivity()).getMyBeamsterUserProfile().getPictureUrl(), "", newMessage, ((ChatActivity)getActivity()).getCurrentCenter().getLatitude(), ((ChatActivity)getActivity()).getCurrentCenter().getLongitude(), false, "");

						// Get tracker.
				        Tracker t = ((AppConfig)getActivity().getApplication()).getTracker(
				            TrackerName.APP_TRACKER);
				        // Build and send an Event.
				        t.send(new HitBuilders.EventBuilder()
				            .setCategory("Message")
				            .setAction("TextMessageSentMap") // actionId
				            .setLabel("chat") 
				            .build());	
				        
					} catch (XMPPException e) {
						try
						{
							((AppConfig)getActivity().getApplication()).trackException(63, e);			
		    			}
		    			catch (Exception e2)
		    			{
    		    	        Log.e("BEAMSTER", "Failed to report exception 63"+e2);	    			            		    				
		    			}

						Log.e("BEAMSTER", "message not sent! "+e);
					} catch (Exception e)
		        	{
						try
						{
							((AppConfig)getActivity().getApplication()).trackException(64, e);			
		    			}
		    			catch (Exception e2)
		    			{
    		    	        Log.e("BEAMSTER", "Failed to report exception 61"+e2);	    			            		    				
		    			}
							
						Toast.makeText(getActivity(), "Connection lost ... logging out ... sorry.", Toast.LENGTH_LONG).show();
						// if the above crashed ... do this
	    				Intent intent = new Intent(getActivity(), LoginActivity.class);
	    				intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP); 
	    				intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
	    				startActivity(intent);	    				
		        	}
					
		        	sendButton.setVisibility(View.GONE);
		        	photoButton.setVisibility(View.VISIBLE);
		        	
					// hide soft keyboard
					InputMethodManager imm = (InputMethodManager)getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
					imm.hideSoftInputFromWindow(text.getWindowToken(), 0);				
		    		//text.removeTextChangedListener(this);			
	    		}
	            break;
		 }		
	}

	@Override
	public void afterTextChanged(Editable s) {}

	@Override
	public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

	@Override
	public void onTextChanged(CharSequence s, int start, int before, int count) {
		try
		{
			if (s.length()>0 && s.charAt(s.length() - 1) == '\n') 
			{
	            Log.d("BEAMSTER", "Enter was pressed");
	        	sendButton.performClick();
	        }
	        else if (s.length()>0) // send a composing message
			{
	        	sendButton.setVisibility(View.VISIBLE);
	        	photoButton.setVisibility(View.GONE);
	        		        	
				try {
					BeamsterAPI.getInstance().sendMessage("chat@beamster.", ((ChatActivity)getActivity()).getMyBeamsterUserProfile().getName(), ((ChatActivity)getActivity()).getMyBeamsterUserProfile().getPictureUrl(), "", "", ((ChatActivity)getActivity()).getCurrentCenter().getLatitude(), ((ChatActivity)getActivity()).getCurrentCenter().getLongitude(), true, BeamsterAPI.COMPOSING_TYPE_TYPING);
				} catch (XMPPException e) {
					try
					{
						((AppConfig)getActivity().getApplication()).trackException(65, e);			
	    			}
	    			catch (Exception e2)
	    			{
		    	        Log.e("BEAMSTER", "Failed to report exception 65"+e2);	    			            		    				
	    			}

					Log.e("BEAMSTER", "Composing Message could not be sent");
				} catch (Exception e)
	        	{
					try
					{
						((AppConfig)getActivity().getApplication()).trackException(66, e);			
	    			}
	    			catch (Exception e2)
	    			{
		    	        Log.e("BEAMSTER", "Failed to report exception 66"+e2);	    			            		    				
	    			}

					Toast.makeText(getActivity(), "Connection lost ... logging out ... sorry.", Toast.LENGTH_LONG).show();					
					// if the above crashed ... do this
    				Intent intent = new Intent(getActivity(), LoginActivity.class);
    				intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP); 
    				intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    				startActivity(intent);	    				
	        	}						
			}
	        else
	        {
	        	sendButton.setVisibility(View.GONE);
	        	photoButton.setVisibility(View.VISIBLE);	        	
	        }
		}
        catch (Exception e)
        {
        	try
        	{
        		((AppConfig)getActivity().getApplication()).trackException(67, e);			
			}
			catch (Exception e2)
			{
    	        Log.e("BEAMSTER", "Failed to report exception 67"+e2);	    			            		    				
			}
			Log.e("BEAMSTER", "onTextChanged: "+e.getMessage());	        	
        }
	}    
	
	void showAlert(int appName, int errorBuffer) {
		// Create an alert dialog builder

		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

		// Set the title, message and button

		builder.setTitle(appName);
		builder.setMessage(errorBuffer);
		builder.setNeutralButton(android.R.string.ok,
				new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
			}
		});

		// Create the dialog
		AlertDialog dialog = builder.create();

		// Show it
		dialog.show();
	}		
    
	long start = 0; 
	
	@Override
	public boolean onTouch(View arg0, MotionEvent event) {

		FragmentManager fm = getFragmentManager();
		
		switch (event.getAction())		
		{
			case MotionEvent.ACTION_DOWN:
				myAudioVisualizationDialogFragment = new AudioVisualizationDialogFragment(null, "map");
				myAudioVisualizationDialogFragment.setRetainInstance(true);
				myAudioVisualizationDialogFragment.show(fm, "audioVisualizationDialogFragment");
				start = System.currentTimeMillis();
				break;
			case MotionEvent.ACTION_UP:
				if (myAudioVisualizationDialogFragment!=null)
					myAudioVisualizationDialogFragment.dismiss();
				long timePassed = System.currentTimeMillis()-start;
				Log.d("BEAMSTER", "Time passed: "+timePassed);
				if (timePassed<200)
				{
					getActivity().runOnUiThread(new Runnable() {
			            @Override
			            public void run() {
							//Toast.makeText(getActivity(), R.string.holdtotalk, 7000).show();
							showAlert(R.string.app_name, R.string.holdtotalk);
			            }
			        });		
					
				}
				
				
				break;
		}
		return false;
	}

  	
}

