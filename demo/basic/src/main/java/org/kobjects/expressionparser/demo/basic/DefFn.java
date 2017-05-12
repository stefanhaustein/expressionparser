package org.kobjects.expressionparser.demo.basic;

class DefFn {
  Interpreter interpreter;
  String[] parameterNames;
  String name;
  Node expression;

  DefFn(Interpreter interpreter, Node assignment) {
    this.interpreter = interpreter;
    if (!(assignment instanceof Operator)
        || !((Operator) assignment).name.equals("=")
        || !(assignment.children[0] instanceof FnCall)) {
      throw new RuntimeException("SetLocal to function declaration expected.");
    }
    FnCall target = (FnCall) assignment.children[0];
    this.name = target.name;
    parameterNames = new String[target.children.length];
    for (int i = 0; i < parameterNames.length; i++) {
      Node param = target.children[i];
      if (!(param instanceof Variable) || param.children.length != 0) {
        throw new RuntimeException("parameter name expected, got " + param);
      }
      parameterNames[i] = ((Variable) param).name;
    }
    expression = assignment.children[1];
  }

  public Object eval(Object[] parameterValues) {
    Object[] saved = new Object[parameterNames.length];
    for (int i = 0; i < parameterNames.length; i++) {
      String param = parameterNames[i];
      saved[i] = interpreter.variables.get(param);
      interpreter.variables.put(param, parameterValues[i]);
    }
    try {
      return expression.eval();
    } finally {
      for (int i = 0; i < parameterNames.length; i++) {
        interpreter.variables.put(parameterNames[i], saved[i]);
      }
    }
  }
}
