import java.util.List;

interface Stmt {
  record ExprStmt(Expr expr) implements Stmt {}
  record Print(Expr expr) implements Stmt {}
  record Var(Token name, Expr expr) implements Stmt {}
  record Block(List<Stmt> stmts) implements Stmt {}
  record If(Expr condition, Stmt ifStmt, Stmt elseStmt) implements Stmt {}
  record While(Expr condition, Stmt stmt) implements Stmt {}
  record Function(Token name, List<Token> params, List<Stmt> stmts) implements Stmt {}
  record Return(Token keyword, Expr expr) implements Stmt {}
}
