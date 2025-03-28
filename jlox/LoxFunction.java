import java.util.List;

record LoxFunction(Stmt.Function declaration, Environment environment) implements LoxCallable {
  public String toString() {
    return String.format("<function - %s>", declaration().identifier().lexeme());
  }

  public int arity() {
    return declaration().params().size();
  }

  public Object call(Interpreter interpreter, List<Object> arguments) {
    var environment = new Environment(environment());

    var params = declaration().params();
    for (var i = 0; i < params.size(); i++) {
      environment.define(params.get(i).lexeme(), arguments.get(i));
    }

    try {
      interpreter.executeBlock(declaration().stmts(), environment);
    } catch (Return ret) {
      return ret.value;
    }

    return null;
  }
}
