package com.lodoskent.makrosafe;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.content.SharedPreferences;
import android.net.Uri;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

/**
 * Zero-permission offline screenshot reader. User chooses each screenshot using
 * Android's document picker; no Accessibility, screen capture, or internet permissions.
 */
public final class SafeActivity extends Activity {
    private static final int PICK_IMAGE=1337;
    private final int BG=0xff061124, DARK=0xff102647, BORDER=0xff35648e;
    private final int CYAN=0xff60e6f4, SECOND=0xffa8c5df;
    private SafePlanner planner;
    private SharedPreferences prefs;
    private LinearLayout body;
    private boolean processing=false;

    private int dp(float value){return (int)(value*getResources().getDisplayMetrics().density+.5f);}
    private GradientDrawable panel(int color,int radius){
        GradientDrawable g=new GradientDrawable();
        g.setColor(color);g.setCornerRadius(dp(radius));
        g.setStroke(dp(1),BORDER);return g;
    }
    private GradientDrawable gradient(){
        GradientDrawable g=new GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            new int[]{0xff4463e8,0xff24a9dc,0xff26d4cf});
        g.setCornerRadius(dp(13));return g;
    }
    private TextView label(String text,int size,int color,boolean bold){
        TextView v=new TextView(this);
        v.setText(text);v.setTextColor(color);v.setTextSize(size);
        if(bold)v.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        return v;
    }
    private TextView button(String text,boolean primary){
        TextView v=label(text,15,Color.WHITE,true);
        v.setGravity(Gravity.CENTER);
        v.setPadding(dp(11),dp(13),dp(11),dp(13));
        v.setBackground(primary?gradient():panel(DARK,12));
        return v;
    }
    private LinearLayout box(){
        LinearLayout v=new LinearLayout(this);
        v.setOrientation(LinearLayout.VERTICAL);
        v.setPadding(dp(15),dp(15),dp(15),dp(15));
        v.setBackground(panel(0xff0e2342,16));
        return v;
    }
    private void space(LinearLayout where,int height){
        View v=new View(this);
        where.addView(v,new LinearLayout.LayoutParams(1,dp(height)));
    }
    private void section(LinearLayout child){
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);
        lp.bottomMargin=dp(13);body.addView(child,lp);
    }
    @Override protected void onCreate(Bundle state){
        super.onCreate(state);
        planner=new SafePlanner(this);
        prefs=getSharedPreferences("lk_safe_ocr",MODE_PRIVATE);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        draw();
    }
    private void draw(){
        ScrollView scroll=new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(BG);
        body=new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(dp(17),dp(18),dp(17),dp(22));
        scroll.addView(body);setContentView(scroll);

        LinearLayout hero=new LinearLayout(this);
        hero.setGravity(Gravity.CENTER_VERTICAL);
        ImageView icon=new ImageView(this);
        icon.setImageResource(R.drawable.lk_launcher_icon);
        icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
        hero.addView(icon,new LinearLayout.LayoutParams(dp(59),dp(59)));
        LinearLayout names=new LinearLayout(this);
        names.setOrientation(LinearLayout.VERTICAL);
        names.setPadding(dp(12),0,0,0);
        names.addView(label("LK Makro Görüş",23,Color.WHITE,true));
        names.addView(label("İZİNLERİ AZALTILMIŞ • v2.4",10,CYAN,true));
        hero.addView(names);
        body.addView(hero);
        space(body,18);

        LinearLayout security=box();
        security.addView(label("🔒  Hassas yetki istemez",18,0xff8df4c2,true));
        space(security,8);
        security.addView(label("Bu sürümde erişilebilirlik hizmeti, otomatik dokunma, internet ve ekranı gizlice yakalama yok. OCR yalnızca senin seçtiğin ekran görüntüsünü cihazda okur.",13,SECOND,false));
        space(security,7);
        security.addView(label("ALTIN HARCAMA: 0 • Otomatik satın alma: YOK",13,0xff9cecb2,true));
        section(security);

        LinearLayout reader=box();
        reader.addView(label("👁  Ekran Görüntüsü OCR",19,Color.WHITE,true));
        space(reader,8);
        reader.addView(label("Warline'da ekran görüntüsü al. Buradan seç; oyun yazıları cihaz içinde okunup tanınacak. Fotoğraf sunucuya yüklenmez.",13,SECOND,false));
        space(reader,11);
        TextView pick=button(processing?"Okunuyor…":"📷  Ekran Görüntüsü Seç ve Oku",true);
        pick.setOnClickListener(v->pickImage());
        reader.addView(pick);
        space(reader,12);
        String result=prefs.getString("last_text","");
        String fail=prefs.getString("last_error","");
        reader.addView(label("Tanınan ekran: "+prefs.getString("last_screen","Henüz yok"),14,CYAN,true));
        if(!fail.isEmpty()){
            space(reader,5);
            reader.addView(label("Hata: "+fail,12,0xffffb09f,false));
        }
        if(!result.isEmpty()){
            space(reader,8);
            TextView extracted=label(result.length()>1600?result.substring(0,1600)+"…":result,
                12,0xffd7eaff,false);
            extracted.setTextIsSelectable(true);
            reader.addView(extracted);
        }
        section(reader);

        LinearLayout task=box();
        task.addView(label("🔀  Akıllı Görev Planlayıcısı",19,Color.WHITE,true));
        space(task,7);
        task.addView(label("Hazır görev: "+planner.readyCount()+" / "+SafePlanner.IDS.length,
            13,SECOND,false));
        space(task,6);
        task.addView(label("Bekleyen veya kapalı görev seçilmez. Sıradaki görev rastgele belirlenir; hiçbir oyun dokunuşu yapılmaz.",12,SECOND,false));
        space(task,10);
        TextView select=button("Sıradaki Hazır Görevi Seç",true);
        select.setOnClickListener(v->{
            SafePlanner.Job chosen=planner.next();
            Toast.makeText(this,chosen==null?"Hazır görev yok":
                "Seçilen: "+chosen.name+" (gözlem modu)",Toast.LENGTH_LONG).show();
        });
        task.addView(select);
        section(task);

        for(SafePlanner.Job job:planner.all()){
            LinearLayout card=box();
            long now=System.currentTimeMillis();
            card.addView(label(job.name,16,Color.WHITE,true));
            space(card,5);
            String status=job.disabled?"Devre dışı":
                job.ready(now)?"Hazır":("Bekliyor • "+SafePlanner.remaining(job.deadline));
            card.addView(label(status,12,job.ready(now)?0xff8df4c2:0xfff6d39a,false));
            space(card,10);
            LinearLayout actions=new LinearLayout(this);
            actions.setOrientation(LinearLayout.HORIZONTAL);
            String[] texts={"15 dk","1 saat","Hazır",job.disabled?"Aç":"Kapat"};
            for(int i=0;i<texts.length;i++){
                final int command=i;
                TextView act=button(texts[i],false);
                act.setTextSize(12);
                LinearLayout.LayoutParams cell=new LinearLayout.LayoutParams(0,dp(43),1);
                cell.rightMargin=dp(4);
                actions.addView(act,cell);
                act.setOnClickListener(v->{
                    if(command==0)planner.waitFor(job.id,15*60_000L);
                    else if(command==1)planner.waitFor(job.id,60*60_000L);
                    else if(command==2)planner.clear(job.id);
                    else planner.toggle(job.id,!job.disabled);
                    draw();
                });
            }
            card.addView(actions);
            section(card);
        }
        body.addView(label("Açıklama: Manuel bekleme süreleri örnektir. Oyunun gerçek sayaçlarını OCR ile otomatik kaydetme sonraki aşamadır. Play Protect'in onayı garanti edilemez.",
            12,SECOND,false));
    }
    private void pickImage(){
        if(processing)return;
        try{
            Intent intent=new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("image/*");
            startActivityForResult(intent,PICK_IMAGE);
        }catch(Exception error){
            Toast.makeText(this,"Fotoğraf seçici açılamadı.",Toast.LENGTH_LONG).show();
        }
    }
    @Override protected void onActivityResult(int request,int code,Intent data){
        super.onActivityResult(request,code,data);
        if(request!=PICK_IMAGE||code!=RESULT_OK||data==null||data.getData()==null)return;
        Uri uri=data.getData();
        processing=true;draw();
        final TextRecognizer client=TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
        try{
            InputImage image=InputImage.fromFilePath(this,uri);
            client.process(image).addOnSuccessListener(result->{
                String content=result.getText();
                String screen=SafePlanner.classify(content);
                prefs.edit().putString("last_text",content)
                    .putString("last_error","").putString("last_screen",screen).apply();
                Toast.makeText(this,"Ekran tanındı: "+screen,Toast.LENGTH_LONG).show();
            }).addOnFailureListener(error->{
                prefs.edit().putString("last_error",error.getClass().getSimpleName())
                    .putString("last_screen","Okunamadı").apply();
            }).addOnCompleteListener(done->{
                client.close();processing=false;draw();
            });
        }catch(Exception ex){
            client.close();processing=false;
            prefs.edit().putString("last_error",ex.getClass().getSimpleName()).apply();
            draw();
        }
    }
}
