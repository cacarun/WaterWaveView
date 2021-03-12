package com.cjw.waterwaveview.waterwave;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import com.cjw.waterwaveview.R;


public class WaterView extends View {

    private static final String TAG = "WaterView";

    private int mWidth;
    private int mHeight;
    private float centerX;
    private float centerY;

    private Path firstPath = new Path();
    private Paint firstPaint;
    private Path secondPath = new Path();
    private Paint secondPaint;

    private Paint bitmapPaint;
    private Bitmap cupBitmap;
    private Bitmap cupShadeBitmap;
    private Rect bitmapSrcRect, bitmapDestRect;

    private int firstPaintColor = Color.parseColor("#00E0FF");
    private int secondPaintColor = Color.parseColor("#8FF1FF");

    private int sin_amplitude = 6; //振幅 ，10到100之间
    private float sin_offset_increment_value = 0.4f; // 初项递增值，表示波浪的快慢
    private float sin_cycle = 0.025f; // 周期，0.01f左右
    private int sin_up_velocity = 9; // 上升速度，参考值3
    private int sleep_time = 100; // 休眠时间，参考值100

    private float max_goal_amount = 5f;
    private float per_amount = 0.25f;

    private int current_goal_amount_total_count = 0; // 最多上涨次数
    private int current_count = 0; // 当前上涨次数
    private int max_count = (int) (max_goal_amount / per_amount);

    private RunThread runThread;
    private boolean isStart = false;
    private boolean isStop = false;
    private boolean isKeepHeight = false;
    private boolean isUpdate = false;
    private boolean isReduce = false;

    float sin_offset = 0.0f; // 初项，偏移量，左右移动
    float h = 0f; // 上下移动

    private float stepHeight = 0f; // 每次喝水高度

    private float lastY;
    private float moveFactor = 3; // 滑动因子


    private WaterWaveListener waterWaveListener;

    public void setWaterWaveListener(WaterWaveListener waterWaveListener) {
        this.waterWaveListener = waterWaveListener;
    }

    public interface WaterWaveListener {
        void updateCurrentAmount(float currentAmount);
    }

    public WaterView(Context context) {
        this(context, null);
    }

