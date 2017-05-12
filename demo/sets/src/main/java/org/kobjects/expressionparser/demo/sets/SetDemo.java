package org.kobjects.expressionparser.demo.sets;

import org.kobjects.expressionparser.ExpressionParser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Demo for set expression operators.
 */
public class SetDemo {

  static class SetProcessor extends ExpressionParser.Processor<Object> {

    private Set<Object> assertSet(Object o) {
      if (!(o instanceof Set)) {
        throw new RuntimeException("Set expected for " + o);
      }
      return (Set<Object>) o;
    }

    @Override
    public Object infixOperator(ExpressionParser.Tokenizer tokenizer, String name, Object left, Object right) {
      if (name.equals("\u2229")) {  // intersection
        assertSet(left).retainAll(assertSet(right));
        return left;
      }
      if (name.equals("\u222a")) {  // union
        assertSet(left).addAll(assertSet(right));
        return left;
      }
      if (name.equals("\u2216") || name.equals("\\")) {  // set minus
        assertSet(left).removeAll(assertSet(right));
        return left;
      }
      throw new UnsupportedOperationException(name);
    }

    @Override
    public Object numberLiteral(ExpressionParser.Tokenizer tokenizer, String value) {
      return Double.parseDouble(value);
    }

    @Override
    public Object stringLiteral(ExpressionParser.Tokenizer tokenizer, String value) {
      return value;
    }

    @Override
    public Object primary(ExpressionParser.Tokenizer tokenizer, String name) {
      if (name.equals("\u2205")){
        return new LinkedHashSet<Object>();
      }
      throw new UnsupportedOperationException("Symbol: " + name);
    }

    @Override
    public Object identifier(ExpressionParser.Tokenizer tokenizer, String name) {
      return name;
    }

    @Override
    public Object group(ExpressionParser.Tokenizer tokenizer, String paren, List<Object> elements) {
      if (paren.equals("(")) {
        return elements.get(0);
      }
      if (paren.equals("{")) {
        LinkedHashSet<Object> set = new LinkedHashSet<>();
        set.addAll(elements);
        return set;
      }
      if (paren.equals("|")) {
        Object o = elements.get(0);
        if (o instanceof Set) {
          return ((Set) o).size();
        }
        if (o instanceof Double) {
          return Math.abs((Double) o);
        }
        throw new RuntimeException("Can't apply || to " + o);
      }
      return super.group(tokenizer, paren, elements);
    }
  }

  public static void main(String[] args) throws IOException {
    System.out.println("Operators: \u2229 \u222a \u2216 \u2205");
    ExpressionParser<Object> parser = new ExpressionParser<>(new SetProcessor());
    parser.addGroupBrackets("(", null, ")");
    parser.addGroupBrackets("{", ",", "}");
    parser.addGroupBrackets("|", null, "|");
    parser.addOperators(ExpressionParser.OperatorType.INFIX, 1, "\u2229");
    parser.addOperators(ExpressionParser.OperatorType.INFIX, 0, "\u222a", "\u2216", "\\");
    parser.addPrimary("\u2205");
    BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
    while (true) {
      System.out.print("Expression? ");
      String input = reader.readLine();
      if (input == null || input.isEmpty()) {
        break;
      }
      try {
        System.out.println("Result:     " + parser.parse(input).toString().replace('[', '{').replace(']', '}'));
      } catch (ExpressionParser.ParsingException e) {
        char[] fill = new char[e.position + 6];
        Arrays.fill(fill, '-');
        System.out.println("Error " + new String(fill) + "^: " + e.getMessage());
      } catch (RuntimeException e) {
        System.out.println("Error: " + e.toString());
      }
    }
  }
}
