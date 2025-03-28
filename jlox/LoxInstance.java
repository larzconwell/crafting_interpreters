record LoxInstance(LoxClass klass) {
  public String toString() {
    return String.format("<instance - %s>", klass().identifier());
  }
}
