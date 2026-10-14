package com.lodoskent.makro;

import android.content.Context;
import android.content.SharedPreferences;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/**
 * Persistent, conservative scheduler. No game interactions are issued here.
 * Each job can be ready, waiting until a wall-clock deadline, or manually paused.
 * Randomization ONLY selects among currently ready jobs. A blocked job is not revisited
 * until its known deadline, and completing the selected job is up to a future action engine.
 */
public final class SmartPlanner {
    public static final String[] IDS = {
        "radar", "ittifak", "ganimet", "seferberlik", "silahli_taarruz",
        "arastirma", "asker", "ziyaretci", "etkinlik", "subay", "yaver"
    };
    public static final String[] LABELS = {
        "Radar", "İttifak yardımı", "Ganimet", "Seferberlik", "Silahlı Taarruz",
        "Teknoloji", "Asker yetiştirme", "Üs ziyaretçileri", "Etkinlikler",
        "Subay alımı", "Yaver alımı"
    };
    public static final class Entry {
        public final String id, name;
        public final long readyAt;
        public final boolean disabled;
        Entry(String i, String n, long r, boolean d) {
            id=i; name=n; readyAt=r; disabled=d;
        }
        public boolean ready(long now) { return !disabled && readyAt <= now; }
    }

    private final SharedPreferences prefs;
    private final Random random = new Random();
    public SmartPlanner(Context context) {
        prefs=context.getSharedPreferences(MacroAccessibilityService.PREFS, Context.MODE_PRIVATE);
    }
    private static int indexOf(String id) {
        for (int i=0; i<IDS.length; i++) if (IDS[i].equals(id)) return i;
        return -1;
    }
    public List<Entry> all() {
        List<Entry> data=new ArrayList<>();
        for (int i=0;i<IDS.length;i++)
            data.add(new Entry(IDS[i],LABELS[i],
                prefs.getLong("smart_wait_"+IDS[i],0),
                prefs.getBoolean("smart_disabled_"+IDS[i],false)));
        return data;
    }
    public void waitFor(String id,long durationMs) {
        if(indexOf(id)<0) return;
        long safe=Math.min(30L*24*60*60*1000,Math.max(1000L,durationMs));
        prefs.edit().putLong("smart_wait_"+id,System.currentTimeMillis()+safe).apply();
    }
    public void ready(String id) {
        if(indexOf(id)<0)return;
        prefs.edit().remove("smart_wait_"+id).apply();
    }
    public void disable(String id,boolean value) {
        if(indexOf(id)<0)return;
        prefs.edit().putBoolean("smart_disabled_"+id,value).apply();
    }
    public int readyCount(long now) {
        int count=0;
        for(Entry e:all())if(e.ready(now))count++;
        return count;
    }
    public Entry nextRandom(long now) {
        List<Entry> ready=new ArrayList<>();
        for(Entry e:all())if(e.ready(now))ready.add(e);
        if(ready.isEmpty())return null;
        String previous=prefs.getString("smart_last_selected","");
        if(ready.size()>1)ready.removeIf(e->e.id.equals(previous));
        Entry picked=ready.get(random.nextInt(ready.size()));
        prefs.edit().putString("smart_last_selected",picked.id).apply();
        return picked;
    }
    public long nextWake(long now) {
        long earliest=Long.MAX_VALUE;
        for(Entry e:all())if(!e.disabled&&e.readyAt>now)earliest=Math.min(earliest,e.readyAt);
        return earliest==Long.MAX_VALUE?0:earliest;
    }
    public static String remaining(long readyAt,long now) {
        if(readyAt<=now)return "Hazır";
        long sec=(readyAt-now+999)/1000;
        long hr=sec/3600, min=(sec%3600)/60, s=sec%60;
        return hr>0?hr+" sa "+min+" dk":min>0?min+" dk "+s+" sn":s+" sn";
    }
    public static String clock(long when) {
        if(when<=0)return "—";
        return new SimpleDateFormat("dd.MM HH:mm",new Locale("tr","TR")).format(new Date(when));
    }

    /**
     * Read-only OCR classification; deliberately does not trigger touches or
     * infer a job's completion from untrusted partial text.
     */
    public static String detectScreen(String raw) {
        String t=raw.toLowerCase(new Locale("tr","TR"));
        if(t.contains("silahlı taarruz")||t.contains("silahli taarruz"))return "Silahlı Taarruz";
        if(t.contains("seferberlik savaşları")||t.contains("seferberlik savaşlari"))return "Seferberlik";
        if(t.contains("kurtarılan sinyaller")||t.contains("sinyal kulesi"))return "Radar";
        if(t.contains("teknoloji hızlandırması")||t.contains("subay sp artışı"))return "Teknoloji";
        if(t.contains("araştırma")||t.contains("araştirma"))return "Teknoloji / araştırma";
        if(t.contains("askeralma dairesi")||t.contains("askeralma hızlandırması"))return "Asker yetiştirme";
        if(t.contains("ganimet")&&t.contains("tümünü al"))return "Ganimet";
        if(t.contains("müttefiklere yardım et"))return "Radar / müttefik yardımı";
        if(t.contains("yaver alımı")||t.contains("yaver alimi"))return "Yaver alımı";
        if(t.contains("subay alımı")||t.contains("subay alimi"))return "Subay alımı";
        if(t.contains("ittifak yardımı")||t.contains("ittifak yardimi"))return "İttifak yardımı";
        if(t.contains("etkinlikler"))return "Etkinlik menüsü";
        return "Bilinmeyen ekran — işlem yapılmaz";
    }
    public static boolean hasGoldRisk(String raw) {
        String t=raw.toLowerCase(new Locale("tr","TR"));
        return t.contains("altın harca")||t.contains("hemen tamamla")
          ||t.contains("hızlı hızlandırma")||t.contains("altin harca");
    }
}
