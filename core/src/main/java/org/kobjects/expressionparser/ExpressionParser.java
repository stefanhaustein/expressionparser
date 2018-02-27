package org.kobjects.expressionparser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.TreeSet;
import java.util.regex.Pattern;

/**
 * A simple configurable expression parser.
 */
public class ExpressionParser<T, C> {

  /**
   * Called by the expression parser, needs to be implemented by the user. May process
   * the expressions directly or build a tree. Abstract class instead of an interface
   * to avoid the need to implement methods that never trigger for a given syntax.
   */
  public static class Processor<T, C> {

    /** Called when an argument list with the given base, opening bracket and elements is parsed. */
    public T apply(C context, Tokenizer tokenizer, T base, String bracket, List<T> arguments) {
      throw new UnsupportedOperationException(
          "apply(" + base+ ", " + bracket + ", " + arguments + ")");
    }

    /**
     * Called when a bracket registered for calls following an identifier is parsed.
     * Useful to avoid apply() in simple cases, see calculator example.
     */
    public T call(C context, Tokenizer tokenizer, String identifier, String bracket, List<T> arguments) {
      throw new UnsupportedOperationException(
          "call(" + identifier + ", " + bracket + ", " + arguments + ")");
    }

    /** Called when a group with the given opening bracket and elements is parsed. */
    public T group(C context, Tokenizer tokenizer, String paren, List<T> elements) {
      throw new UnsupportedOperationException("group(" + paren + ", " + elements + ')');
    }

    /** Called when the given identifier is parsed. */
    public T identifier(C context, Tokenizer tokenizer, String name) {
      throw new UnsupportedOperationException("identifier(" + name + ')');
    }

    /** Called when an implicit operator is parsed. */
    public T implicitOperator(C context, Tokenizer tokenizer, boolean strong, T left, T right) {
      throw new UnsupportedOperationException("implicitOperator(" + left + ", " + right + ')');
    }

    /** Called when an infix operator with the given name is parsed. */
    public T infixOperator(C context, Tokenizer tokenizer, String name, T left, T right) {
      throw new UnsupportedOperationException("infixOperator(" + name + ", " + left + ", " + right + ')');
    }

    /** Called when the given number literal is parsed. */
    public T numberLiteral(C context, Tokenizer tokenizer, String value) {
      throw new UnsupportedOperationException("numberLiteral(" + value + ")");
    }

    /** Called when a prefix operator with the given name is parsed. */
    public T prefixOperator(C context, Tokenizer tokenizer, String name, T argument) {
      throw new UnsupportedOperationException("prefixOperator(" + name + ", " + argument + ')');
    }

    /**
     * Called when a primary symbol is parsed (e.g. the empty set symbol in the set demo).
     */
    public T primary(C context, Tokenizer tokenizer, String name) {
      throw new UnsupportedOperationException("primary(" + name + ", " + tokenizer + ")");
    }

    /** Called when a suffix operator with the given name is parsed. */
    public T suffixOperator(C context, Tokenizer tokenizer, String name, T argument) {
      throw new UnsupportedOperationException("suffixOperator(" + name + ", " + argument + ')');
    }

    /** 
     * Called when the given (quoted) string literal is parsed.
     * The string is handed in in its original quoted form; use ExpressionParser.unquote()
     * to unquote and unescape the string.
     */
    public T stringLiteral(C context, Tokenizer tokenizer, String value) {
      throw new UnsupportedOperationException("stringLiteral(" + value + ')');
    }

    /**
     * Called for ternaryOperator operators.
     */
    public T ternaryOperator(C context, Tokenizer tokenizer, String operator, T left, T middle, T right) {
      throw new UnsupportedOperationException("ternaryOperator(" + operator + ')');
    }
  }

  public enum OperatorType {
    INFIX, INFIX_RTL, PREFIX, SUFFIX
  }

  private static class Symbol{
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

