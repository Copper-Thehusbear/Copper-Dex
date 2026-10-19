package com.copper.dex;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.content.ComponentName;
import android.webkit.WebResourceRequest;
import android.graphics.Color;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.provider.Settings;
import android.util.Base64;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.view.KeyEvent;
import android.view.View;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MainActivity extends Activity {
 private WebView web;
 @Override public void onCreate(Bundle state) {
  super.onCreate(state);
  getWindow().getDecorView().setSystemUiVisibility(5894 | View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
  web=new WebView(this);
  web.getSettings().setJavaScriptEnabled(true);
  web.getSettings().setAllowFileAccess(false);
  web.getSettings().setAllowContentAccess(false);
  web.getSettings().setDomStorageEnabled(false);
  web.setBackgroundColor(Color.BLACK);
  web.setWebViewClient(new WebViewClient(){@Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request){return true;}});
  web.addJavascriptInterface(new Bridge(),"CopperDex");
  setContentView(web);
  web.loadUrl("file:///android_asset/index.html");
 }
 @Override public boolean onKeyDown(int key, KeyEvent event) {
  String direction=null;
  if(key==KeyEvent.KEYCODE_DPAD_LEFT) direction="left";
  if(key==KeyEvent.KEYCODE_DPAD_RIGHT) direction="right";
  if(key==KeyEvent.KEYCODE_DPAD_UP) direction="up";
  if(key==KeyEvent.KEYCODE_DPAD_DOWN) direction="down";
  if(key==KeyEvent.KEYCODE_DPAD_CENTER || key==KeyEvent.KEYCODE_ENTER) direction="ok";
  if(direction!=null){ final String val=direction; web.evaluateJavascript("window.copperKey&&window.copperKey('"+val+"')",null);return true; }
  return super.onKeyDown(key,event);
 }
 private final class Bridge {
  @JavascriptInterface public String apps(){
   JSONArray out=new JSONArray(); PackageManager pm=getPackageManager();
   Set<String> seen=new HashSet<>();
   for(String cat:new String[]{Intent.CATEGORY_LEANBACK_LAUNCHER,Intent.CATEGORY_LAUNCHER}){
    Intent query=new Intent(Intent.ACTION_MAIN).addCategory(cat);
    List<ResolveInfo> all=pm.queryIntentActivities(query,0);
    for(ResolveInfo item:all)try{
     String pkg=item.activityInfo.packageName;
     if(pkg.equals(getPackageName()) || !seen.add(pkg))continue;
     JSONObject app=new JSONObject();
     app.put("pkg",pkg); app.put("name",item.loadLabel(pm).toString());
     Drawable icon=item.loadIcon(pm); Bitmap bitmap=Bitmap.createBitmap(96,96,Bitmap.Config.ARGB_8888);
     Canvas canvas=new Canvas(bitmap); icon.setBounds(0,0,96,96); icon.draw(canvas);
     ByteArrayOutputStream stream=new ByteArrayOutputStream(); bitmap.compress(Bitmap.CompressFormat.PNG,90,stream);
     app.put("icon","data:image/png;base64,"+Base64.encodeToString(stream.toByteArray(),Base64.NO_WRAP));
     out.put(app);
    }catch(Exception ignored){}
   }
   return out.toString();
  }
  @JavascriptInterface public void open(String pkg){
   runOnUiThread(()->{
    try{
     PackageManager pm=getPackageManager();
     Intent intent=pm.getLeanbackLaunchIntentForPackage(pkg);
     if(intent==null)intent=pm.getLaunchIntentForPackage(pkg);
     if(intent!=null)startActivity(intent);
    }catch(Exception ignored){}
   });
  }
  @JavascriptInterface public void settings(){runOnUiThread(()->startActivity(new Intent(Settings.ACTION_SETTINGS)));}
  @JavascriptInterface public void wifi(){runOnUiThread(()->startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS)));}
  @JavascriptInterface public void inputs(){runOnUiThread(()->{
   try{ startActivity(new Intent("android.settings.TV_INPUT_SETTINGS")); }
   catch(Exception ignored){startActivity(new Intent(Settings.ACTION_SETTINGS));}
  });}
 }
 @Override public void onBackPressed(){web.evaluateJavascript("window.copperBack&&window.copperBack()",null);}
}
