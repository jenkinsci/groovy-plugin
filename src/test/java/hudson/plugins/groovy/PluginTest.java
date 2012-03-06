package hudson.plugins.groovy;

import org.jvnet.hudson.test.HudsonTestCase;

import com.gargoylesoftware.htmlunit.ElementNotFoundException;
import com.gargoylesoftware.htmlunit.html.HtmlCheckBoxInput;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

public class PluginTest extends HudsonTestCase {

    public void testAllowTokenMacro() throws ElementNotFoundException, Exception {
        HtmlPage page = createWebClient().goTo("configure");
        HtmlCheckBoxInput allowMacroCheckBox = page.getElementByName("allowMacro");
        allowMacroCheckBox.setChecked(true);
        submit(page.getFormByName("config"));
        assertTrue(Groovy.DESCRIPTOR.getAllowMacro());
    }
}
