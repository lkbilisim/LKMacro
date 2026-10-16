package com.lodoskent.makro;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.content.SharedPreferences;
import android.graphics.Path;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import android.view.accessibility.AccessibilityNodeInfo;

import com.google.mlkit.vision.text.Text;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Conservative on-device Warline automation pilot.
 *
 * This is an ACTION ENGINE, not a proof that an on-screen item is safe:
 * only explicit, pre-approved, text-verified, on-screen actions are used.
 * No coordinates of purchase buttons are hardcoded. Unrecognized screens
 * never trigger gestures. The caller is responsible for user consent.
 */
public final class WarlineAutoBot {
    private static final String PACKAGE = "com.farlightgames.warline.gp.global";
    private static final Pattern CLOCK = Pattern.compile("(?<!\\d)(\\d{1,2}):(\\d{2}):(\\d{2})(?!\\d)");
    private static final String[] RED_FLAGS = {
        "altın harca", "altin harca", "hemen tamamla", "hızlı hızlandırma",
        "hizli hizlandirma", "altınla tamamla", "altinla tamamla", "satın al",
        "satin al", "gerçek para", "premium", "şimdi satın al"
    };
    private final MacroAccessibilityService service;
    private final SharedPreferences prefs;
    private final SmartPlanner planner;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Random random = new Random();

    private boolean active, busy, actionPending;
    private int generation;
    private long nextActionAllowedAt;
    private long lastRestAt;
    private long restUntil;
    private int successfulActions;

    private final Runnable tick = new Runnable() {
        @Override public void run() {
            if(!active || busy)return;
            long now=SystemClock.uptimeMillis();
            if(now < restUntil) {
                schedule(Math.min(15000L,restUntil-now));
                return;
            }
            if(!inWarline()) {
                write("Warline ön planda değil. Oyunu açınca devam edecek.");
                schedule(9000);
                return;
            }
            busy=true;
            SmartScreenReader.readStructured(service,(vision,error)->{
                busy=false;
                if(!active)return;
                if(!error.isEmpty()||vision==null){
                    write("Ekran okunamadı: "+error);
                    schedule(12000);
                    return;
                }
                evaluate(vision);
            });
        }
    };

    public WarlineAutoBot(MacroAccessibilityService service,SharedPreferences prefs){
        this.service=service;
        this.prefs=prefs;
        this.planner=new SmartPlanner(service);
    }

    public boolean isActive(){return active;}
    public void start(){
        if(active)return;
        active=true;
        generation++;
        nextActionAllowedAt=SystemClock.uptimeMillis()+1300;
        lastRestAt=SystemClock.uptimeMillis();
        restUntil=0L;
        successfulActions=0;
        write("Akıllı bot başladı. Yalnız izinli, ücretsiz eylemler etkin.");
        handler.removeCallbacks(tick);
        handler.postDelayed(tick,1300);
    }
    public void stop(String reason){
        active=false;
        generation++;
        busy=false;
        actionPending=false;
        handler.removeCallbacks(tick);
        write(reason);
    }
    private void schedule(long millis){
        if(!active)return;
        handler.removeCallbacks(tick);
        handler.postDelayed(tick,Math.max(2000,millis));
    }
    private void write(String message){
        prefs.edit().putString("smart_auto_status",message)
            .putLong("smart_auto_updated",System.currentTimeMillis()).apply();
        Log.i("LKAuto",message);
    }

    private boolean inWarline(){
        try{
            AccessibilityNodeInfo root=service.getRootInActiveWindow();
            if(root==null)return false;
            try{
                CharSequence name=root.getPackageName();
                return name!=null&&PACKAGE.contentEquals(name);
            }finally{root.recycle();}
        }catch(RuntimeException e){return false;}
    }
    private static String normalize(String txt){
        return txt==null?"":txt.toLowerCase(new Locale("tr","TR")).trim();
    }
    private boolean goldRisk(String text){
        String lowered=normalize(text);
        for(String risk:RED_FLAGS)if(lowered.contains(risk))return true;
        return false;
    }
    private long timerMs(String text){
        Matcher m=CLOCK.matcher(text);
        long min=Long.MAX_VALUE;
        while(m.find()){
            try{
                long h=Long.parseLong(m.group(1));
                long mm=Long.parseLong(m.group(2));
                long s=Long.parseLong(m.group(3));
                if(mm<60&&s<60){
                    long ms=(h*3600+mm*60+s)*1000L;
                    if(ms>0)min=Math.min(min,ms);
                }
            }catch(NumberFormatException ignored){}
        }
        return min==Long.MAX_VALUE?0:min;
    }

    private static final class Target {
        final String task, word;
        final Rect rect;
        Target(String t,String w,Rect r){task=t;word=w;rect=r;}
    }
    private Target target(String task,String trigger,Text text){
        for(Text.TextBlock block:text.getTextBlocks()){
            for(Text.Line line:block.getLines()){
                String contents=normalize(line.getText());
                if(contents.equals(trigger) || contents.contains(trigger)){
                    Rect r=line.getBoundingBox();
                    if(r!=null && r.width()>=14 && r.height()>=10)
                        return new Target(task,trigger,r);
                }
            }
        }
        return null;
    }
    private void offer(List<Target> list,String id,String key,Text document){
        SmartPlanner.Entry e=null;
        long now=System.currentTimeMillis();
        for(SmartPlanner.Entry x:planner.all())
            if(x.id.equals(id)){e=x;break;}
        if(e==null||!e.ready(now))return;
        Target t=target(id,key,document);
        if(t!=null)list.add(t);
    }

