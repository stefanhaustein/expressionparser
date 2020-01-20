package org.kobjects.expressionparser;

import java.util.List;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class PrecedenceTest {

    static class TestProcessor extends Processor<String> {
        private String counterBracket(String bracket) {
            switch (bracket) {
                case "(": return ")";
                case "{": return "}";
                case "[": return "]";
                default:
                    throw new IllegalArgumentException("Unknown counter for: '" + bracket +"'");
            }
        }

        @Override
        public String infixOperator(Tokenizer tokenizer, String name, String left, String right) {
            return "(" + left + " " + name + " " + right + ")";
        }

        @Override
        public String implicitOperator(Tokenizer tokenizer, boolean strong, String left, String right) {
            return "(" + left + (strong ? "" : " ") + right + ")";
        }

        @Override
        public String prefixOperator(Tokenizer tokenizer, String name, String argument) {
            return "(" + name + " " + argument + ")";
        }

        @Override
        public String numberLiteral(Tokenizer tokenizer, String value) {
            return value;
        }

        @Override
        public String identifier(Tokenizer tokenizer, String name) {
            return name;
        }

        @Override
        public String group(Tokenizer tokenizer, String paren, List<String> elements) {
            return paren + elements + counterBracket(paren);
        }

        /** 
         * Delegates function calls to Math via reflection.
         */
        @Override
        public String apply(Tokenizer tokenizer, String left, String bracket, List<String> arguments) {
            return "(" + left + bracket + arguments + counterBracket(bracket) + ")";
        }

        /**
         * Creates a parser for this processor with matching operations and precedences set up.
         */
        static ExpressionParser<String> createParser() {
            ExpressionParser<String> parser = new ExpressionParser<String>(new TestProcessor());
            parser.addGroupBrackets("(", null, ")");
            parser.addOperators(OperatorType.INFIX, 7, ".");
            parser.addApplyBrackets(6, "(", ",", ")");
            parser.addOperators(OperatorType.INFIX_RTL, 5, "^");
            parser.addOperators(OperatorType.PREFIX, 4, "+", "-");
            parser.setImplicitOperatorPrecedence(true, 3);
            parser.setImplicitOperatorPrecedence(false, 3);
            parser.addOperators(OperatorType.INFIX, 2, "*", "/");
            parser.addOperators(OperatorType.INFIX, 1, "+", "-");
            return parser;
        }


    }

    static String parse(String input) {
        return TestProcessor.createParser().parse(input);
    }

    @Test
    public void testSimple() {
        assertEquals("(3 + 4)", parse("3 + 4"));
    }


    @Test
    public void testPath() {
        assertEquals("((a . b)([4]))", parse("a.b(4)"));
        assertEquals("((call([x])) . size)", parse("call(x).size"));
    }

}
