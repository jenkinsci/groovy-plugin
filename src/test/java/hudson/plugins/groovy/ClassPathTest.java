package hudson.plugins.groovy;

import hudson.model.FreeStyleProject;

import java.io.File;

import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.JenkinsRule;

public class ClassPathTest {
    
    @Rule
    public JenkinsRule j = new JenkinsRule();
    
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
        Groovy g = new Groovy(script,"(Default)", "", "","aaa=\"bbb\"", "", this.getClass().getResource("/lib").getPath() + File.separator + testJar);
        FreeStyleProject p = j.createFreeStyleProject();
        p.getBuildersList().add(g);
        j.assertLogContains("bbb", j.buildAndAssertSuccess(p));
    }
    
}
