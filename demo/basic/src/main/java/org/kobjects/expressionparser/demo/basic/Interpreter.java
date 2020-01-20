package org.kobjects.expressionparser.demo.basic;

import org.kobjects.expressionparser.Tokenizer;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Full implementation of <a href="http://goo.gl/kIIPc0">ECMA-55</a> minimal interpreter with
 * some common additions.
 * <p>
 * Example for mixing the expresion parser with "outer" parsing.
 */
public class Interpreter {
  static final String INVISIBLE_STRING = new String();

  static String toString(double d) {
    if (d == (int) d) {
      return String.valueOf((int) d);
    }
    return String.valueOf(d);
  }

  static String toString(Object o) {
    return o instanceof Number ? toString(((Number) o).doubleValue()) : String.valueOf(o);
  }

  Parser parser = new Parser(this);
  TreeMap<Integer, List<Statement>> program = new TreeMap<>();

  // Program state

  TreeMap[] arrays = {
      new TreeMap(), new TreeMap(), new TreeMap(), new TreeMap(), new TreeMap(), new TreeMap()
  };
  TreeMap<String, Object> variables = new TreeMap<>();
  Exception lastException;
  BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
  ArrayList<StackEntry> stack = new ArrayList<>();
  TreeMap<String, double[]> forMap = new TreeMap<>();
  TreeMap<String, DefFn> functionDefinitions = new TreeMap<>();

  int currentLine;
  int currentIndex;
  int nextSubIndex;  // index within next when skipping a for loop; reset in next
  int[] dataPosition = new int[3];
  Statement dataStatement;
  int[] stopped;
  int tabPos;
  boolean trace;

  Interpreter(BufferedReader reader) {
    this.reader = reader;
    clear();
  }

  void clear() {
    variables.clear();
    for (TreeMap t : arrays) {
      t.clear();
    }
    variables.put("pi", Math.PI);
    variables.put("tau", 2 * Math.PI);
    Arrays.fill(dataPosition, 0);
    dataStatement = null;
    nextSubIndex = 0;
    forMap.clear();
    stack.clear();
    stopped = null;
    functionDefinitions.clear();
  }

  void runProgram() {
    Map.Entry<Integer, List<Statement>> entry;
    while (null != (entry = program.ceilingEntry(currentLine))) {
      currentLine = entry.getKey();
      runStatements(entry.getValue());
    }
  }

  void runStatements(List<Statement> statements) {
    int line = currentLine;
    while (currentIndex < statements.size()) {
      int index = currentIndex;
      statements.get(index).eval();
      if (currentLine != line) {
        return;  // Goto or similar out of the current line
      }
      if (currentIndex == index) {
        currentIndex++;
      }
    }
    currentIndex = 0;
    currentLine++;
  }

  /**
   * Returns true if the line was "interactive" and a "ready" prompt should be displayed.
   */
  boolean processInputLine(String line) {
    Tokenizer tokenizer = parser.createTokenizer(line);

    tokenizer.nextToken();
    switch (tokenizer.currentType) {
      case EOF:
        return false;
      case NUMBER:
        int lineNumber = (int) Double.parseDouble(tokenizer.currentValue);
        tokenizer.nextToken();
        if (tokenizer.currentType == Tokenizer.TokenType.EOF) {
          program.remove(lineNumber);
        } else {
          program.put(lineNumber, parser.parseStatementList(tokenizer));
        }
        return false;
      default:
        List<Statement> statements = parser.parseStatementList(tokenizer);
        currentLine = -2;
        currentIndex = 0;
        runStatements(statements);
        if (currentLine != -1) {
          runProgram();
        }
        return true;
    }
  }

  String tab(int pos) {
    pos = Math.max(0, pos - 1);
    char[] fill;
    if (pos < tabPos) {
      fill = new char[pos + 1];
      Arrays.fill(fill, ' ');
      fill[0] = '\n';
    } else {
      fill = new char[pos - tabPos];
      Arrays.fill(fill, ' ');
    }
    return new String(fill);
  }

  void print(String s) {
    System.out.print(s);
    int cut = s.lastIndexOf('\n');
    if (cut == -1) {
      tabPos += s.length();
    } else {
      tabPos = s.length() - cut - 1;
    }
  }

  static class StackEntry {
    int lineNumber;
    int statementIndex;
    Variable forVariable;
    double step;
    double end;
  }
}
