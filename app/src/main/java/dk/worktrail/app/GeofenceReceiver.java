package dk.worktrail.app;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;

public final class GeofenceReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context ctx, Intent intent) {
        GeofencingEvent event = GeofencingEvent.fromIntent(intent);
        if (event == null || event.hasError() || event.getTriggeringGeofences() == null) return;
        int transition = event.getGeofenceTransition();
        if (transition != Geofence.GEOFENCE_TRANSITION_ENTER && transition != Geofence.GEOFENCE_TRANSITION_EXIT) return;
        TrackerDb db = new TrackerDb(ctx);
        boolean entering = transition == Geofence.GEOFENCE_TRANSITION_ENTER;
        long timestamp = System.currentTimeMillis(); // detection time; not guaranteed physical crossing time
        for (Geofence fence : event.getTriggeringGeofences()) {
            String id = fence.getRequestId();
            if (!id.startsWith("place:")) continue;
            try {
                TrackerDb.Place p = db.place(Long.parseLong(id.substring(6)));
                if (p == null) continue;
                String wifi = wifiStatus(ctx, p.wifi);
                db.recordTransition(p.id, entering, timestamp, wifi);
                notifyRecord(ctx, p.name, entering);
            } catch (NumberFormatException ignored) { }
        }
    }

    @SuppressWarnings("deprecation")
    private String wifiStatus(Context ctx, String expected) {
        if (expected == null || expected.trim().isEmpty()) return "not configured";
        if (ctx.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) return "unknown";
        WifiManager wifi = (WifiManager) ctx.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (wifi == null) return "unknown";
        WifiInfo info = wifi.getConnectionInfo();
        if (info == null || info.getSSID() == null) return "unknown";
        String observed = info.getSSID().replace("\"", "");
        if ("<unknown ssid>".equalsIgnoreCase(observed)) return "unknown";
        return expected.equals(observed) ? "confirmed" : "other network";
    }

    private void notifyRecord(Context ctx, String place, boolean entered) {
        if (Build.VERSION.SDK_INT >= 33 && ctx.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return;
        NotificationManager manager = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null) return;
        String channel = "worktrail-events";
        if (Build.VERSION.SDK_INT >= 26) manager.createNotificationChannel(new NotificationChannel(channel, "Arrival and departure records", NotificationManager.IMPORTANCE_DEFAULT));
        String text = (entered ? "Arrival recorded: " : "Departure recorded: ") + place;
        Notification notification = new Notification.Builder(ctx, channel)
                .setSmallIcon(android.R.drawable.ic_menu_mylocation)
                .setContentTitle("WorkTrail")
                .setContentText(text)
                .setAutoCancel(true)
                .build();
        manager.notify(1005, notification);
    }
}
