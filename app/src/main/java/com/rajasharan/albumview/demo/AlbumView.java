/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2015 Raja Sharan Mamidala
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.rajasharan.albumview.demo;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Camera;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.RectF;
import android.graphics.Region;
import android.os.Environment;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;

/**
 * Created by rajasharan on 9/18/15.
 */
public class AlbumView extends View {
    private static final String TAG = "AlbumView-TAG";
    private static final float FULL_ANGLE = 180.0f;
    private static final float FLIP_ANGLE = 70.0f;
    private static final float REVERSE_FLIP_ANGLE = FLIP_ANGLE - 180.0f;
    private static float MAX_WIDTH;

    private Path mPath;
    private RectF mFrameRect;
    private Matrix mMatrix;
    private Camera mCamera;
    private float mAngle;
    private float mAnglePrev;
    private PointF mStartTouch;
    private float mSwipeDistance;
    private Bitmap mBitmap;
    private boolean mViewFlipped;

    public AlbumView(Context context) {
        this(context, null, 0);
    }

    public AlbumView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AlbumView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Point screen = new Point();
        wm.getDefaultDisplay().getSize(screen);
        MAX_WIDTH = screen.x;

        mPath = new Path();
        mPath.setFillType(Path.FillType.EVEN_ODD);

        mMatrix = new Matrix();
        mCamera = new Camera();

        mAngle = 0.0f;
        mAnglePrev = 0.0f;

        //String path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) + "/template/loc1/bg3.jpg";
        String path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) + "/Screenshots/test.png";
        mBitmap = createBitmap(path, screen);

        mViewFlipped = false;
    }

    private Bitmap createBitmap(String path, Point screen) {
        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, opts);
        int imgWidth = opts.outWidth;
        int imgHeight = opts.outHeight;

        opts.inJustDecodeBounds = false;
        opts.inSampleSize = calculateSampleSize(imgWidth, imgHeight, screen.x, screen.y);
        return BitmapFactory.decodeFile(path, opts);
    }

    private int calculateSampleSize(int imgWidth, int imgHeight, int screenWidth, int screenHeight) {
        int inSampleSize = 1;
        int imgSize = imgWidth > imgHeight? imgWidth: imgHeight;
        int screenSize = imgWidth > imgHeight? screenWidth: screenHeight;
        while (imgSize / inSampleSize > screenSize) {
            inSampleSize = inSampleSize * 2;
        }
        return inSampleSize;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int w = MeasureSpec.getSize(widthMeasureSpec);
        int h = MeasureSpec.getSize(heightMeasureSpec);

        widthMeasureSpec = MeasureSpec.makeMeasureSpec(w, MeasureSpec.EXACTLY);
        heightMeasureSpec = MeasureSpec.makeMeasureSpec(h, MeasureSpec.EXACTLY);

        setMeasuredDimension(widthMeasureSpec, heightMeasureSpec);

        if (mFrameRect == null) {
            mFrameRect = new RectF(20, 20, getMeasuredWidth()-20, getMeasuredHeight()-20);

            RectF src = new RectF(0, 0, mBitmap.getWidth(), mBitmap.getHeight());
            RectF dst = new RectF(0, 0, mFrameRect.width(), mFrameRect.height());
            Matrix matrix = new Matrix();
            matrix.setRectToRect(src, dst, Matrix.ScaleToFit.CENTER);
            mBitmap = Bitmap.createBitmap(mBitmap, 0, 0, mBitmap.getWidth(), mBitmap.getHeight(), matrix, true);

            /*Bitmap scaledBitmap = Bitmap.createBitmap((int)mFrameRect.width(), (int)mFrameRect.height(), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(scaledBitmap);
            float aspectRatio = (mBitmap.getWidth() * 1.0f) / (mBitmap.getHeight() * 1.0f);

            Rect dst = new Rect(0, 0, (int)(mFrameRect.height() * aspectRatio), (int)mFrameRect.height());
            canvas.drawBitmap(mBitmap, null, dst, new Paint(Paint.FILTER_BITMAP_FLAG));
            mBitmap = scaledBitmap;*/
        }
    }

    private void rotate() {
        mAngle = mAnglePrev + (FULL_ANGLE * mSwipeDistance ) / MAX_WIDTH;

        if (mAngle > FLIP_ANGLE) {
            mAnglePrev = REVERSE_FLIP_ANGLE;
            mAngle = REVERSE_FLIP_ANGLE;
            mViewFlipped = true;
        }
        else if (mAngle < REVERSE_FLIP_ANGLE) {
            mAnglePrev = FLIP_ANGLE;
            mAngle = FLIP_ANGLE;
            mViewFlipped = true;
        }
        //Log.d(TAG, String.format("rotate: %s", mAngle));

        mCamera.save();
        mCamera.translate(getWidth() / 2, 0, 0);
        mCamera.rotateY(mAngle);
        mCamera.translate(-getWidth() / 2, 0, 0);
        mCamera.getMatrix(mMatrix);
        mCamera.restore();
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        mPath.rewind();
        mPath.addRoundRect(mFrameRect, 15, 15, Path.Direction.CW);

        canvas.save();
        canvas.concat(mMatrix);
        canvas.clipPath(mPath, Region.Op.INTERSECT);
        canvas.drawBitmap(mBitmap, mFrameRect.left, mFrameRect.top, null);
        canvas.restore();

    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                startTouch(x, y);
                break;
            case MotionEvent.ACTION_MOVE:
                if (mViewFlipped) {
                    startTouch(x, y);
                    mViewFlipped = false;
                }
                mSwipeDistance = x - mStartTouch.x;
                rotate();
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                endTouch();
                break;
        }
        return true;
    }

    private void startTouch(float x, float y) {
        if (mStartTouch == null) {
            mStartTouch = new PointF(x, y);
        }
        else {
            mStartTouch.set(x, y);
        }
    }

    private void endTouch() {
        mAnglePrev = mAngle;
    }
}
