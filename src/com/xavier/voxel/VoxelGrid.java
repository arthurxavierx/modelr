package com.xavier.voxel;

import android.graphics.Color;
import android.content.res.Resources;

import android.graphics.Bitmap;

import java.util.HashSet;
import java.util.Iterator;
import java.util.HashMap;
import java.util.ArrayList;

import android.util.Log;

public class VoxelGrid {

  private int size;
  private int fileSize;

  private float edgeSize;

  private float minEdgeInterval;
  private float maxEdgeInterval;

  private HashMap<Integer, HashSet<ImageView>> views;

  private boolean drawLine;

  private Vertex grid[];

  private float limitAngleBetweenNormals;
  private float pivotInterpolation;

  private HashMap<VertexKey, Vertex> vertices;
  private HashSet<Triangle> triangles;
 
  private int mask;

  /* PUBLIC */
  public VoxelGrid() {
    this.minEdgeInterval = -2.0f;
    this.maxEdgeInterval =  2.0f;
    this.drawLine = false;
    this.fileSize = 512;
    this.pivotInterpolation = 0.5f;
    this.mask = Color.WHITE;

    this.views = new HashMap<Integer, HashSet<ImageView>>();
    for(int i = 0; i < ImageView.VIEWS; i++) {
      this.views.put(i, new HashSet<ImageView>());
    }

    this.vertices = new HashMap<VertexKey, Vertex>();
    this.triangles = new HashSet<Triangle>();
  }

  public void createGrid(int size) {
    int i, x, y, z;
    float xValue, yValue, zValue;

    this.size = size;
    this.edgeSize = (this.maxEdgeInterval - this.minEdgeInterval) / (float)size;
    this.grid = new Vertex[size*size*size];

    for(x = 0, xValue = this.minEdgeInterval; x < size; x++, xValue += this.edgeSize) {
    for(y = 0, yValue = this.maxEdgeInterval; y < size; y++, yValue += this.edgeSize) {
    for(z = 0, zValue = this.maxEdgeInterval; z < size; z++, zValue += this.edgeSize) {
      i = this.getVertexIndex(x, y, z);
      this.grid[i] = new Vertex(i, new Vector3D(xValue, yValue, zValue));
    }}}
  }

  public void processSmoothMesh() {
    for(HashMap.Entry entry : this.vertices.entrySet()) {
      Vertex v = (Vertex)entry.getValue();
      if(!v.isDestroyed())
        this.relaxVertex(v, 2.0f);
    }
  }

  public void setColorMask(int color) {
    this.mask = color;
  }
  public int getColorMask() {
    return this.mask;
  }

  public void setFileSize(int fileSize) {
    this.fileSize = fileSize;
  }
  public int getFileSize() {
    return this.fileSize;
  }

  public void addView(int view, Bitmap bitmap, int startIndex, int endIndex) {
    this.views.get(view).add(new ImageView(bitmap, startIndex, endIndex));
  }

  public void addView(int view, Resources res, int id, int startIndex, int endIndex) {
    this.views.get(view).add(new ImageView(res, id, startIndex, endIndex));
  }

  public Vertex getVertex(int x, int y, int z) {
    return this.grid[this.getVertexIndex(x, y, z)];
  }

  public int getVertexIndex(int x, int y, int z) {
    return x + y*size + z*size*size;
  }

  private Vector3D getPosition(int x, int y, int z) {
    return this.getVertex(x, y, z).getPosition();
  }
  private Vector3D getOriginalPosition(int x, int y, int z) {
    return this.getVertex(x, y, z).getOriginalPosition();
  }
  private Vector3D getPositionByImageIndex(int x, int y, int z) {
    float imageEdgeSize = (maxEdgeInterval - minEdgeInterval) / 512.0f;
    return new Vector3D(minEdgeInterval + x*imageEdgeSize,
                        maxEdgeInterval - y*imageEdgeSize,
                        maxEdgeInterval - z*imageEdgeSize);
  }


  private boolean checkImageVisibility(int x, int y, int z) {
    for(HashMap.Entry entry : this.views.entrySet()) {
      for(ImageView view : (HashSet<ImageView>)entry.getValue()) {
        if(this.checkPixel(view, x, y) == false)
          return false;
      }
    }
    return true;
  }

  private boolean isVisible(int x, int y, int z) {
    return getVertex(x, y, z).isVisible();
  }


  public void processVisualHull(boolean drawLine) {
    this.drawLine = drawLine;

    for(HashMap.Entry entry : this.views.entrySet()) {
      for(ImageView view : (HashSet<ImageView>)entry.getValue()) {
        this.processVisualHull((Integer)entry.getKey(), view);
      }
    }
  }

  public void generateMesh(float pivotInterpolation, float limitAngleBetweenNormals, int smoothSteps) {
    this.limitAngleBetweenNormals = limitAngleBetweenNormals;
    this.pivotInterpolation = pivotInterpolation;

    this.processEdgeIntersects();
    this.generateTriangles();

    for(int i = 0; i < smoothSteps; i++) {
      this.processSmoothMesh();
    }

    this.collapse(0.0f);
  }

  public void setDrawLine(boolean drawLine) {
    this.drawLine = drawLine;
  }

  public VertexBuffer getVertexBuffer() {
    VertexBuffer vb = new VertexBuffer(this.triangles.size()*3);
    Log.i("getVertexBuffer", "Created VertexBuffer size = " + this.triangles.size()*3);
    for(Triangle t : this.triangles) {
      if(!t.isDestroyed())
        vb.addTriangle(t);
    }
    vb.finish();
    return vb;
  }
  public VertexBuffer getGridVertexBuffer() {
    VertexBuffer vb = new VertexBuffer(this.grid.length);
    for(Vertex v : this.grid) 
      if(v.isVisible())
        vb.addVertex(v);
    vb.finish();
    return vb;
  }

