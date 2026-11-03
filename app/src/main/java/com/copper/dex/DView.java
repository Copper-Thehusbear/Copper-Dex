package com.copper.dex;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.view.View;

/** A view that draws in 1920x1080 design coordinates; StageLayout sets the scale and places it. */
abstract class DView extends View {
 final Rect dr;
 float k = 1f;

 DView(Context c, Rect designRect) {
  super(c);
  dr = designRect;
 }

 @Override protected final void onDraw(Canvas c) {
  c.save();
  c.scale(k, k);
  c.translate(-dr.left, -dr.top);
  paintDesign(c);
  c.restore();
 }

 abstract void paintDesign(Canvas c);
}
