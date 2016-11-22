package hudson.plugins.groovy;

import static org.junit.Assert.*;
import hudson.model.Result;
import hudson.model.FreeStyleProject;
import hudson.tasks.Builder;

import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import hudson.cli.CLICommandInvoker;
import hudson.cli.CreateJobCommand;
import hudson.model.Item;
import java.io.ByteArrayInputStream;
import jenkins.model.Jenkins;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.jvnet.hudson.test.BuildWatcher;
import org.jvnet.hudson.test.Issue;

public class GroovyPluginTest {

    @ClassRule
    public static BuildWatcher buildWatcher = new BuildWatcher();

    @Rule
    public JenkinsRule j = new JenkinsRule();

    //JENKINS-25392
    @Test
    public void testFailWhenScriptThrowsException() throws Exception {
        ScriptSource script = new StringScriptSource("throw new Exception(\"test\")");
        Groovy g = new Groovy(script,"(Default)", "", "","", "", "");
        FreeStyleProject p = j.createFreeStyleProject();
        p.getBuildersList().add(g);
        assertEquals(Result.FAILURE, p.scheduleBuild2(0).get().getResult());

    }

    @Test
    public void assignNullToBindingVariables() throws Exception {
        FreeStyleProject p = j.createFreeStyleProject();
        p.getBuildersList().add(new SystemGroovy(new StringScriptSource("bindingVar = null"),"",""));
        j.buildAndAssertSuccess(p);
    }

    /// Roundtrip

    @Test
    public void roundtripTestSystemGroovyStringScript() throws Exception {
        SystemGroovy before = new SystemGroovy(new StringScriptSource("println 'Test'"),"TEST=45","test.jar");
        SystemGroovy after = doRoundtrip(before, SystemGroovy.class);

        j.assertEqualBeans(before, after, "scriptSource,bindings,classpath");
    }

    @Test
    public void roundtripTestSystemGroovyFileScript() throws Exception {
        SystemGroovy before = new SystemGroovy(new FileScriptSource("test.groovy"),"TEST=45","test.jar");
        SystemGroovy after = doRoundtrip(before, SystemGroovy.class);

        j.assertEqualBeans(before, after, "scriptSource,bindings,classpath");
    }

    @Test
    public void roundtripTestGroovyFileScript() throws Exception {
        Groovy before = new Groovy(new FileScriptSource("test.groovy"),"(Default)", "-Xmx1024m", "TEST=45","some.property=true", "-Xmx1024m", "test.jar");
        Groovy after = doRoundtrip(before, Groovy.class);

        j.assertEqualBeans(before, after, "scriptSource,groovyName,parameters,scriptParameters,properties,javaOpts,classPath");
    }

    @Test
    public void roundtripTestGroovyStringScript() throws Exception {
        Groovy before = new Groovy(new StringScriptSource("println 'Test'"),"(Default)", "-Xmx1024m", "TEST=45","some.property=true", "-Xmx1024m", "test.jar");
        Groovy after = doRoundtrip(before, Groovy.class);

        j.assertEqualBeans(before, after, "scriptSource,groovyName,parameters,scriptParameters,properties,javaOpts,classPath");
    }

    @Test
    public void roundtripTestGroovyParams() throws Exception {
        Groovy before = new Groovy(new StringScriptSource("println 'Test'"),"(Default)", "some 'param with spaces' and other params", "some other 'param with spaces' and other params", "some.property=true", "-Xmx1024m", "test.jar");
        Groovy after = doRoundtrip(before, Groovy.class);
        j.assertEqualBeans(before, after, "parameters,scriptParameters");
    }

    @Test
    public void roundtripTestGroovyParamsSlashes() throws Exception {
        Groovy before = new Groovy(new StringScriptSource("println 'Test'"),"(Default)", "some 'param with spaces' and http://slashes/and c:\\backslashes", "some other 'param with spaces' and http://slashes/and c:\\backslashes", "some.property=true", "-Xmx1024m", "test.jar");
        Groovy after = doRoundtrip(before, Groovy.class);
        j.assertEqualBeans(before, after, "parameters,scriptParameters");
    }

    private <T extends Builder> T doRoundtrip(T before, Class<T> clazz) throws Exception {
        FreeStyleProject p = j.createFreeStyleProject();
        p.getBuildersList().add(before);

        j.submit(j.createWebClient().getPage(p,"configure").getFormByName("config"));
        T after = p.getBuildersList().get(clazz);
        return after;
    }

    @Ignore("TODO expected null, but was:<pwned>")
    @Issue("SECURITY-292")
    @Test
    public void scriptSecurity() throws Exception {
        String configXml = "<project><builders><" + SystemGroovy.class.getName() + ">" +
            "<scriptSource class=\"" + StringScriptSource.class.getName() + "\"><command>jenkins.model.Jenkins.instance.systemMessage = 'pwned'</command></scriptSource>" +
            "<bindings/><classpath/></" + SystemGroovy.class.getName() + "></builders></project>";
        assertThat(new CLICommandInvoker(j, new CreateJobCommand()).authorizedTo(Jenkins.READ, Item.CREATE).withArgs("attack").withStdin(new ByteArrayInputStream(configXml.getBytes())).invoke(), CLICommandInvoker.Matcher.succeeded());
        j.jenkins.getItemByFullName("attack", FreeStyleProject.class).scheduleBuild2(0).get();
        assertNull(j.jenkins.getSystemMessage());
    }

}
