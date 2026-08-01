package com.masson.cruciblecraft.recipe.rule;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Small, typed expression language for material rules. It deliberately has no
 * reflection, script-engine, allocation, or arbitrary call escape hatch.
 */
public final class RuleExpression {
    private static final double MAX_SAFE_INTEGER = 9_007_199_254_740_991.0;
    public enum Type { NUMBER, BOOLEAN }

    public interface Context {
        double number(String name);
        default double lookup(String function, String key) {
            throw new IllegalArgumentException("lookup is unavailable: " + function + "(" + key + ")");
        }
        boolean materialHas(String flag);
        boolean hasPrefix(String prefix);
    }

    private final String source;
    private final String ruleId;
    private final Node root;
    private final Type type;

    private RuleExpression(String source, String ruleId, Node root, Type type) {
        this.source = source;
        this.ruleId = ruleId;
        this.root = root;
        this.type = type;
    }

    public static RuleExpression numeric(String source, String ruleId) {
        return compile(source, ruleId, Type.NUMBER);
    }

    public static RuleExpression bool(String source, String ruleId) {
        return compile(source, ruleId, Type.BOOLEAN);
    }

    public double evaluateNumber(Context context) {
        if (type != Type.NUMBER) {
            throw failure("expression is not numeric");
        }
        try {
            return finite(root.eval(context).number(), "non-finite result");
        } catch (IllegalArgumentException exception) {
            throw failure(exception.getMessage());
        }
    }

    public boolean evaluateBoolean(Context context) {
        if (type != Type.BOOLEAN) {
            throw failure("expression is not boolean");
        }
        try {
            return root.eval(context).bool();
        } catch (IllegalArgumentException exception) {
            throw failure(exception.getMessage());
        }
    }

    public long evaluateLong(Context context) {
        double value = evaluateNumber(context);
        if (value != Math.rint(value)
                || value < -MAX_SAFE_INTEGER
                || value > MAX_SAFE_INTEGER) {
            throw failure("result is not an exact long: " + value);
        }
        return (long) value;
    }

    public int evaluateInt(Context context) {
        long value = evaluateLong(context);
        if (value < Integer.MIN_VALUE || value > Integer.MAX_VALUE) {
            throw failure("integer overflow: " + value);
        }
        return (int) value;
    }

    public boolean usesLookup() {
        return source.contains("target_units(") || source.contains("prefix_units(");
    }

    private static RuleExpression compile(String source, String ruleId, Type expected) {
        if (source == null || source.isBlank()) {
            throw new IllegalArgumentException(label(ruleId) + "empty expression");
        }
        try {
            Parser parser = new Parser(source);
            Node root = parser.parse();
            if (root.type() != expected) {
                throw new IllegalArgumentException(
                        "expected " + expected.name().toLowerCase(Locale.ROOT)
                                + " expression, got " + root.type().name().toLowerCase(Locale.ROOT));
            }
            return new RuleExpression(source, ruleId, root, expected);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(
                    label(ruleId) + "'" + source + "': " + exception.getMessage(),
                    exception);
        }
    }

    private IllegalArgumentException failure(String message) {
        return new IllegalArgumentException(label(ruleId) + "'" + source + "': " + message);
    }

    private static String label(String ruleId) {
        return "material rule " + (ruleId == null || ruleId.isBlank() ? "<unknown>" : ruleId) + ": ";
    }

