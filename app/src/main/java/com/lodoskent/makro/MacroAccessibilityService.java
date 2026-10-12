package com.lodoskent.makro;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import java.util.Random;

public class MacroAccessibilityService extends AccessibilityService {
    public static final String PREFS = "ldk_macro_preferences";
    private static MacroAccessibilityService active;
    private static final int[] COLORS = {
        Color.rgb(27, 138, 221), Color.rgb(211, 77, 57), Color.rgb(34, 164, 105)
    };

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Random random = new Random();
    private final View[] markers = new View[3];
    private final WindowManager.LayoutParams[] markerPositions = new WindowManager.LayoutParams[3];
    private SharedPreferences prefs;
    private WindowManager wm;
    private View bubble;
    private TextView bubbleText;
    private WindowManager.LayoutParams bubblePosition;
    private LinearLayout menu;
    private WindowManager.LayoutParams menuPosition;
    private TextView message;
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
            updateMessage();
            handler.postDelayed(this, 1000);
        }
    };

    public static boolean isConnected() { return active != null; }

    @Override protected void onServiceConnected() {
        super.onServiceConnected();
        active = this;
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        try {
            makeBubble();
            makeMenu();
            handler.post(refresh);
        } catch (RuntimeException ex) {
            Toast.makeText(this, "Yüzen düğme açılamadı.", Toast.LENGTH_LONG).show();
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
        bubbleText = text("M", 22, true);
        bubbleText.setGravity(Gravity.CENTER);
        bubbleText.setBackground(bg(Color.rgb(20, 54, 91), 40));
        bubble = bubbleText;
        bubblePosition = layout(dp(54), dp(54),
            prefs.getInt("bubble_x", dp(8)),
            prefs.getInt("bubble_y", dp(155)));
        wm.addView(bubble, bubblePosition);
        bubble.setOnTouchListener(new View.OnTouchListener() {
            float downX, downY;
            int startX, startY;
            boolean moving;
            @Override public boolean onTouch(View v, MotionEvent e) {
                switch (e.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        downX = e.getRawX();
                        downY = e.getRawY();
                        startX = bubblePosition.x;
                        startY = bubblePosition.y;
                        moving = false;
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        int dx = Math.round(e.getRawX() - downX);
                        int dy = Math.round(e.getRawY() - downY);
                        if (Math.abs(dx) + Math.abs(dy) > dp(8)) moving = true;
                        if (moving) {
                            bubblePosition.x = bound(startX + dx, 0, Math.max(0, width() - dp(54)));
                            bubblePosition.y = bound(startY + dy, 0, Math.max(0, height() - dp(54)));
                            wm.updateViewLayout(bubble, bubblePosition);
                        }
                        return true;
                    case MotionEvent.ACTION_UP:
                        if (moving) {
                            prefs.edit().putInt("bubble_x", bubblePosition.x)
                                .putInt("bubble_y", bubblePosition.y).apply();
                        } else {
                            if (menuShowing) hideMenu();
                            else showMenu();
                        }
                        return true;
                    default:
                        return true;
                }
            }
        });
    }

    private void makeMenu() {
        menu = new LinearLayout(this);
        menu.setOrientation(LinearLayout.VERTICAL);
        menu.setPadding(dp(12), dp(12), dp(12), dp(12));
        menu.setBackground(bg(Color.rgb(21, 36, 55), 14));

        menu.addView(text("LDK MAKRO  •  Kontrol", 16, true));
        message = text("Hazır", 12, false);
        message.setPadding(0, dp(6), 0, dp(6));
        menu.addView(message);

        addAction("①②③  Noktaları yerleştir", () -> {
            if (running) {
                toast("Önce makroyu durdur.");
                return;
            }
            showMarkers();
            hideMenu();
            toast("1 Hedef, 2 Saldır, 3 Konuşlandır. Kaydetmek için M'ye bas.");
        });
        addAction("Konumları kaydet", () -> {
            if (!markersShowing) {
                toast("Önce noktaları yerleştir.");
                return;
            }
            saveMarkers();
            hideMarkers();
            toast("Noktalar kaydedildi.");
        });
        addAction("▶  BAŞLAT", this::start);
        addAction("■  DURDUR", () -> stop("Elle durduruldu"));
        menu.addView(text("45–90 sn rastgele bekleme. Ara sıra 1–5 dk ek mola.", 11, false));
        menuPosition = layout(dp(298), WindowManager.LayoutParams.WRAP_CONTENT, dp(65), dp(140));
    }

    private void addAction(String title, Runnable fn) {
        Button b = new Button(this);
        b.setText(title);
        b.setAllCaps(false);
        b.setTextSize(14);
        b.setTextColor(Color.WHITE);
        b.setBackground(bg(Color.rgb(51, 81, 118), 7));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(46));
        lp.bottomMargin = dp(6);
        menu.addView(b, lp);
        b.setOnClickListener(v -> fn.run());
    }

    private void showMenu() {
        if (menu == null || menuShowing) return;
        int x = bubblePosition.x + dp(54);
        if (x + dp(298) > width()) x = bubblePosition.x - dp(298);
        menuPosition.x = bound(x, 0, Math.max(0, width() - dp(298)));
        menuPosition.y = bound(bubblePosition.y, 0, Math.max(0, height() - dp(370)));
        wm.addView(menu, menuPosition);
        menuShowing = true;
        updateMessage();
    }

    private void hideMenu() {
        if (!menuShowing) return;
        remove(menu);
        menuShowing = false;
    }

    private void showMarkers() {
        if (markersShowing) return;
        String[] ids = {"1", "2", "3"};
        int w = width(), h = height(), size = dp(48);
        for (int i = 0; i < 3; i++) {
            final int index = i;
            TextView circle = text(ids[i], 21, true);
            circle.setGravity(Gravity.CENTER);
            circle.setBackground(bg(COLORS[i], 30));
            int cx = prefs.getInt("x" + i, w / 2);
            int cy = prefs.getInt("y" + i, h / 2 + (i - 1) * dp(85));
            WindowManager.LayoutParams p = layout(size, size,
                bound(cx - size / 2, 0, Math.max(0, w - size)),
                bound(cy - size / 2, 0, Math.max(0, h - size)));
            markers[index] = circle;
            markerPositions[index] = p;
            circle.setOnTouchListener(new View.OnTouchListener() {
                float x, y;
                int baseX, baseY;
                @Override public boolean onTouch(View v, MotionEvent e) {
                    switch (e.getActionMasked()) {
                        case MotionEvent.ACTION_DOWN:
                            x = e.getRawX();
                            y = e.getRawY();
                            baseX = markerPositions[index].x;
                            baseY = markerPositions[index].y;
                            return true;
                        case MotionEvent.ACTION_MOVE:
                            markerPositions[index].x = bound(baseX + Math.round(e.getRawX() - x),
                                0, Math.max(0, width() - dp(48)));
                            markerPositions[index].y = bound(baseY + Math.round(e.getRawY() - y),
                                0, Math.max(0, height() - dp(48)));
                            wm.updateViewLayout(markers[index], markerPositions[index]);
                            return true;
                        default: return true;
                    }
                }
            });
            wm.addView(circle, p);
        }
        markersShowing = true;
    }

    private void saveMarkers() {
        SharedPreferences.Editor edit = prefs.edit();
        for (int i = 0; i < 3; i++) {
            edit.putInt("x" + i, markerPositions[i].x + dp(24));
            edit.putInt("y" + i, markerPositions[i].y + dp(24));
        }
        edit.putBoolean("points_set", true).apply();
    }

    private void hideMarkers() {
        for (int i = 0; i < 3; i++) {
            remove(markers[i]);
            markers[i] = null;
            markerPositions[i] = null;
        }
        markersShowing = false;
    }

    private void start() {
        if (running) {
            toast("Makro zaten çalışıyor.");
            return;
        }
        if (!prefs.getBoolean("points_set", false)) {
            toast("Önce üç noktayı kaydet.");
            return;
        }
        if (markersShowing) hideMarkers();
        hideMenu();
        running = true;
        cycles = 0;
        nextCycleTime = 0;
        nextPauseTime = SystemClock.uptimeMillis() + randomSeconds(240, 480);
        label = "Çalışıyor";
        bubbleText.setText("▶");
        bubbleText.setBackground(bg(Color.rgb(23, 126, 82), 40));
        handler.removeCallbacks(next);
        handler.post(next);
        toast("Başladı. Durdurmak için M / ▶ simgesine dokun.");
    }

    private void stop(String reason) {
        running = false;
        label = reason;
        nextCycleTime = 0;
        handler.removeCallbacks(next);
        if (bubbleText != null) {
            bubbleText.setText("M");
            bubbleText.setBackground(bg(Color.rgb(20, 54, 91), 40));
        }
        updateMessage();
    }

    private void click(int step) {
        if (!running) return;
        if (step > 2) {
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
        String[] names = {"Hedef", "Saldır", "Konuşlandır"};
        label = "Tur " + cycles + " • " + names[step];
        updateMessage();
        Path p = new Path();
        p.moveTo(x, y);
        GestureDescription g = new GestureDescription.Builder()
            .addStroke(new GestureDescription.StrokeDescription(p, 0, 105))
            .build();
        boolean accepted = dispatchGesture(g, new GestureResultCallback() {
            @Override public void onCompleted(GestureDescription done) {
                if (!running) return;
                if (step == 2) schedule();
                else {
                    int delay = bound(prefs.getInt("step_ms", 1100), 300, 5000);
                    handler.postDelayed(() -> click(step + 1), delay);
                }
            }
            @Override public void onCancelled(GestureDescription done) {
                if (!running) return;
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
        int min = bound(prefs.getInt("min_seconds", 45), 45, 90);
        int max = bound(prefs.getInt("max_seconds", 90), min, 90);
        long wait = randomSeconds(min, max);
        long now = SystemClock.uptimeMillis();
        if (now >= nextPauseTime) {
            long rest = randomSeconds(60, 300);
            wait += rest;
            nextPauseTime = now + wait + randomSeconds(240, 480);
            label = "Tur " + cycles + " • ekstra mola";
        } else {
            label = "Tur " + cycles + " • rastgele bekleme";
        }
        nextCycleTime = now + wait;
        handler.removeCallbacks(next);
        handler.postDelayed(next, wait);
        updateMessage();
    }

    private long randomSeconds(int min, int max) {
        return ((long) min + random.nextInt(max - min + 1)) * 1000L;
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
