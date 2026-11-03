package com.copper.dex;

import android.app.Notification;
import android.content.pm.PackageManager;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/** Reads the notifications the TV posts so the status display can show them. Needs "notification access"
 *  (tools/install-apk.sh switches it on with: cmd notification allow_listener). */
public class NotifService extends NotificationListenerService {
 static final class Item {
  String app = "", title = "";
  long time;
 }

 static volatile List<Item> recent = Collections.emptyList();
 static volatile boolean connected;
 static volatile Runnable listener;

 @Override public void onListenerConnected() {
  connected = true;
  refresh();
 }

 @Override public void onListenerDisconnected() {
  connected = false;
 }

 @Override public void onNotificationPosted(StatusBarNotification sbn) { refresh(); }

 @Override public void onNotificationRemoved(StatusBarNotification sbn) { refresh(); }

 private void refresh() {
  List<Item> out = new ArrayList<>();
  try {
   StatusBarNotification[] all = getActiveNotifications();
   PackageManager pm = getPackageManager();
   if (all != null) {
    for (StatusBarNotification n : all) {
     if (getPackageName().equals(n.getPackageName())) continue;
     if ((n.getNotification().flags & Notification.FLAG_ONGOING_EVENT) != 0) continue;
     Item it = new Item();
     it.time = n.getPostTime();
     try { it.app = String.valueOf(pm.getApplicationLabel(pm.getApplicationInfo(n.getPackageName(), 0))); } catch (Exception e) { it.app = n.getPackageName(); }
     CharSequence t = n.getNotification().extras.getCharSequence(Notification.EXTRA_TITLE);
     it.title = t == null ? "" : t.toString();
     out.add(it);
    }
   }
  } catch (Exception ignored) {
  }
  Collections.sort(out, new Comparator<Item>() {
   @Override public int compare(Item a, Item b) { return Long.compare(b.time, a.time); }
  });
  recent = out;
  Runnable r = listener;
  if (r != null) r.run();
 }
}
