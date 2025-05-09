import java.time.Instant;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

class Interpreter {
  final Environment global = new Environment();
  private Environment environment = global;
  private final Map<Expr, Integer> locals = new HashMap<>();

  Interpreter() {
    global.define("time", new LoxCallable() {
      public String toString() { return "<interpreter function - time>"; }
      public int arity() { return 0; }

      public Object call(Interpreter interpreter, List<Object> arguments) {
        return Instant.now().getEpochSecond();
      }
    });

    global.define("print", new LoxCallable() {
      public String toString() { return "<interpreter function - print>"; }
      public int arity() { return 1; }

      public Object call(Interpreter interpreter, List<Object> arguments) {
        var value = arguments.getFirst();

        var str = "nil";
        if (value != null) {
          str = value.toString();
        }

        if (value instanceof Double && str.endsWith(".0")) {
          str = str.substring(0, str.length() - 2);
        }

        System.out.println(str);
        return null;
      }
    });
  }

  void interpret(List<Stmt> stmts) {
    try {
      for (var stmt : stmts) {
        execute(stmt);
      }
    } catch (RuntimeError error) {
      Lox.runtimeError(error);
    }
  }

  void executeBlock(List<Stmt> stmts, Environment environment) {
    var enclosingEnvironment = this.environment;

    try {
      this.environment = environment;

      for (var stmt : stmts) {
        execute(stmt);
      }
    } finally {
      this.environment = enclosingEnvironment;
    }
  }

  void resolve(Expr expr, int depth) {
    locals.put(expr, depth);
  }

  private void execute(Stmt stmt) {
    switch (stmt) {
      case Stmt.ExprStmt expr -> execExprStmt(expr);
      case Stmt.Var var -> execVar(var);
      case Stmt.Block block -> execBlock(block);
      case Stmt.If ifStmt -> execIf(ifStmt);
      case Stmt.While whileStmt  -> execWhile(whileStmt);
      case Stmt.For forStmt -> execFor(forStmt);
      case Stmt.Function func -> execFunction(func);
      case Stmt.Return returnStmt -> execReturn(returnStmt);
      case Stmt.Break breakStmt -> execBreak(breakStmt);
      case Stmt.Continue continueStmt -> execContinue(continueStmt);
      case Stmt.Class classStmt -> execClass(classStmt);
      default -> {}
    };
  }

  private Object evaluate(Expr expr) {
    return switch (expr) {
      case Expr.Literal literal -> evalLiteral(literal);
      case Expr.Grouping grouping -> evalGrouping(grouping);
      case Expr.Logical logical -> evalLogical(logical);
      case Expr.Binary binary -> evalBinary(binary);
      case Expr.Unary unary -> evalUnary(unary);
      case Expr.Var var -> evalVar(var);
      case Expr.Assign assign -> evalAssign(assign);
      case Expr.Call call -> evalCall(call);
      case Expr.InstanceGet get -> evalInstanceGet(get);
      case Expr.InstanceSet set -> evalInstanceSet(set);
      case Expr.This thisExpr -> evalThis(thisExpr);
      case Expr.Super superExpr -> evalSuper(superExpr);
      default -> null;
    };
  }

  private void execExprStmt(Stmt.ExprStmt stmt) {
    evaluate(stmt.expr());
  }

  private void execVar(Stmt.Var stmt) {
    Object value = null;
    if (stmt.value() != null) {
      value = evaluate(stmt.value());
    }

    environment.define(stmt.identifier().lexeme(), value);
  }

  private void execBlock(Stmt.Block stmt) {
    executeBlock(stmt.stmts(), new Environment(environment));
  }

  private void execIf(Stmt.If stmt) {
    if (isTruthy(evaluate(stmt.condition()))) {
      execute(stmt.ifStmt());
    } else if (stmt.elseStmt() != null) {
      execute(stmt.elseStmt());
    }
  }

  private void execWhile(Stmt.While stmt) {
    while (isTruthy(evaluate(stmt.condition()))) {
      try {
        execute(stmt.stmt());
      } catch (Break _) {
        break;
      } catch (Continue _) {
        continue;
      }
    }
  }

