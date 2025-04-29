import java.util.List;
import java.util.ArrayList;

class Parser {
  private static class ParseError extends RuntimeException {}

  private interface ExprMatcher {
    Expr match();
  }

  private final List<Token> tokens;
  private int current = 0;

  Parser(List<Token> tokens) {
    this.tokens = tokens;
  }

  List<Stmt> parse() {
    var stmts = new ArrayList<Stmt>();

    while (!isAtEnd()) {
      stmts.add(declaration());
    }

    return stmts;
  }

  private Stmt declaration() {
    try {
      if (match(TokenType.VAR)) {
        return variableDeclaration();
      }

      if (match(TokenType.FUN)) {
        return functionDeclaration("function");
      }

      if (match(TokenType.CLASS)) {
        return classDeclaration();
      }

      return statement();
    } catch (ParseError error) {
      synchronize();
      return null;
    }
  }

  private Stmt.Var variableDeclaration() {
    var identifier = consume(TokenType.IDENTIFIER, "Expect identifier in variable declaration.");

    Expr value = null;
    if (match(TokenType.EQUAL)) {
      value = expression();
    }

    var expect = TokenType.SEMICOLON;
    consume(expect, String.format("Expect '%s' after declaration.", expect));

    return new Stmt.Var(identifier, value);
  }

  private Stmt.Function functionDeclaration(String kind) {
    var identifier = consume(TokenType.IDENTIFIER, String.format("Expect name for %s declaration.", kind));

    var expect = TokenType.LEFT_PAREN;
    consume(expect, String.format("Expect '%s' after %s name.", expect, kind));

    var parameters = new ArrayList<Token>();
    if (!check(TokenType.RIGHT_PAREN)) {
      while (true) {
        if (parameters.size() >= 255) {
          error(peek(), "Cannot have more than 255 parameters.");
        }

        var parameter = consume(TokenType.IDENTIFIER, String.format("Expect parameter name or '%s'.", TokenType.RIGHT_PAREN));
        parameters.add(parameter);

        if (!match(TokenType.COMMA)) {
          break;
        }
      }
    }

    expect = TokenType.RIGHT_PAREN;
    consume(expect, String.format("Expect '%s' after %s paremeters.", expect, kind));

    expect = TokenType.LEFT_BRACE;
    consume(expect, String.format("Expect '%s' before block.", expect));

    return new Stmt.Function(identifier, parameters, block());
  }

  private Stmt.Class classDeclaration() {
    var identifier = consume(TokenType.IDENTIFIER, "Expect name for class declaration.");

    Expr.Var superclass = null;
    if (match(TokenType.LESS)) {
      var superidentifier = consume(TokenType.IDENTIFIER, "Expect superclass name for class declaration.");
      superclass = new Expr.Var(superidentifier);
    }

    var expect = TokenType.LEFT_BRACE;
    consume(expect, String.format("Expect '%s' after class name.", expect));

    var methods = new ArrayList<Stmt.Function>();
    while (!check(TokenType.RIGHT_BRACE) && !isAtEnd()) {
      methods.add(functionDeclaration("method"));
    }

    expect = TokenType.RIGHT_BRACE;
    consume(expect, String.format("Expect '%s' after class block.", expect));

    return new Stmt.Class(identifier, superclass, methods);
  }

  private Stmt statement() {
    if (match(TokenType.RETURN)) {
      return returnStatement();
    }

    if (match(TokenType.LEFT_BRACE)) {
      return blockStatement();
    }

    if (match(TokenType.IF)) {
      return ifStatement();
    }

    if (match(TokenType.WHILE)) {
      return whileStatement();
    }

    if (match(TokenType.FOR)) {
      return forStatement();
    }

    return expressionStatement();
  }

  private Stmt.Return returnStatement() {
    var keyword = previous();

    Expr expr = null;
    if (!check(TokenType.SEMICOLON)) {
      expr = expression();
    }

    var expect = TokenType.SEMICOLON;
    consume(expect, String.format("Expect '%s' after return.", expect));

    return new Stmt.Return(keyword, expr);
  }

  private Stmt.Block blockStatement() {
    return new Stmt.Block(block());
  }

