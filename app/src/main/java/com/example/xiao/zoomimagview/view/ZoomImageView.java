package com.example.xiao.zoomimagview.view;

import android.content.Context;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewTreeObserver;
import android.widget.ImageView;

/**
 * 全局的布局完成后会调用这个方法 OnGlobalLayoutListener
 * Created by xiao on 2017/3/2.
 */

public class ZoomImageView extends ImageView implements ViewTreeObserver.OnGlobalLayoutListener,
        ScaleGestureDetector.OnScaleGestureListener, View.OnTouchListener {

    private Context mContext;

    private boolean mOnce = false;
    /**
     * 初始化的缩放值
     */
    private float mInitScale = 1.0f;
    /**
     * 双击放大的值
     */
    private float mMidScale;

    /**
     * 放大的最大值
     */
    private float mMaxScale;

    private Matrix mScaleMatrix;

    /**
     * 捕获用户多点触控时缩放的比例
     */
    private ScaleGestureDetector mScaleGesture;


    //--------------------自由移动-------------------------

    private int mLastPointerCount;//记录上一次多点触控的数量

    private float mLastX;
    private float mLastY;

    private int mTouchSlop;
    private boolean isCanDrag;
    private boolean isCheckLeftAndRight;
    private boolean isCheckTopAndBottom;

    //-----------------------------双击放大和缩小-------------------
    private GestureDetector mGestureDetector;
    private boolean isAutoScale;

    public ZoomImageView(Context context) {
        this(context, null);

    }

    public ZoomImageView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ZoomImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mContext = context;
        initView();

    }

    private void initView() {
        setScaleType(ScaleType.MATRIX); // 设置后可以进行缩放
        mScaleMatrix = new Matrix();
        mScaleGesture = new ScaleGestureDetector(mContext, this);
        setOnTouchListener(this);
        mTouchSlop = ViewConfiguration.get(mContext).getScaledTouchSlop();

        mGestureDetector = new GestureDetector(mContext, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDoubleTap(MotionEvent e) {

                if (isAutoScale) return true;

                float x = e.getX();
                float y = e.getY();
                if (getScale() < mMidScale) {
//                    mScaleMatrix.postScale(mMidScale / getScale(), mMidScale / getScale(), x, y);
//                    checkBorderAndCenterWhenScale();
//                    setImageMatrix(mScaleMatrix);
                    postDelayed(new AutoScaleRunnable(mMidScale, x, y), 16);
                    isAutoScale = true;
                } else {
//                    mScaleMatrix.postScale(mInitScale / getScale(), mInitScale / getScale(), x, y);
//                    checkBorderAndCenterWhenScale();
//                    setImageMatrix(mScaleMatrix);

                    postDelayed(new AutoScaleRunnable(mInitScale, x, y), 16);
                    isAutoScale = true;
                }

                return true;
            }
        });
    }

    /**
     * 缓慢缩放的Runnable
     */
    private class AutoScaleRunnable implements Runnable {
        private float mTargetScale;//缩放的目标值
        private float x;//缩放的中心点
        private float y;//

        //缩放的梯度值
        private final float BIGGER = 1.07f;
        private final float SMALL = 0.93f;

        private float tempScale;

        public AutoScaleRunnable(float targetScale, float centerX, float centerY) {
            mTargetScale = targetScale;
            x = centerX;
            y = centerY;

            if (getScale() < mTargetScale) {
                tempScale = BIGGER;
            }
            if (getScale() > mTargetScale) {
                tempScale = SMALL;
            }
        }

        @Override
        public void run() {

            mScaleMatrix.postScale(tempScale, tempScale, x, y);
            checkBorderAndCenterWhenScale();
            setImageMatrix(mScaleMatrix);

            float currentScale = getScale();
            if ((tempScale > 1.0f && currentScale < mTargetScale)
                    || (tempScale < 1.0f && currentScale > mTargetScale)) {
                postDelayed(this, 16);
            } else {
                float scale = mTargetScale / currentScale;
                mScaleMatrix.postScale(scale, scale, x, y);
                checkBorderAndCenterWhenScale();
                setImageMatrix(mScaleMatrix);

                isAutoScale = false;
            }

        }
    }


    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        getViewTreeObserver().addOnGlobalLayoutListener(this);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        getViewTreeObserver().removeOnGlobalLayoutListener(this);
    }

    /**
     * 获取ImageView加载完成的图片的大小
     */
    @Override
    public void onGlobalLayout() {
        if (!mOnce) {

            //得到控件的宽和高
            int width = getWidth();
            int height = getHeight();
            //得到图片，以及宽和高
            Drawable drawable = getDrawable();
            if (drawable == null) {
                return;
            }
            int dWidth = drawable.getIntrinsicWidth();
            int dHeight = drawable.getIntrinsicHeight();

            float scale = 1.0f;
            if (dWidth > width && dHeight < height) {
                scale = 1.0f * width / dWidth;
            }

            if (dWidth < width && dHeight > height) {
                scale = 1.0f * height / dHeight;
            }

            if ((dWidth > width && dHeight > height) || (dWidth < width && dHeight < height)) {
                scale = Math.min(1.0f * width / dWidth, 1.0f * height / dHeight);
            }
            /**
             * 获得初始化缩放的比例
             */
            mInitScale = scale;
            mMaxScale = scale * 4;
            mMidScale = scale * 2;

            /**
             * 将图片移动到当前控件的中心
             */
            int dx = width / 2 - dWidth / 2;
            int dy = height / 2 - dHeight / 2;


            mScaleMatrix.postTranslate(dx, dy);
            mScaleMatrix.postScale(mInitScale, mInitScale, width / 2, height / 2);

            setImageMatrix(mScaleMatrix);

            mOnce = true;

        }
    }

    /**
     * 获取当前图片的缩放值
     *
     * @return
     */
    public float getScale() {
        float[] values = new float[9];
        mScaleMatrix.getValues(values);
        return values[Matrix.MSCALE_X];
    }


    @Override
    public boolean onScaleBegin(ScaleGestureDetector detector) {
        return true;
    }

    @Override
    public boolean onScale(ScaleGestureDetector detector) {

        float scale = getScale();
        float scaleFactor = detector.getScaleFactor();
        if (getDrawable() == null) {
            return true;
        }

        float newScale = scale * scaleFactor;
        if ((scale < mMaxScale && scaleFactor > 1.0f)
                || (scale > mInitScale && scaleFactor < 1.0f)) {
            if (newScale < mInitScale) {
                scaleFactor = mInitScale / scale;
            }
            if (newScale > mMaxScale) {
                scaleFactor = mMaxScale / scale;
            }

            mScaleMatrix.postScale(scaleFactor, scaleFactor, detector.getFocusX(), detector.getFocusY());

            checkBorderAndCenterWhenScale();

            setImageMatrix(mScaleMatrix);
        }


        return true;
    }

    /**
     * 获得图片放大缩放后的宽和高，left,right,bottom,top
     *
     * @return
     */
    private RectF getMatrixRectF() {
        Matrix matrix = mScaleMatrix;
        RectF rectF = new RectF();
        Drawable drawable = getDrawable();
        if (drawable == null) {
            return null;
        }
        rectF.set(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
        matrix.mapRect(rectF);
        return rectF;
    }

    /**
     * 在缩放时进行边界和位置的控制
     */
    private void checkBorderAndCenterWhenScale() {
        RectF rectF = getMatrixRectF();
        float dx = 0;
        float dy = 0;

        float width = getWidth();
        float height = getHeight();
        if (rectF.width() >= width) {
            if (rectF.left > 0) {
                dx = -rectF.left;
            }
            if (rectF.right < width) {
                dx = width - rectF.right;
            }
        }

        if (rectF.height() >= height) {
            if (rectF.top > 0) {
                dy = -rectF.top;
            }
            if (rectF.bottom < height) {
                dy = height - rectF.bottom;
            }
        }
        //如果宽度和高度小于控件的宽度和高度
        if (rectF.width() < width) {
            dx = width / 2f - rectF.right + rectF.width() / 2f;
        }
        if (rectF.height() < height) {
            dy = height / 2f - rectF.bottom + rectF.height() / 2f;
        }

        mScaleMatrix.postTranslate(dx, dy);


    }

    @Override
    public void onScaleEnd(ScaleGestureDetector detector) {

    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (mGestureDetector.onTouchEvent(event)) {
            return true;
        }

        mScaleGesture.onTouchEvent(event);

        float x = 0;
        float y = 0;
        int pointerCount = event.getPointerCount();
        for (int i = 0; i < pointerCount; i++) {
            x += event.getX(i);
            y += event.getY(i);
        }

        x /= pointerCount;
        y /= pointerCount;
        if (mLastPointerCount != pointerCount) {
            isCanDrag = false;
            mLastX = x;
            mLastY = y;

        }
        mLastPointerCount = pointerCount;
        RectF rect = getMatrixRectF();
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (rect.width() > getWidth() + 0.01 || rect.height() > getHeight() + 0.01) {
                    if (getParent() instanceof ViewPager)
                        getParent().requestDisallowInterceptTouchEvent(true);//请求父控件不拦截事件
                }


                break;

            case MotionEvent.ACTION_MOVE:
                if (rect.width() > getWidth() + 0.01 || rect.height() > getHeight() + 0.01) {
                    if (getParent() instanceof ViewPager)
                        getParent().requestDisallowInterceptTouchEvent(true);//请求父控件不拦截事件
                }
                float dx = x - mLastX;
                float dy = y - mLastY;
                if (!isCanDrag) {
                    isCanDrag = isMoveAction(dx, dy);
                }
                if (isCanDrag) {
                    RectF rectf = getMatrixRectF();
                    if (getDrawable() != null) {
                        isCheckLeftAndRight = isCheckTopAndBottom = true;
                        //如果图片的宽度小于控件宽度，不允许横向移动
                        if (rectf.width() < getWidth()) {
                            isCheckLeftAndRight = false;
                            dx = 0;
                        }
                        //如果图片的高度小于控件高度，不允许纵向移动
                        if (rectf.height() < getHeight()) {
                            isCheckTopAndBottom = false;
                            dy = 0;
                        }

                        mScaleMatrix.postTranslate(dx, dy);
                        checkBorderWhenTranslate();
                        setImageMatrix(mScaleMatrix);
                    }

                }
                mLastX = x;
                mLastY = y;
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                mLastPointerCount = 0;
                break;
        }


        return true;
    }

    /**
     * 当移动的时候检测边界
     */
    private void checkBorderWhenTranslate() {

        RectF rectf = getMatrixRectF();
        float dx = 0;
        float dy = 0;
        int width = getWidth();
        int height = getHeight();
        if (rectf.top > 0 && isCheckTopAndBottom) {
            dy = -rectf.top;
        }
        if (rectf.bottom < height && isCheckTopAndBottom) {
            dy = height - rectf.bottom;
        }
        if (rectf.left > 0 && isCheckLeftAndRight) {
            dx = -rectf.left;
        }
        if (rectf.right < width && isCheckLeftAndRight) {
            dx = width - rectf.right;
        }

        mScaleMatrix.postTranslate(dx, dy);


    }

    /**
     * 是否足以移动
     *
     * @param dx
     * @param dy
     * @return
     */
    private boolean isMoveAction(float dx, float dy) {

        return Math.sqrt(dx * dx + dy * dy) > mTouchSlop;
    }


}
