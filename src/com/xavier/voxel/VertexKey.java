package com.xavier.voxel;

import java.lang.ClassCastException;

public class VertexKey 
  implements Comparable<VertexKey>
{

  private float x, y, z;

  public VertexKey(float x, float y, float z) {
    this.x = x;
    this.y = y;
    this.z = z;
  }

  @Override
  public boolean equals(Object object) throws ClassCastException {
    VertexKey k = (VertexKey)object;
    return x == k.x && y == k.y && z == k.z;
  }

  @Override
  public int hashCode() {
    return (int)(x + (y*512) + (z*512*512));
  }

  @Override
  public int compareTo(VertexKey k) {
    if(this.equals(k))
      return 0;

    double result = x + (y*512.0f) + (z*512.0f*512.0f);
    double resultKey = k.x + (k.y*512.0f) + (k.z*512.0f*512.0f);

    if(resultKey < result)
      return -1;

    return 1;
  }

  public String toString() {
    return "(" + x + ", " + y + ", " + z + ")";
  }
}