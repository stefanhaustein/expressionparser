package org.kobjects.expressionparser.demo.calculator;

import org.kobjects.expressionparser.ExpressionParser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class Calculator {
  static final Class[] DOUBLE_TYPE_ARRAY_1 = {Double.TYPE};

  static HashMap<String, Double> variables = new HashMap<>();

  /**
   * Processes the calls from the parser directly to a Double value.
   */
  static class DoubleProcessor extends ExpressionParser.Processor<Double> {
    @Override
    public Double infixOperator(ExpressionParser.Tokenizer tokenizer, String name, Double left, Double right) {
      switch (name.charAt(0)) {
        case '+': return left + right;
        case '-': return left - right;
        case '*': return left * right;
        case '/': return left / right;
        case '^': return Math.pow(left, right);
        default:
          throw new IllegalArgumentException();
      }
    }

    @Override
    public Double implicitOperator(ExpressionParser.Tokenizer tokenizer, boolean strong, Double left, Double right) {
      return left * right;
    }

    @Override
    public Double prefixOperator(ExpressionParser.Tokenizer tokenizer, String name, Double argument) {
      return name.equals("-") ? -argument : argument;
    }

    @Override
    public Double numberLiteral(ExpressionParser.Tokenizer tokenizer, String value) {
      return Double.parseDouble(value);
    }

    @Override
    public Double identifier(ExpressionParser.Tokenizer tokenizer, String name) {
      Double value = variables.get(name);
      if (value == null) {
        throw new IllegalArgumentException("Undeclared variable: " + name);
      }
      return value;
    }

    @Override
    public Double group(ExpressionParser.Tokenizer tokenizer, String paren, List<Double> elements) {
      return elements.get(0);
    }

    /** 
     * Delegates function calls to Math via reflection.
     */
    @Override
    public Double call(ExpressionParser.Tokenizer tokenizer, String identifier, String bracket, List<Double> arguments) {
      if (arguments.size() == 1) {
        try {
          return (Double) Math.class.getMethod(
              identifier, DOUBLE_TYPE_ARRAY_1).invoke(null, arguments.get(0));
        } catch (Exception e) {
          // Fall through
        }
      }
      return super.call(tokenizer, identifier, bracket, arguments);
    }

    /**
     * Creates a parser for this processor with matching operations and precedences set up.
     */
    static ExpressionParser<Double> createParser() {
      ExpressionParser<Double> parser = new ExpressionParser<Double>(new DoubleProcessor());
      parser.addCallBrackets("(", ",", ")");
      parser.addGroupBrackets("(", null, ")");
      parser.addOperators(ExpressionParser.OperatorType.INFIX_RTL, 4, "^");
      parser.addOperators(ExpressionParser.OperatorType.PREFIX, 3, "+", "-");
      parser.setImplicitOperatorPrecedence(true, 2);
      parser.setImplicitOperatorPrecedence(false, 2);
      parser.addOperators(ExpressionParser.OperatorType.INFIX, 1, "*", "/");
      parser.addOperators(ExpressionParser.OperatorType.INFIX, 0, "+", "-");
      return parser;
    }
  }

  public static void main(String[] args) throws IOException {
    variables.put("tau", 2 * Math.PI);
    variables.put("pi", Math.PI);
    variables.put("e", Math.E);

    BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
    ExpressionParser<Double> parser = DoubleProcessor.createParser();
    while (true) {
      System.out.print("Expression? ");
      String input = reader.readLine();
      if (input == null || input.isEmpty()) {
        break;
      }
      try {
        System.out.println("Result:     " + parser.parse(input));
      } catch (ExpressionParser.ParsingException e) {
        char[] fill = new char[Math.max(e.end, e.start + 1)];
        Arrays.fill(fill, 0, e.start, '-');
        Arrays.fill(fill, e.start, Math.max(e.end, e.start + 1), '^');
        System.out.println("Error       " + new String(fill) + ": " + e.getMessage());
      } catch (RuntimeException e) {
        e.printStackTrace();
      }
    }
  }
}
