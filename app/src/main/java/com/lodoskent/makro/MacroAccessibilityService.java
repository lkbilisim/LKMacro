package com.lodoskent.makro;

import android.accessibilityservice.AccessibilityService;
import android.app.AlertDialog;
import android.accessibilityservice.GestureDescription;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import android.text.InputType;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import java.util.Random;
import java.util.ArrayList;

public class MacroAccessibilityService extends AccessibilityService {
    // Keep the old preferences file so upgrades retain previously saved settings.
    public static final String PREFS = "ldk_macro_preferences";
    private static MacroAccessibilityService active;
    private static final int[] COLORS = {
        Color.rgb(27, 138, 221), Color.rgb(211, 77, 57), Color.rgb(34, 164, 105)
    };

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Random random = new Random();
    private final ArrayList<View> markers = new ArrayList<>();
    private final ArrayList<WindowManager.LayoutParams> markerPositions = new ArrayList<>();
    private SharedPreferences prefs;
    private WindowManager wm;
    private View bubble;
    private TextView bubbleText;
    private WindowManager.LayoutParams bubblePosition;
    private LinearLayout menu;
    private WindowManager.LayoutParams menuPosition;
    private TextView message;
    private TextView pointCountLabel;
    private int activeCount = 3;
    private int generation = 0;
    private long lastRecoveryAttempt = 0;
    private boolean shuttingDown = false;
    private boolean menuShowing, markersShowing, running;
    private long nextCycleTime, nextPauseTime;
    private int cycles;
    private String label = "Hazır";

    private final Runnable next = new Runnable() {
        @Override public void run() {
            if (!running) return;
            nextCycleTime = 0;
            cycles++;
            click(0);
        }
    };

    private final Runnable refresh = new Runnable() {
        @Override public void run() {
            if (active != MacroAccessibilityService.this || shuttingDown) return;
            // Xiaomi/Android can move or detach an accessibility overlay
            // when the current window changes. Check without interrupting taps.
            ensureBubbleVisible();
            updateMessage();
            handler.postDelayed(this, 2000);
        }
    };

    public static boolean isConnected() {
        MacroAccessibilityService service = active;
        return service != null && service.wm != null && !service.shuttingDown;
    }

    public static boolean isBubbleVisible() {
        MacroAccessibilityService service = active;
        return service != null && !service.shuttingDown
            && service.bubble != null && service.bubble.isAttachedToWindow();
    }

    /** Safe to invoke from the Activity even when the user closed the menu with X. */
    public static boolean restoreBubble() {
        MacroAccessibilityService service = active;
        if (service == null || service.shuttingDown || service.wm == null) return false;
        service.handler.post(() -> service.restoreOverlay());
        return true;
    }

    private void reportError(String phase, RuntimeException exception) {
        Log.e("LKMacro", phase, exception);
        if (prefs != null) {
            String detail = phase + ": " + exception.getClass().getSimpleName()
                + (exception.getMessage() != null ? " — " + exception.getMessage() : "");
            prefs.edit().putString("last_service_error", detail).apply();
        }
    }

    @Override protected void onServiceConnected() {
        super.onServiceConnected();
        shuttingDown = false;
        active = this;
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        try {
            makeBubble();
        } catch (RuntimeException ex) {
            reportError("LK düğmesi oluşturulamadı", ex);
        }
        try {
            makeMenu();
        } catch (RuntimeException ex) {
            reportError("LK paneli oluşturulamadı", ex);
        }
        handler.removeCallbacks(refresh);
        handler.post(refresh);
        Log.i("LKMacro", "Accessibility service connected; bubble="+(bubble != null && bubble.isAttachedToWindow()));
    }

