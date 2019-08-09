package com.qianyue.circlerviewlib;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.RectF;
import android.graphics.SweepGradient;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;

import java.math.BigDecimal;

public class YQCircleView extends View {

    /**
     * 前景画笔
     */
    private Paint frontPaint;

    /**
     * 背景画笔
     */
    private Paint bgPaint;

    /**
     * 前景画笔 渐变颜色集合
     */
    private int frontColors[];

    /**
     * view的宽
     */
    private int viewWidth;

    /**
     * view的高
     */
    private int viewHeight;

    /**
     * 半径的x坐标
     */
    private int radiusCx;

    /**
     * 半径的Y坐标
     */
    private int radiusCy;

    /**
     * 画笔的宽度
     */
    private int paintWidth;

    /**
     * 半径
     */
    private int radius;

    /**
     * 半径范围 min
     */
    private int radiusMin;

    /**
     * 使用双缓冲
     */
    private Bitmap bitmap;

    /**
     * 双缓冲的画板
     */
    private Canvas canvas;

    /**
     * 当前滑动的角度
     */
    private float currentAngle;

    /**
     * 老的滑动角度
     */
    private float oldAngle;

    /**
     * 是否移动
     */
    private boolean isMove;

    /**
     * 刻度监听
     */
    private OnScaleListener listener;

    /**
     * 刻度
     */
    private int scale;

    /**
     * 最大范围
     */
    private int max;

    /**
     * 平均值
     */
    private float average;


    public YQCircleView(Context context) {
        super(context);
    }

    public YQCircleView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public YQCircleView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.init(context, attrs);
    }

    /**
     * 初始化 画笔等工具
     */
    private void init(Context context, @Nullable AttributeSet attrs) {
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.YQCircleView);
        int startFrontColor = typedArray.getColor(R.styleable.YQCircleView_cv_start_color, Color.parseColor("#F5EF72"));
        int endFrontColor = typedArray.getColor(R.styleable.YQCircleView_cv_end_color, Color.parseColor("#FEFEF6"));
        paintWidth = typedArray.getInteger(R.styleable.YQCircleView_cv_paint_width, 50);
        int bgPaintColor = typedArray.getColor(R.styleable.YQCircleView_cv_bg_color, Color.parseColor("#80888888"));
        max = typedArray.getInteger(R.styleable.YQCircleView_cv_max, 10);
        average = 180.0f / max;
        typedArray.recycle();

        // 需禁用硬件加速
        setLayerType(LAYER_TYPE_SOFTWARE, null);

        // 颜色集合
        frontColors = new int[]{startFrontColor, endFrontColor, startFrontColor};

        // 前景画笔初始化
        frontPaint = new Paint();
        // 画笔宽度
        frontPaint.setStrokeWidth(paintWidth);
        // 笔触风格
        frontPaint.setStrokeCap(Paint.Cap.ROUND);
        // 抗锯齿
        frontPaint.setAntiAlias(true);
        // 设置防抖
        frontPaint.setDither(true);
        // 画笔样式
        frontPaint.setStyle(Paint.Style.STROKE);
        // 发光特效
        paintBeam(frontPaint, 10f);

        // ------------------- bg paint -------------------- //

        // 背景画笔初始化
        bgPaint = new Paint();
        // 画笔宽度
        bgPaint.setStrokeWidth(paintWidth);
        // 笔触风格
        bgPaint.setStrokeCap(Paint.Cap.ROUND);
        // 抗锯齿
        bgPaint.setAntiAlias(true);
        // 设置防抖
        bgPaint.setDither(true);
        // 画笔样式
        bgPaint.setStyle(Paint.Style.STROKE);
        // 画笔颜色
        bgPaint.setColor(bgPaintColor);
        // 发光特效
        paintBeam(bgPaint, 10f);

    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        this.viewWidth = w;
        this.viewHeight = h;
        this.radiusCx = w / 2;
        this.radiusCy = h / 2;
        this.radius = Math.min(radiusCx, radiusCy);
        this.radiusMin = radius - paintWidth;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        // 初始化画板
        if (bitmap == null) {
            bitmap = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
            this.canvas = new Canvas(bitmap);
            drawBgArc(canvas, 180, 180);
            drawFrontArc(canvas, 180, average);
        }
        canvas.drawBitmap(bitmap, 0, 0, null);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getAction();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                touchRange(event.getX(), event.getY());
                break;
            case MotionEvent.ACTION_MOVE:
                if (isMove) {
                    canvas.drawColor(0, PorterDuff.Mode.CLEAR); // 画布清理
                    drawBgArc(canvas, 180, 180);
                    drawFrontArcRange(event.getX(), event.getY());
                    scale = scalesChange();
                    Log.v("scale==", "scale" + scale);
                    if (listener != null)
                        listener.onScaleChange(scale);
                    invalidate();
                }
                break;
            case MotionEvent.ACTION_UP:
                if (isMove) {
                    isMove = false;
                    canvas.drawColor(0, PorterDuff.Mode.CLEAR); // 画布清理
                    drawBgArc(canvas, 180, 180);
                    drawFrontArc(canvas, 180, scale >= max ? 180 : scale * average);
                    Log.v("result= 刻度值", scale * average + "");
                    if (listener != null) {
                        listener.onScaleChange(scale);
                        listener.onScaleFinish();
                    }
                    invalidate(); // 刷新布局
                }
                break;
            case MotionEvent.ACTION_CANCEL: // 事件 被上层拦截时触发。
                if (isMove) {
                    isMove = false;
                    canvas.drawColor(0, PorterDuff.Mode.CLEAR); // 画布清理
                    drawBgArc(canvas, 180, 180);
                    drawFrontArc(canvas, 180, scale >= max ? 180 : scale * average);
                    if (listener != null) {
                        listener.onScaleChange(scale);
                        listener.onScaleFinish();
                    }
                    invalidate(); // 刷新布局
                    Log.v("result= 取消触发", currentAngle + "");
                }

                break;
        }
        return true;
