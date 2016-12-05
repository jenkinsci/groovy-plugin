package hudson.plugins.groovy;

import hudson.cli.CLICommandInvoker;
import hudson.cli.CreateJobCommand;
import hudson.diagnosis.OldDataMonitor;
import hudson.model.AdministrativeMonitor;
import hudson.model.FreeStyleProject;
import hudson.model.Item;
import hudson.tasks.Builder;
import java.io.ByteArrayInputStream;
import java.util.Collections;
import jenkins.model.Jenkins;
import static org.hamcrest.Matchers.containsString;
import org.jenkinsci.plugins.scriptsecurity.sandbox.groovy.SecureGroovyScript;
import org.jenkinsci.plugins.scriptsecurity.scripts.ClasspathEntry;
import org.jenkinsci.plugins.scriptsecurity.scripts.UnapprovedUsageException;
import org.junit.ClassRule;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Rule;
import org.jvnet.hudson.test.BuildWatcher;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.recipes.LocalData;

@Issue("SECURITY-292")
public class SystemGroovyTest {

    @ClassRule
    public static BuildWatcher buildWatcher = new BuildWatcher();

    @Rule
    public JenkinsRule r = new JenkinsRule();

    @Test
    public void scriptSecurity() throws Exception {
        String configXml = "<project><builders><" + SystemGroovy.class.getName() + ">" +
            "<scriptSource class=\"" + StringScriptSource.class.getName() + "\"><command>jenkins.model.Jenkins.instance.systemMessage = 'pwned'</command></scriptSource>" +
            "<bindings/><classpath/></" + SystemGroovy.class.getName() + "></builders></project>";
        assertThat(new CLICommandInvoker(r, new CreateJobCommand()).authorizedTo(Jenkins.READ, Item.CREATE).withArgs("attack").withStdin(new ByteArrayInputStream(configXml.getBytes())).invoke(), CLICommandInvoker.Matcher.succeeded());
        r.assertLogContains(UnapprovedUsageException.class.getName(), r.jenkins.getItemByFullName("attack", FreeStyleProject.class).scheduleBuild2(0).get());
        assertNull(r.jenkins.getSystemMessage());
    }

    @LocalData
    @Test
    public void upgrade() throws Exception {
        verifyUpgrade(Groovy.class, new Groovy(new StringScriptSource("println 'hello'"), "(Default)", "", "", "", "-ea", "/tmp/x.jar"), "external", null);
        SystemGroovy sg = new SystemGroovy(new StringSystemScriptSource(new SecureGroovyScript("println x", false, null)));
        sg.setBindings("x=33");
        verifyUpgrade(SystemGroovy.class, sg, "string", null);
        verifyUpgrade(SystemGroovy.class, new SystemGroovy(new StringSystemScriptSource(new SecureGroovyScript("true", false, Collections.singletonList(new ClasspathEntry("/tmp/x.jar"))))), "jarcp", null);
        verifyUpgrade(SystemGroovy.class, null, "dircp", "directory-based classpath entries not supported in system Groovy script string source");
        verifyUpgrade(SystemGroovy.class, new SystemGroovy(new FileSystemScriptSource("x.groovy")), "ws", null);
        verifyUpgrade(SystemGroovy.class, null, "wscp", "classpath no longer supported in conjunction with system Groovy script file source");
    }
    private <T extends Builder> void verifyUpgrade(Class<T> type, T expected, String job, String err) throws Exception {
        FreeStyleProject p = r.jenkins.getItemByFullName(job, FreeStyleProject.class);
        T actual = p.getBuildersList().get(type);
        r.assertEqualDataBoundBeans(expected, actual);
        assert expected == null ^ err == null;
        if (err != null) {
            OldDataMonitor.VersionRange datum = AdministrativeMonitor.all().get(OldDataMonitor.class).getData().get(p);
            assertNotNull(datum);
            assertThat("right message for " + job, datum.extra, containsString(err));
        }
    }

}
