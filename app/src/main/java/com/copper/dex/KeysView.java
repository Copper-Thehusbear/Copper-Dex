package com.copper.dex;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;

/** The six raised keys: icons, labels and the focus ring. */
final class KeysView extends DView {
 static final String[] LABELS = {"Settings", "Network", "Inputs", "Live TV", "Voice", "Power"};
 private int focus = -1;
 private final Paint icon = Gfx.stroke(0xFFE9EFF4, 1f);
 private final Paint iconFill = Gfx.fill(0xFFE9EFF4);
 private final Paint glow = Gfx.stroke(Design.alpha(0xFFBEE6FF, 0.35f), 1f);
 private final Paint label = Gfx.text(Design.semi, 20, 0xFFFFF0EC, 0.08f);
 private final Paint ring = Gfx.stroke(Design.CYAN, 3f);
 private final Paint ringGlow = Gfx.stroke(Design.alpha(Design.CYAN, 0.28f), 9f);
 private final RectF tmp = new RectF();
 private final Path path = new Path();

 KeysView(Context c) {
  super(c, Design.KEYS);
  label.setTextAlign(Paint.Align.CENTER);
  label.setShadowLayer(2f, 0, 2f, 0x99780000);
 }

 void setFocus(int i) {
  if (i == focus) return;
  focus = i;
  invalidate();
 }

 @Override void paintDesign(Canvas c) {
  for (int i = 0; i < 6; i++) {
   float cx = Design.KEY_X[i], cy = Design.KEY_Y;
   if (i == focus) {
    float h = Design.KEY_SIZE / 2f + 6;
    tmp.set(cx - h, cy - h, cx + h, cy + h);
    c.drawRoundRect(tmp, 26, 26, ringGlow);
    c.drawRoundRect(tmp, 26, 26, ring);
   }
   drawIcon(c, i, cx, cy, 46);
   c.drawText(LABELS[i].toUpperCase(), cx, 1026, label);
  }
 }

 /** Icons are drawn in a 24x24 box scaled to {@code size}. */
 private void drawIcon(Canvas c, int which, float cx, float cy, float size) {
  float s = size / 24f;
  c.save();
  c.translate(cx - size / 2f, cy - size / 2f);
  c.scale(s, s);
  icon.setStrokeWidth(1.9f);
  glow.setStrokeWidth(4.2f);
  for (int pass = 0; pass < 2; pass++) {
   Paint p = pass == 0 ? glow : icon;
   switch (which) {
    case 0: // settings: hub and eight ticks
     c.drawCircle(12, 12, 3, p);
     float[] t = {12, 2.5f, 12, 5.5f, 12, 18.5f, 12, 21.5f, 2.5f, 12, 5.5f, 12, 18.5f, 12, 21.5f, 12,
       5.3f, 5.3f, 7.4f, 7.4f, 16.6f, 16.6f, 18.7f, 18.7f, 5.3f, 18.7f, 7.4f, 16.6f, 16.6f, 7.4f, 18.7f, 5.3f};
     for (int i = 0; i < t.length; i += 4) c.drawLine(t[i], t[i + 1], t[i + 2], t[i + 3], p);
     break;
    case 1: // network: three arcs and a dot
     tmp.set(-3, 5.18f, 27, 35.18f); c.drawArc(tmp, 228.2f, 83.6f, false, p);
     tmp.set(2, 9.64f, 22, 29.64f); c.drawArc(tmp, 225.6f, 88.8f, false, p);
     tmp.set(7, 14.57f, 17, 24.57f); c.drawArc(tmp, 225.6f, 88.8f, false, p);
     if (pass == 1) c.drawCircle(12, 19.5f, 1.2f, iconFill);
     break;
    case 2: // inputs: arrow into a socket
     tmp.set(13, 4.5f, 20.5f, 19.5f); c.drawRoundRect(tmp, 2, 2, p);
     c.drawLine(3, 12, 13, 12, p); c.drawLine(9, 8, 13, 12, p); c.drawLine(13, 12, 9, 16, p);
     break;
    case 3: // live tv
     tmp.set(3, 7, 21, 19.5f); c.drawRoundRect(tmp, 2.2f, 2.2f, p);
     c.drawLine(8, 3, 12, 7, p); c.drawLine(12, 7, 16, 3, p);
     break;
    case 4: // voice: microphone
     tmp.set(9, 2.5f, 15, 14.5f); c.drawRoundRect(tmp, 3, 3, p);
     tmp.set(5, 4, 19, 18); c.drawArc(tmp, 0, 180, false, p);
     c.drawLine(12, 18.5f, 12, 21.5f, p);
     break;
    default: // power
     c.drawLine(12, 3, 12, 12, p);
     tmp.set(4, 4.11f, 20, 20.11f); c.drawArc(tmp, 225, -270, false, p);
     break;
   }
  }
  c.restore();
 }
}
