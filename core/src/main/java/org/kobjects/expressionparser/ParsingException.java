package org.kobjects.expressionparser;

public class ParsingException extends RuntimeException {
  final public int start;
  final public int end;
  public ParsingException(int start, int end, String text, Exception base) {
    super(text, base);
    this.start = start;
    this.end = end;
  }
}
