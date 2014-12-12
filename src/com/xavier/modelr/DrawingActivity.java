package com.xavier.modelr;

import android.app.Activity;
import android.os.Bundle;

import android.content.Intent;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.NumberPicker;
import android.widget.Spinner;
import android.view.View;
import android.view.View.OnClickListener;

import android.graphics.Bitmap;
import android.graphics.Paint;
import android.graphics.Color;

import java.util.HashMap;
import java.io.ByteArrayOutputStream;
import com.xavier.voxel.ImageView;

import android.util.Log;

public class DrawingActivity extends Activity implements OnClickListener {

  private DrawingView drawingView;
  private Paint paint;

  private HashMap<Integer, Bitmap> bitmaps;

  private ImageButton frontButton;
  private ImageButton topButton;
  private ImageButton rightButton;

  private NumberPicker smoothStepsNumberPicker;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    this.setContentView(R.layout.drawing);
    this.drawingView = (DrawingView)this.findViewById(R.id.drawingView);
    this.drawingView.setDrawingCacheEnabled(true);
    this.drawingView.reset();

    int s = this.drawingView.getBitmapSize();
    this.bitmaps = new HashMap<Integer, Bitmap>();

    this.frontButton = (ImageButton)this.findViewById(R.id.frontButton);
    this.topButton = (ImageButton)this.findViewById(R.id.topButton);
    this.rightButton = (ImageButton)this.findViewById(R.id.rightButton);

    this.smoothStepsNumberPicker = (NumberPicker)this.findViewById(R.id.smoothStepsNumberPicker);
    this.smoothStepsNumberPicker.setMinValue(0);
    this.smoothStepsNumberPicker.setMaxValue(8);
  }

  @Override
  public void onClick(View view) {
    Intent intent = new Intent(this, MainActivity.class);
    String size = ((Spinner)this.findViewById(R.id.gridSizeSpinner)).getSelectedItem().toString();
    try {
      intent.putExtra("size", Integer.parseInt(size));
      intent.putExtra("steps", this.smoothStepsNumberPicker.getValue());
    } catch(Exception e) {
      intent.putExtra("size", 16);
      intent.putExtra("steps", 0);
    }

    switch(view.getId()) {
      case R.id.generateButton:
        for(Integer key : this.bitmaps.keySet()) {
          ByteArrayOutputStream stream = new ByteArrayOutputStream();
          this.bitmaps.get(key).compress(Bitmap.CompressFormat.PNG, 100, stream);
          intent.putExtra("bitmap" + key.toString(), stream.toByteArray());
        }
        this.startActivity(intent);
        break;
      // TODO: add support for multiple input views
      case R.id.dogButton:
        this.startActivity(intent);
        break;
      case R.id.clearButton:
        this.drawingView.reset();
        this.drawingView.invalidate();
        if(this.drawingView.getActiveButton() != null)
          this.drawingView.getActiveButton().invalidate();
        break;
     }
  }

  public void addImage(View view) {
    switch(view.getId()) {
      case R.id.frontButton:
        if(this.bitmaps.get(ImageView.FRONT) == null) {
          this.bitmaps.put(ImageView.FRONT, Bitmap.createBitmap(this.drawingView.getBitmap()));
          this.frontButton.setImageBitmap(this.bitmaps.get(ImageView.FRONT));
          this.frontButton.invalidate();
        }
        this.drawingView.setBitmap(this.bitmaps.get(ImageView.FRONT));
        this.drawingView.setActiveButton(this.frontButton);
        break;
      case R.id.topButton:
        if(this.bitmaps.get(ImageView.TOP) == null) {
          this.bitmaps.put(ImageView.TOP, Bitmap.createBitmap(this.drawingView.getBitmap()));
          this.topButton.setImageBitmap(this.bitmaps.get(ImageView.TOP));
          this.topButton.invalidate();
        }
        this.drawingView.setBitmap(this.bitmaps.get(ImageView.TOP));
        this.drawingView.setActiveButton(this.topButton);
        break;
      case R.id.rightButton:
        if(this.bitmaps.get(ImageView.RIGHT) == null) {
          this.bitmaps.put(ImageView.RIGHT, Bitmap.createBitmap(this.drawingView.getBitmap()));
          this.rightButton.setImageBitmap(this.bitmaps.get(ImageView.RIGHT));
          this.rightButton.invalidate();
        }
        this.drawingView.setBitmap(this.bitmaps.get(ImageView.RIGHT));
        this.drawingView.setActiveButton(this.rightButton);
        break;
    }
    this.drawingView.invalidate();
  }

  @Override
  public void onPause() {
    super.onPause();
  }

  @Override
  public void onResume() {
    super.onResume();
    this.drawingView.invalidate();
  }
}