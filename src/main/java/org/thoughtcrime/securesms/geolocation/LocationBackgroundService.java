package org.thoughtcrime.securesms.geolocation;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import org.thoughtcrime.securesms.ConversationListActivity;
import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.notifications.NotificationCenter;
import org.thoughtcrime.securesms.util.IntentUtils;

import java.util.concurrent.atomic.AtomicBoolean;

public class LocationBackgroundService extends Service {

    private static final int INITIAL_TIMEOUT = 1000 * 60 * 2;
    private static final String TAG = LocationBackgroundService.class.getSimpleName();
    private LocationManager locationManager = null;
    private static final int LOCATION_INTERVAL = 1000;
    private static final float LOCATION_DISTANCE = 25F;
    ServiceLocationListener locationListener;
    private final AtomicBoolean isForeground = new AtomicBoolean(false);

    private final IBinder mBinder = new LocationBackgroundServiceBinder();

    @Override
    public boolean bindService(Intent service, ServiceConnection conn, int flags) {
        return super.bindService(service, conn, flags);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        
        locationManager = (LocationManager) getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
        if (locationManager == null) {
            Log.e(TAG, "Unable to initialize location service");
            // Initialize foreground first, then stop
            initializeForegroundService();
            stopForeground(true);
            stopSelf();
            return;
        }

        // Initialize foreground service after successful location manager setup
        initializeForegroundService();

        locationListener = new ServiceLocationListener();
        Location lastLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        if (lastLocation != null) {
          long locationAge = System.currentTimeMillis() - lastLocation.getTime();
          if (locationAge <= 600 * 1000) { // not older than 10 minutes
            DcLocation.getInstance().updateLocation(lastLocation);
          }
        }
        //requestLocationUpdate(LocationManager.NETWORK_PROVIDER);
        requestLocationUpdate(LocationManager.GPS_PROVIDER);
        initialLocationUpdate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        
        // Ensure foreground notification is shown (handles edge cases)
        initializeForegroundService();
        
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        // Stop foreground notification
        stopForeground(true);

        if (locationManager == null) {
            return;
        }

        try {
            locationManager.removeUpdates(locationListener);
        } catch (Exception ex) {
            Log.i(TAG, "fail to remove location listeners, ignore", ex);
        }
    }

    private void initializeForegroundService() {
        if (isForeground.compareAndSet(false, true)) {
            createNotificationChannel();
            startForeground(NotificationCenter.ID_LOCATION, createNotification());
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                NotificationCenter.CH_LOCATION,
                getString(R.string.location),
                NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Location sharing notification");
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    private Notification createNotification() {
        Intent intent = new Intent(this, ConversationListActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
            this, 
            0, 
            intent, 
            IntentUtils.FLAG_IMMUTABLE()
        );

        return new NotificationCompat.Builder(this, NotificationCenter.CH_LOCATION)
            .setContentTitle(getString(R.string.location_sharing_notification_title))
            .setContentText(getString(R.string.location_sharing_notification_text))
            .setSmallIcon(R.drawable.ic_location_on_white_24dp)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build();
    }

    private void requestLocationUpdate(String provider) {
        try {
            locationManager.requestLocationUpdates(
                    provider, LOCATION_INTERVAL, LOCATION_DISTANCE,
                    locationListener);
        } catch (SecurityException | IllegalArgumentException  ex) {
            Log.e(TAG, String.format("Unable to request %s provider based location updates.", provider), ex);
        }
    }

    private void initialLocationUpdate() {
        try {
            Location gpsLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (gpsLocation != null && System.currentTimeMillis() - gpsLocation.getTime() < INITIAL_TIMEOUT) {
              locationListener.onLocationChanged(gpsLocation);
            }

        } catch (NullPointerException | SecurityException e) {
            e.printStackTrace();
        }
    }

    class LocationBackgroundServiceBinder extends Binder {
        LocationBackgroundServiceBinder getService() {
            return LocationBackgroundServiceBinder.this;
        }

        void stop() {
            DcLocation.getInstance().reset();
            stopSelf();
        }
    }

    private class ServiceLocationListener implements LocationListener {

        @Override
        public void onLocationChanged(@NonNull Location location) {
            Log.d(TAG, "onLocationChanged: " + location);
            if (location == null) {
                return;
            }
            DcLocation.getInstance().updateLocation(location);
        }

        @Override
        public void onProviderDisabled(@NonNull String provider) {
            Log.e(TAG, "onProviderDisabled: " + provider);
        }

        @Override
        public void onProviderEnabled(@NonNull String provider) {
            Log.e(TAG, "onProviderEnabled: " + provider);
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            Log.e(TAG, "onStatusChanged: " + provider + " status: " + status);
        }
    }

}
