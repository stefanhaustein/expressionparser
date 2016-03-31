package org.kobjects.expressionparser.demo.thinscript.statement;

import org.kobjects.expressionparser.demo.thinscript.CodePrinter;
import org.kobjects.expressionparser.demo.thinscript.EvaluationContext;
import org.kobjects.expressionparser.demo.thinscript.parser.ParsingContext;
import org.kobjects.expressionparser.demo.thinscript.expression.Expression;

public class ExpressionStatement extends SimpleStatement {

  public ExpressionStatement(Expression expression) {
    super(expression);
  }

  @Override
  public Object eval(EvaluationContext context) {
    expression.eval(context);
    return NO_RESULT;
  }

  @Override
  public void print(CodePrinter cp) {
    expression.print(cp);
    cp.append("; ");
  }

}