  private final HashMap<String,Symbol> prefix = new HashMap<>();
  private final HashMap<String,Symbol> infix = new HashMap<>();
  private final HashSet<String> otherSymbols = new HashSet<>();
  private final HashSet<String> primary = new HashSet<>();
  private final HashMap<String, String[]> calls = new HashMap<>();
  private final HashMap<String, String[]> groups = new HashMap<>();

  private final Processor<T, C> processor;
  private int strongImplicitOperatorPrecedence = -1;
  private int weakImplicitOperatorPrecedence = -1;

  public static String unquote(String s) {
    StringBuilder sb = new StringBuilder();
    int len = s.length() - 1;
    for (int i = 1; i < len; i++) {
      char c = s.charAt(i);
      if (c == '\\') {
        c = s.charAt(++i);
        switch(c) {
          case 'b': sb.append('\b'); break;
          case 'f': sb.append('\f'); break;
          case 'n': sb.append('\n'); break;
          case 't': sb.append('\t'); break;
          case 'r': sb.append('\r'); break;
          default:
            sb.append(c);
        }
      } else {
        sb.append(c);
      }
    }
    return sb.toString();
  }

  public ExpressionParser(Processor<T, C> processor) {
    this.processor = processor;
  }

  /**
   * Adds "apply" brackets with the given precedence. Used for function calls or array element access.
   */
  public void addApplyBrackets(int precedence, String open, String separator, String close) {
    infix.put(open, new Symbol(precedence, separator, close));
    if (separator != null) {
      otherSymbols.add(separator);
    }
    otherSymbols.add(close);
  }

  /**
   * Adds "call" brackets, parsed eagerly after identifiers.
   */
  public void addCallBrackets(String open, String separator, String close) {
    calls.put(open, new String[]{separator, close});
    otherSymbols.add(open);
    if (separator != null) {
      otherSymbols.add(separator);
    }
    otherSymbols.add(close);
  }

  /**
   * Adds grouping. If the separator is null, only a single element will be permitted.
   * If the separator is empty, whitespace will be sufficient for element
   * separation. Used for parsing lists or overriding the operator precedence (typically with
   * parens and a null separator).
   */
  public void addGroupBrackets(String open, String separator, String close) {
    groups.put(open, new String[] {separator, close});
    otherSymbols.add(open);
    if (separator != null) {
      otherSymbols.add(separator);
    }
    otherSymbols.add(close);
  }

  public void addPrimary(String... names) {
    for (String name : names) {
      primary.add(name);
    }
  }

  public void addTernaryOperator(int precedence, String primaryOperator, String secondaryOperator) {
    infix.put(primaryOperator, new Symbol(precedence, secondaryOperator, null));
    otherSymbols.add(secondaryOperator);
  }

  /**
   * Add prefixOperator, infixOperator or postfix operators with the given precedence.
   */
  public void addOperators(OperatorType type, int precedence, String... names) {
    for (String name : names) {
      if (type == OperatorType.PREFIX) {
        prefix.put(name, new Symbol(precedence, type));
      } else {
        infix.put(name, new Symbol(precedence, type));
      }
    }
  }


  public void setImplicitOperatorPrecedence(boolean strong, int precedence) {
    if (strong) {
      strongImplicitOperatorPrecedence = precedence;
    } else {
      weakImplicitOperatorPrecedence = precedence;
    }
  }

  /**
   * Returns all symbols registered via add...Operator and add...Bracket calls.
   * Useful for tokenizer construction.
   */
  public Iterable<String> getSymbols() {
    HashSet<String> result = new HashSet<>();
    result.addAll(otherSymbols);
    result.addAll(infix.keySet());
    result.addAll(prefix.keySet());
    result.addAll(primary);
    return result;
  }

  /**
   * Parser the given expression using a simple StreamTokenizer-based parser.
   * Leftover tokens will cause an exception.
   */
  public T parse(C context, String expr) {
    Tokenizer tokenizer = new Tokenizer(new Scanner(expr), getSymbols());
    tokenizer.nextToken();
    T result = parse(context, tokenizer);
    if (tokenizer.currentType != Tokenizer.TokenType.EOF) {
      throw tokenizer.exception("Leftover input.", null);
    }
    return result;
  }

