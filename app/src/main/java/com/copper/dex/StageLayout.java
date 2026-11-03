package com.copper.dex;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

/** Letterboxes a 1920x1080 stage into whatever size it gets and positions the DViews on it. */
final class StageLayout extends ViewGroup {
 StageLayout(Context c) { super(c); }

 @Override protected void onMeasure(int w, int h) {
  setMeasuredDimension(MeasureSpec.getSize(w), MeasureSpec.getSize(h));
 }

 @Override protected void onLayout(boolean changed, int l, int t, int r, int b) {
  int w = r - l, h = b - t;
  float k = Math.min(w / (float) Design.W, h / (float) Design.H);
  float ox = (w - Design.W * k) / 2f, oy = (h - Design.H * k) / 2f;
  for (int i = 0; i < getChildCount(); i++) {
   View v = getChildAt(i);
   if (v instanceof DView) {
    DView d = (DView) v;
    d.k = k;
    int x0 = Math.round(ox + d.dr.left * k), y0 = Math.round(oy + d.dr.top * k);
    int x1 = Math.round(ox + d.dr.right * k), y1 = Math.round(oy + d.dr.bottom * k);
    v.measure(MeasureSpec.makeMeasureSpec(x1 - x0, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(y1 - y0, MeasureSpec.EXACTLY));
    v.layout(x0, y0, x1, y1);
   } else {
    v.measure(MeasureSpec.makeMeasureSpec(w, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(h, MeasureSpec.EXACTLY));
    v.layout(0, 0, w, h);
   }
  }
 }
}
