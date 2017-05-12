package org.kobjects.expressionparser.demo.basic;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.Arrays;
import java.util.List;
import java.util.Map;


class Statement extends Node {

  enum Type {
    CLEAR, CONTINUE, DATA, DIM, DEF, DUMP,
    END, FOR, GOTO, GOSUB, IF, INPUT, LET, LIST, LOAD,
    NEW, NEXT, ON, PRINT, READ, REM, RESTORE, RETURN, RUN,
    STOP, TRON, TROFF
  }

  final Interpreter interpreter;
  final Type type;
  final String[] delimiter;

  Statement(Interpreter interpreter, Type type, String[] delimiter, Node... children) {
    super(children);
    this.interpreter = interpreter;
    this.type = type;
    this.delimiter = delimiter;
  }

  Statement(Interpreter interpreter, Type type, Node... children) {
    this(interpreter, type, null, children);
  }

  Object eval() {
    if (type == null) {
      return null;
    }

    if (interpreter.trace) {
      System.out.print(interpreter.currentLine + ":" + interpreter.currentIndex + ": " + this);
    }

    switch (type) {
      case CONTINUE:
        if (interpreter.stopped == null) {
          throw new RuntimeException("Not stopped.");
        }
        interpreter.currentLine = interpreter.stopped[0];
        interpreter.currentIndex = interpreter.stopped[1] + 1;
        break;

      case CLEAR:
        interpreter.clear();
        break;

      case DEF: {
        DefFn f = new DefFn(interpreter, children[0]);
        interpreter.functionDefinitions.put(f.name, f);
        break;
      }
      case DATA:
      case DIM:   // We just do dynamic expansion as needed.
      case REM:
        break;

      case DUMP:
        if (interpreter.lastException != null) {
          interpreter.lastException.printStackTrace();
          interpreter.lastException = null;
        } else {
          System.out.println("\n" + interpreter.variables);
          for (int i = 0; i < interpreter.arrays.length; i++) {
            if (!interpreter.arrays[i].isEmpty()) {
              System.out.println((i + 1) + ": " + interpreter.arrays[i]);
            }
          }
        }
        break;

      case END:
        interpreter.currentLine = Integer.MAX_VALUE;
        interpreter.currentIndex = 0;
        break;

      case FOR:
        loopStart();
        break;

      case GOSUB: {
        Interpreter.StackEntry entry = new Interpreter.StackEntry();
        entry.lineNumber = interpreter.currentLine;
        entry.statementIndex = interpreter.currentIndex;
        interpreter.stack.add(entry);
      }  // Fallthrough intended
      case GOTO:
        interpreter.currentLine = (int) evalDouble(0);
        interpreter.currentIndex = 0;
        break;

      case IF:
        if (evalDouble(0) == 0.0) {
          interpreter.currentLine++;
          interpreter.currentIndex = 0;
        } else if (children.length == 2) {
          interpreter.currentLine = (int) evalDouble(1);
          interpreter.currentIndex = 0;
        }
        break;

      case LET: {
        ((Variable) children[0]).set(children[1].eval());
        if (interpreter.trace) {
          System.out.print (" // " + children[0].eval());
        }
        break;
      }
      case LIST:
        list();
        break;

      case LOAD:
        load();
        break;

      case NEW:
        interpreter.clear();
        interpreter.program.clear();
        break;

      case NEXT:
        loopEnd();
        break;

      case INPUT:
        input();
        break;

      case PRINT:
        for (int i = 0; i < children.length; i++) {
          Object val = children[i].eval();
          if (val instanceof Double) {
            double d = (Double) val;
            interpreter.print((d < 0 ? "" : " ") + Interpreter.toString(d) + " ");
          } else {
            interpreter.print(Interpreter.toString(val));
          }
          if (i < delimiter.length && delimiter[i].equals(", ")) {
            interpreter.print(
                "                    ".substring(0, 14 - (interpreter.tabPos % 14)));
          }
        }
        if (delimiter.length < children.length &&
            (children.length == 0 || !children[children.length - 1].toString().startsWith("TAB"))) {
          interpreter.print("\n");
        }
        break;

      case ON: {
        int index = (int) Math.round(evalDouble(0));
        if (index < children.length && index > 0) {
          if (delimiter[0].equals(" GOSUB ")) {
            Interpreter.StackEntry entry = new Interpreter.StackEntry();
            entry.lineNumber = interpreter.currentLine;
            entry.statementIndex = interpreter.currentIndex;
            interpreter.stack.add(entry);
          }
          interpreter.currentLine = (int) evalDouble(index);
          interpreter.currentIndex = 0;
        }
        break;
      }
      case READ:
        for (int i = 0; i < children.length; i++) {
          while (interpreter.dataStatement == null
              || interpreter.dataPosition[2] >= interpreter.dataStatement.children.length) {
            interpreter.dataPosition[2] = 0;
            if (interpreter.dataStatement != null) {
              interpreter.dataPosition[1]++;
            }
            interpreter.dataStatement = find(Type.DATA, null, interpreter.dataPosition);
            if (interpreter.dataStatement == null) {
              throw new RuntimeException("Out of data.");
            }
          }
          ((Variable) children[i]).set(interpreter.dataStatement.children[interpreter.dataPosition[2]++].eval());
        }
        break;

      case RESTORE:
        interpreter.dataStatement = null;
        Arrays.fill(interpreter.dataPosition, 0);
        if (children.length > 0) {
          interpreter.dataPosition[0] = (int) evalDouble(0);
        }
        break;

      case RETURN:
        while (true) {
          if (interpreter.stack.isEmpty()) {
            throw new RuntimeException("RETURN without GOSUB.");
          }
          Interpreter.StackEntry entry = interpreter.stack.remove(interpreter.stack.size() - 1);
          if (entry.forVariable == null) {
            interpreter.currentLine = entry.lineNumber;
            interpreter.currentIndex = entry.statementIndex + 1;
            break;
          }
        }
        break;

      case RUN:
        interpreter.clear();
        interpreter.currentLine = children.length == 0 ? 0 : (int) evalDouble(0);
        interpreter.currentIndex = 0;
        break;

      case STOP:
        interpreter.stopped = new int[]{interpreter.currentLine, interpreter.currentIndex};
        System.out.println("\nSTOPPED in " + interpreter.currentLine + ":" + interpreter.currentIndex);
        interpreter.currentLine = Integer.MAX_VALUE;
        interpreter.currentIndex = 0;
        break;
      case TRON:
        interpreter.trace = true;
        break;
      case TROFF:
        interpreter.trace = false;
        break;

      default:
        throw new RuntimeException("Unimplemented statement: " + type);
    }
    if (interpreter.trace) {
      System.out.println();
    }
    return null;
  }

