package hudson.plugins.groovy;

import com.thoughtworks.xstream.XStream;
import groovy.lang.Binding;
import groovy.lang.GroovyShell;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.*;
import hudson.security.ACL;
import hudson.tasks.Builder;

import java.io.IOException;

import hudson.util.Secret;
import hudson.util.XStream2;
import net.sf.json.JSONObject;

import org.acegisecurity.Authentication;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

/**
 *  A Builder which executes system Groovy script in Hudson JVM (similar to HUDSON_URL/script).
 * 
 * @author dvrzalik
 */
public class SystemGroovy extends AbstractGroovy {

    // initial variable bindings
    private String bindings;
    private String classpath;
    private transient Object output;

    private static final XStream XSTREAM = new XStream2();
    
    @DataBoundConstructor
    public SystemGroovy(final ScriptSource scriptSource, final String bindings, final String classpath) {
        super(scriptSource);
        this.bindings = bindings;
        this.classpath = classpath;
    }

    /**
     * @return SystemGroovy as an encrypted String
     */
    public String getSecret() {
        return Secret.fromString(XSTREAM.toXML(this)).getEncryptedValue();
    }

    @Override
    public boolean perform(final AbstractBuild<?, ?> build,
                           final Launcher launcher,
                           final BuildListener listener)
        throws InterruptedException, IOException
    {
        // Hudson.getInstance().checkPermission(Hudson.ADMINISTER); // WTF - always pass, executed as SYSTEM

        CompilerConfiguration compilerConfig = new CompilerConfiguration();
        if (classpath != null) {
            compilerConfig.setClasspath(classpath);
        }

        // see RemotingDiagnostics.Script
        ClassLoader cl = Hudson.getInstance().getPluginManager().uberClassLoader;

        if (cl == null) {
            cl = Thread.currentThread().getContextClassLoader();
        }

        GroovyShell shell =
            new GroovyShell(cl, new Binding(parseProperties(bindings)), compilerConfig);

        shell.setVariable("build", build);
        shell.setVariable("launcher", launcher);
        shell.setVariable("listener", listener);
        shell.setVariable("out", listener.getLogger());

        output = shell.evaluate(
            getScriptSource().getScriptStream(build.getWorkspace(),build,listener)
        );
        
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

        // No output. Suppose success.
        return true;
    }

    @Override
    public Descriptor<Builder> getDescriptor() {
        return DESCRIPTOR;
    }

    @Extension
    public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();

    public static final class DescriptorImpl extends AbstractGroovyDescriptor  {

        DescriptorImpl() {
            super(SystemGroovy.class);
            load();
        }
        
        @Override
        public String getDisplayName() {
            return "Execute system Groovy script";
        }
        
        @Override
        public boolean isApplicable(Class<? extends AbstractProject> jobType){
        	Authentication a = Hudson.getAuthentication();
            if(Hudson.getInstance().getACL().hasPermission(a,Hudson.ADMINISTER)){
            	return true;
            }
        	return false;
        }
        
        @Override
        public SystemGroovy newInstance(StaplerRequest req, JSONObject data) throws FormException {

            // don't allow unauthorized users to modify scripts
            Authentication a = Hudson.getAuthentication();
            if (Hudson.getInstance().getACL().hasPermission(a,Hudson.ADMINISTER)) {
                ScriptSource source = getScriptSource(req, data);
                String binds = data.getString("bindings");
                String classp = data.getString("classpath");
                return new SystemGroovy(source, binds, classp);
            } else {
                String secret = data.getString("secret");
                return (SystemGroovy) XSTREAM.fromXML(Secret.decrypt(secret).getPlainText());
            }
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

    public Object getOutput() {
        return output;
    }
}
