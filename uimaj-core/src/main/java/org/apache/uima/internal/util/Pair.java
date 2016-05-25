package org.apache.uima.internal.util;

public class Pair<T, U> {

  public final T t;
  /** updatable but don't update if used as key in hashtable */
  public U u;  

  public Pair(T t, U u) {
    this.t = t;
    this.u = u;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((t == null) ? 0 : t.hashCode());
    result = prime * result + ((u == null) ? 0 : u.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    Pair other = (Pair) obj;
    if (t == null) {
      if (other.t != null) {
        return false;
      }
    } else if (!t.equals(other.t)) {
      return false;
    }
    if (u == null) {
      if (other.u != null) {
        return false;
      }
    } else if (!u.equals(other.u)) {
      return false;
    }
    return true;
  }
  
}
