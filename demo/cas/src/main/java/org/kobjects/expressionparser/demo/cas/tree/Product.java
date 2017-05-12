package org.kobjects.expressionparser.demo.cas.tree;

import org.kobjects.expressionparser.demo.cas.string2d.String2d;

import java.util.Map;
import java.util.Set;

class Product extends QuantifiedComponents {

  /**
   * Used by base class for simplifications and substitutions.
   */
  @Override
  Node create(double c, QuantifiedSet<Node> components) {
    return new Product(c, components);
  }

  public static String2d toString2d(Stringify type, double c, Node factor) {
    String2d f2d = factor.embrace(type, PRECEDENCE_ADDITIVE);
    if ((c == 1 || c == -1) && type != Stringify.VERBOSE) {
      return f2d;
    }
    String2d.Builder sb = new String2d.Builder();
    sb.append(Constant.toString(c));

    String fss = f2d.toString() + "  ";
    if (type == Stringify.VERBOSE || f2d.height() != 1
        || !Character.isLetter(fss.charAt(0)) || Character.isLetter(fss.charAt(1))) {
      sb.append("⋅");
    }
    sb.append(f2d);
    return sb.build();
  }

  Product(double c, QuantifiedSet<Node> factors) {
    super(c, factors, 1);
  }

  @Override
  public Node simplify(Set<String> explanation) {
    Node s = super.simplify(explanation);
    if (s != this) {
      return s;
    }

    double cc = c;
    QuantifiedSet.Mutable<Node> simplified = new QuantifiedSet.Mutable<>(true);

    // See what we can inline / de-duplicate
    for (Map.Entry<Node, Double> entry : components.entries()) {
      Node node = entry.getKey();
      double exponent = entry.getValue();
      if (node instanceof Power && ((Power) node).exponent instanceof Constant) {
        simplified.add(exponent * ((Constant) ((Power) node).exponent).value, ((Power) node).base);
      } else if (node instanceof Constant) {
        cc *= Math.pow(((Constant) node).value, exponent);
      } else if ((node instanceof Sum) && ((Sum) node).c == 0 && ((Sum) node).components.size() == 1) {
        Map.Entry<Node, Double> summand = ((Sum) node).components.entries().iterator().next();
        cc *= summand.getValue();
        simplified.add(exponent, summand.getKey());
      } else {
        simplified.add(exponent, node);
      }
    }

    // Just 0
    if (cc == 0) {
      return new Constant(cc);
    }

    // Constant factor only?
    if (simplified.size() == 1) {
      Map.Entry<Node, Double> entry = simplified.entries().iterator().next();
      if (entry.getValue() == 1.0) {
        return NodeFactory.cMul(cc, entry.getKey());
      }
    }

    return new Product(cc, simplified);
  }


  @Override
  public String2d toString2d(Stringify type) {
    String2d.Builder top = new String2d.Builder();
    String2d.Builder bottom = new String2d.Builder();

    if (c != 1 || type == Stringify.VERBOSE) {
      top.append(Constant.toString(c));
    }

    if (type == Stringify.VERBOSE && components.size() == 0) {
      top.append("⋅1");
    }

    for (Map.Entry<Node,Double> entry: components.entries()) {
      Node node = entry.getKey();
      double exponent = entry.getValue();
      String2d.Builder target = exponent >= 0 ? top : bottom;
      if (!target.isEmpty()) {
        target.append("⋅");
      }
      target.append(Power.toString2d(type, node, Math.abs(exponent)));
    }

    if (top.isEmpty()) {
      top.append("1");
    }

    if (bottom.isEmpty()) {
      return top.build();
    }
    if (type == Stringify.BLOCK){
      return String2d.stack(String2d.HorizontalAlign.CENTER, 1,
          top.build(),
          String2d.hline(Math.max(top.length(), bottom.length())),
          bottom.build());
    }
    return bottom.size() == 1
        ? String2d.concat(top, "/", bottom)
        : String2d.concat(top, "/(", bottom, ")");
  }

  @Override
  public int getPrecedence() {
    return PRECEDENCE_MULTIPLICATIVE;
  }
}
