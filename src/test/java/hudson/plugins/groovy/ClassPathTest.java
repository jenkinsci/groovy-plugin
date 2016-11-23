package hudson.plugins.groovy;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import hudson.model.Result;
import hudson.model.FreeStyleProject;

import java.io.File;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.jenkinsci.plugins.scriptsecurity.sandbox.groovy.SecureGroovyScript;

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
        final ScriptSource script = new StringScriptSource(new SecureGroovyScript(
                "def printCP(classLoader){\n "
                + "  classLoader.getURLs().each {println \"$it\"}\n"
                + "  if(classLoader.parent) {printCP(classLoader.parent)}\n"
                + "}\n"
                + "printCP(this.class.classLoader)", true, null));
        Groovy g = new Groovy(script,"(Default)", "", "","", "", this.getClass().getResource("/lib").getPath() + "/*");
        FreeStyleProject p = j.createFreeStyleProject();
        p.getBuildersList().add(g);
        assertEquals(Result.SUCCESS, p.scheduleBuild2(0).get(10,TimeUnit.SECONDS).getResult());
        assertTrue(containsString(p.scheduleBuild2(0).get().getLog(100), testJar));
    }

    @Issue("JENKINS-29577")
    @Test
    public void testClassPathAndProperties() throws Exception {
        final String testJar = "groovy-cp-test.jar";
        StringBuilder sb = new StringBuilder();
        final ScriptSource script = new StringScriptSource(new SecureGroovyScript(
                sb.append("import App\n")
                .append("println System.getProperty(\"aaa\")")
                .toString()
                , true, null));
        Groovy g = new Groovy(script,"(Default)", "", "","aaa=\"bbb\"", "", this.getClass().getResource("/lib").getPath() + File.separator + testJar);
        FreeStyleProject p = j.createFreeStyleProject();
        p.getBuildersList().add(g);
        assertEquals(Result.SUCCESS, p.scheduleBuild2(0).get(10,TimeUnit.SECONDS).getResult());
        assertTrue(containsString(p.scheduleBuild2(0).get().getLog(100), "bbb"));
    }
    
    private boolean containsString(List<String> input, String searchStr) {
        boolean isPresent = false;
        for(String str : input) {
            if(str.contains(searchStr)) {
                isPresent = true;
                break;
            }
        }
        return isPresent;
    }
    
}
