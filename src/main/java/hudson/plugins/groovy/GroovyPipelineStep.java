package hudson.plugins.groovy;

import groovy.lang.GroovyObject;
import hudson.Extension;
import hudson.FilePath;
import hudson.Util;
import hudson.remoting.ClassFilter;
import hudson.remoting.ObjectInputStreamEx;
import hudson.slaves.WorkspaceList;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import org.jenkinsci.plugins.workflow.cps.steps.ingroovy.GroovyStep;
import org.jenkinsci.plugins.workflow.cps.steps.ingroovy.GroovyStepDescriptor;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

/**
 * Runs {@code groovy} from Pipeline.
 */
public class GroovyPipelineStep extends GroovyStep {

    private final String args;
    private String tool;
    private Object input;

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

    public Object getInput() {
        return input;
    }

    @DataBoundSetter
    public void setInput(Object input) {
        this.input = input;
    }

    public String prepArgs(StepContext context, AtomicReference<String> loc) throws Exception {
        if (input == null) {
            return args;
        }
        FilePath tmp = tempDir(context.get(FilePath.class));
        tmp.mkdirs();
        FilePath dir = tmp.createTempDir("groovyio", "");
        loc.set(dir.getRemote());
        // TODO remoting calls here are going to be blocking CPS VM thread
        try (OutputStream os = dir.child("input.ser").write(); ObjectOutputStream oos = new ObjectOutputStream(os)) {
            oos.writeObject(input);
        }
        dir.child("Pipeline.groovy").copyFrom(GroovyPipelineStep.class.getResource("Pipeline.groovy"));
        StringBuilder fullArgs = new StringBuilder();
        fullArgs.append("-cp ").append(dir); // TODO look for -classpath/-cp/--classpath in args and append
        fullArgs.append(" -Dpipeline.io.dir=").append(dir).append(' ');
        return fullArgs.append(args).toString();
    }

    public Object returnValue(StepContext context, String loc) throws Exception {
        if (loc == null) {
            return null;
        }
        FilePath ser = context.get(FilePath.class).child(loc).child("output.ser");
        if (ser.exists()) {
            try (InputStream is = ser.read(); ObjectInputStream ois = new ObjectInputStreamEx(is, GroovyObject.class.getClassLoader(), ClassFilter.DEFAULT)) {
                return ois.readObject();
            }
        } else {
            return null;
        }
    }

    // TODO use 1.652 use WorkspaceList.tempDir
    private static FilePath tempDir(FilePath ws) {
        return ws.sibling(ws.getName() + System.getProperty(WorkspaceList.class.getName(), "@") + "tmp");
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

        @Override
        public Set<Class<?>> getRequiredContext() {
            return Collections.<Class<?>>singleton(FilePath.class);
        }

    }

}
