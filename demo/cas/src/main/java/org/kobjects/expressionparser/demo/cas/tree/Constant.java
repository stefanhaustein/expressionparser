package org.kobjects.expressionparser.demo.cas.tree;

public class Constant extends Node {
  public final double value;

  Constant(double value) {
    this.value = value;
  }

  public static String toString(double d) {
    String s = String.valueOf(d);
    return s.endsWith(".0") ? s.substring(0, s.length() - 2) : s;
  }

  @Override
  public int getPrecedence() {
    return PRECEDENCE_PRIMARY;
  }

  @Override
  public String toString() {
    return toString(value);
  }
}
