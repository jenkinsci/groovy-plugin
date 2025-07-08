package hudson.plugins.groovy;

import hudson.model.FreeStyleProject;
import org.jenkinsci.plugins.scriptsecurity.scripts.ScriptApproval;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

@WithJenkins
class FileSystemScriptSourceTest {

    private JenkinsRule j;

    @BeforeEach
    void setUp(JenkinsRule rule) {
        j = rule;
    }

    @Test
    void smokes() throws Exception {
        FreeStyleProject p = j.createFreeStyleProject("p");
        ScriptApproval.get().approveSignature("method java.io.PrintStream println java.lang.String"); // TODO add methods like this or `method groovy.lang.Script println java.lang.Object` to generic-whitelist
        j.jenkins.getWorkspaceFor(p).child("x.groovy").write("out.println('ran OK')", null);
        p.getBuildersList().add(new SystemGroovy(new FileSystemScriptSource("x.groovy")));
        j.assertLogContains("ran OK", j.buildAndAssertSuccess(p));
    }

}
