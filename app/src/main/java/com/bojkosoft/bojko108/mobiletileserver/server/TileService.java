package com.bojkosoft.bojko108.mobiletileserver.server;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.drawable.Icon;
import android.os.IBinder;
import android.util.Log;

import com.bojkosoft.bojko108.mobiletileserver.MainActivity;
import com.bojkosoft.bojko108.mobiletileserver.R;

import androidx.annotation.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

/**
 * This service controls the Mobile Tile Server. Use {@link TileServiceReceiver TileServiceReceiver class}
 * for more info about interacting with this service. For more info regarding the HTTP Server go to {@link TileServer TileServer class}.
 * <p>
 * Mobile Tile Server, Copyright (c) 2020 by bojko108
 * <p/>
 * <b>Startup parameters: </b>
 * <ul>
 * <li><b>KEY_SERVER_PATH</b> - sets the server URL address as <i>String</i></li>
 * <li><b>KEY_SERVER_PORT</b> - sets the server's listening port</li>
 * <li><b>KEY_ROOT_PATH</b> - sets the root directory, served by the server</li>
 * <li><b>KEY_NO_TILE_IMAGE</b> - opens the Server home page WebBrowser</li>
 * </ul>
 * <p/>
 */
public class TileService extends Service {
    private static final String TAG = TileService.class.getName();
    /**
     * Use this key to trigger ping action to see if  {@link TileService} is running.
     * A receiver is registered in {@link TileService} to listen for this action and
     * to broadcast {@link TileService#ACTION_RUNNING} task if the service is running.
     */
    public static final String ACTION_PING = "mobiletileserver.ACTION_PING";
    /**
     * Use this key to register a local receiver which will help you to determine if {@link TileService}
     * is running. First register a local receiver with {@link IntentFilter} set to {@link TileService#ACTION_RUNNING}
     * and then broadcast {@link TileService#ACTION_PING} action to trigger the process.
     */
    public static final String ACTION_RUNNING = "mobiletileserver.ACTION_RUNNING";

    /**
     * Use this key to set the server URL address as <i>String</i>
     */
    public static final String KEY_SERVER_PATH = "KEY_SERVER_PATH";
    /**
     * Use this key to set the server listening port as <i>int</i>
     */
    public static final String KEY_SERVER_PORT = "KEY_SERVER_PORT";
    /**
     * Use this key to set the server root directory path as <i>String</i>
     */
    public static final String KEY_ROOT_PATH = "KEY_ROOT_PATH";
    /**
     * Use this key to set no_tile_data as <i>byte[]</i>
     */
    public static final String KEY_NO_TILE_IMAGE = "KEY_NO_TILE_IMAGE";

    /**
     * NotificationChannel used by this Service
     */
    private NotificationChannel notificationChannel;
    /**
     * Reference to the HTTP Server
     */
    private TileServer server;

    @Override
    public void onCreate() {
        super.onCreate();

        Log.i(TAG, "onCreate");
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);

        Log.i(TAG, "onStartCommand");
        try {
            LocalBroadcastManager.getInstance(this).registerReceiver(this.localReceiver, new IntentFilter(ACTION_PING));

            if (this.server == null) {
                int port = intent.getIntExtra(KEY_SERVER_PORT, 9999);
                String rootPath = intent.getStringExtra(KEY_ROOT_PATH);
                byte[] noTileData = intent.getByteArrayExtra(KEY_NO_TILE_IMAGE);

                this.server = new TileServer(port, rootPath, noTileData, getApplicationContext());
                this.server.start();

                startForeground(1, this.createNotification());
            }
        } catch (IOException e) {
            Log.e(TAG, "onStartCommand throws: ", e);
        }

        return START_REDELIVER_INTENT;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        LocalBroadcastManager.getInstance(this).unregisterReceiver(this.localReceiver);

        if (this.server != null) {
            this.server.stop();
            Log.i(TAG, "TileServer: stopped");
        }

        Log.i(TAG, "onDestroy");