  /**
   * Parser an expression from the given tokenizer. Leftover tokens will be ignored and
   * may be handled by the caller.
   */
  public T parse(C context, Tokenizer tokenizer) {
    try {
      return parseOperator(context, tokenizer, -1);
    } catch (ParsingException e) {
      throw e;
    } catch (Exception e) {
      throw tokenizer.exception(e.getMessage(), e);
    }
  }

  private T parsePrefix(C context, Tokenizer tokenizer) {
    String token = tokenizer.currentValue;
    Symbol prefixSymbol = prefix.get(token);
    if (prefixSymbol == null) {
      return parsePrimary(context, tokenizer);
    }
    tokenizer.nextToken();
    T operand = parseOperator(context, tokenizer, prefixSymbol.precedence);
    return processor.prefixOperator(context, tokenizer, token, operand);
  }


  private T parseOperator(C context, Tokenizer tokenizer, int precedence) {
    T left = parsePrefix(context, tokenizer);

    while(true) {
      String token = tokenizer.currentValue;
      Symbol symbol = infix.get(token);
      if (symbol == null) {
        if (token.equals("") || otherSymbols.contains(token)) {
          break;
        }
        // Implicit operator
        boolean strong = tokenizer.leadingWhitespace.isEmpty();
        int implicitPrecedence = strong ? strongImplicitOperatorPrecedence : weakImplicitOperatorPrecedence;
        if (!(implicitPrecedence > precedence)) {
          break;
        }
        T right = parseOperator(context, tokenizer, implicitPrecedence);
        left = processor.implicitOperator(context, tokenizer, strong, left, right);
      } else {
        if (!(symbol.precedence > precedence)) {
          break;
        }
        tokenizer.nextToken();
        if (symbol.type == null) {
          if (symbol.close == null) {
            // Ternary
            T middle = parseOperator(context, tokenizer, -1);
            tokenizer.consume(symbol.separator);
            T right = parseOperator(context, tokenizer, symbol.precedence);
            left = processor.ternaryOperator(context, tokenizer, token, left, middle, right);
          } else {
            // Group
            List<T> list = parseList(context, tokenizer, symbol.separator, symbol.close);
            left = processor.apply(context, tokenizer, left, token, list);
          }
        } else {
          switch (symbol.type) {
            case INFIX: {
              T right = parseOperator(context, tokenizer, symbol.precedence);
              left = processor.infixOperator(context, tokenizer, token, left, right);
              break;
            }
            case INFIX_RTL: {
              T right = parseOperator(context, tokenizer, symbol.precedence - 1);
              left = processor.infixOperator(context, tokenizer, token, left, right);
              break;
            }
            case SUFFIX:
              left = processor.suffixOperator(context, tokenizer, token, left);
              break;
            default:
              throw new IllegalStateException();
          }
        }
      }
    }
    return left;
  }


  // Precondition: Opening paren consumed
  // Postcondition: Closing paren consumed
  List<T> parseList(C context, Tokenizer tokenizer, String separator, String close) {
    ArrayList<T> elements = new ArrayList<>();
    if (!tokenizer.currentValue.equals(close)) {
      while (true) {
        elements.add(parse(context, tokenizer));
        String op = tokenizer.currentValue;
        if (op.equals(close)) {
          break;
        }
        if (separator == null) {
          throw tokenizer.exception("Closing bracket expected: '" + close + "'.", null);
        }
        if (!separator.isEmpty()) {
          if (!op.equals(separator)) {
            throw tokenizer.exception("List separator '" + separator + "' or closing paren '"
                + close + " expected.", null);
          }
          tokenizer.nextToken();  // separator
        }
      }
    }
    tokenizer.nextToken();  // closing paren
    return elements;
  }

