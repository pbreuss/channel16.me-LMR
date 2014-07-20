package com.beamster.list;

import java.util.ArrayList;
import java.util.Iterator;

import me.channel16.lmr.R;
import android.app.AlertDialog;
import android.app.FragmentManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.beamster.BeamsterMessage;
import com.beamster.ChatActivity;
import com.beamster.android_api.BeamsterAPI;
import com.beamster.android_api.BeamsterRosterItem;
import com.beamster.photo.PhotoDialogFragment;
import com.beamster.util.PrettyTimeSingleton;
import com.beamster.util.Utils;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.ocpsoft.pretty.time.PrettyTime;

/**
 * AwesomeAdapter is a Custom class to implement custom row in ListView
 * 
 * @author PBS
 *
 */
public class FragmentListAdapter extends BaseAdapter {
	private static final int TYPE_TEXT_LEFT = 0;
	private static final int TYPE_TEXT_RIGHT = 1;
    private static final int TYPE_AUDIO_LEFT = 2;
    private static final int TYPE_AUDIO_RIGHT = 3;
    private static final int TYPE_PHOTO_LEFT = 4;
    private static final int TYPE_PHOTO_RIGHT = 5;
    private static final int TYPE_STATUS = 6;
    private static final int TYPE_MAX_COUNT = 7;

    private Context mContext;
	private ArrayList<BeamsterMessage> mMessages;
	private PrettyTime p;
	private DisplayImageOptions options;

	public FragmentListAdapter(Context context, ArrayList<BeamsterMessage> messages) {
		super();
		this.mContext = context;
		this.mMessages = messages;
		options = new DisplayImageOptions.Builder()
	    .showImageForEmptyUri(R.drawable.ic_launcher)
	    .showImageOnFail(R.drawable.no_photo)
	    .build();
		try
		{
			this.p = PrettyTimeSingleton.getInstance(BeamsterAPI.getInstance().getLocale());
		}
		catch (Exception e)
		{
        	// go to the home activity
        	Intent intent = new Intent(Intent.ACTION_MAIN);
        	intent.addCategory(Intent.CATEGORY_HOME);
        	intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        	context.startActivity(intent);		            	
		}
	}
	@Override
	public int getCount() {
		if (mMessages!=null)
			return mMessages.size();
		else
			return 0;
	}
	@Override
	public Object getItem(int position) {		
		return mMessages.get(position);
	}
	
    @Override
    public int getViewTypeCount() {
        return TYPE_MAX_COUNT;
    }
    
    @Override
    public int getItemViewType(int position) {
    	BeamsterMessage message = (BeamsterMessage) this.getItem(position);
    	if (message.isStatusMessage())
    		return TYPE_STATUS;
    	else if (message.isAudio())
    	{
    		 if (message.isMine())
	    		return TYPE_AUDIO_RIGHT;
    		 else
 	    		return TYPE_AUDIO_LEFT;    			 
    	}
    	else if (message.isPhoto())
    	{
    		 if (message.isMine())
	    		return TYPE_PHOTO_RIGHT;
    		 else
 	    		return TYPE_PHOTO_LEFT;    			 
    	}
    	else
    	{
	   		 if (message.isMine())
	    		return TYPE_TEXT_RIGHT;
	   		 else
	    		return TYPE_TEXT_LEFT;    			     		
    	}
    }    
    
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		
		ViewHolder holder = null; 		
		int type = getItemViewType(position);		
		
