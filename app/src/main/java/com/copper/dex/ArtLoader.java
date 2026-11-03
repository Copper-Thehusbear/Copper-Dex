package com.copper.dex;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Loads an app's banner and icon off the UI thread and works out an accent colour from it. */
final class ArtLoader {
 private final PackageManager pm;
 private final ExecutorService pool = Executors.newSingleThreadExecutor();
 private final Handler ui = new Handler(Looper.getMainLooper());

 ArtLoader(Context c) { pm = c.getPackageManager(); }

 void ensure(final AppEntry e, final Runnable done) {
  if (e.loaded || e.loading || e.type != AppEntry.APP) return;
  e.loading = true;
  pool.execute(new Runnable() {
   @Override public void run() {
    try {
     Drawable banner = null;
     try {
      Intent li = pm.getLeanbackLaunchIntentForPackage(e.pkg);
      if (li != null) banner = pm.getActivityBanner(li);
      if (banner == null) banner = pm.getApplicationBanner(e.pkg);
     } catch (Exception ignored) {
     }
     Bitmap b = banner != null ? toBitmap(banner, 320, 180) : null;
     Bitmap i = null;
     try { i = toBitmap(pm.getApplicationIcon(e.pkg), 128, 128); } catch (Exception ignored) { }
     e.banner = b;
     e.icon = i;
     Bitmap src = b != null ? b : i;
     if (src != null) e.accent = accentOf(src);
    } catch (Throwable ignored) {
    }
    e.loaded = true;
    e.loading = false;
    ui.post(done);
   }
  });
 }

 private static Bitmap toBitmap(Drawable d, int w, int h) {
  Bitmap bm = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
  Canvas c = new Canvas(bm);
  d.setBounds(0, 0, w, h);
  d.draw(c);
  return bm;
 }

 /** A saturated, mid-bright colour taken from the most colourful pixels. */
 static int accentOf(Bitmap src) {
  Bitmap s = Bitmap.createScaledBitmap(src, 24, 24, true);
  float[] hsv = new float[3];
  double r = 0, g = 0, b = 0, wsum = 0;
  for (int y = 0; y < 24; y++) {
   for (int x = 0; x < 24; x++) {
    int p = s.getPixel(x, y);
    if (Color.alpha(p) < 128) continue;
    Color.colorToHSV(p, hsv);
    double w = hsv[1] * hsv[2] * hsv[1];
    r += Color.red(p) * w; g += Color.green(p) * w; b += Color.blue(p) * w; wsum += w;
   }
  }
  if (s != src) s.recycle();
  if (wsum < 0.5) return 0xFF2B6CFF;
  Color.colorToHSV(Color.rgb((int) (r / wsum), (int) (g / wsum), (int) (b / wsum)), hsv);
  hsv[1] = Math.max(hsv[1], 0.6f);
  hsv[2] = Math.max(0.55f, Math.min(hsv[2], 0.92f));
  return Color.HSVToColor(hsv);
 }
}
