package hudson.plugins.groovy;

import hudson.EnvVars;
import hudson.Extension;
import hudson.Launcher.LocalLauncher;
import hudson.Util;
import hudson.model.EnvironmentSpecific;
import hudson.model.TaskListener;
import hudson.model.Hudson;
import hudson.model.Node;
import hudson.remoting.Callable;
import hudson.remoting.VirtualChannel;
import hudson.slaves.NodeSpecific;
import hudson.tools.ToolDescriptor;
import hudson.tools.ToolInstaller;
import hudson.tools.ToolProperty;
import hudson.tools.ToolInstallation;
import hudson.util.NullStream;
import hudson.util.StreamTaskListener;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

import org.kohsuke.stapler.DataBoundConstructor;

public class GroovyInstallation extends ToolInstallation implements EnvironmentSpecific<GroovyInstallation>, NodeSpecific<GroovyInstallation> {
	
	private final String name;
    private final String home;

    /*
    public GroovyInstallation(String name, String home) {
    	super(name,home,new LinkedList());
        this.name = name;
        this.home = home;
    }
	*/
    @DataBoundConstructor
    public GroovyInstallation(String name, String home, List<? extends ToolProperty<?>> properties){
    	super(name,home,properties);
    	this.name = name;
    	this.home = home;
  
    }
    
    /**
     * install directory.
     */
    public String getHome() {
        return home;
    }

    /**
     * Human readable display name.
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the executable path of this groovy installation on the given target system.
     */
    public String getExecutable(VirtualChannel channel) throws IOException, InterruptedException {
        return channel.call(new Callable<String, IOException>() {

            public String call() throws IOException {
                File exe = getExeFile("groovy");
                if (exe.exists()) {
                    return exe.getPath();
                }
                return null;
            }
        });
    }

    private File getExeFile(String execName) {
        String groovyHome = Util.replaceMacro(getHome(),EnvVars.masterEnvVars);
        File binDir = new File(groovyHome, "bin/");
        if (File.separatorChar == '\\') {                
            if(new File(binDir, execName + ".exe").exists()) {
                execName += ".exe";
            } else {
                execName += ".bat";
            }
        }
        return new File(binDir, execName);            
    }

    /**
     * Returns true if the executable exists.
     */
    /*
    public boolean exists() {
        try {
            return getExecutable(new LocalLauncher(new StreamTaskListener(new NullStream())).getChannel()) != null;
        } catch (IOException e) {
            return false;
        } catch (InterruptedException e) {
            return false;
        }
    }
	*/
    
    public GroovyInstallation forEnvironment(EnvVars environment) {                                                                            
        return new GroovyInstallation(getName(), environment.expand(getHome()), getProperties().toList());                                    
    }                                                                                                                                          
                                                                                                                                               
    public GroovyInstallation forNode(Node node, TaskListener log) throws IOException, InterruptedException {                                  
        return new GroovyInstallation(getName(), translateFor(node, log), getProperties().toList());                                           
    }       
    
    @Extension                                                                                                                                 
    public static class DescriptorImpl extends ToolDescriptor<GroovyInstallation> {                                                            
                                                                                                                                               
        public DescriptorImpl() {                                                                                                              
        }                                                                                                                                      
                                                                                                                                               
        @Override                                                                                                                              
        public String getDisplayName() {                                                                                                       
            return "Groovy";//Messages.installer_displayName();                                                                                           
        }                                                                                                                                      
                                                                                                                                               
        @Override                                                                                                                              
        public List<? extends ToolInstaller> getDefaultInstallers() {                                                                          
            return Collections.singletonList(new GroovyInstaller(null));                                                                       
        }                                                                                                                                      
                                                                                                                                               
        // for compatibility reasons, the persistence is done by GradleBuilder.DescriptorImpl                                                  
                                                                                                                                               
        @Override                                                                                                                              
        public GroovyInstallation[] getInstallations() {                                                                                       
            return Hudson.getInstance().getDescriptorByType(Groovy.DescriptorImpl.class).getInstallations();                                   
        }                                                                                                                                      
                                                                                                                                               
        @Override                                                                                                                              
        public void setInstallations(GroovyInstallation... installations) {                                                                    
            Hudson.getInstance().getDescriptorByType(Groovy.DescriptorImpl.class).setInstallations(installations);                             
        }                                                                                                                                      
                                                                                                                                               
    }       
    
    
    private static final long serialVersionUID = 1L;

}
