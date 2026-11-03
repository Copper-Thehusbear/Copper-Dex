package com.copper.dex;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.text.format.DateFormat;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/** Time, seconds beam, day-of-week strip and date, inside the slanted clock window. */
final class ClockView extends DView {
 private final Path clip = new Path();
 private final Paint panel = Gfx.fill(Design.PANEL);
 private final Paint beam = Gfx.fill(Design.CYAN);
 private final Paint beamGlow = Gfx.fill(Design.alpha(Design.CYAN, 0.35f));
 private final Paint time = Gfx.text(Design.thin, 66, 0xFFF4FBFF, -0.03f);
 private final Paint colon = Gfx.text(Design.light, 66, Design.CYAN, 0f);
 private final Paint ampm = Gfx.text(Design.semi, 15, 0xFFDFE9F1, 0.2f);
 private final Paint secs = Gfx.text(Design.light, 26, Design.CYAN, 0f);
 private final Paint day = Gfx.text(Design.medium, 10, 0xFF6F8498, 0.1f);
 private final Paint dayOn = Gfx.text(Design.semi, 10, 0xFF04101C, 0.1f);
 private final Paint chipOn = Gfx.fill(Design.CYAN);
 private final Paint date = Gfx.text(Design.medium, 11, 0xFF8FA6B8, 0.35f);
 private final SimpleDateFormat dateFmt = new SimpleDateFormat("dd MMM", Locale.ENGLISH);
 private final RectF tmp = new RectF();
 private final boolean h24;
 private final Runnable tick = new Runnable() {
  @Override public void run() {
   invalidate();
   postDelayed(this, 1000 - System.currentTimeMillis() % 1000);
  }
 };

 ClockView(Context c) {
  super(c, Design.CLOCK);
  int[][] p = Design.CLOCK_POLY;
  clip.moveTo(p[0][0], p[0][1]);
  for (int i = 1; i < p.length; i++) clip.lineTo(p[i][0], p[i][1]);
  clip.close();
  h24 = DateFormat.is24HourFormat(c);
  day.setTextAlign(Paint.Align.CENTER);
  dayOn.setTextAlign(Paint.Align.CENTER);
  date.setTextAlign(Paint.Align.RIGHT);
  colon.setShadowLayer(14, 0, 0, Design.alpha(Design.CYAN, 0.9f));
  secs.setShadowLayer(10, 0, 0, Design.alpha(Design.CYAN, 0.6f));
 }

 @Override protected void onAttachedToWindow() {
  super.onAttachedToWindow();
  post(tick);
 }

 @Override protected void onDetachedFromWindow() {
  removeCallbacks(tick);
  super.onDetachedFromWindow();
 }

 @Override void paintDesign(Canvas c) {
  c.save();
  c.clipPath(clip);
  c.drawPath(clip, panel);
  Calendar cal = Calendar.getInstance();
  int hour = cal.get(Calendar.HOUR_OF_DAY), min = cal.get(Calendar.MINUTE), sec = cal.get(Calendar.SECOND);
  int dh = h24 ? hour : (hour % 12 == 0 ? 12 : hour % 12);
  float ox = Design.CLOCK.left, oy = Design.CLOCK.top;
  // seconds beam along the top edge, from the end of the slanted side
  float bx0 = ox + 136, bw = (409 - 136) * (sec + 1) / 60f;
  tmp.set(bx0, oy, bx0 + bw, oy + 3);
  c.drawRoundRect(tmp, 1.5f, 1.5f, beamGlow);
  tmp.set(bx0, oy, bx0 + bw, oy + 2.5f);
  c.drawRect(tmp, beam);
  // time
  String hs = h24 ? String.format(Locale.ENGLISH, "%02d", dh) : String.valueOf(dh);
  String ms = String.format(Locale.ENGLISH, "%02d", min);
  float tx = ox + 128, base = oy + 58;
  c.drawText(hs, tx, base, time);
  float hw = time.measureText(hs);
  c.drawText(":", tx + hw + 2, base - 2, colon);
  c.drawText(ms, tx + hw + colon.measureText(":") + 4, base, time);
  // am/pm and seconds
  float sx = ox + 308;
  if (!h24) c.drawText(hour < 12 ? "AM" : "PM", sx, oy + 23, ampm);
  c.drawText(String.format(Locale.ENGLISH, "%02d", sec), sx, oy + (h24 ? 36 : 52), secs);
  // week strip (Monday first) and date
  int dow = (cal.get(Calendar.DAY_OF_WEEK) + 5) % 7;
  String letters = "MTWTFSS";
  for (int i = 0; i < 7; i++) {
   float x = ox + 44 + i * 26, y = oy + 89 - 5 - 18;
   if (i == dow) {
    tmp.set(x, y, x + 22, y + 18);
    c.drawRoundRect(tmp, 6, 6, chipOn);
    c.drawText(String.valueOf(letters.charAt(i)), x + 11, y + 13, dayOn);
   } else {
    c.drawText(String.valueOf(letters.charAt(i)), x + 11, y + 13, day);
   }
  }
  c.drawText(dateFmt.format(new Date()).toUpperCase(Locale.ENGLISH), ox + 409 - 18, oy + 80, date);
  c.restore();
 }
}
