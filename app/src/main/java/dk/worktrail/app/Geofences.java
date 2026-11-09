package dk.worktrail.app;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;
import java.util.ArrayList;
import java.util.List;

public final class Geofences {
    private static final String TAG = "WorkTrailGeofences";
    private Geofences() {}

    public static boolean canMonitor(Context ctx) {
        boolean fine = ctx.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        boolean background = Build.VERSION.SDK_INT < 29 || ctx.checkSelfPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED;
        return fine && background;
    }

    private static PendingIntent pendingIntent(Context ctx) {
        Intent i = new Intent(ctx, GeofenceReceiver.class);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= 31) flags |= PendingIntent.FLAG_MUTABLE;
        return PendingIntent.getBroadcast(ctx, 1004, i, flags);
    }

    public static void sync(Context ctx) {
        if (!canMonitor(ctx)) return;
        List<Geofence> fences = new ArrayList<>();
        for (TrackerDb.Place place : new TrackerDb(ctx).places()) {
            fences.add(new Geofence.Builder()
                    .setRequestId("place:" + place.id)
                    .setCircularRegion(place.latitude, place.longitude, place.radius)
                    .setExpirationDuration(Geofence.NEVER_EXPIRE)
                    .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_EXIT)
                    .setNotificationResponsiveness(60000)
                    .build());
        }
        try {
            // Remove obsolete registrations, then re-register from our saved places.
            LocationServices.getGeofencingClient(ctx).removeGeofences(pendingIntent(ctx)).addOnCompleteListener(task -> {
                if (fences.isEmpty()) return;
                GeofencingRequest request = new GeofencingRequest.Builder()
                        .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
                        .addGeofences(fences)
                        .build();
                try {
                    LocationServices.getGeofencingClient(ctx).addGeofences(request, pendingIntent(ctx))
                            .addOnFailureListener(error -> Log.e(TAG, "Could not register places", error));
                } catch (SecurityException error) {
                    Log.e(TAG, "Location permission was revoked", error);
                }
            });
        } catch (SecurityException error) {
            Log.e(TAG, "Location permission missing", error);
        }
    }
}
