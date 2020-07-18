package com.sev.activitylog;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Pair;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class GraphView extends View implements View.OnTouchListener {
    private Rect drawArea, totalArea;
    private GraphStyle style;
    private GraphScale scale;
    private float zoomX = 1, zoomY = 1;
    private float initialZoomX = 1, initialZoomY = 1;
    private float pixelsPerX, pixelsPerY;
    private float startX = 0, startY = 0;

    private ArrayList<ArrayList<Pair<Double, Double>>> data;
    private ArrayList<Regression> regs;

    private final float dp2px;
    private final int labelSize = 8;

    private boolean graphDirty = true;

    private Paint linePaint, textPaint;

   private float[] lastTouch = new float[] {-1, -1, -1, -1};
    public GraphView(Context context) {
        super(context);
        init(null, 0);
        dp2px = getResources().getDisplayMetrics().density;
    }

    public GraphView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(attrs, 0);
        dp2px = getResources().getDisplayMetrics().density;
    }

    public GraphView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs, 0);
        dp2px = getResources().getDisplayMetrics().density;
    }

    public GraphView(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(attrs, 0);
        dp2px = getResources().getDisplayMetrics().density;
    }

    private void init(AttributeSet attr, int defStyle){
        drawArea = new Rect();
        totalArea = new Rect();
        data = new ArrayList<>();
        final TypedArray attrs = getContext().obtainStyledAttributes(attr, R.styleable.ActivityView, defStyle, 0);
        linePaint = new Paint();
        linePaint.setAntiAlias(true);
        linePaint.setStyle(Paint.Style.STROKE);
        textPaint = new Paint();
        textPaint.setAntiAlias(true);
        textPaint.setColor(Color.GRAY);
        setOnTouchListener(this);

    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        initGraph();
        if(style == null) return;
        for(int i = 0; i < data.size(); ++i){
            Path p = new Path();
            int startIndex = getIndexOfStartX(startX, data.get(i));
            float x = (data.get(i).get(startIndex).first.floatValue() - startX) * pixelsPerX * zoomX + drawArea.left;
            float y = drawArea.bottom - (data.get(i).get(startIndex).second.floatValue() - startY) * pixelsPerY * zoomY;
            p.moveTo(x, y);
            for(int j = startIndex + 1; j < data.get(i).size() && x < drawArea.right; ++j){
                float x2 = (data.get(i).get(j).first.floatValue() - startX) * pixelsPerX * zoomX + drawArea.left;
                float y2 = drawArea.bottom - (data.get(i).get(j).second.floatValue() - startY) * pixelsPerY * zoomY;
//                p.quadTo((x + x2) / 2, (y + y2) / 2, x2, y2);
                p.lineTo(x2, y2);
                x = x2;
                y = y2;
            }
            linePaint.setStrokeWidth(style.strokes[i]);
            linePaint.setColor(style.colors[i]);
            linePaint.setStrokeCap(Paint.Cap.ROUND);
            canvas.drawPath(p, linePaint);

            if(style.showTrend){
                Regression r = regs.get(i);
                x = pixelsPerX * zoomX + drawArea.left;
                y = drawArea.bottom - (r.getY(startX) - startY) * pixelsPerY * zoomY;
                p.moveTo(x, y);
                x = drawArea.right;
                y = drawArea.bottom - (r.getY((x - drawArea.left) / (pixelsPerX * zoomX) + startX) - startY) * pixelsPerY * zoomY;
                p.lineTo(x, y);
                if((style.flags & GraphStyle.MATCH_REG_TO_LINE) > 0){
                    linePaint.setStrokeWidth(1);
                    linePaint.setColor(Util.setAlpha(style.colors[i], 0.7f));
                }else {
                    linePaint.setStrokeWidth(style.trendStroke);
                    linePaint.setColor(style.trendColor);
                }
                canvas.drawPath(p, linePaint);
            }
        }
        int x = drawArea.left;
        linePaint.setColor(Util.rgba(0.5f, .5f, .5f, .5f));
        linePaint.setStrokeWidth(1);
        do{
            float val = (x - drawArea.left) / (pixelsPerX * zoomX) + startX;
            canvas.drawText(formatX(val), x, drawArea.bottom, textPaint);
            if((style.flags & GraphStyle.VERT_GRID) > 0){
                canvas.drawLine(x, drawArea.bottom, x, drawArea.top, linePaint);
            }
            x += textPaint.measureText(formatX(val)) + 3 * dp2px;
        } while(x < drawArea.right);
        int y = drawArea.bottom;
        float textHeight = getTextHeight(textPaint);
        do{
            float val = (drawArea.bottom - y) / (pixelsPerY * zoomY) + startY;
            canvas.drawText(String.format("%.1f", val), totalArea.left, y, textPaint);
            if((style.flags & GraphStyle.HORZ_GRID) > 0)
                canvas.drawLine(drawArea.left, y, drawArea.right, y, linePaint);
            y -= textHeight - 3 * dp2px;
        } while(y > drawArea.top);
        canvas.drawText(style.xAxis, (totalArea.right + totalArea.left) / 2.f - textPaint.measureText(style.xAxis) / 2.f, (totalArea.bottom - drawArea.bottom) / 2 + drawArea.bottom, textPaint);
        canvas.drawText(style.yAxis, totalArea.left, totalArea.top, textPaint);
        canvas.drawText(style.title, (totalArea.right + totalArea.left) / 2.f - textPaint.measureText(style.title) / 2.f, totalArea.top, textPaint);


    }
    private int getIndexOfStartX(double startX, ArrayList<Pair<Double, Double>> data){
        int index, start = 0, end = data.size() - 1;
        do{
            index = (int)Math.round((end - start) / (data.get(end).first - data.get(start).first) * (startX - data.get(start).first) + start);
            if(index < start) index = start;
            if(index > end) index = end;
            if(data.get(index).first.equals(startX)) return index;
            if(data.get(index).first > startX)
                end = index - 1;
            else if(data.get(index).first < startX)
                start = index + 1;
        } while(start <= end);
        return index;
    }
    @Override
    protected void onLayout(boolean repeat, int left, int top, int right, int bottom){
        super.onLayout(repeat, left, top, right, bottom);
        graphDirty = true;
        textPaint.setTextSize(labelSize * dp2px);
        float textHeight = getTextHeight(textPaint);
        totalArea.right = right - getPaddingRight();
        totalArea.left = left + getPaddingLeft();
        totalArea.bottom = bottom - getPaddingBottom();
        totalArea.top = top + getPaddingTop();
        drawArea.left = totalArea.left + Math.round(textPaint.measureText("1234"));
        drawArea.right = totalArea.right;
        drawArea.top = totalArea.top + Math.round(textHeight);
        drawArea.bottom = totalArea.bottom - 2 * Math.round(textHeight);
    }
    public void setScale(GraphScale scale){
        this.scale = scale;
        graphDirty = true;
    }
    public GraphStyle getStyle() {
        return style;
    }
    public void setStyle(GraphStyle style){
        this.style = style;
    }
    public void setData(ArrayList<Pair<Double, Double>> dataPoints, int lineNum){
        if(lineNum < data.size())
            data.set(lineNum, sort(dataPoints, 0, dataPoints.size() - 1));
        else
            data.add(lineNum, sort(dataPoints, 0, dataPoints.size() - 1));
        graphDirty = true;
    }
    private float getTextHeight(Paint p){
        final Paint.FontMetrics fm = p.getFontMetrics();
        final float textHeight = fm.bottom - fm.top + fm.leading;
        return textHeight;
    }
    private void initGraph(){
        if(graphDirty){
            graphDirty = false;

            int maxSize = 0;
            ArrayList<Pair<Double, Double>> biggest = null;
            float mnx = System.currentTimeMillis(), mmx = Integer.MIN_VALUE, mmy = Integer.MIN_VALUE, mny = Integer.MAX_VALUE;
            for(ArrayList<Pair<Double, Double>> l : data) {
                if (l.size() > maxSize) {
                    maxSize = l.size();
                    biggest = l;
                }
                if(scale == null) {
                    for (Pair<Double, Double> p : l) {
                        if (p.first < mnx) mnx = p.first.floatValue();
                        if (p.first > mmx) mmx = p.first.floatValue();
                        if (p.second < mny) mny = p.second.floatValue();
                        if (p.second > mmy) mmy = p.second.floatValue();
                    }
                }
            }
            if(scale == null) scale = new GraphScale(mmx, mnx, mmy, mny);
            pixelsPerX = (drawArea.right - drawArea.left) / (scale.maxX - scale.minX);
            pixelsPerY = (drawArea.bottom - drawArea.top) / (scale.maxY - scale.minY);

            if(biggest != null && biggest.size() > 30){
                initialZoomX = zoomX = (drawArea.right - drawArea.left) / (float)(biggest.get(biggest.size() - 1).first - biggest.get(biggest.size() - 31).first) / pixelsPerX;
//                zoomY = (drawArea.right - drawArea.left) / (float)(biggest.get(biggest.size() - 1).second - biggest.get(biggest.size() - 31).second) / pixelsPerY;
                startX = biggest.get(biggest.size() - 31).first.floatValue();
            }
            startY = scale.minY;
//            startX = scale.minX;

            regs = new ArrayList<>(data.size());
            for(ArrayList<Pair<Double, Double>> points : data)
                regs.add(Regression.linearRegression(points));
        }
    }
    private String formatX(float val){
        if((style.flags & GraphStyle.STRING_AS_X) > 0)
            return Util.fromBase26((long)val);
        switch(style.type){
            case TYPE_TIME_LINE:
                return TimeSpan.fromSeconds((long)val);
            case TYPE_DATE_LINE:
                return new SimpleDateFormat("MM/dd/yy").format(new Date((long)val));
            default:
                return String.format("%.2f", val);
        }
    }

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        switch(motionEvent.getActionMasked()){
            case MotionEvent.ACTION_MOVE:
            {
                if(motionEvent.getPointerCount() == 1) {
                    if (lastTouch[0] != -1) {
                        float dx = lastTouch[0] - motionEvent.getX();
                        float dy = lastTouch[1] - motionEvent.getY();
                        startX += dx / (pixelsPerX * zoomX);
                        if (startX <= scale.minX) startX = scale.minX;
                        if (startX >= scale.maxX) startX = scale.maxX;
                    }
                }else if(motionEvent.getPointerCount() == 2){
                    if(lastTouch[0] != -1 && lastTouch[2] != -1){
                        float dx = Math.abs(motionEvent.getX(0) - motionEvent.getX(1)) - Math.abs(lastTouch[0] - lastTouch[2]); //delta x-dist
                        float dy = Math.abs(motionEvent.getY(0) - motionEvent.getY(1)) - Math.abs(lastTouch[1] - lastTouch[3]); //delta y-dist
                        float dd = (float)Math.sqrt(Math.pow(motionEvent.getX(0) - motionEvent.getX(1), 2) + Math.pow(motionEvent.getY(0) - motionEvent.getY(1), 2)) -
                                (float)Math.sqrt(Math.pow(lastTouch[0] - lastTouch[2], 2) + Math.pow(lastTouch[1] - lastTouch[3], 2));
                        zoomX += dd * 0.005;
                        if(zoomX <= 1) zoomX = 1;
                        if(zoomX >= initialZoomX * 3) zoomX = initialZoomX * 3;
//                        zoomY += dy * 0.005;
                    }
                    lastTouch[2] = motionEvent.getX(1);
                    lastTouch[3] = motionEvent.getY(1);
                }
                lastTouch[0] = motionEvent.getX(0);
                lastTouch[1] = motionEvent.getY(0);
                break;
            }
            case MotionEvent.ACTION_POINTER_UP:
            case MotionEvent.ACTION_UP:
                lastTouch[2] = -1;
                lastTouch[3] = -1;
                lastTouch[0] = -1;
                lastTouch[1] = -1;
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                if(motionEvent.getActionIndex() == 1){
                    lastTouch[2] = motionEvent.getX(1);
                    lastTouch[3] = motionEvent.getY(1);
                }
                break;
            case MotionEvent.ACTION_DOWN:
                lastTouch[0] = motionEvent.getX();
                lastTouch[1] = motionEvent.getY();
                break;
            default:
                return false;
        }
        invalidate();
        return true;
    }

    private ArrayList<Pair<Double, Double>> sort(ArrayList<Pair<Double, Double>> list, int start, int end){

        if(start < end){
            return merge(sort(list, start, (start + end) / 2), sort(list, (start + end) / 2 + 1, end));
        }
        assert(start == end);
        ArrayList<Pair<Double, Double>> out = new ArrayList<>();
        out.add(new Pair<Double, Double>(list.get(start).first, list.get(start).second));
        return out;
    }
    private ArrayList<Pair<Double, Double>> merge(ArrayList<Pair<Double, Double>> a, ArrayList<Pair<Double, Double>> b){
        ArrayList<Pair<Double, Double>> output = new ArrayList<>();
        int i = 0, j = 0;
        while(i < a.size() && j < b.size()){
            if(a.get(i).first < b.get(j).first){
                output.add(a.get(i++));
            }else if(b.get(j).first <= a.get(i).first)
                output.add(b.get(j++));
        }
        if(i != a.size()) {
            for (; i < a.size(); ++i)
                output.add(a.get(i));
        }else if(j != b.size()){
            for (; j < b.size(); ++j)
                output.add(b.get(j));
        }
        return output;
    }
}
class GraphStyle {
    enum GraphType {
        TYPE_LINE,
        TYPE_DATE_LINE,
        TYPE_TIME_LINE,
        TYPE_BAR
    }
    public GraphType type;
    public int[] colors;
    public int[] strokes;
    public String xAxis, yAxis, title;
    public String[] xNames;
    public boolean showTrend;
    public int trendColor, trendStroke;