        stopForeground(true);
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);

        Log.i(TAG, "onTaskRemoved");
    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);

        Log.i(TAG, "onTrimMemory: " + level);
    }

    /**
     * Creates a notification channel for this service
     */
    private void createNotificationChannel() {
        String CHANNEL_ID = "com.bojkosoft.bojko108.mobiletileserver";
        this.notificationChannel = new NotificationChannel(CHANNEL_ID, getResources().getString(R.string.app_name), NotificationManager.IMPORTANCE_HIGH);
        this.notificationChannel.setLightColor(Color.WHITE);
        this.notificationChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        this.notificationChannel.setSound(null, null);
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        assert manager != null;
        manager.createNotificationChannel(this.notificationChannel);
    }

    /**
     * Creates a new notification to the notification channel
     *
     * @return notification
     */
    private Notification createNotification() {
        if (this.notificationChannel == null) {
            this.createNotificationChannel();
        }

        Intent openIntent = new Intent(this, MainActivity.class);
        openIntent.setAction(Intent.ACTION_MAIN);
        openIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        PendingIntent pendingIntentNotification = PendingIntent
                .getActivity(this, 0, openIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        Notification.Action stopAction = this.createAction(
                TileServiceReceiver.ACTION_STOP, getResources().getString(R.string.server_action_stop_short), null, 1, 0);

        List<String[]> extras = new ArrayList<>();
        extras.add(new String[]{KEY_ROOT_PATH, this.server.getRootPath()});
        Notification.Action browseRootAction = this.createAction(
                TileServiceReceiver.ACTION_OPEN_ROOT_PATH, getResources().getString(R.string.server_action_browse_short), extras, 2, 0);

        extras.clear();
        extras.add(new String[]{KEY_SERVER_PATH, this.server.getUrlAddress()});
        Notification.Action navigateAction = this.createAction(
                TileServiceReceiver.ACTION_NAVIGATE_TO_SERVER, getResources().getString(R.string.server_action_navigate_short), extras, 3, PendingIntent.FLAG_UPDATE_CURRENT);

        Notification.Builder builder = new Notification.Builder(this, this.notificationChannel.getId())
                .setOngoing(true)
                .setCategory(Notification.CATEGORY_SERVICE)
                .setSmallIcon(R.drawable.ic_stat_name)
                .setColor(getResources().getColor(R.color.colorPrimary, null))
                .setContentTitle(getResources().getString(R.string.app_not_title)).setContentText(getResources().getText(R.string.app_not_text))
                .addAction(navigateAction)
                .addAction(browseRootAction)
                .addAction(stopAction)
                .setContentIntent(pendingIntentNotification);

        return builder.build();
    }

    /**
     * Creates new notification action
     *
     * @param actionType  According to {@link TileServiceReceiver TileServiceReceiver class}
     * @param title       Text to use for the button
     * @param extras      List of extra values, which will be passed to the Intent, triggered by this action, according to {@link TileServiceReceiver TileServiceReceiver class}
     * @param requestCode
     * @param flags
     * @return new notification action
     */
    private Notification.Action createAction(String actionType, String title, List<String[]> extras, int requestCode, int flags) {
        Intent actionIntent = new Intent(this, TileServiceReceiver.class);
        actionIntent.setAction(actionType);
        if (extras != null) {
            for (String[] data : extras) {
                actionIntent.putExtra(data[0], data[1]);
            }
        }

        PendingIntent pendingIntentAction = PendingIntent
                .getBroadcast(this, requestCode, actionIntent, flags);

        return new Notification.Action.Builder(
                Icon.createWithResource(this, R.drawable.ic_stat_name),
                title,
                pendingIntentAction).build();
    }

    private BroadcastReceiver localReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (ACTION_PING.equals(intent.getAction())) {
                LocalBroadcastManager manager = LocalBroadcastManager.getInstance(getApplicationContext());
                manager.sendBroadcast(new Intent(ACTION_RUNNING));
            }
        }
    };
}