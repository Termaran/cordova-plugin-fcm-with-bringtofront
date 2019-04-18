package com.gae.scaffolder.plugin;

import java.io.File;
import android.app.NotificationManager;
import android.app.NotificationChannel;
import android.app.Notification;
import android.app.PendingIntent;
import android.media.AudioAttributes;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.graphics.Color;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import java.util.Map;
import java.util.HashMap;
import android.R.drawable;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.sun.org.apache.xml.internal.utils.URI;

import android.graphics.BitmapFactory;
import android.content.res.AssetManager;
import java.util.Arrays;
import java.io.IOException;
import java.io.InputStream;

import android.service.notification.StatusBarNotification;

/**
 * Created by Felipe Echanique on 08/06/2016.
 */
public class MyFirebaseMessagingService extends FirebaseMessagingService {

  private static final String TAG = "FCMPlugin";
  private static final int NOTIFICATION_ID = 1;
  private String callId = "";

  /**
   * Called when message is received.
   *
   * @param remoteMessage Object representing the message received from Firebase
   *                      Cloud Messaging.
   */
  // [START receive_message]
  @Override
  public void onMessageReceived(RemoteMessage remoteMessage) {
    // TODO(developer): Handle FCM messages here.
    // If the application is in the foreground handle both data and notification
    // messages here.
    // Also if you intend on generating your own notifications as a result of a
    // received FCM
    // message, here is where that should be initiated. See sendNotification method
    // below.
    Log.d(TAG, "==> MyFirebaseMessagingService onMessageReceived");

    if (remoteMessage.getNotification() != null) {
      Log.d(TAG, "\tNotification Title: " + remoteMessage.getNotification().getTitle());
      Log.d(TAG, "\tNotification Message: " + remoteMessage.getNotification().getBody());
    }

    Map<String, Object> data = new HashMap<String, Object>();
    data.put("wasTapped", false);
    for (String key : remoteMessage.getData().keySet()) {
      Object value = remoteMessage.getData().get(key);
      Log.d(TAG, "\tKey: " + key + " Value: " + value);
      data.put(key, value);
    }

    NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
    Log.d(TAG, "\tNotification Data: " + data.toString());
    if (!data.get("type").toString().equals("CANCEL")) {
      StatusBarNotification[] oldNotifications = notificationManager.getActiveNotifications();
      if (!notificationActive(oldNotifications)) {
        callId = data.get("call_id").toString();
        if (!FCMPlugin.sendPushPayload(data)) {
          this.bringToFront();
        }

        // Display a notification
        sendNotification((data.get("title") != null ? data.get("title").toString() : getApplicationInfo().name),
            (data.get("caller_name") != null && !data.get("caller_name").toString().equals("")
                ? data.get("caller_name").toString()
                : "unkown"),
            notificationManager);
      }
    } else {
      String x = data.get("type").toString();
      Log.d(TAG, getApplicationInfo().packageName);
      if (data.get("call_id").toString().equals(callId)) {
        notificationManager.cancel(NOTIFICATION_ID);
        callId = "";
      }

    }
  }
  // [END receive_message]

  /**
   * Start the application. Function should be called if app is not running.
   */
  private void bringToFront() {
    Log.d(TAG, "APP will be brought to front because it is running in background");
    Intent notificationIntent = new Intent(this, FCMPluginActivity.class);
    notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
    PendingIntent pendingIntent = PendingIntent.getActivity(this, 0 /* Request code */, notificationIntent,
        PendingIntent.FLAG_ONE_SHOT);
    try {
      pendingIntent.send();
    } catch (PendingIntent.CanceledException e) {
      e.printStackTrace();
    }
  }

  private boolean notificationActive(StatusBarNotification[] oldNotifications) {
    boolean hasNotification = false;
    for (int i = 0; i < oldNotifications.length; i++) {
      if (oldNotifications[i].getId() == NOTIFICATION_ID) {
        hasNotification = true;
      }
    }
    return hasNotification;
  }

  /**
   * Create and show a simple notification containing the received FCM message.
   *
   * @param messageBody FCM message body received.
   */
  private void sendNotification(String title, String messageBody, NotificationManager notificationManager) {
    Intent intent = new Intent(this, FCMPluginActivity.class);
    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

    PendingIntent pendingIntent = PendingIntent.getActivity(this, 0 /* Request code */, intent,
        PendingIntent.FLAG_ONE_SHOT);
    Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);

    Log.d(TAG, "URI OF SOUND: " + defaultSoundUri.toString());
    NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this)
        .setSmallIcon(android.R.drawable.sym_call_incoming)
        .setLargeIcon(BitmapFactory.decodeResource(getResources(), android.R.drawable.sym_call_incoming))
        .setContentTitle(getApplicationInfo().name).setContentText(messageBody).setAutoCancel(true)
        .setSound(defaultSoundUri).setContentIntent(pendingIntent).setTimeoutAfter(20000);

    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
      final String NOTIFICATION_CHANNEL_ID = "10001";
      AudioAttributes att = new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA)
          .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build();
      int importance = NotificationManager.IMPORTANCE_HIGH;
      NotificationChannel notificationChannel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, "PUSH_CHANNEL",
          importance);
      notificationChannel.enableLights(true);
      notificationChannel.setImportance(importance);
      notificationChannel.setLightColor(Color.RED);
      notificationChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
      notificationChannel.setSound(defaultSoundUri, att);
      notificationChannel.enableVibration(true);
      notificationChannel.setVibrationPattern(new long[] { 100, 200, 300, 400, 500, 400, 300, 200, 400 });
      assert notificationManager != null;
      mBuilder.setChannelId(NOTIFICATION_CHANNEL_ID);
      notificationManager.createNotificationChannel(notificationChannel);
    }

    Notification notification = mBuilder.build();
    notification.flags |= Notification.FLAG_INSISTENT;

    notificationManager.notify(NOTIFICATION_ID, notification);
  }

  private void loadSoundFileToFolder(String path) {
    InputStream soundFileStream = getAssets().open(path);
    URI testUri = Uri.parseFile(new File(soundFileStream));
    Log.d(TAG, testUri.toString());
  }
}
