package hudson.plugins.groovy;

import groovy.lang.GroovyShell;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Descriptor;
import hudson.model.Hudson;
import hudson.remoting.Callable;
import hudson.tasks.Builder;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.StaplerRequest;

/**
 *  A Builder which executes system Groovy script in Hudson JVM (similar to HUDSON_URL/script).
 * 
 * @author dvrzalik
 */
public class SystemGroovy extends Builder {

    private String command;
     
     /**
     * @stapler-constructor
     */
    SystemGroovy(String command) {
        this.command = command;
    }  
    
    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
       Hudson.getInstance().checkPermission(Hudson.ADMINISTER);
       return launcher.getChannel().call(new SystemGroovyCallable( command, listener));
    }
    
    private static class SystemGroovyCallable implements Callable<Boolean, RuntimeException> {
        
        private BuildListener listener;
        private String command;
        
        public SystemGroovyCallable(String command, BuildListener listener) {
            this.listener = listener;
            this.command = command;
        }
        
        public Boolean call() throws RuntimeException {
                    GroovyShell shell = new GroovyShell();
                    
                    shell.setVariable("out", listener.getLogger());
                    Object output = shell.evaluate(command);
                             
                    //Check if the script returns something meaningful
                    if(output instanceof Boolean) {
                        return (Boolean) output;
                    } else {
                        if (output != null) {
                            listener.getLogger().println("Script returned: "+output);
                        }

                        if (output instanceof Number) {
                            return ((Number) output).intValue() == 0;
                        } 
                    } 
                    //No output. Suppose success.
                    return true;
                }
    }
    
    public Descriptor<Builder> getDescriptor() {
        return DESCRIPTOR;
    }   
    
    public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();

    public static final class DescriptorImpl extends Descriptor<Builder> {

        DescriptorImpl() {
            super(SystemGroovy.class);
            load();
        }
        
        @Override
        public String getDisplayName() {
            return "Execute system Groovy script";
        }
        
         @Override
        public Builder newInstance(StaplerRequest req) throws FormException {
            String cmd = req.getParameter("groovy.system_command");            
            return new SystemGroovy(cmd);
        }

        @Override
        public String getHelpFile() {
            return "/plugin/groovy/sytemscript-projectconfig.html";
        }
        
         
    }

    public String getCommand() {
        return command;
    }
    
    
}
