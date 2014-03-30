package hudson.plugins.groovy;

import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.model.BuildListener;
import hudson.model.Descriptor;
import hudson.model.AbstractBuild;

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
public class FileScriptSource extends ScriptSource {

    private String scriptFile;

    @DataBoundConstructor
    public FileScriptSource(String scriptFile) {
        this.scriptFile = scriptFile;
    }

    @Override
    public FilePath getScriptFile(FilePath projectWorkspace) {
        return new FilePath(projectWorkspace, scriptFile);
    }
    
    @Override
    public FilePath getScriptFile(FilePath projectWorkspace, AbstractBuild<?, ?> build, BuildListener listener) throws IOException, InterruptedException{
    	EnvVars env = build.getEnvironment(listener);
    	String expandedScriptdFile = env.expand(this.scriptFile);
        return new FilePath(projectWorkspace, expandedScriptdFile);
    }

    public String getScriptFile() {
      return scriptFile;
    }

    @Override
    public InputStream getScriptStream(FilePath projectWorkspace) throws IOException {
        return getScriptFile(projectWorkspace).read();
    }

    @Override
    public InputStream getScriptStream(FilePath projectWorkspace, AbstractBuild<?, ?> build, BuildListener listener) throws IOException, InterruptedException {
        return getScriptFile(projectWorkspace,build,listener).read();
    }
    
    @Extension
    public static class DescriptorImpl extends Descriptor<ScriptSource> {

        @Override
        public String getDisplayName() {
            return "Groovy script file";
        }
        
    }
}
