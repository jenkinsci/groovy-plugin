package hudson.plugins.groovy;

import groovy.lang.Binding;
import hudson.Extension;
import hudson.Launcher;
import hudson.Util;
import hudson.model.BuildListener;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import jenkins.model.Jenkins;
import org.kohsuke.stapler.DataBoundConstructor;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import org.jenkinsci.plugins.scriptsecurity.sandbox.groovy.SecureGroovyScript;
import org.jenkinsci.plugins.scriptsecurity.scripts.ApprovalContext;
import org.jenkinsci.plugins.scriptsecurity.scripts.ClasspathEntry;

/**
 * A Builder which executes system Groovy script in Jenkins JVM (similar to JENKINS_URL/script).
 *
 * @author dvrzalik
 */
public class SystemGroovy extends AbstractGroovy {

    private SystemScriptSource source;
    private String bindings;

    @DataBoundConstructor
    public SystemGroovy(final SystemScriptSource source, final String bindings) {
        this.source = source;
        this.bindings = bindings;
    }

    @Deprecated
    public SystemGroovy(final ScriptSource scriptSource, final String bindings, final String classpath) {
        if (scriptSource instanceof StringScriptSource) {
            source = new StringSystemScriptSource(new SecureGroovyScript(((StringScriptSource) scriptSource).getCommand(), false, null));
        } else {
            source = new FileSystemScriptSource(((FileScriptSource) scriptSource).getScriptFile());
        }
        this.bindings = bindings;
        if (Util.fixEmpty(classpath) != null) {
            throw new UnsupportedOperationException("classpath no longer supported"); // TODO convert StringScriptSource at least
        }
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
        Jenkins jenkins = Jenkins.getInstance();
        if (jenkins == null) {
            throw new IllegalStateException("Jenkins instance is null - Jenkins is shutting down?");
        }
        @Nonnull ClassLoader cl = jenkins.getPluginManager().uberClassLoader;
        // Use HashMap as a backend for Binding as Hashtable does not accept nulls
        Map<Object, Object> binding = new HashMap<Object, Object>();
        binding.putAll(parseProperties(bindings));
        binding.put("build", build);
        if (launcher != null) {
            binding.put("launcher", launcher);
        }
        if (listener != null) {
            binding.put("listener", listener);
            binding.put("out", listener.getLogger());
        }
        try {
            return source.getSecureGroovyScript(build.getWorkspace(), build, listener).evaluate(cl, new Binding(binding));
        } catch (IOException x) {
            throw x;
        } catch (InterruptedException x) {
            throw x;
        } catch (RuntimeException x) {
            throw x;
        } catch (Exception x) {
            throw new IOException(x);
        }
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

    @Deprecated
    private transient String command;
    @Deprecated
    private transient ScriptSource scriptSource;
    @Deprecated
    private transient String classpath;

    @SuppressWarnings("deprecation")
    private Object readResolve() throws Exception {
        if (command != null) {
            source = new StringSystemScriptSource(new SecureGroovyScript(command, false, null).configuring(ApprovalContext.create()));
            command = null;
        } else if (scriptSource instanceof StringScriptSource) {
            List<ClasspathEntry> classpathEntries = new ArrayList<ClasspathEntry>();
            if (classpath != null) {
                StringTokenizer tokens = new StringTokenizer(classpath);
                while (tokens.hasMoreTokens()) {
                    classpathEntries.add(new ClasspathEntry(tokens.nextToken()));
                }
            }
            source = new StringSystemScriptSource(new SecureGroovyScript(Util.fixNull(((StringScriptSource) scriptSource).getCommand()), false, classpathEntries).configuring(ApprovalContext.create()));
            scriptSource = null;
        } else if (scriptSource instanceof FileScriptSource) {
            if (Util.fixEmpty(classpath) != null) {
                throw new UnsupportedOperationException("classpath no longer supported in conjunction with Groovy script file source");
            } else {
                source = new FileSystemScriptSource(((FileScriptSource) scriptSource).getScriptFile());
                scriptSource = null;
            }
        }

        return this;
    }

    public SystemScriptSource getSource() {
        return source;
    }

    public String getBindings() {
        return bindings;
    }

}
