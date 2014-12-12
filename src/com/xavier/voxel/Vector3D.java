package com.xavier.voxel;

import java.io.Serializable;

/**
 * Copyright 2008 - 2010
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 *
 * @project loonframework
 * @author chenpeng  
 * @email：ceponline@yahoo.com.cn 
 * @version 0.1
 */
public class Vector3D implements Serializable {
  /**
   * 
   */
  private static final long serialVersionUID = -7026354578113311982L;

  public float x, y, z;

  public Vector3D() {
    this(0, 0, 0);
  }
  public Vector3D(float value) {
    this(value, value, value);
  }

  public Vector3D(float x, float y, float z) {
    this.x = x;
    this.y = y;
    this.z = z;
  }

  public Vector3D(Vector3D vector3D) {
    this.x = vector3D.x;
    this.y = vector3D.y;
    this.z = vector3D.z;
  }

  public Object clone() {
    return new Vector3D(x, y, z);
  }

  public void move(Vector3D vector3D) {
    this.x += vector3D.x;
    this.y += vector3D.y;
    this.z += vector3D.z;
  }

  public void move(float x, float y, float z) {
    this.x += x;
    this.y += y;
    this.z += z;
  }

  public float[] getCoords() {
    return (new float[] { x, y,z });
  }

  public boolean equals(Object o) {
    if (o instanceof Vector3D) {
      Vector3D p = (Vector3D) o;
      return p.x == x && p.y == y && p.z == z;
    }
    return false;
  }

  public int hashCode() {
    return (int)(x + y + z);
  }

  public void set(Vector3D other) {
    this.x = other.x;
    this.y = other.y;
    this.z = other.z;
  }
  public void set(float x, float y, float z) {
    this.x = x;
    this.y = y;
    this.z = z;
  }

  public Vector3D add(Vector3D other) {
    float x = this.x + other.x;
    float y = this.y + other.y;
    float z = this.z + other.z;
    return new Vector3D(x, y, z);
  }

  public Vector3D sub(Vector3D other) {
    float x = this.x - other.x;
    float y = this.y - other.y;
    float z = this.z - other.z;
    return new Vector3D(x, y, z);
  }

  public Vector3D mul(float value) {
    return new Vector3D(value * x, value * y, value * z);
  }

  public Vector3D cross(Vector3D other) {
    float x = this.y * other.z - other.y * this.z;
    float y = this.z * other.x - other.z * this.x;
    float z = this.x * other.y - other.x * this.y;
    return new Vector3D(x, y, z);
  }

  public float dot(Vector3D other) {
    return other.x * x + other.y * y + other.z * z;
  }

  public float distance(Vector3D other) {
    return this.sub(other).length();
  }

  public Vector3D normalize() {
    float length = this.length();
    return new Vector3D(x / length, y / length, z / length);
  }

  public float length() {
    return (float)Math.sqrt(dot(this));
  }

  public Vector3D modulate(Vector3D other) {
    float x = this.x * other.x;
    float y = this.y * other.y;
    float z = this.z * other.z;
    return new Vector3D(x, y, z);
  }

  public String toString() {
    return (new StringBuffer("[Vector3D x:")).append(x).append(" y:")
      .append(y).append(" z:").append(z).append("]").toString();
  }
}