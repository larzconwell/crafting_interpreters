import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;

class Lox {
  private static final Interpreter interpreter = new Interpreter();
  private static boolean hadError = false;
  private static boolean hadRuntimeError = false;

  public static void main(String[] args) throws IOException {
    if (args.length > 1) {
      System.out.println("Usage: jlox <path>");
      System.exit(64);
    } else if (args.length == 1) {
      runFile(args[0]);
    } else {
      runPrompt();
    }
  }

  static void error(int line, String message) {
    report(line, "", message);
  }

  static void error(Token token, String message) {
    if (token.type() == TokenType.EOF) {
      report(token.line(), " at end", message);
    } else {
      report(token.line(), String.format(" at '%s'", token.lexeme()), message);
    }
  }

  static void runtimeError(RuntimeError error) {
    hadRuntimeError = true;

    System.err.println(String.format("%s\n[line %d]", error.getMessage(), error.token.line()));
  }

  private static void runFile(String path) throws IOException {
    var bytes = Files.readAllBytes(Paths.get(path));
    run(new String(bytes, Charset.defaultCharset()));

    if (hadError) {
      System.exit(65);
    }
    if (hadRuntimeError) {
      System.exit(70);
    }
  }

  private static void runPrompt() throws IOException {
    var input = new InputStreamReader(System.in);
    var reader = new BufferedReader(input);

    for (;;) {
      System.out.print("> ");

      var line = reader.readLine();
      if (line == null || line.equals("exit")) {
        break;
      }

      run(line);
      hadError = false;
      hadRuntimeError = false;
    }
  }

  private static void run(String source) {
    var scanner = new Scanner(source);
    var tokens = scanner.scanTokens();
    if (hadError) {
      return;
    }

    var parser = new Parser(tokens);
    var stmts = parser.parse();
    if (hadError) {
      return;
    }

    var resolver = new Resolver(interpreter);
    resolver.resolve(stmts);
    if (hadError) {
      return;
    }

    interpreter.interpret(stmts);
  }

  private static void report(int line, String where, String message) {
    hadError = true;

    System.err.println(String.format("[line %d] Error%s: %s", line, where, message));
  }
}
