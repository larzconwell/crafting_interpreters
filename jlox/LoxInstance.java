import java.util.Map;
import java.util.HashMap;

class LoxInstance {
  private LoxClass klass;
  private final Map<String, Object> properties = new HashMap<>();

  LoxInstance(LoxClass klass) {
    this.klass = klass;
  }

  @Override
  public String toString() {
    return String.format("<instance - %s>", klass.identifier());
  }

  Object get(Token identifier) {
    var key = identifier.lexeme();
    if (properties.containsKey(key)) {
      return properties.get(key);
    }

    var method = klass.getMethod(key);
    if (method != null) {
      return method.bind(this);
    }

    throw new RuntimeError(identifier, String.format("Undefined instance property '%s'.", key));
  }

  void set(Token identifier, Object value) {
    properties.put(identifier.lexeme(), value);
  }
}
