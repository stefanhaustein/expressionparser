package org.kobjects.expressionparser;

import java.util.List;

/**
 * Called by the expression parser, needs to be implemented by the user. May process
 * the expressions directly or build a tree. Abstract class instead of an interface
 * to avoid the need to implement methods that never trigger for a given syntax.
 */
public class Processor<T> {

  /** Called when an argument list with the given base, opening bracket and elements is parsed. */
  public T apply(Tokenizer tokenizer, T base, String bracket, List<T> arguments) {
    throw new UnsupportedOperationException(
        "apply(" + base+ ", " + bracket + ", " + arguments + ")");
  }

  /**
   * Called when a bracket registered for calls following an identifier is parsed.
   * Useful to avoid apply() in simple cases, see calculator example.
   */
  public T call(Tokenizer tokenizer, String identifier, String bracket, List<T> arguments) {
    throw new UnsupportedOperationException(
        "call(" + identifier + ", " + bracket + ", " + arguments + ")");
  }

  /** Called when a group with the given opening bracket and elements is parsed. */
  public T group(Tokenizer tokenizer, String paren, List<T> elements) {
    throw new UnsupportedOperationException("group(" + paren + ", " + elements + ')');
  }

  /** Called when the given identifier is parsed. */
  public T identifier(Tokenizer tokenizer, String name) {
    throw new UnsupportedOperationException("identifier(" + name + ')');
  }

  /** Called when an implicit operator is parsed. */
  public T implicitOperator(Tokenizer tokenizer, boolean strong, T left, T right) {
    throw new UnsupportedOperationException("implicitOperator(" + left + ", " + right + ')');
  }

  /** Called when an infix operator with the given name is parsed. */
  public T infixOperator(Tokenizer tokenizer, String name, T left, T right) {
    throw new UnsupportedOperationException("infixOperator(" + name + ", " + left + ", " + right + ')');
  }

  /** Called when the given number literal is parsed. */
  public T numberLiteral(Tokenizer tokenizer, String value) {
    throw new UnsupportedOperationException("numberLiteral(" + value + ")");
  }

  /** Called when a prefix operator with the given name is parsed. */
  public T prefixOperator(Tokenizer tokenizer, String name, T argument) {
    throw new UnsupportedOperationException("prefixOperator(" + name + ", " + argument + ')');
  }

  /**
   * Called when a primary symbol is parsed (e.g. the empty set symbol in the set demo).
   */
  public T primary(Tokenizer tokenizer, String name) {
    throw new UnsupportedOperationException("primary(" + name + ", " + tokenizer + ")");
  }

  /** Called when a suffix operator with the given name is parsed. */
  public T suffixOperator(Tokenizer tokenizer, String name, T argument) {
    throw new UnsupportedOperationException("suffixOperator(" + name + ", " + argument + ')');
  }

  /** 
   * Called when the given (quoted) string literal is parsed.
   * The string is handed in in its original quoted form; use ExpressionParser.unquote()
   * to unquote and unescape the string.
   */
  public T stringLiteral(Tokenizer tokenizer, String value) {
    throw new UnsupportedOperationException("stringLiteral(" + value + ')');
  }

  /**
   * Called for ternaryOperator operators.
   */
  public T ternaryOperator(Tokenizer tokenizer, String operator, T left, T middle, T right) {
    throw new UnsupportedOperationException("ternaryOperator(" + operator + ')');
  }
}