  T parsePrimary(C context, Tokenizer tokenizer) {
    String candidate = tokenizer.currentValue;
    if (groups.containsKey(candidate)) {
      tokenizer.nextToken();
      String[] grouping = groups.get(candidate);
      return processor.group(context, tokenizer, candidate, parseList(context, tokenizer, grouping[0], grouping[1]));
    }

     if (primary.contains(candidate)) {
      tokenizer.nextToken();
      return processor.primary(context, tokenizer, candidate);
    }

    T result;
    switch (tokenizer.currentType) {
      case NUMBER:
        tokenizer.nextToken();
        result = processor.numberLiteral(context, tokenizer, candidate);
        break;
      case IDENTIFIER:
        tokenizer.nextToken();
        if (calls.containsKey(tokenizer.currentValue)) {
          String openingBracket = tokenizer.currentValue;
          String[] call = calls.get(openingBracket);
          tokenizer.nextToken();
          result = processor.call(context, tokenizer, candidate, openingBracket, parseList(context, tokenizer, call[0], call[1]));
        } else {
          result = processor.identifier(context, tokenizer, candidate);
        }
        break;
      case STRING:
        tokenizer.nextToken();
        result = processor.stringLiteral(context, tokenizer, candidate);
        break;
      default:
        throw tokenizer.exception("Unexpected token type.", null);
    }
    return result;
  }


  public static class ParsingException extends RuntimeException {
    final public int start;
    final public int end;
    public ParsingException(int start, int end, String text, Exception base) {
      super(text, base);
      this.start = start;
      this.end = end;
    }
  }

  /** 
   * A simple tokenizer utilizing java.util.Scanner.
   */
  public static class Tokenizer {
    public static final Pattern DEFAULT_NUMBER_PATTERN = Pattern.compile(
        "\\G\\s*(\\d+(\\.\\d*)?|\\.\\d+)([eE][+-]?\\d+)?");

    public static final Pattern DEFAULT_IDENTIFIER_PATTERN = Pattern.compile(
            "\\G\\s*[\\p{Alpha}_$][\\p{Alpha}_$\\d]*");

    public static final Pattern DEFAULT_STRING_PATTERN = Pattern.compile(
        // "([^"\\]*(\\.[^"\\]*)*)"|\'([^\'\\]*(\\.[^\'\\]*)*)\'
        "\\G\\s*(\"([^\"\\\\]*(\\\\.[^\"\\\\]*)*)\"|'([^'\\\\]*(\\\\.[^'\\\\]*)*)')");
    public static final Pattern DEFAULT_END_PATTERN = Pattern.compile("\\G\\s*\\Z");

    public static final Pattern DEFAULT_LINE_COMMENT_PATTERN = Pattern.compile("\\G\\h*#.*(\\v|\\Z)");

    public static final Pattern DEFAULT_NEWLINE_PATTERN = Pattern.compile("\\G\\h*\\v");

    public enum TokenType {
      UNRECOGNIZED, BOF, IDENTIFIER, SYMBOL, NUMBER, STRING, EOF
    }

    public Pattern numberPattern = DEFAULT_NUMBER_PATTERN;
    public Pattern identifierPattern = DEFAULT_IDENTIFIER_PATTERN;
    public Pattern stringPattern = DEFAULT_STRING_PATTERN;
    public Pattern endPattern = DEFAULT_END_PATTERN;
    public Pattern newlinePattern = DEFAULT_NEWLINE_PATTERN;
    public Pattern lineCommentPattern = DEFAULT_LINE_COMMENT_PATTERN;
    public Pattern symbolPattern;

    public int currentLine = 1;
    public int lastLineStart = 0;
    public int currentPosition = 0;
    public String currentValue = "";
    public TokenType currentType = TokenType.BOF;
    public String leadingWhitespace = "";
    public boolean insertSemicolons;

    protected final Scanner scanner;
    private StringBuilder skippedComments = new StringBuilder();

    public Tokenizer(Scanner scanner, Iterable<String> symbols, String... additionalSymbols) {
      this.scanner = scanner;
      StringBuilder sb = new StringBuilder("\\G\\s*(");

      TreeSet<String> sorted = new TreeSet<>(new Comparator<String>() {
        @Override
        public int compare(String s1, String s2) {
          int dl = -Integer.compare(s1.length(), s2.length());
          return dl == 0 ? s1.compareTo(s2) : dl;
        }
      });
      for (String symbol: symbols) {
        sorted.add(symbol);
      }
      Collections.addAll(sorted, additionalSymbols);
      for (String s : sorted) {
        sb.append(Pattern.quote(s));
        sb.append('|');
      }
      sb.setCharAt(sb.length() - 1, ')');
      symbolPattern = Pattern.compile(sb.toString());
    }

