package hudson.plugins.groovy;

import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import hudson.EnvVars;
import hudson.Extension;
import hudson.Launcher;
import hudson.Util;
import hudson.model.BuildListener;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Hudson;
import hudson.util.Secret;
import hudson.util.VariableResolver;
import hudson.util.XStream2;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.Charset;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import jenkins.model.Jenkins;
import net.sf.json.JSONObject;

import org.acegisecurity.Authentication;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

import com.thoughtworks.xstream.XStream;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

/**
 * A Builder which executes system Groovy script in Jenkins JVM (similar to JENKINS_URL/script).
 *
 * @author dvrzalik
 */
public class SystemGroovy extends AbstractGroovy {

    private String bindings;
    private String classpath;

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

        Object output = run(build, listener, launcher);

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

        // No error indication - success
        return true;
    }

    /*packahge*/ Object run(AbstractBuild<?, ?> build, BuildListener listener, @CheckForNull Launcher launcher) throws IOException, InterruptedException {
        final List<URL> classPathURLs = new ArrayList<URL>();
        if (classpath != null) {
            EnvVars env = build.getEnvironment(listener);
            env.overrideAll(build.getBuildVariables());
            VariableResolver<String> vr = new VariableResolver.ByMap<String>(env);
            classPathURLs.addAll(parseClassPath(classpath, vr));
        }

        Jenkins jenkins = Jenkins.getInstance();
        if (jenkins == null ) {
            throw new IllegalStateException("Jenkins instance is null - Jenkins is shutting down?");
        }
        
        @Nonnull final ClassLoader cl = jenkins.getPluginManager().uberClassLoader;
        // normally doPrivileged should be called only when System.getSecurityManager() is not null, but Findbugs still considers 
        // creating new classloader in else branch as an issues, so calling doPrivileged always (until some better solution is found 
        // or Finbugs is more clever)
        URLClassLoader extendedClassLoader = AccessController.doPrivileged(new PrivilegedAction<URLClassLoader>() {
            @Override
            public URLClassLoader run() {
                return new URLClassLoader(classPathURLs.toArray(new URL[classPathURLs.size()]), cl);
            }
        });
        // Use HashMap as a backend for Binding as Hashtable does not accept nulls
        Map<Object, Object> binding = new HashMap<Object, Object>();
        binding.putAll(parseProperties(bindings));
        GroovyShell shell = new GroovyShell(extendedClassLoader, new Binding(binding));

        shell.setVariable("build", build);
        if (launcher != null)
            shell.setVariable("launcher", launcher);
        if (listener != null) {
            shell.setVariable("listener", listener);
            shell.setVariable("out", listener.getLogger());
        }

        InputStream scriptStream = getScriptSource().getScriptStream(build.getWorkspace(), build, listener);
        return shell.evaluate(new InputStreamReader(scriptStream, Charset.defaultCharset()));
    }

    private URL pathToURL(String path){
        try {
            return new URL(path);
        } catch (MalformedURLException x) {
            try {
                return new File(path).toURI().toURL();
            } catch (MalformedURLException e) {
                return null;
            }
        }
    }

    private List<URL> parseClassPath(String classPath, VariableResolver<String> vr) {
        List<URL> cp = new ArrayList<URL>();
        for (String path : classPath.split("\n")){
            StringTokenizer tokens = new StringTokenizer(classPath);
            while(tokens.hasMoreTokens()) {
                URL url = pathToURL(Util.replaceMacro(tokens.nextToken(),vr));
                if (url != null){
                    cp.add(url);
                }
            }
        }
        return cp;
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
            Jenkins jenkins = Jenkins.getInstance();
            if (jenkins == null ) {
                throw new IllegalStateException("Jenkins instance is null - Jenkins is shutting down?");
            }
            
            Authentication a = Jenkins.getAuthentication();
            if (jenkins.getACL().hasPermission(a, Jenkins.RUN_SCRIPTS)) {
                return true;
            }
            return false;
        }

        @Override
        public SystemGroovy newInstance(StaplerRequest req, JSONObject data) throws FormException {
            Jenkins jenkins = Jenkins.getInstance();
            if (jenkins == null ) {
                throw new IllegalStateException("Jenkins instance is null - Jenkins is shutting down?");
            }
            // don't allow unauthorized users to modify scripts
            Authentication a = Jenkins.getAuthentication();
            if (jenkins.getACL().hasPermission(a, Jenkins.RUN_SCRIPTS)) {
                return (SystemGroovy) super.newInstance(req, data);
            } else {
                String secret = data.getString("secret");
                return (SystemGroovy) XSTREAM.fromXML(Secret.decrypt(secret).getPlainText());
            }
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
}
