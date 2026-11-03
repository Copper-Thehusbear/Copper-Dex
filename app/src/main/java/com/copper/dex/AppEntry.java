package com.copper.dex;

import android.content.Intent;
import android.graphics.Bitmap;

/** One row in the list: an installed app, a TV input, or Live TV. */
final class AppEntry {
 static final int APP = 0, INPUT = 1;
 final int type;
 final String pkg;      // package name for apps, input id for inputs
 final String label;
 final String category; // upper-case caption under the name
 Intent launch;
 String glyph;          // text drawn in the tile when there is no artwork
 volatile Bitmap banner, icon;
 volatile int accent = 0xFF0A5CFF;
 volatile boolean loaded, loading;

 AppEntry(int type, String pkg, String label, String category) {
  this.type = type;
  this.pkg = pkg;
  this.label = label;
  this.category = category;
 }

 char initial() {
  for (int i = 0; i < label.length(); i++) {
   char c = Character.toUpperCase(label.charAt(i));
   if (c >= 'A' && c <= 'Z') return c;
   if (Character.isLetterOrDigit(c)) return c < 'A' ? '#' : c;
  }
  return '#';
 }
}
