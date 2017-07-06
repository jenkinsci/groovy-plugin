package hudson.plugins.groovy;

import hudson.FilePath;
import hudson.model.AbstractBuild;
import hudson.model.AbstractDescribableImpl;
import hudson.model.TaskListener;
import java.io.IOException;
import org.jenkinsci.plugins.scriptsecurity.sandbox.groovy.SecureGroovyScript;

/**
 * Source of {@link SystemGroovy} scripts.
 */
public abstract class SystemScriptSource extends AbstractDescribableImpl<SystemScriptSource> {

    /**
     * Provides a script.
     */
    public abstract SecureGroovyScript getSecureGroovyScript(FilePath projectWorkspace, AbstractBuild<?, ?> build, TaskListener listener) throws IOException, InterruptedException;

}
