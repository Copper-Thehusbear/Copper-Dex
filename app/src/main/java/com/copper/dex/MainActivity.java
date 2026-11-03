package com.copper.dex;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.provider.Settings;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

/** Copper Dex: a Pokédex-styled home screen. All drawing is native; see the *View classes. */
public class MainActivity extends Activity {
 private static final int LIST = 0, KEYS = 1;

 private AppRepo repo;
 private ArtLoader loader;
 private ShellView shell;
 private HeroView hero;
 private ListPanelView list;
 private KeysView keys;
 private StandbyView standby;
 private View fade;

 // tabs: 0 favourites, 1 inputs, 2 all apps
 private static final int T_FAV = 0, T_INPUTS = 1, T_ALL = 2;
 private int tab = T_FAV, zone = LIST, keyFocus = 2;
 private final int[] selTab = new int[3];
 private List<AppEntry> current = new ArrayList<>();
 private boolean resumedOnce;

 private final Runnable redraw = new Runnable() {
  @Override public void run() {
   list.invalidate();
   hero.invalidate();
  }
 };

 @Override protected void onCreate(Bundle state) {
  super.onCreate(state);
  Design.load(this);
  getWindow().setBackgroundDrawable(new ColorDrawable(Color.BLACK));
  immersive();

  repo = new AppRepo(this);
  loader = new ArtLoader(this);
  repo.load();

  StageLayout stage = new StageLayout(this);
  shell = new ShellView(this);
  hero = new HeroView(this);
  list = new ListPanelView(this);
  keys = new KeysView(this);
  standby = new StandbyView(this);
  stage.addView(shell);
  stage.addView(new LedView(this));
  stage.addView(hero);
  stage.addView(new ClockView(this));
  stage.addView(list);
  stage.addView(keys);
  stage.addView(standby);
  fade = new View(this);
  fade.setBackgroundColor(Color.BLACK);
  fade.setAlpha(0f);
  stage.addView(fade);
  setContentView(stage);

  list.setFavs(new ListPanelView.Favs() {
   @Override public boolean isFav(AppEntry e) { return repo.isFav(e); }
  });
  showTab(T_FAV);
 }

 private void immersive() {
  getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_FULLSCREEN
    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
 }

 @Override protected void onResume() {
  super.onResume();
  immersive();
  if (resumedOnce && repo.load()) showTab(tab);
  resumedOnce = true;
  fade.animate().alpha(0f).setDuration(220).start();
 }

 @Override protected void onNewIntent(Intent intent) {
  super.onNewIntent(intent);          // Home pressed while we are showing
  if (standby.isShown2()) standby.setShown(false);
  setZone(LIST);
  if (tab != T_FAV) showTab(T_FAV);
  select(0);
 }

 // ---- state ----

 private void showTab(int t) {
  tab = t;
  current = t == T_FAV ? repo.favourites() : t == T_INPUTS ? repo.inputs : repo.apps;
  selTab[t] = Math.max(0, Math.min(selTab[t], Math.max(0, current.size() - 1)));
  list.setCounts(repo.apps.size(), repo.favourites().size(), repo.inputs.size());
  list.setItems(current, t, selTab[t]);
  updateHero(false);
  preload();
 }

 private void select(int i) {
  if (current.isEmpty()) return;
  i = Math.max(0, Math.min(i, current.size() - 1));
  selTab[tab] = i;
  list.setSelected(i);
  updateHero(true);
  preload();
 }

 private void updateHero(boolean animate) {
  hero.setEntry(current.isEmpty() ? null : current.get(selTab[tab]), animate);
 }

 private void preload() {
  int s = selTab[tab];
  for (int i = Math.max(0, s - 5); i <= Math.min(current.size() - 1, s + 5); i++) loader.ensure(current.get(i), redraw);
 }

 private void setZone(int z) {
  zone = z;
  list.setActive(z == LIST);
  keys.setFocus(z == KEYS ? keyFocus : -1);
 }

 // ---- remote ----

