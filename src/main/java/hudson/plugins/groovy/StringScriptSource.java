package hudson.plugins.groovy;

import hudson.FilePath;
import hudson.model.Descriptor;
import hudson.model.Descriptor.FormException;
import java.io.IOException;
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
    public FilePath getScriptFile(FilePath workspace) throws IOException, InterruptedException {
        return workspace.createTextTempFile("hudson", ".groovy", command, true);
    }

    public String getCommand() {
      return command;
    }

    public Descriptor getDescriptor() {
        return DESCRIPTOR;
    }
    public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();

    public static class DescriptorImpl extends Descriptor {

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
