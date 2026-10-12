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

        menu.addView(text("LK MAKRO  •  Kontrol", 16, true));
        message = text("Hazır", 12, false);
        message.setPadding(0, dp(6), 0, dp(6));
        menu.addView(message);

        menu.addView(text("Kaç tane tuş açılsın?", 14, true));
        LinearLayout countRow = new LinearLayout(this);
        countRow.setGravity(Gravity.CENTER);
        Button less = new Button(this);
        less.setText("−");
        less.setTextSize(20);
        countRow.addView(less, new LinearLayout.LayoutParams(0, dp(44), 1f));
        pointCountLabel = text(String.valueOf(getPointCount()), 22, true);
        pointCountLabel.setGravity(Gravity.CENTER);
        countRow.addView(pointCountLabel, new LinearLayout.LayoutParams(0, dp(44), 1f));
        Button more = new Button(this);
        more.setText("+");
        more.setTextSize(20);
        countRow.addView(more, new LinearLayout.LayoutParams(0, dp(44), 1f));
        less.setOnClickListener(v -> setPointCount(getPointCount() - 1));
        more.setOnClickListener(v -> setPointCount(getPointCount() + 1));
        menu.addView(countRow);
        addAction("Noktaları yerleştir", () -> {
            if (running) {
                toast("Önce makroyu durdur.");
                return;
            }
            showMarkers();
            hideMenu();
            toast(getPointCount() + " nokta açıldı. Sırayla yerleştir, M üzerinden kaydet.");
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
        menu.addView(text("Saldırı ve mola sürelerini uygulamanın ayar ekranından belirleyebilirsin.", 11, false));
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

    private void showMenu() {
        if (menu == null || menuShowing) return;
        if (pointCountLabel != null) pointCountLabel.setText(String.valueOf(getPointCount()));
        int x = bubblePosition.x + dp(54);
        if (x + dp(298) > width()) x = bubblePosition.x - dp(298);
        menuPosition.x = bound(x, 0, Math.max(0, width() - dp(298)));
        menuPosition.y = bound(bubblePosition.y, 0, Math.max(0, height() - dp(425)));
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
        int count = getPointCount();
        int w = width(), h = height(), size = dp(48);
        int columns = Math.max(1, Math.min(5, w / dp(56)));
        // Existing points keep their positions when point count changes.
        for (int i = 0; i < count; i++) {
            final int index = i;
            TextView circle = text(String.valueOf(i + 1), i < 9 ? 21 : 16, true);
            circle.setGravity(Gravity.CENTER);
            circle.setBackground(bg(COLORS[i % COLORS.length], 30));
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
        bubbleText.setBackground(bg(Color.rgb(23, 126, 82), 40));
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
            bubbleText.setText("M");
            bubbleText.setBackground(bg(Color.rgb(20, 54, 91), 40));
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
        long wait = randomSeconds(min, max);
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
