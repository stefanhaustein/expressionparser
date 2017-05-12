package org.kobjects.expressionparser.demo.basic;

abstract class Node {
  Node[] children;

  Node(Node... children) {
    this.children = children;
  }

  abstract Object eval();

  double evalDouble(int i) {
    Object o = children[i].eval();
    if (!(o instanceof Number)) {
      throw new RuntimeException("Number expected in " + this.toString());
    }
    return ((Number) o).doubleValue();
  }

  int evalInt(int i) {
    return (int) evalDouble(i);
  }

  String evalString(int i) {
    return Interpreter.toString(children[i].eval());
  }

  public String toString() {
    if (children.length == 0) {
      return "";
    } else if (children.length == 1) {
      return children[0].toString();
    } else {
      StringBuilder sb = new StringBuilder(children[0].toString());
      for (int i = 1; i < children.length; i++) {
        sb.append(", ");
        sb.append(children[i]);
      }
      return sb.toString();
    }
  }

  abstract Class<?> returnType();
}