		if(convertView == null)
		{
			holder = new ViewHolder();
			switch (type) {
            case TYPE_TEXT_LEFT:
				convertView = LayoutInflater.from(mContext).inflate(R.layout.list_bubble_row_message_left, parent, false);								
				holder.message = (TextView) convertView.findViewById(R.id.message_text);
				holder.player = null;

				holder.picture = (ImageView) convertView.findViewById(R.id.peopleRowPicture);
				holder.name_text = (TextView) convertView.findViewById(R.id.name_text);
				holder.info_text = (TextView) convertView.findViewById(R.id.info_text);
				
				break;
            case TYPE_TEXT_RIGHT:
				convertView = LayoutInflater.from(mContext).inflate(R.layout.list_bubble_row_message_right, parent, false);								
				holder.message = (TextView) convertView.findViewById(R.id.message_text);
				holder.player = null;

				holder.picture = (ImageView) convertView.findViewById(R.id.peopleRowPicture);
				holder.name_text = (TextView) convertView.findViewById(R.id.name_text);
				holder.info_text = (TextView) convertView.findViewById(R.id.info_text);
				
				break;
            case TYPE_AUDIO_LEFT:
				convertView = LayoutInflater.from(mContext).inflate(R.layout.list_bubble_row_player_left, parent, false);								
				holder.player = (LinearLayout) convertView.findViewById(R.id.player_bubble);
				holder.message = null;

				holder.picture = (ImageView) convertView.findViewById(R.id.peopleRowPicture);
				holder.name_text = (TextView) convertView.findViewById(R.id.name_text);
				holder.info_text = (TextView) convertView.findViewById(R.id.info_text);

				break;
            case TYPE_AUDIO_RIGHT:
				convertView = LayoutInflater.from(mContext).inflate(R.layout.list_bubble_row_player_right, parent, false);								
				holder.player = (LinearLayout) convertView.findViewById(R.id.player_bubble);
				holder.message = null;

				holder.picture = (ImageView) convertView.findViewById(R.id.peopleRowPicture);
				holder.name_text = (TextView) convertView.findViewById(R.id.name_text);
				holder.info_text = (TextView) convertView.findViewById(R.id.info_text);

				break;
            case TYPE_PHOTO_LEFT:
				convertView = LayoutInflater.from(mContext).inflate(R.layout.list_bubble_row_photo_left, parent, false);								
				holder.photo = (ImageView) convertView.findViewById(R.id.message_photo);
				
				holder.player = null;
				holder.message = null;

				holder.picture = (ImageView) convertView.findViewById(R.id.peopleRowPicture);
				holder.name_text = (TextView) convertView.findViewById(R.id.name_text);
				holder.info_text = (TextView) convertView.findViewById(R.id.info_text);

				break;	
            case TYPE_PHOTO_RIGHT:
				convertView = LayoutInflater.from(mContext).inflate(R.layout.list_bubble_row_photo_right, parent, false);								
				holder.photo = (ImageView) convertView.findViewById(R.id.message_photo);
				holder.player = null;
				holder.message = null;

				holder.picture = (ImageView) convertView.findViewById(R.id.peopleRowPicture);
				holder.name_text = (TextView) convertView.findViewById(R.id.name_text);
				holder.info_text = (TextView) convertView.findViewById(R.id.info_text);

				break;	
            case TYPE_STATUS:
				convertView = LayoutInflater.from(mContext).inflate(R.layout.list_bubble_row_status, parent, false);								
				holder.message = (TextView) convertView.findViewById(R.id.status_text);
				holder.picture = (ImageView) convertView.findViewById(R.id.peopleRowPicture);
				holder.player = null;
				
				break;
			}			
			
			convertView.setTag(holder);
		}
		else
			holder = (ViewHolder) convertView.getTag();

		final BeamsterMessage message = (BeamsterMessage) this.getItem(position);
		
		// format the date
		String messageDate = p.format(message.getMessagedate().getTime());
		
		String radiusUnit = "miles";
		try
		{
			radiusUnit = ((ChatActivity)mContext).getMyBeamsterUserProfile().getRadiusUnit();			
		}
		catch (Exception e)
		{
	        Log.e("BEAMSTER", "Error while retrieving radiusUnit from profile ", e);
		}
				
