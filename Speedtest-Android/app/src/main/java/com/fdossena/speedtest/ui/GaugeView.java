package com.fdossena.speedtest.ui;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import your.name.here.speedtest.R;

public class GaugeView extends View {
    private float strokeWidth;
    private int backgroundColor;
    private int fillColor;
    private int startAngle;
    private int angles;
    private int maxValue;
    private int value=0;

    public GaugeView(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.GaugeView, 0, 0);
        setStrokeWidth(a.getDimension(R.styleable.GaugeView_gauge_strokeWidth, 10));
        setBackgroundColor(a.getColor(R.styleable.GaugeView_gauge_backgroundColor, 0xFFCCCCCC));
        setFillColor(a.getColor(R.styleable.GaugeView_gauge_fillColor, 0xFFFFFFFF));
        setStartAngle(a.getInt(R.styleable.GaugeView_gauge_startAngle, 135));
        setAngles(a.getInt(R.styleable.GaugeView_gauge_angles, 270));
        setMaxValue(a.getInt(R.styleable.GaugeView_gauge_maxValue, 1000));
    }

    public GaugeView(Context context) {
        super(context);
    }

    private Paint paint=null;
    private RectF rect=null;
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float size = getWidth()<getHeight() ? getWidth() : getHeight();
        float w = size - (2*strokeWidth);
        float h = size - (2*strokeWidth);
        float radius = (w < h ? w/2 : h/2);
        if(rect==null) rect = new RectF();
        rect.set((getWidth() - (2*strokeWidth))/2 - radius + strokeWidth, (getHeight() - (2*strokeWidth))/2 - radius + strokeWidth, (getWidth() - (2*strokeWidth))/2 - radius + strokeWidth + w, (getHeight() - (2*strokeWidth))/2 - radius + strokeWidth + h);
        if(paint==null) paint = new Paint();
        paint.setStrokeWidth(strokeWidth);
        paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setColor(backgroundColor);
        canvas.drawArc(rect, startAngle, angles, false, paint);
        paint.setColor(fillColor);
        canvas.drawArc(rect, startAngle, (float)((startAngle + value *((double) Math.abs(angles) / maxValue))- startAngle), false, paint);
    }

    public void setValue(int value) {
        this.value = value;
        invalidate();
    }

    public int getValue() {
        return value;
    }

    public float getStrokeWidth() {
        return strokeWidth;
    }

    public void setStrokeWidth(float strokeWidth) {
        this.strokeWidth = strokeWidth;
        invalidate();
    }

    public int getBackgroundColor() {
        return backgroundColor;
    }

    public void setBackgroundColor(int backgroundColor) {
        this.backgroundColor = backgroundColor;
        invalidate();
    }

    public void setFillColor(int fillColor) {
        this.fillColor = fillColor;
        invalidate();
    }

    public int getFillColor() {
        return fillColor;
    }

    public int getStartAngle() {
        return startAngle;
    }

    public void setStartAngle(int startAngle) {
        this.startAngle = startAngle;
        invalidate();
    }

    public int getAngles() {
        return angles;
    }

    public void setAngles(int angles) {
        this.angles = angles;
        invalidate();
    }

    public int getMaxValue() {
        return maxValue;
    }

    public void setMaxValue(int maxValue) {
        this.maxValue = maxValue;
        invalidate();
    }

}