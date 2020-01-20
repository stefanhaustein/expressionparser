package org.kobjects.expressionparser;

class Symbol {
  final int precedence;
  final OperatorType type;
  final String separator;
  final String close;

  Symbol(int precedence, OperatorType type) {
    this.precedence = precedence;
    this.type = type;
    this.separator = null;
    this.close = null;
  }
  Symbol(int precedence, String separator, String close) {
    this.precedence = precedence;
    this.type = null;
    this.separator = separator;
    this.close = close;
  }
}
