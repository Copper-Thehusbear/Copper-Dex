package com.copper.dex;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.os.SystemClock;

import java.util.Random;

/** Soft glows over the lens and the three LEDs: a short power-on sequence, then a slow breathing and random blinks. */
final class LedView extends DView {
 private static final float[][] LED = {{311, 106}, {395, 106}, {479, 106}};
 private static final int[] COLOR = {0xFFFF3B30, 0xFFFFC21A, 0xFF2ED158};
 private static final long[] ONSET = {100, 450, 800};
 private final long t0 = SystemClock.uptimeMillis();
 private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
 private final Random rnd = new Random();
 private final long[] next = new long[3];
 private final Runnable loop = new Runnable() {
  @Override public void run() {
   invalidate();
   postDelayed(this, 80);
  }
 };

 LedView(Context c) {
  super(c, Design.LEDS);
  for (int i = 0; i < 3; i++) next[i] = 2600 + rnd.nextInt(3000);
 }

 @Override protected void onAttachedToWindow() {
  super.onAttachedToWindow();
  post(loop);
 }

 @Override protected void onDetachedFromWindow() {
  removeCallbacks(loop);
  super.onDetachedFromWindow();
 }

 @Override void paintDesign(Canvas c) {
  long t = SystemClock.uptimeMillis() - t0;
  float breath = 0.5f + 0.5f * (float) Math.sin(t / 1100.0);
  float flash = t < 1400 ? (float) Math.pow(Math.max(0f, 1f - t / 800f), 2) * 0.7f : 0f;
  // soft halo just outside the lens ring
  float ha = Math.min(0.9f, 0.14f + 0.14f * breath + flash * 0.5f);
  p.setShader(new RadialGradient(140, 118, 138, new int[]{0x005CE1FF, 0x005CE1FF, Design.alpha(Design.CYAN, ha), 0x005CE1FF},
    new float[]{0f, 0.77f, 0.84f, 1f}, Shader.TileMode.CLAMP));
  c.drawCircle(140, 118, 138, p);
  // power-on flash over the picture
  if (flash > 0.01f) {
   p.setShader(new RadialGradient(140, 118, 80, Design.alpha(0xFFFFFFFF, flash), 0x00FFFFFF, Shader.TileMode.CLAMP));
   c.drawCircle(140, 118, 80, p);
  }
  for (int i = 0; i < 3; i++) {
   float level = Math.max(0f, Math.min(1f, (t - ONSET[i]) / 220f));
   if (t >= next[i] + 180) next[i] = t + 1800 + rnd.nextInt(5200);
   float dip = (t >= next[i] && t < next[i] + 180) ? 0f : 1f;
   float g = 0.55f * level * dip;
   if (g <= 0.01f) continue;
   p.setShader(new RadialGradient(LED[i][0], LED[i][1], 38, Design.alpha(COLOR[i], g), Design.alpha(COLOR[i], 0f), Shader.TileMode.CLAMP));
   c.drawCircle(LED[i][0], LED[i][1], 38, p);
  }
  p.setShader(null);
 }
}
