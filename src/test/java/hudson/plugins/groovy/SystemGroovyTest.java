package hudson.plugins.groovy;

import hudson.Functions;
import hudson.cli.CLICommandInvoker;
import hudson.cli.CreateJobCommand;
import hudson.diagnosis.OldDataMonitor;
import hudson.model.AdministrativeMonitor;
import hudson.model.FreeStyleProject;
import hudson.model.Item;
import hudson.tasks.Builder;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.scriptsecurity.sandbox.groovy.SecureGroovyScript;
import org.jenkinsci.plugins.scriptsecurity.scripts.ClasspathEntry;
import org.jenkinsci.plugins.scriptsecurity.scripts.UnapprovedUsageException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;
import org.jvnet.hudson.test.recipes.LocalData;

import java.io.ByteArrayInputStream;
import java.util.Collections;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@Issue("SECURITY-292")
@WithJenkins
class SystemGroovyTest {

    private JenkinsRule j;

    @BeforeEach
    void setUp(JenkinsRule rule) {
        j = rule;
    }

    @Test
    void scriptSecurity() throws Exception {
        String configXml = "<project><builders><" + SystemGroovy.class.getName() + ">" +
            "<scriptSource class=\"" + StringScriptSource.class.getName() + "\"><command>jenkins.model.Jenkins.instance.systemMessage = 'pwned'</command></scriptSource>" +
            "<bindings/><classpath/></" + SystemGroovy.class.getName() + "></builders></project>";
        assertThat(new CLICommandInvoker(j, new CreateJobCommand()).authorizedTo(Jenkins.READ, Item.CREATE).withArgs("attack").withStdin(new ByteArrayInputStream(configXml.getBytes())).invoke(), CLICommandInvoker.Matcher.succeeded());
        j.assertLogContains(UnapprovedUsageException.class.getName(), j.jenkins.getItemByFullName("attack", FreeStyleProject.class).scheduleBuild2(0).get());
        assertNull(j.jenkins.getSystemMessage());
    }

    @LocalData
    @Test
    void upgrade() throws Exception {
        SystemGroovy sg = new SystemGroovy(new StringSystemScriptSource(new SecureGroovyScript("println x", false, null)));
        sg.setBindings("x=33");
        verifyUpgrade(SystemGroovy.class, sg, "string", null);
        verifyUpgrade(SystemGroovy.class, new SystemGroovy(new FileSystemScriptSource("x.groovy")), "ws", null);
        if (!Functions.isWindows()) { // test data includes Unix pathnames
            verifyUpgrade(Groovy.class, new Groovy(new StringScriptSource("println 'hello'"), "(Default)", "", "", "", "-ea", "/tmp/x.jar"), "external", null);
            verifyUpgrade(SystemGroovy.class, new SystemGroovy(new StringSystemScriptSource(new SecureGroovyScript("true", false, Collections.singletonList(new ClasspathEntry("/tmp/x.jar"))))), "jarcp", null);
            verifyUpgrade(SystemGroovy.class, null, "dircp", "directory-based classpath entries not supported in system Groovy script string source");
            verifyUpgrade(SystemGroovy.class, null, "wscp", "classpath no longer supported in conjunction with system Groovy script file source");
        }
    }

    private <T extends Builder> void verifyUpgrade(Class<T> type, T expected, String job, String err) throws Exception {
        FreeStyleProject p = j.jenkins.getItemByFullName(job, FreeStyleProject.class);
        T actual = p.getBuildersList().get(type);
        j.assertEqualDataBoundBeans(expected, actual);
        assert expected == null ^ err == null;
        if (err != null) {
            OldDataMonitor.VersionRange datum = AdministrativeMonitor.all().get(OldDataMonitor.class).getData().get(p);
            assertNotNull(datum);
            assertThat("right message for " + job, datum.extra, containsString(err));
        }
    }

}
