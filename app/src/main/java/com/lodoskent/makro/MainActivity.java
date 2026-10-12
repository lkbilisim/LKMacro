package com.lodoskent.makro;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.provider.Settings;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
    private EditText minimum, maximum, gap;
    private TextView state;
    private SharedPreferences prefs;

    @Override public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        prefs = getSharedPreferences(MacroAccessibilityService.PREFS, MODE_PRIVATE);
        ScrollView scroller = new ScrollView(this);
        scroller.setFillViewport(true);
        scroller.setBackgroundColor(Color.rgb(243, 247, 252));
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(28), dp(20), dp(24));
        scroller.addView(root);
        root.addView(text("LDK MAKRO", 26, true));
        root.addView(text("Yüzen üç noktalı otomasyon", 15, false));
        root.addView(text(" ", 16, false));
        root.addView(text("Konuşlandır sonrası rastgele süre", 17, true));
        root.addView(text("Her tur 45 ile 90 saniye arasında yeniden seçilir.", 14, false));
        minimum = number(prefs.getInt("min_seconds", 45));
        maximum = number(prefs.getInt("max_seconds", 90));
        root.addView(text("En az (saniye)", 14, false));
        root.addView(minimum);
        root.addView(text("En çok (saniye)", 14, false));
        root.addView(maximum);
        root.addView(text("Üç dokunuş arasındaki gecikme", 17, true));
        root.addView(text("Oyun menülerinin açılması için varsayılan 1100 ms.", 14, false));
        gap = number(prefs.getInt("step_ms", 1100));
        root.addView(text("Gecikme (milisaniye)", 14, false));
        root.addView(gap);
        root.addView(text("Ek mola: 4–8 dakika aralıkla 1–5 dakika (rastgele)", 14, true));
        Button save = new Button(this);
        save.setText("AYARLARI KAYDET");
        root.addView(save);
        save.setOnClickListener(v -> save());
        state = text("", 15, true);
        root.addView(state);
        Button settings = new Button(this);
        settings.setText("ERİŞİLEBİLİRLİK HİZMETİNİ AÇ");
        root.addView(settings);
        settings.setOnClickListener(v -> startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
        root.addView(text("KULLANIM", 18, true));
        root.addView(text(
            "1) Ayarları kaydet, erişilebilirlik hizmetini aç.\n" +
            "2) Oyuna dön ve yüzen M simgesine bas.\n" +
            "3) Noktaları yerleştir seç; 1 Hedef, 2 Saldır, 3 Konuşlandır üzerine sürükle.\n" +
            "4) M üzerinden konumları kaydet.\n" +
            "5) Başlat; durdurmak için M tuşunu kullan.\n\n" +
            "Uygulama OCR veya hedef tanıma yapmaz. Ekranın düzeni değişirse yanlış yere dokunabilir. " +
            "Rastgele aralıklar oyun kurallarına uygunluk veya yaptırım koruması sağlamaz.",
            15, false));
        setContentView(scroller);
    }

    @Override protected void onResume() {
        super.onResume();
        if (state != null) {
            state.setText(MacroAccessibilityService.isConnected()
                ? "Hizmet açık ✓ — Oyuna dön, M düğmesini kullan."
                : "Hizmet kapalı — yukarıdaki düğmeden etkinleştir.");
        }
    }

    private void save() {
        try {
            int min = Integer.parseInt(minimum.getText().toString().trim());
            int max = Integer.parseInt(maximum.getText().toString().trim());
            int ms = Integer.parseInt(gap.getText().toString().trim());
            if (min < 45 || max > 90 || min > max) {
                Toast.makeText(this, "Aralık 45–90 sn içinde olmalı.", Toast.LENGTH_LONG).show();
                return;
            }
            if (ms < 300 || ms > 5000) {
                Toast.makeText(this, "Dokunuş gecikmesi 300–5000 ms olmalı.", Toast.LENGTH_LONG).show();
                return;
            }
            prefs.edit().putInt("min_seconds", min).putInt("max_seconds", max).putInt("step_ms", ms).apply();
            Toast.makeText(this, "Ayarlar kaydedildi.", Toast.LENGTH_SHORT).show();
        } catch (NumberFormatException ex) {
            Toast.makeText(this, "Geçerli bir sayı gir.", Toast.LENGTH_LONG).show();
        }
    }

    private EditText number(int n) {
        EditText value = new EditText(this);
        value.setSingleLine(true);
        value.setInputType(InputType.TYPE_CLASS_NUMBER);
        value.setText(Integer.toString(n));
        value.setSelectAllOnFocus(true);
        return value;
    }

    private TextView text(String message, int sp, boolean strong) {
        TextView t = new TextView(this);
        t.setText(message);
        t.setTextSize(sp);
        t.setTextColor(Color.rgb(23, 43, 61));
        t.setPadding(0, dp(6), 0, dp(7));
        if (strong) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return t;
    }

    private int dp(float amount) {
        return (int) (getResources().getDisplayMetrics().density * amount + 0.5f);
    }
}
