package com.lodoskent.makro;

import android.app.Activity;
import android.content.Intent;
import android.content.ClipboardManager;
import android.content.ClipData;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.provider.Settings;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
    private SharedPreferences prefs;
    private EditText minSeconds, maxSeconds, stepMillis, pauseFrom, pauseTo, restFrom, restTo;
    private Switch randomToggle;
    private TextView status;
    private LinearLayout contentArea;
    private int currentPage=0;

    private int d(float px) { return UiKit.dp(this,px); }
    @Override public void onCreate(Bundle saved) {
        super.onCreate(saved);
        prefs = getSharedPreferences(MacroAccessibilityService.PREFS, MODE_PRIVATE);
        Window w=getWindow();
        w.setStatusBarColor(0xff050d1e);
        w.setNavigationBarColor(0xff061123);
        w.getDecorView().setSystemUiVisibility(0);
        build(0);
    }

    private LinearLayout vertical() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        return l;
    }

    private LinearLayout horizontal() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.HORIZONTAL);
        l.setGravity(Gravity.CENTER_VERTICAL);
        return l;
    }

    private LinearLayout.LayoutParams columnParams(int width, int height, int bottomDp) {
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(width,height);
        p.bottomMargin=d(bottomDp);
        return p;
    }

    private void addSpace(LinearLayout c,int size) {
        View v=new View(this);
        c.addView(v,new LinearLayout.LayoutParams(1,d(size)));
    }

    private TextView subtitle(String value) {
        TextView t=UiKit.label(this,value,13,UiKit.SECONDARY,false);
        t.setLineSpacing(d(2),1f);
        return t;
    }

    private void build(int page) {
        currentPage=page;
        minSeconds=maxSeconds=stepMillis=pauseFrom=pauseTo=restFrom=restTo=null;
        randomToggle=null;
        FrameLayout root=new FrameLayout(this);
        root.addView(new UiKit.TechBackground(this),
            new FrameLayout.LayoutParams(-1,-1));
        LinearLayout shell=vertical();
        shell.setPadding(d(17),d(8),d(17),d(3));
        FrameLayout.LayoutParams full=new FrameLayout.LayoutParams(-1,-1);
        root.addView(shell,full);
        shell.addView(header(),columnParams(-1,d(58),7));
        ScrollView scroller=new ScrollView(this);
        scroller.setFillViewport(true);
        scroller.setVerticalScrollBarEnabled(false);
        contentArea=vertical();
        contentArea.setPadding(d(1),d(4),d(1),d(18));
        scroller.addView(contentArea);
        shell.addView(scroller,new LinearLayout.LayoutParams(-1,0,1f));
        shell.addView(navbar(),columnParams(-1,d(70),2));
        setContentView(root);
        if(page==0)home();
        else if(page==1)settingsPage();
        else if(page==3)smartPage();
        else about();
    }

    private View header() {
        LinearLayout l=horizontal();
        ImageView logo=UiKit.logo(this,43);
        l.addView(logo,new LinearLayout.LayoutParams(d(43),d(43)));
        LinearLayout texts=vertical();
        texts.setPadding(d(11),0,0,0);
        texts.addView(UiKit.label(this,"LK Makro",22,Color.WHITE,true));
        texts.addView(UiKit.label(this,"PREMIUM CONTROL",9,0xff67cee9,true));
        l.addView(texts,new LinearLayout.LayoutParams(0,-2,1f));
        TextView gear=UiKit.center(this,"⚙",24,0xffe0edff,true);
        gear.setBackground(UiKit.surface(this,13));
        l.addView(gear,new LinearLayout.LayoutParams(d(46),d(43)));
        gear.setOnClickListener(v->build(1));
        logo.setOnClickListener(v->build(0));
        return l;
    }

    private View navbar() {
        LinearLayout bar=horizontal();
        bar.setPadding(d(7),d(4),d(7),d(4));
        bar.setBackground(UiKit.gradient(this,new int[]{0xee0c1a34,0xf1081327},18,0xff314a73,1));
        navTab(bar,"⌂","Ana Sayfa",0);
        navTab(bar,"⚙","Ayarlar",1);
        navTab(bar,"◎","Akıllı",3);
        navTab(bar,"ⓘ","Hakkında",2);
        return bar;
    }

    private void navTab(LinearLayout bar,String icon,String name,int page) {
        LinearLayout tab=vertical();
        tab.setGravity(Gravity.CENTER);
        boolean selected=currentPage==page;
        if(selected)tab.setBackground(UiKit.gradient(this,new int[]{0xff122f62,0xff133358},13,0xff4776e9,1));
        TextView ico=UiKit.center(this,icon,23,selected?UiKit.CYAN:0xffb4c6df,true);
        tab.addView(ico);
        TextView txt=UiKit.center(this,name,10,selected?Color.WHITE:0xff9db2d3,false);
        tab.addView(txt);
        LinearLayout.LayoutParams pp=new LinearLayout.LayoutParams(0,-1,1);
        pp.leftMargin=d(3);pp.rightMargin=d(3);
        bar.addView(tab,pp);
        tab.setOnClickListener(v->{if(currentPage==1 && page!=1)save(false);build(page);});
    }

    private LinearLayout card() {
        LinearLayout c=vertical();
        c.setPadding(d(16),d(15),d(16),d(15));
        c.setBackground(UiKit.surface(this,19));
        c.setElevation(d(5));
        return c;
    }

    private void addCard(LinearLayout parent,View card) {
        LinearLayout.LayoutParams cp=columnParams(-1,-2,12);
        cp.leftMargin=d(2);cp.rightMargin=d(2);
        parent.addView(card,cp);
    }

    private TextView button(String label,boolean primary) {
        TextView b=UiKit.center(this,label,16,Color.WHITE,true);
        b.setBackground(primary?UiKit.action(this):
            UiKit.gradient(this,new int[]{0xff182e52,0xff0c203a},15,0xff43628c,1));
        b.setElevation(d(primary?5:2));
        b.setPadding(d(9),d(10),d(9),d(10));
        return b;
    }

    private void home() {
        contentArea.setGravity(Gravity.CENTER_HORIZONTAL);
        addSpace(contentArea,29);
        ImageView hero=UiKit.logo(this,148);
        contentArea.addView(hero,new LinearLayout.LayoutParams(d(148),d(148)));
        addSpace(contentArea,23);
        TextView title=UiKit.center(this,"LK Makro",38,Color.WHITE,true);
        title.setTypeface(Typeface.create("sans-serif-black",Typeface.BOLD));
        contentArea.addView(title);
        addSpace(contentArea,8);
        TextView slogan=UiKit.center(this,"D A H A   A K I L L I   O Y N A\nZ A M A N D A N   K A Z A N",10,UiKit.CYAN,true);
        contentArea.addView(slogan);
        addSpace(contentArea,26);

        LinearLayout highlight=card();
        highlight.addView(UiKit.label(this,"◇  Oyunlarını daha akıllı yönet",17,Color.WHITE,true));
        addSpace(highlight,8);
        highlight.addView(subtitle("Tekrarlayan dokunuşlar, rastgele beklemeler ve sana özel mola zamanları — tek bir kontrol panelinde."));
        addCard(contentArea,highlight);

        LinearLayout featureRow=horizontal();
        LinearLayout f1=card();
        f1.addView(UiKit.label(this,"⚡  Esnek",15,UiKit.CYAN,true));
        f1.addView(subtitle("Sana özel süreler"));
        LinearLayout f2=card();
        f2.addView(UiKit.label(this,"◎  Yüzen",15,UiKit.CYAN,true));
        f2.addView(subtitle("Pratik kontrol"));
        LinearLayout.LayoutParams fp=new LinearLayout.LayoutParams(0,-2,1);
        fp.rightMargin=d(5);
        featureRow.addView(f1,fp);
        LinearLayout.LayoutParams gp=new LinearLayout.LayoutParams(0,-2,1);
        gp.leftMargin=d(5);
        featureRow.addView(f2,gp);
        contentArea.addView(featureRow,columnParams(-1,-2,16));

        LinearLayout info=card();
        info.addView(UiKit.label(this,"●  SİSTEM DURUMU",11,UiKit.CYAN,true));
        status=UiKit.label(this,"",13,Color.WHITE,false);
        status.setPadding(0,d(8),0,0);
        info.addView(status);
        refreshStatus();
        addCard(contentArea,info);
        String crash=prefs.getString("last_service_error","");
        if(!crash.isEmpty()) {
            TextView diag=button("⚠  Son Hata Kaydını Kopyala",false);
            contentArea.addView(diag,columnParams(-1,d(46),8));
            diag.setOnClickListener(v->copyDiagnostics());
        }
        addSpace(contentArea,6);
        TextView smart=button("🧠  Akıllı Görüş ve Görevler",true);
        contentArea.addView(smart,columnParams(-1,d(56),9));
        smart.setOnClickListener(v->build(3));
        TextView cta=button("Başlayalım   →",true);
        contentArea.addView(cta,columnParams(-1,d(57),13));
        cta.setOnClickListener(v->build(1));
        TextView restore=button("◎  Yüzen LK Düğmesini Geri Getir",true);
        contentArea.addView(restore,columnParams(-1,d(56),9));
        restore.setOnClickListener(v->restoreFloatingButton());
        TextView access=button("Erişilebilirliği Aç",false);
        contentArea.addView(access,columnParams(-1,d(50),6));
        access.setOnClickListener(v->startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
    }

    private void settingsPage() {
        TextView heading=UiKit.label(this,"Makro Ayarları",25,Color.WHITE,true);
        contentArea.addView(heading);
        addSpace(contentArea,4);
        contentArea.addView(subtitle("Dokunuşları, beklemeleri ve molaları dilediğin gibi özelleştir."));
        addSpace(contentArea,15);

        LinearLayout randCard=card();
        LinearLayout title=horizontal();
        LinearLayout titles=vertical();
        titles.addView(UiKit.label(this,"⌛  Rastgele Bekleme",17,Color.WHITE,true));
        titles.addView(subtitle("Her işlem sonunda yeni süre seçilir."));
        title.addView(titles,new LinearLayout.LayoutParams(0,-2,1));
        randomToggle=new Switch(this);
        randomToggle.setChecked(prefs.getBoolean("random_enabled",true));
        randomToggle.setThumbTintList(ColorStateList.valueOf(0xffc4f9ff));
        randomToggle.setTrackTintList(ColorStateList.valueOf(0xff2869dc));
        title.addView(randomToggle);
        randCard.addView(title);
        addSpace(randCard,16);
        LinearLayout pair=horizontal();
        minSeconds=numberField(pair,"En az (saniye)",prefs.getInt("min_seconds",45),1);
        maxSeconds=numberField(pair,"En çok (saniye)",prefs.getInt("max_seconds",90),1);
        randCard.addView(pair);
        addCard(contentArea,randCard);

        LinearLayout touchCard=card();
        touchCard.addView(UiKit.label(this,"☝  Dokunuş Gecikmesi",17,Color.WHITE,true));
        addSpace(touchCard,4);
        touchCard.addView(subtitle("Sıradaki tuşa geçmeden önce bekleme."));
        addSpace(touchCard,11);
        LinearLayout touchField=horizontal();
        stepMillis=numberField(touchField,"Milisaniye",prefs.getInt("step_ms",1100),100);
        touchCard.addView(touchField);
        addCard(contentArea,touchCard);

        LinearLayout pauseCard=card();
        pauseCard.addView(UiKit.label(this,"☕  Mola Aralığı",17,Color.WHITE,true));
        addSpace(pauseCard,4);
        pauseCard.addView(subtitle("Kaç dakikada bir ek mola verileceği (işlem sayısı değil)."));
        addSpace(pauseCard,11);
        LinearLayout pausePair=horizontal();
        pauseFrom=numberField(pausePair,"En az (dakika)",prefs.getInt("pause_interval_min_minutes",4),1);
        pauseTo=numberField(pausePair,"En çok (dakika)",prefs.getInt("pause_interval_max_minutes",8),1);
        pauseCard.addView(pausePair);
        addCard(contentArea,pauseCard);

        LinearLayout restCard=card();
        restCard.addView(UiKit.label(this,"◷  Mola Süresi",17,Color.WHITE,true));
        addSpace(restCard,4);
        restCard.addView(subtitle("Normal rastgele beklemeye eklenecek mola süresi."));
        addSpace(restCard,11);
        LinearLayout restPair=horizontal();
        restFrom=numberField(restPair,"En az (dakika)",prefs.getInt("rest_min_minutes",1),1);
        restTo=numberField(restPair,"En çok (dakika)",prefs.getInt("rest_max_minutes",5),1);
        restCard.addView(restPair);
        addCard(contentArea,restCard);

        TextView save=button("⚙   Ayarları Kaydet",true);
        contentArea.addView(save,columnParams(-1,d(57),12));
        save.setOnClickListener(v->save(true));
        LinearLayout help=card();
        help.addView(UiKit.label(this,"◎  Yüzen Kontrol",16,UiKit.CYAN,true));
        addSpace(help,5);
        help.addView(subtitle("Tuş sayısını oyun içindeki M panelinden belirle. Noktaları orada konumlandırıp kaydet, sonra Başlat'a dokun."));
        addSpace(help,12);
        TextView recover=button("◎  Yüzen LK Düğmesini Geri Getir",true);
        help.addView(recover,columnParams(-1,d(50),10));
        recover.setOnClickListener(v->restoreFloatingButton());
        addSpace(help,3);
        TextView enable=button("Erişilebilirlik Hizmetini Aç  ↗",false);
        help.addView(enable,columnParams(-1,d(46),0));
        enable.setOnClickListener(v->startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
        addCard(contentArea,help);
    }

    private EditText numberField(LinearLayout row, String name, int initial, int increment) {
        LinearLayout outer=vertical();
        outer.setPadding(d(8),d(9),d(8),d(9));
        outer.setBackground(UiKit.field(this,13));
        TextView label=UiKit.label(this,name,12,0xffd5e6ff,true);
        label.setGravity(Gravity.CENTER);
        outer.addView(label);
        addSpace(outer,5);
        LinearLayout inner=horizontal();
        TextView less=UiKit.center(this,"−",20,Color.WHITE,true);
        less.setBackground(UiKit.gradient(this,new int[]{0xff243b60,0xff162948},9,0xff3e5480,1));
        inner.addView(less,new LinearLayout.LayoutParams(d(30),d(39)));
        EditText field=new EditText(this);
        field.setSingleLine(true);
        field.setSelectAllOnFocus(true);
        field.setInputType(InputType.TYPE_CLASS_NUMBER);
        field.setText(String.valueOf(initial));
        field.setTextColor(Color.WHITE);
        field.setTextSize(15);
        field.setGravity(Gravity.CENTER);
        field.setBackgroundColor(Color.TRANSPARENT);
        field.setPadding(0,0,0,0);
        inner.addView(field,new LinearLayout.LayoutParams(0,d(43),1));
        TextView more=UiKit.center(this,"+",20,Color.WHITE,true);
        more.setBackground(UiKit.gradient(this,new int[]{0xff243b60,0xff162948},9,0xff3e5480,1));
        inner.addView(more,new LinearLayout.LayoutParams(d(30),d(39)));
        less.setOnClickListener(v->adjust(field,-increment));
        more.setOnClickListener(v->adjust(field,increment));
        outer.addView(inner);
        LinearLayout.LayoutParams weight=new LinearLayout.LayoutParams(0,-2,1);
        weight.leftMargin=d(3);weight.rightMargin=d(3);
        row.addView(outer,weight);
        return field;
    }

    private void adjust(EditText field,int delta) {
        try {
            long n=Long.parseLong(field.getText().toString());
            long val=Math.max(1,Math.min(2000000000L,n+delta));
            field.setText(String.valueOf(val));
            field.setSelection(field.length());
        } catch(Exception ex) {
            field.setText("1");
        }
    }

    private int parse(EditText e) {
        return Integer.parseInt(e.getText().toString().trim());
    }

    private boolean save(boolean feedback) {
        if(minSeconds==null) return true;
        try {
            int a=parse(minSeconds),b=parse(maxSeconds),delay=parse(stepMillis);
            int pa=parse(pauseFrom),pb=parse(pauseTo),ra=parse(restFrom),rb=parse(restTo);
            if(a<1||b<a||delay<1||pa<1||pb<pa||ra<1||rb<ra) {
                if(feedback)Toast.makeText(this,"Geçerli pozitif aralıklar gir; üst sınır alt sınırdan küçük olamaz.",Toast.LENGTH_LONG).show();
                return false;
            }
            prefs.edit().putInt("min_seconds",a).putInt("max_seconds",b)
                .putInt("step_ms",delay)
                .putInt("pause_interval_min_minutes",pa)
                .putInt("pause_interval_max_minutes",pb)
                .putInt("rest_min_minutes",ra)
                .putInt("rest_max_minutes",rb)
                .putBoolean("random_enabled",randomToggle.isChecked()).apply();
            if(feedback)Toast.makeText(this,"LK Makro ayarları kaydedildi ✓",Toast.LENGTH_SHORT).show();
            return true;
        } catch(NumberFormatException ex) {
            if(feedback)Toast.makeText(this,"Lütfen tüm alanlara geçerli tam sayılar gir.",Toast.LENGTH_LONG).show();
            return false;
        }
    }

    /** v2.3: read-only OCR and persistent, user-controllable task schedule. */
    private void smartPage() {
        contentArea.addView(UiKit.label(this,"◎  Akıllı Görüş",25,Color.WHITE,true));
        addSpace(contentArea,6);
        contentArea.addView(subtitle("Akıllı bot: Warline açıkken OCR ile onaylı ücretsiz işlemleri otomatik yapar. Saldırı, seferberlik ve asker komutları doğrulanana kadar kapalı."));
        addSpace(contentArea,12);

        LinearLayout protection=card();
        protection.addView(UiKit.label(this,"🔒  ALTIN HARCAMA: 0",17,0xff83f5b3,true));
        addSpace(protection,7);
        protection.addView(subtitle("Altın, elmas, ücretli paket ve onaysız hızlandırma işlemleri otomasyona kapalıdır. Şahsi nişan görevleri de kapsam dışıdır."));
        addCard(contentArea,protection);

        LinearLayout control=card();
        control.addView(UiKit.label(this,"🤖  Canlı Akıllı Bot",18,UiKit.CYAN,true));
        addSpace(control,9);
        boolean live=MacroAccessibilityService.isSmartRunning();
        control.addView(UiKit.label(this,live?"●  BOT ÇALIŞIYOR":"○  BOT DURUYOR",
            14,live?0xff7bf1bf:0xffe0b7a3,true));
        addSpace(control,7);
        String statusText=prefs.getString("smart_auto_status","Henüz başlatılmadı.");
        control.addView(subtitle("Son durum: "+statusText));
        addSpace(control,11);
        TextView start=button("▶  Akıllı Botu Başlat",true);
        control.addView(start,columnParams(-1,d(51),8));
        start.setOnClickListener(v->{
            if(!MacroAccessibilityService.startSmartFromActivity())
                Toast.makeText(this,"Önce erişilebilirlik hizmetini aç.",Toast.LENGTH_LONG).show();
            else {
                Toast.makeText(this,"Warline'a dön. Bot oyun ekranını tarayacak.",Toast.LENGTH_LONG).show();
                new android.os.Handler(getMainLooper()).postDelayed(()->build(3),900);
            }
        });
        TextView stop=button("■  Akıllı Botu Durdur",false);
        control.addView(stop,columnParams(-1,d(48),0));
        stop.setOnClickListener(v->{
            MacroAccessibilityService.stopSmartFromActivity();
            new android.os.Handler(getMainLooper()).postDelayed(()->build(3),400);
        });
        addCard(contentArea,control);

        LinearLayout scan=card();
        scan.addView(UiKit.label(this,"👁  Son OCR taraması",17,UiKit.CYAN,true));
        addSpace(scan,8);
        String extracted=prefs.getString("smart_ocr_text","");
        String error=prefs.getString("smart_ocr_error","");
        long stamped=prefs.getLong("smart_ocr_when",0);
        scan.addView(subtitle("Tarih: "+SmartPlanner.clock(stamped)));
        addSpace(scan,6);
        scan.addView(subtitle("Ekran: "+prefs.getString("smart_ocr_screen","Henüz taranmadı")));
        if(!error.isEmpty()){
            addSpace(scan,5);
            scan.addView(UiKit.label(this,"Tarama hatası: "+error,13,0xffffaa99,false));
        }
        if(!extracted.isEmpty()){
            addSpace(scan,8);
            TextView content=UiKit.label(this,extracted.length()>1200
                ? extracted.substring(0,1200)+" …":extracted,12,0xffd5e6ff,false);
            content.setTextIsSelectable(true);
            scan.addView(content);
        }
        addSpace(scan,10);
        TextView note=UiKit.label(this,"Warline'da LK menüsünden Akıllı Bot Başlat seçilebilir. Ekran otomatik okunur. Kaydedilen yalnızca tanınan metin ve işlem durumudur.",12,0xff91c9e4,false);
        scan.addView(note);
        addCard(contentArea,scan);

        SmartPlanner planner=new SmartPlanner(this);
        long now=System.currentTimeMillis();
        LinearLayout summary=card();
        summary.addView(UiKit.label(this,"🔀  Rastgele Görev Planlayıcısı",17,UiKit.CYAN,true));
        addSpace(summary,7);
        summary.addView(subtitle("Hazır: "+planner.readyCount(now)+" / "+SmartPlanner.IDS.length
            +"    •    Sonraki uyanma: "+SmartPlanner.clock(planner.nextWake(now))));
        addSpace(summary,7);
        summary.addView(subtitle("Yalnız hazır görevler seçilir. Bekleyen görevlere süre bitmeden dönülmez. Gerçek eylem, yalnızca ekranda doğrulanmış ücretsiz düğmede yapılır."));
        addSpace(summary,10);
        TextView shuffle=button("⤨   Sıradaki Hazır Görevi Seç",true);
        summary.addView(shuffle,columnParams(-1,d(50),8));
        shuffle.setOnClickListener(v->{
            SmartPlanner.Entry candidate=planner.nextRandom(System.currentTimeMillis());
            Toast.makeText(this,candidate==null
                ? "Hazır görev yok. Sürelerin dolmasını bekle."
                : "Sıradaki görev: "+candidate.name+" (gözlem modu)",Toast.LENGTH_LONG).show();
        });
        addCard(contentArea,summary);

        for(SmartPlanner.Entry entry:planner.all()){
            LinearLayout item=card();
            long time=System.currentTimeMillis();
            item.addView(UiKit.label(this,entry.name,16,Color.WHITE,true));
            addSpace(item,4);
            item.addView(UiKit.label(this,
                entry.ready(time)?"● Hazır":
                entry.disabled?"○ Devre dışı":"◷ "+SmartPlanner.remaining(entry.readyAt,time)
                   +" kaldı • "+SmartPlanner.clock(entry.readyAt),
                12,entry.ready(time)?0xff79efce:0xffe4c88e,false));
            addSpace(item,9);
            LinearLayout controls=horizontal();
            TextView quarter=UiKit.center(this,"15 dk",12,Color.WHITE,true);
            TextView hour=UiKit.center(this,"1 saat",12,Color.WHITE,true);
            TextView ready=UiKit.center(this,"Hazır",12,Color.WHITE,true);
            TextView off=UiKit.center(this,entry.disabled?"Aç":"Kapat",12,Color.WHITE,true);
            TextView[] bs={quarter,hour,ready,off};
            for(TextView v:bs){
                v.setBackground(UiKit.field(this,9));
                LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,d(40),1);
                lp.rightMargin=d(4);
                controls.addView(v,lp);
            }
            quarter.setOnClickListener(v->{planner.waitFor(entry.id,15*60*1000L);build(3);});
            hour.setOnClickListener(v->{planner.waitFor(entry.id,60*60*1000L);build(3);});
            ready.setOnClickListener(v->{planner.ready(entry.id);build(3);});
            off.setOnClickListener(v->{planner.disable(entry.id,!entry.disabled);build(3);});
            item.addView(controls);
            addCard(contentArea,item);
        }
    }

    private void about() {
        contentArea.setGravity(Gravity.CENTER_HORIZONTAL);
        addSpace(contentArea,35);
        ImageView bigLogo=UiKit.logo(this,123);
        contentArea.addView(bigLogo,new LinearLayout.LayoutParams(d(123),d(123)));
        addSpace(contentArea,17);
        contentArea.addView(UiKit.center(this,"LK Makro",30,Color.WHITE,true));
        addSpace(contentArea,3);
        contentArea.addView(UiKit.center(this,"PREMIUM  •  v2.5",12,UiKit.CYAN,true));
        addSpace(contentArea,28);
        LinearLayout card=card();
        card.addView(UiKit.label(this,"Otomatik Dokunuş",17,Color.WHITE,true));
        addSpace(card,10);
        card.addView(subtitle("Sürüklenebilir 1–100 dokunma noktası, zaman bazlı ek molalar ve kullanıcı kontrollü rastgele beklemeler."));
        addSpace(card,13);
        card.addView(subtitle("Bu uygulama OCR yapmaz. Sabit konumları kullanır. Arayüz değişirse dokunuşlar yanlış yerlere gidebilir."));
        addSpace(card,13);
        card.addView(subtitle("Oyunların otomasyon kuralları farklı olabilir. Rastgele süre kullanılması hesabın yaptırımdan korunacağı anlamına gelmez."));
        addCard(contentArea,card);
        TextView cta=button("⚙   Ayarlara Git",true);
        contentArea.addView(cta,columnParams(-1,d(55),4));
        cta.setOnClickListener(v->build(1));
    }

    private void copyDiagnostics() {
        String error=prefs.getString("last_service_error","");
        if(error.isEmpty()) {
            Toast.makeText(this,"Henüz kayıtlı uygulama çökmesi yok.",Toast.LENGTH_SHORT).show();
            return;
        }
        ClipboardManager clip=(ClipboardManager)getSystemService(CLIPBOARD_SERVICE);
        clip.setPrimaryClip(ClipData.newPlainText("LK Makro hata kaydı",error));
        Toast.makeText(this,"Hata kaydı kopyalandı.",Toast.LENGTH_SHORT).show();
    }

    private void restoreFloatingButton() {
        if (MacroAccessibilityService.restoreBubble()) {
            Toast.makeText(this,
                "LK düğmesi sol tarafa geri çağrılıyor. Oyuna dönüp kontrol et.",
                Toast.LENGTH_LONG).show();
            new android.os.Handler(getMainLooper()).postDelayed(this::refreshStatus, 1000);
        } else {
            Toast.makeText(this,
                "Dokunma hizmeti çalışmıyor. Erişilebilirlikten kapatıp yeniden aç.",
                Toast.LENGTH_LONG).show();
            startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
        }
    }

    private void refreshStatus() {
        if (status==null) return;
        if (MacroAccessibilityService.isBubbleVisible()) {
            status.setText("●  LK düğmesi aktif ve ekranda");
        } else if (MacroAccessibilityService.isConnected()) {
            status.setText("⚠  Hizmet açık ancak LK düğmesi görünmüyor. Geri getir'e dokun.");
        } else {
            String lastError=prefs.getString("last_service_error","");
            status.setText("◌  Hizmet bağlı değil. Erişilebilirlikten aç."
                + (lastError.isEmpty()?"":"\nHata kaydı var. Kopyala ile bana ilet."));
        }
    }

    @Override protected void onResume() {
        super.onResume();
        refreshStatus();
    }
}