  /* PRIVATE */
  private void processVisualHull(int direction, ImageView view) {

    int startIndex, endIndex;

    switch(direction) {
    case ImageView.FRONT:
      endIndex = (view.getEndIndex() == ImageView.GRID_END) ? this.size : view.getEndIndex();
      for(int i = 0; i < this.fileSize; i++) {
        for(int j = 0; j < this.fileSize; j++) {
          int x = (i*this.size) / this.fileSize;
          int y = (j*this.size) / this.fileSize;
          boolean visible = this.checkPixel(view, i, j);
          for(int z = view.getStartIndex(); z < endIndex; z++) {
            if(!visible)
              this.getVertex(x, y, z).setVisible(false);
          }
        }
      }
      break;

    case ImageView.BACK:
      startIndex = (view.getStartIndex() == ImageView.GRID_END) ? this.size - 1 : view.getStartIndex();
      for(int i = 0; i < this.fileSize; i++) {
        for(int j = 0; j < this.fileSize; j++) {
          int x = (this.fileSize - i - 1) * this.size / this.fileSize;
          int y = j*this.size / this.fileSize;
          boolean visible = this.checkPixel(view, i, j);
          for(int z = startIndex; z >= view.getEndIndex(); z--) {
            if(!visible)
              this.getVertex(x, y, z).setVisible(false);
          }
        }
      }
      break;

    case ImageView.LEFT:
      startIndex = (view.getStartIndex() == ImageView.GRID_END) ? this.size - 1 : view.getStartIndex();
      for(int i = 0; i < this.fileSize; i++) {
        for(int j = 0; j < this.fileSize; j++) {
          int z = (this.fileSize - 1 - i) * this.size / this.fileSize;
          int y = j * this.size / this.fileSize;
          boolean visible = this.checkPixel(view, i, j);
          for(int x = startIndex; x >= view.getEndIndex(); x--) {
            if(!visible)
              this.getVertex(x, y, z).setVisible(false);
          }
        }
      }
      break;

    case ImageView.RIGHT:
      endIndex = (view.getEndIndex() == ImageView.GRID_END) ? this.size : view.getEndIndex();
      for(int i = 0; i < this.fileSize; i += 2) {
        for(int j = 0; j < this.fileSize; j += 2) {
          int z = i * this.size / this.fileSize;
          int y = j * this.size / this.fileSize;
          boolean visible = this.checkPixel(view, i, j);
          for(int x = view.getStartIndex(); x < endIndex; x++) {
            if(!visible)
              this.getVertex(x, y, z).setVisible(false);
          }
        }
      }
      break;

    case ImageView.TOP:
      endIndex = (view.getEndIndex() == ImageView.GRID_END) ? this.size : view.getEndIndex();
      for(int i = 0; i < this.fileSize; i++) {
        for(int j = 0; j < this.fileSize; j++) {
          int x = i * this.size / this.fileSize;
          int z = (this.fileSize - 1 - j) * this.size / this.fileSize;
          boolean visible = this.checkPixel(view, i, j);
          for(int y = view.getStartIndex(); y < endIndex; y++) {
            if(!visible)
              this.getVertex(x, y, z).setVisible(false);
          }
        }
      }
      break;

    case ImageView.BOTTOM:
      // not implemented in the original algorithm... weirdo...
      break;

    default:
      // throw new SomethingException...
      break;
    }
  }

  private void generateTriangles() {
    this.vertices.clear();
    this.triangles.clear();

    int step = 1;
    for(int z = 0; z < this.size - 1; z += step) {
    for(int y = 0; y < this.size - 1; y += step) {
    for(int x = 0; x < this.size - 1; x += step) {
      short lookup = this.checkLookupTable(x, y, z, step);
      if(lookup == 0 || lookup == 255)
        continue;

      Vector3D[] voxelMesh = new Vector3D[12];

      // 0 - 1
      if((this.EDGE_TABLE[lookup] & 1) != 0)
        voxelMesh[0] = this.interpolate(x, y+step, z+step,
                                        x+step, y+step, z+step);
      // 1 - 2
      if((this.EDGE_TABLE[lookup] & 2) != 0)
        voxelMesh[1] = this.interpolate(x+step, y+step, z+step,
                                        x+step, y+step, z);
      // 2 - 3
      if((this.EDGE_TABLE[lookup] & 4) != 0)
        voxelMesh[2] = this.interpolate(x+step, y+step, z,
                                        x, y+step, z);
      // 3 - 0
      if((this.EDGE_TABLE[lookup] & 8) != 0)
        voxelMesh[3] = this.interpolate(x, y+step, z,
                                        x, y+step, z+step);
      // 4 - 5
      if((this.EDGE_TABLE[lookup] & 16) != 0)
        voxelMesh[4] = this.interpolate(x, y, z+step,
                                        x+step, y, z+step);
      // 5 - 6
      if((this.EDGE_TABLE[lookup] & 32) != 0)
        voxelMesh[5] = this.interpolate(x+step, y, z+step,
                                        x+step, y, z);
      // 6 - 7
      if((this.EDGE_TABLE[lookup] & 64) != 0)
        voxelMesh[6] = this.interpolate(x+step, y, z,
                                        x, y, z);
      // 7 - 4
      if((this.EDGE_TABLE[lookup] & 128) != 0)
        voxelMesh[7] = this.interpolate(x, y, z,
                                        x, y, z+step);
      // 0 - 4
      if((this.EDGE_TABLE[lookup] & 256) != 0)
        voxelMesh[8] = this.interpolate(x, y+step, z+step,
                                        x, y, z+step);
      // 1 - 5
      if((this.EDGE_TABLE[lookup] & 512) != 0)
        voxelMesh[9] = this.interpolate(x+step, y+step, z+step,
                                        x+step, y, z+step);
      // 2 - 6
      if((this.EDGE_TABLE[lookup] & 1024) != 0)
        voxelMesh[10] = this.interpolate(x+step, y+step, z,
                                         x+step, y, z);
      // 3 - 7
      if((this.EDGE_TABLE[lookup] & 2048) != 0)
        voxelMesh[11] = this.interpolate(x, y+step, z,
                                         x, y, z);

      for(int k = 0; this.TRIANGLE_TABLE[lookup][k] != -1; k += 3) {
        Triangle t = new Triangle();
        for(int j = k; j < k+3; j++) {
          Vertex v = null;
          Vector3D position = voxelMesh[this.TRIANGLE_TABLE[lookup][j]];
          VertexKey key = new VertexKey(position.x, position.y, position.z);

          if(!this.vertices.containsKey(key)) {
            v = new Vertex(this.vertices.size(), position);
            this.vertices.put(key, v);
          } else {
            v = this.vertices.get(key);
          }

          t.setVertex(j - k, v);
          v.addFace(t);
        }

        t.updateNeighbours();
        t.computeNormal();
        this.triangles.add(t);
      }
    }}}

    Log.i("generateTriangles", "Generated " + this.triangles.size() + " triangles");
  }

  private void processEdgeIntersects() {
    for(int x = 0; x < this.size - 1; x++) {
    for(int y = 0; y < this.size - 1; y++) {
    for(int z = 0; z < this.size - 1; z++) {
      short lookup = this.checkLookupTable(x, y, z, 1);
      if(lookup == 0 || lookup == 255)
        continue;

      // 0 - 1
      if((this.EDGE_TABLE[lookup] & 1) != 0)
        this.updateVoxelVertex(x, y+1, z+1,
                               x+1, y+1, z+1);
      // 1 - 2
      if((this.EDGE_TABLE[lookup] & 2) != 0)
        this.updateVoxelVertex(x+1, y+1, z+1,
                               x+1, y+1, z);
      // 2 - 3
      if((this.EDGE_TABLE[lookup] & 4) != 0)
        this.updateVoxelVertex(x+1, y+1, z,
                               x, y+1, z);
      // 3 - 0
      if((this.EDGE_TABLE[lookup] & 8) != 0)
        this.updateVoxelVertex(x, y+1, z,
                               x, y+1, z+1);
      // 4 - 5
      if((this.EDGE_TABLE[lookup] & 16) != 0)
        this.updateVoxelVertex(x, y, z+1,
                               x+1, y, z+1);
      // 5 - 6
      if((this.EDGE_TABLE[lookup] & 32) != 0)
        this.updateVoxelVertex(x+1, y, z+1,
                               x+1, y, z);
      // 6 - 7
      if((this.EDGE_TABLE[lookup] & 64) != 0)
        this.updateVoxelVertex(x+1, y, z,
                               x, y, z);
      // 7 - 4
      if((this.EDGE_TABLE[lookup] & 128) != 0)
        this.updateVoxelVertex(x, y, z,
                               x, y, z+1);
      // 0 - 4
      if((this.EDGE_TABLE[lookup] & 256) != 0)
        this.updateVoxelVertex(x, y+1, z+1,
                               x, y, z+1);
      // 1 - 5
      if((this.EDGE_TABLE[lookup] & 512) != 0)
        this.updateVoxelVertex(x+1, y+1, z+1,
                               x+1, y, z+1);
      // 2 - 6
      if((this.EDGE_TABLE[lookup] & 1024) != 0)
        this.updateVoxelVertex(x+1, y+1, z+1,
                               x+1, y, z);
      // 3 - 7
      if((this.EDGE_TABLE[lookup] & 2048) != 0)
        this.updateVoxelVertex(x, y+1, z,
                               x, y, z);
    }}}
  }

