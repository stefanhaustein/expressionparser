package org.kobjects.expressionparser.demo.cas.tree;

import org.kobjects.expressionparser.demo.cas.string2d.String2d;

import java.util.Map;
import java.util.Set;

class Sum extends QuantifiedComponents {

  Sum(double c, QuantifiedSet<Node> summands) {
    super(c, summands, 0);
  }

  @Override
  Node create(double c, QuantifiedSet<Node> components) {
    return new Sum(c, components);
  }

  @Override
  public Node simplify(Set<String> explanation) {
    Node s = super.simplify(explanation);
    if (s != this) {
      return s;
    }

    QuantifiedSet.Mutable<Node> simplified = new QuantifiedSet.Mutable<>(true);
    double cc = c;

    for (Map.Entry<Node, Double> entry : components.entries()) {
      double count = entry.getValue();
      Node node = entry.getKey();
      if (node instanceof Product && ((Product) node).c != 1) {
        // Pull up constant components.
        Product product = ((Product) node);
        simplified.add(count * product.c, new Product(1, product.components));
      } else if (node instanceof Constant) {
        cc += count * ((Constant) node).value;
      } else {
        simplified.add(count, node);
      }
    }

    // Single simple summand
    if (simplified.size() == 1 && cc == 0) {
      Map.Entry<Node, Double> entry = simplified.entries().iterator().next();
      if (entry.getValue() == 1.0) {
        return entry.getKey();
      }
    }

    return new Sum(cc, simplified);
  }

  public String2d toString2d(Stringify type) {
    String2d.Builder sb = new String2d.Builder();
    if (c != 0 || components.size() == 0 || type == Stringify.VERBOSE) {
      sb.append(Constant.toString(c));
    }
    if (type == Stringify.VERBOSE && components.size() == 0) {
      sb.append(" + 0");
    }
    for (Map.Entry<Node,Double> entry: components.entries()) {
      double count = entry.getValue();
      Node node = entry.getKey();
      sb.append(count >= 0
          ? (sb.isEmpty() ? "" : " + ")
          : (sb.isEmpty() ? "-" : " − "));
      sb.append(Product.toString2d(type, Math.abs(count), node));
    }
    return sb.build();
  }

  @Override
  public int getPrecedence() {
    return PRECEDENCE_ADDITIVE;
  }
}
