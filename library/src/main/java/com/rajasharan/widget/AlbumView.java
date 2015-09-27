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

package com.rajasharan.widget;

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
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.WindowManager;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by rajasharan on 9/18/15.
 */
public class AlbumView extends View implements Handler.Callback, ViewTreeObserver.OnGlobalLayoutListener {
    private static final String TAG = "AlbumView-TAG";
    private static final String THREAD_NAME = "ImageLoader-Thread";

    private static final int WHAT_LOAD_IMAGE = 0;
    private static final int WHAT_SHOW_IMAGE = 1;

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
    private Bitmap mBackupBitmap;
    private BitmapFactory.Options mOptions;
    private boolean mViewFlipped;
    private boolean mBitmapRequested;
    private Point mScreen;
    private List<String> mImagePaths;
    private int mCurrentIndex;

    private Handler mBackgroundHandler;
    private Handler mMainHandler;

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
        mScreen = new Point();
        wm.getDefaultDisplay().getSize(mScreen);
        MAX_WIDTH = mScreen.x;

        mPath = new Path();
        mPath.setFillType(Path.FillType.EVEN_ODD);

        mMatrix = new Matrix();
        mCamera = new Camera();

        mAngle = 0.0f;
        mAnglePrev = 0.0f;

        mOptions = new BitmapFactory.Options();
        mViewFlipped = false;
        mImagePaths = new ArrayList<>();
        mCurrentIndex = -1;
        mBitmapRequested = false;

        HandlerThread thread = new HandlerThread(THREAD_NAME);
        thread.start();

