import java.util.Map;
import java.util.HashMap;

class LoxInstance {
  private LoxClass klass;
  private final Map<String, Object> fields = new HashMap<>();

  LoxInstance(LoxClass klass) {
    this.klass = klass;
  }

  @Override
  public String toString() {
    return String.format("<instance - %s>", klass.identifier());
  }

  Object get(Token identifier) {
    var key = identifier.lexeme();
    if (fields.containsKey(key)) {
      return fields.get(key);
    }

    throw new RuntimeError(identifier, String.format("Undefined instance field '%s'.", key));
  }
}
