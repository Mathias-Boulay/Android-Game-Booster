package com.spse.gameresolutionchanger;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;

public class WrappedDrawable extends Drawable {

    private final Drawable _drawable;
    protected Drawable getDrawable() {
        return _drawable;
    }

    public WrappedDrawable(Drawable drawable) {
        super();
        _drawable = drawable;
    }

    public WrappedDrawable(Drawable drawable, int left, int top, int right, int bottom) {
        super();
        _drawable = drawable;
        setBounds(left, top, right, bottom);
    }

    @Override
    public void setBounds(int left, int top, int right, int bottom) {
        //update bounds to get correctly
        super.setBounds(left, top, right, bottom);
        Drawable drawable = getDrawable();
        if (drawable != null) {
            drawable.setBounds(left, top, right, bottom);
        }
    }

    @Override
    public void setAlpha(int alpha) {
        Drawable drawable = getDrawable();
        if (drawable != null) {
            drawable.setAlpha(alpha);
        }
    }

    @Override
    public void setColorFilter(ColorFilter colorFilter) {
        Drawable drawable = getDrawable();
        if (drawable != null) {
            drawable.setColorFilter(colorFilter);
        }
    }

    @Override
    public int getOpacity() {
        Drawable drawable = getDrawable();
        return drawable != null
                ? drawable.getOpacity()
                : PixelFormat.UNKNOWN;
    }

    @Override
    public void draw(Canvas canvas) {
        Drawable drawable = getDrawable();
        if (drawable != null) {
            drawable.draw(canvas);
        }
    }

    @Override
    public int getIntrinsicWidth() {
        Drawable drawable = getDrawable();
        return drawable != null
                ? drawable.getBounds().width()
                : 0;
    }

    @Override
    public int getIntrinsicHeight() {
        Drawable drawable = getDrawable();
        return drawable != null ?
                drawable.getBounds().height()
                : 0;
    }
}
