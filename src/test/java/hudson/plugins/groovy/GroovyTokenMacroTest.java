/*
 * The MIT License
 *
 * Copyright (c) Red Hat, Inc.
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

import com.gargoylesoftware.htmlunit.html.HtmlCheckBoxInput;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.FreeStyleProject;
import org.jenkinsci.plugins.tokenmacro.MacroEvaluationException;
import org.jenkinsci.plugins.tokenmacro.TokenMacro;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.TestBuilder;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class GroovyTokenMacroTest {

    public static final String TEST_SCRIPT = "${GROOVY,script = \"return 6 * 7\"}";

    public @Rule JenkinsRule j = new JenkinsRule();

    private void allowTokenMacro() throws Exception {
        HtmlPage page = j.createWebClient().goTo("configure");
        HtmlCheckBoxInput allowMacroCheckBox = page.getElementByName("_.allowMacro");
        allowMacroCheckBox.setChecked(true);
        j.submit(page.getFormByName("config"));
        Groovy.DescriptorImpl descriptor = (Groovy.DescriptorImpl) j.jenkins.getDescriptor(Groovy.class);
        assertTrue(descriptor.getAllowMacro());
    }

    @Test
    public void expand() throws Exception {
        allowTokenMacro();
        FreeStyleProject p = j.createFreeStyleProject();
        p.getBuildersList().add(new MacroTestBuilder("42", TEST_SCRIPT));

        j.buildAndAssertSuccess(p);
    }

    @Test
    public void ignoreExpansionWhenNotAllowed() throws Exception {
        FreeStyleProject p = j.createFreeStyleProject();
        p.getBuildersList().add(new MacroTestBuilder("return 6 * 7", TEST_SCRIPT));

        j.buildAndAssertSuccess(p);
    }

    private static class MacroTestBuilder extends TestBuilder {
        private final String expected;
        private final String stringWithMacro;

        private MacroTestBuilder(String expected, String stringWithMacro) {
            this.expected = expected;
            this.stringWithMacro = stringWithMacro;
        }

        @Override
        public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
            try {
                System.out.println(TokenMacro.all());
                String out = TokenMacro.expand(build, listener, stringWithMacro);
                assertEquals(expected, out);
            } catch (MacroEvaluationException e) {
                e.printStackTrace();
                return false;
            }
            return true;
        }
    }
}