  private void relaxVertex(Vertex u, float limitAreaScalar) {
    int neighbours = 0;
    Vector3D contribution = new Vector3D();

    for(Vertex v : u.getNeighbours()) {
      if(v.isDestroyed())
        continue;
      neighbours++;
      contribution.move(v.getPosition().sub(u.getPosition()));
    }

    if(neighbours != 0) {
      u.getPosition().move(contribution.mul(1.0f / ((float)neighbours + 1.0f)));

      if(u.getPosition().x < u.getOriginalPosition().x - (this.edgeSize*limitAreaScalar))
         u.getPosition().x = u.getOriginalPosition().x - (this.edgeSize*limitAreaScalar);
 else if(u.getPosition().x > u.getOriginalPosition().x + (this.edgeSize*limitAreaScalar))
         u.getPosition().x = u.getOriginalPosition().x + (this.edgeSize*limitAreaScalar);

      if(u.getPosition().y < u.getOriginalPosition().y - (this.edgeSize*limitAreaScalar))
         u.getPosition().y = u.getOriginalPosition().y - (this.edgeSize*limitAreaScalar);
 else if(u.getPosition().y > u.getOriginalPosition().y + (this.edgeSize*limitAreaScalar))
         u.getPosition().y = u.getOriginalPosition().y + (this.edgeSize*limitAreaScalar);

      if(u.getPosition().z < u.getOriginalPosition().z - (this.edgeSize*limitAreaScalar))
         u.getPosition().z = u.getOriginalPosition().z - (this.edgeSize*limitAreaScalar);
 else if(u.getPosition().z > u.getOriginalPosition().z + (this.edgeSize*limitAreaScalar))
         u.getPosition().z = u.getOriginalPosition().z + (this.edgeSize*limitAreaScalar);
    }
  }

  private short checkLookupTable(int x, int y, int z, int step) {
    short lookup = 0;

    // Vertex 0
    if(isVisible(x, y, z))
      lookup |= 128;
    // Vertex 1
    if(isVisible(x + step, y, z))
      lookup |= 64;
    // Vertex 2
    if(isVisible(x + step, y + step, z))
      lookup |= 4;
    // Vertex 3
    if(isVisible(x, y + step, z))
      lookup |= 8;
    // Vertex 4
    if(isVisible(x, y, z + step))
      lookup |= 16;
    // Vertex 5
    if(isVisible(x + step, y, z + step))
      lookup |= 32;
    // Vertex 6
    if(isVisible(x + step, y + step, z + step))
      lookup |= 2;
    // Vertex 7
    if(isVisible(x, y + step, z + step))
      lookup |= 1;

    return lookup;
  }

  private void collapse(float vertexPercentage) {
    if(vertexPercentage < 0.0f || vertexPercentage > 1.0f)
      return;

    for(HashMap.Entry entry : this.vertices.entrySet()) {
      this.computeEdgeCostAtVertex((Vertex)entry.getValue(), true);
    }

    float vertexCount = (float)this.vertices.size();
    while(this.vertices.size() > vertexCount * vertexPercentage) {
      // get collapse edge
      Vertex u = this.getMinimumCostVertex();

      // check if it's possible to remove more vertices
      if(u == null)
        return;

      Vertex v = u.getCollapseVertex();

      // collapse the edge (u,v) by moving vertex u into v
      if(v == null) {
        this.vertices.remove(new VertexKey(u.getPosition().x, u.getPosition().y, u.getPosition().z));
        // remove u from neighbours
        for(Vertex w : u.getNeighbours())
          w.getNeighbours().remove(u);
        u.setDestroyed(true);
        continue;
      }

      // save u neighbours
      HashSet<Vertex> tmp = new HashSet<Vertex>();
      for(Vertex w : u.getNeighbours()) {
        if(!w.isDestroyed())
          tmp.add(w);
      }

      // delete triangles on edge (u,v)
      for(Triangle t : u.getFaces()) {
        if(t.hasVertex(v)) {
          // remove this face
          this.triangles.remove(t);
          t.setDestroyed(true);
          for(int i = 0; i < 3; i++) {
            if(u != t.getVertex(i))
              t.getVertex(i).getFaces().remove(t);
          }
        } else {
          // update remaining triangles to have v instead of u
          t.replaceVertex(u, v);
        }
      }

      // remove u from neighbours
      for(Vertex w : u.getNeighbours())
        w.getNeighbours().remove(u);

      // destroy u object
      this.vertices.remove(new VertexKey(u.getPosition().x, u.getPosition().y, u.getPosition().z));
      u.setDestroyed(true);

      // recalculate edge collapse costs in neighbourhood
      for(Vertex w : tmp) {
        if(w.isDestroyed())
          continue;
        this.computeEdgeCostAtVertex(w, false);
      }
    }
  }

  private Vertex getMinimumCostVertex() {
    Vertex best = null;
    // Calculate the neighbour cost
    for(HashMap.Entry entry : this.vertices.entrySet()) {
      Vertex v = (Vertex)entry.getValue();
      if(!v.isDestroyed() && v.canRemove()) {
        if(best == null || v.getCollapseCost() < best.getCollapseCost())
          best = v;
      }
    } 
    return best;
  }

  private void computeEdgeCostAtVertex(Vertex u, boolean canRemove) {
    u.setCanRemove(true);
    if(u.getNeighbours().size() == 0) {
      u.setCollapseVertex(null);
      u.setCollapseCost(0.0f);
      return;
    }

    float totalCost = 0.0f;
    float smallestCost = 100000.0f;
    u.setCollapseVertex(null);

    // Search all neighbouring edges for "least cost" edge
    for(Vertex v : u.getNeighbours()) {
      if(v.isDestroyed())
        continue;

      float cost = this.computeEdgeCollapseCost(u, v);
      if(!u.canRemove()) {
        u.setCollapseCost(cost);
        return;
      }
      if(cost < smallestCost) {
        u.setCollapseVertex(v);
        smallestCost = cost;
      }

      totalCost += cost;
    }

    if(u.getCollapseVertex() == null) {
      u.setCollapseCost(0.0f);
    } else {
      u.setCollapseCost(totalCost);
    }
  }

