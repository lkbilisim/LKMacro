package com.lodoskent.makrosafe;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/** Offline scheduler. Remembers cooldowns, excludes waiting and disabled tasks. */
public final class SafePlanner {
    public static final String[] IDS={"radar","ittifak","ganimet","seferberlik",
        "silahli","arastirma","asker","ziyaretci","etkinlik","subay","yaver"};
    public static final String[] NAMES={"Radar","İttifak yardımı","Ganimet",
        "Seferberlik","Silahlı Taarruz","Teknoloji","Asker yetiştirme",
        "Üs ziyaretçileri","Etkinlikler","Subay alımı","Yaver alımı"};
    private final SharedPreferences store;
    private final Random random=new Random();
    public SafePlanner(Context c){store=c.getSharedPreferences("lk_safe_planner",Context.MODE_PRIVATE);}
    public static final class Job{
        public final String id,name;
        public final long deadline;
        public final boolean disabled;
        Job(String i,String n,long time,boolean off){id=i;name=n;deadline=time;disabled=off;}
        public boolean ready(long now){return !disabled&&deadline<=now;}
    }
    public List<Job> all(){
        List<Job> jobs=new ArrayList<>();
        for(int i=0;i<IDS.length;i++)
            jobs.add(new Job(IDS[i],NAMES[i],store.getLong("due_"+IDS[i],0),
                store.getBoolean("off_"+IDS[i],false)));
        return jobs;
    }
    public void waitFor(String id,long millis){
        long safe=Math.max(1000L,Math.min(30L*86400000,millis));
        store.edit().putLong("due_"+id,System.currentTimeMillis()+safe).apply();
    }
    public void clear(String id){store.edit().remove("due_"+id).apply();}
    public void toggle(String id,boolean disabled){store.edit().putBoolean("off_"+id,disabled).apply();}
    public Job next(){
        long now=System.currentTimeMillis();
        List<Job> options=new ArrayList<>();
        for(Job j:all())if(j.ready(now))options.add(j);
        if(options.isEmpty())return null;
        String last=store.getString("last","");
        if(options.size()>1)options.removeIf(j->j.id.equals(last));
        Job item=options.get(random.nextInt(options.size()));
        store.edit().putString("last",item.id).apply();
        return item;
    }
    public int readyCount(){
        int n=0;
        long now=System.currentTimeMillis();
        for(Job job:all())if(job.ready(now))n++;
        return n;
    }
    public static String remaining(long deadline){
        long seconds=Math.max(0,(deadline-System.currentTimeMillis()+999)/1000);
        if(seconds==0)return "Hazır";
        long h=seconds/3600,m=seconds%3600/60;
        return h>0?h+" sa "+m+" dk":m>0?m+" dk":"<1 dk";
    }
    public static String classify(String input){
        String t=input.toLowerCase(new Locale("tr","TR"));
        if(t.contains("silahlı taarruz")||t.contains("silahli taarruz"))return "Silahlı Taarruz";
        if(t.contains("seferberlik savaşları"))return "Seferberlik";
        if(t.contains("kurtarılan sinyaller")||t.contains("sinyal kulesi"))return "Radar";
        if(t.contains("ganimet")&&t.contains("tümünü al"))return "Ganimet";
        if(t.contains("subay alımı"))return "Subay alımı";
        if(t.contains("yaver alımı"))return "Yaver alımı";
        if(t.contains("teknoloji")||t.contains("araştırma"))return "Teknoloji";
        if(t.contains("askeralma"))return "Asker yetiştirme";
        if(t.contains("ittifak yardımı"))return "İttifak yardımı";
        if(t.contains("etkinlikler"))return "Etkinlikler";
        return "Tanımlanamayan ekran";
    }
    public static boolean risky(String input){
        String t=input.toLowerCase(new Locale("tr","TR"));
        return t.contains("altın harca")||t.contains("hemen tamamla")||
            t.contains("elmas")||t.contains("hızlı hızlandırma")||
            t.contains("altin harca");
    }
}
