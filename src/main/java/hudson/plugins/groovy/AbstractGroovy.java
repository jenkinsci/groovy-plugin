package hudson.plugins.groovy;

import hudson.DescriptorExtensionList;
import hudson.model.Descriptor;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.Nonnull;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.kohsuke.stapler.StaplerRequest;

/**
 * Shared functionality for Groovy builders
 * (nothing but script sources at the moment)
 *
 * @author dvrzalik
 */
public abstract class AbstractGroovy extends Builder {

    protected ScriptSource scriptSource;

    public AbstractGroovy(ScriptSource scriptSource) {
        this.scriptSource = scriptSource;
    }

    public static abstract class AbstractGroovyDescriptor extends BuildStepDescriptor<Builder> {

        public AbstractGroovyDescriptor(Class<? extends Builder> clazz) {
            super(clazz);
        }

        /**
         * Extracts ScriptSource from given form data.
         */
        protected ScriptSource getScriptSource(
            final StaplerRequest req,
            final JSONObject data
        ) throws FormException {
            Object scriptSourceObject = data.get("scriptSource");

            if (scriptSourceObject instanceof JSONArray) {
                // Dunno why this happens. Let's fix the JSON object so that
                // newInstanceFromRadioList() doesn't go mad.

                JSONArray scriptSourceJSONArray = (JSONArray) scriptSourceObject;
                JSONObject scriptSourceJSONObject = new JSONObject();
                Object nestedObject = scriptSourceJSONArray.get(1);

                if (nestedObject instanceof JSONObject) {
                    // command/file path
                    scriptSourceJSONObject.putAll((JSONObject) nestedObject);

                    // selected radio button index
                    scriptSourceJSONObject.put("value", scriptSourceJSONArray.get(0));

                    data.put("scriptSource", scriptSourceJSONObject);
                }
            }

            return ScriptSource.all().newInstanceFromRadioList(data, "scriptSource");
        }

        // shortcut
        public static DescriptorExtensionList<ScriptSource, Descriptor<ScriptSource>> getScriptSources() {
            return ScriptSource.all();
        }

        // Used for grouping radio buttons together
        private AtomicInteger instanceCounter = new AtomicInteger(0);

        public int nextInstanceID() {
            return instanceCounter.incrementAndGet();
        }
    }

    public ScriptSource getScriptSource() {
        return scriptSource;
    }


    public static @Nonnull Properties parseProperties(final String properties) throws IOException {
        Properties props = new Properties();

        if (properties != null) {
            try {
                props.load(new StringReader(properties));
            } catch (NoSuchMethodError err) {
                props.load(new ByteArrayInputStream(properties.getBytes()));
            }
        }
        return props;
    }
}
