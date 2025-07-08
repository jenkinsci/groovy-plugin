package hudson.plugins.groovy;

import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.SystemCredentialsProvider;
import com.cloudbees.plugins.credentials.domains.Domain;
import com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.images.builder.ImageFromDockerfile;

import java.util.Collections;

import static org.junit.jupiter.api.Assumptions.assumeFalse;

@WithJenkins
class WithGroovyStepTest {

    private JenkinsRule j;

    @BeforeEach
    void setUp(JenkinsRule rule) {
        j = rule;
    }

    @Test
    void smokes() throws Exception {
        DumbSlave s = j.createSlave("remote", null, null);
        j.waitOnline(s);
        WorkflowJob p = j.createProject(WorkflowJob.class, "p");
        s.getWorkspaceFor(p).child("x.groovy").write("println(/got: ${111/3}/)", null);
        p.setDefinition(new CpsFlowDefinition("node('remote') {withGroovy {if (isUnix()) {sh 'env | fgrep PATH; groovy x.groovy'} else {bat 'groovy x.groovy'}}}", true));
        j.assertLogContains("got: 37", j.buildAndAssertSuccess(p));
        s.getWorkspaceFor(p).child("x.groovy").write("System.exit(23)", null);
        p.setDefinition(new CpsFlowDefinition("node('remote') {withGroovy {try {if (isUnix()) {sh 'groovy x.groovy'} else {bat 'groovy x.groovy'}} catch (e) {echo(/caught: $e/)}}}", true));
        j.assertLogContains("caught: ", j.buildAndAssertSuccess(p));
    }

    @Test
    void configRoundtrip() throws Exception {
        StepConfigTester tester = new StepConfigTester(j);
        WithGroovyStep step = new WithGroovyStep();
        j.assertEqualDataBoundBeans(step, tester.configRoundTrip(step));
        j.jenkins.getDescriptorByType(GroovyInstallation.DescriptorImpl.class).setInstallations(new GroovyInstallation("groovy3", "/usr/share/groovy3", null));
        j.assertEqualDataBoundBeans(step, tester.configRoundTrip(step));
        step.setTool("groovy3");
        j.assertEqualDataBoundBeans(step, tester.configRoundTrip(step));
        j.jenkins.getDescriptorByType(JDK.DescriptorImpl.class).setInstallations(new JDK("jdk17", "/usr/lib/jvm/java-17-openjdk-amd64"));
        step.setJdk("jdk17");
        j.assertEqualDataBoundBeans(step, tester.configRoundTrip(step));
    }

    @Test
    void io() throws Exception {
        WorkflowJob p = j.createProject(WorkflowJob.class, "p");
        j.jenkins.getWorkspaceFor(p).child("calc.groovy").write("Pipeline.output(Pipeline.input().collect {k, v -> k * v})", null);
        p.setDefinition(new CpsFlowDefinition("node {def r = withGroovy(input: [once: 1, twice: 2, thrice: 3]) {if (isUnix()) {sh 'env | fgrep PATH; groovy calc.groovy'} else {bat 'groovy calc.groovy'}}; echo r.join('/')}", true));
        j.assertLogContains("once/twicetwice/thricethricethrice", j.buildAndAssertSuccess(p));
    }

    @Test
    void ioJep200() throws Exception {
        WorkflowJob p = j.createProject(WorkflowJob.class, "p");
        j.jenkins.getWorkspaceFor(p).child("die.groovy").write("@Grab('com.google.guava:guava:11.0.1') import com.google.common.collect.LinkedListMultimap; Pipeline.output(LinkedListMultimap.create())", null);
        p.setDefinition(new CpsFlowDefinition("node {withGroovy(input: true) {if (isUnix()) {sh 'groovy die.groovy'} else {bat 'groovy die.groovy'}}}", true));
        j.assertLogContains("java.lang.SecurityException: Rejected: com.google.common.collect.LinkedListMultimap; see https://jenkins.io/redirect/class-filter/", j.assertBuildStatus(Result.FAILURE, p.scheduleBuild2(0)));
    }

    @Test
    void tool() throws Exception {
        assumeFalse(Functions.isWindows(), "TODO fails on Windows CI: JAVA_HOME is set to an invalid directory: C:/tools/jdk-8");
        FilePath home = j.jenkins.getRootPath();
        home.unzipFrom(WithGroovyStepTest.class.getResourceAsStream("/groovy-binary-2.4.21.zip"));
        j.jenkins.getDescriptorByType(GroovyInstallation.DescriptorImpl.class).setInstallations(new GroovyInstallation("2.4.x", home.child("groovy-2.4.21").getRemote(), null));
        WorkflowJob p = j.createProject(WorkflowJob.class, "p");
        j.jenkins.getWorkspaceFor(p).child("x.groovy").write("println(/running $GroovySystem.version/)", null);
        p.setDefinition(new CpsFlowDefinition("node {withGroovy(tool: '2.4.x') {if (isUnix()) {sh 'env | egrep \"PATH|GROOVY\"; groovy x.groovy'} else {bat 'groovy x.groovy'}}}", true));
        j.assertLogContains("running 2.4.21", j.buildAndAssertSuccess(p));
    }

