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

import jenkins.model.Jenkins;

import org.codehaus.groovy.control.CompilationFailedException;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

/**
 * Groovy script specified by command string.
 * 
 * @author dvrzalik
 */
public class StringScriptSource implements ScriptSource {

    private String command;

    @DataBoundConstructor
    public StringScriptSource(String command) {
        this.command = command;
    }

    @Override
    public InputStream getScriptStream(FilePath projectWorkspace) {
        return new ByteArrayInputStream(command.getBytes());
    }

    @Override
    public InputStream getScriptStream(FilePath projectWorkspace, AbstractBuild<?, ?> build, BuildListener listener) {
        return getScriptStream(projectWorkspace);
    }

    @Override
    public FilePath getScriptFile(FilePath projectWorkspace) throws IOException, InterruptedException {
        return projectWorkspace.createTextTempFile("hudson", ".groovy", command, true);
    }

    @Override
    public FilePath getScriptFile(FilePath projectWorkspace, AbstractBuild<?, ?> build, BuildListener listener)
            throws IOException, InterruptedException {
        return getScriptFile(projectWorkspace);
    }

    public String getCommand() {
        return command;
    }

    public Descriptor getDescriptor() {
        return (DescriptorImpl) Jenkins.getInstance().getDescriptor(getClass());
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<ScriptSource> {

        public DescriptorImpl() {
            super(StringScriptSource.class);
        }

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