    private void restoreOverlay() {
        if (wm == null || shuttingDown) return;
        try {
            hideMenu();
            if (running) stop("Düğme kurtarıldı — güvenlik için makro durduruldu");
            // Always use a known visible location for an explicit recovery.
            int x = Math.min(dp(18), Math.max(0, width() - dp(62)));
            int y = Math.min(dp(220), Math.max(0, height() - dp(150)));
            prefs.edit().putInt("bubble_x", x).putInt("bubble_y", y).apply();
            if (bubble != null && bubble.isAttachedToWindow()) {
                bubblePosition.x = x;
                bubblePosition.y = y;
                wm.updateViewLayout(bubble, bubblePosition);
            } else {
                remove(bubble);
                bubble = null;
                makeBubble();
            }
            if (menu == null) makeMenu();
            prefs.edit().remove("last_service_error").apply();
            toast("LK düğmesi sol tarafta yeniden açıldı.");
        } catch (RuntimeException ex) {
            reportError("Düğme geri getirilemedi", ex);
            toast("LK düğmesi açılamadı; hizmeti kapatıp aç.");
        }
    }

    private void ensureBubbleVisible() {
        if (wm == null || shuttingDown) return;
        long now = SystemClock.uptimeMillis();
        if (bubble == null || !bubble.isAttachedToWindow()) {
            if (now - lastRecoveryAttempt < 10000) return;
            lastRecoveryAttempt = now;
            try {
                remove(bubble);
                bubble = null;
                makeBubble();
            } catch (RuntimeException ex) {
                reportError("Yüzen düğme otomatik kurtarılamadı", ex);
            }
            return;
        }
        if (bubblePosition != null) {
            int newX = bound(bubblePosition.x, dp(8), Math.max(dp(8), width() - dp(70)));
            int newY = bound(bubblePosition.y, dp(55), Math.max(dp(55), height() - dp(125)));
            if (newX != bubblePosition.x || newY != bubblePosition.y) {
                bubblePosition.x = newX;
                bubblePosition.y = newY;
                try {
                    wm.updateViewLayout(bubble, bubblePosition);
                    prefs.edit().putInt("bubble_x", newX).putInt("bubble_y", newY).apply();
                } catch (RuntimeException ex) {
                    reportError("Düğme konumu düzeltilemedi", ex);
                }
            }
        }
    }

    @Override public void onAccessibilityEvent(AccessibilityEvent event) { }
    @Override public void onInterrupt() { stop("Hizmet kesintiye uğradı"); }
    @Override public boolean onUnbind(android.content.Intent intent) {
        clear();
        return super.onUnbind(intent);
    }
    @Override public void onDestroy() {
        clear();
        super.onDestroy();
    }

    private void clear() {
        shuttingDown = true;
        stop("Hizmet kapandı");
        handler.removeCallbacks(refresh);
        hideMarkers();
        hideMenu();
        remove(bubble);
        bubble = null;
        if (active == this) active = null;
    }

    private WindowManager.LayoutParams layout(int w, int h, int x, int y) {
        WindowManager.LayoutParams p = new WindowManager.LayoutParams(
            w, h,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL |
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            android.graphics.PixelFormat.TRANSLUCENT
        );
        p.gravity = Gravity.TOP | Gravity.START;
        p.x = x;
        p.y = y;
        return p;
    }

