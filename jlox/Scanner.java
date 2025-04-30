import java.util.List;
import java.util.ArrayList;
import java.util.Map;

class Scanner {
  private static final Map<String, TokenType> keywords = Map.ofEntries(
    Map.entry("nil", TokenType.NIL),
    Map.entry("true", TokenType.TRUE),
    Map.entry("false", TokenType.FALSE),
    Map.entry("if", TokenType.IF),
    Map.entry("else", TokenType.ELSE),
    Map.entry("and", TokenType.AND),
    Map.entry("or", TokenType.OR),
    Map.entry("for", TokenType.FOR),
    Map.entry("while", TokenType.WHILE),
    Map.entry("fun", TokenType.FUN),
    Map.entry("return", TokenType.RETURN),
    Map.entry("break", TokenType.BREAK),
    Map.entry("continue", TokenType.CONTINUE),
    Map.entry("var", TokenType.VAR),
    Map.entry("class", TokenType.CLASS),
    Map.entry("super", TokenType.SUPER),
    Map.entry("this", TokenType.THIS)
  );

  private final String source;
  private int start = 0;
  private int current = 0;
  private int line = 1;

  Scanner(String source) {
    this.source = source;
  }

  List<Token> scanTokens() {
    var tokens = new ArrayList<Token>();

    while(!isAtEnd()) {
      start = current;

      var startLine = line;
      var token = scanToken();
      if (token == null) {
        if (line != startLine && !tokens.isEmpty()) {
          if (insertSemicolon(tokens.getLast().type())) {
            tokens.add(createToken(TokenType.SEMICOLON));
          }
        }

        continue;
      }

      tokens.add(token);
    }

    if (!tokens.isEmpty()) {
      if (insertSemicolon(tokens.getLast().type())) {
        tokens.add(createToken(TokenType.SEMICOLON));
      }
    }

    tokens.add(new Token(TokenType.EOF, "", null, line));
    return tokens;
  }

  private Token scanToken() {
    var c = advance();

    return switch (c) {
      case '(' -> createToken(TokenType.LEFT_PAREN);
      case ')' -> createToken(TokenType.RIGHT_PAREN);
      case '{' -> createToken(TokenType.LEFT_BRACE);
      case '}' -> createToken(TokenType.RIGHT_BRACE);
      case ',' -> createToken(TokenType.COMMA);
      case '.' -> createToken(TokenType.DOT);
      case ';' -> createToken(TokenType.SEMICOLON);
      case '+' -> createToken(TokenType.PLUS);
      case '-' -> createToken(TokenType.MINUS);
      case '*' -> createToken(TokenType.STAR);
      case '%' -> createToken(TokenType.PERCENT);
      case '/' -> {
        if (match('/')) {
          while (peek() != '\n' && !isAtEnd()) {
            advance();
          }

          yield null;
        } else {
          yield createToken(TokenType.SLASH);
        }
      }
      case '!' -> createToken(match('=') ? TokenType.BANG_EQUAL : TokenType.BANG);
      case '=' -> createToken(match('=') ? TokenType.EQUAL_EQUAL: TokenType.EQUAL);
      case '>' -> createToken(match('=') ? TokenType.GREATER_EQUAL: TokenType.GREATER);
      case '<' -> createToken(match('=') ? TokenType.LESS_EQUAL: TokenType.LESS);
      case '"' -> string();
      default -> {
        if (isDigit(c)) {
          yield number();
        } else if (isAlpha(c)) {
          yield identifier();
        } else {
          if (c == '\n') {
            line++;
          }

          if (!isWhitespace(c)) {
            Lox.error(line, String.format("Unexpected character ''%c'.", c));
          }

          yield null;
        }
      }
    };
  }

  private Token string() {
    while (peek() != '"' && !isAtEnd()) {
      if (peek() == '\n') {
        line++;
      }

      advance();
    }

    if (isAtEnd()) {
      Lox.error(line, "Unterminated string.");
      return null;
    }

    advance(); // Consume closing quote

    var value = source.substring(start + 1, current - 1); // Omit surrounding quotes
    return createToken(TokenType.STRING, value);
  }

  private Token number() {
    while (isDigit(peek())) {
      advance();
    }

    if (peek() == '.' && isDigit(peekNext())) {
      advance(); // Consume .

      while (isDigit(peek())) {
        advance();
      }
    }

    var value = Double.parseDouble(source.substring(start, current));
    return createToken(TokenType.NUMBER, value);
  }

  private Token identifier() {
    while (isAlphaNumeric(peek())) {
      advance();
    }

    var value = source.substring(start, current);
    var type = keywords.get(value);
    if (type == null) {
      type = TokenType.IDENTIFIER;
    }

    return createToken(type);
  }

  private boolean isWhitespace(char c) {
    return c == 0x09 || c == 0x0A || c == 0x0D || c == 0x20;
  }

  private boolean isDigit(char c) {
    return c >= '0' && c <= '9';
  }

  private boolean isAlpha(char c) {
    return (c >= 'a' && c <= 'z') ||
            (c >= 'A' && c <= 'Z') ||
            c == '_';
  }

  private boolean isAlphaNumeric(char c) {
    return isDigit(c) || isAlpha(c);
  }

  private char advance() {
    return source.charAt(current++);
  }

  private boolean match(char expected) {
    if (isAtEnd() || peek() != expected) {
      return false;
    }

    current++;
    return true;
  }

  private char peek() {
    if (isAtEnd()) {
      return '\0';
    }

    return source.charAt(current);
  }

  private char peekNext() {
    if (current + 1 >= source.length()) {
      return '\0';
    }

    return source.charAt(current + 1);
  }

  private boolean isAtEnd() {
    return current >= source.length();
  }

  private Token createToken(TokenType type) {
    return createToken(type, null);
  }

  private Token createToken(TokenType type, Object literal) {
    var text = source.substring(start, current);
    return new Token(type, text, literal, line);
  }

  private boolean insertSemicolon(TokenType type) {
    return switch (type) {
      case TokenType.IDENTIFIER,
            TokenType.NUMBER,
            TokenType.STRING,
            TokenType.NIL,
            TokenType.TRUE,
            TokenType.FALSE,
            TokenType.THIS,
            TokenType.RETURN,
            TokenType.BREAK,
            TokenType.CONTINUE,
            TokenType.RIGHT_PAREN
            -> true;
      default -> false;
    };
  }
}
