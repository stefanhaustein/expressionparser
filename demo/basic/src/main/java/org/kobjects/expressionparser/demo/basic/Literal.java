package org.kobjects.expressionparser.demo.basic;

class Literal extends Node {
  Object value;

  Literal(Object value) {
    super((Node[]) null);
    this.value = value;
  }

  @Override
  public Object eval() {
    return value;
  }

  @Override
  Class<?> returnType() {
    return value.getClass();
  }

  @Override
  public String toString() {
    if (value != Interpreter.INVISIBLE_STRING && value instanceof String) {
      return "\"" + ((String) value).replace("\"", "\"\"") + '"';
    }
    return Interpreter.toString(value);
  }
}
