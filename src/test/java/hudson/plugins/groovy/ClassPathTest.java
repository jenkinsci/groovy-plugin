package hudson.plugins.groovy;

import hudson.FilePath;
import hudson.Functions;
import hudson.model.FreeStyleProject;
import java.io.File;
import static org.junit.Assume.assumeFalse;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.JenkinsRule;

public class ClassPathTest {

    @BeforeClass
    public static void linux() {
        assumeFalse("TODO on CI (but not locally in Windows 10 on Java 19): find.exe is not recognized as an internal or external command…", Functions.isWindows());
    }
    
    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Before
    public void setUpGroovy() throws Exception {
        FilePath home = j.jenkins.getRootPath();
        home.unzipFrom(ClassPathTest.class.getResourceAsStream("/groovy-binary-2.4.21.zip"));
        j.jenkins.getDescriptorByType(GroovyInstallation.DescriptorImpl.class).setInstallations(new GroovyInstallation("2.4.x", home.child("groovy-2.4.21").getRemote(), null));
    }
    
    /**
     * Tests that groovy build step accepts wild cards on class path
     */
    @Issue("JENKINS-26070")
    @Test
    public void testWildcartOnClassPath() throws Exception {
        final String testJar = "groovy-cp-test.jar";
        final ScriptSource script = new StringScriptSource(
                "def printCP(classLoader){\n "
                + "  if (classLoader instanceof java.net.URLClassLoader) {\n"
                + "    classLoader.getURLs().each {println \"$it\"}\n"
                + "  }\n"
                + "  if(classLoader.parent) {printCP(classLoader.parent)}\n"
                + "}\n"
                + "printCP(this.class.classLoader)");
        Groovy g = new Groovy(script, "2.4.x", "", "", "", "", this.getClass().getResource("/lib").getPath() + "/*");
        FreeStyleProject p = j.createFreeStyleProject();
        p.setAssignedNode(j.createSlave());
        p.getBuildersList().add(g);
        j.assertLogContains(testJar, j.buildAndAssertSuccess(p));
    }
    
    @Issue("JENKINS-29577")
    @Test
    public void testClassPathAndProperties() throws Exception {
        final String testJar = "groovy-cp-test.jar";
        StringBuilder sb = new StringBuilder();
        final ScriptSource script = new StringScriptSource(
                sb.append("import App\n")
                .append("println System.getProperty(\"aaa\")")
                .toString()
                );
        Groovy g = new Groovy(script, "2.4.x", "", "", "aaa=\"bbb\"", "", this.getClass().getResource("/lib").getPath() + File.separator + testJar);
        FreeStyleProject p = j.createFreeStyleProject();
        p.getBuildersList().add(g);
        j.assertLogContains("bbb", j.buildAndAssertSuccess(p));
    }
    
}