    public int flags; //allows for the style to be more easily extended without further overhead. I'll be honest, this is being sort of lazy
    public static final int VERT_GRID = 1;
    public static final int HORZ_GRID = 2;
    public static final int STRING_AS_X = 4;
    public static final int MATCH_REG_TO_LINE = 8;
    public GraphStyle() {
        type = GraphType.TYPE_LINE;
        colors = new int[] {Color.RED};
        strokes = new int[] {2};
        xAxis = yAxis = title = "";
        showTrend = false;
        trendColor = Color.YELLOW;
        trendStroke = 1;
    }
    public GraphStyle clone(){
        GraphStyle c = new GraphStyle();
        c.type = type;
        c.colors = new int[colors.length];
        c.strokes = new int[strokes.length];
        for(int i = 0; i < colors.length; ++i){
            c.colors[i] = colors[i];
        }
        for(int i = 0; i < strokes.length; ++i){
            c.strokes[i] = strokes[i];
        }
        c.xAxis = "" + xAxis; //copy
        c.yAxis = "" + yAxis;
        c.title = "" + title;
        c.trendStroke = trendStroke;
        c.trendColor = trendColor;
        c.showTrend = showTrend;
        if(xNames != null) {
            c.xNames = new String[xNames.length];
            for (int i = 0; i < xNames.length; ++i) {
                c.xNames[i] = "" + xNames[i];
            }
        }
        c.flags = flags;
        return c;

    }
}
class GraphStyleBuilder {
    private GraphStyle style;
    private ArrayList<Integer> colors, strokes;
    public GraphStyleBuilder(){
        style = new GraphStyle();
        colors = new ArrayList<>();
        strokes = new ArrayList<>();
    }
    public GraphStyleBuilder(GraphStyle style){
        this.style = style;//style.clone();
        colors = new ArrayList<>();
        strokes = new ArrayList<>();
        for(int c : style.colors)
            colors.add(c);
        for(int s : style.strokes)
            strokes.add(s);
    }
    public GraphStyleBuilder type(GraphStyle.GraphType type){
        style.type = type;
        return this;
    }
    public GraphStyleBuilder colors(int... color){
        for(int c : color)
            colors.add(c);
        return this;
    }
    public GraphStyleBuilder strokes(int... stroke){
        for(int s : stroke)
            strokes.add(s);
        return this;
    }
    public GraphStyleBuilder color(int color, int lineNum){
        if(colors.size() > lineNum)
            colors.set(lineNum, color);
        else colors.add(lineNum, color);
        return this;
    }
    public GraphStyleBuilder stroke(int stroke, int lineNum){
        if(strokes.size() > lineNum)
            strokes.set(lineNum, stroke);
        else strokes.add(lineNum, stroke);
        return this;
    }
    public GraphStyleBuilder lines(int[] color, int[] stroke){
        for(int i = 0; i < color.length; ++i){
            strokes.add(stroke[i]);
            colors.add(color[i]);
        }
        return this;
    }
    public GraphStyleBuilder x(String xAxis){
        style.xAxis = xAxis;
        return this;
    }
    public GraphStyleBuilder y(String yAxis){
        style.yAxis = yAxis;
        return this;
    }
    public GraphStyleBuilder xAlias(String[] aliases){
        style.xNames = aliases;
        return this;
    }
    public GraphStyleBuilder title(String title){
        style.title = title;
        return this;
    }
    public GraphStyleBuilder regression(boolean enabled){
        style.showTrend = enabled;
        return this;
    }
    public GraphStyleBuilder regresssionStyle(int stroke, int color){
        style.trendColor = color;
        style.trendStroke = stroke;
        return this;
    }
    public GraphStyle build() {
        style.colors = new int[colors.size()];
        style.strokes = new int[strokes.size()];
        for(int i = 0; i < colors.size(); ++i)
            style.colors[i] = colors.get(i);
        for(int i = 0; i < strokes.size(); ++i)
            style.strokes[i] = strokes.get(i);
        return style;
    }
    public GraphStyleBuilder grid(boolean enabled){
        if(enabled)
            addFlag(GraphStyle.HORZ_GRID | GraphStyle.VERT_GRID);
        else
            removeFlag(GraphStyle.HORZ_GRID | GraphStyle.VERT_GRID);
        return this;
    }
    public GraphStyleBuilder addFlag(int flag){
        style.flags |= flag;
        return this;
    }
    public GraphStyleBuilder removeFlag(int flag){
        style.flags &= ~flag;
        return this;
    }
}
class GraphScale {
    public float maxX, minX, maxY, minY;
    public GraphScale(float mxx, float mnx, float mxy, float mny){
        maxX = mxx;
        minX = mnx;
        maxY = mxy;
        minY = mny;
    }
}
class Regression {
    private float m, b;
    private Regression(float slope, float yIntercept){
        m = slope;
        b = yIntercept;
    }
    public static Regression linearRegression(ArrayList<Pair<Double, Double>> points){
        float x = 0, y = 0, xy = 0, xx = 0;
        for(Pair<Double, Double> p : points){
            x += p.first.floatValue();
            y += p.second.floatValue();
            xy += p.first.floatValue() * p.second.floatValue();
            xx += p.first.floatValue() * p.first.floatValue();
        }
        x /= points.size();
        y /= points.size();
        xy /= points.size();
        xx /= points.size();
        float slope = (x * y - xy) / (x * x - xx); // m = (mean(x) * mean(y) - mean(xy)) / (mean(x)^2 - mean(x^2))
        float intercept = y - slope * x; //b = mean(y) - slope * mean(x)
        return new Regression(slope, intercept);
    }
    public float getY(float x){
        return m * x + b;
    }
}
