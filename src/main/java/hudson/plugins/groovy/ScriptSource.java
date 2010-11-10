package hudson.plugins.groovy;

import hudson.FilePath;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Describable;
import hudson.util.DescriptorList;
import java.io.IOException;
import java.io.InputStream;

/**
 * Base interface for Groovy script sources.
 * 
 * @author dvrzalik
 */
public interface ScriptSource extends Describable<ScriptSource> {

    /**
     * In the end, every script is a file...
     * 
     * @param Project workspace (useful when the source has to create temporary file)
     * @return Path to the executed script file 
     * @throws java.io.IOException
     * @throws java.lang.InterruptedException
     */
    public FilePath getScriptFile(FilePath projectWorkspace) throws IOException, InterruptedException;
    
    /**
     * Able to load script when script path contains parameters
     * 
     * @param Project workspace to create tmp file
     * @param Build - needed to obtain environment variables
     * @param listener - build listener needed by Environment
     * @return Path to the executed script file
     * @throws IOException
     * @throws InterruptedException
     */
    public FilePath getScriptFile(FilePath projectWorkspace, AbstractBuild<?, ?> build, BuildListener listener) throws IOException, InterruptedException;
    
    /**
     * @return Stream containing the script
     */
    public InputStream getScriptStream(FilePath projectWorkspace) throws IOException, InterruptedException;
    
    /**
     * @return Stream containing the script, able to load script when script path contains parameters
     */
    public InputStream getScriptStream(FilePath projectWorkspace, AbstractBuild<?, ?> build, BuildListener listener) throws IOException, InterruptedException;
    
    public static final DescriptorList<ScriptSource> SOURCES = 
            new DescriptorList<ScriptSource>(ScriptSource.class);

}
