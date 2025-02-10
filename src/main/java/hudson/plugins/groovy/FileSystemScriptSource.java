package hudson.plugins.groovy;

import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.model.AbstractBuild;
import hudson.model.Descriptor;
import hudson.model.TaskListener;
import java.io.IOException;
import org.jenkinsci.plugins.scriptsecurity.sandbox.groovy.SecureGroovyScript;
import org.jenkinsci.plugins.scriptsecurity.scripts.ApprovalContext;
import org.kohsuke.stapler.DataBoundConstructor;

public class FileSystemScriptSource extends SystemScriptSource {

    private final String scriptFile;

    @DataBoundConstructor
    public FileSystemScriptSource(String scriptFile) {
        this.scriptFile = scriptFile;
    }

    public String getScriptFile() {
        return scriptFile;
    }

    @Override
    public SecureGroovyScript getSecureGroovyScript(FilePath projectWorkspace, AbstractBuild<?, ?> build, TaskListener listener) throws IOException, InterruptedException {
        EnvVars env = build.getEnvironment(listener);
        String expandedScriptFile = env.expand(this.scriptFile);
        String text = new FilePath(projectWorkspace, expandedScriptFile).readToString();
        try {
            return new SecureGroovyScript(text, true, null).configuring(ApprovalContext.create()/* unused but just in case: */.withItem(build.getParent()));
        } catch (Descriptor.FormException e) {
            throw new RuntimeException(e);
        }
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<SystemScriptSource> {

        @Override
        public String getDisplayName() {
            return "Groovy script file";
        }

    }

}
