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
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.jenkinsci.plugins.workflow.steps.BodyExecutionCallback;
import org.jenkinsci.plugins.workflow.steps.EnvironmentExpander;
import org.jenkinsci.plugins.workflow.steps.Step;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepDescriptor;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

/**
 * Makes {@code groovy} in {@code PATH} be bound to an appropriate executable.
 */
public class WithGroovyStep extends Step {

    private String tool;
    private Object input;

    @DataBoundConstructor
    public WithGroovyStep() {}

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

    @Override
    public StepExecution start(StepContext context) throws Exception {
        return new Execution(context, this);
    }

    private static class Execution extends StepExecution {

        private final transient WithGroovyStep step;

        Execution(StepContext context, WithGroovyStep step) {
            super(context);
            this.step = step;
        }

        @Override
        public boolean start() throws Exception {
            FilePath base = WorkspaceList.tempDir(getContext().get(FilePath.class));
            base.mkdirs();
            FilePath tmp = base.createTempDir("groovy", "");
            Map<String, String> env = new HashMap<>();
            // TODO set PATH+GROOVY to $tool.home/bin
            // TODO or if tool == null, create wrapper scripts $tmp/{groovy,groovy.bat/startGroovy/startGroovy.bat} and copy groovy.jar from agent
            if (step.input != null) {
                // TODO remoting calls here are going to be blocking CPS VM thread
                try (OutputStream os = tmp.child("input.ser").write(); ObjectOutputStream oos = new ObjectOutputStream(os)) {
                    oos.writeObject(step.input);
                }
                tmp.child("Pipeline.groovy").copyFrom(WithGroovyStep.class.getResource("Pipeline.groovy"));
                env.put("CLASSPATH+GROOVY", tmp.getRemote());
            }
            getContext().newBodyInvoker().
                withContext(EnvironmentExpander.constant(env)).
                withCallback(new Callback(tmp)).
                start();
            return false;
        }

    }

    private static class Callback extends BodyExecutionCallback {

        private final String tmp;

        Callback(FilePath tmp) {
            this.tmp = tmp.getRemote();
        }

        @Override
        public void onSuccess(StepContext context, Object result) {
            try {
                FilePath ser = context.get(FilePath.class).child(tmp).child("output.ser");
                if (ser.exists()) {
                    try (InputStream is = ser.read(); ObjectInputStream ois = new ObjectInputStreamEx(is, GroovyObject.class.getClassLoader(), ClassFilter.DEFAULT)) {
                        context.onSuccess(ois.readObject());
                        return;
                    }
                }
            } catch (Exception x) {
                context.onFailure(x);
                return;
            }
            context.onSuccess(result);
        }

        @Override
        public void onFailure(StepContext context, Throwable t) {
            context.onFailure(t);
        }

    }

    @Extension
    public static class DescriptorImpl extends StepDescriptor {

        @Override
        public String getFunctionName() {
            return "withGroovy";
        }

        @Override
        public String getDisplayName() {
            return "Execute Groovy script";
        }

        @Override
        public Set<Class<?>> getRequiredContext() {
            return Collections.<Class<?>>singleton(FilePath.class);
        }

        @Override
        public boolean takesImplicitBlockArgument() {
            return true;
        }

    }

}
