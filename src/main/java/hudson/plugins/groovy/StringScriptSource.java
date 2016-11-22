package hudson.plugins.groovy;

import groovy.lang.GroovyShell;
import hudson.AbortException;
import hudson.Extension;
import hudson.FilePath;
import hudson.model.BuildListener;
import hudson.model.AbstractBuild;
import hudson.model.Descriptor;
import hudson.util.FormValidation;

import java.io.IOException;
import org.codehaus.groovy.control.CompilationFailedException;
import org.jenkinsci.plugins.scriptsecurity.sandbox.groovy.SecureGroovyScript;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

/**
 * Groovy script specified by command string.
 * 
 * @author dvrzalik
 */
public class StringScriptSource extends ScriptSource {

    private final SecureGroovyScript script;

    @Deprecated
    transient String command;

    @DataBoundConstructor
    public StringScriptSource(SecureGroovyScript script) {
        this.script = script.configuringWithNonKeyItem();
    }

    @Deprecated
    public StringScriptSource(String command) {
        this(new SecureGroovyScript(command, true, null));
    }

    @Override
    public SecureGroovyScript getSecureGroovyScript(FilePath projectWorkspace, AbstractBuild<?, ?> build, BuildListener listener) throws IOException, InterruptedException {
        return script;
    }

    @Override
    public FilePath getScriptFile(
            FilePath projectWorkspace, AbstractBuild<?, ?> build, BuildListener listener
    ) throws IOException, InterruptedException {
        if (!script.getClasspath().isEmpty()) {
            throw new AbortException("Define classpath in the Groovy builder, not here");
        }
        return projectWorkspace.createTextTempFile("hudson", ".groovy", script.getScript(), true);
    }

    public SecureGroovyScript getScript() {
        return script;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        StringScriptSource that = (StringScriptSource) o;

        return script.getScript().equals(that.script.getScript()) &&
               script.isSandbox() == that.script.isSandbox() &&
               script.getClasspath().equals(that.script.getClasspath());

    }

    @Override
    public int hashCode() {
        return script.getClasspath().hashCode();
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<ScriptSource> {

        @Override
        public String getDisplayName() {
            return "Groovy command";
        }

        public FormValidation doCheckScript(@QueryParameter String command) {
            if (command == null || command.trim().isEmpty())
                return FormValidation.error("Script seems to be empty string!");

            try {
                new GroovyShell().parse(command);
                return FormValidation.ok("So far so good");
            } catch (CompilationFailedException e) {
                return FormValidation.error(e.getMessage());
            }
        }
    }
}
