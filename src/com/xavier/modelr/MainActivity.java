package com.xavier.modelr;

import android.app.Activity;
import android.opengl.GLSurfaceView;
import android.os.Bundle;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.util.HashMap;
import com.xavier.voxel.ImageView;

public class MainActivity extends Activity {

  private GLSurfaceView glView;
  private HashMap<Integer, Bitmap> bitmaps;

  /** Called when the activity is first created. */
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    this.bitmaps = new HashMap<Integer, Bitmap>();

    Bundle extras = this.getIntent().getExtras();
    int size = extras.getInt("size");
    int steps = extras.getInt("steps");
    for(Integer i = 0; i < ImageView.VIEWS; i++) {
      byte[] bytes = this.getIntent().getByteArrayExtra("bitmap" + i.toString());
      if(bytes != null)
        this.bitmaps.put(i, BitmapFactory.decodeByteArray(bytes, 0, bytes.length)); 
    }

    this.glView = new ModelrSurfaceView(this, size, steps, this.bitmaps);
    this.setContentView(this.glView);
  }

  @Override
  public void onPause() {
    super.onPause();
    this.glView.onPause();
  }

  @Override
  public void onResume() {
    super.onResume();
    this.glView.onResume();
  }
}
