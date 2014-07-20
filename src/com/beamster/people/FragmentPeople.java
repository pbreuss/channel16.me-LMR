package com.beamster.people;

import java.util.Iterator;

import me.channel16.lmr.R;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ListFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.Toast;

import com.beamster.ChatActivity;
import com.beamster.android_api.BeamsterRosterItem;

public class FragmentPeople extends ListFragment {

	FragmentPeopleAdapter adapter;
	
    @Override
	public void onCreate(Bundle savedInstanceState) {
		
    	super.onCreate(savedInstanceState);
		Log.d("BEAMSTER", "FragmentTask onCreate");				
		
		adapter = new FragmentPeopleAdapter(getActivity(), ((ChatActivity)getActivity()).getPeople());
		setListAdapter(adapter);
	}

	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
		Log.d("BEAMSTER", "FragmentPeople onCreateView");				
        
		View rootView = inflater.inflate(R.layout.fragment_people_list, container, false);        
				
        return rootView;
	}
	
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        
		Log.d("BEAMSTER", "FragmentPeople onActivityCreated");				
    }    
    	
    
    
	@Override
	public void onDestroyView() {
		super.onDestroyView();
		Log.d("BEAMSTER", "FragmentPeople onDestroyView");				

	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		Log.d("BEAMSTER", "FragmentPeople onAttach");				

	}

	@Override
	public void onDestroy() {
		super.onDestroy();		
		Log.d("BEAMSTER", "FragmentPeople onDestroy");				

	}

	@Override
	public void onDetach() {
		super.onDetach();
		Log.d("BEAMSTER", "FragmentPeople onDetach");				

	}

	@Override
	public void onLowMemory() {
		super.onLowMemory();
		Log.d("BEAMSTER", "FragmentPeople onLowMemory");				

	}

	@Override
	public void onPause() {
		super.onPause();
		Log.d("BEAMSTER", "FragmentPeople onPause");						
	}

	@Override
	public void onResume() {
		super.onResume();		
		Log.d("BEAMSTER", "FragmentPeople onResume");				
	}

	@Override
	public void onStart() {
		super.onStart();
		Log.d("BEAMSTER", "FragmentPeople onStart");				
	}

	@Override
	public void onStop() {
		super.onStop();
		Log.d("BEAMSTER", "FragmentPeople onStop");				

	}
   
	@Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);

        ((FragmentPeopleAdapter) adapter).setSelectedPosition( position );    

        ((ChatActivity)getActivity()).setSelectedPerson(position);    
        
        final BeamsterRosterItem p = ((ChatActivity)getActivity()).getPeople().get(position);
        
    	// check if it is not me
    	if (!p.getJid().equals(((ChatActivity)getActivity()).getMyBeamsterUserProfile().getUserName()))
    	{
        	// Add Dialog
    		DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
    		    @Override
    		    public void onClick(DialogInterface dialog, int which) {
    		        switch (which){
    		        case DialogInterface.BUTTON_POSITIVE:
    		        	// close the list
    		        	//Yes button clicked
    		        	((ChatActivity)getActivity()).addTab(p.getJid(), p.getName(), p.getPictureUrl(), true, true);
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
        			if (p.getJid().equals(myBeamsterRosterItem.getJid()))
        			{
        				found = true;
            			break;
        			} // end if			            						            			
        		} // end while

        		if (found)
        		{
            		
            		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            		builder.setMessage(getActivity().getString(R.string.dialog_startPrivateChannel, p.getName()))
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
        
        
    }	
    
}