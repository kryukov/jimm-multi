/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.bombusmod.location;

import android.location.Location;
import android.location.LocationManager;

import android.content.Context;
import ru.net.jimm.JimmActivity;

/**
 *
 * @author Vitaly
 */
public class LocationAndroid {

    LocationManager locationManager;
    Location lastKnownLocation;

    public LocationAndroid(JimmActivity ctx) {
        // Acquire a reference to the system Location Manager
        locationManager = (LocationManager) ctx.getSystemService(Context.LOCATION_SERVICE);
    }

    public void getCoordinates() throws Exception {
        lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
    }

    public String getLatitude() {
        return String.valueOf(lastKnownLocation.getLatitude());
    }

    public String getLongitude() {
        return String.valueOf(lastKnownLocation.getLongitude());
    }
}
