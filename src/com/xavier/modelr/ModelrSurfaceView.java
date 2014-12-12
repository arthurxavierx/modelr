package com.xavier.modelr;

import android.opengl.GLSurfaceView;
import android.content.Context;
import android.view.MotionEvent;

import android.graphics.Bitmap;

import java.util.HashMap;

public class ModelrSurfaceView extends GLSurfaceView {

  private ModelrRenderer renderer;
  private float x, y;
  private float density;
  private static final float TOUCH_TOLERANCE = 4;

  public ModelrSurfaceView(Context context, int size, int steps, HashMap<Integer, Bitmap> bitmaps) {
    super(context);

    setEGLContextClientVersion(2);

    this.renderer = new ModelrRenderer(context, size, steps, bitmaps);
    this.setRenderer(this.renderer);
    //setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
    this.density = context.getResources().getDisplayMetrics().density;
  }

  @Override
  public boolean onTouchEvent(MotionEvent event) {
    float x = event.getX();
    float y = event.getY();

    if(event.getAction() == MotionEvent.ACTION_MOVE) {
      float dx = (x - this.x) / this.density / 2.0f;
      float dy = (y - this.y) / this.density / 2.0f;
      this.renderer.rotate(dy, -dx);
    }

    this.x = x;
    this.y = y;

    return true;
  }
}
