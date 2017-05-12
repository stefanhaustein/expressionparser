package org.kobjects.expressionparser.demo.cas;

import org.kobjects.expressionparser.ExpressionParser;
import org.kobjects.expressionparser.ExpressionParser.ParsingException;
import org.kobjects.expressionparser.demo.cas.string2d.String2d;
import org.kobjects.expressionparser.demo.cas.tree.Node;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

public class CasDemo {

  public static void main(String[] args) throws IOException {
    ExpressionParser<Node> parser = TreeBuilder.createParser();
    BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
    while (true) {
      System.out.print("Input?  ");
      String input = reader.readLine();
      if (input == null || input.isEmpty()) {
        break;
      }
      try {
        Node expr = parser.parse(input);
        String s = expr.toString();
        System.out.println("\nParsed: " + s + '\n');
        s = "Equals: " + s;
        Set<String> explanation = new LinkedHashSet<String>();
        while (true) {
          String2d s2d = String2d.concat("Equals: ", expr.toString2d(Node.Stringify.BLOCK));
          if (!explanation.isEmpty()) {
            s2d = s2d.vBar(String2d.concat(s2d, "     "), String2d.concat(" ", String2d.stack(
                String2d.HorizontalAlign.LEFT,
                explanation.size()/2,
                explanation.toArray(new Object[explanation.size()]))));
            explanation.clear();
          }
          String t = s2d.toString();
          if (!s.equals(t)) {
            s = t;
            System.out.println(s + "\n");
          }
          Node simplified = expr.simplify(explanation);
          if (simplified.equals(expr)) {
            break;
          }
          expr = simplified;
        }
        if (s.indexOf('\n') != -1) {
          System.out.println("Flat:   " + expr.toString() + "\n");
        }

      } catch (ParsingException e) {
        char[] fill = new char[e.position + 8];
        Arrays.fill(fill, '-');
        System.out.println("Error " + new String(fill) + "^: " + e.getMessage());
      } catch (RuntimeException e) {
        e.printStackTrace();
      }
    }
  }
}
