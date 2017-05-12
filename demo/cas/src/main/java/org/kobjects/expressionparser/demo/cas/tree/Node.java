package org.kobjects.expressionparser.demo.cas.tree;

import org.kobjects.expressionparser.demo.cas.string2d.String2d;

import java.util.Set;

public abstract class Node implements Comparable<Node> {
  static final public int PRECEDENCE_PRIMARY = 10;
  static final public int PRECEDENCE_POWER = 5;
  static final public int PRECEDENCE_SIGNUM = 4;
  static final public int PRECEDENCE_IMPLICIT_MULTIPLICATION = 3;
  static final public int PRECEDENCE_UNARY_FUNCTION = 2;
  static final public int PRECEDENCE_MULTIPLICATIVE = 1;
  static final public int PRECEDENCE_ADDITIVE = 0;

  public enum Stringify {FLAT, VERBOSE, BLOCK}

  public Node simplify(Set<String> explanation) {
    return this;
  }

  public int getPrecedence() {
    return -1;
  }

  @Override
  public int compareTo(Node another) {
    return toString().compareTo(another.toString());
  }

  public boolean equals(Object o) {
    return (o instanceof Node) && (o.getClass() == this.getClass())
        && ((Node) o).toString2d(Stringify.VERBOSE).toString().equals(
          this.toString2d(Stringify.VERBOSE).toString());
  }

  public String toString() {
    return toString2d(Stringify.FLAT).toString();
  }

  public String2d toString2d(Stringify type) {
    return String2d.valueOf(toString());
  }

  public String2d embrace(Stringify type, int callerPrecedence) {
    if (callerPrecedence < getPrecedence()) {
      return toString2d(type);
    }
    return String2d.embrace('(', toString2d(type), ')');
  }

  public Node substitute(String var, Node replacement) {
    return this;
  }
}
