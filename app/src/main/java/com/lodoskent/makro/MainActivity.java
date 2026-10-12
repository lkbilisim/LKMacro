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
    private EditText minimum, maximum, gap, countInput;
    private EditText pauseMin, pauseMax, restMin, restMax;
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
        root.addView(text("LK MAKRO", 26, true));
        root.addView(text("İstediğin sayıda sıralı dokunma noktası", 15, false));
        root.addView(text(" ", 16, false));
        root.addView(text("Kaç tane tuş açılsın?", 17, true));
        root.addView(text("1 ile 100 arasında tuş sayısı yaz. M panelinden de + / − ile değiştirebilirsin. Noktaları yeniden yerleştirip kaydetmen gerekir.", 14, false));
        countInput = number(prefs.getInt("point_count", 3));
        root.addView(countInput);
        root.addView(text("Son dokunuştan sonraki rastgele bekleme", 17, true));
        root.addView(text("Buraya yazdığın iki sayı arasında her tur yeniden rastgele süre seçilir. Sabit 45–90 saniye sınırı yok.", 14, false));
        minimum = number(prefs.getInt("min_seconds", 45));
        maximum = number(prefs.getInt("max_seconds", 90));
        root.addView(text("En az (saniye)", 14, false));
        root.addView(minimum);
        root.addView(text("En çok (saniye)", 14, false));
        root.addView(maximum);
        root.addView(text("Ardışık dokunuşlar arasındaki gecikme", 17, true));
        root.addView(text("Oyun menülerinin açılması için varsayılan 1100 ms.", 14, false));
        gap = number(prefs.getInt("step_ms", 1100));
        root.addView(text("Gecikme (milisaniye)", 14, false));
        root.addView(gap);
        root.addView(text("Zaman bazlı ek mola", 17, true));
        root.addView(text("Basış sayısına göre değil, geçen süreye göre. Belirlenen zaman geldiğinde tüm seçili tuşlara sırayla dokunulur; ardından ek mola uygulanır.", 14, false));
        root.addView(text("Mola aralığı en az (dakika)", 14, false));
        pauseMin = number(prefs.getInt("pause_interval_min_minutes", 4));
        root.addView(pauseMin);
        root.addView(text("Mola aralığı en çok (dakika)", 14, false));
        pauseMax = number(prefs.getInt("pause_interval_max_minutes", 8));
        root.addView(pauseMax);
        root.addView(text("Mola süresi en az (dakika)", 14, false));
        restMin = number(prefs.getInt("rest_min_minutes", 1));
        root.addView(restMin);
        root.addView(text("Mola süresi en çok (dakika)", 14, false));
        restMax = number(prefs.getInt("rest_max_minutes", 5));
        root.addView(restMax);
        root.addView(text("Mola, her turun normal beklemesine eklenir. Ayarları çalışırken değiştirirsen makroyu durdurup yeniden başlat.", 13, false));
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
            "3) M panelinde tuş sayısını belirle; Noktaları yerleştir ile numaralı işaretleri sürükle.\n" +
            "4) M üzerinden konumları kaydet. Her tur 1, 2, 3 ... sırasıyla dokunulur.\n" +
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
            int count = Integer.parseInt(countInput.getText().toString().trim());
            int min = Integer.parseInt(minimum.getText().toString().trim());
            int max = Integer.parseInt(maximum.getText().toString().trim());
            int ms = Integer.parseInt(gap.getText().toString().trim());
            int pauseFrom = Integer.parseInt(pauseMin.getText().toString().trim());
            int pauseTo = Integer.parseInt(pauseMax.getText().toString().trim());
            int restFrom = Integer.parseInt(restMin.getText().toString().trim());
            int restTo = Integer.parseInt(restMax.getText().toString().trim());
            if (count < 1 || count > 100) {
                Toast.makeText(this, "Tuş sayısı 1–100 arasında olmalı.", Toast.LENGTH_LONG).show();
                return;
            }
            if (min <= 0 || max < min) {
                Toast.makeText(this, "Saldırı aralığında pozitif sayılar gir. En çok, en azdan küçük olamaz.", Toast.LENGTH_LONG).show();
                return;
            }
            if (ms <= 0) {
                Toast.makeText(this, "Dokunuş gecikmesi pozitif olmalı.", Toast.LENGTH_LONG).show();
                return;
            }
            if (pauseFrom <= 0 || pauseTo < pauseFrom || restFrom <= 0 || restTo < restFrom) {
                Toast.makeText(this, "Mola dakika aralıklarında pozitif sayılar gir. En çok, en azdan küçük olamaz.", Toast.LENGTH_LONG).show();
                return;
            }
            boolean countChanged = count != prefs.getInt("point_count", 3);
            SharedPreferences.Editor edit = prefs.edit()
                .putInt("point_count", count)
                .putInt("min_seconds", min)
                .putInt("max_seconds", max)
                .putInt("step_ms", ms)
                .putInt("pause_interval_min_minutes", pauseFrom)
                .putInt("pause_interval_max_minutes", pauseTo)
                .putInt("rest_min_minutes", restFrom)
                .putInt("rest_max_minutes", restTo);
            if (countChanged) edit.putBoolean("points_set", false);
            edit.apply();
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
