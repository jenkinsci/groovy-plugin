package hudson.plugins.groovy;

import hudson.Extension;
import hudson.Util;
import org.jenkinsci.plugins.workflow.cps.steps.ingroovy.GroovyStep;
import org.jenkinsci.plugins.workflow.cps.steps.ingroovy.GroovyStepDescriptor;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

/**
 * Runs {@code groovy} from Pipeline.
 */
public class GroovyPipelineStep extends GroovyStep {

    private final String args;
    private String tool;

    @DataBoundConstructor
    public GroovyPipelineStep(String args) {
        this.args = args.replaceAll("\\s+", " ").trim();
    }

    public String getArgs() {
        return args;
    }

    public String getTool() {
        return tool;
    }

    @DataBoundSetter
    public void setTool(String tool) {
        this.tool = Util.fixEmpty(tool);
    }

    @Extension
    public static class DescriptorImpl extends GroovyStepDescriptor {

        @Override
        public String getFunctionName() {
            return "groovy";
        }

        @Override
        public String getDisplayName() {
            return "Execute Groovy script";
        }

    }

}
