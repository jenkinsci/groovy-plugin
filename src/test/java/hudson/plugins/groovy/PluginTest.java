package hudson.plugins.groovy;

import static org.junit.Assert.assertTrue;

import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import com.gargoylesoftware.htmlunit.ElementNotFoundException;
import com.gargoylesoftware.htmlunit.html.HtmlCheckBoxInput;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

public class PluginTest {

    @Rule
    public JenkinsRule j = new JenkinsRule();
    
    @Test
    public void testAllowTokenMacro() throws ElementNotFoundException, Exception {
        HtmlPage page = j.createWebClient().goTo("configure");
        HtmlCheckBoxInput allowMacroCheckBox = page.getElementByName("allowMacro");
        allowMacroCheckBox.setChecked(true);
        j.submit(page.getFormByName("config"));
        assertTrue(Groovy.DESCRIPTOR.getAllowMacro());
    }
}
