package hudson.plugins.groovy.exceptions;

/**
 * Thrown to indicate that there are some problems while working with script runner.
 *
 * @author sshelomentsev
 */
public class GroovyScriptExecutionException extends Exception {

    public GroovyScriptExecutionException(String message) {
        super(message);
    }

    public GroovyScriptExecutionException(Throwable cause) {
        super(cause);
    }

    public GroovyScriptExecutionException(String message, Throwable cause) {
        super(message, cause);
    }
}