  private float computeEdgeCollapseCost(Vertex u, Vertex v) {
    // If we collapse edge (u,v) by moving u to v then how much
    // different will the model change? i.e. the "error".
    float edgeLength = v.getPosition().distance(u.getPosition());
    float curvature = 0.0f;

    // Use the triangle adjacent triangles
    // to determine our curvature term
    int validFaces = 0;
    for(Triangle f : u.getFaces()) {
      if(f.isDestroyed() || f.hasVertex(v) == false)
        continue;

      validFaces++;

      float mincurv = 360.0f;
      for(Triangle g : u.getFaces()) {
        if(g.isDestroyed() || f == g)
          continue;

        float dot = f.getNormal().normalize().dot(g.getNormal().normalize());
        float angle = (float)Math.acos(dot);
        float degrees = (angle * 180.0f) / (float)Math.PI;

        mincurv += degrees;

        if(degrees > this.limitAngleBetweenNormals) {
          u.setCanRemove(false);
          return 10000.0f;
        }
      }
      curvature += mincurv;
    }

    curvature /= validFaces;
    if(curvature == 0.0f);
      curvature = 1.0f;

    return edgeLength * curvature;
  }

  private boolean checkPixel(ImageView view, int x, int y) {
    if(view.getBitmap().getPixel(x, y) == this.mask)
      return false;
    return true;
  }

  private Vector3D interpolate(int x1, int y1, int z1, 
                               int x2, int y2, int z2)
  {
    Vector3D result = new Vector3D();
    Vector3D v1P = this.getVertex(x1, y1, z1).getPosition();
    Vector3D v2P = this.getVertex(x2, y2, z2).getPosition();

    float multiplier = 0.5f;
    if(this.isVisible(x1, y1, z1) || !this.isVisible(x2, y2, z2))
      multiplier = this.pivotInterpolation;
    else if(!this.isVisible(x1, y1, z1) || this.isVisible(x2, y2, z2))
      multiplier = 1.0f - this.pivotInterpolation;

    result.set(v1P.x + (v2P.x - v1P.x)*multiplier,
               v1P.y + (v2P.y - v1P.y)*multiplier,
               v1P.z + (v2P.z - v1P.z)*multiplier);
    return result;
  }

  private void updateVoxelVertex(int x1, int y1, int z1, 
                                 int x2, int y2, int z2)
  {
    int imgXIndex1 = x1 * this.fileSize / this.size;
    int imgYIndex1 = y1 * this.fileSize / this.size;
    int imgZIndex1 = z1 * this.fileSize / this.size;

    int imgXIndex2 = x2 * this.fileSize / this.size;
    int imgYIndex2 = y2 * this.fileSize / this.size;
    int imgZIndex2 = z2 * this.fileSize / this.size;

    boolean updateX = (imgXIndex1 != imgXIndex2);
    boolean updateY = (imgYIndex1 != imgYIndex2);
    boolean updateZ = (imgZIndex1 != imgZIndex2);

    if((this.isVisible(x1, y1, z1) && !isVisible(x2, y2, z2)) || 
       (!this.isVisible(x1, y1, z1) && isVisible(x2, y2, z2))) {
      int xStart = imgXIndex1;
      int xEnd = imgXIndex2;
      if(this.isVisible(x1, y1, z1) && !this.isVisible(x2, y2, z2)) {
        xStart = xEnd;
        xEnd = imgXIndex1;
      }
      int xIncrement = (xEnd - xStart) / 4;

      int yStart = imgYIndex1;
      int yEnd = imgYIndex2;
      if(this.isVisible(x1, y1, z1) && !this.isVisible(x2, y2, z2)) {
        yStart = yEnd;
        yEnd = imgYIndex1;
      }
      int yIncrement = (yEnd - yStart) / 4;

      int zStart = imgZIndex1;
      int zEnd = imgZIndex2;
      if(this.isVisible(x1, y1, z1) && !this.isVisible(x2, y2, z2)) {
        zStart = zEnd;
        zEnd = imgZIndex1;
      }
      int zIncrement = (zEnd - zStart) / 4;

      for(int x = xStart; x != xEnd; x += xIncrement) {
      for(int y = yStart; y != yEnd; y += yIncrement) {
      for(int z = zStart; z != zEnd; z += zIncrement) {
        if(this.checkImageVisibility(x, y, z)) {
          boolean result = false;

          if(this.isVisible(x1, y1, z1) && !this.isVisible(x2, y2, z2)) {
            result = updateVoxelVertex(x, y, z, x1, y1, z1, updateX, updateY, updateZ);
          } else {
            result = updateVoxelVertex(x, y, z, x2, y2, z2, updateX, updateY, updateZ);
          }

          if(result)
            return;
        }
      }}}
    }
  }

  private boolean updateVoxelVertex(int x, int y, int z,
                                    int vx, int vy, int vz,
                                    boolean updateX, boolean updateY, boolean updateZ)
  {
    Vector3D position = this.getPositionByImageIndex(x, y, z);
    if(updateX)
      this.getVertex(vx, vy, vx).getPosition().x = position.x;
    if(updateY)
      this.getVertex(vx, vy, vx).getPosition().y = position.y;
    if(updateZ)
      this.getVertex(vx, vy, vx).getPosition().z = position.z;
    return true;
  }

