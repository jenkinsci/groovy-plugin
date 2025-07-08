package hudson.plugins.groovy;

import hudson.model.FreeStyleProject;
import hudson.model.Result;
import hudson.tasks.Builder;
import org.jenkinsci.plugins.scriptsecurity.sandbox.groovy.SecureGroovyScript;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

import static org.junit.jupiter.api.Assertions.assertEquals;

@WithJenkins
class GroovyPluginTest {

    private JenkinsRule j;

    @BeforeEach
    void setUp(JenkinsRule rule) {
        j = rule;
    }


    //JENKINS-25392
    @Test
    void testFailWhenScriptThrowsException() throws Exception {
        ScriptSource script = new StringScriptSource("throw new Exception(\"test\")");
        Groovy g = new Groovy(script,"(Default)", "", "","", "", "");
        FreeStyleProject p = j.createFreeStyleProject();
        p.getBuildersList().add(g);
        assertEquals(Result.FAILURE, p.scheduleBuild2(0).get().getResult());
    }

    @Test
    void assignNullToBindingVariables() throws Exception {
        FreeStyleProject p = j.createFreeStyleProject();
        p.getBuildersList().add(new SystemGroovy(new StringSystemScriptSource(new SecureGroovyScript("bindingVar = null", true, null))));
        j.buildAndAssertSuccess(p);
    }

    /// Roundtrip

    @Test
    void roundtripTestSystemGroovyStringScript() throws Exception {
        SystemGroovy before = new SystemGroovy(new StringSystemScriptSource(new SecureGroovyScript("println 'Test'", true, null)));
        before.setBindings("TEST=45");
        SystemGroovy after = doRoundtrip(before, SystemGroovy.class);

        j.assertEqualDataBoundBeans(before, after);
    }

    @Test
    void roundtripTestSystemGroovyFileScript() throws Exception {
        SystemGroovy before = new SystemGroovy(new FileSystemScriptSource("test.groovy"));
        before.setBindings("TEST=45");
        SystemGroovy after = doRoundtrip(before, SystemGroovy.class);

        j.assertEqualDataBoundBeans(before, after);
    }

    @Test
    void roundtripTestGroovyFileScript() throws Exception {
        Groovy before = new Groovy(new FileScriptSource("test.groovy"),"(Default)", "-Xmx1024m", "TEST=45","some.property=true", "-Xmx1024m", "test.jar");
        Groovy after = doRoundtrip(before, Groovy.class);

        j.assertEqualBeans(before, after, "scriptSource,groovyName,parameters,scriptParameters,properties,javaOpts,classPath");
    }

    @Test
    void roundtripTestGroovyStringScript() throws Exception {
        Groovy before = new Groovy(new StringScriptSource("println 'Test'"),"(Default)", "-Xmx1024m", "TEST=45","some.property=true", "-Xmx1024m", "test.jar");
        Groovy after = doRoundtrip(before, Groovy.class);

        j.assertEqualBeans(before, after, "scriptSource,groovyName,parameters,scriptParameters,properties,javaOpts,classPath");
    }

    @Test
    void roundtripTestGroovyParams() throws Exception {
        Groovy before = new Groovy(new StringScriptSource("println 'Test'"),"(Default)", "some 'param with spaces' and other params", "some other 'param with spaces' and other params", "some.property=true", "-Xmx1024m", "test.jar");
        Groovy after = doRoundtrip(before, Groovy.class);
        j.assertEqualBeans(before, after, "parameters,scriptParameters");
    }

    @Test
    void roundtripTestGroovyParamsSlashes() throws Exception {
        Groovy before = new Groovy(new StringScriptSource("println 'Test'"),"(Default)", "some 'param with spaces' and http://slashes/and c:\\backslashes", "some other 'param with spaces' and http://slashes/and c:\\backslashes", "some.property=true", "-Xmx1024m", "test.jar");
        Groovy after = doRoundtrip(before, Groovy.class);
        j.assertEqualBeans(before, after, "parameters,scriptParameters");
    }

    private <T extends Builder> T doRoundtrip(T before, Class<T> clazz) throws Exception {
        FreeStyleProject p = j.createFreeStyleProject();
        p.getBuildersList().add(before);

        j.submit(j.createWebClient().getPage(p,"configure").getFormByName("config"));
        p.doReload(); // Workaround to drop transient properties in Script Security 1172.v35f6a_0b_8207e+
        T after = p.getBuildersList().get(clazz);
        return after;
    }

}
