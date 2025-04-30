import java.util.List;

interface Stmt {
  record ExprStmt(Expr expr) implements Stmt {}
  record Var(Token identifier, Expr value) implements Stmt {}
  record Block(List<Stmt> stmts) implements Stmt {}
  record If(Expr condition, Stmt ifStmt, Stmt elseStmt) implements Stmt {}
  record While(Expr condition, Stmt stmt) implements Stmt {}
  record Function(Token identifier, List<Token> params, List<Stmt> stmts) implements Stmt {}
  record Return(Token keyword, Expr expr) implements Stmt {}
  record Break(Token keyword) implements Stmt {}
  record Continue(Token keyword) implements Stmt {}
  record Class(Token identifier, Expr.Var superclass, List<Stmt.Function> methods) implements Stmt {}
}
