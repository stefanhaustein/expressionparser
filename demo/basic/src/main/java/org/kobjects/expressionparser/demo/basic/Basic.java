package org.kobjects.expressionparser.demo.basic;

import org.kobjects.expressionparser.ParsingException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;

public class Basic {

  public static void main(String[] args) throws IOException {
    BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
    Interpreter interpreter = new Interpreter(reader);

    System.out.println("  **** EXPRESSION PARSER BASIC DEMO V1 ****\n");
    System.out.println("  " + (Runtime.getRuntime().totalMemory() / 1024) + "K SYSTEM  "
        + Runtime.getRuntime().freeMemory() + " BASIC BYTES FREE\n");

    boolean prompt = true;
    while (true) {
      if (prompt) {
        System.out.println("\nREADY.");
      }
      String line = reader.readLine();
      if (line == null) {
        break;
      }
      prompt = true;
      try {
        prompt = interpreter.processInputLine(line);
      } catch (ParsingException e) {
        char[] fill = new char[Math.max(e.end, e.start + 1)];
        Arrays.fill(fill, 0, e.start, '-');
        Arrays.fill(fill, e.start, Math.max(e.end, e.start + 1), '^');
        System.out.println(new String(fill));
        System.out.println("?SYNTAX ERROR: " + e.getMessage());
        interpreter.lastException = e;
      } catch (Exception e) {
        System.out.println("\nERROR in " + interpreter.currentLine + ':'
            + interpreter.currentIndex + ": " + e.getMessage());
        System.out.println("\nREADY.");
        interpreter.lastException = e;
      }
    }
  }
}
