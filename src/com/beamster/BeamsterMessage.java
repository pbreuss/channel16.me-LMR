package com.beamster;

import java.util.Calendar;

import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;

/**
 * Message is a Custom Object to encapsulate message information/fields
 * 
 * @author pbreuss
 *
 */
public class BeamsterMessage {
	/**
	 * The content of the message
	 */
	String message;
	/**
	 * boolean to determine, who is sender of this message
	 */
	boolean isMine;
	/**
	 * boolean to determine, whether the message is a status message or not.
	 * it reflects the changes/updates about the sender is writing, have entered text etc
	 */
	public boolean isStatusMessage;

	private boolean composing = false;
    private String username="";
    private String lat="";
    private String lon="";
    private String bearing="";
    private String speed="";
    private String name="";
    private Calendar messagedate=null;
    private double distanceAway=0;
    private boolean beamed=false;
    private String clientId="";
    private String pictureurl="";	
    boolean stillNeedsToBePlayed = false; 
    
    private SeekBar seekBar=null;	
    private TextView currentDurationLabel=null;	
	private TextView totalDurationLabel=null;	
	private ImageButton playButton=null;	
     
    public ImageButton getPlayButton() {
		return playButton;
	}
	public void setPlayButton(ImageButton playButton) {
		this.playButton = playButton;
	}
	public TextView getCurrentDurationLabel() {
		return currentDurationLabel;
	}
	public void setCurrentDurationLabel(TextView currentDurationLabel) {
		this.currentDurationLabel = currentDurationLabel;
	}
	
	public TextView getTotalDurationLabel() {
		return totalDurationLabel;
	}
	public void setTotalDurationLabel(TextView totalDurationLabel) {
		this.totalDurationLabel = totalDurationLabel;
	}
	public SeekBar getSeekBar() {
		return seekBar;
	}
	public void setSeekBar(SeekBar seekBar) {
		this.seekBar = seekBar;
	}
	/**
	 * Constructor to make a Message object
	 */
	public BeamsterMessage(String message, boolean isMine) {
		super();
		this.message = message;
		this.isMine = isMine;
		this.isStatusMessage = false;
		this.messagedate = Calendar.getInstance();
	}
	

	
	public boolean isStillNeedsToBePlayed() {
		return stillNeedsToBePlayed;
	}
	public void setStillNeedsToBePlayed(boolean stillNeedsToBePlayed) {
		this.stillNeedsToBePlayed = stillNeedsToBePlayed;
	}
	/**
	 * Constructor to make a status Message object
	 * consider the parameters are swaped from default Message constructor,
	 *  not a good approach but have to go with it.
	 */
	public BeamsterMessage(boolean status, String message) {
		super();
		this.message = message;
		this.isMine = false;
		this.isStatusMessage = status;
	}
	public String getMessage() {
		return message;
	}
	public void setMessage(String message) {
		this.message = message;
	}
	public boolean isMine() {
		return isMine;
	}
	public void setMine(boolean isMine) {
		this.isMine = isMine;
	}
	public boolean isStatusMessage() {
		return isStatusMessage;
	}
	public boolean isAudio() {
		return (message!=null && message.trim().startsWith("http://") && (message.trim().endsWith(".mp3") || message.trim().endsWith(".aac")));
	}	
	public boolean isPhoto() {
		return (message!=null && message.trim().startsWith("http://") && (message.trim().endsWith(".png") || message.trim().endsWith(".jpg") || message.trim().endsWith(".jpeg")));
	}	
	
	public void setStatusMessage(boolean isStatusMessage) {
		this.isStatusMessage = isStatusMessage;
	}
	public String getUsername() {
		return username;
	}
	public void setUsername(String username) {
		this.username = username;
	}
	public String getLat() {
		return lat;
	}
	public void setLat(String lat) {
		this.lat = lat;
	}
	public String getLon() {
		return lon;
	}
	public void setLon(String lon) {
		this.lon = lon;
	}
	public String getBearing() {
		return bearing;
	}
	public void setBearing(String bearing) {
		this.bearing = bearing;
	}
	public String getSpeed() {
		return speed;
	}
	public void setSpeed(String speed) {
		this.speed = speed;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public Calendar getMessagedate() {
		return messagedate;
	}
	public void setMessagedate(Calendar messagedate) {
		this.messagedate = messagedate;
	}
	public double getDistanceAway() {
		return distanceAway;
	}
	public void setDistanceAway(double distanceAway) {
		this.distanceAway = distanceAway;
	}
	public boolean isBeamed() {
		return beamed;
	}
	public void setBeamed(boolean beamed) {
		this.beamed = beamed;				
	}
	public void setBeamed(String beamed) {
		if (beamed!=null && beamed.equals("1"))
		{
			this.beamed = true;	
		}
		else
		{
			this.beamed = false;				
		}			
	}
	public String getClientId() {
		return clientId;
	}
	public void setClientId(String clientId) {
		this.clientId = clientId;
	}
	public String getPictureurl() {
		return pictureurl;
	}
	public void setPictureurl(String pictureurl) {
		if (pictureurl==null || pictureurl.equals("nourl"))
			this.pictureurl = "http://www.beamster.com/images/anonymous_m.png?type=square";
		else
			this.pictureurl = pictureurl;
	}
	@Override
	public String toString() {
		return "BeamsterMessage [message=" + message + ", isMine=" + isMine
				+ ", isStatusMessage=" + isStatusMessage + ", username="
				+ username + ", lat=" + lat + ", lon=" + lon + ", bearing="
				+ bearing + ", speed=" + speed + ", name=" + name
				+ ", messagedate=" + messagedate + ", distanceAway="
				+ distanceAway + ", beamed=" + beamed + ", clientId="
				+ clientId + ", pictureurl=" + pictureurl + "]";
	}
	public boolean isComposing() {
		return composing;
	}
	public void setComposing(boolean composing) {
		this.composing = composing;
	}
	
	
}
