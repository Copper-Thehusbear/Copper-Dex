package com.copper.dex;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.LinearGradient;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;

import java.io.InputStream;

/** The device body: one pre-rendered image, plus the name plate. */
final class ShellView extends DView {
 private Bitmap shell;
 private final Paint bmp = new Paint(Paint.FILTER_BITMAP_FLAG | Paint.ANTI_ALIAS_FLAG);
 private final Paint word = Gfx.text(Design.semi, 31, 0xFFFFFFFF, 0.06f);
 private final Paint wordDex = Gfx.text(Design.regular, 31, Design.CYAN, 0.06f);
 private final Paint tag = Gfx.text(Design.medium, 11, 0xFF8FA6B8, 0.34f);
 private final Paint plate = new Paint(Paint.ANTI_ALIAS_FLAG);
 private final Paint edge = Gfx.stroke(Design.alpha(Design.CYAN, 0.35f), 1.5f);
 private final Paint ballFill = new Paint(Paint.ANTI_ALIAS_FLAG);
 private final Paint ballLine = Gfx.fill(0xFF181418);
 private final RectF box = new RectF();
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
  word.setShadowLayer(8, 0, 0, Design.alpha(0xFFFFFFFF, 0.25f));
  wordDex.setShadowLayer(10, 0, 0, Design.alpha(Design.CYAN, 0.6f));
 }

 @Override void paintDesign(Canvas c) {
  c.drawColor(Color.BLACK);
  if (shell != null) c.drawBitmap(shell, null, all, bmp);
  drawPlate(c);
 }

 /** The logo on the name plate: a ball mark, "COPPER DEX" and a small tagline. */
 private void drawPlate(Canvas c) {
  Rect n = Design.NAME;
  box.set(n.left + 5, n.top + 5, n.right - 5, n.bottom - 5);
  plate.setShader(new LinearGradient(0, box.top, 0, box.bottom, 0xFF0B1426, 0xFF050810, Shader.TileMode.CLAMP));
  c.drawRoundRect(box, 9, 9, plate);
  plate.setShader(null);
  c.drawRoundRect(box, 9, 9, edge);
  float cx = n.left + 38, cy = n.centerY();
  drawBall(c, cx, cy, 21);
  float tx = n.left + 72;
  c.drawText("COPPER", tx, n.top + 38, word);
  float w = word.measureText("COPPER ");
  c.drawText("DEX", tx + w, n.top + 38, wordDex);
  c.drawText("HOME FOR YOUR TV", tx + 2, n.top + 58, tag);
 }

 private void drawBall(Canvas c, float cx, float cy, float r) {
  box.set(cx - r - 1.5f, cy - r - 1.5f, cx + r + 1.5f, cy + r + 1.5f);
  c.drawOval(box, ballLine);
  box.set(cx - r, cy - r, cx + r, cy + r);
  ballFill.setColor(0xFFE23B3B);
  c.drawArc(box, 180, 180, true, ballFill);
  ballFill.setColor(0xFFF4F4F6);
  c.drawArc(box, 0, 180, true, ballFill);
  c.drawRect(cx - r, cy - r * 0.11f, cx + r, cy + r * 0.11f, ballLine);
  c.drawCircle(cx, cy, r * 0.34f, ballLine);
  ballFill.setColor(0xFFF8F8FA);
  c.drawCircle(cx, cy, r * 0.22f, ballFill);
  ballFill.setColor(0x66FFFFFF);
  box.set(cx - r * 0.7f, cy - r * 0.82f, cx - r * 0.1f, cy - r * 0.4f);
  c.drawOval(box, ballFill);
 }
}
