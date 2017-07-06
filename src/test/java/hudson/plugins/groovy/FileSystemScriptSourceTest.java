package hudson.plugins.groovy;

import hudson.model.FreeStyleProject;
import org.jenkinsci.plugins.scriptsecurity.scripts.ScriptApproval;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.Rule;
import org.jvnet.hudson.test.BuildWatcher;
import org.jvnet.hudson.test.JenkinsRule;

public class FileSystemScriptSourceTest {

    @ClassRule
    public static BuildWatcher buildWatcher = new BuildWatcher();

    @Rule
    public JenkinsRule r = new JenkinsRule();

    @Test
    public void smokes() throws Exception {
        FreeStyleProject p = r.createFreeStyleProject("p");
        ScriptApproval.get().approveSignature("method java.io.PrintStream println java.lang.String"); // TODO add methods like this or `method groovy.lang.Script println java.lang.Object` to generic-whitelist
        r.jenkins.getWorkspaceFor(p).child("x.groovy").write("out.println('ran OK')", null);
        p.getBuildersList().add(new SystemGroovy(new FileSystemScriptSource("x.groovy")));
        r.assertLogContains("ran OK", r.buildAndAssertSuccess(p));
    }

}