  private void execFor(Stmt.For stmt) {
    if (stmt.initializer() != null) {
      execute(stmt.initializer());
    }

    while (isTruthy(evaluate(stmt.condition()))) {
      try {
        execute(stmt.stmt());

        if (stmt.increment() != null) {
          evaluate(stmt.increment());
        }
      } catch (Break _) {
        break;
      } catch (Continue _) {
        if (stmt.increment() != null) {
          evaluate(stmt.increment());
        }

        continue;
      }
    }
  }

  private void execFunction(Stmt.Function stmt) {
    environment.define(stmt.identifier().lexeme(), new LoxFunction(stmt, environment, false));
  }

  private void execReturn(Stmt.Return stmt) {
    Object value = null;
    if (stmt.expr() != null) {
      value = evaluate(stmt.expr());
    }

    throw new Return(value);
  }

  private void execBreak(Stmt.Break stmt) {
    throw new Break();
  }

  private void execContinue(Stmt.Continue stmt) {
    throw new Continue();
  }

  private void execClass(Stmt.Class stmt) {
    LoxClass superclass = null;
    if (stmt.superclass() != null) {
      var expr = evaluate(stmt.superclass());
      if (!(expr instanceof LoxClass)) {
        throw new RuntimeError(stmt.superclass().identifier(), "Superclass must be a class.");
      }

      superclass = (LoxClass)expr;
    }

    // Defining it first will allow referencing the class within the classes methods.
    environment.define(stmt.identifier().lexeme(), null);

    if (superclass != null) {
      environment = new Environment(environment);
      environment.define("super", superclass);
    }

    var methods = new HashMap<String, LoxFunction>();
    for (var method : stmt.methods()) {
      var identifier = method.identifier().lexeme();
      var function = new LoxFunction(method, environment, identifier.equals("init"));

      methods.put(identifier, function);
    }

    var klass = new LoxClass(stmt.identifier().lexeme(), superclass, methods);

    if (superclass != null) {
      environment = environment.enclosing;
    }

    environment.assign(stmt.identifier(), klass);
  }

  private Object evalLiteral(Expr.Literal expr) {
    return expr.value();
  }

  private Object evalGrouping(Expr.Grouping expr) {
    return evaluate(expr.expr());
  }

  private Object evalLogical(Expr.Logical expr) {
    var left = evaluate(expr.left());

    if (expr.operator().type() == TokenType.OR) {
      if (isTruthy(left)) {
        return left;
      }
    } else {
      if (!isTruthy(left)) {
        return left;
      }
    }

    return evaluate(expr.right());
  }

  private Object evalBinary(Expr.Binary expr) {
    var left = evaluate(expr.left());
    var right = evaluate(expr.right());

    return switch (expr.operator().type()) {
      case TokenType.PLUS -> {
        if (left instanceof Double) {
          checkNumberOperand(expr.operator(), right);
          yield (double)left + (double)right;
        } else {
          checkStringOperands(expr.operator(), left, right);
          yield (String)left + (String)right;
        }
      }
      case TokenType.MINUS -> {
        checkNumberOperands(expr.operator(), left, right);
        yield (double)left - (double)right;
      }
      case TokenType.STAR -> {
        checkNumberOperands(expr.operator(), left, right);
        yield (double)left * (double)right;
      }
      case TokenType.SLASH -> {
        checkNumberOperands(expr.operator(), left, right);
        checkNotDivideByZero(expr.operator(), right);
        yield (double)left / (double)right;
      }
      case TokenType.PERCENT -> {
        checkNumberOperands(expr.operator(), left, right);
        checkNotDivideByZero(expr.operator(), right);
        yield (double)left % (double)right;
      }
      case TokenType.GREATER -> {
        checkNumberOperands(expr.operator(), left, right);
        yield (double)left > (double)right;
      }
      case TokenType.LESS -> {
        checkNumberOperands(expr.operator(), left, right);
        yield (double)left < (double)right;
      }
      case TokenType.GREATER_EQUAL -> {
        checkNumberOperands(expr.operator(), left, right);
        yield (double)left >= (double)right;
      }
      case TokenType.LESS_EQUAL -> {
        checkNumberOperands(expr.operator(), left, right);
        yield (double)left <= (double)right;
      }
      case TokenType.EQUAL_EQUAL -> isEqual(left, right);
      case TokenType.BANG_EQUAL -> !isEqual(left, right);
      default -> null;
    };
  }

