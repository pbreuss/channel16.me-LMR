package com.beamster.photo;

import me.channel16.lmr.R;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.ProgressDialog;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.InflateException;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;

import com.beamster.AppConfig;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.assist.SimpleImageLoadingListener;
 
public class PhotoDialogFragment extends DialogFragment {
 
	private static View view;

    public PhotoDialogFragment() {
		super();
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
            view = inflater.inflate(R.layout.photo_dialog, container, false);
            ImageView picture = (ImageView) view.findViewById(R.id.detail_photo);
            
            String url = getArguments().getString("url");    
            final ProgressDialog spinner = ProgressDialog.show(getActivity(), "", "");				
			ImageLoader.getInstance().displayImage(url, picture, new SimpleImageLoadingListener() {
			    @Override
			    public void onLoadingStarted(String imageUri, View view) {
			        spinner.show();
			    }

			    @Override
			    public void onLoadingFailed(String imageUri, View view, FailReason failReason) {
			        spinner.dismiss();
			    }

			    @Override
			    public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
			        spinner.dismiss();
			    }
			});
        } 
        catch (InflateException e) 
        {
        	try
        	{
        		((AppConfig)getActivity().getApplication()).trackException(51, e);			
			}
			catch (Exception e2)
			{
    	        Log.e("BEAMSTER", "Failed to report exception 51"+e2);	    			            		    				
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