    public WaterView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public WaterView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.WaterView);
        firstPaintColor = typedArray.getColor(R.styleable.WaterView_waterview_paint_color_first, firstPaintColor);
        secondPaintColor = typedArray.getColor(R.styleable.WaterView_waterview_paint_color_second, secondPaintColor);
        sin_amplitude = typedArray.getInt(R.styleable.WaterView_waterview_amplitude, sin_amplitude);
        sin_offset_increment_value = typedArray.getFloat(R.styleable.WaterView_waterview_offset_increment_value, sin_offset_increment_value);
        sin_cycle = typedArray.getFloat(R.styleable.WaterView_waterview_sin_cycle, sin_cycle);
        sin_up_velocity = typedArray.getInt(R.styleable.WaterView_waterview_up_velocity, sin_up_velocity);
        sleep_time = typedArray.getInt(R.styleable.WaterView_waterview_sleep_time, sleep_time);

        typedArray.recycle();

        firstPaint = new Paint();
        firstPaint.setAntiAlias(true);

        secondPaint = new Paint();
        secondPaint.setColor(secondPaintColor);
        secondPaint.setAntiAlias(true);

        bitmapPaint = new Paint();
        bitmapPaint.setDither(true);
        bitmapPaint.setFilterBitmap(true);
        bitmapPaint.setAntiAlias(true);

        cupBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.common_cup);
        cupShadeBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.common_cup_shade);
    }


    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        this.h = mHeight;

        updateStepHeight();

        // 水渐变
        firstPaint.setShader(new LinearGradient(mWidth / 2f, 0, mWidth / 2f, mHeight,
                firstPaintColor, secondPaintColor, Shader.TileMode.CLAMP));

        // 水杯位置
        bitmapSrcRect = new Rect(0, 0, cupBitmap.getWidth(), cupBitmap.getHeight());
        bitmapDestRect = new Rect(0, 0, mWidth, mHeight);
    }

    @Override
    protected void onDraw(Canvas canvas) {
//        Log.d(TAG, "onDraw, isStart=" + isStart);

        canvas.drawBitmap(cupShadeBitmap, bitmapSrcRect, bitmapDestRect, bitmapPaint);

        if (isStart) {
            canvas.drawPath(secondPath(), secondPaint);
            canvas.drawPath(firstPath(), firstPaint);
        }

        canvas.drawBitmap(cupBitmap, bitmapSrcRect, bitmapDestRect, bitmapPaint);
    }

    //y = Asin(wx+b)+h ，这个公式里：w影响周期，A影响振幅，h影响y位置，b为初相；
    private Path firstPath() {
        firstPath.reset();
        firstPath.moveTo(0, mHeight);// 移动到左下角的点

        for (float x = 0; x <= mWidth; x++) {
            float y = (float) (sin_amplitude * Math.sin(sin_cycle * x + sin_offset + 35)) + h;
            firstPath.lineTo(x, y);
        }
        firstPath.lineTo(mWidth, mHeight);
        firstPath.lineTo(0, mHeight);
        firstPath.close();
        return firstPath;
    }

    private Path secondPath() {
        secondPath.reset();
        secondPath.moveTo(0, mHeight);// 移动到左下角的点

        for (float x = 0; x <= mWidth; x++) {
            float y = (float) (sin_amplitude * Math.sin(sin_cycle * x + sin_offset)) + h;
            secondPath.lineTo(x, y);
        }
        secondPath.lineTo(mWidth, mHeight);
        secondPath.lineTo(0, mHeight);
        secondPath.close();
        return secondPath;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        /**
         * 设置宽度
         * 单位 px
         */
        int specMode = MeasureSpec.getMode(widthMeasureSpec);
        int specSize = MeasureSpec.getSize(widthMeasureSpec);

        if (specMode == MeasureSpec.EXACTLY)// match_parent , accurate
        {
            mWidth = specSize;
        } else {
            mWidth = dip2px(getContext(), 72);
        }

        /***
         * 设置高度
         */
        specMode = MeasureSpec.getMode(heightMeasureSpec);
        specSize = MeasureSpec.getSize(heightMeasureSpec);
        if (specMode == MeasureSpec.EXACTLY)// match_parent , accurate
        {
            mHeight = specSize;
        } else {
            mHeight = dip2px(getContext(), 128);
        }
        centerX = mWidth / 2f;
        centerY = mHeight / 2f;

        Log.d(TAG, "onMeasure, mWidth=" + mWidth + " mHeight=" + mHeight);

        setMeasuredDimension(mWidth, mHeight);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (stepHeight == 0) {
            return false;
        }

        float y = event.getY();

        switch (event.getAction()) {

            case MotionEvent.ACTION_DOWN:

                lastY = y;
                break;
            case MotionEvent.ACTION_MOVE:

                float offsetY = y - lastY;
                offsetY *= moveFactor;
                if (offsetY > 0) {

                    // 向下
                    int moveCount = (int)(offsetY / stepHeight);
                    int updateCurrentCount = current_count - moveCount;

                    Log.d(TAG, "onTouchEvent move down moveCount=" + moveCount
                            + " current_count=" + current_count
                            + " total_count=" + current_goal_amount_total_count
                            + " updateCurrentCount=" + updateCurrentCount);

                    if (moveCount > 0 && updateCurrentCount >= 0) {

                        update(updateCurrentCount, current_goal_amount_total_count);
                        lastY = y;

                        updateCurrentAmount();
                    }
                } else if (offsetY < 0) {

                    // 向上
                    offsetY = -offsetY;
                    int moveCount = (int)(offsetY / stepHeight);
                    int updateCurrentCount = current_count + moveCount;

                    Log.d(TAG, "onTouchEvent move up moveCount=" + moveCount
                            + " current_count=" + current_count
                            + " total_count=" + current_goal_amount_total_count
                            + " updateCurrentCount=" + updateCurrentCount);

                    if (moveCount > 0 && updateCurrentCount <= max_count) {

                        update(updateCurrentCount, current_goal_amount_total_count);
                        lastY = y;

                        updateCurrentAmount();
                    }
                }
                break;
        }

        return true;
    }

    public void reset() {
        if (runThread != null) {
            runThread = null;
        }
        isStart = false;
        isStop = false;
        isKeepHeight = false;
        isReduce = false;
        isUpdate = false;
        h = mHeight;
        sin_offset = 0;
        current_count = 0;
        invalidate();
    }

    public void start() {
        if (!isStart) {
            isStart = true;
            runThread = new RunThread();
            runThread.start();
        }
    }

    public void stop() {
        isStop = true;
        isKeepHeight = false;
    }

    public void recover() {
        isStop = false;
    }

    public void keepHeight() {
        isStop = true;
        isKeepHeight = true;
    }

    public void increase() {
        isReduce = false;
        isUpdate = false;

        if (current_count < max_count) {
            current_count++;

            Log.d(TAG, "[increase] current_count=" + current_count + " total_count=" + current_goal_amount_total_count);

            recover();

            updateCurrentAmount();
        }

        if (!isStart) {
            isStart = true;
            runThread = new RunThread();
            runThread.start();
        }
    }

    public void reduce() {
        isReduce = true;
        isUpdate = false;

        if (current_count > 0) {
            current_count--;

            Log.d(TAG, "[reduce] current_count=" + current_count + " total_count=" + current_goal_amount_total_count);

            if (current_count <= 0) {
                reset();
            } else {
                recover();
            }

            updateCurrentAmount();
        }
    }

    public void update(float currentAmount, float currentGoalAmount) {
        post(() -> {

            Log.d(TAG, "[update] currentAmount=" + currentAmount + " currentGoalAmount=" + currentGoalAmount);

            if (currentAmount < 0 || currentAmount > currentGoalAmount) {
                return;
            }

            int currentCount = (int) (currentAmount / per_amount);
            int totalCount = (int) (currentGoalAmount / per_amount);

            update(currentCount, totalCount);
        });
    }

    private void update(int currentCount, int totalCount) {
        Log.d(TAG, "update currentCount=" + currentCount + " totalCount=" + totalCount);

        isUpdate = true;

        current_count = currentCount;
        current_goal_amount_total_count = totalCount;

        updateStepHeight();

        recover();

        start();
    }

    private class RunThread extends Thread {

        @Override
        public void run() {
            while (isStart) {

                if (isStop) {
                    if (isKeepHeight) {
                        try {

//                            Log.d(TAG, "keep height");

                            Thread.sleep(sleep_time);
                            sin_offset += sin_offset_increment_value;
                            postInvalidate();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    continue;
                }
                try {

                    if (isUpdate) {

                        h = mHeight - getTargetHeight();
                        keepHeight();
                    } else if (isReduce) {

                        Thread.sleep(sleep_time);
                        h += sin_up_velocity;
                        sin_offset += sin_offset_increment_value;
                        postInvalidate();

                        float goHeight = mHeight - h;
                        float targetHeight = getTargetHeight();
                        Log.d(TAG, "isReduce goHeight=" + goHeight + " targetHeight=" + targetHeight);

                        if (goHeight < targetHeight) {
                            keepHeight();
                        }
                    } else {

                        Log.d(TAG, "mHeight=" + mHeight + " h=" + h);

                        Thread.sleep(sleep_time);
                        h -= sin_up_velocity;
                        sin_offset += sin_offset_increment_value;
                        postInvalidate();

                        float goHeight = mHeight - h;
                        float targetHeight = getTargetHeight();
                        Log.d(TAG, "goHeight=" + goHeight + " targetHeight=" + targetHeight);

                        if (goHeight > targetHeight) {
                            keepHeight();
                        }
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void updateStepHeight() {
        stepHeight = mHeight * 1f / current_goal_amount_total_count;
    }

    private float getTargetHeight() {
        return current_count * stepHeight;
    }

    private void updateCurrentAmount() {
        if (waterWaveListener != null) {
            waterWaveListener.updateCurrentAmount(current_count * per_amount);
        }
    }

    private int dip2px(Context context, float dpValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }

}