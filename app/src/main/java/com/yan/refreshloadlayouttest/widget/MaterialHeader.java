package com.yan.refreshloadlayouttest.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.Transformation;

import com.yan.pullrefreshlayout.PullRefreshLayout;

public class MaterialHeader extends View implements PullRefreshLayout.OnPullListener {

    private MaterialProgressDrawable mDrawable;
    private float mScale = 1f;
    private boolean isHolding = false;
    private float multiple;
    private PullRefreshLayout refreshLayout;

    private Animation mScaleAnimation = new Animation() {
        @Override
        public void applyTransformation(float interpolatedTime, Transformation t) {
            mScale = 1f - interpolatedTime;
            mDrawable.setAlpha((int) (255 * mScale));
            if (mScale == 0) {
                setTranslationY(0);
                refreshLayout.setMoveWithHeader(true);
            }
            invalidate();
        }
    };

    public MaterialHeader(Context context) {
        super(context);
        initView();
    }

    public MaterialHeader(Context context, PullRefreshLayout refreshLayout, float multiple) {
        super(context);
        this.multiple = multiple;
        this.refreshLayout = refreshLayout;
        initView();
    }

    private void initView() {
        mDrawable = new MaterialProgressDrawable(getContext(), this);
        mDrawable.setBackgroundColor(Color.WHITE);
        mDrawable.setCallback(this);
        mScaleAnimation.setDuration(200);
    }

    @Override
    public void invalidateDrawable(Drawable dr) {
        if (dr == mDrawable) {
            invalidate();
        } else {
            super.invalidateDrawable(dr);
        }
    }

    public void setColorSchemeColors(int[] colors) {
        mDrawable.setColorSchemeColors(colors);
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int height = mDrawable.getIntrinsicHeight() + getPaddingTop() + getPaddingBottom();
        heightMeasureSpec = MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY);
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        final int size = mDrawable.getIntrinsicHeight();
        mDrawable.setBounds(0, 0, size, size);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        final int saveCount = canvas.save();
        Rect rect = mDrawable.getBounds();
        int l = getPaddingLeft() + (getMeasuredWidth() - mDrawable.getIntrinsicWidth()) / 2;
        canvas.translate(l, getPaddingTop());
        canvas.scale(mScale, mScale, rect.exactCenterX(), rect.exactCenterY());
        mDrawable.draw(canvas);
        canvas.restoreToCount(saveCount);
    }

    @Override
    public void onPullChange(float percent) {
        if (isHolding) return;
        percent = Math.abs(percent / multiple);

        mDrawable.setAlpha((int) (percent * 255));
        mDrawable.showArrow(true);
        float strokeStart = ((percent) * .8f);
        mDrawable.setStartEndTrim(0f, Math.min(0.8f, strokeStart));
        mDrawable.setArrowScale(Math.min(1f, percent));

        // magic
        float rotation = (-0.25f + .4f * percent + percent * 2) * .5f;
        mDrawable.setProgressRotation(rotation);
        invalidate();
    }

    @Override
    public void onPullHoldTrigger() {

    }

    @Override
    public void onPullHoldUnTrigger() {

    }

    @Override
    public void onPullHolding() {
        isHolding = true;
        mDrawable.setAlpha(255);
        mDrawable.start();
    }

    @Override
    public void onPullFinish() {
        mDrawable.stop();
        refreshLayout.setMoveWithHeader(false);
        startAnimation(mScaleAnimation);
    }

    @Override
    public void onPullReset() {
        isHolding = false;
        mScale = 1f;
        mDrawable.stop();
        refreshLayout.setMoveWithHeader(true);
    }
}