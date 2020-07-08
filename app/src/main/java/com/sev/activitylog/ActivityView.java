package com.sev.activitylog;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;

import java.util.LinkedList;

/**
 * Displays ride metrics in a grid layout on a round rect background
 */
public class ActivityView extends View implements Subject {
    private String title, subtitle;
    private StringPair[][] info;
    private int infoPadding = 3;
    private long id;

    private TextPaint textPaint;
    private Paint paint;

    private int x, y, w, h;
    private int pl, pr, pb, pt; //padding left, right, bottom, top
    private float gridX, gridY; //width and height of each grid cell
    private float dp2pxFactor;

    private Map img;
    private Rect mapDimensions;

    private LinkedList<Observer> observers;

    private ActivityViewFont fnt;

    public ActivityView(Context context) {
        super(context);
        init(null, 0);
    }

    public ActivityView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs, 0);
    }

    public ActivityView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(attrs, defStyle);
    }

    private void init(AttributeSet attrs, int defStyle) {
        // Load attributes from xml (Optional)
        fnt = new ActivityViewFont();
        final TypedArray a = getContext().obtainStyledAttributes(
                attrs, R.styleable.ActivityView, defStyle, 0);

        if (a.hasValue(R.styleable.ActivityView_title))
            title = a.getString(R.styleable.ActivityView_title);
        if(a.hasValue(R.styleable.ActivityView_subtitle))
            subtitle = a.getString(R.styleable.ActivityView_subtitle);
        if(a.hasValue(R.styleable.ActivityView_titleSize))
            fnt.titleSize = a.getInt(R.styleable.ActivityView_titleSize, (int)fnt.titleSize);
        if(a.hasValue(R.styleable.ActivityView_subtitleSize))
            fnt.subtitleSize = a.getInt(R.styleable.ActivityView_subtitleSize, (int)fnt.subtitleSize);
        if(a.hasValue(R.styleable.ActivityView_infoSize))
            fnt.infoSize = a.getInt(R.styleable.ActivityView_infoSize, (int)fnt.infoSize);
        if(a.hasValue(R.styleable.ActivityView_labelSize))
            fnt.labelSize = a.getInt(R.styleable.ActivityView_titleSize, (int)fnt.labelSize);

        a.recycle();

        // Set up a default TextPaint object
        textPaint = new TextPaint();
        textPaint.setFlags(Paint.ANTI_ALIAS_FLAG);
        textPaint.setTextAlign(Paint.Align.LEFT);

        pl = getPaddingLeft();
        pr = getPaddingRight();
        pb = getPaddingBottom();
        pt = getPaddingTop();
        dp2pxFactor = getResources().getDisplayMetrics().density;

        paint = new Paint();
        paint.setColor(Color.argb(255, 255, 69, 0));
        paint.setStrokeWidth(2);

        setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                clickEvent(view);
            }
        });
        observers = new LinkedList<>();
    }
    public void setId(long id) {this.id = id;}
    @Override
    protected void onDraw(Canvas canvas) {
        //canvas is translated to child position
        //(0, 0) is this views origin
        super.onDraw(canvas);


        Drawable bg = getResources().getDrawable(R.drawable.round_rect_shadow, null);
        bg.setBounds(pl, pt, w - pr, h); //left top right bottom
        bg.draw(canvas);
        textPaint.setTextSize(fnt.titleSize * dp2pxFactor);
        textPaint.setColor(Color.BLACK);
        float space = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8, getResources().getDisplayMetrics()); //converts 8dip to px
        final Paint.FontMetrics fm = textPaint.getFontMetrics();
        final float titlelineHeight = fm.bottom - fm.top + fm.leading;
        if(title != null) {
            Rect titleBounds = new Rect();
            textPaint.getTextBounds(title, 0, title.length(), titleBounds);
            canvas.drawText(title, pl + space, pt + space + titleBounds.height(), textPaint);
        }
        if(subtitle != null) {
            Rect subtitleBounds = new Rect();
            textPaint.setTextSize(fnt.subtitleSize * dp2pxFactor);
            textPaint.setColor(Color.GRAY);
            float subLen = textPaint.measureText(subtitle);
            textPaint.getTextBounds(subtitle, 0, subtitle.length(), subtitleBounds);
            canvas.drawText(subtitle, w - pr - space * 2 - subLen, pt + space, textPaint);
        }
        if(info != null) {
            for (int r = 0; r < info.length; ++r) {
                for (int c = 0; c < info[r].length; ++c) {
                    if(info[r][c] != null) {
                        textPaint.setTextSize(fnt.labelSize * dp2pxFactor);
                        textPaint.setColor(Color.BLACK);
 /*                       if (textPaint.measureText(info[r][c].getKey()) > gridX) {
                            float lengthPerLetter = (float) textPaint.measureText(info[r][c].getKey()) / info[r][c].getKey().length();
                            float letters = (float) gridX / lengthPerLetter;
                            if (info[r][c].getKey().length() > (int) letters)
                                info[r][c].setKey(info[r][c].getKey().substring(0, (int) letters));
                        }*/
                        canvas.drawText(info[r][c].getKey(), pl + space + gridX * c, pt + space + titlelineHeight + gridY * r + infoPadding * dp2pxFactor, textPaint);
                        Rect keyBounds = new Rect();
                        textPaint.getTextBounds(info[r][c].getKey(), 0, info[r][c].getKey().length(), keyBounds);
                        textPaint.setTextSize(fnt.infoSize * dp2pxFactor);
                        textPaint.setColor(Color.GRAY);
 /*                       if (textPaint.measureText(info[r][c].getValue()) > gridX) {
                            float lengthPerLetter = (float) textPaint.measureText(info[r][c].getValue()) / info[r][c].getValue().length();
                            float letters = (float) gridX / lengthPerLetter;
                            if (info[r][c].getValue().length() > (int) letters)
                                info[r][c].setValue(info[r][c].getValue().substring(0, (int) letters));
                        }*/
                        canvas.drawText(info[r][c].getValue(), pl + space + gridX * c, pt + space + titlelineHeight + keyBounds.height() + gridY * r + infoPadding * dp2pxFactor, textPaint);
                    }
                }
            }
        }
        if(img != null){
            Rect dst = new Rect((int)(mapDimensions.left * gridX + pl + space), (int)(mapDimensions.top * gridY + pt + titlelineHeight),
                    (int)(mapDimensions.right * gridX + pl + space), (int)(mapDimensions.bottom * gridY + pt + titlelineHeight)); //left top right bottom
            img.draw(canvas, img.calcPreservingDestination(dst), paint);

        }


    }

    @Override
    protected void onLayout(boolean again, int x, int y, int right, int bottom){
        super.onLayout(again, x, y, w, h);
        this.x = x;
        this.y = y;
        this.w = right - x;
        this.h = bottom - y;
        if(info != null) {
            gridX = (w - pr - pl) / info[0].length;
            Rect bounds = new Rect();
            textPaint.setTextSize(dp2pxFactor * fnt.titleSize);
            Paint.FontMetrics fm = textPaint.getFontMetrics();
            float titlelineHeight = fm.bottom - fm.top + fm.leading;
            gridY = (h - titlelineHeight - pt - pb) / (float)info.length;
        }
    }

    @Override
    protected void onMeasure(int w, int h){
        super.onMeasure(w, h);
        setMeasuredDimension(w, h);
        this.w = w;
        this.h = h;
    }
    public void setTitle(String title){this.title = title;}
    public void setInfoGrid(int rows, int cols) {
        info = new StringPair[rows][cols];
        gridX = (w - pr - pl) / cols;
        Rect bounds = new Rect();
        textPaint.setTextSize(dp2pxFactor * fnt.titleSize);
        Paint.FontMetrics fm = textPaint.getFontMetrics();
        float titlelineHeight = fm.bottom - fm.top + fm.leading;
        gridY = (h - titlelineHeight - pt - pb) / (float)rows;
    }
    public void setFont(ActivityViewFont f) {fnt = f;}
    public void setInfo(String label, String data, int row, int col){
        if(info != null && row < info.length && col < info[0].length){
            info[row][col] = new StringPair(label, data);
        }
    }
    public StringPair getInfo(int row, int col){
        if(info != null && row < info.length && col < info[0].length)
            return info[row][col];
        else return null;
    }
    public void setMap(Map map, int row, int col, int rowspan, int colspan){
        img = map;
        mapDimensions = new Rect(col, row, col + colspan, row + rowspan);
    }
    public void setSubtitle(String subtitle) {this.subtitle = subtitle;}
    public void setPadding(Padding padding) {
        pt = (int)(padding.top * dp2pxFactor);
        pb = (int)(padding.bottom * dp2pxFactor);
        pl = (int)(padding.left * dp2pxFactor);
        pr = (int)(padding.right * dp2pxFactor);
    }
    public void setInfoPadding(int padding) {
        this.infoPadding = padding;
    }
    private void clickEvent(View v) {
        ObserverEventArgs a = new ObserverEventArgs(ObserverNotifications.ACTIVITY_SELECT_NOTIFY, id, v);
        for(Observer o : observers)
            o.notify(a);
    }

    @Override
    public void attach(Observer observer) {
        observers.add(observer);
    }

    @Override
    public void detach(Observer observer) {
        observers.remove(observer);
    }
}

class ActivityViewFont {
    public float titleSize = 24, subtitleSize = 14, labelSize = 18, infoSize = 12; //units are dps
}

class ActivityViewFontBuilder {
    private ActivityViewFont font; //units are dps
    ActivityViewFontBuilder(){
        font = new ActivityViewFont();
    }
    ActivityViewFontBuilder titleSize(float s){
        font.titleSize = s;
        return this;
    }
    ActivityViewFontBuilder subtitleSize(float s){
        font.subtitleSize = s;
        return this;
    }
    ActivityViewFontBuilder labelSize(float s){
        font.labelSize = s;
        return this;
    }
    ActivityViewFontBuilder infoSize(float s){
        font.infoSize = s;
        return this;
    }
    ActivityViewFont build(){
        return font;
    }
}

