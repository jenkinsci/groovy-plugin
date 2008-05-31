package hudson.plugins.groovy;

import hudson.FilePath;
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
     * @return Stream containing the script
     */
    public InputStream getScriptStream(FilePath projectWorkspace) throws IOException, InterruptedException;
    
    public static final DescriptorList<ScriptSource> SOURCES = 
            new DescriptorList<ScriptSource>(StringScriptSource.DESCRIPTOR, FileScriptSource.DESCRIPTOR);

}
