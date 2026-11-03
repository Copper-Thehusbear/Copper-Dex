package com.copper.dex;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.text.format.DateFormat;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/** Standby screen for the Power key: black, with a big dim clock. Any key wakes it. */
final class StandbyView extends DView {
 private boolean shown;
 private final boolean h24;
 private final Paint time = Gfx.text(Design.thin, 260, 0xFF9FB4C6, -0.03f);
 private final Paint date = Gfx.text(Design.medium, 26, 0xFF5F7488, 0.4f);
 private final SimpleDateFormat fmt = new SimpleDateFormat("EEEE  dd MMM", Locale.ENGLISH);
 private final Runnable tick = new Runnable() {
  @Override public void run() {
   if (!shown) return;
   invalidate();
   postDelayed(this, 1000 - System.currentTimeMillis() % 1000);
  }
 };

 StandbyView(Context c) {
  super(c, new Rect(0, 0, Design.W, Design.H));
  h24 = DateFormat.is24HourFormat(c);
  time.setTextAlign(Paint.Align.CENTER);
  date.setTextAlign(Paint.Align.CENTER);
  setVisibility(GONE);
 }

 boolean isShown2() { return shown; }

 void setShown(boolean s) {
  shown = s;
  setVisibility(s ? VISIBLE : GONE);
  if (s) post(tick);
 }

 @Override void paintDesign(Canvas c) {
  c.drawColor(Color.BLACK);
  Calendar cal = Calendar.getInstance();
  int h = cal.get(Calendar.HOUR_OF_DAY), m = cal.get(Calendar.MINUTE);
  int dh = h24 ? h : (h % 12 == 0 ? 12 : h % 12);
  String s = (h24 ? String.format(Locale.ENGLISH, "%02d", dh) : String.valueOf(dh)) + ":" + String.format(Locale.ENGLISH, "%02d", m);
  c.drawText(s, Design.W / 2f, 560, time);
  c.drawText(fmt.format(new Date()).toUpperCase(Locale.ENGLISH), Design.W / 2f, 640, date);
 }
}