		if (holder.picture != null) {
			if (message.getPictureurl()==null || message.getPictureurl().equals(""))
			{
				Drawable myIcon = this.mContext.getResources().getDrawable( R.drawable.channel16_50x50 );				
				holder.picture.setImageDrawable(myIcon);
			}
			else
			{
				ImageLoader.getInstance().displayImage(message.getPictureurl(), holder.picture);
			}
			
			if (!message.isMine())
			{
				holder.picture.setOnClickListener(new OnClickListener()
			    {
			        @Override
			        public void onClick(View v)
			        {
			        	// Add Dialog
			    		DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
			    		    @Override
			    		    public void onClick(DialogInterface dialog, int which) {
			    		        switch (which){
			    		        case DialogInterface.BUTTON_POSITIVE:
			    		            //Yes button clicked
			    		        	((ChatActivity)mContext).addTab(message.getUsername(), message.getName(), message.getPictureurl(), true, true);
			    		        	
			    		            break;
	
			    		        case DialogInterface.BUTTON_NEGATIVE:
			    		            //No button clicked
			    		            break;
			    		        }
			    		    }
			    		};		  
			    		
			    		boolean found  = false;
			    		
			    		if (!((ChatActivity)mContext).getMyBeamsterUserProfile().isAnonymous())
			    		{
	            			// remove this one from the roster and display message to user
		            		Iterator<BeamsterRosterItem> peopleIterator = ((ChatActivity)mContext).getPeople().iterator();
		            		while (peopleIterator.hasNext())
		            		{
		            			BeamsterRosterItem myBeamsterRosterItem = peopleIterator.next();
		            			if (message.getUsername().equals(myBeamsterRosterItem.getJid()))
		            			{
		            				found = true;
			            			break;
		            			} // end if			            						            			
		            		} // end while

		            		if (found)
		            		{
					    		AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
					    		builder.setMessage(mContext.getString(R.string.dialog_startPrivateChannel, message.getName()))
					    		    .setNegativeButton(R.string.no, dialogClickListener)
					    		    .setPositiveButton(R.string.yes, dialogClickListener).show();	            			
		            		}
		            		else
		            		{
								Toast.makeText(mContext, R.string.is_offline, Toast.LENGTH_LONG).show();
		            		}			    			
			    		}
			    		else
			    		{
							Toast.makeText(mContext, R.string.anonymous_not_working_channel, Toast.LENGTH_LONG).show();			    			
			    		}
			    		
			        }
			    });						
			} // end not mine message
		}									
		
		if (message.isAudio())
		{
			// setting the info text	
			if (holder.name_text!=null)
				holder.name_text.setText(message.getName());
			holder.info_text.setText(messageDate+" "+Utils.getFormattedDistance(this.mContext, message.getDistanceAway(), radiusUnit, message.isBeamed(), message.getClientId()));									
 
			// save reference to seekbar
			message.setSeekBar((SeekBar)convertView.findViewById(R.id.songProgressBar));
			message.setCurrentDurationLabel((TextView)convertView.findViewById(R.id.songCurrentDurationLabel));
			message.setTotalDurationLabel((TextView)convertView.findViewById(R.id.songTotalDurationLabel));
			message.setPlayButton((ImageButton)convertView.findViewById(R.id.btnPlay));
						
			if (message.isAudio() && message.isStillNeedsToBePlayed())
			{
				((ChatActivity)mContext).playSong(message, message.getPlayButton(), message.getSeekBar(), message.getCurrentDurationLabel(), message.getTotalDurationLabel(), message.getMessage());
			}			
			
			holder.getButton().setOnClickListener(new OnClickListener()
		    {
		        @Override
		        public void onClick(View v)
		        {
					if(((ChatActivity)mContext).mp.isPlaying()){
						if(((ChatActivity)mContext).mp!=null){
							((ChatActivity)mContext).mp.pause();
							// Changing button image to play button
							((ImageButton)v).setImageResource(R.drawable.btn_play);
						}
					}else{
						// Resume song						
						((ChatActivity)mContext).playSong(message, ((ImageButton)v), message.getSeekBar(), message.getCurrentDurationLabel(), message.getTotalDurationLabel(), message.getMessage());
					}

		        }
		    });			
		}
		else if (message.isPhoto())
		{
			try
			{
				ImageLoader.getInstance().displayImage(message.getMessage(), holder.photo, options);
				holder.photo.setOnClickListener(new OnClickListener()
			    {
			        @Override
			        public void onClick(View v)
			        {
			        	FragmentManager fm = ((ChatActivity)mContext).getFragmentManager();
			        	PhotoDialogFragment photoDialog = new PhotoDialogFragment();
			        	// Supply num input as an argument.
			            Bundle args = new Bundle();
			            args.putString("url", message.getMessage());
			            photoDialog.setArguments(args);
			        	photoDialog.setRetainInstance(false);
			        	photoDialog.show(fm, "people_in_the_area");
			        }
			    });			
				
				// setting the info text	
				if (holder.name_text!=null)				
					holder.name_text.setText(message.getName());
				holder.info_text.setText(messageDate+" "+Utils.getFormattedDistance(this.mContext, message.getDistanceAway(), radiusUnit, message.isBeamed(), message.getClientId()));													
			}
			catch (Exception e)
			{
				Log.e("BEAMSTER", e.getMessage());
			}
		}
		else if (message.isStatusMessage())
		{
			try
			{
				holder.message.setText(message.getMessage());							
			}
			catch (Exception e)
			{
				Log.e("BEAMSTER", e.getMessage());
			}
		}
		else
		{
			try
			{
				holder.message.setText(message.getMessage());				
				
				// setting the info text	
				if (holder.name_text!=null)				
					holder.name_text.setText(message.getName());
				holder.info_text.setText(messageDate+" "+Utils.getFormattedDistance(this.mContext, message.getDistanceAway(), radiusUnit, message.isBeamed(), message.getClientId()));													
			}
			catch (Exception e)
			{
				Log.e("BEAMSTER", e.getMessage());
			}
		}
		return convertView;
	}
	
	private static class ViewHolder
	{
		TextView message = null;
		ImageView photo = null;
		ImageView picture = null;
		TextView name_text = null;
		TextView info_text = null;
		LinearLayout player = null;
		ImageButton button = null;
		
		public ImageButton getButton()
	    {
			if (player==null)
				return null;
			else if (button == null)
	        {
	            button = (ImageButton) player.findViewById(R.id.btnPlay);
	        }
	        return (button);
	    }				
	}
	
	@Override
	public long getItemId(int position) {
		//Unimplemented, because we aren't using Sqlite.
		return 0;
	}
	