    private void evaluate(Text document){
        final String raw=document.getText();
        final String text=normalize(raw);
        final String screen=SmartPlanner.detectScreen(raw);
        prefs.edit().putString("smart_ocr_text",
            raw.length()>4000?raw.substring(0,4000):raw)
            .putString("smart_ocr_screen",screen)
            .putLong("smart_ocr_when",System.currentTimeMillis()).apply();

        if(goldRisk(raw)) {
            write("ALTIN/ÜCRETLİ EKRAN: Dokunma engellendi; güvenli ekran bekleniyor.");
            schedule(18000);
            return;
        }
        if(text.contains("nişan al")||text.contains("nisan al")
            ||text.contains("dürbün")||text.contains("sniper")) {
            write("Şahsi nişan görevi algılandı, atlanıyor.");
            schedule(24000);
            return;
        }

        long now=SystemClock.uptimeMillis();
        if(now<nextActionAllowedAt) {
            schedule(Math.max(2500,nextActionAllowedAt-now));
            return;
        }

        List<Target> candidates=new ArrayList<>();
        // Only known screens; never tap a generic 'Al', 'Git' or 'Saldır'.
        if(text.contains("ganimet")&&text.contains("tümünü al"))
            offer(candidates,"ganimet","tümünü al",document);
        if(text.contains("ittifak yardımı") &&
            (text.contains("tümüne yardım")||text.contains("tümüne yardım et")))
            offer(candidates,"ittifak","tümüne yardım",document);
        if(text.contains("araştırma tamamlandı")&&text.contains("tamamla"))
            offer(candidates,"arastirma","tamamla",document);
        // Radar, rallies, troop training and purchases require more
        // complete confirmation screens before automatic taps are enabled.

        if(candidates.isEmpty()){
            write("OCR: "+screen+" • Güvenli işlem bulunamadı, kontrol bekleniyor.");
            schedule(12000);
            return;
        }
        // Randomize ready/visible tasks; avoid selecting last completed task.
        String last=prefs.getString("smart_auto_last","");
        if(candidates.size()>1)candidates.removeIf(x->x.task.equals(last));
        Target chosen=candidates.get(random.nextInt(candidates.size()));
        if(!inWarline()){
            write("Warline odağı kayboldu; işlem yapılmadı.");
            schedule(7000);
            return;
        }
        final int x=chosen.rect.centerX(), y=chosen.rect.centerY();
        if(x<5||y<5||x>=service.screenWidth()||y>=service.screenHeight()){
            write("OCR hedefi ekran dışında; dokunma iptal edildi.");
            schedule(12000);
            return;
        }
        int token=generation;
        Path tap=new Path();tap.moveTo(x,y);
        GestureDescription gesture=new GestureDescription.Builder()
            .addStroke(new GestureDescription.StrokeDescription(tap,0,95)).build();
        actionPending=true;
        boolean accepted=service.dispatchGesture(gesture,
            new AccessibilityService.GestureResultCallback(){
                @Override public void onCompleted(GestureDescription g){
                    actionPending=false;
                    if(!active||token!=generation)return;
                    successfulActions++;
                    prefs.edit().putString("smart_auto_last",chosen.task).apply();
                    long delay=rangeSeconds("min_seconds","max_seconds",45,90)*1000L;
                    nextActionAllowedAt=SystemClock.uptimeMillis()+delay;
                    // Preserve task state, and do not re-enter same task until timeout.
                    planner.waitFor(chosen.task,Math.max(delay,90000L));
                    long overtime=SystemClock.uptimeMillis()-lastRestAt;
                    long interval=rangeSeconds("pause_interval_min_minutes",
                        "pause_interval_max_minutes",4,8)*60000L;
                    if(overtime>=interval){
                        restUntil=SystemClock.uptimeMillis()+rangeSeconds("rest_min_minutes",
                            "rest_max_minutes",1,5)*60000L;
                        lastRestAt=SystemClock.uptimeMillis();
                    }
                    write("İşlem: "+chosen.task+" • Tamamlandı; "+(delay/1000)
                        +" sn bekleme. Toplam: "+successfulActions);
                    schedule(5500);
                }
                @Override public void onCancelled(GestureDescription g){
                    actionPending=false;
                    if(!active||token!=generation)return;
                    write("Dokunma Android tarafından iptal edildi.");
                    schedule(13000);
                }
            },handler);
        if(!accepted){
            actionPending=false;
            write("Android dokunmayı reddetti.");
            schedule(15000);
        }else{
            write("Doğrulanan eylem gönderildi: "+chosen.task);
        }
    }
    private long rangeSeconds(String minKey,String maxKey,int minDefault,int maxDefault){
        long a=Math.max(1,prefs.getInt(minKey,minDefault));
        long b=Math.max(a,prefs.getInt(maxKey,maxDefault));
        return a+(long)(random.nextDouble()*(b-a+1));
    }
}