  Statement find(Type type, String name, int[] position) {
    Map.Entry<Integer, List<Statement>> entry;
    while (null != (entry = interpreter.program.ceilingEntry(position[0]))) {
      position[0] = entry.getKey();
      List<Statement> list = entry.getValue();
      while (position[1] < list.size()) {
        Statement statement = list.get(position[1]);
        if (statement.type == type) {
          if (name == null || statement.children.length == 0) {
            return statement;
          }
          for (int i = 0; i < statement.children.length; i++) {
            if (statement.children[i].toString().equalsIgnoreCase(name)) {
              position[2] = i;
              return statement;
            }
          }
        }
        position[1]++;
      }
      position[0]++;
      position[1] = 0;
    }
    return null;
  }

  void list() {
    System.out.println();
    for (Map.Entry<Integer, List<Statement>> entry : interpreter.program.entrySet()) {
      System.out.print(entry.getKey());
      List<Statement> line = entry.getValue();
      for (int i = 0; i < line.size(); i++) {
        System.out.print(i == 0 || line.get(i - 1).type == Type.IF ? "" : " :");
        System.out.print(line.get(i));
      }
      System.out.println();
    }
  }

  void load() {
    String line = null;
    try {
      URLConnection connection = new URL(evalString(0)).openConnection();
      connection.setDoInput(true);
      InputStream is = connection.getInputStream();
      BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
      while (null != (line = reader.readLine())) {
        try {
          interpreter.processInputLine(line);
        } catch (Exception e) {
          System.out.println(line);
          System.out.println(e.getMessage());
        }
      }
      reader.close();
      is.close();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  void loopStart() {
    double current = evalDouble(1);
    ((Variable) children[0]).set(current);
    double end = evalDouble(2);
    double step = children.length > 3 ? evalDouble(3) : 1.0;
    if (Math.signum(step) == Math.signum(Double.compare(current, end))) {
      int nextPosition[] = new int[3];
      if (find(Type.NEXT, children[0].toString(), nextPosition) == null) {
        throw new RuntimeException("FOR without NEXT");
      }
      interpreter.currentLine = nextPosition[0];
      interpreter.currentIndex = nextPosition[1];
      interpreter.nextSubIndex = nextPosition[2] + 1;
    } else {
      Interpreter.StackEntry entry = new Interpreter.StackEntry();
      entry.forVariable = (Variable) children[0];
      entry.end = end;
      entry.step = step;
      entry.lineNumber = interpreter.currentLine;
      entry.statementIndex = interpreter.currentIndex;
      interpreter.stack.add(entry);
    }
  }

  void loopEnd() {
    for (int i = interpreter.nextSubIndex; i < Math.max(children.length, 1); i++) {
      String name = children.length == 0 ? null : children[i].toString();
      Interpreter.StackEntry entry;
      while (true) {
        if (interpreter.stack.isEmpty()
            || interpreter.stack.get(interpreter.stack.size() - 1).forVariable == null) {
          throw new RuntimeException("NEXT " + name + " without FOR.");
        }
        entry = interpreter.stack.remove(interpreter.stack.size() - 1);
        if (name == null || entry.forVariable.name.equals(name)) {
          break;
        }
      }
      double current = ((Double) entry.forVariable.eval()) + entry.step;
      entry.forVariable.set(current);
      if (Math.signum(entry.step) != Math.signum(Double.compare(current, entry.end))) {
        interpreter.stack.add(entry);
        interpreter.currentLine = entry.lineNumber;
        interpreter.currentIndex = entry.statementIndex + 1;
        break;
      }
    }
    interpreter.nextSubIndex = 0;
  }

  void input() {
    for (int i = 0; i < children.length; i++) {
      Node child = children[i];
      if (type == Type.INPUT && child instanceof Variable) {
        if (i <= 0 || i > delimiter.length || !delimiter[i-1].equals(", ")) {
          interpreter.print("? ");
        }
        Variable variable = (Variable) child;
        Object value;
        while(true) {
          try {
            value = interpreter.reader.readLine();
          } catch (IOException e) {
            throw new RuntimeException(e);
          }
          if (variable.name.endsWith("$")) {
            break;
          }
          try {
            value = Double.parseDouble((String) value);
            break;
          } catch (NumberFormatException e) {
            interpreter.print("Not a number. Please enter a number: ");
          }
        }
        variable.set(value);
      } else {
        interpreter.print(Interpreter.toString(child.eval()));
      }
    }
  }

  @Override
  public Class<?> returnType() {
    return Void.class;
  }

  @Override
  public String toString() {
    if (type == null) {
      return "";
    }
    StringBuilder sb = new StringBuilder(" ");
    sb.append(type.name());
    if (children.length > 0) {
      sb.append(' ');
      sb.append(children[0]);
      for (int i = 1; i < children.length; i++) {
        sb.append((delimiter == null || i > delimiter.length) ? ", " : delimiter[i - 1]);
        sb.append(children[i]);
      }
      if (delimiter != null && delimiter.length == children.length) {
        sb.append(delimiter[delimiter.length - 1]);
      }
    }
    return sb.toString();
  }
}
