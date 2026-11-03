package com.copper.dex;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.view.animation.DecelerateInterpolator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/** Right screen: tabs, a wheel list of numbered entries with the focused one enlarged, and an A-Z rail. */
final class ListPanelView extends DView {
 interface Favs { boolean isFav(AppEntry e); }

 private static final String[] TABS = {"ALL APPS", "FAVOURITES", "INPUTS"};
 // wheel layout by offset from the focused row, -4..+4: top, height, alpha, indent (panel coordinates)
 private static final float[] TOP = {84, 128, 184, 246, 326, 454, 530, 594, 650};
 private static final float[] HGT = {40, 46, 54, 68, 118, 68, 56, 48, 40};
 private static final float[] ALP = {0f, 0.20f, 0.38f, 0.62f, 1f, 0.80f, 0.55f, 0.30f, 0f};
 private static final float[] IND = {100, 76, 52, 26, 0, 24, 50, 76, 100};

 private List<AppEntry> items = new ArrayList<>();
 private int tab, sel;
 private final int[] counts = new int[3];
 private float pos;
 private boolean active = true;
 private ValueAnimator anim;
 private Favs favs;
 private Bitmap backdrop;
 private final float pw = Design.RIGHT.width(), ph = Design.RIGHT.height();

 private final Paint fill = Gfx.fill(0), line = Gfx.stroke(0, 1.5f), art = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
 private final Paint num = Gfx.text(Design.light, 16, 0, 0.1f);
 private final Paint name = Gfx.text(Design.light, 30, 0, -0.01f);
 private final Paint cat = Gfx.text(Design.medium, 10, 0, 0.4f);
 private final Paint tabP = Gfx.text(Design.medium, 13, 0, 0.23f);
 private final Paint badge = Gfx.text(Design.medium, 11, 0, 0.08f);
 private final Paint idxBig = Gfx.text(Design.light, 26, 0xFFE9F6FF, 0f);
 private final Paint idxSmall = Gfx.text(Design.medium, 13, Design.GRAY, 0.15f);
 private final Paint letter = Gfx.text(Design.medium, 11, 0xFF4B6073, 0f);
 private final Paint letterOn = Gfx.text(Design.semi, 11, 0xFF04101C, 0f);
 private final Paint glyph = Gfx.text(Design.semi, 20, 0xFFFFFFFF, 0f);
 private final Paint hintP = Gfx.text(Design.regular, 18, Design.GRAY, 0.02f);
 private final Paint fade = new Paint();
 private final RectF r = new RectF(), tile = new RectF();
 private final float[] v = new float[4];
 private final List<Integer> order = new ArrayList<>();

 ListPanelView(Context c) {
  super(c, Design.RIGHT);
  letter.setTextAlign(Paint.Align.CENTER);
  letterOn.setTextAlign(Paint.Align.CENTER);
  idxBig.setTextAlign(Paint.Align.RIGHT);
  glyph.setTextAlign(Paint.Align.CENTER);
  hintP.setTextAlign(Paint.Align.CENTER);
 }

 void setFavs(Favs f) { favs = f; }

 void setCounts(int all, int fav, int inputs) {
  counts[0] = all; counts[1] = fav; counts[2] = inputs;
  invalidate();
 }

 void setItems(List<AppEntry> list, int tabIndex, int selected) {
  items = list;
  tab = tabIndex;
  sel = Math.max(0, Math.min(selected, Math.max(0, list.size() - 1)));
  pos = sel;
  if (anim != null) anim.cancel();
  invalidate();
 }

