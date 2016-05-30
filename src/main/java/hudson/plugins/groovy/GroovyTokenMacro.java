/*
 * The MIT License
 *
 * Copyright 2011 Nigel Magnay / NiRiMa
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

import hudson.Extension;
import hudson.model.BuildListener;
import hudson.model.TaskListener;
import jenkins.model.Jenkins;
import hudson.model.AbstractBuild;

import java.io.IOException;

import org.jenkinsci.plugins.tokenmacro.DataBoundTokenMacro;
import org.jenkinsci.plugins.tokenmacro.MacroEvaluationException;

/**
 * {@code GROOVY} token that evaluates groovy expressions.
 * 
 * @author Nigel Magnay
 */
@Extension(optional=true)
public class GroovyTokenMacro extends DataBoundTokenMacro {
	/**
	 * Script to use.
	 */
	@Parameter
	public String script;

	@Override
	public boolean acceptsMacroName(String macroName) {
		return macroName.equals("GROOVY");
	}

	@Override
	public String evaluate(
		final AbstractBuild<?, ?> context,
		final TaskListener listener,
		final String macroName
	) throws MacroEvaluationException, IOException, InterruptedException {
		Groovy.DescriptorImpl decs =
			(Groovy.DescriptorImpl) Jenkins.getInstance().getDescriptorOrDie(Groovy.class);

		if (decs.getAllowMacro()) {
			StringScriptSource scriptSource = new StringScriptSource(script);

			SystemGroovy systemGroovy = new SystemGroovy(scriptSource, "", null);
			Object output = systemGroovy.run(context, (BuildListener) listener, null);
			
			return output != null ? output.toString() : "";
		} else {
			return script;
		}
	}

}
