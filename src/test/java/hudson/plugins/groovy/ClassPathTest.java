package hudson.plugins.groovy;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import hudson.model.FreeStyleBuild;
import hudson.model.Result;
import hudson.model.FreeStyleProject;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.JenkinsRule;

public class ClassPathTest {
    
    @Rule
    public JenkinsRule j = new JenkinsRule();

    @BeforeClass
    public static void checkGroovyExecutable() throws Exception {
        try {
            Process proc = Runtime.getRuntime().exec("groovy -v");
            int exitCode = proc.waitFor(); // We rely on the test timeout
            Assume.assumeThat("Groovy is not installed", exitCode, equalTo(0));
        } catch (IOException ex) {
            Assume.assumeNoException("Failed to validate the Groovy installation", ex);
        }
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
                + "  classLoader.getURLs().each {println \"$it\"}\n"
                + "  if(classLoader.parent) {printCP(classLoader.parent)}\n"
                + "}\n"
                + "printCP(this.class.classLoader)");
        Groovy g = new Groovy(script,"(Default)", "", "","", "", this.getClass().getResource("/lib").getPath() + "/*");
        FreeStyleProject p = j.createFreeStyleProject();
        p.getBuildersList().add(g);

        FreeStyleBuild build = j.buildAndAssertSuccess(p);
        j.assertLogContains(testJar, build);
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
        Groovy g = new Groovy(script,"(Default)", "", "","aaa=\"bbb\"", "", this.getClass().getResource("/lib").getPath() + File.separator + testJar);
        FreeStyleProject p = j.createFreeStyleProject();
        p.getBuildersList().add(g);

        FreeStyleBuild build = j.buildAndAssertSuccess(p);
        j.assertLogContains("bbb", build);
    }
}
