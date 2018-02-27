package org.kobjects.expressionparser.demo.basic;

import org.kobjects.expressionparser.ExpressionParser;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class Parser {
  final Interpreter interpreter;
  final ExpressionParser<Node, Void> expressionParser;

  Parser(Interpreter interpreter) {
    this.interpreter = interpreter;

    expressionParser = new ExpressionParser<>(new ExpressionBuilder());
    expressionParser.addCallBrackets("(", ",", ")");
    expressionParser.addCallBrackets("[", ",", "]");  // HP
    expressionParser.addGroupBrackets("(", null, ")");
    expressionParser.addOperators(ExpressionParser.OperatorType.INFIX, 8, "^");
    expressionParser.addOperators(ExpressionParser.OperatorType.PREFIX, 7, "-");
    expressionParser.addOperators(ExpressionParser.OperatorType.INFIX, 6, "*", "/");
    expressionParser.addOperators(ExpressionParser.OperatorType.INFIX, 5, "+", "-");
    expressionParser.addOperators(ExpressionParser.OperatorType.INFIX, 4, ">=", "<=", "<>", ">", "<", "=");
    expressionParser.addOperators(ExpressionParser.OperatorType.PREFIX, 3, "not", "NOT", "Not");
    expressionParser.addOperators(ExpressionParser.OperatorType.INFIX, 2, "and", "AND", "And");
    expressionParser.addOperators(ExpressionParser.OperatorType.INFIX, 1, "or", "OR", "Or");
  }

  ExpressionParser.Tokenizer createTokenizer(String line) {
    return new GwTokenizer(new Scanner(line), expressionParser.getSymbols());
  }

  Statement parseStatement(ExpressionParser.Tokenizer tokenizer) {
    String name = tokenizer.currentValue;
    if (tryConsume(tokenizer, "GO")) {  // GO TO, GO SUB -> GOTO, GOSUB
      name += tokenizer.currentValue;
    } else if (name.equals("?")) {
      name = "PRINT";
    }
    Statement.Type type = null;
    for (Statement.Type t : Statement.Type.values()) {
      if (name.equalsIgnoreCase(t.name())) {
        type = t;
        break;
      }
    }
    if (type == null) {
      type = Statement.Type.LET;
    } else {
      tokenizer.nextToken();
    }
    switch (type) {
      case RUN:  // 0 or 1 param; Default is 0
      case RESTORE:
        if (tokenizer.currentType != ExpressionParser.Tokenizer.TokenType.EOF &&
            !tokenizer.currentValue.equals(":")) {
          return new Statement(interpreter, type, expressionParser.parse(null, tokenizer));
        }
        return new Statement(interpreter, type);

      case DEF:  // Exactly one param
      case GOTO:
      case GOSUB:
      case LOAD:
        return new Statement(interpreter, type, expressionParser.parse(null, tokenizer));

      case NEXT:   // Zero of more
        ArrayList<Node> vars = new ArrayList<>();
        if (tokenizer.currentType != ExpressionParser.Tokenizer.TokenType.EOF &&
            !tokenizer.currentValue.equals(":")) {
          do {
            vars.add(expressionParser.parse(null, tokenizer));
          } while (tokenizer.tryConsume(","));
        }
        return new Statement(interpreter, type, vars.toArray(new Node[vars.size()]));

      case DATA:  // One or more params
      case DIM:
      case READ: {
        ArrayList<Node> expressions = new ArrayList<>();
        do {
          expressions.add(expressionParser.parse(null, tokenizer));
        } while (tokenizer.tryConsume(","));
        return new Statement(interpreter, type, expressions.toArray(new Node[expressions.size()]));
      }

      case FOR: {
        Node assignment = expressionParser.parse(null, tokenizer);
        if (!(assignment instanceof Operator) || !(assignment.children[0] instanceof Variable)
            || assignment.children[0].children.length != 0
            || !((Operator) assignment).name.equals("=")) {
          throw new RuntimeException("LocalVariable assignment expected after FOR");
        }
        require(tokenizer, "TO");
        Node end = expressionParser.parse(null, tokenizer);
        if (tryConsume(tokenizer, "STEP")) {
          return new Statement(interpreter, type, new String[]{" = ", " TO ", " STEP "},
              assignment.children[0], assignment.children[1], end,
              expressionParser.parse(null, tokenizer));
        }
        return new Statement(interpreter, type, new String[]{" = ", " TO "},
            assignment.children[0], assignment.children[1], end);
      }

      case IF:
        Node condition = expressionParser.parse(null, tokenizer);
        if (!tryConsume(tokenizer, "THEN") && !tryConsume(tokenizer, "GOTO")) {
          throw tokenizer.exception("'THEN expected after IF-condition.'", null);
        }
        if (tokenizer.currentType == ExpressionParser.Tokenizer.TokenType.NUMBER) {
          double target = (int) Double.parseDouble(tokenizer.currentValue);
          tokenizer.nextToken();
          return new Statement(interpreter, type, new String[]{" THEN "}, condition,
              new Literal(target));
        }
        return new Statement(interpreter, type, new String[]{" THEN"}, condition);

      case INPUT:
      case PRINT:
        List<Node> args = new ArrayList<>();
        List<String> delimiter = new ArrayList<>();
        while (tokenizer.currentType != ExpressionParser.Tokenizer.TokenType.EOF
            && !tokenizer.currentValue.equals(":")) {
          if (tokenizer.currentValue.equals(",") || tokenizer.currentValue.equals(";")) {
            delimiter.add(tokenizer.currentValue + " ");
            tokenizer.nextToken();
            if (delimiter.size() > args.size()) {
              args.add(new Literal(Interpreter.INVISIBLE_STRING));
            }
          } else {
            args.add(expressionParser.parse(null, tokenizer));
          }
        }
        return new Statement(interpreter, type, delimiter.toArray(new String[delimiter.size()]),
            args.toArray(new Node[args.size()]));

      case LET: {
        Node assignment = expressionParser.parse(null, tokenizer);
        if (!(assignment instanceof Operator) || !(assignment.children[0] instanceof Variable)
            || !((Operator) assignment).name.equals("=")) {
          throw tokenizer.exception("Unrecognized statement or illegal assignment: '"
              + assignment + "'.", null);
        }
        return new Statement(interpreter, type, new String[]{" = "}, assignment.children);
      }
      case ON: {
        List<Node> expressions = new ArrayList<Node>();
        expressions.add(expressionParser.parse(null, tokenizer));
        String[] kind = new String[1];
        if (tryConsume(tokenizer, "GOTO")) {
          kind[0] = " GOTO ";
        } else if (tryConsume(tokenizer, "GOSUB")) {
          kind[0] = " GOSUB ";
        } else {
          throw tokenizer.exception("GOTO or GOSUB expected.", null);
        }
        do {
          expressions.add(expressionParser.parse(null, tokenizer));
        } while (tokenizer.tryConsume(","));
        return new Statement(interpreter, type, kind,
            expressions.toArray(new Node[expressions.size()]));
      }
      case REM: {
        StringBuilder sb = new StringBuilder();
        while (tokenizer.currentType != ExpressionParser.Tokenizer.TokenType.EOF) {
          sb.append(tokenizer.leadingWhitespace).append(tokenizer.currentValue);
          tokenizer.nextToken();
        }
        if (sb.length() > 0 && sb.charAt(0) == ' ') {
          sb.deleteCharAt(0);
        }
        return new Statement(interpreter, type, new Variable(interpreter, sb.toString()));
      }
      default:
        return new Statement(interpreter, type);
    }
  }

  List<Statement> parseStatementList(ExpressionParser.Tokenizer tokenizer) {
    ArrayList<Statement> result = new ArrayList<>();
    Statement statement;
    do {
      while (tokenizer.tryConsume(":")) {
        result.add(new Statement(interpreter, null));
      }
      if (tokenizer.currentType == ExpressionParser.Tokenizer.TokenType.EOF) {
        break;
      }
      statement = parseStatement(tokenizer);
      result.add(statement);
    } while (statement.type == Statement.Type.IF ? statement.children.length == 1
        : tokenizer.tryConsume(":"));
    if (tokenizer.currentType != ExpressionParser.Tokenizer.TokenType.EOF) {
      throw tokenizer.exception("Leftover input.", null);
    }
    return result;
  }

  void require(ExpressionParser.Tokenizer tokenizer, String s) {
    if (!tryConsume(tokenizer, s)) {
      throw tokenizer.exception("Expected: '" + s + "'.", null);
    }
  }

  boolean tryConsume(ExpressionParser.Tokenizer tokenizer, String s) {
    if (tokenizer.currentValue.equalsIgnoreCase(s)) {
      tokenizer.nextToken();
      return true;
    }
    return false;
  }

  /**
   * A tokenizer subclass that splits identifiers if they contain reserved words,
   * so it will report "IFA<4THENPRINTZ" as "IF" "A" "<" "4" "THEN" "PRINT" "Z"
   */
  static class GwTokenizer extends ExpressionParser.Tokenizer {
    static Pattern reservedWordPattern;
    static {
      StringBuilder sb = new StringBuilder();
      for (Statement.Type t: Statement.Type.values()) {
        sb.append(t.name());
        sb.append('|');
      }
      sb.append("AND|ELSE|NOT|OR|STEP|TO|THEN");
      reservedWordPattern = Pattern.compile(sb.toString());
    }

    Matcher gwMatcher;
    String gwIdentifier;
    int gwConsumed = 0;

    GwTokenizer(Scanner scanner, Iterable<String> symbols) {
      super(scanner, symbols, ":", ";", "?");
      stringPattern = Pattern.compile("\\G\\s*(\"[^\"]*\")+");
    }

    private TokenType gwToken(int start, int end) {
      currentValue = gwIdentifier.substring(start, end);
      if (end == gwIdentifier.length()) {
        gwIdentifier = null;
        gwMatcher = null;
        gwConsumed = 0;
      } else {
        gwConsumed = end;
      }
      currentType = currentValue.matches("\\d+") ? TokenType.NUMBER : TokenType.IDENTIFIER;
      return currentType;
    }

    public TokenType nextToken() {
      if (gwIdentifier != null && gwConsumed < gwIdentifier.length()) {
        if (gwConsumed == gwMatcher.start()) {
          return gwToken(gwConsumed, gwMatcher.end());
        }
        if (gwMatcher.find()) {
          return gwToken(gwConsumed, gwMatcher.start() > gwConsumed
              ? gwMatcher.start() : gwMatcher.end());
        }
        return gwToken(gwConsumed, gwIdentifier.length());
      }

      super.nextToken();

      if (currentType == ExpressionParser.Tokenizer.TokenType.IDENTIFIER) {
        gwMatcher = reservedWordPattern.matcher(currentValue);
        if (gwMatcher.find()) {
          gwIdentifier = currentValue;
          return gwToken(0, gwMatcher.start() == 0 ? gwMatcher.end() : gwMatcher.start());
        }
        gwMatcher = null;
      }
      return currentType;
    }
  }


  /**
   * This class configures and manages the parser and is able to turn the expression parser
   * callbacks into an expression node tree.
   */
  class ExpressionBuilder extends ExpressionParser.Processor<Node, Void> {

    @Override
    public Node call(Void context, ExpressionParser.Tokenizer tokenizer, String name, String bracket, List<Node> arguments) {
      Node[] children = arguments.toArray(new Node[arguments.size()]);
      for (Builtin.Type builtinId: Builtin.Type.values()) {
        if (name.equalsIgnoreCase(builtinId.name())) {
          String signature = builtinId.signature;
          if (arguments.size() > signature.length() ||
              arguments.size() < builtinId.minParams ) {
            throw new IllegalArgumentException("Parameter count mismatch.");
          }
          for (int i = 0; i < arguments.size(); i++) {
            if (signature.charAt(i) != arguments.get(i).returnType().getSimpleName().charAt(0)) {
              throw new RuntimeException("Parameter number " + i + " type mismatch.");
            }
          }
          return new Builtin(interpreter, builtinId, children);
        }
      }
      name = name.toLowerCase();
      if (name.startsWith("fn") && name.length() > 2) {
        return new FnCall(interpreter, name, children);
      }
      if (name.length() > 2) {
        System.out.println("Unsupported Function? " + name);
      }
      for (int i = 0; i < arguments.size(); i++) {
        if (arguments.get(i).returnType() != Double.class) {
          throw new IllegalArgumentException("Numeric array index expected.");
        }
      }
      return new Variable(interpreter, name, children);
    }

    @Override
    public Node prefixOperator(Void context, ExpressionParser.Tokenizer tokenizer, String name, Node param) {
      if (param.returnType() != Double.class) {
        throw new IllegalArgumentException("Numeric argument expected for '" + name + "'.");
      }
      if (name.equalsIgnoreCase("NOT")) {
        return new Builtin(interpreter, Builtin.Type.NOT, param);
      }
      if (name.equals("-")) {
        return new Builtin(interpreter, Builtin.Type.NEG, param);
      }
      if (name.equals("+")) {
        return param;
      }
      return super.prefixOperator(context, tokenizer, name, param);
    }

    @Override
    public Node infixOperator(Void context, ExpressionParser.Tokenizer tokenizer, String name, Node left, Node right) {
      if ("+<=<>=".indexOf(name) == -1 && (left.returnType() != Double.class ||
          right.returnType() != Double.class)) {
        throw new IllegalArgumentException("Numeric arguments expected for '" + name + "'.");
      }
      return new Operator(name.toLowerCase(), left, right);
    }

    @Override
    public Node group(Void context, ExpressionParser.Tokenizer tokenizer, String bracket, List<Node> args) {
      return new Builtin(interpreter, null, args.get(0));
    }

    @Override public Node identifier(Void context, ExpressionParser.Tokenizer tokenizer, String name) {
      if (name.equalsIgnoreCase(Builtin.Type.RND.name())) {
        return new Builtin(interpreter, Builtin.Type.RND);
      }
      name = name.toLowerCase();
      if (name.startsWith("fn") && name.length() > 2) {
        return new FnCall(interpreter, name);
      }
      return new Variable(interpreter, name);
    }

    @Override public Node numberLiteral(Void context, ExpressionParser.Tokenizer tokenizer, String value) {
      return new Literal(Double.parseDouble(value));
    }

    @Override
    public Node stringLiteral(Void context, ExpressionParser.Tokenizer tokenizer, String value) {
      return new Literal(value.substring(1, value.length()-1).replace("\"\"", "\""));
    }
  }
}
