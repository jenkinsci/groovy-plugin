package hudson.plugins.groovy;

import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Descriptor;
import hudson.model.Hudson;
import hudson.tasks.Builder;
import java.io.IOException;
import net.sf.json.JSONObject;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

/**
 *  A Builder which executes system Groovy script in Hudson JVM (similar to HUDSON_URL/script).
 * 
 * @author dvrzalik
 */
public class SystemGroovy extends AbstractGroovy {

    //initial variable bindings
    String bindings;
    String classpath;
    
    @DataBoundConstructor
    public SystemGroovy(ScriptSource scriptSource, String bindings,String classpath) {
        super(scriptSource);
        this.bindings = bindings;
        this.classpath = classpath;
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
        Hudson.getInstance().checkPermission(Hudson.ADMINISTER);
        
        CompilerConfiguration compilerConfig = new CompilerConfiguration();
        if(classpath != null) {
            compilerConfig.setClasspath(classpath);
        }
        GroovyShell shell = new GroovyShell(new Binding(parseProperties(bindings)),compilerConfig);

        shell.setVariable("out", listener.getLogger());
        Object output = shell.evaluate(getScriptSource().getScriptStream(build.getProject().getWorkspace()));
        if (output instanceof Boolean) {
            return (Boolean) output;
        } else {
            if (output != null) {
                listener.getLogger().println("Script returned: " + output);
            }
            
            if (output instanceof Number) {
                return ((Number) output).intValue() == 0;
            }
        }
        //No output. Suppose success.
        return true;
    }
    
    public Descriptor<Builder> getDescriptor() {
        return DESCRIPTOR;
    }   
    
    public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();

    public static final class DescriptorImpl extends AbstractGroovyDescriptor {

        DescriptorImpl() {
            super(SystemGroovy.class);
            load();
        }
        
        @Override
        public String getDisplayName() {
            return "Execute system Groovy script";
        }
        
         @Override
        public Builder newInstance(StaplerRequest req, JSONObject data) throws FormException {
            ScriptSource source = getScriptSource(req, data);
            String binds = data.getString("bindings");
            String classp = data.getString("classpath");
            return new SystemGroovy(source, binds, classp);
         }

        @Override
        public String getHelpFile() {
            return "/plugin/groovy/systemscript-projectconfig.html";
        }
    }

    //---- Backward compatibility -------- //
    
    public enum BuilderType { COMMAND,FILE }
    
    private String command;
    
    private Object readResolve() {
        if(command != null) {
            scriptSource = new StringScriptSource(command);
            command = null;
        }

        return this;
    }
    
    public String getCommand() {
        return command;
    }

    public String getBindings() {
        return bindings;
    }

    public String getClasspath() {
        return classpath;
    }
}
