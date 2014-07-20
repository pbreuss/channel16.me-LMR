package com.beamster.util;

import android.content.res.Resources;

import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.clustering.ClusterItem;

public class Person implements ClusterItem {
    private final String jid;
    private final String name;    
    private String pictureUrl;
    private LatLng mPosition;

    public Person(final Resources res, String jid, LatLng position, String name, String pictureResourceUrl) {
        this.jid = jid;
        this.name = name;
        this.mPosition = position;
        this.pictureUrl = pictureResourceUrl;
    }

	public String getJid() {
		return jid;
	}    
    
    @Override
    public LatLng getPosition() {
        return mPosition;
    }
    
    public void setPosition(LatLng position) {
    	mPosition = position;
    }    

	public String getName() {
		return name;
	}

	public String getPictureUrl() {
		return pictureUrl;
	}

	public void setPictureUrl(String pictureUrl) {
		this.pictureUrl = pictureUrl;
	}
}
