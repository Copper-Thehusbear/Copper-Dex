package com.copper.dex;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Environment;
import android.os.StatFs;

import java.util.List;
import java.util.Locale;

/** The small display at the lower left: network, notifications and free storage. */
final class StatusView extends DView {
 private final Context ctx;
 private final Paint line = Gfx.fill(Design.alpha(Design.CYAN, 0.22f));
 private final Paint big = Gfx.text(Design.medium, 21, 0xFFF2FAFF, 0.01f);
 private final Paint small = Gfx.text(Design.medium, 12, Design.GRAY, 0.22f);
 private final Paint sub = Gfx.text(Design.regular, 13, 0xFF9FB4C6, 0.04f);
 private final Paint cyan = Gfx.fill(Design.CYAN);
 private final Paint dim = Gfx.fill(0xFF1D3346);
 private final Paint sheen = new Paint();
 private final RectF tmp = new RectF();
 private final Path bell = new Path();

 private String net = "", netSub = "";
 private int level = -1;        // -1 offline, 0..4
 private boolean wired;
 private String free = "";
 private float usedFrac;

 private final Runnable poll = new Runnable() {
  @Override public void run() {
   read();
   invalidate();
   postDelayed(this, 5000);
  }
 };
 private final Runnable onNotes = new Runnable() {
  @Override public void run() { invalidate(); }
 };

 StatusView(Context c) {
  super(c, Design.STATUS);
  ctx = c.getApplicationContext();
  sheen.setShader(new LinearGradient(Design.STATUS.left, Design.STATUS.top, Design.STATUS.left + 160, Design.STATUS.top + 40, 0x16FFFFFF, 0x00FFFFFF, Shader.TileMode.CLAMP));
  small.setLetterSpacing(0.22f);
  read();
 }

 @Override protected void onAttachedToWindow() {
  super.onAttachedToWindow();
  NotifService.listener = new Runnable() {
   @Override public void run() { post(onNotes); }
  };
  post(poll);
 }

 @Override protected void onDetachedFromWindow() {
  removeCallbacks(poll);
  NotifService.listener = null;
  super.onDetachedFromWindow();
 }