    public int currentColumn() {
      return  currentPosition - lastLineStart + 1;
    }

    public TokenType consume(String expected) {
      if (!tryConsume(expected)) {
        throw exception("Expected: '" + expected + "'.", null);
      }
      return currentType;
    }

    public String consumeIdentifier() {
      if (currentType != TokenType.IDENTIFIER) {
        throw exception("Identifier expected!", null);
      }
      String identifier = currentValue;
      nextToken();
      return identifier;
    }

    public ParsingException exception(String message, Exception cause) {
      return new ParsingException(currentPosition - currentValue.length(), currentPosition,
              message + " Token: '" + currentValue + "' Type: " + currentType, cause);
    }

    protected boolean insertSemicolon() {
      return (currentType == TokenType.IDENTIFIER || currentType == TokenType.NUMBER ||
              currentType == TokenType.STRING  ||
              (currentValue.length() == 1 && ")]}".indexOf(currentValue) != -1));
    }

    public String consumeComments() {
      String result = skippedComments.toString();
      skippedComments.setLength(0);
      return result;
    }

    public TokenType nextToken() {
      currentPosition += currentValue.length();
      String value;
      if (scanner.ioException() != null) {
        throw exception("IO Exception: " + scanner.ioException().getMessage(), scanner.ioException());
      }

      boolean newLine = false;
      while (true) {
        if ((value = scanner.findWithinHorizon(lineCommentPattern, 0)) != null) {
          skippedComments.append(value.trim() + "\n");
          System.out.println("Comment: " + value);
        } else if ((value = scanner.findWithinHorizon(newlinePattern, 0)) == null) {
          break;
        }
        newLine = true;
        currentPosition += value.length();
        currentLine++;
        lastLineStart = currentPosition;
      }

      if (newLine && insertSemicolons && insertSemicolon()) {
        value = ";";
        currentPosition--;
        currentType = TokenType.SYMBOL;
      } else if ((value = scanner.findWithinHorizon(identifierPattern, 0)) != null) {
        currentType = TokenType.IDENTIFIER;
      } else if ((value = scanner.findWithinHorizon(numberPattern, 0)) != null) {
        currentType = TokenType.NUMBER;
      } else if ((value = scanner.findWithinHorizon(stringPattern, 0)) != null) {
        currentType = TokenType.STRING;
      } else if ((value = scanner.findWithinHorizon(symbolPattern, 0)) != null) {
        currentType = TokenType.SYMBOL;
      } else if ((value = scanner.findWithinHorizon(endPattern, 0)) != null) {
        currentType = TokenType.EOF;
      } else if ((value = scanner.findWithinHorizon("\\G\\s*\\S*", 0)) != null) {
        currentType = TokenType.UNRECOGNIZED;
      } else {
        currentType = TokenType.UNRECOGNIZED;
        throw exception("EOF not reached, but catchall not matched.", null);
      }
      if (value.length() > 0 && value.charAt(0) <= ' ') {
        currentValue = value.trim();
        leadingWhitespace = value.substring(0, value.length() - currentValue.length());
        int pos = 0;
        while (true) {
          int j = leadingWhitespace.indexOf('\n', pos);
          if (j == -1) {
            break;
          }
          pos = j + 1;
          currentLine++;
          lastLineStart = currentPosition + j;
        }
        currentPosition += leadingWhitespace.length();
      } else {
        leadingWhitespace = "";
        currentValue = value;
      }
      return currentType;
    }

    @Override
    public String toString() {
      return currentType + " " + currentValue + " position: " + currentPosition;
    }

    public boolean tryConsume(String value) {
      if (!currentValue.equals(value)) {
        return false;
      }
      nextToken();
      return true;
    }
  }
}