  private Stmt.If ifStatement() {
    var expect = TokenType.LEFT_PAREN;
    consume(expect, String.format("Expect '%s' after 'if'.", expect));

    var condition = expression();

    expect = TokenType.RIGHT_PAREN;
    consume(expect, String.format("Expect '%s' after 'if' condition.", expect));

    var ifStmt = statement();

    Stmt elseStmt = null;
    if (match(TokenType.ELSE)) {
      elseStmt = statement();
    }

    return new Stmt.If(condition, ifStmt, elseStmt);
  }

  private Stmt.While whileStatement() {
    var expect = TokenType.LEFT_PAREN;
    consume(expect, String.format("Expect '%s' after 'while'.", expect));

    var condition = expression();

    expect = TokenType.RIGHT_PAREN;
    consume(expect, String.format("Expect '%s' after 'while' condition.", expect));

    var stmt = statement();
    return new Stmt.While(condition, stmt);
  }

  private Stmt forStatement() {
    var expect = TokenType.LEFT_PAREN;
    consume(expect, String.format("Expect '%s' after 'for'.", expect));

    Stmt initializer;
    if (match(TokenType.SEMICOLON)) {
      initializer = null;
    } else if (match(TokenType.VAR)) {
      initializer = variableDeclaration();
    } else {
      initializer = expressionStatement();
    }

    Expr condition = new Expr.Literal(true);
    if (!match(TokenType.SEMICOLON)) {
      condition = expression();
    }

    expect = TokenType.SEMICOLON;
    consume(expect, String.format("Expect '%s' after 'for' condition.", expect));

    Expr increment = null;
    if (!match(TokenType.RIGHT_PAREN)) {
      increment = expression();
    }

    expect = TokenType.RIGHT_PAREN;
    consume(expect, String.format("Expect '%s' after 'for' increment.", expect));

    var stmt = statement();
    if (increment != null) {
      stmt = new Stmt.Block(List.of(
        stmt,
        new Stmt.ExprStmt(increment)
      ));
    }

    stmt = new Stmt.While(condition, stmt);

    if (initializer != null) {
      stmt = new Stmt.Block(List.of(
        initializer,
        stmt
      ));
    }

    return stmt;
  }

  private Stmt.ExprStmt expressionStatement() {
    var expr = expression();

    var expect = TokenType.SEMICOLON;
    consume(expect, String.format("Expect '%s' after expression.", expect));

    return new Stmt.ExprStmt(expr);
  }

  private Expr expression() {
    return assignment();
  }

  private Expr assignment() {
    var expr = or();

    if (match(TokenType.EQUAL)) {
      var equals = previous();
      var value = assignment();

      if (expr instanceof Expr.Var) {
        var identifier = ((Expr.Var)expr).identifier();
        return new Expr.Assign(identifier, value);
      } else if (expr instanceof Expr.InstanceGet) {
        var get = (Expr.InstanceGet)expr;
        return new Expr.InstanceSet(get.instance(), get.identifier(), value);
      }

      error(equals, "Invalid assignment identifier");
    }

    return expr;
  }

  private Expr or() {
    return logicalExpr(() -> and(), TokenType.OR);
  }

  private Expr and() {
    return logicalExpr(() -> equality(), TokenType.AND);
  }

  private Expr equality() {
    return binaryExpr(() -> comparison(), TokenType.EQUAL_EQUAL, TokenType.BANG_EQUAL);
  }

  private Expr comparison() {
    return binaryExpr(() -> term(), TokenType.GREATER, TokenType.LESS, TokenType.GREATER_EQUAL, TokenType.LESS_EQUAL);
  }

  private Expr term() {
    return binaryExpr(() -> factor(), TokenType.PLUS, TokenType.MINUS);
  }

  private Expr factor() {
    return binaryExpr(() -> unary(), TokenType.STAR, TokenType.SLASH);
  }

  private Expr unary() {
    if (match(TokenType.MINUS, TokenType.BANG)) {
      var operator = previous();
      var expr = unary();

      return new Expr.Unary(operator, expr);
    }

    return call();
  }

  private Expr call() {
    var expr = primary();

    while (true) {
      if (match(TokenType.LEFT_PAREN)) {
        expr = finishCall(expr);
      } else if (match(TokenType.DOT)) {
        var expect = TokenType.IDENTIFIER;
        var identifier = consume(expect, String.format("Expect property '%s' after '.'.", expect));

        expr = new Expr.InstanceGet(expr, identifier);
      } else {
        break;
      }
    }

    return expr;
  }

