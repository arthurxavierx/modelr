package com.xavier.modelr;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;
import android.opengl.GLES20;
import android.opengl.GLES10;
import android.opengl.GLSurfaceView;
import android.opengl.GLU;
import android.opengl.Matrix;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Bitmap;
import android.os.SystemClock;

import java.util.Map;
import java.util.HashMap;
import java.lang.System;

import com.xavier.voxel.ImageView;
import com.xavier.voxel.Triangle;
import com.xavier.voxel.Vector3D;
import com.xavier.voxel.Vertex;
import com.xavier.voxel.VertexBuffer;
import com.xavier.voxel.VoxelGrid;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.ByteOrder;

import android.util.Log;

public class ModelrRenderer implements GLSurfaceView.Renderer {

  private VoxelGrid voxelGrid;
  private VertexBuffer vertexBuffer;

  private Context context;

  private int program;

  private static final String vertexShaderCode =
    "attribute vec3 position;" +
    "attribute vec3 barycentric;" +
    "attribute vec3 normal;" +
    "varying vec3 vBarycentric;" +
    "varying vec3 vNormal;" +
    "uniform mat4 model;" + 
    "uniform mat4 view;" + 
    "uniform mat4 proj;" +
    "void main() {" +
    "  mat4 normalMatrix = transpose(inverse(model));" +
    "  vNormal = normalize(normalMatrix*vec4(normal, 1.0f)).xyz;" +
    "  vBarycentric = barycentric;" + 
    //"  gl_Position = (proj*(view*model)) * (vec4(position, 1.0f) + vec4(0.0f, 0.0f, -3.75f, 0.0f));" +
    "  gl_Position = (proj*(view*model)) * vec4(position, 1.0f);" +
    "  gl_PointSize = 1.0f;" +
    "}";

  private final String fragmentShaderCode =
    "precision mediump float;" +
    "uniform vec4 color;" +
    "varying vec3 vBarycentric;" +
    "varying vec3 vNormal;" +
    "vec4 light = vec4(2.0f, -1.0f, -3.0f, 1.0f);" +
    "vec4 ambient = vec4(0.5f, 0.5f, 0.5f, 1.0f);" +
    "void main() {" +
    "  float diffuse = clamp(dot(normalize(light.xyz), vNormal), 0.0f, 1.0f);" +
    "  float f = min(min(vBarycentric.x, vBarycentric.y), vBarycentric.z);" +
    "  gl_FragColor.rgb = color.rgb*diffuse + ambient.xyz*ambient.w;" +
    "  gl_FragColor.rgb *= pow(f+0.0625f, 0.125f);" +
    "  gl_FragColor.a = 2.0f;" +
    "}";

  private final HashMap<String, Integer> uniforms = new HashMap<String, Integer>();

  private final float[] model = new float[16];
  private final float[] arotation = new float[16];
  private final float[] rotation = new float[16];
  private final float[] tmp = new float[16];
  private float pitch, yaw;
  private final float[] view = new float[16];
  private final float[] proj = new float[16];

  public ModelrRenderer(Context context, int size, int steps, HashMap<Integer, Bitmap> bitmaps) {
    this.context = context;

    /**! Creates the VoxelGrid */
    this.voxelGrid = new VoxelGrid();
    this.voxelGrid.createGrid(size);

    /**! Load views */
    if(bitmaps.size() == 0) {
      this.voxelGrid.addView(ImageView.FRONT, context.getResources(), 
        R.drawable.dog_front, 0, ImageView.GRID_END);
      this.voxelGrid.addView(ImageView.RIGHT, context.getResources(), 
        R.drawable.dog_right, 0, ImageView.GRID_END);
      this.voxelGrid.addView(ImageView.TOP, context.getResources(), 
        R.drawable.dog_top, 0, ImageView.GRID_END);
      this.voxelGrid.setColorMask(Color.RED);
    } else {
      this.voxelGrid.setFileSize(bitmaps.get(ImageView.FRONT).getWidth());
      for(Integer key : bitmaps.keySet()) {
        Bitmap bitmap = bitmaps.get(key);
        this.voxelGrid.addView(key, bitmap, 0, ImageView.GRID_END);
      }
      this.voxelGrid.setColorMask(Color.WHITE);
    }

    this.voxelGrid.processVisualHull(false);
    this.voxelGrid.generateMesh(0.5f, 25.0f, steps);

    //this.vertexBuffer = this.voxelGrid.getGridVertexBuffer();
    this.vertexBuffer = this.voxelGrid.getVertexBuffer();
  }

