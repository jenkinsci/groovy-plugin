package hudson.plugins.groovy;

import hudson.Extension;
import hudson.FilePath;
import hudson.model.AbstractBuild;
import hudson.model.Descriptor;
import hudson.model.TaskListener;
import java.io.IOException;
import org.jenkinsci.plugins.scriptsecurity.sandbox.groovy.SecureGroovyScript;
import org.kohsuke.stapler.DataBoundConstructor;

public class StringSystemScriptSource extends SystemScriptSource {

    private final SecureGroovyScript script;

    @DataBoundConstructor
    public StringSystemScriptSource(SecureGroovyScript script) {
        this.script = script.configuringWithNonKeyItem();
    }

    public SecureGroovyScript getScript() {
        return script;
    }

    @Override
    public SecureGroovyScript getSecureGroovyScript(FilePath projectWorkspace, AbstractBuild<?, ?> build, TaskListener listener) throws IOException, InterruptedException {
        return script;
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<SystemScriptSource> {

        @Override
        public String getDisplayName() {
            return "Groovy command";
        }

    }

}
