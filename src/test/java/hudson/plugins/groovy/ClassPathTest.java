package hudson.plugins.groovy;

import hudson.FilePath;
import hudson.model.FreeStyleProject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

import java.io.File;

@WithJenkins
@DisabledOnOs(value = OS.WINDOWS, disabledReason = "TODO on CI (but not locally in Windows 10 on Java 19): find.exe is not recognized as an internal or external command…")
class ClassPathTest {

    private JenkinsRule j;

    @BeforeEach
    void setUp(JenkinsRule rule) throws Exception {
        j = rule;

        FilePath home = j.jenkins.getRootPath();
        home.unzipFrom(ClassPathTest.class.getResourceAsStream("/groovy-binary-2.4.21.zip"));
        j.jenkins.getDescriptorByType(GroovyInstallation.DescriptorImpl.class).setInstallations(new GroovyInstallation("2.4.x", home.child("groovy-2.4.21").getRemote(), null));
    }

    /**
     * Tests that groovy build step accepts wild cards on class path
     */
    @Issue("JENKINS-26070")
    @Test
    void testWildcardOnClassPath() throws Exception {
        final String testJar = "groovy-cp-test.jar";
        final ScriptSource script = new StringScriptSource(
                """
                        def printCP(classLoader) {
                          if (classLoader instanceof java.net.URLClassLoader) {
                            classLoader.getURLs().each {println "$it"}
                          }
                          if(classLoader.parent) {printCP(classLoader.parent)}
                        }
                        printCP(this.class.classLoader)""");
        Groovy g = new Groovy(script, "2.4.x", "", "", "", "", this.getClass().getResource("/lib").getPath() + "/*");
        FreeStyleProject p = j.createFreeStyleProject();
        p.setAssignedNode(j.createSlave());
        p.getBuildersList().add(g);
        j.assertLogContains(testJar, j.buildAndAssertSuccess(p));
    }

    @Issue("JENKINS-29577")
    @Test
    void testClassPathAndProperties() throws Exception {
        final String testJar = "groovy-cp-test.jar";
        final ScriptSource script = new StringScriptSource(
                """
                import App
                println System.getProperty("aaa")"""
                );
        Groovy g = new Groovy(script, "2.4.x", "", "", "aaa=\"bbb\"", "", this.getClass().getResource("/lib").getPath() + File.separator + testJar);
        FreeStyleProject p = j.createFreeStyleProject();
        p.getBuildersList().add(g);
        j.assertLogContains("bbb", j.buildAndAssertSuccess(p));
    }

}
