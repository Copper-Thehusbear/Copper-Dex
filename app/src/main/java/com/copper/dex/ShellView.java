package com.copper.dex;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;

import java.io.InputStream;

/** The device body: one pre-rendered image, plus the name plate. */
final class ShellView extends DView {
 private Bitmap shell;
 private final Paint bmp = new Paint(Paint.FILTER_BITMAP_FLAG | Paint.ANTI_ALIAS_FLAG);
 private final Paint name = Gfx.text(Design.medium, 36, 0xFF8FE9FF, 0.05f);
 private final Rect all = new Rect(0, 0, Design.W, Design.H);

 ShellView(Context c) {
  super(c, new Rect(0, 0, Design.W, Design.H));
  try {
   InputStream in = c.getAssets().open("shell.png");
   BitmapFactory.Options o = new BitmapFactory.Options();
   o.inPreferredConfig = Bitmap.Config.ARGB_8888;
   shell = BitmapFactory.decodeStream(in, null, o);
   in.close();
  } catch (Exception ignored) {
  }
  name.setTextAlign(Paint.Align.CENTER);
 }

 @Override void paintDesign(Canvas c) {
  c.drawColor(Color.BLACK);
  if (shell != null) c.drawBitmap(shell, null, all, bmp);
  c.drawText("Copper TV", Design.NAME.centerX(), Design.NAME.centerY() + 13, name);
 }
}
