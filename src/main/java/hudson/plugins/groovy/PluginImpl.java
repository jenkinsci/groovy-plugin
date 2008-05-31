package hudson.plugins.groovy;

import hudson.Plugin;
import hudson.tasks.BuildStep;

/**
 * Entry point of Groovy plugin.
 *
 * @author dvrzalik
 * @plugin
 */
public class PluginImpl extends Plugin {
    @Override
    public void start() throws Exception {
       
        BuildStep.BUILDERS.add(Groovy.DESCRIPTOR);
        BuildStep.BUILDERS.add(SystemGroovy.DESCRIPTOR);
    }
}
