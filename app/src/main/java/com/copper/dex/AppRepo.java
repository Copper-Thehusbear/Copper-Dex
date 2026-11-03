package com.copper.dex;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.media.tv.TvContract;
import android.media.tv.TvInputInfo;
import android.media.tv.TvInputManager;
import android.os.Build;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Installed apps, favourites and TV inputs. */
final class AppRepo {
 private static final String[] PINNED = {"com.netflix.ninja", "com.google.android.youtube.tv", "com.amazon.amazonvideo.livingroom",
   "com.disney.disneyplus", "com.spotify.tv.android", "com.wbd.stream", "com.plexapp.android"};
 private static final String[] LIVE_TV = {"com.google.android.tv", "com.tcl.tv", "com.tcl.livetv"};

 private final Context ctx;
 private final PackageManager pm;
 private final SharedPreferences prefs;
 final List<AppEntry> apps = new ArrayList<>();
 final List<AppEntry> inputs = new ArrayList<>();
 private final Set<String> favs = new HashSet<>();

 AppRepo(Context c) {
  ctx = c;
  pm = c.getPackageManager();
  prefs = c.getSharedPreferences("copper", Context.MODE_PRIVATE);
 }

 /** Rebuilds the lists; returns false (and keeps the old entries) when the set of apps has not changed. */
 boolean load() {
  List<AppEntry> old = new ArrayList<>(apps);
  apps.clear();
  Set<String> seen = new HashSet<>();
  for (String cat : new String[]{Intent.CATEGORY_LEANBACK_LAUNCHER, Intent.CATEGORY_LAUNCHER}) {
   List<ResolveInfo> list = pm.queryIntentActivities(new Intent(Intent.ACTION_MAIN).addCategory(cat), 0);
   for (ResolveInfo r : list) {
    String pkg = r.activityInfo.packageName;
    if (pkg.equals(ctx.getPackageName()) || !seen.add(pkg)) continue;
    AppEntry e = new AppEntry(AppEntry.APP, pkg, String.valueOf(r.loadLabel(pm)), categoryOf(r.activityInfo.applicationInfo, pkg));
    Intent in = pm.getLeanbackLaunchIntentForPackage(pkg);
    if (in == null) in = pm.getLaunchIntentForPackage(pkg);
    if (in == null) continue;
    e.launch = in;
    apps.add(e);
   }
  }
  final Collator col = Collator.getInstance();
  Collections.sort(apps, new Comparator<AppEntry>() {
   @Override public int compare(AppEntry a, AppEntry b) { return col.compare(a.label, b.label); }
  });
  boolean same = old.size() == apps.size();
  for (int i = 0; same && i < old.size(); i++) same = old.get(i).pkg.equals(apps.get(i).pkg);
  if (same) { apps.clear(); apps.addAll(old); }
  favs.clear();
  if (prefs.contains("favs")) {
   favs.addAll(prefs.getStringSet("favs", new HashSet<String>()));
  } else {
   for (String p : PINNED) favs.add(p);
  }
  loadInputs();
  return !same;
 }

 private String categoryOf(ApplicationInfo ai, String pkg) {
  if (pkg.startsWith("com.tcl.tv") || pkg.equals("com.google.android.tv")) return "LIVE TV";
  if (Build.VERSION.SDK_INT >= 26) {
   switch (ai.category) {
    case ApplicationInfo.CATEGORY_GAME: return "GAME";
    case ApplicationInfo.CATEGORY_AUDIO: return "MUSIC";
    case ApplicationInfo.CATEGORY_VIDEO: return "STREAMING";
    case ApplicationInfo.CATEGORY_IMAGE: return "PHOTOS";
    case ApplicationInfo.CATEGORY_SOCIAL: return "SOCIAL";
    case ApplicationInfo.CATEGORY_NEWS: return "NEWS";
    case ApplicationInfo.CATEGORY_MAPS: return "MAPS";
    case ApplicationInfo.CATEGORY_PRODUCTIVITY: return "TOOLS";
    default: break;
   }
  }
  return "APP";
 }

 private void loadInputs() {
  inputs.clear();
  AppEntry live = new AppEntry(AppEntry.INPUT, "live", "Live TV", "BROADCAST");
  live.glyph = "TV";
  live.accent = 0xFF14C8D4;
  for (String p : LIVE_TV) {
   Intent in = pm.getLeanbackLaunchIntentForPackage(p);
   if (in == null) in = pm.getLaunchIntentForPackage(p);
   if (in != null) { live.launch = in; break; }
  }
  if (live.launch != null) inputs.add(live);
  try {
   TvInputManager tim = (TvInputManager) ctx.getSystemService(Context.TV_INPUT_SERVICE);
   if (tim == null) return;
   int n = 0;
   for (TvInputInfo info : tim.getTvInputList()) {
    if (!info.isPassthroughInput()) continue;
    CharSequence l = info.loadCustomLabel(ctx);
    if (l == null || l.length() == 0) l = info.loadLabel(ctx);
    AppEntry e = new AppEntry(AppEntry.INPUT, info.getId(), String.valueOf(l), info.getType() == TvInputInfo.TYPE_HDMI ? "HDMI" : "INPUT");
    e.glyph = info.getType() == TvInputInfo.TYPE_HDMI ? "HDMI" : "AV";
    e.accent = 0xFF2B6CFF;
    e.launch = new Intent(Intent.ACTION_VIEW, TvContract.buildChannelUriForPassthroughInput(info.getId())).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    inputs.add(e);
    n++;
   }
  } catch (Exception ignored) {
  }
 }

 List<AppEntry> favourites() {
  List<AppEntry> out = new ArrayList<>();
  for (AppEntry e : apps) if (favs.contains(e.pkg)) out.add(e);
  return out;
 }

 boolean isFav(AppEntry e) { return favs.contains(e.pkg); }

 void toggleFav(AppEntry e) {
  if (e.type != AppEntry.APP) return;
  if (!favs.remove(e.pkg)) favs.add(e.pkg);
  prefs.edit().putStringSet("favs", new HashSet<>(favs)).apply();
 }
}