    @Test
    void builtInGroovy() throws Exception {
        assumeFalse(Functions.isWindows(), "needs Linux Docker");

        try (GenericContainer<?> container = new GenericContainer<>(new ImageFromDockerfile("java17-ssh", false)
                .withFileFromClasspath("Dockerfile", "Dockerfile"))
                .withExposedPorts(22)) {
            container.start();

            SystemCredentialsProvider.getInstance().getDomainCredentialsMap().put(Domain.global(), Collections.singletonList(new UsernamePasswordCredentialsImpl(CredentialsScope.SYSTEM, "test", null, "test", "test")));
            DumbSlave s = new DumbSlave("docker", "/home/test", new SSHLauncher(container.getHost(), container.getMappedPort(22), "test"));
            j.jenkins.addNode(s);
            j.waitOnline(s);
            WorkflowJob p = j.createProject(WorkflowJob.class, "p");
            s.getWorkspaceFor(p).child("x.groovy").write("println(/running $GroovySystem.version/)", null);
            p.setDefinition(new CpsFlowDefinition("node('docker') {withGroovy {sh 'env | fgrep PATH; groovy x.groovy'}}", true));
            j.assertLogContains("running " + GroovySystem.getVersion(), j.buildAndAssertSuccess(p));
            s.getWorkspaceFor(p).child("x.groovy").write("System.exit(23)", null);
            p.setDefinition(new CpsFlowDefinition("node('docker') {withGroovy {sh 'groovy x.groovy'}}", true));
            j.assertLogContains("23", j.assertBuildStatus(Result.FAILURE, p.scheduleBuild2(0)));
        }
    }

    @Test
    void builtInGroovyGrab() throws Exception {
        DumbSlave s = j.createOnlineSlave();
        WorkflowJob p = j.createProject(WorkflowJob.class, "p");
        s.getWorkspaceFor(p).child("grape.groovy").write("@Grab('commons-primitives:commons-primitives:1.0') import org.apache.commons.collections.primitives.ArrayIntList; def ls = new ArrayIntList(); ls.add(1); ls.add(2); println(ls)", null);
        p.setDefinition(new CpsFlowDefinition("node('" + s.getNodeName() + "') {withGroovy {if (isUnix()) {sh 'env | fgrep PATH; groovy grape.groovy'} else {bat 'groovy grape.groovy'}}}", true));
        j.assertLogContains("[1, 2]", j.buildAndAssertSuccess(p));
    }

    @Test
    void jdk() throws Exception {
        assumeFalse(Functions.isWindows(), "needs Linux Docker");

        try (GenericContainer<?> container = new GenericContainer<>(new ImageFromDockerfile("java17-ssh", false)
                .withFileFromClasspath("Dockerfile", "Dockerfile"))
                .withExposedPorts(22)) {
            container.start();

            SystemCredentialsProvider.getInstance().getDomainCredentialsMap().put(Domain.global(), Collections.singletonList(new UsernamePasswordCredentialsImpl(CredentialsScope.SYSTEM, "test", null, "test", "test")));
            DumbSlave s = new DumbSlave("docker", "/home/test", new SSHLauncher(container.getHost(), container.getMappedPort(22), "test"));
            j.jenkins.addNode(s);
            j.waitOnline(s);
            WorkflowJob p = j.createProject(WorkflowJob.class, "p");
            s.getWorkspaceFor(p).child("x.groovy").write("println(/running ${System.properties['java.version']}/)", null);
            p.setDefinition(new CpsFlowDefinition("node('docker') {withGroovy {sh 'env | fgrep PATH; groovy x.groovy'}}", true));
            j.assertLogContains("running 17.", j.buildAndAssertSuccess(p));
            j.jenkins.getDescriptorByType(JDK.DescriptorImpl.class).setInstallations(new JDK("jdk17", "/usr/lib/jvm/java-17-openjdk-amd64"));
            p.setDefinition(new CpsFlowDefinition("node('docker') {withGroovy(jdk: 'jdk17') {sh 'env | fgrep PATH; groovy x.groovy'}}", true));
            j.assertLogContains("running 17.", j.buildAndAssertSuccess(p));
            FilePath home = s.getRootPath();
            home.unzipFrom(WithGroovyStepTest.class.getResourceAsStream("/groovy-binary-2.4.21.zip"));
            j.jenkins.getDescriptorByType(GroovyInstallation.DescriptorImpl.class).setInstallations(new GroovyInstallation("2.4.x", home.child("groovy-2.4.21").getRemote(), null));
            s.getWorkspaceFor(p).child("x.groovy").write("println(/running $GroovySystem.version on ${System.properties['java.version']}/)", null);
            p.setDefinition(new CpsFlowDefinition("node('docker') {withGroovy(tool: '2.4.x', jdk: 'jdk17') {sh 'env | fgrep PATH; groovy x.groovy'}}", true));
            j.assertLogContains("running 2.4.21 on 17.", j.buildAndAssertSuccess(p));
        }
    }

}
