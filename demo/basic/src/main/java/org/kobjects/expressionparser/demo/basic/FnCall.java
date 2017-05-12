package org.kobjects.expressionparser.demo.basic;

// User-defined function
class FnCall extends Node {
  final Interpreter interpreter;
  final String name;

  FnCall(Interpreter interpreter, String name, Node... children) {
    super(children);
    this.interpreter = interpreter;
    this.name = name;
  }

  Object eval() {
    DefFn def = interpreter.functionDefinitions.get(name);
    if (def == null) {
      throw new RuntimeException("Undefined function: " + name);
    }
    Object[] params = new Object[children.length];
    for (int i = 0; i < params.length; i++) {
      params[i] = children[i].eval();
    }
    return def.eval(params);
  }

  Class<?> returnType() {
    return name.endsWith("$") ? String.class : Double.class;
  }

  public String toString() {
    return children.length == 0 ? name : name + "(" + super.toString() + ")";
  }
}
