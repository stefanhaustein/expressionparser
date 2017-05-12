package org.kobjects.expressionparser.demo.basic;

import java.util.TreeMap;

// Not static for access to the variables.
class Variable extends Node {
  final Interpreter interpreter;
  final String name;

  Variable(Interpreter interpreter, String name, Node... children) {
    super(children);
    this.interpreter = interpreter;
    this.name = name;
  }

  void set(Object value) {
    if (name.endsWith("$")) {
      value = String.valueOf(value);
    } else if (!(value instanceof Double)) {
      throw new RuntimeException("Cannot assign string to number variable " + name);
    }
    if (children.length == 0) {
      interpreter.variables.put(name, value);
      return;
    }
    TreeMap<Integer, Object> target = (TreeMap<Integer, Object>)
        interpreter.arrays[children.length - 1].get(name);
    if (target == null) {
      target = new TreeMap<>();
      interpreter.arrays[children.length - 1].put(name, target);
    }
    for (int i = 0; i < children.length - 2; i++) {
      int index = (int) evalDouble(i);
      TreeMap<Integer, Object> sub = (TreeMap<Integer, Object>) target.get(index);
      if (sub == null) {
        sub = new TreeMap<>();
        target.put(index, sub);
      }
      target = sub;
    }
    target.put((int) evalDouble(children.length - 1), value);
  }

  public Object eval() {
    Object result;
    if (children.length == 0) {
      result = interpreter.variables.get(name);
    } else {
      TreeMap<Integer, Object> arr =
          (TreeMap<Integer, Object>) interpreter.arrays[children.length - 1].get(name);
      for (int i = 0; i < children.length - 2 && arr != null; i++) {
        arr = (TreeMap<Integer, Object>) arr.get((int) evalDouble(i));
      }
      result = arr == null ? null : arr.get((int) evalDouble(children.length - 1));
    }
    return result == null ? name.endsWith("$") ? "" : 0.0 : result;
  }

  Class<?> returnType() {
    return name.endsWith("$") ? String.class : Double.class;
  }

  public String toString() {
    return children.length == 0 ? name : name + "(" + super.toString() + ")";
  }
}