  private Object evalUnary(Expr.Unary expr) {
    var value = evaluate(expr.expr());

    return switch (expr.operator().type()) {
      case TokenType.MINUS -> {
        checkNumberOperand(expr.operator(), value);
        yield -(double)value;
      }
      case TokenType.BANG -> !isTruthy(value);
      default -> null;
    };
  }

  private Object evalVar(Expr.Var expr) {
    return lookupVar(expr.identifier(), expr);
  }

  private Object evalAssign(Expr.Assign expr) {
    var value = evaluate(expr.value());

    var depth = locals.get(expr);
    if (depth == null) {
      global.assign(expr.identifier(), value);
    } else {
      environment.assignAt(depth, expr.identifier(), value);
    }

    return value;
  }

  private Object evalCall(Expr.Call expr) {
    var callee = evaluate(expr.callee());
    if (!(callee instanceof LoxCallable)) {
      throw new RuntimeError(expr.paren(), "Calls can only be made on functions and classes.");
    }

    var func = (LoxCallable)callee;
    var args = new ArrayList<Object>(expr.args().size());
    if (expr.args().size() != func.arity()) {
      throw new RuntimeError(expr.paren(), String.format("Expected %d arguments but got %d", func.arity(), expr.args().size()));
    }

    for (var arg : expr.args()) {
      args.add(evaluate(arg));
    }

    return func.call(this, args);
  }

  private Object evalInstanceGet(Expr.InstanceGet expr) {
    var instance = evaluate(expr.instance());
    if (!(instance instanceof LoxInstance)) {
      throw new RuntimeError(expr.identifier(), "Only instances of classes have properties.");
    }

    return ((LoxInstance)instance).get(expr.identifier());
  }

  private Object evalInstanceSet(Expr.InstanceSet expr) {
    var instance = evaluate(expr.instance());
    if (!(instance instanceof LoxInstance)) {
      throw new RuntimeError(expr.identifier(), "Only instances of classes have properties.");
    }

    var value = evaluate(expr.value());
    ((LoxInstance)instance).set(expr.identifier(), value);
    return value;
  }

  private Object evalThis(Expr.This expr) {
    return lookupVar(expr.keyword(), expr);
  }

  private Object evalSuper(Expr.Super expr) {
    var depth = locals.get(expr);
    var superclass = (LoxClass)environment.getAt(depth, "super");
    var instance = (LoxInstance)environment.getAt(depth - 1, "this");

    var identifier = expr.method().lexeme();
    var method = superclass.getMethod(identifier);
    if (method == null) {
      throw new RuntimeError(expr.method(), String.format("Undefined instance property '%s'.", identifier));
    }

    return method.bind(instance);
  }

  private Object lookupVar(Token identifier, Expr expr) {
    var depth = locals.get(expr);
    if (depth == null) {
      return global.get(identifier);
    }

    return environment.getAt(depth, identifier.lexeme());
  }

  private boolean isEqual(Object left, Object right) {
    if (left == null && right == null) {
      return true;
    }

    if (left == null) {
      return false;
    }

    return left.equals(right);
  }

  private boolean isTruthy(Object value) {
    if (value == null) {
      return false;
    }

    if (value instanceof Boolean) {
      return (boolean)value;
    }

    return true;
  }

  private void checkNumberOperand(Token operator, Object operand) {
    if (operand instanceof Double) {
      return;
    }

    throw new RuntimeError(operator, "Operand must be a number.");
  }

  private void checkNumberOperands(Token operator, Object... operands) {
    for (var operand : operands) {
      if (!(operand instanceof Double)) {
        throw new RuntimeError(operator, "Operands must be numbers.");
      }
    }
  }

  private void checkStringOperands(Token operator, Object... operands) {
    for (var operand : operands) {
      if (!(operand instanceof String)) {
        throw new RuntimeError(operator, "Operands must be strings.");
      }
    }
  }

  private void checkNotDivideByZero(Token operator, Object operand) {
    if (operand instanceof Double && (double)operand == 0) {
      throw new RuntimeError(operator, "Cannot be divide by zero.");
    }
  }
}
