package com.xavier.voxel;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.nio.ByteOrder;

import javax.microedition.khronos.opengles.GL10;
import android.opengl.GLES10;
import android.opengl.GLES20;

import com.xavier.voxel.Triangle;
import com.xavier.voxel.Vertex;

public class VertexBuffer {

  private FloatBuffer vertexBuffer;
  private FloatBuffer normalBuffer;
  private FloatBuffer barycentricBuffer;
  private int vertexCount;

  public VertexBuffer(int vertices) {
    this.vertexBuffer = ByteBuffer.allocateDirect(vertices*3*4)
      .order(ByteOrder.nativeOrder())
      .asFloatBuffer();
    this.barycentricBuffer = ByteBuffer.allocateDirect(vertices*3*4)
      .order(ByteOrder.nativeOrder())
      .asFloatBuffer();
    this.normalBuffer = ByteBuffer.allocateDirect(vertices*3*4)
      .order(ByteOrder.nativeOrder())
      .asFloatBuffer();
    this.vertexCount = 0;
  }

  public void finish() {
    this.vertexBuffer.position(0);
    this.barycentricBuffer.position(0);
    this.normalBuffer.position(0);
  }

  public void addVertex(Vertex v) {
    this.vertexBuffer.put(v.getCoords());
    this.vertexCount++;
  }
  public void addVertex(Vector3D v) {
    float[] coords = new float[]{v.x, v.y, v.z};
    this.vertexBuffer.put(coords);
    this.vertexCount++;
  }
  public void addVertices(Vertex[] vs) {
    for(Vertex v : vs) {
      this.addVertex(v);
    }
  }
  public void addTriangle(Triangle t) {
    this.addVertices(t.getVertices());

    Vector3D n = t.getNormal();
    for(int i = 0; i < 3; i++)
      this.normalBuffer.put(new float[]{n.x, n.y, n.z});

    this.barycentricBuffer.put(new float[]{1.0f, 0.0f, 0.0f});
    this.barycentricBuffer.put(new float[]{0.0f, 1.0f, 0.0f});
    this.barycentricBuffer.put(new float[]{0.0f, 0.0f, 1.0f});
  }

  public void addNormal(Vector3D n) {
    this.normalBuffer.put(n.getCoords());
  }

  public void draw(int program, int drawType) {
    /*GLES10.glVertexPointer(3, GL10.GL_FLOAT, 0, this.vertexBuffer);
    GLES10.glDrawArrays(GL10.GL_TRIANGLES, 0, this.vertexCount);*/

    int aPosition = GLES20.glGetAttribLocation(program, "position");
    int aNormal = GLES20.glGetAttribLocation(program, "normal");
    int aBarycentric = GLES20.glGetAttribLocation(program, "barycentric");

    GLES20.glUseProgram(program);

    GLES20.glVertexAttribPointer(aPosition, 3, GLES20.GL_FLOAT, false,
                                 3*4, this.vertexBuffer);
    GLES20.glVertexAttribPointer(aNormal, 3, GLES20.GL_FLOAT, false,
                                 3*4, this.normalBuffer);
    GLES20.glVertexAttribPointer(aBarycentric, 3, GLES20.GL_FLOAT, false,
                                 3*4, this.barycentricBuffer);

    GLES20.glEnableVertexAttribArray(aPosition);
    GLES20.glEnableVertexAttribArray(aNormal);
    GLES20.glEnableVertexAttribArray(aBarycentric);

    GLES20.glDrawArrays(drawType, 0, this.vertexCount);

    GLES20.glDisableVertexAttribArray(aPosition);
    GLES20.glDisableVertexAttribArray(aNormal);
    GLES20.glDisableVertexAttribArray(aBarycentric);
  }

  public FloatBuffer getVertexBuffer() {
    return this.vertexBuffer;
  }
  public int getVertexCount() {
    return this.vertexCount;
  }
}