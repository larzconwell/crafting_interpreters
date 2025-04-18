import java.util.List;

interface Expr {
  record Literal(Object value) implements Expr {}
  record Grouping(Expr expr) implements Expr {}
  record Logical(Expr left, Token operator, Expr right) implements Expr {}
  record Binary(Expr left, Token operator, Expr right) implements Expr {}
  record Unary(Token operator, Expr expr) implements Expr {}
  record Var(Token identifier) implements Expr {}
  record Assign(Token identifier, Expr value) implements Expr {}
  record Call(Expr callee, Token paren, List<Expr> args) implements Expr {}
  record InstanceGet(Expr instance, Token identifier) implements Expr {}
  record InstanceSet(Expr instance, Token identifier, Expr value) implements Expr {}
  record This(Token keyword) implements Expr {}
  record Super(Token keyword, Token method) implements Expr {}
}
