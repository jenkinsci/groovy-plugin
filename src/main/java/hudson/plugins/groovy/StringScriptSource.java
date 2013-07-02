package hudson.plugins.groovy;

import hudson.Extension;
import hudson.FilePath;
import hudson.model.BuildListener;
import hudson.model.AbstractBuild;
import hudson.model.Descriptor;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import net.sf.json.JSONObject;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

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
    public FilePath getScriptFile(FilePath projectWorkspace, AbstractBuild<?, ?> build, BuildListener listener) throws IOException, InterruptedException {
        return getScriptFile(projectWorkspace);
    }

    public String getCommand() {
      return command;
    }

    public Descriptor getDescriptor() {
        return DESCRIPTOR;
    }

    @Extension
    public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();

    public static class DescriptorImpl extends Descriptor<ScriptSource> {

        public DescriptorImpl() {
            super(StringScriptSource.class);
        }

        @Override
        public String getDisplayName() {
            return "Groovy command";
        }

        @Override
        public ScriptSource newInstance(StaplerRequest req, JSONObject formData) throws FormException {
            return req.bindJSON(StringScriptSource.class, formData);
        }
        
    }
}
