package hudson.plugins.groovy;

import groovy.lang.GroovySystem;
import hudson.FilePath;
import hudson.Functions;
import hudson.model.JDK;
import hudson.model.Result;
import hudson.plugins.sshslaves.SSHLauncher;
import hudson.slaves.DumbSlave;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.steps.StepConfigTester;
import org.jenkinsci.test.acceptance.docker.DockerRule;
import org.jenkinsci.test.acceptance.docker.fixtures.JavaContainer;
import static org.junit.Assume.assumeFalse;
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

    @Rule
    public DockerRule<JavaContainer> javaContainerRule = new DockerRule<>(JavaContainer.class);

    @Rule
    public DockerRule<JavaContainerWith9> java9ContainerRule = new DockerRule<>(JavaContainerWith9.class);

    @Test
    public void smokes() throws Exception {
        DumbSlave s = r.createSlave("remote", null, null);
        r.waitOnline(s);
        WorkflowJob p = r.createProject(WorkflowJob.class, "p");
        s.getWorkspaceFor(p).child("x.groovy").write("println(/got: ${111/3}/)", null);
        p.setDefinition(new CpsFlowDefinition("node('remote') {withGroovy {if (isUnix()) {sh 'env | fgrep PATH; groovy x.groovy'} else {bat 'groovy x.groovy'}}}", true));
        r.assertLogContains("got: 37", r.buildAndAssertSuccess(p));
        s.getWorkspaceFor(p).child("x.groovy").write("System.exit(23)", null);
        p.setDefinition(new CpsFlowDefinition("node('remote') {withGroovy {try {if (isUnix()) {sh 'groovy x.groovy'} else {bat 'groovy x.groovy'}} catch (e) {echo(/caught: $e/)}}}", true));
        r.assertLogContains("caught: ", r.buildAndAssertSuccess(p));
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
        r.jenkins.getDescriptorByType(JDK.DescriptorImpl.class).setInstallations(new JDK("jdk9", "/usr/share/jdk9"));
        step.setJdk("jdk9");
        r.assertEqualDataBoundBeans(step, tester.configRoundTrip(step));
    }

    @Test
    public void io() throws Exception {
        WorkflowJob p = r.createProject(WorkflowJob.class, "p");
        r.jenkins.getWorkspaceFor(p).child("calc.groovy").write("Pipeline.output(Pipeline.input().collect {k, v -> k * v})", null);
        p.setDefinition(new CpsFlowDefinition("node {def r = withGroovy(input: [once: 1, twice: 2, thrice: 3]) {if (isUnix()) {sh 'env | fgrep PATH; groovy calc.groovy'} else {bat 'groovy calc.groovy'}}; echo r.join('/')}", true));
        r.assertLogContains("once/twicetwice/thricethricethrice", r.buildAndAssertSuccess(p));
    }

    @Test
    public void ioJep200() throws Exception {
        WorkflowJob p = r.createProject(WorkflowJob.class, "p");
        r.jenkins.getWorkspaceFor(p).child("die.groovy").write("@Grab('com.google.guava:guava:11.0.1') import com.google.common.collect.LinkedListMultimap; Pipeline.output(LinkedListMultimap.create())", null);
        p.setDefinition(new CpsFlowDefinition("node {withGroovy(input: true) {if (isUnix()) {sh 'groovy die.groovy'} else {bat 'groovy die.groovy'}}}", true));
        r.assertLogContains("java.lang.SecurityException: Rejected: com.google.common.collect.LinkedListMultimap; see https://jenkins.io/redirect/class-filter/", r.assertBuildStatus(Result.FAILURE, p.scheduleBuild2(0)));
    }

    @Test
    public void tool() throws Exception {
        assumeFalse("TODO fails on Windows CI: JAVA_HOME is set to an invalid directory: C:/tools/jdk-8", Functions.isWindows());
        FilePath home = r.jenkins.getRootPath();
        home.unzipFrom(WithGroovyStepTest.class.getResourceAsStream("/groovy-binary-2.4.13.zip"));
        r.jenkins.getDescriptorByType(GroovyInstallation.DescriptorImpl.class).setInstallations(new GroovyInstallation("2.4.x", home.child("groovy-2.4.13").getRemote(), null));
        WorkflowJob p = r.createProject(WorkflowJob.class, "p");
        r.jenkins.getWorkspaceFor(p).child("x.groovy").write("println(/running $GroovySystem.version/)", null);
        p.setDefinition(new CpsFlowDefinition("node {withGroovy(tool: '2.4.x') {if (isUnix()) {sh 'env | fgrep PATH; groovy x.groovy'} else {bat 'groovy x.groovy'}}}", true));
        r.assertLogContains("running 2.4.13", r.buildAndAssertSuccess(p));
    }

    @Test
    public void builtInGroovy() throws Exception {
        assumeFalse("needs Linux Docker", Functions.isWindows());
        JavaContainer container = javaContainerRule.get();
        DumbSlave s = new DumbSlave("docker", "/home/test", new SSHLauncher(container.ipBound(22), container.port(22), "test", "test", "", ""));
        r.jenkins.addNode(s);
        r.waitOnline(s);
        WorkflowJob p = r.createProject(WorkflowJob.class, "p");
        s.getWorkspaceFor(p).child("x.groovy").write("println(/running $GroovySystem.version/)", null);
        p.setDefinition(new CpsFlowDefinition("node('docker') {withGroovy {sh 'env | fgrep PATH; groovy x.groovy'}}", true));
        r.assertLogContains("running " + GroovySystem.getVersion(), r.buildAndAssertSuccess(p));
        s.getWorkspaceFor(p).child("x.groovy").write("System.exit(23)", null);
        p.setDefinition(new CpsFlowDefinition("node('docker') {withGroovy {sh 'groovy x.groovy'}}", true));
        r.assertLogContains("23", r.assertBuildStatus(Result.FAILURE, p.scheduleBuild2(0)));
    }

    @Test
    public void builtInGroovyGrab() throws Exception {
        DumbSlave s = r.createOnlineSlave();
        WorkflowJob p = r.createProject(WorkflowJob.class, "p");
        s.getWorkspaceFor(p).child("grape.groovy").write("@Grab('commons-primitives:commons-primitives:1.0') import org.apache.commons.collections.primitives.ArrayIntList; def ls = new ArrayIntList(); ls.add(1); ls.add(2); println(ls)", null);
        p.setDefinition(new CpsFlowDefinition("node('" + s.getNodeName() + "') {withGroovy {if (isUnix()) {sh 'env | fgrep PATH; groovy grape.groovy'} else {bat 'groovy grape.groovy'}}}", true));
        r.assertLogContains("[1, 2]", r.buildAndAssertSuccess(p));
    }

    @Test
    public void jdk() throws Exception {
        assumeFalse("needs Linux Docker", Functions.isWindows());
        JavaContainerWith9 container = java9ContainerRule.get();
        DumbSlave s = new DumbSlave("docker", "/home/test", new SSHLauncher(container.ipBound(22), container.port(22), "test", "test", "", ""));
        r.jenkins.addNode(s);
        r.waitOnline(s);
        WorkflowJob p = r.createProject(WorkflowJob.class, "p");
        s.getWorkspaceFor(p).child("x.groovy").write("println(/running ${System.properties['java.version']}/)", null);
        p.setDefinition(new CpsFlowDefinition("node('docker') {withGroovy {sh 'env | fgrep PATH; groovy x.groovy'}}", true));
        r.assertLogContains("running 1.8.", r.buildAndAssertSuccess(p));
        r.jenkins.getDescriptorByType(JDK.DescriptorImpl.class).setInstallations(new JDK("jdk9", "/opt/jdk9"));
        p.setDefinition(new CpsFlowDefinition("node('docker') {withGroovy(jdk: 'jdk9') {sh 'env | fgrep PATH; groovy x.groovy'}}", true));
        r.assertLogContains("running 9.", r.buildAndAssertSuccess(p));
        FilePath home = s.getRootPath();
        home.unzipFrom(WithGroovyStepTest.class.getResourceAsStream("/groovy-binary-2.4.13.zip"));
        r.jenkins.getDescriptorByType(GroovyInstallation.DescriptorImpl.class).setInstallations(new GroovyInstallation("2.4.x", home.child("groovy-2.4.13").getRemote(), null));
        s.getWorkspaceFor(p).child("x.groovy").write("println(/running $GroovySystem.version on ${System.properties['java.version']}/)", null);
        p.setDefinition(new CpsFlowDefinition("node('docker') {withGroovy(tool: '2.4.x', jdk: 'jdk9') {sh 'env | fgrep PATH; groovy x.groovy'}}", true));
        r.assertLogContains("running 2.4.13 on 9.", r.buildAndAssertSuccess(p));
    }

}
