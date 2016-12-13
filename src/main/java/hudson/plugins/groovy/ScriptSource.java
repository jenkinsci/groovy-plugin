package hudson.plugins.groovy;

import hudson.DescriptorExtensionList;
import hudson.FilePath;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Describable;
import hudson.model.Descriptor;
import jenkins.model.Jenkins;

import java.io.IOException;
import java.io.InputStream;

/**
 * Base interface for Groovy script sources.
 *
 * @author dvrzalik
 */
public abstract class ScriptSource implements Describable<ScriptSource> {

    /**
     * Able to load script when script path contains parameters
     *
     * @param projectWorkspace Project workspace to create tmp file
     * @param build            - needed to obtain environment variables
     * @param listener         - build listener needed by Environment
     * @return Path to the executed script file
     * @throws IOException
     * @throws InterruptedException
     */
    public abstract FilePath getScriptFile(FilePath projectWorkspace, AbstractBuild<?, ?> build, BuildListener listener) throws IOException, InterruptedException;

    /**
     * @return Stream containing the script, able to load script when script path contains parameters
     */
    public abstract InputStream getScriptStream(FilePath projectWorkspace, AbstractBuild<?, ?> build, BuildListener listener) throws IOException, InterruptedException;

    /**
     * In the end, every script is a file...
     *
     * @param projectWorkspace Project workspace (useful when the source has to create temporary file)
     * @return Path to the executed script file
     * @deprecated Unused.
     */
    @Deprecated
    public FilePath getScriptFile(FilePath projectWorkspace) throws IOException, InterruptedException {
        throw new UnsupportedOperationException();
    }

    /**
     * @deprecated Unused
     */
    @Deprecated
    public InputStream getScriptStream(FilePath projectWorkspace) throws IOException, InterruptedException {
        throw new UnsupportedOperationException();
    }

    @Override
    @SuppressWarnings("unchecked")
    public Descriptor<ScriptSource> getDescriptor() {
        Jenkins jenkins = Jenkins.getInstance();
        if (jenkins == null ) {
            throw new IllegalStateException("Jenkins instance is null - Jenkins is shutting down?");
        }
        
        return jenkins.getDescriptorOrDie(getClass());
    }

    public static final DescriptorExtensionList<ScriptSource, Descriptor<ScriptSource>> all() {
        Jenkins jenkins = Jenkins.getInstance();
        if (jenkins == null ) {
            throw new IllegalStateException("Jenkins instance is null - Jenkins is shutting down?");
        }
        
        return jenkins.getDescriptorList(ScriptSource.class);
    }
}
