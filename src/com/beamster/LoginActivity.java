package com.beamster;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import me.channel16.lmr.R;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.FragmentManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender.SendIntentException;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.beamster.AppConfig.TrackerName;
import com.beamster.android_api.BeamsterAPI;
import com.beamster.android_api.BeamsterSecurityException;
import com.beamster.android_api.BeamsterUser;
import com.beamster.android_api.BeamsterUserProfile;
import com.beamster.settings.AboutDialogFragment;
import com.beamster.settings.HelpTranslateDialogFragment;
import com.beamster.settings.PrivacyPolicyDialogFragment;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.plus.People;
import com.google.android.gms.plus.People.LoadPeopleResult;
import com.google.android.gms.plus.Plus;
import com.google.android.gms.plus.model.people.Person;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;

/**
 * Activity which displays a login screen to the user, offering registration as
 * well.
 */
public class LoginActivity extends Activity implements Button.OnClickListener, ConnectionCallbacks, OnConnectionFailedListener,
ResultCallback<People.LoadPeopleResult> {

	private AnonymousUser myAnonymousUser = null;
	private ProgressDialog authDialog = null;
    private BeamsterUserProfile myBeamsterUserProfile = null;
    private Button emailSignInButton = null, anonymousSignInButton = null;
	final int RQS_GooglePlayServices = 1;
	boolean directLogin = false;
	
	// for FB Login
    private final String PENDING_ACTION_BUNDLE_KEY = "com.beamster:PendingAction";

    private PendingAction pendingAction = PendingAction.NONE;

    // FB variable
    /*
    private GraphUser fbUser;
    private UiLifecycleHelper uiHelper;
    private LoginButton loginButton;
*/
    private enum PendingAction {
        NONE,
        LOGIN_BEAMSTER
    }

	// for FB Login />

	// for G+ Login
    private Person gPlusUser = null;
    private static final int STATE_DEFAULT = 0;
    private static final int STATE_SIGN_IN = 1;
    private static final int STATE_IN_PROGRESS = 2;

    private static final int RC_SIGN_IN = 0;

    private static final int DIALOG_PLAY_SERVICES_ERROR = 0;

    private static final String SAVED_PROGRESS = "sign_in_progress";
    
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
    
    // Used to store the PendingIntent most recently returned by Google Play
    // services until the user clicks 'sign in'.
    private PendingIntent mSignInIntent;
    
    // Used to store the error code most recently returned by Google Play services
    // until the user clicks 'sign in'.
    private int mSignInError;
    
    private SignInButton mSignInButton;
	// for G+ Login />
    
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        Log.i("BEAMSTER", "4-onCreate");            

        // make button clickable
        getActionBar().setHomeButtonEnabled(true);        
        
        PackageInfo pInfo;
		try {
			pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
	        AppConfig.versionCode = pInfo.versionCode;        
	        AppConfig.versionName = pInfo.versionName;        
		} catch (NameNotFoundException e1) {
			try
			{
				((AppConfig)getApplication()).trackException(17, e1);
			}
			catch (Exception e2)
			{
    	        Log.e("BEAMSTER", "Failed to report exception 17"+e2);	    			            		    				
			}
				
			Log.e("BEAMSTER", "NameNotFoundException: "+e1);
		}
        
		Intent intent = getIntent();
        
		// set directLogin - set when loggedout, such that we sign in right away
		directLogin = intent.getBooleanExtra("com.beamster.directLogin", true);  

		/* fb code
        uiHelper = new UiLifecycleHelper(this, callback);
        uiHelper.onCreate(savedInstanceState);
*/
        if (savedInstanceState != null) {
            String name = savedInstanceState.getString(PENDING_ACTION_BUNDLE_KEY);
            pendingAction = PendingAction.valueOf(name);
        }
		
		setContentView(R.layout.activity_login);	
				
	    DisplayImageOptions defaultOptions = new DisplayImageOptions.Builder()
        .cacheInMemory(true)
        .cacheOnDisc(true)
        .build();	    
	    
	    ImageLoaderConfiguration config = new ImageLoaderConfiguration.Builder(this)
        .defaultDisplayImageOptions(defaultOptions)
        .build();

	    ImageLoader.getInstance().init(config); // Do it on Application start	    

		Log.i("BEAMSTER", "Init: ImageLoader configured.");
	    // *** /INIT ***
		
		this.emailSignInButton = (Button) findViewById(R.id.email_sign_in_button);
		this.emailSignInButton.setOnClickListener(this);			
		
		this.anonymousSignInButton = (Button) findViewById(R.id.anonymous_sign_in_button);
		this.anonymousSignInButton.setOnClickListener(this);	
		
	    // FB Login Code
/*
        loginButton = (LoginButton) findViewById(R.id.fb_login_button);
        loginButton.setUserInfoChangedCallback(new LoginButton.UserInfoChangedCallback() {
            @Override
            public void onUserInfoFetched(GraphUser user) 
            {
    			Log.d("BEAMSTER", "5-onUserInfoFetched called: "+user);
    			LoginActivity.this.fbUser = user;
    			if (user!=null)
    			{
    				// set previous users to null
    				myAnonymousUser = null; 
    				if (directLogin)
    				{
    					loginFacebook();    					
    				}
    				else
    				{
        				LoginActivity.this.directLogin = true;
    				}
    				
        			updateUI();
                    // It's possible that we were waiting for this.user to be populated in order to post a
                    // status update.
                    handlePendingAction();
    			}
    			
            }
            
        });
*/
        // FB Login Code />
        
        // G+ Login Code

        mSignInButton = (SignInButton) findViewById(R.id.google_plus_sign_in_button);
        mSignInButton.setOnClickListener(this);
        
        if (savedInstanceState != null) {
          mSignInProgress = savedInstanceState
              .getInt(SAVED_PROGRESS, STATE_DEFAULT);
        }
        
        mGoogleApiClient = buildGoogleApiClient();
        
        // G+ Login Code />
         		
        TextView privacyPolicy = (TextView) findViewById(R.id.textview_privacy_policy);
        
        // Get tracker.
        Tracker t = ((AppConfig) this.getApplication()).getTracker(TrackerName.APP_TRACKER);

        // Set screen name.
        t.setScreenName("LoginActivity");
        t.setAppVersion(AppConfig.versionName+"-"+AppConfig.versionCode);
        t.setAppName("Channel16.me");

        // Send a screen view.
        t.send(new HitBuilders.AppViewBuilder().build());        
        
	} // end on create
	
    // G+ Login Code
	private GoogleApiClient buildGoogleApiClient() 
	{
	    // When we build the GoogleApiClient we specify where connected and
	    // connection failed callbacks should be returned, which Google APIs our
	    // app uses and which OAuth 2.0 scopes our app requests.
	    return new GoogleApiClient.Builder(this)
	        .addConnectionCallbacks(this)
	        .addOnConnectionFailedListener(this)
	        .addApi(Plus.API, null)
	        .addScope(Plus.SCOPE_PLUS_LOGIN)
	        .build();
	}
	// G+ Login Code />


    // FB Login Code
    /*
    private Session.StatusCallback callback = new Session.StatusCallback() {
        @Override
        public void call(Session session, SessionState state, Exception exception) {
            onSessionStateChange(session, state, exception);
            Log.i("BEAMSTER", "1-call");            
        }
    };

    private FacebookDialog.Callback dialogCallback = new FacebookDialog.Callback() {
        @Override
        public void onError(FacebookDialog.PendingCall pendingCall, Exception error, Bundle data) {
            Log.e("BEAMSTER", String.format("Error: %s", error.toString()));
            Log.i("BEAMSTER", "3-error");            
        }

        @Override
        public void onComplete(FacebookDialog.PendingCall pendingCall, Bundle data) {
            Log.i("BEAMSTER", "2-success");            
        }
    };
   
    
    private void onSessionStateChange(Session session, SessionState state, Exception exception) {
        Log.i("BEAMSTER", "10-onSessionStateChange");            
        if (pendingAction != PendingAction.NONE &&
                (exception instanceof FacebookOperationCanceledException ||
                exception instanceof FacebookAuthorizationException)) {
        		try
        		{
        			((AppConfig)getApplication()).trackException(19, exception);
    			}
    			catch (Exception e2)
    			{
        	        Log.e("BEAMSTER", "Failed to report exception 19"+e2);	    			            		    				
    			}

                new AlertDialog.Builder(this)
                    .setTitle(R.string.cancelled)
                    .setMessage(R.string.permission_not_granted)
                    .setPositiveButton(R.string.button_ok, null)
                    .show();
            pendingAction = PendingAction.NONE;
        } else if (state == SessionState.OPENED_TOKEN_UPDATED) {
            handlePendingAction();
        }
        updateUI();
    }

    private void updateUI() {
        Log.i("BEAMSTER", "11-updateUI");            
        Session session = Session.getActiveSession();
        boolean enableButtons = (session != null && session.isOpened());
    }

    @SuppressWarnings("incomplete-switch")
    private void handlePendingAction() {
        Log.i("BEAMSTER", "12-handlePendingAction");            
        PendingAction previouslyPendingAction = pendingAction;
        // These actions may re-set pendingAction if they are still pending, but we assume they
        // will succeed.
        pendingAction = PendingAction.NONE;

        switch (previouslyPendingAction) {
            case LOGIN_BEAMSTER:
            	loginFacebook();
                break;
        }
    }
*/	    
    // FB Login Code />

    @Override
    protected void onStart() {
      super.onStart();
      mGoogleApiClient.connect();
    }

    @Override
    protected void onStop() {
      super.onStop();

      if (mGoogleApiClient.isConnected()) {
        mGoogleApiClient.disconnect();
      }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Log.i("BEAMSTER", "6-onSaveInstanceState");            
//        uiHelper.onSaveInstanceState(outState); // FB

        outState.putString(PENDING_ACTION_BUNDLE_KEY, pendingAction.name()); // FB

        outState.putInt(SAVED_PROGRESS, mSignInProgress); // G+
    }

    
    
    @Override
    public void onPause() {
        super.onPause();
        Log.i("BEAMSTER", "7-onPause");            
//        uiHelper.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i("BEAMSTER", "8-onDestroy");            
//        uiHelper.onDestroy();
    }

	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		getMenuInflater().inflate(R.menu.login, menu);
		
  		try
  		{
	        // enable translation menu, if new language has been detected
	        String isoL = Locale.getDefault().getISO3Language();        
	        Set<String> languages = new HashSet<String>(Arrays.asList(AppConfig.translatedLanguages));
	        if (!languages.contains(isoL))
	        {
	    		MenuItem item = menu.findItem(R.id.help_translate);  
	    		item.setVisible(true);
	//    		invalidateOptionsMenu(); // thisabled this pbre20140417
	        }
	        else
	        {
	    		MenuItem item = menu.findItem(R.id.help_translate);  
	    		item.setVisible(false);
	//    		invalidateOptionsMenu(); // thisabled this pbre20140417
	        }
  		}
  		catch (Exception e)
  		{
  			try
  			{
  				((AppConfig)getApplication()).trackException(301, e);
			}
			catch (Exception e2)
			{
    	        Log.e("BEAMSTER", "Failed to report exception 301"+e2);	    			            		    				
			}

			Log.e("BEAMSTER", "onCreateOptionsMenu: "+e);  			
  		}
        
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    // Handle item selection
    	FragmentManager fm = getFragmentManager();

	    switch (item.getItemId()) {       
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
	
	@Override
	public void onClick(View v) {
		
		switch(v.getId())
		{
			case R.id.google_plus_sign_in_button:
			    if (!mGoogleApiClient.isConnecting()) {
			        // We only process button clicks when GoogleApiClient is not transitioning
			        // between connected and not connected.
			        switch (v.getId()) {
			            case R.id.google_plus_sign_in_button:
			              resolveSignInError();
			              break;
			        }
			      }
				break;
			case R.id.anonymous_sign_in_button:
				loginAnonymous();				
				break;
			case R.id.email_sign_in_button:
		    	Intent intent = new Intent(LoginActivity.this, LoginActivityEmail.class); 
	            startActivity(intent);
				break;
				
	        case R.id.textview_privacy_policy:
	    	    // Handle item selection
	        	FragmentManager fm = getFragmentManager();
	        	PrivacyPolicyDialogFragment myPrivacyPolicyDialogFragment = new PrivacyPolicyDialogFragment();
	        	myPrivacyPolicyDialogFragment.setRetainInstance(true);
	        	myPrivacyPolicyDialogFragment.show(fm, "privacy_policy");
	        	break;
		}
		
	}

	  /* onConnected is called when our Activity successfully connects to Google
	   * Play services.  onConnected indicates that an account was selected on the
	   * device, that the selected account has granted any requested permissions to
	   * our app and that we were able to establish a service connection to Google
	   * Play services.
	   */
	  @Override
	  public void onConnected(Bundle connectionHint) {
	    // Reaching onConnected means we consider the user signed in.
	    Log.i("BEAMSTER", "onConnected");
	    
	    // Update the user interface to reflect that the user is signed in.
	    mSignInButton.setEnabled(false);
	    
	    // Retrieve some profile information to personalize our app for the user.
	    gPlusUser = Plus.PeopleApi.getCurrentPerson(mGoogleApiClient);
	    if (gPlusUser!=null && gPlusUser.hasImage())
	    	Log.d("BEAMSTER","Google+ icon: "+gPlusUser.getImage());
	    
	    Plus.PeopleApi.loadVisible(mGoogleApiClient, null)
	        .setResultCallback(this);
	    
	    // Indicate that the sign in process is complete.
	    mSignInProgress = STATE_DEFAULT;
	    
		if (directLogin)
		{
		    loginGooglePlus();
		}
		else
		{
			LoginActivity.this.directLogin = true;
		}	    
	  }

	  /* onConnectionFailed is called when our Activity could not connect to Google
	   * Play services.  onConnectionFailed indicates that the user needs to select
	   * an account, grant permissions or resolve an error in order to sign in.
	   */
	  @Override
	  public void onConnectionFailed(ConnectionResult result) {
	    // Refer to the javadoc for ConnectionResult to see what error codes might
	    // be returned in onConnectionFailed.
	    Log.i("BEAMSTER", "onConnectionFailed: ConnectionResult.getErrorCode() = "
	        + result.getErrorCode());
	    
	    if (mSignInProgress != STATE_IN_PROGRESS) {
	      // We do not have an intent in progress so we should store the latest
	      // error resolution intent for use when the sign in button is clicked.
	      mSignInIntent = result.getResolution();
	      mSignInError = result.getErrorCode();
	      
	      if (mSignInProgress == STATE_SIGN_IN) {
	        // STATE_SIGN_IN indicates the user already clicked the sign in button
	        // so we should continue processing errors until the user is signed in
	        // or they click cancel.
	        resolveSignInError();
	      }
	    }
	    
	    // In this sample we consider the user signed out whenever they do not have
	    // a connection to Google Play services.
	    onSignedOut();
	  }
	  
	  /* Starts an appropriate intent or dialog for user interaction to resolve
	   * the current error preventing the user from being signed in.  This could
	   * be a dialog allowing the user to select an account, an activity allowing
	   * the user to consent to the permissions being requested by your app, a
	   * setting to enable device networking, etc.
	   */
	  private void resolveSignInError() {
	    if (mSignInIntent != null) {
	      // We have an intent which will allow our user to sign in or
	      // resolve an error.  For example if the user needs to
	      // select an account to sign in with, or if they need to consent
	      // to the permissions your app is requesting.

	      try {
	        // Send the pending intent that we stored on the most recent
	        // OnConnectionFailed callback.  This will allow the user to
	        // resolve the error currently preventing our connection to
	        // Google Play services.  
	        mSignInProgress = STATE_IN_PROGRESS;
	        startIntentSenderForResult(mSignInIntent.getIntentSender(),
	            RC_SIGN_IN, null, 0, 0, 0);
	      } catch (SendIntentException e) {
	    	  try
	    	  {
	    		  ((AppConfig)getApplication()).trackException(21, e);
	  			}
	  			catch (Exception e2)
	  			{
	      	        Log.e("BEAMSTER", "Failed to report exception 21"+e2);	    			            		    				
	  			}

	    	  Log.i("BEAMSTER", "Sign in intent could not be sent: "
	            + e.getLocalizedMessage());
	    	  // The intent was canceled before it was sent.  Attempt to connect to
	    	  // get an updated ConnectionResult.
	    	  mSignInProgress = STATE_SIGN_IN;
	    	  mGoogleApiClient.connect();
	      }
	    } else {
	      // Google Play services wasn't able to provide an intent for some
	      // error types, so we show the default Google Play services error
	      // dialog which may still start an intent on our behalf if the
	      // user can resolve the issue.
	      showDialog(DIALOG_PLAY_SERVICES_ERROR);
	    }  
	  }
	  
	  @Override
	  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		  
		  // FB
		  super.onActivityResult(requestCode, resultCode, data);
		  Log.i("BEAMSTER", "6-onActivityResult");            
//		  uiHelper.onActivityResult(requestCode, resultCode, data, dialogCallback);
		  
		  // G+
		  switch (requestCode) {
		      case RC_SIGN_IN:
		        if (resultCode == RESULT_OK) {
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
	    
	  @Override
	  public void onResult(LoadPeopleResult peopleData) {
	  }

	  private void onSignedOut() {
	    // Update the UI to reflect that the user is signed out.
	    mSignInButton.setEnabled(true);
	  }

	  @Override
	  public void onConnectionSuspended(int cause) {
	    // The connection to Google Play services was lost for some reason.
	    // We call connect() to attempt to re-establish the connection or get a
	    // ConnectionResult that we can attempt to resolve.
	    mGoogleApiClient.connect();
	  }

	  @Override
	  protected Dialog onCreateDialog(int id) {
	    switch(id) {
	      case DIALOG_PLAY_SERVICES_ERROR:
	        if (GooglePlayServicesUtil.isUserRecoverableError(mSignInError)) {
	          return GooglePlayServicesUtil.getErrorDialog(
	              mSignInError,
	              this,
	              RC_SIGN_IN, 
	              new DialogInterface.OnCancelListener() {
	                @Override
	                public void onCancel(DialogInterface dialog) {
	                  Log.e("BEAMSTER", "Google Play services resolution cancelled");
	                  mSignInProgress = STATE_DEFAULT;
	                }
	              });
	        } else {
	          return new AlertDialog.Builder(this)
	              .setMessage(R.string.play_services_error)
	              .setPositiveButton(R.string.close_button,
	                  new DialogInterface.OnClickListener() {
	                    @Override
	                    public void onClick(DialogInterface dialog, int which) {
	                      Log.e("BEAMSTER", "Google Play services error could not be "
	                          + "resolved: " + mSignInError);
	                      mSignInProgress = STATE_DEFAULT;
	                    }
	                  }).create();
	        }
	      default:
	        return super.onCreateDialog(id);
	    }
	  }
	
	private void loginGooglePlus()
	{
		try
		{	
			// set others to null		
			myAnonymousUser = null;
//			fbUser = null;
			
	        Log.i("BEAMSTER", "13-loginG+");            
	    	// yes, we are logged into FB
	    	authDialog = ProgressDialog.show(LoginActivity.this, getString(R.string.authenticating), getString(R.string.please_wait), false);
			authDialog.show();				
			
			// set user id and password in the Beamster API as soon as we are logged in with FB
			String username = "g_"+gPlusUser.getId();
			BeamsterAPI.getInstance().setUsername(username);
			BeamsterAPI.getInstance().setPassword(md5(username+AppConfig.secret));
			        			
			// Create user
		    BeamsterAPI.getInstance().createUser(BeamsterAPI.getInstance().getUsername(), BeamsterAPI.getInstance().getPassword(), myCreateUserCallback);
		    
	        Tracker t = ((AppConfig)getApplication()).getTracker(
		            TrackerName.APP_TRACKER);
		        // Build and send an Event.
		        t.send(new HitBuilders.EventBuilder()
		            .setCategory("Login")
		            .setAction("LoginAccount") // actionId
		            .setLabel("GOOGLEPLUS") 
		            .build());	
	   
		}
		catch(Exception e)
		{
	        Log.e("BEAMSTER", "Failed login G+ 400"+e);	    			            		    				
			try
			{
				((AppConfig)getApplication()).trackException(400, e);
			}
			catch (Exception e2)
			{
    	        Log.e("BEAMSTER", "Failed login G+ 400"+e2);	    			            		    				
			}			
		}
	}	
	
	
	private void loginFacebook()
	{
		try
		{
			// set others to null
			gPlusUser = null;
			myAnonymousUser = null;
			
	        Log.i("BEAMSTER", "13-loginFacebook");            
	    	// yes, we are logged into FB
	    	authDialog = ProgressDialog.show(LoginActivity.this, getString(R.string.authenticating), getString(R.string.please_wait), false);
	    	
			authDialog.show();				
			
			// set user id and password in the Beamster API as soon as we are logged in with FB
			/*
			String username = "fb_"+fbUser.getId();
			BeamsterAPI.getInstance().setUsername(username);
			BeamsterAPI.getInstance().setPassword(md5(username+AppConfig.secret));
			        			
			// Create user
		    BeamsterAPI.getInstance().createUser(BeamsterAPI.getInstance().getUsername(), BeamsterAPI.getInstance().getPassword(), myCreateUserCallback);
		    
	        Tracker t = ((AppConfig)getApplication()).getTracker(
		            TrackerName.APP_TRACKER);
		        // Build and send an Event.
		        t.send(new HitBuilders.EventBuilder()
		        	.setCategory("Login")
		        	.setAction("LoginAccount") // actionId
		            .setLabel("FACEBOOK") 
		            .build());	
		            */
		}
		catch(Exception e)
		{
	        Log.e("BEAMSTER", "Failed login FB 401"+e);	    			            		    				
			try
			{
				((AppConfig)getApplication()).trackException(400, e);
			}
			catch (Exception e2)
			{
		        Log.e("BEAMSTER", "Failed login FB 401"+e2);	    			            		    				
			}			
		}	        
		    
	}	

	private void loginAnonymous()
	{
		try
		{
			// set others to null		
			gPlusUser = null;
			//fbUser = null;
			
	    	// yes, we are logged into FB
	    	authDialog = ProgressDialog.show(LoginActivity.this, getString(R.string.authenticating), getString(R.string.please_wait), false);    	
			authDialog.show();				
			
			String nickname = "Anonymous";
			int randomnumber=(int)(10000+Math.floor(Math.random()*100000));		    					
			String username = "a_"+randomnumber;
			
			myAnonymousUser = new AnonymousUser(username, nickname, randomnumber); 
	
			// set user id and password in the Beamster API as soon as we are logged in with FB
			BeamsterAPI.getInstance().setUsername(username);
			BeamsterAPI.getInstance().setPassword(myAnonymousUser.getPw());
			
			// Create user
		    BeamsterAPI.getInstance().createUser(BeamsterAPI.getInstance().getUsername(), BeamsterAPI.getInstance().getPassword(), myCreateUserCallback);		
		    
	        Tracker t = ((AppConfig)getApplication()).getTracker(
		            TrackerName.APP_TRACKER);
		        // Build and send an Event.
		        t.send(new HitBuilders.EventBuilder()
		        	.setCategory("Login")
		        	.setAction("LoginAccount") // actionId
		            .setLabel("ANONYMOUS") 
		            .build());	
		}
		catch(Exception e)
		{
	        Log.e("BEAMSTER", "Failed login A 402"+e);	    			            		    				
			try
			{
				((AppConfig)getApplication()).trackException(400, e);
			}
			catch (Exception e2)
			{
		        Log.e("BEAMSTER", "Failed login A 402"+e2);	    			            		    				
			}			
		}	 	    
	}	

	@Override
	protected void onResume() {
		super.onResume();
        Log.i("BEAMSTER", "14-onResume");            
		
	    // *** INIT ***
    	// make sure connected exists already - after resume
    	if (BeamsterAPI.getInstance()!=null && BeamsterAPI.getInstance().isConnected())
    	{		    		
    		BeamsterAPI.getInstance().destroy();
    	}
        
	    // create a BeamsterAPI singleton
	    try {
			BeamsterAPI.getInstance(getApplicationContext(), AppConfig.api_key, AppConfig.secret, AppConfig.PRODUCTION);
			BeamsterAPI.getInstance().setClientId(AppConfig.CLIENT_ID);
		} catch (BeamsterSecurityException e) {
			try
			{
				((AppConfig)getApplication()).trackException(18, e);
			}
			catch (Exception e2)
			{
    	        Log.e("BEAMSTER", "Failed to report exception 16"+e2);	    			            		    				
			}
			// TODO show error to user
	        Log.e("BEAMSTER", "BeamsterSecurityException 16: "+e);	    			            		    				
		}
	    
	    // Set locale
		Locale currentLocale = getResources().getConfiguration().locale;
		BeamsterAPI.getInstance().setLocale(currentLocale);
		
		Log.i("BEAMSTER", "Init: Locale set to "+currentLocale);
	    
	    
		int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(getApplicationContext());
		  
		  if (resultCode == ConnectionResult.SUCCESS){			 
			  // do nothing
			  //Toast.makeText(getApplicationContext(), "isGooglePlayServicesAvailable SUCCESS", Toast.LENGTH_LONG).show();
		  }else{
			  GooglePlayServicesUtil.getErrorDialog(resultCode, this, RQS_GooglePlayServices);
		  }
		  
		  /*
	    uiHelper.onResume();
	
	    updateUI();
		  		  
		if (directLogin && fbUser!=null)
		{
			loginFacebook();    					
		}*/
	    
	}
	
	/**
	 * This callback is called by the BeamsterAPI after a Beamster user has been created
	 */
	private BeamsterAPI.CreateUserCallback myCreateUserCallback = new BeamsterAPI.CreateUserCallback()
	{
	    public void onSuccess(String message, final String token)
	    {
	        Log.i("BEAMSTER", "15-myCreateUserCallback.onSuccess");            
	    	if (authDialog!=null)
	    		authDialog.dismiss();	    
	    	
	    	if (message==null)
				Log.e("BEAMSTER", "in CreateUserCallback: message==null!!");			
	    	
	    	// if user is new and the user is not an anonymous user, ask if he/she should initially be visible on the map 
	    	if (message.equals("ok") && myAnonymousUser==null)
	    	{
	    		DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
	    		    @Override
	    		    public void onClick(DialogInterface dialog, int which) {
	    		    	
    		            Tracker t = ((AppConfig)getApplication()).getTracker(
    		    	            TrackerName.APP_TRACKER);
	    		    	
	    		        switch (which){
	    		        case DialogInterface.BUTTON_POSITIVE:

	    		    	        // Build and send an Event.
	    		    	        t.send(new HitBuilders.EventBuilder()
	    		    	            .setCategory("Login")
	    		    	            .setAction("setInitialVisibilityHideOnMap=1") // actionId
	    		    	            .setLabel(gPlusUser!=null?"GOOGLEPLUS":"ANONYMOUS") 
	    		    	            .build());	
	    		        	
	    		        	//Yes button clicked	    		        	
	    		        	createOrLoadProfile(token, false);
	    		            break;

	    		        case DialogInterface.BUTTON_NEGATIVE:

	    		    	        // Build and send an Event.
	    		    	        t.send(new HitBuilders.EventBuilder()
	    		    	            .setCategory("Login")
	    		    	            .setAction("setInitialVisibilityHideOnMap=0") // actionId
	    		    	            .setLabel(gPlusUser!=null?"GOOGLEPLUS":"ANONYMOUS") 
	    		    	            .build());	
	    		        	
	    		        	
	    		        	//No button clicked
	    		        	createOrLoadProfile(token, true);
	    		            break;
	    		        }
	    		    }
	    		};

	    		AlertDialog.Builder builder = new AlertDialog.Builder(LoginActivity.this);
	    		builder.setMessage(R.string.location_questions)
	    		    .setNegativeButton(R.string.no, dialogClickListener)
	    		    .setPositiveButton(R.string.yes, dialogClickListener).show();	    		
	    	}
	    	else
	    	{
	    		createOrLoadProfile(token, false);
	    	}
	    	
			Log.d("BEAMSTER", "in CreateUserCallback");			
	    }

	    public void onError(String error)
	    {
	    	
	    	if (authDialog!=null)
	    		authDialog.dismiss();	    	 
				    		    	
            Toast.makeText(getApplicationContext(), getString(R.string.no_internet_connection), Toast.LENGTH_LONG).show();   
	    }
	};	
	
	private void createOrLoadProfile(String token, boolean hideOnMap)
	{
    	if (gPlusUser!=null)
    	{
	    	// get or create default profile ...
			BeamsterAPI.getInstance().getProfile("GOOGLEPLUS",
					gPlusUser.getId(), 
					gPlusUser.getDisplayName(),
					(gPlusUser.getGender()==0?"m":"w"),
					new Locale(gPlusUser.getLanguage()).toString(),
					gPlusUser.getUrl(),
					gPlusUser.getImage().getUrl(),
					BeamsterUser.PRIVACY_SHOW_FIRSTNAME, // privacy default
					false, // showfbfriendsonly 
					false, // blockallanonymoususers 
					true, // blockbeamedusers 
					hideOnMap, // hideonmap 
					0f, // locationblurring meters 
					"", // statusmessage 
					"", // TODO friends
					token,
					LoginActivity.this.myGetProfileCallback);   	    		    		
    	}
    	else if (myAnonymousUser!=null)
    	{
	    	// get or create default profile ...
			BeamsterAPI.getInstance().getProfile("ANONYMOUS",
					myAnonymousUser.getUsername(), 
					myAnonymousUser.getFullname(),
					myAnonymousUser.getGender(),
					myAnonymousUser.getLocale(),
					myAnonymousUser.getLink(),
					myAnonymousUser.getPictureurl(),
					BeamsterUser.PRIVACY_ANONYMOUS, // privacy default
					false, // showfbfriendsonly 
					false, // blockallanonymoususers 
					true, // blockbeamedusers 
					hideOnMap, // hideonmap 
					0f, // locationblurring meters 
					"", // statusmessage 
					"", // TODO friends
					token,
					LoginActivity.this.myGetProfileCallback);   	    		    		
    	}
		
	}
	
    /**
     * This callback is called by the BeamsterAPI after all users have been loaded
     */
	private BeamsterAPI.GetProfileCallback myGetProfileCallback = new BeamsterAPI.GetProfileCallback()
	{

		@Override
		public void onSuccess(BeamsterUserProfile profile) {

			// TODO save this in a profile option
			Log.d("BEAMSTER", "GetProfileCallback success!");
			myBeamsterUserProfile = profile;					
			
			// for new users, show the Settings dialog here
			Intent intent = new Intent(LoginActivity.this, ChatActivity.class);
			
			// this is actually not needed, because user name is already stored in BeamsterAPI Singleton
			intent.putExtra("beamsterUsername", BeamsterAPI.getInstance().getUsername());
			intent.putExtra("beamsterPassword", BeamsterAPI.getInstance().getPassword());
			intent.putExtra("com.beamster.beamed", false);
			intent.putExtra("com.beamster.BeamsterAPI.BeamsterUserProfile", myBeamsterUserProfile);
			
			startActivity(intent);		        					    								
		}

		@Override
		public void onError(String error) {
			Log.e("BEAMSTER", error);
            Toast.makeText(getApplicationContext(), getString(R.string.no_internet_connection), Toast.LENGTH_LONG).show();   
			
		}
	    
	};		
	
	public String md5(String plaintext)
	{
		try
		{
			MessageDigest m = MessageDigest.getInstance("MD5");
			m.reset();
			m.update(plaintext.getBytes());
			byte[] digest = m.digest();
			BigInteger bigInt = new BigInteger(1,digest);
			String hashtext = bigInt.toString(16);
			// Now we need to zero pad it if you actually want the full 32 chars.
			while(hashtext.length() < 32 ){
			  hashtext = "0"+hashtext;
			}
			return hashtext;
		}
		catch (Exception e)
		{
			try
			{
				((AppConfig)getApplication()).trackException(22, e);
			}
			catch (Exception e2)
			{
    	        Log.e("BEAMSTER", "Failed to report exception 22"+e2);	    			            		    				
			}

	        Log.e("BEAMSTER", "MD5 Exception 22: "+e);	    			            		    				
		}
		return "";
	}	
	
	
}

class AnonymousUser
{
	String username = "";
	String nickname = "Anonymous";
	String pw = "private";
	String fullname = "";
	String gender = "m";
	String locale = "en_US";
	String link = "http://www.google.com";
	String pictureurl = "nourl";

	AnonymousUser(String username, String nickname, int randomNumber)
	{
		this.username = username;
		this.nickname = nickname;
				
		if (nickname!=null && nickname.trim()!="")
			fullname = nickname;
		else						
			fullname = "Anonymous "+randomNumber;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getNickname() {
		return nickname;
	}

	public void setNickname(String nickname) {
		this.nickname = nickname;
	}

	public String getPw() {
		return pw;
	}

	public void setPw(String pw) {
		this.pw = pw;
	}

	public String getFullname() {
		return fullname;
	}

	public void setFullname(String fullname) {
		this.fullname = fullname;
	}

	public String getGender() {
		return gender;
	}

	public void setGender(String gender) {
		this.gender = gender;
	}

	public String getLocale() {
		return locale;
	}

	public void setLocale(String locale) {
		this.locale = locale;
	}

	public String getLink() {
		return link;
	}

	public void setLink(String link) {
		this.link = link;
	}

	public String getPictureurl() {
		return pictureurl;
	}

	public void setPictureurl(String pictureurl) {
		this.pictureurl = pictureurl;
	}
	

}