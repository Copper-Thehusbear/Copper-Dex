package com.copper.dex;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.Typeface;

/** Design-space constants (a 1920x1080 canvas, matching assets/shell.png) and shared helpers. */
final class Design {
 static final int W = 1920, H = 1080;
 static final Rect LEFT = new Rect(107, 312, 842, 716);
 static final Rect RIGHT = new Rect(1059, 71, 1827, 790);
 static final Rect CLOCK = new Rect(504, 147, 913, 236);
 static final int[][] CLOCK_POLY = {{640, 147}, {913, 147}, {913, 235}, {535, 235}, {504, 214}};
 static final Rect NAME = new Rect(612, 31, 902, 104);
 static final int[] KEY_X = {1115, 1245, 1375, 1505, 1635, 1765};
 static final int KEY_Y = 928, KEY_SIZE = 100;
 static final Rect KEYS = new Rect(1040, 860, 1850, 1050);
 static final Rect LEDS = new Rect(0, 0, 560, 270);

 static final int BG = 0xFF040812, PANEL = 0xFF050810, CYAN = 0xFF5CE1FF, GRAY = 0xFF7E93A6, DIM = 0xFF5F7488;

 static Typeface thin, light, regular, medium, semi;

 static void load(Context c) {
  if (light != null) return;
  AssetManager a = c.getAssets();
  thin = face(a, "Thin"); light = face(a, "Light"); regular = face(a, "Regular"); medium = face(a, "Medium"); semi = face(a, "SemiBold");
 }

 private static Typeface face(AssetManager a, String n) {
  try { return Typeface.createFromAsset(a, "fonts/Outfit-" + n + ".ttf"); } catch (Exception e) { return Typeface.DEFAULT; }
 }

 /** Scales the alpha of an ARGB colour. */
 static int alpha(int color, float a) {
  int v = Math.max(0, Math.min(255, Math.round(Color.alpha(color) * a)));
  return (v << 24) | (color & 0xFFFFFF);
 }

 static float lerp(float a, float b, float t) { return a + (b - a) * t; }

 static int lerp(int a, int b, float t) {
  float x = Math.max(0f, Math.min(1f, t));
  return Color.argb(mix(Color.alpha(a), Color.alpha(b), x), mix(Color.red(a), Color.red(b), x),
    mix(Color.green(a), Color.green(b), x), mix(Color.blue(a), Color.blue(b), x));
 }

 private static int mix(int a, int b, float t) { return Math.round(a + (b - a) * t); }
}