  private static final int[] EDGE_TABLE = {
    0x0  , 0x109, 0x203, 0x30a, 0x406, 0x50f, 0x605, 0x70c,
    0x80c, 0x905, 0xa0f, 0xb06, 0xc0a, 0xd03, 0xe09, 0xf00,
    0x190, 0x99 , 0x393, 0x29a, 0x596, 0x49f, 0x795, 0x69c,
    0x99c, 0x895, 0xb9f, 0xa96, 0xd9a, 0xc93, 0xf99, 0xe90,
    0x230, 0x339, 0x33 , 0x13a, 0x636, 0x73f, 0x435, 0x53c,
    0xa3c, 0xb35, 0x83f, 0x936, 0xe3a, 0xf33, 0xc39, 0xd30,
    0x3a0, 0x2a9, 0x1a3, 0xaa , 0x7a6, 0x6af, 0x5a5, 0x4ac,
    0xbac, 0xaa5, 0x9af, 0x8a6, 0xfaa, 0xea3, 0xda9, 0xca0,
    0x460, 0x569, 0x663, 0x76a, 0x66 , 0x16f, 0x265, 0x36c,
    0xc6c, 0xd65, 0xe6f, 0xf66, 0x86a, 0x963, 0xa69, 0xb60,
    0x5f0, 0x4f9, 0x7f3, 0x6fa, 0x1f6, 0xff , 0x3f5, 0x2fc,
    0xdfc, 0xcf5, 0xfff, 0xef6, 0x9fa, 0x8f3, 0xbf9, 0xaf0,
    0x650, 0x759, 0x453, 0x55a, 0x256, 0x35f, 0x55 , 0x15c,
    0xe5c, 0xf55, 0xc5f, 0xd56, 0xa5a, 0xb53, 0x859, 0x950,
    0x7c0, 0x6c9, 0x5c3, 0x4ca, 0x3c6, 0x2cf, 0x1c5, 0xcc ,
    0xfcc, 0xec5, 0xdcf, 0xcc6, 0xbca, 0xac3, 0x9c9, 0x8c0,
    0x8c0, 0x9c9, 0xac3, 0xbca, 0xcc6, 0xdcf, 0xec5, 0xfcc,
    0xcc , 0x1c5, 0x2cf, 0x3c6, 0x4ca, 0x5c3, 0x6c9, 0x7c0,
    0x950, 0x859, 0xb53, 0xa5a, 0xd56, 0xc5f, 0xf55, 0xe5c,
    0x15c, 0x55 , 0x35f, 0x256, 0x55a, 0x453, 0x759, 0x650,
    0xaf0, 0xbf9, 0x8f3, 0x9fa, 0xef6, 0xfff, 0xcf5, 0xdfc,
    0x2fc, 0x3f5, 0xff , 0x1f6, 0x6fa, 0x7f3, 0x4f9, 0x5f0,
    0xb60, 0xa69, 0x963, 0x86a, 0xf66, 0xe6f, 0xd65, 0xc6c,
    0x36c, 0x265, 0x16f, 0x66 , 0x76a, 0x663, 0x569, 0x460,
    0xca0, 0xda9, 0xea3, 0xfaa, 0x8a6, 0x9af, 0xaa5, 0xbac,
    0x4ac, 0x5a5, 0x6af, 0x7a6, 0xaa , 0x1a3, 0x2a9, 0x3a0,
    0xd30, 0xc39, 0xf33, 0xe3a, 0x936, 0x83f, 0xb35, 0xa3c,
    0x53c, 0x435, 0x73f, 0x636, 0x13a, 0x33 , 0x339, 0x230,
    0xe90, 0xf99, 0xc93, 0xd9a, 0xa96, 0xb9f, 0x895, 0x99c,
    0x69c, 0x795, 0x49f, 0x596, 0x29a, 0x393, 0x99 , 0x190,
    0xf00, 0xe09, 0xd03, 0xc0a, 0xb06, 0xa0f, 0x905, 0x80c,
    0x70c, 0x605, 0x50f, 0x406, 0x30a, 0x203, 0x109, 0x0
  };
  private static final int[][] TRIANGLE_TABLE = 
    {{-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
    {0, 8, 3, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
    {0, 1, 9, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
    {1, 8, 3, 9, 8, 1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
    {1, 2, 10, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
    {0, 8, 3, 1, 2, 10, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
    {9, 2, 10, 0, 2, 9, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
    {2, 8, 3, 2, 10, 8, 10, 9, 8, -1, -1, -1, -1, -1, -1, -1},
    {3, 11, 2, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
    {0, 11, 2, 8, 11, 0, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
    {1, 9, 0, 2, 3, 11, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
    {1, 11, 2, 1, 9, 11, 9, 8, 11, -1, -1, -1, -1, -1, -1, -1},
    {3, 10, 1, 11, 10, 3, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
    {0, 10, 1, 0, 8, 10, 8, 11, 10, -1, -1, -1, -1, -1, -1, -1},
    {3, 9, 0, 3, 11, 9, 11, 10, 9, -1, -1, -1, -1, -1, -1, -1},
    {9, 8, 10, 10, 8, 11, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
    {4, 7, 8, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
    {4, 3, 0, 7, 3, 4, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
    {0, 1, 9, 8, 4, 7, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
    {4, 1, 9, 4, 7, 1, 7, 3, 1, -1, -1, -1, -1, -1, -1, -1},
    {1, 2, 10, 8, 4, 7, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
    {3, 4, 7, 3, 0, 4, 1, 2, 10, -1, -1, -1, -1, -1, -1, -1},
    {9, 2, 10, 9, 0, 2, 8, 4, 7, -1, -1, -1, -1, -1, -1, -1},
    {2, 10, 9, 2, 9, 7, 2, 7, 3, 7, 9, 4, -1, -1, -1, -1},
    {8, 4, 7, 3, 11, 2, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
    {11, 4, 7, 11, 2, 4, 2, 0, 4, -1, -1, -1, -1, -1, -1, -1},
    {9, 0, 1, 8, 4, 7, 2, 3, 11, -1, -1, -1, -1, -1, -1, -1},
    {4, 7, 11, 9, 4, 11, 9, 11, 2, 9, 2, 1, -1, -1, -1, -1},
    {3, 10, 1, 3, 11, 10, 7, 8, 4, -1, -1, -1, -1, -1, -1, -1},
    {1, 11, 10, 1, 4, 11, 1, 0, 4, 7, 11, 4, -1, -1, -1, -1},
    {4, 7, 8, 9, 0, 11, 9, 11, 10, 11, 0, 3, -1, -1, -1, -1},
    {4, 7, 11, 4, 11, 9, 9, 11, 10, -1, -1, -1, -1, -1, -1, -1},
    {9, 5, 4, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
    {9, 5, 4, 0, 8, 3, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
    {0, 5, 4, 1, 5, 0, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
    {8, 5, 4, 8, 3, 5, 3, 1, 5, -1, -1, -1, -1, -1, -1, -1},
    {1, 2, 10, 9, 5, 4, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
    {3, 0, 8, 1, 2, 10, 4, 9, 5, -1, -1, -1, -1, -1, -1, -1},
    {5, 2, 10, 5, 4, 2, 4, 0, 2, -1, -1, -1, -1, -1, -1, -1},
    {2, 10, 5, 3, 2, 5, 3, 5, 4, 3, 4, 8, -1, -1, -1, -1},
    {9, 5, 4, 2, 3, 11, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
    {0, 11, 2, 0, 8, 11, 4, 9, 5, -1, -1, -1, -1, -1, -1, -1},
    {0, 5, 4, 0, 1, 5, 2, 3, 11, -1, -1, -1, -1, -1, -1, -1},
    {2, 1, 5, 2, 5, 8, 2, 8, 11, 4, 8, 5, -1, -1, -1, -1},
    {10, 3, 11, 10, 1, 3, 9, 5, 4, -1, -1, -1, -1, -1, -1, -1},
    {4, 9, 5, 0, 8, 1, 8, 10, 1, 8, 11, 10, -1, -1, -1, -1},
    {5, 4, 0, 5, 0, 11, 5, 11, 10, 11, 0, 3, -1, -1, -1, -1},
    {5, 4, 8, 5, 8, 10, 10, 8, 11, -1, -1, -1, -1, -1, -1, -1},
    {9, 7, 8, 5, 7, 9, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
    {9, 3, 0, 9, 5, 3, 5, 7, 3, -1, -1, -1, -1, -1, -1, -1},
    {0, 7, 8, 0, 1, 7, 1, 5, 7, -1, -1, -1, -1, -1, -1, -1},
    {1, 5, 3, 3, 5, 7, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
    {9, 7, 8, 9, 5, 7, 10, 1, 2, -1, -1, -1, -1, -1, -1, -1},
    {10, 1, 2, 9, 5, 0, 5, 3, 0, 5, 7, 3, -1, -1, -1, -1},
    {8, 0, 2, 8, 2, 5, 8, 5, 7, 10, 5, 2, -1, -1, -1, -1},
    {2, 10, 5, 2, 5, 3, 3, 5, 7, -1, -1, -1, -1, -1, -1, -1},
    {7, 9, 5, 7, 8, 9, 3, 11, 2, -1, -1, -1, -1, -1, -1, -1},
    {9, 5, 7, 9, 7, 2, 9, 2, 0, 2, 7, 11, -1, -1, -1, -1},
    {2, 3, 11, 0, 1, 8, 1, 7, 8, 1, 5, 7, -1, -1, -1, -1},
    {11, 2, 1, 11, 1, 7, 7, 1, 5, -1, -1, -1, -1, -1, -1, -1},
    {9, 5, 8, 8, 5, 7, 10, 1, 3, 10, 3, 11, -1, -1, -1, -1},
    {5, 7, 0, 5, 0, 9, 7, 11, 0, 1, 0, 10, 11, 10, 0, -1},
    {11, 10, 0, 11, 0, 3, 10, 5, 0, 8, 0, 7, 5, 7, 0, -1},
    {11, 10, 5, 7, 11, 5, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
    {10, 6, 5, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
    {0, 8, 3, 5, 10, 6, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
    {9, 0, 1, 5, 10, 6, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
    {1, 8, 3, 1, 9, 8, 5, 10, 6, -1, -1, -1, -1, -1, -1, -1},
    {1, 6, 5, 2, 6, 1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
    {1, 6, 5, 1, 2, 6, 3, 0, 8, -1, -1, -1, -1, -1, -1, -1},
    {9, 6, 5, 9, 0, 6, 0, 2, 6, -1, -1, -1, -1, -1, -1, -1},
    {5, 9, 8, 5, 8, 2, 5, 2, 6, 3, 2, 8, -1, -1, -1, -1},
    {2, 3, 11, 10, 6, 5, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
    {11, 0, 8, 11, 2, 0, 10, 6, 5, -1, -1, -1, -1, -1, -1, -1},
    {0, 1, 9, 2, 3, 11, 5, 10, 6, -1, -1, -1, -1, -1, -1, -1},
    {5, 10, 6, 1, 9, 2, 9, 11, 2, 9, 8, 11, -1, -1, -1, -1},
    {6, 3, 11, 6, 5, 3, 5, 1, 3, -1, -1, -1, -1, -1, -1, -1},
    {0, 8, 11, 0, 11, 5, 0, 5, 1, 5, 11, 6, -1, -1, -1, -1},
    {3, 11, 6, 0, 3, 6, 0, 6, 5, 0, 5, 9, -1, -1, -1, -1},
    {6, 5, 9, 6, 9, 11, 11, 9, 8, -1, -1, -1, -1, -1, -1, -1},
    {5, 10, 6, 4, 7, 8, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
    {4, 3, 0, 4, 7, 3, 6, 5, 10, -1, -1, -1, -1, -1, -1, -1},
    {1, 9, 0, 5, 10, 6, 8, 4, 7, -1, -1, -1, -1, -1, -1, -1},
    {10, 6, 5, 1, 9, 7, 1, 7, 3, 7, 9, 4, -1, -1, -1, -1},
    {6, 1, 2, 6, 5, 1, 4, 7, 8, -1, -1, -1, -1, -1, -1, -1},
    {1, 2, 5, 5, 2, 6, 3, 0, 4, 3, 4, 7, -1, -1, -1, -1},
    {8, 4, 7, 9, 0, 5, 0, 6, 5, 0, 2, 6, -1, -1, -1, -1},
    {7, 3, 9, 7, 9, 4, 3, 2, 9, 5, 9, 6, 2, 6, 9, -1},
    {3, 11, 2, 7, 8, 4, 10, 6, 5, -1, -1, -1, -1, -1, -1, -1},
    {5, 10, 6, 4, 7, 2, 4, 2, 0, 2, 7, 11, -1, -1, -1, -1},
    {0, 1, 9, 4, 7, 8, 2, 3, 11, 5, 10, 6, -1, -1, -1, -1},
    {9, 2, 1, 9, 11, 2, 9, 4, 11, 7, 11, 4, 5, 10, 6, -1},
    {8, 4, 7, 3, 11, 5, 3, 5, 1, 5, 11, 6, -1, -1, -1, -1},
    {5, 1, 11, 5, 11, 6, 1, 0, 11, 7, 11, 4, 0, 4, 11, -1},
    {0, 5, 9, 0, 6, 5, 0, 3, 6, 11, 6, 3, 8, 4, 7, -1},
    {6, 5, 9, 6, 9, 11, 4, 7, 9, 7, 11, 9, -1, -1, -1, -1},
    {10, 4, 9, 6, 4, 10, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
    {4, 10, 6, 4, 9, 10, 0, 8, 3, -1, -1, -1, -1, -1, -1, -1},
    {10, 0, 1, 10, 6, 0, 6, 4, 0, -1, -1, -1, -1, -1, -1, -1},
    {8, 3, 1, 8, 1, 6, 8, 6, 4, 6, 1, 10, -1, -1, -1, -1},
    {1, 4, 9, 1, 2, 4, 2, 6, 4, -1, -1, -1, -1, -1, -1, -1},
    {3, 0, 8, 1, 2, 9, 2, 4, 9, 2, 6, 4, -1, -1, -1, -1},
    {0, 2, 4, 4, 2, 6, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
    {8, 3, 2, 8, 2, 4, 4, 2, 6, -1, -1, -1, -1, -1, -1, -1},
    {10, 4, 9, 10, 6, 4, 11, 2, 3, -1, -1, -1, -1, -1, -1, -1},
    {0, 8, 2, 2, 8, 11, 4, 9, 10, 4, 10, 6, -1, -1, -1, -1},
    {3, 11, 2, 0, 1, 6, 0, 6, 4, 6, 1, 10, -1, -1, -1, -1},
    {6, 4, 1, 6, 1, 10, 4, 8, 1, 2, 1, 11, 8, 11, 1, -1},
    {9, 6, 4, 9, 3, 6, 9, 1, 3, 11, 6, 3, -1, -1, -1, -1},
    {8, 11, 1, 8, 1, 0, 11, 6, 1, 9, 1, 4, 6, 4, 1, -1},
    {3, 11, 6, 3, 6, 0, 0, 6, 4, -1, -1, -1, -1, -1, -1, -1},
    {6, 4, 8, 11, 6, 8, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
    {7, 10, 6, 7, 8, 10, 8, 9, 10, -1, -1, -1, -1, -1, -1, -1},
    {0, 7, 3, 0, 10, 7, 0, 9, 10, 6, 7, 10, -1, -1, -1, -1},
    {10, 6, 7, 1, 10, 7, 1, 7, 8, 1, 8, 0, -1, -1, -1, -1},
    {10, 6, 7, 10, 7, 1, 1, 7, 3, -1, -1, -1, -1, -1, -1, -1},
    {1, 2, 6, 1, 6, 8, 1, 8, 9, 8, 6, 7, -1, -1, -1, -1},
    {2, 6, 9, 2, 9, 1, 6, 7, 9, 0, 9, 3, 7, 3, 9, -1},
    {7, 8, 0, 7, 0, 6, 6, 0, 2, -1, -1, -1, -1, -1, -1, -1},
    {7, 3, 2, 6, 7, 2, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
    {2, 3, 11, 10, 6, 8, 10, 8, 9, 8, 6, 7, -1, -1, -1, -1},
    {2, 0, 7, 2, 7, 11, 0, 9, 7, 6, 7, 10, 9, 10, 7, -1},
    {1, 8, 0, 1, 7, 8, 1, 10, 7, 6, 7, 10, 2, 3, 11, -1},
    {11, 2, 1, 11, 1, 7, 10, 6, 1, 6, 7, 1, -1, -1, -1, -1},
    {8, 9, 6, 8, 6, 7, 9, 1, 6, 11, 6, 3, 1, 3, 6, -1},
    {0, 9, 1, 11, 6, 7, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
    {7, 8, 0, 7, 0, 6, 3, 11, 0, 11, 6, 0, -1, -1, -1, -1},
    {7, 11, 6, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
    {7, 6, 11, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
    {3, 0, 8, 11, 7, 6, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
    {0, 1, 9, 11, 7, 6, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
    {8, 1, 9, 8, 3, 1, 11, 7, 6, -1, -1, -1, -1, -1, -1, -1},
    {10, 1, 2, 6, 11, 7, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
    {1, 2, 10, 3, 0, 8, 6, 11, 7, -1, -1, -1, -1, -1, -1, -1},
    {2, 9, 0, 2, 10, 9, 6, 11, 7, -1, -1, -1, -1, -1, -1, -1},
    {6, 11, 7, 2, 10, 3, 10, 8, 3, 10, 9, 8, -1, -1, -1, -1},
    {7, 2, 3, 6, 2, 7, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
    {7, 0, 8, 7, 6, 0, 6, 2, 0, -1, -1, -1, -1, -1, -1, -1},
    {2, 7, 6, 2, 3, 7, 0, 1, 9, -1, -1, -1, -1, -1, -1, -1},
    {1, 6, 2, 1, 8, 6, 1, 9, 8, 8, 7, 6, -1, -1, -1, -1},
    {10, 7, 6, 10, 1, 7, 1, 3, 7, -1, -1, -1, -1, -1, -1, -1},
    {10, 7, 6, 1, 7, 10, 1, 8, 7, 1, 0, 8, -1, -1, -1, -1},
    {0, 3, 7, 0, 7, 10, 0, 10, 9, 6, 10, 7, -1, -1, -1, -1},
    {7, 6, 10, 7, 10, 8, 8, 10, 9, -1, -1, -1, -1, -1, -1, -1},
    {6, 8, 4, 11, 8, 6, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
    {3, 6, 11, 3, 0, 6, 0, 4, 6, -1, -1, -1, -1, -1, -1, -1},
    {8, 6, 11, 8, 4, 6, 9, 0, 1, -1, -1, -1, -1, -1, -1, -1},
    {9, 4, 6, 9, 6, 3, 9, 3, 1, 11, 3, 6, -1, -1, -1, -1},
    {6, 8, 4, 6, 11, 8, 2, 10, 1, -1, -1, -1, -1, -1, -1, -1},
    {1, 2, 10, 3, 0, 11, 0, 6, 11, 0, 4, 6, -1, -1, -1, -1},
    {4, 11, 8, 4, 6, 11, 0, 2, 9, 2, 10, 9, -1, -1, -1, -1},
    {10, 9, 3, 10, 3, 2, 9, 4, 3, 11, 3, 6, 4, 6, 3, -1},
    {8, 2, 3, 8, 4, 2, 4, 6, 2, -1, -1, -1, -1, -1, -1, -1},
    {0, 4, 2, 4, 6, 2, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
    {1, 9, 0, 2, 3, 4, 2, 4, 6, 4, 3, 8, -1, -1, -1, -1},
    {1, 9, 4, 1, 4, 2, 2, 4, 6, -1, -1, -1, -1, -1, -1, -1},
    {8, 1, 3, 8, 6, 1, 8, 4, 6, 6, 10, 1, -1, -1, -1, -1},
    {10, 1, 0, 10, 0, 6, 6, 0, 4, -1, -1, -1, -1, -1, -1, -1},
    {4, 6, 3, 4, 3, 8, 6, 10, 3, 0, 3, 9, 10, 9, 3, -1},
    {10, 9, 4, 6, 10, 4, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
    {4, 9, 5, 7, 6, 11, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
    {0, 8, 3, 4, 9, 5, 11, 7, 6, -1, -1, -1, -1, -1, -1, -1},
    {5, 0, 1, 5, 4, 0, 7, 6, 11, -1, -1, -1, -1, -1, -1, -1},
    {11, 7, 6, 8, 3, 4, 3, 5, 4, 3, 1, 5, -1, -1, -1, -1},
    {9, 5, 4, 10, 1, 2, 7, 6, 11, -1, -1, -1, -1, -1, -1, -1},
    {6, 11, 7, 1, 2, 10, 0, 8, 3, 4, 9, 5, -1, -1, -1, -1},
    {7, 6, 11, 5, 4, 10, 4, 2, 10, 4, 0, 2, -1, -1, -1, -1},
    {3, 4, 8, 3, 5, 4, 3, 2, 5, 10, 5, 2, 11, 7, 6, -1},
    {7, 2, 3, 7, 6, 2, 5, 4, 9, -1, -1, -1, -1, -1, -1, -1},
    {9, 5, 4, 0, 8, 6, 0, 6, 2, 6, 8, 7, -1, -1, -1, -1},
    {3, 6, 2, 3, 7, 6, 1, 5, 0, 5, 4, 0, -1, -1, -1, -1},
    {6, 2, 8, 6, 8, 7, 2, 1, 8, 4, 8, 5, 1, 5, 8, -1},
    {9, 5, 4, 10, 1, 6, 1, 7, 6, 1, 3, 7, -1, -1, -1, -1},
    {1, 6, 10, 1, 7, 6, 1, 0, 7, 8, 7, 0, 9, 5, 4, -1},
    {4, 0, 10, 4, 10, 5, 0, 3, 10, 6, 10, 7, 3, 7, 10, -1},
    {7, 6, 10, 7, 10, 8, 5, 4, 10, 4, 8, 10, -1, -1, -1, -1},
    {6, 9, 5, 6, 11, 9, 11, 8, 9, -1, -1, -1, -1, -1, -1, -1},
    {3, 6, 11, 0, 6, 3, 0, 5, 6, 0, 9, 5, -1, -1, -1, -1},
    {0, 11, 8, 0, 5, 11, 0, 1, 5, 5, 6, 11, -1, -1, -1, -1},
    {6, 11, 3, 6, 3, 5, 5, 3, 1, -1, -1, -1, -1, -1, -1, -1},
    {1, 2, 10, 9, 5, 11, 9, 11, 8, 11, 5, 6, -1, -1, -1, -1},
    {0, 11, 3, 0, 6, 11, 0, 9, 6, 5, 6, 9, 1, 2, 10, -1},
    {11, 8, 5, 11, 5, 6, 8, 0, 5, 10, 5, 2, 0, 2, 5, -1},
    {6, 11, 3, 6, 3, 5, 2, 10, 3, 10, 5, 3, -1, -1, -1, -1},
    {5, 8, 9, 5, 2, 8, 5, 6, 2, 3, 8, 2, -1, -1, -1, -1},
    {9, 5, 6, 9, 6, 0, 0, 6, 2, -1, -1, -1, -1, -1, -1, -1},
    {1, 5, 8, 1, 8, 0, 5, 6, 8, 3, 8, 2, 6, 2, 8, -1},
    {1, 5, 6, 2, 1, 6, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
    {1, 3, 6, 1, 6, 10, 3, 8, 6, 5, 6, 9, 8, 9, 6, -1},
    {10, 1, 0, 10, 0, 6, 9, 5, 0, 5, 6, 0, -1, -1, -1, -1},
    {0, 3, 8, 5, 6, 10, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
    {10, 5, 6, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
    {11, 5, 10, 7, 5, 11, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
    {11, 5, 10, 11, 7, 5, 8, 3, 0, -1, -1, -1, -1, -1, -1, -1},
    {5, 11, 7, 5, 10, 11, 1, 9, 0, -1, -1, -1, -1, -1, -1, -1},
    {10, 7, 5, 10, 11, 7, 9, 8, 1, 8, 3, 1, -1, -1, -1, -1},
    {11, 1, 2, 11, 7, 1, 7, 5, 1, -1, -1, -1, -1, -1, -1, -1},
    {0, 8, 3, 1, 2, 7, 1, 7, 5, 7, 2, 11, -1, -1, -1, -1},
    {9, 7, 5, 9, 2, 7, 9, 0, 2, 2, 11, 7, -1, -1, -1, -1},
    {7, 5, 2, 7, 2, 11, 5, 9, 2, 3, 2, 8, 9, 8, 2, -1},
    {2, 5, 10, 2, 3, 5, 3, 7, 5, -1, -1, -1, -1, -1, -1, -1},
    {8, 2, 0, 8, 5, 2, 8, 7, 5, 10, 2, 5, -1, -1, -1, -1},
    {9, 0, 1, 5, 10, 3, 5, 3, 7, 3, 10, 2, -1, -1, -1, -1},
    {9, 8, 2, 9, 2, 1, 8, 7, 2, 10, 2, 5, 7, 5, 2, -1},
    {1, 3, 5, 3, 7, 5, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
    {0, 8, 7, 0, 7, 1, 1, 7, 5, -1, -1, -1, -1, -1, -1, -1},
    {9, 0, 3, 9, 3, 5, 5, 3, 7, -1, -1, -1, -1, -1, -1, -1},
    {9, 8, 7, 5, 9, 7, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
    {5, 8, 4, 5, 10, 8, 10, 11, 8, -1, -1, -1, -1, -1, -1, -1},
    {5, 0, 4, 5, 11, 0, 5, 10, 11, 11, 3, 0, -1, -1, -1, -1},
    {0, 1, 9, 8, 4, 10, 8, 10, 11, 10, 4, 5, -1, -1, -1, -1},
    {10, 11, 4, 10, 4, 5, 11, 3, 4, 9, 4, 1, 3, 1, 4, -1},
    {2, 5, 1, 2, 8, 5, 2, 11, 8, 4, 5, 8, -1, -1, -1, -1},
    {0, 4, 11, 0, 11, 3, 4, 5, 11, 2, 11, 1, 5, 1, 11, -1},
    {0, 2, 5, 0, 5, 9, 2, 11, 5, 4, 5, 8, 11, 8, 5, -1},
    {9, 4, 5, 2, 11, 3, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
    {2, 5, 10, 3, 5, 2, 3, 4, 5, 3, 8, 4, -1, -1, -1, -1},
    {5, 10, 2, 5, 2, 4, 4, 2, 0, -1, -1, -1, -1, -1, -1, -1},
    {3, 10, 2, 3, 5, 10, 3, 8, 5, 4, 5, 8, 0, 1, 9, -1},
    {5, 10, 2, 5, 2, 4, 1, 9, 2, 9, 4, 2, -1, -1, -1, -1},
    {8, 4, 5, 8, 5, 3, 3, 5, 1, -1, -1, -1, -1, -1, -1, -1},
    {0, 4, 5, 1, 0, 5, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
    {8, 4, 5, 8, 5, 3, 9, 0, 5, 0, 3, 5, -1, -1, -1, -1},
    {9, 4, 5, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
    {4, 11, 7, 4, 9, 11, 9, 10, 11, -1, -1, -1, -1, -1, -1, -1},
    {0, 8, 3, 4, 9, 7, 9, 11, 7, 9, 10, 11, -1, -1, -1, -1},
    {1, 10, 11, 1, 11, 4, 1, 4, 0, 7, 4, 11, -1, -1, -1, -1},
    {3, 1, 4, 3, 4, 8, 1, 10, 4, 7, 4, 11, 10, 11, 4, -1},
    {4, 11, 7, 9, 11, 4, 9, 2, 11, 9, 1, 2, -1, -1, -1, -1},
    {9, 7, 4, 9, 11, 7, 9, 1, 11, 2, 11, 1, 0, 8, 3, -1},
    {11, 7, 4, 11, 4, 2, 2, 4, 0, -1, -1, -1, -1, -1, -1, -1},
    {11, 7, 4, 11, 4, 2, 8, 3, 4, 3, 2, 4, -1, -1, -1, -1},
    {2, 9, 10, 2, 7, 9, 2, 3, 7, 7, 4, 9, -1, -1, -1, -1},
    {9, 10, 7, 9, 7, 4, 10, 2, 7, 8, 7, 0, 2, 0, 7, -1},
    {3, 7, 10, 3, 10, 2, 7, 4, 10, 1, 10, 0, 4, 0, 10, -1},
    {1, 10, 2, 8, 7, 4, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
    {4, 9, 1, 4, 1, 7, 7, 1, 3, -1, -1, -1, -1, -1, -1, -1},
    {4, 9, 1, 4, 1, 7, 0, 8, 1, 8, 7, 1, -1, -1, -1, -1},
    {4, 0, 3, 7, 4, 3, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
    {4, 8, 7, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
    {9, 10, 8, 10, 11, 8, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
    {3, 0, 9, 3, 9, 11, 11, 9, 10, -1, -1, -1, -1, -1, -1, -1},
    {0, 1, 10, 0, 10, 8, 8, 10, 11, -1, -1, -1, -1, -1, -1, -1},
    {3, 1, 10, 11, 3, 10, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
    {1, 2, 11, 1, 11, 9, 9, 11, 8, -1, -1, -1, -1, -1, -1, -1},
    {3, 0, 9, 3, 9, 11, 1, 2, 9, 2, 11, 9, -1, -1, -1, -1},
    {0, 2, 11, 8, 0, 11, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
    {3, 2, 11, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
    {2, 3, 8, 2, 8, 10, 10, 8, 9, -1, -1, -1, -1, -1, -1, -1},
    {9, 10, 2, 0, 9, 2, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
    {2, 3, 8, 2, 8, 10, 0, 1, 8, 1, 10, 8, -1, -1, -1, -1},
    {1, 10, 2, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
    {1, 3, 8, 9, 1, 8, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
    {0, 9, 1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
    {0, 3, 8, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
    {-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1}
  };

}