 @Override public boolean onKeyDown(int code, KeyEvent e) {
  if (standby.isShown2()) {
   standby.setShown(false);
   return true;
  }
  int sel = selTab[tab], n = current.size();
  switch (code) {
   case KeyEvent.KEYCODE_DPAD_UP:
    if (zone == LIST) { if (sel > 0) select(sel - 1); else setZone(KEYS); } else setZone(LIST);
    return true;
   case KeyEvent.KEYCODE_DPAD_DOWN:
    if (zone == LIST) { if (sel < n - 1) select(sel + 1); else setZone(KEYS); }
    return true;
   case KeyEvent.KEYCODE_DPAD_LEFT:
    if (zone == LIST) showTab((tab + 2) % 3); else { keyFocus = Math.max(0, keyFocus - 1); setZone(KEYS); }
    return true;
   case KeyEvent.KEYCODE_DPAD_RIGHT:
    if (zone == LIST) showTab((tab + 1) % 3); else { keyFocus = Math.min(5, keyFocus + 1); setZone(KEYS); }
    return true;
   case KeyEvent.KEYCODE_DPAD_CENTER:
   case KeyEvent.KEYCODE_ENTER:
   case KeyEvent.KEYCODE_NUMPAD_ENTER:
    if (e.getRepeatCount() == 0) e.startTracking();
    return true;
   case KeyEvent.KEYCODE_BACK:
    return true;
   default:
    return super.onKeyDown(code, e);
  }
 }

 @Override public boolean onKeyLongPress(int code, KeyEvent e) {
  if (code == KeyEvent.KEYCODE_DPAD_CENTER || code == KeyEvent.KEYCODE_ENTER || code == KeyEvent.KEYCODE_NUMPAD_ENTER) {
   if (zone == LIST && !current.isEmpty()) {
    AppEntry en = current.get(selTab[tab]);
    if (en.type == AppEntry.APP) {
     repo.toggleFav(en);
     list.setCounts(repo.apps.size(), repo.favourites().size(), repo.inputs.size());
     if (tab == T_FAV) showTab(T_FAV); else list.invalidate();
     Toast.makeText(this, repo.isFav(en) ? en.label + " added to favourites" : en.label + " removed from favourites", Toast.LENGTH_SHORT).show();
    }
   }
   return true;
  }
  return super.onKeyLongPress(code, e);
 }

 @Override public boolean onKeyUp(int code, KeyEvent e) {
  if (code == KeyEvent.KEYCODE_DPAD_CENTER || code == KeyEvent.KEYCODE_ENTER || code == KeyEvent.KEYCODE_NUMPAD_ENTER) {
   if (!e.isCanceled()) activate();
   return true;
  }
  if (code == KeyEvent.KEYCODE_BACK) {
   onBack();
   return true;
  }
  return super.onKeyUp(code, e);
 }

 @Override public void onBackPressed() {
  onBack();
 }

 private void onBack() {
  if (standby.isShown2()) { standby.setShown(false); return; }
  if (zone == KEYS) { setZone(LIST); return; }
  if (tab != T_FAV) showTab(T_FAV);
 }

 private void activate() {
  if (zone == KEYS) { doKey(keyFocus); return; }
  if (current.isEmpty()) return;
  launch(current.get(selTab[tab]));
 }

 private void doKey(int i) {
  switch (i) {
   case 0: go(new Intent(Settings.ACTION_SETTINGS)); break;
   case 1: go(new Intent(Settings.ACTION_WIFI_SETTINGS)); break;
   case 2: setZone(LIST); showTab(T_INPUTS); break;
   case 3:
    for (AppEntry e : repo.inputs) if (e.pkg.equals("live")) { launch(e); return; }
    go(new Intent("android.settings.TV_INPUT_SETTINGS"));
    break;
   case 4:
    try { startActivity(new Intent(Intent.ACTION_VOICE_COMMAND).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); }
    catch (Exception first) { go(new Intent(Intent.ACTION_ASSIST).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); }
    break;
   default: standby.setShown(true); break;
  }
 }

 private void go(Intent in) {
  try {
   startActivity(in);
  } catch (Exception e) {
   Toast.makeText(this, "Not available on this TV", Toast.LENGTH_SHORT).show();
  }
 }

 private void launch(final AppEntry e) {
  if (e.launch == null) return;
  fade.animate().alpha(1f).setDuration(140).withEndAction(new Runnable() {
   @Override public void run() {
    try {
     startActivity(e.launch);
    } catch (Exception ex) {
     fade.animate().alpha(0f).setDuration(200).start();
     Toast.makeText(MainActivity.this, "Could not open " + e.label, Toast.LENGTH_SHORT).show();
    }
   }
  }).start();
 }
}
