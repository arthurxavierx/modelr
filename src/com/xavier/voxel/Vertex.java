package com.xavier.voxel;

import java.util.Set;
import java.util.HashSet;
import java.lang.ClassCastException;

public class Vertex {

  private int index;
  private Vector3D position;
  private Vector3D originalPosition;
  private boolean visible;

  private float collapseCost;
  private Vertex collapseVertex;

  private HashSet<Vertex> neighbours;
  private HashSet<Triangle> faces;

  private boolean destroyed;
  private boolean canRemove;

  public Vertex(int index) {
    this(index, new Vector3D());
  }

  public Vertex(int index, Vector3D position) {
    this.index = index;
    this.collapseVertex = null;
    this.collapseCost = 0.0f;
    this.visible = true;
    this.destroyed = false;
    this.canRemove = true;

    this.position = position;
    this.originalPosition = (Vector3D)position.clone();

    this.neighbours = new HashSet<Vertex>();
    this.faces = new HashSet<Triangle>();
  }

  @Override
  public boolean equals(Object object) throws ClassCastException {
    Vertex v = (Vertex)object;
    return this.index == v.index;
  }
  @Override
  public int hashCode() {
    return index;
  }

  public void set(Vector3D position, boolean visible) {
    this.position = position;
    this.visible = visible;
    this.originalPosition = position;
  }

  public void setIndex(int index) {
    this.index = index;
  }
  public int getIndex() {
    return this.index;
  }

  public float[] getCoords() {
    return new float[]{this.position.x, this.position.y, this.position.z};
  }

  public void setPosition(Vector3D position) {
    this.position = position;
  }
  public Vector3D getPosition() {
    return this.position;
  }

  public Vector3D getOriginalPosition() {
    return this.originalPosition;
  }

  public void setVisible(boolean visible) {
    this.visible = visible;
  }
  public boolean isVisible() {
    return this.visible;
  }

  public void setCollapseCost(float collapseCost) {
    this.collapseCost = collapseCost;
  }
  public float getCollapseCost() {
    return this.collapseCost;
  }

  public void setCollapseVertex(Vertex collapseVertex) {
    this.collapseVertex = collapseVertex;
  }
  public Vertex getCollapseVertex() {
    return this.collapseVertex;
  }

  public void addFace(Triangle triangle) {
    this.faces.add(triangle);
  }
  public Set<Triangle> getFaces() {
    return this.faces;
  }

  public void addNeighbour(Vertex vertex) {
    this.neighbours.add(vertex);
  }
  public Set<Vertex> getNeighbours() {
    return this.neighbours;
  }

  public void setCanRemove(boolean canRemove) {
    this.canRemove = canRemove;
  }
  public boolean canRemove() {
    return this.canRemove;
  }

  public void setDestroyed(boolean destroyed) {
    this.destroyed = destroyed;
  }
  public boolean isDestroyed() {
    return this.destroyed;
  }

}