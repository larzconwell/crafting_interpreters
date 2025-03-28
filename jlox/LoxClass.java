import java.util.List;

record LoxClass(String name) implements LoxCallable {
  public String toString() {
    return String.format("<class - %s>", name());
  }

  public int arity() {
    return 0;
  }

  public Object call(Interpreter interpreter, List<Object> arguments) {
    return new LoxInstance(this);
  }
}
