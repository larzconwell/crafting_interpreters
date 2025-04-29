import java.util.List;

interface Expr {
  record Literal(long id, Object value) implements Expr {
    Literal(Object value) {
      this(System.nanoTime(), value);
    }
  }

  record Grouping(long id, Expr expr) implements Expr {
    Grouping(Expr expr) {
      this(System.nanoTime(), expr);
    }
  }

  record Logical(long id, Expr left, Token operator, Expr right) implements Expr {
    Logical(Expr left, Token operator, Expr right) {
      this(System.nanoTime(), left, operator, right);
    }
  }

  record Binary(long id, Expr left, Token operator, Expr right) implements Expr {
    Binary(Expr left, Token operator, Expr right) {
      this(System.nanoTime(), left, operator, right);
    }
  }

  record Unary(long id, Token operator, Expr expr) implements Expr {
    Unary(Token operator, Expr expr) {
      this(System.nanoTime(), operator, expr);
    }
  }

  record Var(long id, Token identifier) implements Expr {
    Var(Token identifier) {
      this(System.nanoTime(), identifier);
    }
  }

  record Assign(long id, Token identifier, Expr value) implements Expr {
    Assign(Token identifier, Expr value) {
      this(System.nanoTime(), identifier, value);
    }
  }

  record Call(long id, Expr callee, Token paren, List<Expr> args) implements Expr {
    Call(Expr callee, Token paren, List<Expr> args) {
      this(System.nanoTime(), callee, paren, args);
    }
  }

  record InstanceGet(long id, Expr instance, Token identifier) implements Expr {
    InstanceGet(Expr instance, Token identifier) {
      this(System.nanoTime(), instance, identifier);
    }
  }

  record InstanceSet(long id, Expr instance, Token identifier, Expr value) implements Expr {
    InstanceSet(Expr instance, Token identifier, Expr value) {
      this(System.nanoTime(), instance, identifier, value);
    }
  }

  record This(long id, Token keyword) implements Expr {
    This(Token keyword) {
      this(System.nanoTime(), keyword);
    }
  }

  record Super(long id, Token keyword, Token method) implements Expr {
    Super(Token keyword, Token method) {
      this(System.nanoTime(), keyword, method);
    }
  }
}