    private static double finite(double value, String message) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }

    private static double safeArithmetic(double value) {
        finite(value, "numeric overflow");
        if (value == Math.rint(value) && Math.abs(value) > MAX_SAFE_INTEGER) {
            throw new IllegalArgumentException(
                    "integer arithmetic exceeds the exact double range \u00b1(2^53-1)");
        }
        return value;
    }

    private sealed interface Node permits Literal, Variable, Unary, Binary, Function, Lookup, Predicate {
        Type type();
        Value eval(Context context);
    }

    private record Value(Type type, double number, boolean bool) {
        static Value number(double value) { return new Value(Type.NUMBER, finite(value, "non-finite result"), false); }
        static Value bool(boolean value) { return new Value(Type.BOOLEAN, 0.0, value); }
        public double number() {
            if (type != Type.NUMBER) throw new IllegalArgumentException("numeric value required");
            return number;
        }
        public boolean bool() {
            if (type != Type.BOOLEAN) throw new IllegalArgumentException("boolean value required");
            return bool;
        }
    }

    private record Literal(Value value) implements Node {
        @Override public Type type() { return value.type(); }
        @Override public Value eval(Context context) { return value; }
    }

    private record Variable(String name) implements Node {
        @Override public Type type() { return Type.NUMBER; }
        @Override public Value eval(Context context) { return Value.number(context.number(name)); }
    }

    private record Unary(String operator, Node operand, Type type) implements Node {
        @Override
        public Value eval(Context context) {
            return switch (operator) {
                case "-" -> Value.number(finite(-operand.eval(context).number(), "numeric overflow"));
                case "+" -> Value.number(operand.eval(context).number());
                case "!" -> Value.bool(!operand.eval(context).bool());
                default -> throw new IllegalArgumentException("unknown unary operator " + operator);
            };
        }
    }

    private record Binary(String operator, Node left, Node right, Type type) implements Node {
        @Override
        public Value eval(Context context) {
            if (operator.equals("&&")) {
                return Value.bool(left.eval(context).bool() && right.eval(context).bool());
            }
            if (operator.equals("||")) {
                return Value.bool(left.eval(context).bool() || right.eval(context).bool());
            }
            if (operator.equals("==") || operator.equals("!=")) {
                Value a = left.eval(context);
                Value b = right.eval(context);
                boolean equal = a.type == Type.NUMBER
                        ? Double.compare(a.number(), b.number()) == 0
                        : a.bool() == b.bool();
                return Value.bool(operator.equals("==") ? equal : !equal);
            }
            double a = left.eval(context).number();
            double b = right.eval(context).number();
            return switch (operator) {
                case "+" -> Value.number(safeArithmetic(a + b));
                case "-" -> Value.number(safeArithmetic(a - b));
                case "*" -> Value.number(safeArithmetic(a * b));
                case "/" -> {
                    if (b == 0.0) throw new IllegalArgumentException("divide by zero");
                    yield Value.number(safeArithmetic(a / b));
                }
                case "%" -> {
                    if (b == 0.0) throw new IllegalArgumentException("divide by zero");
                    yield Value.number(safeArithmetic(a % b));
                }
                case "<" -> Value.bool(a < b);
                case "<=" -> Value.bool(a <= b);
                case ">" -> Value.bool(a > b);
                case ">=" -> Value.bool(a >= b);
                default -> throw new IllegalArgumentException("unknown operator " + operator);
            };
        }
    }

    private record Function(String name, List<Node> arguments) implements Node {
        @Override public Type type() { return Type.NUMBER; }
        @Override
        public Value eval(Context context) {
            double value = switch (name) {
                case "min" -> arguments.stream().mapToDouble(node -> node.eval(context).number()).min().orElseThrow();
                case "max" -> arguments.stream().mapToDouble(node -> node.eval(context).number()).max().orElseThrow();
                case "ceil" -> Math.ceil(arguments.getFirst().eval(context).number());
                case "floor" -> Math.floor(arguments.getFirst().eval(context).number());
                case "gcd" -> {
                    long left = positiveSafeInteger(arguments.get(0).eval(context).number(), "gcd");
                    long right = positiveSafeInteger(arguments.get(1).eval(context).number(), "gcd");
                    while (right != 0L) {
                        long remainder = left % right;
                        left = right;
                        right = remainder;
                    }
                    yield left;
                }
                case "tier_voltage" -> {
                    double tier = arguments.getFirst().eval(context).number();
                    if (tier != Math.rint(tier) || tier < 0 || tier > 15) {
                        throw new IllegalArgumentException("tier_voltage requires an integer tier from 0 to 15");
                    }
                    yield 8.0 * Math.pow(4.0, tier);
                }
                default -> throw new IllegalArgumentException("unknown function " + name);
            };
            return Value.number(finite(value, "non-finite function result"));
        }
    }

    private static long positiveSafeInteger(double value, String function) {
        if (value != Math.rint(value)
                || value <= 0.0
                || value > MAX_SAFE_INTEGER) {
            throw new IllegalArgumentException(
                    function + " requires positive exact safe integers");
        }
        return (long) value;
    }

    private record Lookup(String function, String key) implements Node {
        @Override public Type type() { return Type.NUMBER; }
        @Override public Value eval(Context context) {
            return Value.number(safeArithmetic(context.lookup(function, key)));
        }
    }

    private record Predicate(String name, String argument) implements Node {
        @Override public Type type() { return Type.BOOLEAN; }
        @Override
        public Value eval(Context context) {
            return Value.bool(switch (name) {
                case "material.has" -> context.materialHas(argument);
                case "has_prefix" -> context.hasPrefix(argument);
                default -> throw new IllegalArgumentException("unknown predicate " + name);
            });
        }
    }

    private static final class Parser {
        private final Lexer lexer;
        private Token token;

        private Parser(String source) {
            lexer = new Lexer(source);
            token = lexer.next();
        }

        private Node parse() {
            Node result = parseOr();
            expect(TokenKind.END);
            return result;
        }

        private Node parseOr() {
            Node left = parseAnd();
            while (accept("||")) {
                Node right = parseAnd();
                require(left, Type.BOOLEAN, "||");
                require(right, Type.BOOLEAN, "||");
                left = new Binary("||", left, right, Type.BOOLEAN);
            }
            return left;
        }

        private Node parseAnd() {
            Node left = parseEquality();
            while (accept("&&")) {
                Node right = parseEquality();
                require(left, Type.BOOLEAN, "&&");
                require(right, Type.BOOLEAN, "&&");
                left = new Binary("&&", left, right, Type.BOOLEAN);
            }
            return left;
        }

        private Node parseEquality() {
            Node left = parseComparison();
            while (token.text.equals("==") || token.text.equals("!=")) {
                String operator = consume().text;
                Node right = parseComparison();
                if (left.type() != right.type()) {
                    throw error(operator + " operands must have the same type");
                }
                left = new Binary(operator, left, right, Type.BOOLEAN);
            }
            return left;
        }

        private Node parseComparison() {
            Node left = parseAdd();
            while (List.of("<", "<=", ">", ">=").contains(token.text)) {
                String operator = consume().text;
                Node right = parseAdd();
                require(left, Type.NUMBER, operator);
                require(right, Type.NUMBER, operator);
                left = new Binary(operator, left, right, Type.BOOLEAN);
            }
            return left;
        }

        private Node parseAdd() {
            Node left = parseMultiply();
            while (token.text.equals("+") || token.text.equals("-")) {
                String operator = consume().text;
                Node right = parseMultiply();
                require(left, Type.NUMBER, operator);
                require(right, Type.NUMBER, operator);
                left = new Binary(operator, left, right, Type.NUMBER);
            }
            return left;
        }

        private Node parseMultiply() {
            Node left = parseUnary();
            while (List.of("*", "/", "%").contains(token.text)) {
                String operator = consume().text;
                Node right = parseUnary();
                require(left, Type.NUMBER, operator);
                require(right, Type.NUMBER, operator);
                left = new Binary(operator, left, right, Type.NUMBER);
            }
            return left;
        }

        private Node parseUnary() {
            if (List.of("+", "-", "!").contains(token.text)) {
                String operator = consume().text;
                Node operand = parseUnary();
                Type type = operator.equals("!") ? Type.BOOLEAN : Type.NUMBER;
                require(operand, type, operator);
                return new Unary(operator, operand, type);
            }
            return parsePrimary();
        }

        private Node parsePrimary() {
            if (token.kind == TokenKind.NUMBER) {
                String text = consume().text;
                try {
                    double value = Double.parseDouble(text);
                    if (value == Math.rint(value) && Math.abs(value) > MAX_SAFE_INTEGER) {
                        throw error(
                                "integer literal exceeds the exact double range \u00b1(2^53-1)");
                    }
                    return new Literal(Value.number(value));
                } catch (NumberFormatException exception) {
                    throw error("invalid numeric literal " + text);
                }
            }
            if (token.text.equals("(")) {
                consume();
                Node nested = parseOr();
                expect(")");
                return nested;
            }
            if (token.kind != TokenKind.IDENTIFIER) {
                throw error("expected literal, variable, function, or parenthesized expression");
            }
            String name = consume().text;
            if (name.equals("true") || name.equals("false")) {
                return new Literal(Value.bool(Boolean.parseBoolean(name)));
            }
            if (!accept("(")) {
                return new Variable(name);
            }
            if (name.equals("material.has") || name.equals("has_prefix")) {
                if (token.kind != TokenKind.IDENTIFIER && token.kind != TokenKind.STRING) {
                    throw error(name + " requires one flag/prefix argument");
                }
                String argument = consume().text;
                expect(")");
                return new Predicate(name, argument);
            }
            if (name.equals("target_units") || name.equals("prefix_units")) {
                if (token.kind != TokenKind.IDENTIFIER && token.kind != TokenKind.STRING) {
                    throw error(name + " requires one target/prefix argument");
                }
                String argument = consume().text;
                expect(")");
                return new Lookup(name, argument);
            }
            List<Node> arguments = new ArrayList<>();
            if (!token.text.equals(")")) {
                do {
                    arguments.add(parseOr());
                } while (accept(","));
            }
            expect(")");
            if (!List.of("min", "max", "ceil", "floor", "tier_voltage", "gcd").contains(name)) {
                throw error("unknown function " + name);
            }
            int minimum = List.of("min", "max", "gcd").contains(name) ? 2 : 1;
            int maximum = List.of("min", "max").contains(name)
                    ? Integer.MAX_VALUE
                    : List.of("gcd").contains(name) ? 2 : 1;
            if (arguments.size() < minimum || arguments.size() > maximum) {
                throw error(name + " received the wrong number of arguments");
            }
            arguments.forEach(argument -> require(argument, Type.NUMBER, name));
            return new Function(name, List.copyOf(arguments));
        }

        private boolean accept(String text) {
            if (!token.text.equals(text)) return false;
            consume();
            return true;
        }

        private void expect(String text) {
            if (!accept(text)) throw error("expected '" + text + "'");
        }

        private void expect(TokenKind kind) {
            if (token.kind != kind) throw error("unexpected token '" + token.text + "'");
        }

        private Token consume() {
            Token current = token;
            token = lexer.next();
            return current;
        }

        private void require(Node node, Type type, String operator) {
            if (node.type() != type) {
                throw error(operator + " requires " + type.name().toLowerCase(Locale.ROOT) + " operands");
            }
        }

        private IllegalArgumentException error(String message) {
            return new IllegalArgumentException(message + " at offset " + token.offset);
        }
    }

    private enum TokenKind { NUMBER, IDENTIFIER, STRING, OPERATOR, END }
    private record Token(TokenKind kind, String text, int offset) {}

    private static final class Lexer {
        private final String source;
        private int offset;

        private Lexer(String source) { this.source = source; }

        private Token next() {
            while (offset < source.length() && Character.isWhitespace(source.charAt(offset))) offset++;
            if (offset == source.length()) return new Token(TokenKind.END, "", offset);
            int start = offset;
            char current = source.charAt(offset);
            if (Character.isDigit(current) || current == '.') {
                offset++;
                while (offset < source.length()) {
                    char c = source.charAt(offset);
                    if (!Character.isDigit(c) && c != '.' && c != 'e' && c != 'E' && c != '+' && c != '-') break;
                    if ((c == '+' || c == '-') && source.charAt(offset - 1) != 'e' && source.charAt(offset - 1) != 'E') break;
                    offset++;
                }
                return new Token(TokenKind.NUMBER, source.substring(start, offset), start);
            }
            if (Character.isLetter(current) || current == '_') {
                offset++;
                while (offset < source.length()) {
                    char c = source.charAt(offset);
                    if (!Character.isLetterOrDigit(c)
                            && c != '_' && c != '.' && c != ':') break;
                    offset++;
                }
                return new Token(TokenKind.IDENTIFIER, source.substring(start, offset), start);
            }
            if (current == '"' || current == '\'') {
                char quote = current;
                offset++;
                StringBuilder result = new StringBuilder();
                while (offset < source.length() && source.charAt(offset) != quote) {
                    char c = source.charAt(offset++);
                    if (c == '\\' && offset < source.length()) c = source.charAt(offset++);
                    result.append(c);
                }
                if (offset >= source.length()) throw new IllegalArgumentException("unterminated string at offset " + start);
                offset++;
                return new Token(TokenKind.STRING, result.toString(), start);
            }
            String two = offset + 1 < source.length() ? source.substring(offset, offset + 2) : "";
            if (List.of("&&", "||", "==", "!=", "<=", ">=").contains(two)) {
                offset += 2;
                return new Token(TokenKind.OPERATOR, two, start);
            }
            if ("+-*/%<>()!,".indexOf(current) >= 0) {
                offset++;
                return new Token(TokenKind.OPERATOR, Character.toString(current), start);
            }
            throw new IllegalArgumentException("invalid character '" + current + "' at offset " + start);
        }
    }
}
