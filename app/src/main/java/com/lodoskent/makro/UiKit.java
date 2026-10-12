package com.lodoskent.makro;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

public final class UiKit {
    private UiKit() {}
    public static final int BG = Color.rgb(5, 13, 31);
    public static final int CARD = Color.rgb(12, 27, 52);
    public static final int SECONDARY = Color.rgb(157, 179, 215);
    public static final int CYAN = Color.rgb(77, 231, 244);
    public static final int BLUE = Color.rgb(49, 114, 245);
    public static final int WHITE = Color.WHITE;

    public static int dp(Context c, float x) {
        return (int) (x * c.getResources().getDisplayMetrics().density + 0.5f);
    }

    public static GradientDrawable gradient(Context c, int[] colors, float radiusDp, int border, float strokeWidth) {
        GradientDrawable g = new GradientDrawable(
            GradientDrawable.Orientation.TL_BR, colors);
        g.setCornerRadius(dp(c, radiusDp));
        if (strokeWidth > 0) g.setStroke(dp(c, strokeWidth), border);
        return g;
    }

    public static GradientDrawable surface(Context c, float radiusDp) {
        return gradient(c, new int[]{0xff132b50, 0xff08172f}, radiusDp, 0xff304f7b, 1f);
    }

    public static GradientDrawable field(Context c, float radiusDp) {
        return gradient(c, new int[]{0xff0a152c, 0xff0e2040}, radiusDp, 0xff42608c, 1f);
    }

    public static GradientDrawable action(Context c) {
        return gradient(c, new int[]{0xff506afb, 0xff2ebee9, 0xff4de0e4}, 16, 0xff91d8ff, 1f);
    }

    public static TextView label(Context c, String str, int sp, int color, boolean bold) {
        TextView t = new TextView(c);
        t.setText(str);
        t.setTextSize(sp);
        t.setTextColor(color);
        t.setIncludeFontPadding(true);
        if (bold) t.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        else t.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        return t;
    }

    public static TextView center(Context c, String str, int sp, int color, boolean bold) {
        TextView t = label(c, str, sp, color, bold);
        t.setGravity(Gravity.CENTER);
        return t;
    }

    public static ImageView logo(Context c, int dpSize) {
        ImageView image = new ImageView(c);
        image.setImageResource(R.drawable.lk_launcher_icon);
        image.setScaleType(ImageView.ScaleType.FIT_CENTER);
        image.setPadding(dp(c, 2), dp(c, 2), dp(c, 2), dp(c, 2));
        image.setBackground(gradient(c, new int[]{0xff0b2250, 0xff112e59}, 22, 0xff467bf9, 1));
        image.setElevation(dp(c, 7));
        return image;
    }

    public static final class TechBackground extends View {
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        public TechBackground(Context c) { super(c); setLayerType(View.LAYER_TYPE_SOFTWARE, null); }
        @Override protected void onDraw(Canvas canvas) {
            int w = getWidth(), h = getHeight();
            if (w <= 0 || h <= 0) return;
            p.setShader(new LinearGradient(0, 0, w, h,
                new int[]{0xff061124, 0xff0a1c3b, 0xff030916},
                null, Shader.TileMode.CLAMP));
            canvas.drawRect(0,0,w,h,p);
            p.setShader(new RadialGradient(w*0.05f,h*0.11f,w*.80f,
                new int[]{0x73436dff,0x244068cb,0x00234778},
                null,Shader.TileMode.CLAMP));
            canvas.drawCircle(w*.05f,h*.11f,w*.80f,p);
            p.setShader(new RadialGradient(w*.95f,h*.77f,w*.80f,
                new int[]{0x4b11c2eb,0x133571dd,0x00234778},
                null,Shader.TileMode.CLAMP));
            canvas.drawCircle(w*.95f,h*.77f,w*.80f,p);
            p.setShader(null);
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(dp(getContext(),.75f));
            p.setColor(0x203da5ff);
            for(int k=-4;k<11;k++){
                float x=k*dp(getContext(),74);
                canvas.drawLine(x,0,x+w*.35f,h,p);
            }
            p.setColor(0x224cc6ff);
            p.setStrokeWidth(dp(getContext(),1.4f));
            for(int i=0;i<4;i++){
                float cx = w*(i%2==0 ? .8f:.13f);
                float cy = h*(i*.235f+.11f);
                float r = dp(getContext(),45+i*13);
                Path shape = new Path();
                for(int n=0;n<6;n++){
                    double a=Math.PI/3*n + Math.PI/6;
                    float xx=cx+(float)Math.cos(a)*r;
                    float yy=cy+(float)Math.sin(a)*r;
                    if(n==0)shape.moveTo(xx,yy);else shape.lineTo(xx,yy);
                }
                shape.close();
                canvas.drawPath(shape,p);
            }
            p.setStyle(Paint.Style.FILL);
            for(int i=0;i<42;i++){
                int x=(i*191+37)%Math.max(1,w);
                int y=(i*311+73)%Math.max(1,h);
                p.setColor(i%4==0?0x7b83f2ff:0x265fc2ff);
                canvas.drawCircle(x,y, i%5==0?dp(getContext(),1.4f):dp(getContext(),.7f),p);
            }
        }
    }
}
