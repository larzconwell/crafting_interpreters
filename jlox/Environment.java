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

  void define(String name, Object value) {
    values.put(name, value);
  }

  void assign(Token name, Object value) {
    var key = name.lexeme();
    if (values.containsKey(key)) {
      values.put(key, value);
      return;
    }

    if (enclosing != null) {
      enclosing.assign(name, value);
      return;
    }

    throw new RuntimeError(name, String.format("Undefined variable '%s'", key));
  }

  void assignAt(int depth, Token name, Object value) {
    ancestor(depth).values.put(name.lexeme(), value);
  }

  Object get(Token name) {
    var key = name.lexeme();
    if (values.containsKey(key)) {
      return values.get(key);
    }

    if (enclosing != null) {
      return enclosing.get(name);
    }

    throw new RuntimeError(name, String.format("Undefined variable '%s'", key));
  }

  Object getAt(int depth, String name) {
    return ancestor(depth).values.get(name);
  }

  Environment ancestor(int depth) {
    var env = this;

    for (var i = 0; i < depth; i++) {
      env = env.enclosing;
    }

    return env;
  }
}