 @SuppressWarnings("deprecation")
 private void read() {
  try {
   ConnectivityManager cm = (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
   NetworkInfo ni = cm == null ? null : cm.getActiveNetworkInfo();
   if (ni == null || !ni.isConnected()) {
    level = -1; wired = false; net = "OFFLINE"; netSub = "NO CONNECTION";
   } else if (ni.getType() == ConnectivityManager.TYPE_ETHERNET) {
    level = 4; wired = true; net = "ETHERNET"; netSub = ip(null);
   } else {
    wired = false;
    WifiManager wm = (WifiManager) ctx.getSystemService(Context.WIFI_SERVICE);
    WifiInfo wi = wm == null ? null : wm.getConnectionInfo();
    String ssid = wi == null ? null : wi.getSSID();
    if (ssid != null) ssid = ssid.replace("\"", "");
    if (ssid == null || ssid.isEmpty() || ssid.contains("unknown ssid")) ssid = "Wi-Fi connected";
    net = ssid;
    level = wi == null ? 3 : Math.max(0, Math.min(4, WifiManager.calculateSignalLevel(wi.getRssi(), 5)));
    netSub = ip(wi);
   }
  } catch (Exception e) {
   level = -1; net = "NETWORK"; netSub = "";
  }
  try {
   StatFs fs = new StatFs(Environment.getDataDirectory().getPath());
   long total = fs.getTotalBytes(), avail = fs.getAvailableBytes();
   free = String.format(Locale.ENGLISH, "%.1f GB FREE", avail / 1073741824.0);
   usedFrac = total > 0 ? 1f - (float) avail / total : 0f;
  } catch (Exception e) {
   free = "";
  }
 }

 private static String ip(WifiInfo wi) {
  if (wi == null) return "";
  int a = wi.getIpAddress();
  if (a == 0) return "";
  return (a & 0xff) + "." + ((a >> 8) & 0xff) + "." + ((a >> 16) & 0xff) + "." + ((a >> 24) & 0xff);
 }

 @Override void paintDesign(Canvas c) {
  float x0 = Design.STATUS.left, y0 = Design.STATUS.top, w = Design.STATUS.width(), h = Design.STATUS.height();
  float c1 = x0 + 186, c2 = x0 + 426;
  // dividers
  tmp.set(c1, y0 + 12, c1 + 1.5f, y0 + h - 12); c.drawRect(tmp, line);
  tmp.set(c2, y0 + 12, c2 + 1.5f, y0 + h - 12); c.drawRect(tmp, line);
  // network
  drawBars(c, x0 + 20, y0 + h / 2f + 14);
  float tx = x0 + 62;
  c.drawText(fit(net, big, c1 - tx - 12), tx, y0 + 33, big);
  c.drawText(netSub, tx, y0 + 53, sub);
  // notifications
  List<NotifService.Item> list = NotifService.recent;
  drawBell(c, c1 + 30, y0 + h / 2f - 2);
  float nx = c1 + 54;
  if (!NotifService.connected) {
   c.drawText("NOTICES", nx, y0 + 29, small);
   c.drawText("Not enabled", nx, y0 + 52, sub);
  } else if (list.isEmpty()) {
   c.drawText("NOTICES", nx, y0 + 29, small);
   c.drawText("All clear", nx, y0 + 53, big);
  } else {
   NotifService.Item it = list.get(0);
   String head = list.size() + (list.size() == 1 ? " NOTICE  ·  " : " NOTICES  ·  ") + it.app.toUpperCase(Locale.ENGLISH);
   c.drawText(fit(head, small, c2 - nx - 12), nx, y0 + 28, small);
   c.drawText(fit(it.title.isEmpty() ? it.app : it.title, big, c2 - nx - 12), nx, y0 + 54, big);
  }
  // storage
  float sx = c2 + 20;
  c.drawText("STORAGE", sx, y0 + 29, small);
  c.drawText(fit(free, sub, w - (sx - x0) - 14), sx, y0 + 47, sub);
  tmp.set(sx, y0 + 53, x0 + w - 18, y0 + 58);
  c.drawRoundRect(tmp, 2.5f, 2.5f, dim);
  tmp.right = tmp.left + (tmp.width()) * Math.max(0.04f, usedFrac);
  c.drawRoundRect(tmp, 2.5f, 2.5f, cyan);
  c.drawRect(Design.STATUS, sheen);
 }

 private void drawBars(Canvas c, float x, float baseY) {
  for (int i = 0; i < 5; i++) {
   float bh = 6 + i * 5;
   tmp.set(x + i * 7, baseY - bh, x + i * 7 + 4.5f, baseY);
   c.drawRoundRect(tmp, 1.5f, 1.5f, (level >= 0 && i <= level) ? cyan : dim);
  }
 }

 private void drawBell(Canvas c, float cx, float cy) {
  bell.reset();
  bell.moveTo(cx - 8, cy + 6);
  bell.lineTo(cx - 8, cy - 1);
  tmp.set(cx - 8, cy - 11, cx + 8, cy + 5);
  bell.arcTo(tmp, 180, 180);
  bell.lineTo(cx + 8, cy + 6);
  bell.close();
  c.drawPath(bell, cyan);
  c.drawRect(cx - 11, cy + 5, cx + 11, cy + 7.5f, cyan);
  c.drawCircle(cx, cy + 11, 2.8f, cyan);
 }

 private static String fit(String s, Paint p, float max) {
  if (s == null) return "";
  if (p.measureText(s) <= max) return s;
  while (s.length() > 1 && p.measureText(s + "…") > max) s = s.substring(0, s.length() - 1);
  return s + "…";
 }
}
