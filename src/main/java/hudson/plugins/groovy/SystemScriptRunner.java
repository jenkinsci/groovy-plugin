package hudson.plugins.groovy;

import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import hudson.EnvVars;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.plugins.groovy.exceptions.GroovyScriptExecutionException;
import jenkins.model.Jenkins;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.customizers.ImportCustomizer;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Map;

/**
 * A runner which executes system Groovy script in Jenkins JVM. Can be used in internal logic of other Jenkins plugins.
 * @param <T>
 *
 * @author sshelomentsev
 */
public class SystemScriptRunner<T> {

    private ScriptSource scriptSource;
    CompilerConfiguration configuration;
    Binding binding;
    ClassLoader cl;
    AbstractBuild<?, ?> build;
    BuildListener listener;

    public SystemScriptRunner(ScriptSource scriptSource, final AbstractBuild<?, ?> build, final BuildListener listener,
                              final String bindings) throws GroovyScriptExecutionException {
        this.scriptSource = scriptSource;
        this.build = build;
        this.listener = listener;
        init(bindings);
    }

    private void init(final String bindings) throws GroovyScriptExecutionException {
        configuration = new CompilerConfiguration();
        configuration.setScriptBaseClass("hudson.plugins.groovy.AbstractScript");
        ImportCustomizer icz = new ImportCustomizer();
        icz.addStarImports("jenkins", "hudson", "jenkins.model", "hudson.model", "hudson.util", "hudson.remoting");
        configuration.addCompilationCustomizers(icz);

        try {
            binding = new Binding(Utils.parseProperties(bindings));
            binding.setVariable("jenkins", Jenkins.getInstance());
        } catch (IOException e) {
            throw new GroovyScriptExecutionException("Failed to bind properties to groovy shell");
        }
        cl = Jenkins.getInstance().pluginManager.uberClassLoader;
        if (null == cl) {
            cl = Thread.currentThread().getContextClassLoader();
        }
    }

    public void addEnvVars() {
        try {
            EnvVars envVars = build.getEnvironment(listener);
            envVars.overrideAll(build.getBuildVariables());
            for (Map.Entry<String, String> entry : envVars.entrySet()) {
                binding.setProperty(entry.getKey(), entry.getValue());
            }
            binding.setVariable("build", build);
            binding.setVariable("listener", listener);
            binding.setVariable("out", listener.getLogger());
        } catch (Exception e) {
            listener.getLogger().println("Failed to bind environment variables to groovy shell");
            listener.getLogger().println(e.getMessage());
        }
    }

    public void bindVariable(String key, Object value) {
        binding.setVariable(key, value);
    }

    public void bindProperty(String key, Object value) {
        binding.setProperty(key, value);
    }

    public T evaluate() throws IOException, InterruptedException {
        GroovyShell shell = new GroovyShell(cl, binding, configuration);
        Object ret = shell.evaluate(new InputStreamReader(scriptSource.getScriptStream(build.getWorkspace(), build,
                                                                                       listener)));
        return (T) ret;
    }

}
