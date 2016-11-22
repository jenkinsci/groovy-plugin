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
import hudson.util.VariableResolver;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import jenkins.model.Jenkins;
import org.kohsuke.stapler.DataBoundConstructor;

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

    @DataBoundConstructor
    public SystemGroovy(final ScriptSource scriptSource, final String bindings, final String classpath) {
        super(scriptSource);
        this.bindings = bindings;
        this.classpath = classpath;
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
        List<URL> classPathURLs = new ArrayList<URL>();
        if (classpath != null) {
            EnvVars env = build.getEnvironment(listener);
            env.overrideAll(build.getBuildVariables());
            VariableResolver<String> vr = new VariableResolver.ByMap<String>(env);
            classPathURLs.addAll(parseClassPath(classpath, vr));
        }

        // see RemotingDiagnostics.Script
        @Nonnull ClassLoader cl = Jenkins.getInstance().getPluginManager().uberClassLoader;
        URLClassLoader extendedClassLoader = new URLClassLoader(classPathURLs.toArray(new URL[classPathURLs.size()]), cl);
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
            return true;
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
