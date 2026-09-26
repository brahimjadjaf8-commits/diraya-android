package com.jafjaf.maktabati;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Html;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;

public class ReaderActivity extends Activity {
    private TextView content;
    private float size = 20f;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        String title = getIntent().getStringExtra("title");
        String path = getIntent().getStringExtra("path");
        String mime = getIntent().getStringExtra("mime");

        LinearLayout root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL); root.setBackgroundColor(Color.rgb(255,253,247));
        LinearLayout bar = new LinearLayout(this); bar.setOrientation(LinearLayout.HORIZONTAL); bar.setGravity(Gravity.CENTER_VERTICAL); bar.setLayoutDirection(View.LAYOUT_DIRECTION_RTL); bar.setPadding(dp(10),dp(10),dp(10),dp(10)); bar.setBackgroundColor(Color.rgb(122,23,23));
        Button back = button("رجوع"); back.setOnClickListener(v -> finish()); bar.addView(back);
        TextView heading = new TextView(this); heading.setText(title); heading.setTextColor(Color.WHITE); heading.setTextSize(18); heading.setMaxLines(1); heading.setPadding(dp(12),0,dp(12),0); bar.addView(heading,new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1));
        Button plus=button("أ＋"), minus=button("أ−"); plus.setOnClickListener(v->{size=Math.min(34,size+2);content.setTextSize(size);}); minus.setOnClickListener(v->{size=Math.max(14,size-2);content.setTextSize(size);}); bar.addView(plus);bar.addView(minus);root.addView(bar);

        ScrollView scroll = new ScrollView(this); content = new TextView(this); content.setTextColor(Color.rgb(25,31,35)); content.setTextSize(size); content.setLineSpacing(0,1.55f); content.setPadding(dp(18),dp(18),dp(18),dp(40)); content.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG_RTL); scroll.addView(content); root.addView(scroll,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,0,1)); setContentView(root);
        try {
            String text = read(path);
            if (mime != null && mime.equals("text/html")) content.setText(Html.fromHtml(text, Html.FROM_HTML_MODE_LEGACY)); else content.setText(text);
        } catch (Exception e) { Toast.makeText(this,"تعذر قراءة الكتاب",Toast.LENGTH_LONG).show(); }
    }
    private Button button(String s){Button b=new Button(this);b.setText(s);b.setTextColor(Color.WHITE);b.setBackgroundColor(Color.TRANSPARENT);return b;}
    private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
    private String read(String path)throws Exception{try(FileInputStream in=new FileInputStream(path);ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] b=new byte[65536];int n;while((n=in.read(b))!=-1)out.write(b,0,n);return out.toString(StandardCharsets.UTF_8.name());}}
}
