package org.kobjects.expressionparser.demo.cas.tree;

/**
 * Builds nodes for the given operators. Does not perform any optimizations.
 */
public class NodeFactory {
  public static Node add(Node... nodes) {
    return new Sum(0, QuantifiedSet.of(nodes));
  }

  public static final Constant C0 = new Constant(0);
  public static final Constant C1 = new Constant(1);

  public static Node f(String name, Node param) {
    return new UnaryFunction(name, param);
  }

  public static Constant c(double c) {
    return c == 0 ? C0 : c == 1 ? C1 : new Constant(c);
  }

  public static Node sub(Node left, Node right) {
    QuantifiedSet.Mutable set = new QuantifiedSet.Mutable(false);
    set.add(1, left);
    set.add(-1, right);
    return new Sum(0, set);
  }

  public static Node neg(Node node) {
    if (node instanceof Constant) {
      return new Constant(-((Constant) node).value);
    }
    QuantifiedSet.Mutable set = new QuantifiedSet.Mutable(false);
    set.add(-1, node);
    return new Sum(0, set);
  }

  public static Node mul(Node... factors) {
    return new Product(1, QuantifiedSet.of(factors));
  }

  public static Node cMul(double factor, Node node) {
    if (factor == 1) {
      return node;
    }
    QuantifiedSet.Mutable set = new QuantifiedSet.Mutable(false);
    set.add(factor, node);
    return new Sum(0, set);
  }

  public static Node div(Node left, Node right) {
    return mul(left, rez(right));
  }

  public static Node rez(Node node) {
    return powC(node, -1);
  }

  public static Node powC(Node base, double exponent) {
    if (exponent == 1) {
      return base;
    }
    QuantifiedSet.Mutable set = new QuantifiedSet.Mutable(false);
    set.add(exponent, base);
    return new Product(1, set);
  }

  public static Node pow(Node base, Node exponent) {
    return new Power(base, exponent);
  }

  public static Node derive(Node node, String to) {
    return new Derive(node, to);
  }

  public static Node var(String name) {
    return new Variable(name);
  }
}