    private GradientDrawable bg(int color, int radius) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(radius));
        d.setStroke(dp(1), Color.argb(92, 255, 255, 255));
        return d;
    }

    private TextView text(String s, int size, boolean bold) {
        TextView v = new TextView(this);
        v.setText(s);
        v.setTextColor(Color.WHITE);
        v.setTextSize(size);
        v.setGravity(Gravity.CENTER_VERTICAL);
        if (bold) v.setTypeface(null, android.graphics.Typeface.BOLD);
        return v;
    }

    private void makeBubble() {
        FrameLayout button=new FrameLayout(this);
        button.setBackground(UiKit.gradient(this,
            new int[]{0xff10366c, 0xff0c2349}, 45, 0xff58d6ff, 2));
        button.setElevation(dp(10));
        ImageView logo=UiKit.logo(this,50);
        FrameLayout.LayoutParams imageParams=new FrameLayout.LayoutParams(dp(48),dp(48),Gravity.CENTER);
        button.addView(logo,imageParams);
        bubbleText=text("●",13,true);
        bubbleText.setTextColor(0xff83e6ff);
        bubbleText.setGravity(Gravity.CENTER);
        bubbleText.setBackground(UiKit.gradient(this,
            new int[]{0xff0c2d55,0xff071830},20,0xff419de5,1));
        FrameLayout.LayoutParams dot=new FrameLayout.LayoutParams(dp(21),dp(21),Gravity.TOP|Gravity.RIGHT);
        button.addView(bubbleText,dot);
        bubble=button;
        int safeX = bound(prefs.getInt("bubble_x", dp(18)), dp(8),
            Math.max(dp(8), width() - dp(70)));
        int safeY = bound(prefs.getInt("bubble_y", dp(155)), dp(55),
            Math.max(dp(55), height() - dp(125)));
        bubblePosition = layout(dp(62), dp(62), safeX, safeY);
        wm.addView(bubble,bubblePosition);
        bubble.setOnTouchListener(new View.OnTouchListener() {
            float downX,downY;
            int startX,startY;
            boolean moving;
            @Override public boolean onTouch(View v, MotionEvent e) {
                switch(e.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        downX=e.getRawX();
                        downY=e.getRawY();
                        startX=bubblePosition.x;
                        startY=bubblePosition.y;
                        moving=false;
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        int dx=Math.round(e.getRawX()-downX);
                        int dy=Math.round(e.getRawY()-downY);
                        if(Math.abs(dx)+Math.abs(dy)>dp(8))moving=true;
                        if(moving){
                            bubblePosition.x=bound(startX+dx,0,Math.max(0,width()-dp(62)));
                            bubblePosition.y=bound(startY+dy,0,Math.max(0,height()-dp(62)));
                            wm.updateViewLayout(bubble,bubblePosition);
                        }
                        return true;
                    case MotionEvent.ACTION_UP:
                        if(moving) prefs.edit().putInt("bubble_x",bubblePosition.x)
                            .putInt("bubble_y",bubblePosition.y).apply();
                        else {
                            if(menuShowing)hideMenu();
                            else showMenu();
                        }
                        return true;
                    default:return true;
                }
            }
        });
    }

    private TextView overlayText(String text,int size,boolean weight){
        TextView v=UiKit.label(this,text,size,Color.WHITE,weight);
        v.setGravity(Gravity.CENTER_VERTICAL);
        return v;
    }

    private void makeMenu() {
        menu=new LinearLayout(this);
        menu.setOrientation(LinearLayout.VERTICAL);
        menu.setPadding(dp(15),dp(14),dp(15),dp(15));
        menu.setBackground(UiKit.gradient(this,
            new int[]{0xf5081a37,0xf20a1429,0xf5081022},
            21,0xff4872ad,1.3f));
        menu.setElevation(dp(14));
        LinearLayout header=new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        ImageView emblem=UiKit.logo(this,39);
        header.addView(emblem,new LinearLayout.LayoutParams(dp(39),dp(39)));
        TextView title=overlayText("  LK Makro",19,true);
        header.addView(title,new LinearLayout.LayoutParams(0,dp(44),1f));
        TextView close=UiKit.center(this,"×",26,0xffc8daff,true);
        close.setBackground(UiKit.field(this,11));
        header.addView(close,new LinearLayout.LayoutParams(dp(38),dp(38)));
        close.setOnClickListener(v->hideMenu());
        menu.addView(header);
        message=overlayText("●  Hazır",12,false);
        message.setTextColor(0xff81d7f5);
        message.setPadding(dp(3),dp(8),0,dp(11));
        menu.addView(message);

        menu.addView(overlayText("Kaç tane tuş açılsın?",14,true));
        LinearLayout countRow=new LinearLayout(this);
        countRow.setGravity(Gravity.CENTER);
        countRow.setPadding(dp(4),dp(6),dp(4),dp(6));
        countRow.setBackground(UiKit.field(this,13));
        Button less=new Button(this);
        less.setText("−");
        less.setAllCaps(false);
        less.setTextSize(20);
        less.setTextColor(Color.WHITE);
        less.setBackground(UiKit.gradient(this,new int[]{0xff183b70,0xff153056},10,0xff4474b8,1));
        countRow.addView(less,new LinearLayout.LayoutParams(dp(48),dp(40)));
        pointCountLabel=UiKit.center(this,String.valueOf(getPointCount()),22,0xff64e5ff,true);
        countRow.addView(pointCountLabel,new LinearLayout.LayoutParams(0,dp(42),1f));
        Button more=new Button(this);
        more.setText("+");
        more.setAllCaps(false);
        more.setTextSize(20);
        more.setTextColor(Color.WHITE);
        more.setBackground(UiKit.gradient(this,new int[]{0xff183b70,0xff153056},10,0xff4474b8,1));
        countRow.addView(more,new LinearLayout.LayoutParams(dp(48),dp(40)));
        less.setOnClickListener(v->setPointCount(getPointCount()-1));
        more.setOnClickListener(v->setPointCount(getPointCount()+1));
        LinearLayout.LayoutParams countLp=new LinearLayout.LayoutParams(-1,dp(53));
        countLp.bottomMargin=dp(8);
        menu.addView(countRow,countLp);
        pointCountLabel.setOnClickListener(v->promptPointCount());
        addAction("▦   Sayı Girerek Tuş Seç",this::promptPointCount);
        addAction("◎   Noktaları Yerleştir",()->{
            if(running){toast("Önce makroyu durdur.");return;}
            showMarkers();
            hideMenu();
            toast(getPointCount()+" nokta açıldı. İşaretleri sürükleyip M üzerinden kaydet.");
        });
        addAction("▣   Konumları Kaydet",()->{
            if(!markersShowing){toast("Önce noktaları yerleştir.");return;}
            saveMarkers();
            hideMarkers();
            toast("Noktalar kaydedildi.");
        });
        addAction("▶   Başlat",this::start);
        addAction("■   Durdur",()->stop("Elle durduruldu"));
        TextView foot=overlayText("Süreleri uygulamanın Ayarlar ekranında değiştirebilirsin.",11,false);
        foot.setTextColor(0xff99b3d4);
        foot.setPadding(dp(4),dp(5),dp(3),0);
        menu.addView(foot);
        menuPosition=layout(dp(309),WindowManager.LayoutParams.WRAP_CONTENT,dp(66),dp(140));
    }

    private void addAction(String title, Runnable fn) {
        TextView b=UiKit.center(this,title,14,Color.WHITE,true);
        b.setGravity(Gravity.CENTER_VERTICAL|Gravity.LEFT);
        b.setPadding(dp(15),0,dp(7),0);
        boolean start=title.contains("Başlat");
        boolean stop=title.contains("Durdur");
        if(start)b.setBackground(UiKit.gradient(this,
            new int[]{0xff4865f0,0xff2baff2,0xff4ae0dd},12,0xff90dafa,1));
        else if(stop)b.setBackground(UiKit.gradient(this,
            new int[]{0xff603447,0xff3b2638},12,0xffb46275,1));
        else b.setBackground(UiKit.gradient(this,
            new int[]{0xff172c4c,0xff0d2039},12,0xff405d88,1));
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(46));
        lp.bottomMargin=dp(7);
        menu.addView(b,lp);
        b.setOnClickListener(v->fn.run());
    }

    private int getPointCount() {
        return bound(prefs.getInt("point_count", 3), 1, 100);
    }

    private void setPointCount(int requested) {
        if (running) {
            toast("Tuş sayısını değiştirmek için önce durdur.");
            return;
        }
        if (requested < 1 || requested > 100) {
            toast("Tuş sayısı 1 ile 100 arasında olmalı.");
            return;
        }
        if (requested != getPointCount()) {
            if (markersShowing) hideMarkers();
            prefs.edit().putInt("point_count", requested)
                .putBoolean("points_set", false).apply();
            toast(requested + " tuş seçildi. Noktaları yeniden kaydet.");
        }
        pointCountLabel.setText(String.valueOf(getPointCount()));
    }

    private void promptPointCount() {
        if (running) {
            toast("Tuş sayısını değiştirmeden önce makroyu durdur.");
            return;
        }
        final EditText input = new EditText(this);
        input.setSingleLine(true);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setText(String.valueOf(getPointCount()));
        input.setSelectAllOnFocus(true);
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(22), dp(4), dp(22), 0);
        content.addView(input, new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT));
        AlertDialog dialog = new AlertDialog.Builder(this)
            .setTitle("Kaç tane tuş açılsın?")
            .setMessage("1–100 arasında sayı gir. Seçimden sonra noktaları yerleştirip kaydet.")
            .setView(content)
            .setNegativeButton("Vazgeç", (d, which) -> {})
            .setPositiveButton("Uygula", null)
            .create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY);
            dialog.getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        }
        dialog.setOnShowListener(ignored -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                try {
                    int requested = Integer.parseInt(input.getText().toString().trim());
                    if (requested < 1 || requested > 100) {
                        input.setError("1–100 arasında sayı gir.");
                        return;
                    }
                    setPointCount(requested);
                    dialog.dismiss();
                } catch (NumberFormatException ex) {
                    input.setError("Geçerli bir sayı gir.");
                }
            });
            input.requestFocus();
        });
        try {
            dialog.show();
        } catch (RuntimeException error) {
            toast("Sayı giriş penceresi açılamadı.");
        }
    }

    private void showMenu() {
        if (shuttingDown || menuShowing || wm == null) return;
        try {
            ensureBubbleVisible();
            if (menu == null || menuPosition == null) makeMenu();
            if (pointCountLabel != null)
                pointCountLabel.setText(String.valueOf(getPointCount()));
            int x = bubblePosition.x + dp(62);
            if (x + dp(309) > width()) x = bubblePosition.x - dp(309);
            menuPosition.x = bound(x, 0, Math.max(0, width() - dp(309)));
            menuPosition.y = bound(bubblePosition.y, 0,
                Math.max(0, height() - dp(555)));
            wm.addView(menu, menuPosition);
            menuShowing = true;
            updateMessage();
        } catch (RuntimeException ex) {
            menuShowing = false;
            reportError("LK kontrol paneli açılırken hata", ex);
            toast("Panel açılamadı. Ana ekrandan LK düğmesini geri getir.");
        }
    }

    private void hideMenu() {
        if (menuShowing) {
            remove(menu);
            menuShowing = false;
        }
        // X must close the menu only, never the always-on LK bubble.
        if (!shuttingDown) ensureBubbleVisible();
    }

    private void showMarkers() {
        if (markersShowing) return;
        int count = getPointCount();
        int w = width(), h = height(), size = dp(48);
        int columns = Math.max(1, Math.min(5, w / dp(56)));
        // Existing points keep their positions when point count changes.
        for (int i = 0; i < count; i++) {
            final int index = i;
            TextView circle = text(String.valueOf(i + 1), i < 9 ? 21 : 16, true);
            circle.setGravity(Gravity.CENTER);
            circle.setBackground(UiKit.gradient(this, new int[]{0xee143a7b,0xee0b2249}, 40, 0xff52daff, 2.5f));
            circle.setTextColor(0xffffffff);
            circle.setElevation(dp(7));
            int initialX = dp(30) + (i % columns) * dp(57);
            int initialY = dp(180) + (i / columns) * dp(57);
            int cx = prefs.getInt("x" + i, initialX);
            int cy = prefs.getInt("y" + i, initialY);
            WindowManager.LayoutParams p = layout(size, size,
                bound(cx - size / 2, 0, Math.max(0, w - size)),
                bound(cy - size / 2, 0, Math.max(0, h - size)));
            circle.setOnTouchListener(new View.OnTouchListener() {
                float x, y;
                int baseX, baseY;
                @Override public boolean onTouch(View v, MotionEvent e) {
                    switch (e.getActionMasked()) {
                        case MotionEvent.ACTION_DOWN:
                            x = e.getRawX();
                            y = e.getRawY();
                            baseX = markerPositions.get(index).x;
                            baseY = markerPositions.get(index).y;
                            return true;
                        case MotionEvent.ACTION_MOVE:
                            WindowManager.LayoutParams pos = markerPositions.get(index);
                            pos.x = bound(baseX + Math.round(e.getRawX() - x),
                                0, Math.max(0, width() - dp(48)));
                            pos.y = bound(baseY + Math.round(e.getRawY() - y),
                                0, Math.max(0, height() - dp(48)));
                            wm.updateViewLayout(markers.get(index), pos);
                            return true;
                        default: return true;
                    }
                }
            });
            markers.add(circle);
            markerPositions.add(p);
            wm.addView(circle, p);
        }
        markersShowing = true;
    }

    private void saveMarkers() {
        if (!markersShowing || markers.size() != getPointCount()) {
            toast("Tüm noktaları yeniden yerleştir.");
            return;
        }
        SharedPreferences.Editor edit = prefs.edit();
        for (int i = 0; i < markers.size(); i++) {
            edit.putInt("x" + i, markerPositions.get(i).x + dp(24));
            edit.putInt("y" + i, markerPositions.get(i).y + dp(24));
        }
        edit.putInt("saved_point_count", markers.size());
        edit.putBoolean("points_set", true).apply();
    }

    private void hideMarkers() {
        for (View marker : markers) remove(marker);
        markers.clear();
        markerPositions.clear();
        markersShowing = false;
    }

    private void start() {
        if (running) {
            toast("Makro zaten çalışıyor.");
            return;
        }
        activeCount = getPointCount();
        int previouslySaved = prefs.getInt("saved_point_count", 3);
        if (!prefs.getBoolean("points_set", false) || previouslySaved != activeCount) {
            toast("Önce seçtiğin " + activeCount + " noktayı yerleştirip kaydet.");
            return;
        }
        if (markersShowing) hideMarkers();
        hideMenu();
        running = true;
        generation++;
        cycles = 0;
        nextCycleTime = 0;
        nextPauseTime = SystemClock.uptimeMillis() + randomMinutes(
            getRangeMin("pause_interval_min_minutes", "pause_interval_max_minutes", 4),
            getRangeMax("pause_interval_min_minutes", "pause_interval_max_minutes", 4, 8));
        label = "Çalışıyor";
        bubbleText.setText("▶");
        bubbleText.setTextColor(0xffb3ffcb);
        bubbleText.setBackground(UiKit.gradient(this,new int[]{0xff175f46,0xff113e38},20,0xff62e9b0,1));
        handler.removeCallbacks(next);
        handler.post(next);
        toast("Başladı. Durdurmak için M / ▶ simgesine dokun.");
    }

    private void stop(String reason) {
        generation++;
        running = false;
        label = reason;
        nextCycleTime = 0;
        handler.removeCallbacks(next);
        if (bubbleText != null) {
            bubbleText.setText("●");
            bubbleText.setTextColor(0xff83e6ff);
            bubbleText.setBackground(UiKit.gradient(this,new int[]{0xff0c2d55,0xff071830},20,0xff419de5,1));
        }
        updateMessage();
    }

    private void click(int step) {
        if (!running) return;
        final int token = generation;
        if (step >= activeCount) {
            schedule();
            return;
        }
        final int x = prefs.getInt("x" + step, -1);
        final int y = prefs.getInt("y" + step, -1);
        if (x < 0 || y < 0 || x >= width() || y >= height()) {
            stop("Koordinat ekran dışında");
            toast("Noktaları yeniden yerleştir.");
            return;
        }
        label = "Tur " + cycles + " • tuş " + (step + 1) + "/" + activeCount;
        updateMessage();
        Path p = new Path();
        p.moveTo(x, y);
        GestureDescription g = new GestureDescription.Builder()
            .addStroke(new GestureDescription.StrokeDescription(p, 0, 105))
            .build();
        boolean accepted = dispatchGesture(g, new GestureResultCallback() {
            @Override public void onCompleted(GestureDescription done) {
                if (!running || token != generation) return;
                if (step == activeCount - 1) schedule();
                else {
                    int delay = Math.max(1, prefs.getInt("step_ms", 1100));
                    handler.postDelayed(() -> {
                        if (running && token == generation) click(step + 1);
                    }, delay);
                }
            }
            @Override public void onCancelled(GestureDescription done) {
                if (!running || token != generation) return;
                stop("Dokunma iptal edildi");
                toast("Dokunma iptal edildi; kontrol et.");
            }
        }, handler);
        if (!accepted) {
            stop("Sistem dokunmayı kabul etmedi");
            toast("Erişilebilirlik dokunuşu gönderilemedi.");
        }
    }

    private void schedule() {
        if (!running) return;
        int min = getRangeMin("min_seconds", "max_seconds", 45);
        int max = getRangeMax("min_seconds", "max_seconds", 45, 90);
        long wait = prefs.getBoolean("random_enabled", true) ? randomSeconds(min, max) : (long)min * 1000L;
        long now = SystemClock.uptimeMillis();
        if (now >= nextPauseTime) {
            long rest = randomMinutes(
                getRangeMin("rest_min_minutes", "rest_max_minutes", 1),
                getRangeMax("rest_min_minutes", "rest_max_minutes", 1, 5));
            wait += rest;
            nextPauseTime = now + wait + randomMinutes(
                getRangeMin("pause_interval_min_minutes", "pause_interval_max_minutes", 4),
                getRangeMax("pause_interval_min_minutes", "pause_interval_max_minutes", 4, 8));
            label = "Tur " + cycles + " • " + (rest / 60000L) + " dk ek mola";
        } else {
            label = "Tur " + cycles + " • rastgele bekleme";
        }
        nextCycleTime = now + wait;
        handler.removeCallbacks(next);
        handler.postDelayed(next, wait);
        updateMessage();
    }

    private int getRangeMin(String minKey, String maxKey, int fallback) {
        return Math.max(1, prefs.getInt(minKey, fallback));
    }

    private int getRangeMax(String minKey, String maxKey, int fallbackMin, int fallbackMax) {
        return Math.max(getRangeMin(minKey, maxKey, fallbackMin),
            prefs.getInt(maxKey, fallbackMax));
    }

    private long randomUnit(int min, int max) {
        // Long kullanımı, iki büyük pozitif sınırın farkında int taşmasını önler.
        long spread = (long) max - min + 1L;
        long picked = (long) (random.nextDouble() * spread);
        return (long) min + Math.min(spread - 1L, picked);
    }

    private long randomSeconds(int min, int max) {
        return randomUnit(min, max) * 1000L;
    }

    private long randomMinutes(int min, int max) {
        return randomUnit(min, max) * 60000L;
    }

    private void updateMessage() {
        if (message == null) return;
        String s = label;
        if (running && nextCycleTime > 0) {
            long sec = Math.max(0L, (nextCycleTime - SystemClock.uptimeMillis() + 999L) / 1000L);
            s += "\nSonraki tur: " + sec + " sn";
        }
        message.setText(s);
    }

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void remove(View v) {
        if (v == null || wm == null) return;
        try { wm.removeView(v); } catch (IllegalArgumentException ignored) { }
        catch (android.view.WindowManager.BadTokenException error) {
            Log.w("LKMacro", "Overlay zaten kapatılmış", error);
        }
    }

    private int bound(int n, int min, int max) {
        return Math.max(min, Math.min(max, n));
    }

    private int dp(float x) {
        return (int) (getResources().getDisplayMetrics().density * x + 0.5f);
    }

    private int width() {
        Point p = new Point();
        wm.getDefaultDisplay().getRealSize(p);
        return p.x;
    }

    private int height() {
        Point p = new Point();
        wm.getDefaultDisplay().getRealSize(p);
        return p.y;
    }
}
