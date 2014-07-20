package com.beamster.people;

import java.util.ArrayList;

import me.channel16.lmr.R;
import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.beamster.AppConfig;
import com.beamster.android_api.BeamsterRosterItem;
import com.nostra13.universalimageloader.core.ImageLoader;

/**
 * FragmentPeopleAdapter is a Custom class to implement custom row in ListView
 * 
 * @author pbre
 *
 */
public class FragmentPeopleAdapter extends BaseAdapter {
	private Context mContext;
	private ArrayList<BeamsterRosterItem> mPeople;
	private int selectedPos = 0;

	public FragmentPeopleAdapter(Context context, ArrayList<BeamsterRosterItem> mPeople) {
		super();
		this.mContext = context;
		this.mPeople = mPeople;
	}
	@Override
	public int getCount() {
		if (mPeople==null)
			return 0;
		else
			return mPeople.size();
	}
	@Override
	public Object getItem(int position) {		
		return mPeople.get(position);
	}

	public void setSelectedPosition( int pos )
	{
		selectedPos = pos; // selectedPos is global variable to handle clicked position
		// inform the view of this change
    	// if connection has been null and BeamsterAPI.getInstance() return null
  		try
  		{
			notifyDataSetChanged();
  		}
  		catch (Exception e2)
  		{
			try
			{
				((AppConfig)this.mContext).trackException(64, e2);
			}
			catch (Exception e3)
			{
    	        Log.e("BEAMSTER", "Failed to report exception 64"+e3);	    			            		    				
			}	  			
  		}			
	}	    


	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		BeamsterRosterItem item = (BeamsterRosterItem) this.getItem(position);

		ViewHolder holder; 
		if(convertView == null)
		{
			holder = new ViewHolder();
			convertView = LayoutInflater.from(mContext).inflate(R.layout.people_list_row, parent, false);

			holder.name = (TextView) convertView.findViewById(R.id.peopleRowName);
			holder.info = (TextView) convertView.findViewById(R.id.peopleRowInfo);
			holder.statusMessage = (TextView) convertView.findViewById(R.id.peopleRowStatus);
			holder.picture = (ImageView) convertView.findViewById(R.id.peopleRowPicture);			

			convertView.setTag(holder);
		}
		else
			holder = (ViewHolder) convertView.getTag();

		// add the message here
		holder.name.setText(item.getName());
		holder.statusMessage.setText(item.getStatusMessage());
		
		double dist = Double.parseDouble(item.getDistanceAway());
		double rounded = Math.floor(1000 * dist + 0.5) / 1000;
		holder.info.setText(String.valueOf(rounded) 
				+ item.getRadiusUnit().substring(0,2)); 

		if (holder.picture != null) {
			ImageLoader.getInstance().displayImage(item.getPictureUrl(), holder.picture);
		}			

		if ( selectedPos == position )
		{
			convertView.setBackgroundColor(Color.rgb(0, 153, 204)); 
		}	
		else
		{
			convertView.setBackgroundColor(convertView.getContext().getResources().getColor(R.color.listViewBg)); 
		}	

		return convertView;
	}

	private static class ViewHolder
	{
		TextView name;
		TextView info;
		TextView statusMessage;
		ImageView picture;				
	}

	@Override
	public long getItemId(int position) {
		//Unimplemented, because we aren't using Sqlite.
		return 0;
	}

}
