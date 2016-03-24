package hudson.plugins.groovy;

import com.thoughtworks.xstream.XStream;
import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import hudson.EnvVars;
import hudson.Extension;
import hudson.Launcher;
import hudson.Util;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.util.Secret;
import hudson.util.VariableResolver;
import hudson.util.XStream2;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.acegisecurity.Authentication;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

/**
 * A Builder which executes system Groovy script in Jenkins JVM (similar to JENKINS_URL/script).
 *
 * @author dvrzalik
 */
public class SystemGroovy extends AbstractGroovy {

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
    public boolean perform(final AbstractBuild<?, ?> build, final Launcher launcher, final BuildListener listener)
            throws InterruptedException, IOException {

        CompilerConfiguration compilerConfig = new CompilerConfiguration();
        if (classpath != null) {
            EnvVars env = build.getEnvironment(listener);
            env.overrideAll(build.getBuildVariables());
            VariableResolver<String> vr = new VariableResolver.ByMap<String>(env);
            if(classpath.contains("\n")) {
                compilerConfig.setClasspathList(Utils.parseClassPath(classpath, vr));
            } else {
                compilerConfig.setClasspath(Util.replaceMacro(classpath,vr));
            }
        }

        // see RemotingDiagnostics.Script
        ClassLoader cl = Jenkins.getInstance().getPluginManager().uberClassLoader;

        if (cl == null) {
            cl = Thread.currentThread().getContextClassLoader();
        }

        // Use HashMap as a backend for Binding as Hashtable does not accept nulls
        Map<Object, Object> binding = new HashMap<Object, Object>();
        binding.putAll(Utils.parseProperties(bindings));
        GroovyShell shell = new GroovyShell(cl, new Binding(binding), compilerConfig);

        shell.setVariable("build", build);
        if (launcher != null)
            shell.setVariable("launcher", launcher);
        if (listener != null) {
            shell.setVariable("listener", listener);
            shell.setVariable("out", listener.getLogger());
        }

        output = shell.evaluate(new InputStreamReader(getScriptSource().getScriptStream(build.getWorkspace(), build,
                listener)));

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

    @Extension
    public static final class DescriptorImpl extends AbstractGroovyDescriptor {

        public DescriptorImpl() {
            super(SystemGroovy.class);
            load();
        }

        @Override
        public String getDisplayName() {
            return "Execute system Groovy script";
        }

        @Override
        @SuppressWarnings("rawtypes")
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            Authentication a = Jenkins.getAuthentication();
            if (Jenkins.getInstance().getACL().hasPermission(a, Jenkins.RUN_SCRIPTS)) {
                return true;
            }
            return false;
        }

        @Override
        public SystemGroovy newInstance(StaplerRequest req, JSONObject data) throws FormException {

            // don't allow unauthorized users to modify scripts
            Authentication a = Jenkins.getAuthentication();
            if (Jenkins.getInstance().getACL().hasPermission(a, Jenkins.RUN_SCRIPTS)) {
                return (SystemGroovy) super.newInstance(req, data);
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

    // ---- Backward compatibility -------- //

    public enum BuilderType {
        COMMAND, FILE
    }

    private String command;

    private Object readResolve() {
        if (command != null) {
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
