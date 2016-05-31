package hudson.plugins.groovy;

import groovy.lang.GroovyShell;
import hudson.Extension;
import hudson.FilePath;
import hudson.model.BuildListener;
import hudson.model.AbstractBuild;
import hudson.model.Descriptor;
import hudson.util.FormValidation;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.Charsets;
import org.codehaus.groovy.control.CompilationFailedException;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

/**
 * Groovy script specified by command string.
 * 
 * @author dvrzalik
 */
public class StringScriptSource extends ScriptSource {

    private String command;

    @DataBoundConstructor
    public StringScriptSource(String command) {
        this.command = command;
    }

    @Override
    public InputStream getScriptStream(FilePath projectWorkspace, AbstractBuild<?, ?> build, BuildListener listener) {
        return new ByteArrayInputStream(command.getBytes(Charsets.UTF_8));
    }

    @Override
    public FilePath getScriptFile(
            FilePath projectWorkspace, AbstractBuild<?, ?> build, BuildListener listener
    ) throws IOException, InterruptedException {
        return projectWorkspace.createTextTempFile("hudson", ".groovy", command, true);
    }

    public String getCommand() {
        return command;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        StringScriptSource that = (StringScriptSource) o;

        return command != null ? command.equals(that.command) : that.command == null;

    }

    @Override
    public int hashCode() {
        return command != null ? command.hashCode() : 0;
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
