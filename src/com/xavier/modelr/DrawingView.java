package com.xavier.modelr;

import android.view.View;
import android.view.View.MeasureSpec;
import android.view.MotionEvent;
import android.content.Context;
import android.util.AttributeSet;

import android.widget.ImageButton;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;

import android.util.Log;

public class DrawingView extends View {

  private int bitmapSize;

  private Bitmap bitmap;
  private Canvas canvas;
  private Paint paint;
  private Path path;

  private ImageButton activeButton;

  public DrawingView(Context context) {
    super(context);
    this.init();
  }

  public DrawingView(Context context, AttributeSet attrs) {
    super(context, attrs);
    this.init();
  }

  public DrawingView(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);
    this.init();
  }

  private void init() {
    this.path = new Path();
    this.paint = new Paint(Paint.DITHER_FLAG);
    this.paint.setAntiAlias(true);
    this.paint.setColor(Color.BLACK);
    this.paint.setStyle(Paint.Style.STROKE);
    this.paint.setStrokeJoin(Paint.Join.ROUND);
    this.paint.setStrokeCap(Paint.Cap.ROUND);
    this.paint.setStrokeWidth(20.0f);
  }

  public void setBitmap(Bitmap bitmap) {
    this.bitmap = bitmap;
    this.canvas.setBitmap(bitmap);
  }
  public Bitmap getBitmap() {
    return this.bitmap;
  }

  public void setActiveButton(ImageButton button) {
    this.activeButton = button;
  }
  public ImageButton getActiveButton() {
    return this.activeButton;
  }

  public void reset() {
    if(this.bitmap != null)
      this.bitmap.eraseColor(Color.WHITE);
    this.setBackgroundColor(Color.WHITE);
  }

  @Override
  protected void onMeasure(int wSpec, int hSpec) {
    int w = MeasureSpec.getSize(wSpec);
    int h = MeasureSpec.getSize(hSpec);
    int s = Math.min(w, h);
    this.setMeasuredDimension(s, s);
  }

  public int getBitmapSize() {
    return this.bitmapSize;
  }

  @Override
  protected void onSizeChanged(int w, int h, int oldw, int oldh) {
    int s = Math.min(w, h);
    this.bitmapSize = s;
    super.onSizeChanged(s, s, oldw, oldh);
    Log.i("onSizeChanged", "Created Bitmap size = " + s);
    this.bitmap = Bitmap.createBitmap(s, s, Bitmap.Config.ARGB_8888);
    this.bitmap.eraseColor(Color.WHITE);
    this.canvas = new Canvas(this.bitmap);
  }

  @Override
  protected void onDraw(Canvas canvas) {
    super.onDraw(canvas);
    if(this.bitmap != null)
      canvas.drawBitmap(this.bitmap, 0, 0, this.paint);
    canvas.drawPath(this.path, this.paint);
    //canvas.drawPath(this.circlePath, this.circlePaint);
  }


  private float x, y;
  private static final float TOUCH_TOLERANCE = 4;

  private void onTouchStart(float x, float y) {
    this.path.reset();
    this.path.moveTo(x, y);
    this.x = x;
    this.y = y;
  }

  private void onTouchMove(float x, float y) {
    float dx = Math.abs(x - this.x);
    float dy = Math.abs(y - this.y);
    if(dx >= TOUCH_TOLERANCE || dy >= TOUCH_TOLERANCE) {
      this.path.quadTo(this.x, this.y, (x + this.x)/2, (y + this.y)/2);
      this.x = x;
      this.y = y;
      //this.circlePath.reset();
      //this.circlePath.addCircle(this.x, this.y, 30, Path.Direction.CW);
    }
  }

  private void onTouchUp() {
    this.path.lineTo(this.x, this.y);
    //this.circlePath.reset();
    this.canvas.drawPath(this.path, this.paint);
    this.path.reset();
  }

  @Override
  public boolean onTouchEvent(MotionEvent event) {
    float x = event.getX();
    float y = event.getY();

    if(this.activeButton != null) {
      this.activeButton.setImageBitmap(this.bitmap);
      this.activeButton.invalidate();
    }

    switch(event.getAction()) {
      case MotionEvent.ACTION_DOWN:
        this.onTouchStart(x, y);
        this.invalidate();
        break;
      case MotionEvent.ACTION_MOVE:
        this.onTouchMove(x, y);
        this.invalidate();
        break;
      case MotionEvent.ACTION_UP:
        this.onTouchUp();
        this.invalidate();
        break;
    }

    return true;
  }
}
