package com.copper.dex;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.view.animation.DecelerateInterpolator;

/** Left screen: the focused app, full size, like the TV's own preview. */
final class HeroView extends DView {
 private AppEntry cur, prev;
 private float t = 1f;
 private ValueAnimator anim;
 private final RectF area = new RectF(Design.LEFT);
 private final Path clip = new Path();
 private final Paint bg = new Paint(Paint.ANTI_ALIAS_FLAG), art = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
 private final Paint cap = Gfx.text(Design.medium, 14, 0xBFFFFFFF, 0.35f);
 private final Paint title = Gfx.text(Design.light, 54, 0xFFFFFFFF, -0.02f);
 private final Paint hint = Gfx.text(Design.regular, 20, 0xCCFFFFFF, 0f);
 private final Paint ok = Gfx.text(Design.semi, 11, 0xCCFFFFFF, 0f);
 private final Paint okBox = Gfx.stroke(0xCCFFFFFF, 2f);
 private final Paint glyph = Gfx.text(Design.semi, 120, 0xFFFFFFFF, -0.02f);
 private final Paint brk = Gfx.stroke(Design.alpha(Design.CYAN, 0.85f), 2f);
 private final Paint shade = new Paint(), sheen = new Paint();
 private final RectF tmp = new RectF();

 HeroView(Context c) {
  super(c, Design.LEFT);
  clip.addRoundRect(area, 12, 12, Path.Direction.CW);
  float w = area.width(), h = area.height();
  shade.setShader(new LinearGradient(0, area.bottom, 0, area.top + h * 0.42f, new int[]{0xE6020610, 0x59020610, 0x00020610}, new float[]{0f, 0.45f, 1f}, Shader.TileMode.CLAMP));
  sheen.setShader(new LinearGradient(area.left, area.top, area.left + w * 0.33f, area.top + h * 0.5f, 0x1AFFFFFF, 0x00FFFFFF, Shader.TileMode.CLAMP));
  ok.setTextAlign(Paint.Align.CENTER);
  glyph.setTextAlign(Paint.Align.CENTER);
 }

 void setEntry(AppEntry e, boolean animate) {
  if (e == cur) return;
  prev = cur;
  cur = e;
  if (anim != null) anim.cancel();
  if (!animate || prev == null) { t = 1f; prev = null; invalidate(); return; }
  t = 0f;
  anim = ValueAnimator.ofFloat(0f, 1f);
  anim.setDuration(240);
  anim.setInterpolator(new DecelerateInterpolator());
  anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
   @Override public void onAnimationUpdate(ValueAnimator a) { t = (Float) a.getAnimatedValue(); if (t >= 1f) prev = null; invalidate(); }
  });
  anim.start();
 }

 @Override void paintDesign(Canvas c) {
  c.save();
  c.clipPath(clip);
  c.drawColor(Design.PANEL);
  if (prev != null && t < 1f) drawHero(c, prev, 1f - t);
  drawHero(c, cur, prev != null ? t : 1f);
  c.restore();
 }

 private void drawHero(Canvas c, AppEntry e, float alpha) {
  float w = area.width(), h = area.height(), cx = area.centerX();
  if (e == null) {
   cap.setAlpha(Math.round(255 * alpha));
   c.drawText("NO APPS", area.left + 38, area.top + 44, cap);
   return;
  }
  int acc = e.accent;
  int hi = Design.lerp(acc, 0xFFFFFFFF, 0.22f), lo = Design.lerp(acc, 0xFF000000, 0.68f);
  bg.setShader(new RadialGradient(cx, area.top + h * 0.38f, w * 0.72f, new int[]{hi, acc, lo}, new float[]{0f, 0.42f, 1f}, Shader.TileMode.CLAMP));
  bg.setAlpha(Math.round(255 * alpha));
  c.drawRect(area, bg);
  float cy = area.top + h * 0.40f;
  if (e.banner != null) {
   tmp.set(cx - 235, cy - 132, cx + 235, cy + 132);
   Gfx.bitmapCover(c, e.banner, tmp, 22, alpha, art);
  } else if (e.icon != null) {
   tmp.set(cx - 80, cy - 100, cx + 80, cy + 60);
   Gfx.bitmapFit(c, e.icon, tmp, alpha, art);
  } else if (e.glyph != null) {
   glyph.setAlpha(Math.round(255 * alpha));
   c.drawText(e.glyph, cx, cy + 40, glyph);
  }
  shade.setAlpha(Math.round(255 * alpha));
  c.drawRect(area, shade);
  sheen.setAlpha(Math.round(255 * alpha));
  c.drawRect(area, sheen);
  cap.setAlpha(Math.round(191 * alpha));
  c.drawText("NOW SELECTED", area.left + 38, area.top + 44, cap);
  title.setAlpha(Math.round(255 * alpha));
  c.drawText(e.label, area.left + 36, area.bottom - 66, title);
  okBox.setAlpha(Math.round(204 * alpha));
  tmp.set(area.left + 38, area.bottom - 50, area.left + 68, area.bottom - 30);
  c.drawRoundRect(tmp, 6, 6, okBox);
  ok.setAlpha(Math.round(204 * alpha));
  c.drawText("OK", tmp.centerX(), tmp.bottom - 6, ok);
  hint.setAlpha(Math.round(204 * alpha));
  c.drawText(e.type == AppEntry.INPUT ? "Press to switch" : "Press to open", area.left + 78, area.bottom - 33, hint);
  brk.setAlpha(Math.round(217 * alpha));
  Gfx.brackets(c, area, 12, 26, brk);
 }
}
