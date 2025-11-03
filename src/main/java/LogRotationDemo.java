import java.io.IOException;
import java.nio.file.Path;

public class LogRotationDemo {
    public static void main(String[] args) throws IOException {
        LogRotationChecker checker = new LogRotationChecker();

        Evaluation e1 = checker.evaluate(checker.loadProperties(Path.of("logging.properties.txt")));
        System.out.println("Case 1: " + e1);

        Evaluation e2 = checker.evaluate(checker.loadProperties(Path.of("logging.pass.properties.txt")));
        System.out.println("Case 2: " + e2);

        Evaluation e3 = checker.evaluate(checker.loadProperties(Path.of("logging.fail.properties.txt")));
        System.out.println("Case 3: " + e3);
    }
}