package org.kobjects.expressionparser.demo.cas.tree;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * Basically a multiset using doubles as counters.
 */
public class QuantifiedSet<T> {
  Map<T,Double> map;

  public static <T> QuantifiedSet of(Iterator<Map.Entry<T, Double>> i) {
    Mutable result = new Mutable(false);
    while (i.hasNext()) {
      Map.Entry<T, Double> entry = i.next();
      result.add(entry.getValue(), entry.getKey());
    }
    return result;
  }

  public static <T> QuantifiedSet<T> of(T[] elements) {
    Mutable result = new Mutable(false);
    for (T element : elements) {
      result.add(1, element);
    }
    return result;
  }

  private QuantifiedSet(boolean sort) {
    map = sort ? new TreeMap<T,Double>() : new LinkedHashMap<T,Double>();
  }

  double getQuantity(T element) {
    Double count = map.get(element);
    return count == null ? 0 : count;
  }

  public int size() {
    return map.size();
  }

  public Iterable<T> elements() {
    return map.keySet();
  }

  public Iterable<Map.Entry<T, Double>> entries() {
    return map.entrySet();
  }

  static class Mutable<T> extends QuantifiedSet<T> {
    Mutable(boolean sort) {
      super(sort);
    }

    public void add(double count, T element) {
      Double old = map.get(element);
      if (old != null) {
        count += old;
      }
      if (map instanceof TreeMap && count == 0) {
        map.remove(element);
      } else {
        map.put(element, count);
      }
    }

    public void addAll(Iterable<Map.Entry<T, Double>> entries) {
      for (Map.Entry<T, Double> entry: entries) {
        add(entry.getValue(), entry.getKey());
      }
    }

    public void add(Map.Entry<T, Double> entry) {
      add(entry.getValue(), entry.getKey());
    }
  }
}
