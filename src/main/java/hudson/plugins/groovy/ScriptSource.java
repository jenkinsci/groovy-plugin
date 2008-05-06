package hudson.plugins.groovy;

import hudson.FilePath;
import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.util.DescriptorList;
import java.io.IOException;
import java.util.List;

/**
 * Base class for Groovy script sources.
 * 
 * @author dvrzalik
 */
public interface ScriptSource extends Describable<ScriptSource> {

    /**
     * In the end, every script is a file...
     * 
     * @param workspace
     * @return Path to the executed script file 
     * @throws java.io.IOException
     * @throws java.lang.InterruptedException
     */
    public FilePath getScriptFile(FilePath workspace) throws IOException, InterruptedException;
    
    public static final DescriptorList<ScriptSource> SOURCES = 
            new DescriptorList<ScriptSource>(StringScriptSource.DESCRIPTOR, FileScriptSource.DESCRIPTOR);

}
