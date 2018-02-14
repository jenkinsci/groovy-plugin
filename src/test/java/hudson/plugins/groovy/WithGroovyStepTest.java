package hudson.plugins.groovy;

import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.steps.StepConfigTester;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.Rule;
import org.jvnet.hudson.test.BuildWatcher;
import org.jvnet.hudson.test.JenkinsRule;

public class WithGroovyStepTest {

    @ClassRule
    public static BuildWatcher buildWatcher = new BuildWatcher();

    @Rule
    public JenkinsRule r = new JenkinsRule();

    @Test
    public void smokes() throws Exception {
        r.createSlave("remote", null, null);
        WorkflowJob p = r.createProject(WorkflowJob.class, "p");
        p.setDefinition(new CpsFlowDefinition("node('remote') {writeFile file: 'x.groovy', text: 'println(/got: ${111/3}/)'; withGroovy {sh 'groovy x.groovy'}}", true));
        r.assertLogContains("got: 37", r.buildAndAssertSuccess(p));
    }

    @Test
    public void configRoundtrip() throws Exception {
        StepConfigTester tester = new StepConfigTester(r);
        WithGroovyStep step = new WithGroovyStep();
        r.assertEqualDataBoundBeans(step, tester.configRoundTrip(step));
        r.jenkins.getDescriptorByType(GroovyInstallation.DescriptorImpl.class).setInstallations(new GroovyInstallation("groovy3", "/usr/share/groovy3", null));
        r.assertEqualDataBoundBeans(step, tester.configRoundTrip(step));
        step.setTool("groovy3");
        r.assertEqualDataBoundBeans(step, tester.configRoundTrip(step));
    }

    @Test
    public void io() throws Exception {
        WorkflowJob p = r.createProject(WorkflowJob.class, "p");
        r.jenkins.getWorkspaceFor(p).child("calc.groovy").write("Pipeline.output(Pipeline.input().collect {k, v -> k * v})", null);
        p.setDefinition(new CpsFlowDefinition("node {def r = withGroovy(input: [once: 1, twice: 2, thrice: 3]) {sh 'groovy calc.groovy'}; echo r.join('/')}", true));
        r.assertLogContains("once/twicetwice/thricethricethrice", r.buildAndAssertSuccess(p));
    }

}
