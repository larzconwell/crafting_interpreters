import java.util.List;
import java.util.Stack;
import java.util.Map;
import java.util.HashMap;

enum FunctionType {
  NONE,
  FUNCTION
}

// This is used to restrict variable references to variables that where
// previously declared in the current scope or parent scopes, previously
// meaning a variable reference on line 50 should only be able to access
// variables declared on lines 1-49 in blocks that are still in scope.
class Resolver {
  private final Interpreter interpreter;
  private final Stack<Map<String, Boolean>> scopes = new Stack<>();
  private FunctionType currentFunction = FunctionType.NONE;

  Resolver(Interpreter interpreter) {
    this.interpreter = interpreter;
  }

  void resolve(List<Stmt> stmts) {
    for (var stmt : stmts) {
      resolve(stmt);
    }
  }

  private void resolve(Stmt stmt) {
    switch (stmt) {
      case Stmt.ExprStmt expr -> resolve(expr.expr());
      case Stmt.Print print -> resolve(print.expr());
      case Stmt.Var var -> resolveVar(var);
      case Stmt.Block block -> resolveBlock(block);
      case Stmt.If ifStmt -> resolveIf(ifStmt);
      case Stmt.While whileStmt -> resolveWhile(whileStmt);
      case Stmt.Function func -> resolveFunction(func);
      case Stmt.Return returnStmt -> resolveReturn(returnStmt);
      case Stmt.Class classStmt -> resolveClass(classStmt);
      default -> {}
    };
  }

  private void resolve(Expr expr) {
    switch (expr) {
      case Expr.Grouping grouping -> resolve(grouping.expr());
      case Expr.Logical logical -> resolveLogical(logical);
      case Expr.Binary binary -> resolveBinary(binary);
      case Expr.Unary unary -> resolve(unary.expr());
      case Expr.Var var -> resolveVar(var);
      case Expr.Assign assign -> resolveAssign(assign);
      case Expr.Call call -> resolveCall(call);
      case Expr.InstanceGet get -> resolve(get.instance());
      case Expr.InstanceSet set -> resolveInstanceSet(set);
      default -> {}
    }
  }

  private void resolveVar(Stmt.Var stmt) {
    declare(stmt.identifier());

    if (stmt.value() != null) {
      resolve(stmt.value());
    }

    define(stmt.identifier());
  }

  private void resolveBlock(Stmt.Block stmt) {
    beginScope();
    resolve(stmt.stmts());
    endScope();
  }

  private void resolveIf(Stmt.If stmt) {
    resolve(stmt.condition());
    resolve(stmt.ifStmt());

    if (stmt.elseStmt() != null) {
      resolve(stmt.elseStmt());
    }
  }

  private void resolveWhile(Stmt.While stmt) {
    resolve(stmt.condition());
    resolve(stmt.stmt());
  }

  private void resolveFunction(Stmt.Function stmt) {
    // Define immediately after declaring to allow recursive references to the function
    declare(stmt.identifier());
    define(stmt.identifier());

    resolveFunctionLiteral(stmt, FunctionType.FUNCTION);
  }

  private void resolveReturn(Stmt.Return stmt) {
    if (currentFunction == FunctionType.NONE) {
      Lox.error(stmt.keyword(), "Can't return outside of a function.");
    }

    if (stmt.expr() != null) {
      resolve(stmt.expr());
    }
  }

  private void resolveClass(Stmt.Class stmt) {
    // Define immediately after declaring to allow recursive references to the class
    declare(stmt.identifier());
    define(stmt.identifier());
  }

  private void resolveLogical(Expr.Logical expr) {
    resolve(expr.left());
    resolve(expr.right());
  }

  private void resolveBinary(Expr.Binary expr) {
    resolve(expr.left());
    resolve(expr.right());
  }

  private void resolveVar(Expr.Var expr) {
    if (!scopes.isEmpty() && scopes.peek().get(expr.identifier().lexeme()) == Boolean.FALSE) {
      Lox.error(expr.identifier(), "Cannot refer to variable in its own initializer");
    }

    resolveLocal(expr, expr.identifier());
  }

  private void resolveAssign(Expr.Assign expr) {
    resolve(expr.value());
    resolveLocal(expr, expr.identifier());
  }

  private void resolveCall(Expr.Call expr) {
    resolve(expr.callee());

    for (var arg : expr.args()) {
      resolve(arg);
    }
  }

  private void resolveInstanceSet(Expr.InstanceSet expr) {
    resolve(expr.value());
    resolve(expr.instance());
  }

  private void resolveFunctionLiteral(Stmt.Function stmt, FunctionType type) {
    var enclosingFunction = currentFunction;
    currentFunction = type;
    beginScope();

    for (var param : stmt.params()) {
      declare(param);
      define(param);
    }

    resolve(stmt.stmts());

    endScope();
    currentFunction = enclosingFunction;
  }

  private void resolveLocal(Expr expr, Token identifier) {
    // Inner most scope to outer most
    for (var i = scopes.size() - 1; i >= 0; i--) {
      if (scopes.get(i).containsKey(identifier.lexeme())) {
        // How many scopes away from the expression we found the declaration
        // e.g. in the same scope then 0, in the parent scope then 1
        interpreter.resolve(expr, scopes.size() - 1 - i);
        break;
      }
    }
  }

  private void beginScope() {
    scopes.push(new HashMap<String, Boolean>());
  }

  private void endScope() {
    scopes.pop();
  }

  private void declare(Token identifier) {
    if (scopes.isEmpty()) {
      return;
    }

    var scope = scopes.peek();
    if (scope.containsKey(identifier.lexeme())) {
      Lox.error(identifier, "A variable with this name already exists in this scope.");
    }

    scope.put(identifier.lexeme(), false);
  }

  private void define(Token identifier) {
    if (scopes.isEmpty()) {
      return;
    }

    scopes.peek().put(identifier.lexeme(), true);
  }
}
