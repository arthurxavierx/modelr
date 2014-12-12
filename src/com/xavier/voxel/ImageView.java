package com.xavier.voxel;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.content.res.Resources;

public class ImageView {

  public static final int FRONT  = 0; //  0, -1
  public static final int BACK   = 1; // -1,  0
  public static final int LEFT   = 2; // -1,  0
  public static final int RIGHT  = 3; //  0, -1
  public static final int TOP    = 4; //  0, -1
  public static final int BOTTOM = 5; // -1,  0
  public static final int VIEWS  = 6;

  public static final int GRID_END = -1;

  private int startIndex;
  private int endIndex;

  private Bitmap bitmap;

  public ImageView(Bitmap bitmap, int startIndex, int endIndex) {
    this.startIndex = startIndex;
    this.endIndex = endIndex;
    this.bitmap = bitmap;
  }

  public ImageView(Resources res, int id, int startIndex, int endIndex) {
    this.startIndex = startIndex;
    this.endIndex = endIndex;
    this.bitmap = BitmapFactory.decodeResource(res, id);
  }

  public int getStartIndex() {
    return this.startIndex;
  }

  public int getEndIndex() {
    return this.endIndex;
  }

  public Bitmap getBitmap() {
    return this.bitmap;
  }
}