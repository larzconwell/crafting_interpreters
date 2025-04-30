enum TokenType {
  LEFT_PAREN("("),
  RIGHT_PAREN(")"),
  LEFT_BRACE("{"),
  RIGHT_BRACE("}"),
  COMMA(","),
  DOT("."),
  SEMICOLON(";"),

  PLUS("+"),
  MINUS("-"),
  STAR("*"),
  SLASH("/"),
  PERCENT("%"),

  BANG("!"),
  EQUAL("="),
  GREATER(">"),
  LESS("<"),
  BANG_EQUAL("!="),
  EQUAL_EQUAL("=="),
  GREATER_EQUAL(">="),
  LESS_EQUAL("<="),

  IDENTIFIER("identifier"),
  STRING("string"),
  NUMBER("number"),
  NIL("nil"),

  TRUE("true"),
  FALSE("false"),

  IF("if"),
  ELSE("else"),
  AND("and"),
  OR("or"),
  FOR("for"),
  WHILE("while"),

  FUN("fun"),
  RETURN("return"),
  BREAK("break"),
  VAR("var"),

  CLASS("class"),
  SUPER("super"),
  THIS("this"),

  EOF("eof");

  private final String literal;

  TokenType(final String literal) {
    this.literal = literal;
  }

  @Override
  public String toString() {
    return literal;
  }
}