 void setSelected(int i) {
  if (items.isEmpty()) return;
  sel = Math.max(0, Math.min(i, items.size() - 1));
  if (anim != null) anim.cancel();
  anim = ValueAnimator.ofFloat(pos, sel);
  anim.setDuration(170);
  anim.setInterpolator(new DecelerateInterpolator());
  anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
   @Override public void onAnimationUpdate(ValueAnimator a) { pos = (Float) a.getAnimatedValue(); invalidate(); }
  });
  anim.start();
 }

 void setActive(boolean a) {
  if (a == active) return;
  active = a;
  invalidate();
 }

 // ---- drawing ----

 @Override void paintDesign(Canvas c) {
  c.save();
  c.translate(Design.RIGHT.left, Design.RIGHT.top);
  if (backdrop == null) backdrop = makeBackdrop();
  c.drawBitmap(backdrop, 0, 0, art);
  drawTabs(c);
  drawRows(c);
  fade.setShader(new LinearGradient(0, 80, 0, 150, Design.BG, 0x00040812, Shader.TileMode.CLAMP));
  c.drawRect(0, 80, pw, 150, fade);
  fade.setShader(new LinearGradient(0, ph, 0, ph - 120, Design.BG, 0x00040812, Shader.TileMode.CLAMP));
  c.drawRect(0, ph - 120, pw, ph, fade);
  drawIndexAndRail(c);
  c.restore();
 }

 private Bitmap makeBackdrop() {
  Bitmap b = Bitmap.createBitmap((int) pw, (int) ph, Bitmap.Config.ARGB_8888);
  Canvas c = new Canvas(b);
  Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
  RectF all = new RectF(0, 0, pw, ph);
  p.setColor(Design.BG);
  c.drawRoundRect(all, 14, 14, p);
  p.setShader(new RadialGradient(pw / 2f, -ph * 0.06f, pw * 0.72f, 0x4D288CFF, 0x00288CFF, Shader.TileMode.CLAMP));
  c.drawRoundRect(all, 14, 14, p);
  p.setShader(null);
  p.setColor(Design.alpha(Design.CYAN, 0.045f));
  p.setStrokeWidth(1f);
  for (float x = 0; x < pw; x += 34) c.drawLine(x, 0, x, ph, p);
  for (float y = 0; y < ph; y += 34) c.drawLine(0, y, pw, y, p);
  Paint br = Gfx.stroke(Design.alpha(Design.CYAN, 0.85f), 2f);
  Gfx.brackets(c, all, 12, 26, br);
  for (int i = 0; i < 26; i++) c.drawText(String.valueOf((char) ('A' + i)), pw - 20, railY(i) + 4, letter);
  return b;
 }

 private float railY(int i) { return 104 + i * ((ph - 60 - 104) / 25f); }

 private void drawTabs(Canvas c) {
  float x = 30;
  for (int i = 0; i < 3; i++) {
   boolean on = i == tab;
   tabP.setColor(on ? 0xFFFFFFFF : Design.GRAY);
   float tw = tabP.measureText(TABS[i]);
   String cn = String.valueOf(counts[i]);
   badge.setColor(on ? Design.CYAN : 0xFF9FB4C6);
   float bw = badge.measureText(cn) + 12;
   c.drawText(TABS[i], x + 14, 47, tabP);
   fill.setColor(on ? Design.alpha(Design.CYAN, 0.18f) : 0x14FFFFFF);
   r.set(x + 14 + tw + 8, 33, x + 14 + tw + 8 + bw, 51);
   c.drawRoundRect(r, 8, 8, fill);
   c.drawText(cn, r.left + 6, 46, badge);
   if (on) {
    fill.setColor(Design.CYAN);
    c.drawRect(x, 58, x + 28 + tw + 8 + bw, 60, fill);
   }
   x += 28 + tw + 8 + bw + 10;
  }
 }

 private void drawIndexAndRail(Canvas c) {
  if (items.isEmpty()) return;
  String big = String.format(java.util.Locale.ENGLISH, "%03d", sel + 1);
  float sx = pw - 34;
  String small = "/ " + String.format(java.util.Locale.ENGLISH, "%03d", items.size());
  idxSmall.setTextAlign(Paint.Align.RIGHT);
  c.drawText(small, sx, 46, idxSmall);
  c.drawText(big, sx - idxSmall.measureText(small) - 8, 48, idxBig);
  char ch = items.get(sel).initial();
  if (ch >= 'A' && ch <= 'Z') {
   float y = railY(ch - 'A');
   r.set(pw - 29, y - 8, pw - 11, y + 8);
   fill.setColor(Design.CYAN);
   c.drawRoundRect(r, 6, 6, fill);
   c.drawText(String.valueOf(ch), pw - 20, y + 4, letterOn);
  }
 }

 private void drawRows(Canvas c) {
  if (items.isEmpty()) {
   String msg = tab == 1 ? "Hold OK on an app to add it to favourites" : "Nothing here";
   c.drawText(msg, pw / 2f, ph / 2f, hintP);
   return;
  }
  order.clear();
  for (int i = 0; i < items.size(); i++) if (Math.abs(i - pos) < 4f) order.add(i);
  final float p = pos;
  Collections.sort(order, new Comparator<Integer>() {
   @Override public int compare(Integer a, Integer b) { return Float.compare(Math.abs(b - p), Math.abs(a - p)); }
  });
  for (int i : order) drawRow(c, items.get(i), i, i - pos);
 }

 private void lerpRow(float d) {
  float f = d + 4f;
  int i0 = (int) Math.floor(f);
  if (i0 < 0) i0 = 0;
  if (i0 > 7) i0 = 7;
  float t = Math.max(0f, Math.min(1f, f - i0));
  v[0] = Design.lerp(TOP[i0], TOP[i0 + 1], t);
  v[1] = Design.lerp(HGT[i0], HGT[i0 + 1], t);
  v[2] = Design.lerp(ALP[i0], ALP[i0 + 1], t);
  v[3] = Design.lerp(IND[i0], IND[i0 + 1], t);
 }

 private void drawRow(Canvas c, AppEntry e, int index, float d) {
  lerpRow(d);
  float top = v[0], h = v[1], a = v[2], ind = v[3];
  if (a < 0.02f) return;
  float k = Math.max(0f, 1f - Math.abs(d)) * (active ? 1f : 0.55f);   // how focused this row is
  float left = 24 + ind, right = pw - 46;
  r.set(left, top, right, top + h);
  // row background
  fill.setShader(new LinearGradient(left, 0, right, 0, Design.alpha(Design.lerp(0x0DFFFFFF, 0x4D288CFF, k), a),
    Design.alpha(Design.lerp(0x04FFFFFF, 0x05288CFF, k), a), Shader.TileMode.CLAMP));
  c.drawRoundRect(r, 14, 14, fill);
  fill.setShader(null);
  if (k > 0.05f) {
   line.setStrokeWidth(1.5f);
   line.setColor(Design.alpha(Design.CYAN, 0.75f * k));
   c.drawRoundRect(r, 14, 14, line);
   line.setStrokeWidth(5f);
   line.setColor(Design.alpha(Design.CYAN, 0.10f * k));
   c.drawRoundRect(r, 15, 15, line);
  }
  // number
  num.setTypeface(k > 0.5f ? Design.medium : Design.light);
  num.setTextSize(Design.lerp(16f, 22f, k));
  num.setColor(Design.alpha(Design.lerp(Design.DIM, Design.CYAN, k), a));
  c.drawText(String.format(java.util.Locale.ENGLISH, "%03d", index + 1), left + 18, top + h / 2f + num.getTextSize() * 0.36f, num);
  // tile
  float th = h - 22, tw = th * 1.6f, tx = left + 80, ty = top + 11;
  tile.set(tx, ty, tx + tw, ty + th);
  drawTile(c, e, tile, a, k);
  // name and caption
  float nameSize = h / 2.9f, nx = tx + tw + 16;
  name.setTypeface(k > 0.5f ? Design.regular : Design.light);
  name.setTextSize(nameSize);
  name.setColor(Design.alpha(Design.lerp(0xFFC9D6E0, 0xFFFFFFFF, k), a));
  float group = nameSize * 0.78f + 14f;
  float base = top + (h - group) / 2f + nameSize * 0.78f;
  String label = e.label;
  float maxW = right - 56 - nx;
  while (name.measureText(label) > maxW && label.length() > 3) label = label.substring(0, label.length() - 2);
  if (!label.equals(e.label)) label = label + "…";
  c.drawText(label, nx, base, name);
  boolean fav = favs != null && favs.isFav(e);
  cat.setColor(Design.alpha(Design.lerp(Design.DIM, Design.CYAN, k), a));
  c.drawText(fav ? e.category + "  ★" : e.category, nx, base + 14, cat);
  if (k > 0.3f) {
   fill.setColor(Design.alpha(Design.CYAN, k * a));
   android.graphics.Path tri = new android.graphics.Path();
   float ax = right - 34, ay = top + h / 2f;
   tri.moveTo(ax, ay - 11); tri.lineTo(ax + 16, ay); tri.lineTo(ax, ay + 11); tri.close();
   c.drawPath(tri, fill);
  }
 }

 private void drawTile(Canvas c, AppEntry e, RectF t, float a, float k) {
  float rad = 10;
  if (e.banner != null) {
   Gfx.bitmapCover(c, e.banner, t, rad, a, art);
  } else {
   int acc = e.accent;
   fill.setShader(new LinearGradient(t.left, t.top, t.right, t.bottom, Design.alpha(Design.lerp(acc, 0xFFFFFFFF, 0.12f), a),
     Design.alpha(Design.lerp(acc, 0xFF000000, 0.45f), a), Shader.TileMode.CLAMP));
   c.drawRoundRect(t, rad, rad, fill);
   fill.setShader(null);
   if (e.icon != null) {
    float s = t.height() * 0.66f;
    RectF ir = new RectF(t.centerX() - s / 2, t.centerY() - s / 2, t.centerX() + s / 2, t.centerY() + s / 2);
    Gfx.bitmapFit(c, e.icon, ir, a, art);
   } else if (e.glyph != null) {
    glyph.setTextSize(t.height() * 0.34f);
    glyph.setColor(Design.alpha(0xFFFFFFFF, a));
    c.drawText(e.glyph, t.centerX(), t.centerY() + glyph.getTextSize() * 0.35f, glyph);
   }
  }
  if (k > 0.3f) {
   line.setStrokeWidth(2f);
   line.setColor(Design.alpha(0xFFFFFFFF, k * a));
   c.drawRoundRect(t, rad, rad, line);
  }
 }
}
