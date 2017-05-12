package org.kobjects.expressionparser.demo.cas.tree;

import java.util.Map;
import java.util.Set;

public abstract class QuantifiedComponents extends Node {
  final double c;
  final QuantifiedSet<Node> components;

  /**
   * Flattens on construction so this "trivial" operations don't show in simplification.
   */
  QuantifiedComponents(double c, QuantifiedSet<Node> components, double ignore) {
    QuantifiedSet.Mutable<Node> builder = new QuantifiedSet.Mutable<>(false);
    for (Map.Entry<Node,Double> entry : components.entries()) {
      if (entry.getKey().getClass() == this.getClass()) {
        QuantifiedComponents sub = (QuantifiedComponents) entry.getKey();
        double parentFactor = entry.getValue();
        if (sub.c != ignore) {
          builder.add(parentFactor, new Constant(sub.c));
        }
        for (Map.Entry<Node,Double> subEntry : sub.components.entries()) {
          builder.add(parentFactor * subEntry.getValue(), subEntry.getKey());
        }
      } else {
        builder.add(entry);
      }
    }
    this.c = c;
    this.components = builder;
  }

  abstract Node create(double c, QuantifiedSet<Node> components);

  public Node simplify(Set<String> explanation) {
    if (components.size() == 0) {
      return new Constant(c);
    }
    boolean changed = false;
    QuantifiedSet.Mutable<Node> simplified = new QuantifiedSet.Mutable<>(false);
    for (Map.Entry<Node, Double> entry: components.entries()) {
      Node node = entry.getKey().simplify(explanation);
      changed = changed || !node.equals(entry.getKey());
      simplified.add(entry.getValue(), node);
    }
    return changed ? create(c, simplified) : this;
  }

  public Node substitute(String variable, Node replacement) {
    QuantifiedSet.Mutable<Node> builder = new QuantifiedSet.Mutable<>(false);
    for (Map.Entry<Node,Double> entry : components.entries()) {
      builder.add(entry.getValue(), entry.getKey().substitute(variable, replacement));
    }
    return create(c, builder);
  }
}