/*
 * maybe this can be used later
	new ImageDownloaderTask(holder.pictureLeft).execute(message.getPictureurl());

	class ImageDownloaderTask extends AsyncTask<String, Void, Bitmap> {
	    private final WeakReference<ImageView> imageViewReference;
	 
	    public ImageDownloaderTask(ImageView imageView) {
	        imageViewReference = new WeakReference<ImageView>(imageView);
	    }
	 
	    @Override
	    // Actual download method, run in the task thread
	    protected Bitmap doInBackground(String... params) {
	        // params comes from the execute() call: params[0] is the url.
	        return downloadBitmap(params[0]);
	    }
	 
	    @Override
	    // Once the image is downloaded, associates it to the imageView
	    protected void onPostExecute(Bitmap bitmap) {
	    	
            Log.w("BEAMSTER", "ImageDownloaderTask.onPostExecute: "+bitmap);
	    	
	        if (isCancelled()) {
	            bitmap = null;
	        }
	 
	        if (imageViewReference != null) {
	            ImageView imageView = imageViewReference.get();
	            if (imageView != null) {
	 
	                if (bitmap != null) {
	                    imageView.setImageBitmap(bitmap);
	                } else {
	                    imageView.setImageDrawable(imageView.getContext().getResources()
	                            .getDrawable(R.drawable.anonymous_m));
	                }
	            }
	 
	        }
	    }
	 
	}
	
	private static Bitmap downloadBitmap(String stringUrl) {
	    URL url = null;
	    HttpURLConnection connection = null;
	    InputStream inputStream = null;
	    
	    try {
	        url = new URL(stringUrl);
	        connection = (HttpURLConnection) url.openConnection();
	        connection.setUseCaches(true);
	        inputStream = connection.getInputStream();
	        
	        return BitmapFactory.decodeStream(new FlushedInputStream(inputStream));
	    } catch (Exception e) {
	        Log.e("BEAMSTER", "Error while retrieving bitmap from " + stringUrl, e);
	    } finally {
	        if (connection != null) {
	            connection.disconnect();
	        }
	    }
	    
	    return null;
	}
	
	static class FlushedInputStream extends FilterInputStream 
	{
		public FlushedInputStream(InputStream inputStream) 
		{
			super(inputStream);
		}	
		
		@Override
		public long skip(long n) throws IOException 
		{
			long totalBytesSkipped = 0L;
			while (totalBytesSkipped < n) 
			{
				long bytesSkipped = in.skip(n - totalBytesSkipped);
				if (bytesSkipped == 0L) 
				{
					int byteValue = read();
					if (byteValue < 0) {
						break; // we reached EOF
					} 
					else 
					{
						bytesSkipped = 1; // we read one byte
					}
				}
				totalBytesSkipped += bytesSkipped;
			}
			return totalBytesSkipped;
		}
	}*/		
}