  private Expr primary() {
    if (match(TokenType.TRUE)) {
      return new Expr.Literal(true);
    }

    if (match(TokenType.FALSE)) {
      return new Expr.Literal(false);
    }

    if (match(TokenType.NIL)) {
      return new Expr.Literal(null);
    }

    if (match(TokenType.STRING, TokenType.NUMBER)) {
      return new Expr.Literal(previous().literal());
    }

    if (match(TokenType.THIS)) {
      return new Expr.This(previous());
    }

    if (match(TokenType.SUPER)) {
      var keyword = previous();

      var expect = TokenType.DOT;
      consume(expect, String.format("Expect '%s' after super.", expect));

      expect = TokenType.IDENTIFIER;
      var method = consume(expect, String.format("Expect identifier after super"));

      return new Expr.Super(keyword, method);
    }

    if (match(TokenType.IDENTIFIER)) {
      return new Expr.Var(previous());
    }

    if (match(TokenType.LEFT_PAREN)) {
      var expr = expression();

      var expect = TokenType.RIGHT_PAREN;
      consume(expect, String.format("Expect '%s' after expression.", expect));

      return new Expr.Grouping(expr);
    }

    throw error(peek(), "Expect expression.");
  }

  private Expr.Call finishCall(Expr callee) {
    var args = new ArrayList<Expr>();

    if (!check(TokenType.RIGHT_PAREN)) {
      while (true) {
        if (args.size() >= 255) {
          error(peek(), "Cannot have more than 255 arguments.");
        }

        args.add(expression());

        if (!match(TokenType.COMMA)) {
          break;
        }
      }
    }

    var expect = TokenType.RIGHT_PAREN;
    var paren = consume(expect, String.format("Expect '%s' after expression.", expect));

    return new Expr.Call(callee, paren, args);
  }

  private Expr logicalExpr(ExprMatcher matcher, TokenType token) {
    var expr = matcher.match();

    while (match(token)) {
      var operator = previous();
      var right = matcher.match();

      expr = new Expr.Logical(expr, operator, right);
    }

    return expr;
  }

  private Expr binaryExpr(ExprMatcher matcher, TokenType... tokens) {
    var expr = matcher.match();

    while (match(tokens)) {
      var operator = previous();
      var right = matcher.match();

      expr = new Expr.Binary(expr, operator, right);
    }

    return expr;
  }

  private List<Stmt> block() {
    var stmts = new ArrayList<Stmt>();

    while (!check(TokenType.RIGHT_BRACE) && !isAtEnd()) {
      stmts.add(declaration());
    }

    var expect = TokenType.RIGHT_BRACE;
    consume(expect, String.format("Expect '%s' after block.", expect));

    return stmts;
  }

  private boolean match(TokenType... types) {
    for (var type : types) {
      if (check(type)) {
        advance();
        return true;
      }
    }

    return false;
  }

  private Token consume(TokenType type, String message) {
    if (check(type)) {
      return advance();
    }

    throw error(peek(), message);
  }

  private boolean check(TokenType type) {
    return !isAtEnd() && peek().type() == type;
  }

  private boolean isAtEnd() {
    return peek().type() == TokenType.EOF;
  }

  private Token peek() {
    return tokens.get(current);
  }

  private Token advance() {
    if (!isAtEnd()) {
      current++;
    }

    return previous();
  }

  private Token previous() {
    return tokens.get(current - 1);
  }

  // Responsible for advancing past bad input to get to the next block of code,
  // once we detect bad input for a block of code it's likely more errors would
  // follow in the same block so trying to skip past it to the next block means
  // we can provide more helpful error messages
  private void synchronize() {
    advance();

    while (!isAtEnd()) {
      if (previous().type() == TokenType.SEMICOLON) {
        return;
      }

      switch (peek().type()) {
        case TokenType.IF:
        case TokenType.FOR:
        case TokenType.WHILE:
        case TokenType.FUN:
        case TokenType.RETURN:
        case TokenType.VAR:
        case TokenType.CLASS:
          return;
      }

      advance();
    }
  }

  private ParseError error(Token token, String message) {
    Lox.error(token, message);
    return new ParseError();
  }
}
