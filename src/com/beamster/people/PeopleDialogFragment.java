package com.beamster.people;

import me.channel16.lmr.R;
import android.app.Dialog;
import android.app.DialogFragment;
import android.os.Bundle;
import android.util.Log;
import android.view.InflateException;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;

import com.beamster.AppConfig;
 
public class PeopleDialogFragment extends DialogFragment {
 
	private static View view;
 
    public PeopleDialogFragment() {
    }
 
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
    	
    	if (view != null) 
    	{
            ViewGroup parent = (ViewGroup) view.getParent();
            if (parent != null)
                parent.removeView(view);
        }

    	try 
        {
            view = inflater.inflate(R.layout.people_dialog, container, false);
        } 
        catch (InflateException e) 
        {
        	try
        	{
        		((AppConfig)getActivity().getApplication()).trackException(70, e);			
			}
			catch (Exception e2)
			{
    	        Log.e("BEAMSTER", "Failed to report exception 70"+e2);	    			            		    				
			}

        	/* map is already there, just return view as it is */
        }
        return view;
    }

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		  Dialog dialog = super.onCreateDialog(savedInstanceState);

		  // request a window without the title
		  dialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
		  return dialog;
	}
    
    
}