import java.util.Map;
import java.util.HashMap;

class Environment {
  final Environment enclosing;
  private final Map<String, Object> values = new HashMap<>();

  Environment() {
    this.enclosing = null;
  }

  Environment(Environment enclosing) {
    this.enclosing = enclosing;
  }

  void define(String identifier, Object value) {
    values.put(identifier, value);
  }

  void assign(Token identifier, Object value) {
    var key = identifier.lexeme();
    if (values.containsKey(key)) {
      values.put(key, value);
      return;
    }

    if (enclosing != null) {
      enclosing.assign(identifier, value);
      return;
    }

    throw new RuntimeError(identifier, String.format("Undefined variable '%s'", key));
  }

  void assignAt(int depth, Token identifier, Object value) {
    ancestor(depth).values.put(identifier.lexeme(), value);
  }

  Object get(Token identifier) {
    var key = identifier.lexeme();
    if (values.containsKey(key)) {
      return values.get(key);
    }

    if (enclosing != null) {
      return enclosing.get(identifier);
    }

    throw new RuntimeError(identifier, String.format("Undefined variable '%s'", key));
  }

  Object getAt(int depth, String identifier) {
    return ancestor(depth).values.get(identifier);
  }

  Environment ancestor(int depth) {
    var env = this;

    for (var i = 0; i < depth; i++) {
      env = env.enclosing;
    }

    return env;
  }
}
