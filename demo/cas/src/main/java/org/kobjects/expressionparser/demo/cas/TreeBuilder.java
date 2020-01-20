package org.kobjects.expressionparser.demo.cas;

import org.kobjects.expressionparser.ExpressionParser;
import org.kobjects.expressionparser.OperatorType;
import org.kobjects.expressionparser.Processor;
import org.kobjects.expressionparser.Tokenizer;
import org.kobjects.expressionparser.demo.cas.tree.Node;
import org.kobjects.expressionparser.demo.cas.tree.NodeFactory;
import org.kobjects.expressionparser.demo.cas.tree.UnaryFunction;
import org.kobjects.expressionparser.demo.cas.tree.Variable;

import java.util.List;
import java.util.Scanner;
import java.util.regex.Pattern;

public class TreeBuilder extends Processor<Node> {

  @Override
  public Node infixOperator(Tokenizer tokenizer, String name, Node left, Node right) {
    switch (name.charAt(0)) {
      case '+': return NodeFactory.add(left, right);
      case '-':
      case '−': return NodeFactory.sub(left, right);
      case '/':
      case ':':
      case '÷': return NodeFactory.div(left, right);
      case '*':
      case '×':
      case '⋅': return NodeFactory.mul(left, right);
      case '^': return NodeFactory.pow(left, right);
    }
    throw new UnsupportedOperationException("Unsupported infix operator: " + name);
  }

  public Node implicitOperator(Tokenizer tokenizer, boolean strong, Node left, Node right) {
    return infixOperator(tokenizer, "⋅", left, right);
  }

  @Override
  public Node prefixOperator(Tokenizer tokenizer, String name, Node argument) {
    if (name.equals("-") || name.equals("−")) {
      return NodeFactory.neg(argument);
    }
    if (name.equals("+")) {
      return argument;
    }
    return NodeFactory.f(name, argument);
  }

  @Override
  public Node numberLiteral(Tokenizer tokenizer, String value) {
    return NodeFactory.c(Double.parseDouble(value));
  }

  @Override
  public Node identifier(Tokenizer tokenizer, String name) {
    return NodeFactory.var(name);
  }

  @Override
  public Node group(Tokenizer tokenizer, String paren, List<Node> elements) {
    return elements.get(0);
  }

  @Override
  public Node call(Tokenizer tokenizer, String identifier, String bracket, List<Node> arguments) {
    if (identifier.equals("derive")) {
      if (arguments.size() != 2) {
        throw new IllegalArgumentException("Two parameters expected for derive.");
      }
      if (!(arguments.get(1) instanceof Variable)) {
        throw new IllegalArgumentException("Second derive parameter must be a variable.");
      }
      return NodeFactory.derive(arguments.get(0), arguments.get(1).toString());
    }
    return super.call(tokenizer, identifier, bracket, arguments);
  }

  @Override
  public Node apply(Tokenizer tokenizer, Node base, String bracket, List<Node> arguments) {
    throw new UnsupportedOperationException();
  }

  public static ExpressionParser<Node> createParser() {
    ExpressionParser<Node> parser = new ExpressionParser<Node>(new TreeBuilder()) {
      @Override
      public Node parse(String expression) {
        Tokenizer tokenizer = new ExpressionTokenizer(new Scanner(expression), this.getSymbols());
        tokenizer.nextToken();
        Node result = parse(tokenizer);
        if (tokenizer.currentType != Tokenizer.TokenType.EOF) {
          throw tokenizer.exception("EOF expected.", null);
        }
        return result;
      }
    };
    parser.addCallBrackets("(", ",", ")");
    parser.addGroupBrackets("(", null, ")");
    parser.addOperators(OperatorType.INFIX_RTL, Node.PRECEDENCE_POWER, "^");
    parser.addOperators(OperatorType.PREFIX, Node.PRECEDENCE_SIGNUM, "+", "-", "−");
    parser.setImplicitOperatorPrecedence(true, Node.PRECEDENCE_IMPLICIT_MULTIPLICATION);
    for (String name : UnaryFunction.DEFINITIONS.keySet()) {
      parser.addOperators(
          OperatorType.PREFIX, Node.PRECEDENCE_UNARY_FUNCTION, name);
    }
    parser.addOperators(OperatorType.INFIX, Node.PRECEDENCE_MULTIPLICATIVE,
        "*", "×", "⋅", "/", ":", "÷");
    parser.addOperators(OperatorType.INFIX, Node.PRECEDENCE_ADDITIVE,
        "+", "-", "−");
    return parser;
  }

  static class ExpressionTokenizer extends Tokenizer {
    static final Pattern EXPONENT_PATTERN = Pattern.compile("\\G\\s*[⁰¹²³⁴⁵⁶⁷⁸⁹]+");

    String pendingExponent;

    public ExpressionTokenizer(Scanner scanner, Iterable<String> symbols) {
      super(scanner, symbols);
    }

    @Override
    public TokenType nextToken() {
      if (pendingExponent != null) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < pendingExponent.length(); i++) {
          sb.append("⁰¹²³⁴⁵⁶⁷⁸⁹".indexOf(pendingExponent.charAt(i)));
        }
        leadingWhitespace = "";
        currentType = TokenType.NUMBER;
        currentValue = sb.toString();
        pendingExponent = null;
        return currentType;
      }
      String value = scanner.findWithinHorizon(EXPONENT_PATTERN, 0);
      if (value != null) {
        currentPosition += currentValue.length();
        if (value.charAt(0) <= ' ') {
          pendingExponent = value.trim();
          leadingWhitespace = value.substring(0, value.length() - pendingExponent.length());
          currentPosition += leadingWhitespace.length();
        } else {
          pendingExponent = value;
          leadingWhitespace = "";
        }
        currentType = TokenType.SYMBOL;
        currentValue = "^";
        return currentType;
      }
      return super.nextToken();
    }
  }

}