        Looper looper = thread.getLooper();
        mBackgroundHandler = new Handler(looper, this);
        mMainHandler = new Handler(Looper.getMainLooper(), this);
    }

    public AlbumView addImage(String filePath) {
        mImagePaths.add(filePath);
        return this;
    }

    public AlbumView addImages(String imageDir) {
        File dir = new File(imageDir);
        if (dir.isDirectory() && dir.exists()) {
            String[] files = dir.list(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String filename) {
                    if (filename.toLowerCase().contains("jpg") || filename.toLowerCase().contains("png")) {
                        return true;
                    }
                    return false;
                }
            });

            if (files != null) {
                for (String file : files) {
                    String img = dir + "/" + file;
                    mImagePaths.add(img);
                }
            }
        }
        return this;
    }

    public void show() {
        getViewTreeObserver().addOnGlobalLayoutListener(this);
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
        }
    }


    @Override
    public void onGlobalLayout() {
        Log.d(TAG, String.format("onGlobalLayout - mFrameRect: %s", mFrameRect));
        requestNextImage();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            getViewTreeObserver().removeOnGlobalLayoutListener(this);
        }
        else {
            getViewTreeObserver().removeGlobalOnLayoutListener(this);
        }
    }

    private void requestImage(boolean forward, float frameWidth, float frameHeight) {
        if (mImagePaths.size() == 0) {
            return;
        }
        if (!mBitmapRequested) {
            mCurrentIndex = forward? mCurrentIndex + 1 : mCurrentIndex - 1;
            mCurrentIndex = mCurrentIndex >= mImagePaths.size()? 0 : mCurrentIndex;
            mCurrentIndex = mCurrentIndex < 0? mImagePaths.size() - 1 : mCurrentIndex;
            Log.d(TAG, String.format("requestImage: index - %s", mCurrentIndex));

            Message msg = mBackgroundHandler.obtainMessage(WHAT_LOAD_IMAGE, (int) frameWidth, (int) frameHeight, mImagePaths.get(mCurrentIndex));
            mBackgroundHandler.sendMessage(msg);
            mBitmapRequested = true;
        }
    }

    private void requestNextImage() {
        requestImage(true, mFrameRect.width(), mFrameRect.height());
    }

    private void requestPreviousImage() {
        requestImage(false, mFrameRect.width(), mFrameRect.height());
    }

    private void rotate() {
        mAngle = mAnglePrev + (FULL_ANGLE * mSwipeDistance ) / MAX_WIDTH;

        if (mAngle > FLIP_ANGLE) {
            mAnglePrev = REVERSE_FLIP_ANGLE;
            mAngle = REVERSE_FLIP_ANGLE;
            mViewFlipped = true;
            flipBitmaps();
        }
        else if (mAngle < REVERSE_FLIP_ANGLE) {
            mAnglePrev = FLIP_ANGLE;
            mAngle = FLIP_ANGLE;
            mViewFlipped = true;
            flipBitmaps();
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

    private void flipBitmaps() {
        if (mBitmap != null && !mBitmap.isRecycled()) {
            mBitmap.recycle();
        }
        mBitmap = Bitmap.createBitmap(mBackupBitmap);
        invalidate();
        mBitmapRequested = false;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        //Log.d(TAG, String.format("onDraw - mBitmap: %s", mBitmap));
        if (mBitmap != null) {
            mPath.rewind();
            mPath.addRoundRect(mFrameRect, 15, 15, Path.Direction.CW);

            canvas.save();
            canvas.concat(mMatrix);
            canvas.clipPath(mPath, Region.Op.INTERSECT);
            canvas.drawBitmap(mBitmap, mFrameRect.left, mFrameRect.top, null);
            canvas.restore();
        }
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
                if (mSwipeDistance < 0) {
                    requestPreviousImage();
                }
                else {
                    requestNextImage();
                }
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

    @Override
    public boolean handleMessage(Message msg) {
        switch (msg.what) {
            /*
             * Runs on background thread
             */
            case WHAT_LOAD_IMAGE:
                //Log.d(TAG, String.format("Thread: %s, Msg: %s", Thread.currentThread().getName(), msg));
                String path = (String) msg.obj;
                int w = msg.arg1;
                int h = msg.arg2;
                Bitmap bitmap = createScaledBitmap(createBitmap(path), w, h);
                Message message = mMainHandler.obtainMessage(WHAT_SHOW_IMAGE, bitmap);
                mMainHandler.sendMessage(message);
                return true;

            /*
             * Runs on Main thread
             */
            case WHAT_SHOW_IMAGE:
                if (mBackupBitmap != null && !mBackupBitmap.isRecycled()) {
                    mBackupBitmap.recycle();
                }
                mBackupBitmap = (Bitmap) msg.obj;

                if (mBitmap == null) {
                    flipBitmaps();
                }
                return true;
        }
        return false;
    }

    private Bitmap createScaledBitmap(Bitmap bitmap, int frameWidth, int frameHeight) {
        if (bitmap != null) {
            RectF src = new RectF(0, 0, bitmap.getWidth(), bitmap.getHeight());
            RectF dst = new RectF(0, 0, frameWidth, frameHeight);
            Matrix matrix = new Matrix();
            matrix.setRectToRect(src, dst, Matrix.ScaleToFit.CENTER);
            Bitmap scaledBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);

            /*Bitmap scaledBitmap = Bitmap.createBitmap((int)mFrameRect.width(), (int)mFrameRect.height(), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(scaledBitmap);
            float aspectRatio = (mBitmap.getWidth() * 1.0f) / (mBitmap.getHeight() * 1.0f);

            Rect dst = new Rect(0, 0, (int)(mFrameRect.height() * aspectRatio), (int)mFrameRect.height());
            canvas.drawBitmap(mBitmap, null, dst, new Paint(Paint.FILTER_BITMAP_FLAG));
            mBitmap = scaledBitmap;*/

            return scaledBitmap;
        }
        return null;
    }

    private Bitmap createBitmap(String path) {
        mOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, mOptions);
        int imgWidth = mOptions.outWidth;
        int imgHeight = mOptions.outHeight;

        mOptions.inJustDecodeBounds = false;
        mOptions.inSampleSize = calculateSampleSize(imgWidth, imgHeight);
        Bitmap bitmap = BitmapFactory.decodeFile(path, mOptions);
        Log.d(TAG, String.format("createBitmap: %s, path: %s, %s", bitmap, path, Thread.currentThread().getName()));
        return bitmap;
    }

    private int calculateSampleSize(int imgWidth, int imgHeight) {
        int inSampleSize = 1;
        int imgSize = imgWidth > imgHeight? imgWidth: imgHeight;
        int screenSize = imgWidth > imgHeight? mScreen.x: mScreen.y;
        while (imgSize / inSampleSize > screenSize) {
            inSampleSize = inSampleSize * 2;
        }
        return inSampleSize;
    }
}
