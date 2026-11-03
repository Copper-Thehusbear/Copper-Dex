package com.copper.dex;

import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;

/** Small drawing helpers shared by the views. */
final class Gfx {
 private static final Matrix M = new Matrix();

 static Paint text(Typeface tf, float size, int color, float spacingEm) {
  Paint p = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
  p.setTypeface(tf);
  p.setTextSize(size);
  p.setColor(color);
  p.setLetterSpacing(spacingEm);
  return p;
 }

 static Paint fill(int color) {
  Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
  p.setColor(color);
  return p;
 }

 static Paint stroke(int color, float width) {
  Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
  p.setStyle(Paint.Style.STROKE);
  p.setStrokeWidth(width);
  p.setStrokeCap(Paint.Cap.ROUND);
  p.setStrokeJoin(Paint.Join.ROUND);
  p.setColor(color);
  return p;
 }

 /** Fills a rounded rectangle with the bitmap, scaled to cover it. */
 static void bitmapCover(Canvas c, Bitmap bm, RectF dst, float radius, float alpha, Paint scratch) {
  if (bm == null || bm.isRecycled()) return;
  BitmapShader sh = new BitmapShader(bm, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
  float s = Math.max(dst.width() / bm.getWidth(), dst.height() / bm.getHeight());
  M.reset();
  M.setScale(s, s);
  M.postTranslate(dst.centerX() - bm.getWidth() * s / 2f, dst.centerY() - bm.getHeight() * s / 2f);
  sh.setLocalMatrix(M);
  scratch.setShader(sh);
  scratch.setAlpha(Math.round(255 * Math.max(0f, Math.min(1f, alpha))));
  c.drawRoundRect(dst, radius, radius, scratch);
  scratch.setShader(null);
  scratch.setAlpha(255);
 }

 /** Draws the bitmap fitted inside the rectangle (no rounding), keeping its aspect ratio. */
 static void bitmapFit(Canvas c, Bitmap bm, RectF dst, float alpha, Paint scratch) {
  if (bm == null || bm.isRecycled()) return;
  float s = Math.min(dst.width() / bm.getWidth(), dst.height() / bm.getHeight());
  float w = bm.getWidth() * s, h = bm.getHeight() * s;
  RectF r = new RectF(dst.centerX() - w / 2f, dst.centerY() - h / 2f, dst.centerX() + w / 2f, dst.centerY() + h / 2f);
  scratch.setAlpha(Math.round(255 * Math.max(0f, Math.min(1f, alpha))));
  c.drawBitmap(bm, null, r, scratch);
  scratch.setAlpha(255);
 }

 /** Four L-shaped corner marks inside the rectangle. */
 static void brackets(Canvas c, RectF r, float inset, float arm, Paint p) {
  float l = r.left + inset, t = r.top + inset, rr = r.right - inset, b = r.bottom - inset;
  c.drawLine(l, t, l + arm, t, p); c.drawLine(l, t, l, t + arm, p);
  c.drawLine(rr, t, rr - arm, t, p); c.drawLine(rr, t, rr, t + arm, p);
  c.drawLine(l, b, l + arm, b, p); c.drawLine(l, b, l, b - arm, p);
  c.drawLine(rr, b, rr - arm, b, p); c.drawLine(rr, b, rr, b - arm, p);
 }
}
