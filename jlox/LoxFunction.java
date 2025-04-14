import java.util.List;

record LoxFunction(Stmt.Function declaration, Environment environment, boolean isInitializer) implements LoxCallable {
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
      if (isInitializer()) {
        return environment().getAt(0, "this");
      }

      return ret.value;
    }

    if (isInitializer()) {
      return environment().getAt(0, "this");
    }

    return null;
  }

  LoxFunction bind(LoxInstance instance) {
    var environment = new Environment(environment());
    environment.define("this", instance);

    return new LoxFunction(declaration(), environment, isInitializer());
  }
}
