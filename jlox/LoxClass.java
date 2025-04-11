import java.util.List;
import java.util.Map;

record LoxClass(String identifier, LoxClass superclass, LoxFunction initializer, Map<String, LoxFunction> methods) implements LoxCallable {
  public String toString() {
    return String.format("<class - %s>", identifier());
  }

  public int arity() {
    if (initializer() != null) {
      return initializer().arity();
    }

    return 0;
  }

  public Object call(Interpreter interpreter, List<Object> arguments) {
    var instance = new LoxInstance(this);

    if (initializer() != null) {
      initializer().bind(instance).callAndReturn(interpreter, arguments, instance);
    }

    return instance;
  }

  LoxFunction getMethod(String identifier) {
    if (methods().containsKey(identifier)) {
      return methods().get(identifier);
    }

    if (superclass() != null) {
      return superclass().getMethod(identifier);
    }

    return null;
  }
}
