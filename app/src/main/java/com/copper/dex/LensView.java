package com.copper.dex;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;

import java.io.File;

/** The big round socket at the top left: a profile picture behind glass. Put your own with tools/set-avatar.sh. */
final class LensView extends DView {
 static final float CX = 140, CY = 118, R = 79;
 private final File file;
 private Bitmap avatar;
 private final Paint pic = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
 private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
 private final Paint arc = Gfx.stroke(0x80FFFFFF, 3f);
 private final Matrix m = new Matrix();
 private final RectF box = new RectF();

 LensView(Context c) {
  super(c, new Rect((int) (CX - R - 6), (int) (CY - R - 6), (int) (CX + R + 6), (int) (CY + R + 6)));
  file = new File(c.getFilesDir(), "avatar.img");
  reload();
 }

 void reload() {
  Bitmap b = null;
  if (file.exists()) {
   try {
    BitmapFactory.Options o = new BitmapFactory.Options();
    o.inJustDecodeBounds = true;
    BitmapFactory.decodeFile(file.getPath(), o);
    int sample = 1;
    while (o.outWidth / (sample * 2) >= 2 * R * 2 && o.outHeight / (sample * 2) >= 2 * R * 2) sample *= 2;
    o = new BitmapFactory.Options();
    o.inSampleSize = sample;
    b = BitmapFactory.decodeFile(file.getPath(), o);
   } catch (Throwable ignored) {
   }
  }
  avatar = b;
  invalidate();
 }

 @Override void paintDesign(Canvas c) {
  if (avatar != null) {
   BitmapShader sh = new BitmapShader(avatar, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
   float s = Math.max(2 * R / avatar.getWidth(), 2 * R / avatar.getHeight());
   m.reset();
   m.setScale(s, s);
   m.postTranslate(CX - avatar.getWidth() * s / 2f, CY - avatar.getHeight() * s / 2f);
   sh.setLocalMatrix(m);
   pic.setShader(sh);
   c.drawCircle(CX, CY, R, pic);
   pic.setShader(null);
  } else {
   drawDefault(c);
  }
  // the glass: darker at the edge, a soft highlight and a thin rim reflection
  fill.setShader(new RadialGradient(CX, CY, R, new int[]{0x00000000, 0x00000000, 0x73000000}, new float[]{0f, 0.68f, 1f}, Shader.TileMode.CLAMP));
  c.drawCircle(CX, CY, R, fill);
  fill.setShader(new RadialGradient(CX - R * 0.34f, CY - R * 0.46f, R * 0.95f, 0x59FFFFFF, 0x00FFFFFF, Shader.TileMode.CLAMP));
  c.drawCircle(CX, CY, R, fill);
  fill.setShader(null);
  box.set(CX - R + 5, CY - R + 5, CX + R - 5, CY + R - 5);
  c.drawArc(box, 200, 70, false, arc);
 }

 /** Stand-in until a picture is set: a head and shoulders on a blue disc, like the TV's default profile. */
 private void drawDefault(Canvas c) {
  fill.setShader(new LinearGradient(CX, CY - R, CX, CY + R, 0xFF5B9BFF, 0xFF1B3A8C, Shader.TileMode.CLAMP));
  c.drawCircle(CX, CY, R, fill);
  fill.setShader(null);
  c.save();
  box.set(CX - R, CY - R, CX + R, CY + R);
  android.graphics.Path clip = new android.graphics.Path();
  clip.addCircle(CX, CY, R, android.graphics.Path.Direction.CW);
  c.clipPath(clip);
  fill.setColor(0xE6FFFFFF);
  c.drawCircle(CX, CY - 14, 25, fill);
  box.set(CX - 50, CY + 18, CX + 50, CY + 108);
  c.drawOval(box, fill);
  c.restore();
 }
}
