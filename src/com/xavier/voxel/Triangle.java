package com.xavier.voxel;

public class Triangle {

  private Vertex[] vertices;
  private Vector3D normal;
  private boolean destroyed;

  public Triangle() {
    this.destroyed = false;
    this.normal = new Vector3D();
    this.vertices = new Vertex[3];
  }

  @Override
  public boolean equals(Object object) throws ClassCastException {
    boolean eq = true;
    Triangle t = (Triangle)object;
    for(int i = 0; i < 3; i++) {
      if(t.vertices[i] != null && vertices[i] != null)
        eq &= t.vertices[i].getIndex() == vertices[i].getIndex();
      else
        return false;
    }
    return eq;
  }

  @Override
  public int hashCode() {
    int hash = 0;
    for(int i = 0; i < 3; i++)
      if(this.vertices[i] != null)
        hash += this.vertices[i].hashCode()*(i+1);
    return hash;
  }

  public void setVertices(Vertex[] vertices) {
    this.vertices = vertices;
  }
  public void setVertex(int index, Vertex vertex) {
    this.vertices[index] = vertex;
  }
  public Vertex getVertex(int index) {
    return this.vertices[index];
  }
  public Vertex[] getVertices() {
    return this.vertices;
  }

  public void setNormal(Vector3D normal) {
    this.normal = normal;
  }
  public Vector3D getNormal() {
    return this.normal;
  }

  public void setDestroyed(boolean destroyed) {
    this.destroyed = destroyed;
  }
  public boolean isDestroyed() {
    return this.destroyed;
  }

  public void computeNormal() {
    this.normal = vertices[1].getPosition().sub(vertices[0].getPosition())
      .cross(vertices[2].getPosition().sub(vertices[0].getPosition()))
      .normalize();
  }

  public Vector3D getNormalFrom(Vertex v) {
    Vector3D n = new Vector3D();

    if(vertices[0].equals(v)) {
      n = vertices[1].getPosition().sub(vertices[0].getPosition())
        .cross(vertices[2].getPosition().sub(vertices[0].getPosition()));
    }
    else if(vertices[1].equals(v)) {
      n = vertices[0].getPosition().sub(vertices[1].getPosition())
        .cross(vertices[2].getPosition().sub(vertices[1].getPosition()));
    }
    else if(vertices[2].equals(v)) {
      n = vertices[0].getPosition().sub(vertices[2].getPosition())
        .cross(vertices[1].getPosition().sub(vertices[2].getPosition()));
    }
    return n;
  }

  public void updateNeighbours() {
    vertices[0].addNeighbour(vertices[1]);
    vertices[0].addNeighbour(vertices[2]);

    vertices[1].addNeighbour(vertices[0]);
    vertices[1].addNeighbour(vertices[2]);

    vertices[2].addNeighbour(vertices[0]);
    vertices[2].addNeighbour(vertices[1]);
    /*for(int i = 0; i < 3; i++) {
      vertices[i].addNeighbour(vertices[(i+1)%3]);
      vertices[i].addNeighbour(vertices[(i+2)%3]);
    }*/
  }

  public void replaceVertex(Vertex u, Vertex v) {
    for(int i = 0; i < 3; i++) {
      if(vertices[i].equals(u)) {
        vertices[i] = v;
        v.addFace(this);
        this.updateNeighbours();
        this.computeNormal();
        break;
      }
    }
  }

  public boolean hasVertex(Vertex v) {
    for(int i = 0; i < 3; i++) {
      if(vertices[i].equals(v))
        return true;
    }
    return false;
  }

  public float[] getCoords() {
    float coords[] = new float[9];
    for(int i = 0; i < vertices.length; i++) {
      coords[i*3+0] = vertices[i].getPosition().x;
      coords[i*3+1] = vertices[i].getPosition().y;
      coords[i*3+2] = vertices[i].getPosition().z;
    }
    return coords;
  }
}