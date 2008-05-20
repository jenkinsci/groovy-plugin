package hudson.plugins.groovy;

import hudson.FilePath;
import hudson.model.Descriptor;
import java.io.IOException;
import java.io.InputStream;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

/**
 * Groovy source based on given script file.
 * 
 * @author dvrzalik
 */
public class FileScriptSource implements ScriptSource {

    private String scriptFile;

    @DataBoundConstructor
    public FileScriptSource(String scriptFile) {
        this.scriptFile = scriptFile;
    }

    @Override
    public FilePath getScriptFile(FilePath projectWorkspace) {
        return new FilePath(projectWorkspace, scriptFile);
    }

    public String getScriptFile() {
      return scriptFile;
    }

    @Override
    public InputStream getScriptStream(FilePath projectWorkspace) throws IOException {
        return getScriptFile(projectWorkspace).read();
    }

    public Descriptor<ScriptSource> getDescriptor() {
        return DESCRIPTOR;
    }
    public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();

    public static class DescriptorImpl extends Descriptor {

        public DescriptorImpl() {
            super(FileScriptSource.class);
        }

        @Override
        public String getDisplayName() {
            return "Groovy script file";
        }
        
        @Override
        public ScriptSource newInstance(StaplerRequest req, JSONObject formData) throws FormException {
            return req.bindJSON(FileScriptSource.class, formData);
        }
    }
}