//        return super.onTouchEvent(event);
    }


    /**
     * ============================ 设置刻度 =========================
     */
    public void setScale(int scale) {
        if (bitmap == null) {
            bitmap = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
            this.canvas = new Canvas(bitmap);
        }
        canvas.drawColor(0, PorterDuff.Mode.CLEAR); // 画布清理
        drawBgArc(canvas, 180, 180);
        if (scale <= 0)
            scale = 1;
        if (scale > max)
            scale = 10;
        this.scale = scale;
        if (listener != null)
            listener.onScaleChange(this.scale);
        drawFrontArc(canvas, 180, scale == max ? 180 : scale * 18);
        invalidate();
    }

    /**
     * 滑动条颜色
     */
    public void setFrontColors(int[] colos) {
        frontColors = colos;
    }

    public void setOnScaleListener(OnScaleListener listener) {
        this.listener = listener;
    }

    /**
     * ================================ 画笔范围 ================================
     */
    protected void drawFrontArcRange(float cx, float cy) {
        oldAngle = currentAngle;
        currentAngle = calcAngle(cx, cy);
        Log.v("mmmmm", "角度=" + currentAngle);
        if (currentAngle < average)
            currentAngle = average;
        if (currentAngle > 270)
            currentAngle = average;
        if (currentAngle > 180)
            currentAngle = 180;
        Log.v("mmmmm", "角度2=" + currentAngle);
        drawFrontArc(canvas, 180, currentAngle);
    }

    /**
     * 刻度
     *
     * @return
     */
    protected int scalesChange() {
        BigDecimal one = new BigDecimal(currentAngle);
        BigDecimal two = new BigDecimal(average);
        int result = one.divide(two, 0, BigDecimal.ROUND_DOWN).intValue();
        Log.v("result=总=", "===" + result + "===" + currentAngle);
        if (oldAngle > currentAngle) {  // 往回退的
            Log.v("result=回退=", result + "");
            return result;
        } else if (oldAngle < currentAngle) { // 前进
            if (result < max) {
                Log.v("result=前进=", result + "");
                return result + 1;
            } else {
                Log.v("result=前进10=", result + "");
                return result;
            }
        } else {
            Log.v("result=相等=", result + "");
            return result;
        }
    }

    /**
     * ===================================   触摸部分 ==============================
     */

    /**
     * 判断手指是否在范围中滑动
     */
    protected void touchRange(float cx, float cy) {
        double mX = Math.pow((cx - radiusCx), 2); // x的y次方
        double mY = Math.pow((cy - radiusCy), 2);
        double dotTodot = Math.abs(Math.sqrt(mX + mY)); // 两点间的距离
        BigDecimal dot = new BigDecimal(dotTodot);
        BigDecimal min = new BigDecimal(radiusMin);
        BigDecimal max = new BigDecimal(radius);
        int one = dot.compareTo(min);
        int two = dot.compareTo(max);
        if (one != -1 && two != 1) { // 手指在范围中滑动
            isMove = true; // 可以滑动
            canvas.drawColor(0, PorterDuff.Mode.CLEAR); // 画布清理
            drawBgArc(canvas, 180, 180);
            drawFrontArcRange(cx, cy);
            scale = scalesChange();
            if (listener != null)
                listener.onScaleStart();
        } else {
            isMove = false; // 不可以滑动
        }
    }

    /**
     * 以圆心为坐标圆点，建立坐标系，求出(targetX, targetY)坐标与x轴的夹角
     *
     * @param targetX x坐标
     * @param targetY y坐标
     * @return (targetX, targetY)坐标与x轴的夹角
     * 1度=π/180≈0.01745弧度，1弧度=180/π≈57.3度。
     */
    private float calcAngle(float targetX, float targetY) {
        float x = targetX - radiusCx;
        float y = targetY - radiusCy;
        double radian;

        if (x != 0) {
            float tan = Math.abs(y / x);
            if (x > 0) {  // 1 or 2
                if (y >= 0) { // 2
                    radian = Math.PI + Math.atan(tan);
                } else { // 1
                    radian = Math.PI - Math.atan(tan);
                }
            } else { // 3 or 4
                if (y >= 0) { // 3
                    radian = 2 * Math.PI - Math.atan(tan);
                } else { // 4
                    radian = Math.atan(tan);
                }
            }
        } else {
            if (y > 0) {
                radian = Math.PI / 2;
            } else {
                radian = 2 * Math.PI - Math.PI / 2;
            }
        }
        return (float) ((radian * 180) / Math.PI);
    }


    /**
     * ============================================  画笔部分 =====================================
     */

    /**
     * 前景画笔圆弧 效果
     */
    protected void drawFrontArc(Canvas canvas, float startAngle, float sweepAngle) {
        int mRadius = radius - paintWidth;
        RectF rectF = new RectF();
        rectF.left = radiusCx - mRadius;
        rectF.top = radiusCy - mRadius;
        rectF.right = radiusCx + mRadius;
        rectF.bottom = radiusCy + mRadius;
        paintGradient(frontPaint, frontColors, radiusCx, radiusCy);
        canvas.drawArc(rectF, startAngle, sweepAngle, false, frontPaint);
    }


    /**
     * 背景画笔圆弧 效果
     */
    protected void drawBgArc(Canvas canvas, float startAngle, float sweepAngle) {
        int mRadius = radius - paintWidth;
        RectF rectF = new RectF();
        rectF.left = radiusCx - mRadius;
        rectF.top = radiusCy - mRadius;
        rectF.right = radiusCx + mRadius;
        rectF.bottom = radiusCy + mRadius;
        canvas.drawArc(rectF, startAngle, sweepAngle, false, bgPaint);
    }

    /**
     * SweepGradient 扫描/梯度/扇形渐变
     * 扇形渐变
     *
     * @param mPaint 画笔
     * @param colors 渐变色数组
     * @param cx     中心点x
     * @param cy     中心点y
     */
    protected void paintGradient(Paint mPaint, int[] colors, float cx, float cy) {
        SweepGradient sweepGradient = new SweepGradient(cx, cy, colors, null);
        mPaint.setShader(sweepGradient);
    }

    /**
     * blurMaskFilter 发光
     *
     * @param mPaint 画笔
     * @param radius he radius to extend the blur from the original mask. Must be > 0.
     */
    protected void paintBeam(Paint mPaint, float radius) {
        BlurMaskFilter blurMaskFilter = new BlurMaskFilter(radius, BlurMaskFilter.Blur.SOLID);
        mPaint.setMaskFilter(blurMaskFilter);
    }

}
