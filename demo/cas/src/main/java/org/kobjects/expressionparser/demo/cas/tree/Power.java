package org.kobjects.expressionparser.demo.cas.tree;

import org.kobjects.expressionparser.demo.cas.string2d.String2d;

import java.util.Set;

class Power extends Node {
  public static String2d toString2d(Stringify type, Node base, double exponent) {
    String2d base2d = base.embrace(type, PRECEDENCE_POWER);
    if (type != Stringify.VERBOSE) {
      if (exponent == 1 && type != Stringify.VERBOSE) {
        return base2d;
      }
      if (exponent > 0 && exponent < 10 && exponent == (int) exponent && base2d.height() == 1) {
        return String2d.concat(base2d, "⁰¹²³⁴⁵⁶⁷⁸⁹".charAt((int) exponent));
      }
    }
    return exponent2d(type, base2d, String2d.valueOf(Constant.toString(exponent)));
  }

  public static String2d exponent2d(Stringify type, String2d base, String2d exponent) {
    return type == Stringify.BLOCK
       ? String2d.concat(base, String2d.stack(String2d.HorizontalAlign.LEFT, 1, exponent, ""))
        : String2d.concat(base, "^", exponent);
  }

  final Node base;
  final Node exponent;

  public Power(Node left, Node right) {
    this.base = left;
    this.exponent = right;
  }

  @Override
  public Node simplify(Set<String> explanation) {
    Node base = this.base.simplify(explanation);
    Node exponent = this.exponent.simplify(explanation);

    if (base.equals(this.base) && exponent.equals(this.exponent)) {
      if (exponent instanceof Constant) {
        // Will turn this into a product, where additional optimization may take place.
        return NodeFactory.powC(base, ((Constant) exponent).value);
      }
      if (base instanceof Constant) {
        double leftValue = ((Constant) base).value;
        if (leftValue == 0) {
          explanation.add("base 0");
          return NodeFactory.C0;
        }
        if (leftValue == 1) {
          explanation.add("base 1");
          return NodeFactory.C1;
        }
      }
      if (base instanceof Power) {
        Power lp = (Power) base;
        return new Power(lp.base, NodeFactory.mul(lp.exponent, exponent));
      }
    }
    return new Power(base, exponent);
  }

  @Override
  public String2d toString2d(Stringify type) {
    return exponent2d(type,
        base.embrace(type, PRECEDENCE_POWER),
        exponent.embrace(type, PRECEDENCE_POWER));
  }

  @Override
  public int getPrecedence() {
    return PRECEDENCE_POWER;
  }
}