  /**
   * Creates a shader reference from a source String
   * @param type   shader type: GLES20.GL_VERTEX_SHADER or GLES20.GL_FRAGMENT_SHADER
   * @param source shader source string
   * @return shader integer reference to internal OpenGL store
   */
  public static int loadShader(int type, String source) {
    int shader = GLES20.glCreateShader(type);
    GLES20.glShaderSource(shader, source);
    GLES20.glCompileShader(shader);

    final int[] compileStatus = new int[1];
    GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compileStatus, 0);
    if(compileStatus[0] == 0) {
      throw new RuntimeException("Error creating shader.");
    }

    return shader;
  }

  @Override
  public void onSurfaceCreated(GL10 gl, EGLConfig config) {
    GLES20.glClearColor(0.51f, 0.56f, 0.6f, 1.0f);

    int vertexShader = ModelrRenderer.loadShader(GLES20.GL_VERTEX_SHADER, this.vertexShaderCode);
    int fragmentShader = ModelrRenderer.loadShader(GLES20.GL_FRAGMENT_SHADER, this.fragmentShaderCode);

    this.program = GLES20.glCreateProgram();
    GLES20.glAttachShader(this.program, vertexShader);
    GLES20.glAttachShader(this.program, fragmentShader);
    GLES20.glBindAttribLocation(this.program, 0, "position");
    GLES20.glLinkProgram(this.program);

    final int[] linkStatus = new int[1];
    GLES20.glGetProgramiv(this.program, GLES20.GL_LINK_STATUS, linkStatus, 0);
    if(linkStatus[0] == 0) {
      throw new RuntimeException("Error creating program.");
    }

    for(String u : new String[]{"color", "model", "view", "proj"})
      uniforms.put(u, GLES20.glGetUniformLocation(this.program, u));

    GLES20.glEnable(GLES20.GL_DEPTH_TEST);
    GLES20.glEnable(GLES20.GL_CULL_FACE);

    Matrix.setIdentityM(this.arotation, 0);
  }

  @Override
  public void onDrawFrame(GL10 gl) {
    GLES20.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);

    /*long time = SystemClock.uptimeMillis() % 4000L;
    float a = 0.090f * (int)time;*/
    Matrix.setIdentityM(this.model, 0);
    //Matrix.rotateM(this.model, 0, this.pitch, 1.0f, 0.0f, 0.0f);
    //Matrix.rotateM(this.model, 0, this.yaw, 0.0f, 1.0f, 0.0f);

    Matrix.setIdentityM(this.rotation, 0);
    Matrix.rotateM(this.rotation, 0, this.yaw, 0.0f, 1.0f, 0.0f);
    Matrix.rotateM(this.rotation, 0, this.pitch, 1.0f, 0.0f, 0.0f);
    this.pitch = 0.0f;
    this.yaw = 0.0f;

    Matrix.multiplyMM(this.tmp, 0, this.rotation, 0, this.arotation, 0);
    System.arraycopy(this.tmp, 0, this.arotation, 0, 16);
    Matrix.multiplyMM(this.tmp, 0, this.model, 0, this.arotation, 0);
    System.arraycopy(this.tmp, 0, this.model, 0, 16);

    Matrix.translateM(this.model, 0, 0.25f, -3.75f, -3.75f);

    Matrix.setLookAtM(this.view, 0, 
      0.0f,  0.0f, -4.0f,  // eye
      0.0f,  0.0f,  0.0f,  // center
      0.0f, -1.0f,  0.0f); // up

    GLES20.glUseProgram(program);

    GLES20.glUniform4f(uniforms.get("color"), 0.6f, 0.8f, 0.3f, 1.0f);
    GLES20.glUniformMatrix4fv(uniforms.get("model"), 1, false, this.model, 0);
    GLES20.glUniformMatrix4fv(uniforms.get("view"), 1, false, this.view, 0);
    GLES20.glUniformMatrix4fv(uniforms.get("proj"), 1, false, this.proj, 0);

    this.vertexBuffer.draw(this.program, GLES20.GL_TRIANGLES);
    this.vertexBuffer.draw(this.program, GLES20.GL_POINTS);
  }

  @Override
  public void onSurfaceChanged(GL10 gl, int width, int height) {
    GLES20.glViewport(0, 0, width, height);

    float ratio = (float)width/height;
    Matrix.frustumM(this.proj, 0, -ratio, ratio, -1, 1, 1, 100);
  }

  public void rotate(float pitch, float yaw) {
    this.pitch += pitch;
    this.yaw += yaw;
  }
}