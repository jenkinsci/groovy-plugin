/*
 * The MIT License
 *
 * Copyright (c) 2019, CloudBees, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package hudson.plugins.groovy;

import hudson.util.FormValidation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertNull;

@WithJenkins
class StringScriptSourceTest {

    private JenkinsRule j;

    @BeforeEach
    void setUp(JenkinsRule rule) {
        j = rule;
    }

    @Issue("SECURITY-1293")
    @Test
    void blockASTTest() {
        StringScriptSource.DescriptorImpl d = j.jenkins.getDescriptorByType(StringScriptSource.DescriptorImpl.class);
        assertThat(d.doCheckScript("""
                import groovy.transform.*
                import jenkins.model.Jenkins
                import hudson.model.FreeStyleProject
                @ASTTest(value={ assert Jenkins.getInstance().createProject(FreeStyleProject.class, "should-not-exist") })
                @Field int x
                echo 'hello'
                """).toString(), containsString("Annotation ASTTest cannot be used in the sandbox"));

        assertNull(j.jenkins.getItem("should-not-exist"));
    }

    @Issue("SECURITY-1293")
    @Test
    void blockGrab() {
        StringScriptSource.DescriptorImpl d = j.jenkins.getDescriptorByType(StringScriptSource.DescriptorImpl.class);
        assertThat(d.doCheckScript("@Grab(group='foo', module='bar', version='1.0')\ndef foo\n").toString(),
                containsString("Annotation Grab cannot be used in the sandbox"));
    }

    @Issue("SECURITY-1338")
    @Test
    void doNotExecuteConstructors() {
        StringScriptSource.DescriptorImpl d = j.jenkins.getDescriptorByType(StringScriptSource.DescriptorImpl.class);
        assertThat(d.doCheckScript("""
                class DoNotRunConstructor {
                  static void main(String[] args) {}
                  DoNotRunConstructor() {
                    assert jenkins.model.Jenkins.instance.createProject(hudson.model.FreeStyleProject, 'should-not-exist')
                  }
                }
                """).kind, equalTo(FormValidation.Kind.OK)); // Compilation ends before the constructor is invoked.
        assertNull(j.jenkins.getItem("should-not-exist"));
    }
}
