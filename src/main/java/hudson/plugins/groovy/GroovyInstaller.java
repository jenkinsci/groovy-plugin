package hudson.plugins.groovy;

import hudson.Extension;
import hudson.tools.DownloadFromUrlInstaller;
import hudson.tools.ToolInstallation;

import org.kohsuke.stapler.DataBoundConstructor;

public class GroovyInstaller extends DownloadFromUrlInstaller {                                                                                
    @DataBoundConstructor                                                                                                                      
    public GroovyInstaller(String id) {                                                                                                        
        super(id);                                                                                                                             
    }                                                                                                                                          
                                                                                                                                               
    @Extension                                                                                                                                 
    public static final class DescriptorImpl extends DownloadFromUrlInstaller.DescriptorImpl<GroovyInstaller> {                                
        public String getDisplayName() {                                                                                                       
            return "Install from http://groovy.codehaus.org";                                                                                                  
        }                                                                                                                                      
                                                                                                                                               
        @Override                                                                                                                              
        public boolean isApplicable(Class<? extends ToolInstallation> toolType) {                                                              
            return toolType == GroovyInstallation.class;                                                                                       
        }                                                                                                                                      
    }                                                                                                                                          
}    