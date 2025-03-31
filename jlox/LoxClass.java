import java.util.List;
import java.util.Map;

record LoxClass(String identifier, Map<String, LoxFunction> methods) implements LoxCallable {
  public String toString() {
    return String.format("<class - %s>", identifier());
  }

  public int arity() {
    return 0;
  }

  public Object call(Interpreter interpreter, List<Object> arguments) {
    return new LoxInstance(this);
  }

  LoxFunction getMethod(String identifier) {
    if (methods().containsKey(identifier)) {
      return methods().get(identifier);
    }

    return null;
  }
}
