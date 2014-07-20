package com.beamster.settings;

import me.channel16.lmr.R;

import org.jivesoftware.smack.packet.Presence.Type;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.InflateException;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;

import com.beamster.AppConfig;
import com.beamster.AppConfig.TrackerName;
import com.beamster.android_api.BeamsterAPI;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.plus.People;
import com.google.android.gms.plus.People.LoadPeopleResult;
import com.google.android.gms.plus.Plus;
import com.google.android.gms.plus.model.people.Person;
import com.google.android.gms.plus.model.people.PersonBuffer;

public class AccountsDialogFragment extends DialogFragment implements ConnectionCallbacks, OnConnectionFailedListener,
ResultCallback<People.LoadPeopleResult>, View.OnClickListener  {

	private static final int STATE_DEFAULT = 0;
	private static final int STATE_SIGN_IN = 1;
	private static final int STATE_IN_PROGRESS = 2;

	private static final int RC_SIGN_IN = 0;

	private static final int DIALOG_PLAY_SERVICES_ERROR = 0;

	private static final String SAVED_PROGRESS = "sign_in_progress";
	
	private static View view;
	
	
	// GOOGLEPLUS
	private Button mSignOutButton;
	private Button mRevokeButton;
	private TextView mStatus, mIdTypeTextView;

	// GoogleApiClient wraps our service connection to Google Play services and
	// provides access to the users sign in state and Google's APIs.
	private GoogleApiClient mGoogleApiClient;

	  // We use mSignInProgress to track whether user has clicked sign in.
	  // mSignInProgress can be one of three values:
	  //
	  //       STATE_DEFAULT: The default state of the application before the user
	  //                      has clicked 'sign in', or after they have clicked
	  //                      'sign out'.  In this state we will not attempt to
	  //                      resolve sign in errors and so will display our
	  //                      Activity in a signed out state.
	  //       STATE_SIGN_IN: This state indicates that the user has clicked 'sign
	  //                      in', so resolve successive errors preventing sign in
	  //                      until the user has successfully authorized an account
	  //                      for our app.
	  //   STATE_IN_PROGRESS: This state indicates that we have started an intent to
	  //                      resolve an error, and so we should not start further
	  //                      intents until the current intent completes.
	  private int mSignInProgress;
	  private String mIdType = "";
	  
	  // Used to store the error code most recently returned by Google Play services
	  // until the user clicks 'sign in'.
	
	
	public AccountsDialogFragment(String userName) {
		if (userName.startsWith("a_"))
			mIdType = "ANONYMOUS";
		else if (userName.startsWith("fb_"))
			mIdType = "FACEBOOK";
		else if (userName.startsWith("g_"))
			mIdType = "GOOGLEPLUS";
		else
			mIdType = "UNKNOWN";
	}
	
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		Dialog dialog = super.onCreateDialog(savedInstanceState);
		
		// Google Analytics
		Tracker t = ((AppConfig)getActivity().getApplication()).getTracker(TrackerName.APP_TRACKER);

        // Set screen name.
        t.setScreenName("Accounts");

        // Send a screen view.
        t.send(new HitBuilders.AppViewBuilder().build());  
        
		Window window = dialog.getWindow();
		window.setBackgroundDrawableResource(R.color.dialogBackgroundDarkColor);
		window.setTitle(getString(R.string.button_accounts));
		return dialog;
	}
	
	
        
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {

		Log.d("BEAMSTER", "mIdType: "+mIdType);
		
		if (view != null) 
		{
			ViewGroup parent = (ViewGroup) view.getParent();
			if (parent != null)
				parent.removeView(view);
		}

		try 
		{
			view = inflater.inflate(R.layout.accounts_dialog, container, false);
		    mIdTypeTextView = (TextView) view.findViewById(R.id.id_type);
		    mSignOutButton = (Button) view.findViewById(R.id.googleplus_sign_out_button);
		    mRevokeButton = (Button) view.findViewById(R.id.googleplus_revoke_access_button);
		    mStatus = (TextView) view.findViewById(R.id.sign_in_status);

		    mIdTypeTextView.setText(getString(R.string.id_type_text, mIdType));

		    if (mIdType.equals("GOOGLEPLUS"))
		    {
		    	mSignOutButton.setVisibility(View.VISIBLE);
		    	mRevokeButton.setVisibility(View.VISIBLE);
		    	mStatus.setVisibility(View.VISIBLE);
		    	mSignOutButton.setOnClickListener(this);
			    mRevokeButton.setOnClickListener(this);	

			    mGoogleApiClient = buildGoogleApiClient();
		    } 
		} 
		catch (InflateException e) 
		{
			try
			{
				((AppConfig)getActivity().getApplication()).trackException(90, e);			
			}
			catch (Exception e2)
			{
    	        Log.e("BEAMSTER", "Failed to report exception 90"+e2);	    			            		    				
			}

			/* map is already there, just return view as it is */
		}
		return view;
	}

	private GoogleApiClient buildGoogleApiClient() {
	    // When we build the GoogleApiClient we specify where connected and
	    // connection failed callbacks should be returned, which Google APIs our
	    // app uses and which OAuth 2.0 scopes our app requests.
	    return new GoogleApiClient.Builder(getActivity())
	        .addConnectionCallbacks(this)
	        .addOnConnectionFailedListener(this)
	        .addApi(Plus.API, null)
	        .addScope(Plus.SCOPE_PLUS_LOGIN)
	        .build();
	}
	

	  @Override
	  public void onClick(View v) {
		  
	    if (!mGoogleApiClient.isConnecting()) {
	      // We only process button clicks when GoogleApiClient is not transitioning
	      // between connected and not connected.
	      switch (v.getId()) {
	          case R.id.googleplus_sign_out_button:
	        	  
	    		DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
	    		    @Override
	    		    public void onClick(DialogInterface dialog, int which) {
	    		        switch (which){
	    		        case DialogInterface.BUTTON_POSITIVE:
	    		            //Yes button clicked
	    		            // We clear the default account on sign out so that Google Play
	    		            // services will not return an onConnected callback without user
	    		            // interaction.
	    		            Plus.AccountApi.clearDefaultAccount(mGoogleApiClient);
	    		            mGoogleApiClient.disconnect();

	    		            signOut();
	    		            break;

	    		        case DialogInterface.BUTTON_NEGATIVE:
	    		            //No button clicked
	    		            break;
	    		        }
	    		    }
	    		};

	    		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
	    		builder.setMessage(R.string.signOutDialog) 
	    		    .setNegativeButton(R.string.no, dialogClickListener)
	    		    .setPositiveButton(R.string.yes, dialogClickListener).show();
	        	  
	            break;
	          case R.id.googleplus_revoke_access_button:
	        	  
		    		dialogClickListener = new DialogInterface.OnClickListener() {
		    		    @Override
		    		    public void onClick(DialogInterface dialog, int which) {
		    		        switch (which){
		    		        case DialogInterface.BUTTON_POSITIVE:
		    		            // After we revoke permissions for the user with a GoogleApiClient
		    		            // instance, we must discard it and create a new one.
		    		            Plus.AccountApi.clearDefaultAccount(mGoogleApiClient);
		    		            // Our sample has caches no user data from Google+, however we
		    		            // would normally register a callback on revokeAccessAndDisconnect
		    		            // to delete user data so that we comply with Google developer
		    		            // policies.
		    		            Plus.AccountApi.revokeAccessAndDisconnect(mGoogleApiClient);
		    		            mGoogleApiClient = buildGoogleApiClient();
		    		            mGoogleApiClient.connect();
		    		            
		    			        Tracker t = ((AppConfig)getActivity().getApplication()).getTracker(
		    				            TrackerName.APP_TRACKER);
		    				        // Build and send an Event.
		    				        t.send(new HitBuilders.EventBuilder()
		    				            .setCategory("SignOut")
		    				            .setAction("SignOutAccountGPlusRevoke") // actionId
		    				            .build());	
		    		            
		    		            signOut();
		    		            break;

		    		        case DialogInterface.BUTTON_NEGATIVE:
		    		            //No button clicked
		    		            break;
		    		        }
		    		    }
		    		};

		    		builder = new AlertDialog.Builder(getActivity());
		    		builder.setMessage(R.string.signOutDialog)
		    		    .setNegativeButton(R.string.no, dialogClickListener)
		    		    .setPositiveButton(R.string.yes, dialogClickListener).show();
	        	  
	            break;
	      }
	    }
	  }

	  
	@Override
	public void onConnected(Bundle arg0) {
	    Log.i("BEAMSTER", "onConnected");
	    if (mIdType.equals("GOOGLEPLUS"))
	    {
	    
		    // Update the user interface to reflect that the user is signed in.
		    mSignOutButton.setEnabled(true);
		    mRevokeButton.setEnabled(true);
		    
		    // Retrieve some profile information to personalize our app for the user.
		    Person currentUser = Plus.PeopleApi.getCurrentPerson(mGoogleApiClient);
		    
		    if (currentUser.hasImage())
		    	Log.d("BEAMSTER", "");
		    
		    mStatus.setText(String.format(
		        getResources().getString(R.string.signed_in_as),
		        currentUser.getDisplayName()));
	
		    Plus.PeopleApi.loadVisible(mGoogleApiClient, null)
		        .setResultCallback(this);
		    
		    // Indicate that the sign in process is complete.
		    mSignInProgress = STATE_DEFAULT;
	    }
	}

	@Override
	public void onStart() {
		super.onStart();
	    if (mIdType.equals("GOOGLEPLUS"))
	    {
	    	mGoogleApiClient.connect();	
	    }
	}

	@Override
	public void onStop() {
		super.onStop();
	    if (mIdType.equals("GOOGLEPLUS"))
	    {
		    if (mGoogleApiClient.isConnected()) {
		      mGoogleApiClient.disconnect();
		    }
	    }
	}

	  @Override
	  public void onActivityResult(int requestCode, int resultCode,
	      Intent data) {
		    if (mIdType.equals("GOOGLEPLUS"))
		    {
			    switch (requestCode) {
			      case RC_SIGN_IN:
			        if (resultCode == Activity.RESULT_OK) {
			          // If the error resolution was successful we should continue
			          // processing errors.
			          mSignInProgress = STATE_SIGN_IN;
			        } else {
			          // If the error resolution was not successful or the user canceled,
			          // we should stop processing errors.
			          mSignInProgress = STATE_DEFAULT;
			        }
			        
			        if (!mGoogleApiClient.isConnecting()) {
			          // If Google Play services resolved the issue with a dialog then
			          // onStart is not called so we need to re-attempt connection here.
			          mGoogleApiClient.connect();
			        }
			        break;
			    }
		    }
	  }
	    
	  @Override
	  public void onResult(LoadPeopleResult peopleData) 
	  {
		    if (mIdType.equals("GOOGLEPLUS"))
		    {
			    if (peopleData.getStatus().getStatusCode() == CommonStatusCodes.SUCCESS) {
			      PersonBuffer personBuffer = peopleData.getPersonBuffer();
		          personBuffer.close();
		
			    } else {
			      Log.e("BEAMSTER", "Error requesting visible circles: " + peopleData.getStatus());
			    }
		    }
	  }

	  private void onSignedOut() {
		    if (mIdType.equals("GOOGLEPLUS"))
		    {
			    // Update the UI to reflect that the user is signed out.
			    mSignOutButton.setEnabled(false);
			    mRevokeButton.setEnabled(false);
			    
			    mStatus.setText(R.string.status_signed_out);
		    }
	  }

	  @Override
	  public void onConnectionSuspended(int cause) {
		    if (mIdType.equals("GOOGLEPLUS"))
		    {
			    // The connection to Google Play services was lost for some reason.
			    // We call connect() to attempt to re-establish the connection or get a
			    // ConnectionResult that we can attempt to resolve.
			    mGoogleApiClient.connect();
		    }
	  }
	  

	  /* onConnectionFailed is called when our Activity could not connect to Google
	   * Play services.  onConnectionFailed indicates that the user needs to select
	   * an account, grant permissions or resolve an error in order to sign in.
	   */
	  @Override
	  public void onConnectionFailed(ConnectionResult result) {
		    if (mIdType.equals("GOOGLEPLUS"))
		    {

			    // Refer to the javadoc for ConnectionResult to see what error codes might
			    // be returned in onConnectionFailed.
			    Log.i("BEAMSTER", "onConnectionFailed: ConnectionResult.getErrorCode() = "
			        + result.getErrorCode());
			    	    
			    // In this sample we consider the user signed out whenever they do not have
			    // a connection to Google Play services.
			    onSignedOut();
		    }
	  }

		/**
		 * Signs a current user out and move back to MainActivity
		 */
		public void signOut()
		{
	        Tracker t = ((AppConfig)getActivity().getApplication()).getTracker(
		            TrackerName.APP_TRACKER);
		        // Build and send an Event.
		        t.send(new HitBuilders.EventBuilder()
		            .setCategory("SignOut")
		            .setAction("SignOutAccount") // actionId
		            .setLabel(mIdType) 
		            .build());	
			
			
			final ProgressDialog dialog = ProgressDialog.show(getActivity(), getString(R.string.disconnecting), getString(R.string.please_wait), false);

			Thread thread = new Thread(new Runnable() {

				@Override
				public void run() {
						    					    				
					BeamsterAPI.getInstance().sendPresence(Type.unavailable);
					BeamsterAPI.getInstance().destroy();
							    						    				
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						try
						{
							((AppConfig)getActivity().getApplication()).trackException(91, e);									
		    			}
		    			catch (Exception e2)
		    			{
    		    	        Log.e("BEAMSTER", "Failed to report exception 91"+e2);	    			            		    				
		    			}

						Log.d("BEAMSTER", "ChatActivity onOptionsItemSelected InterruptedException: "+e.getMessage());
					}
					dialog.dismiss();		
					
					getActivity().runOnUiThread(new Runnable() {
			            @Override
			            public void run() {
			            	
			            	getActivity().finish();     	
			            	
			            	// go to the home activity
			            	Intent intent = new Intent(Intent.ACTION_MAIN);
			            	intent.addCategory(Intent.CATEGORY_HOME);
			            	intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			            	startActivity(intent);		            	
			            	
			            }
			        });		
						    				
				}
			});
			thread.start();
			dialog.show();		
		}	  
}