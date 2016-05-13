package hudson.plugins.groovy;

import hudson.Util;
import hudson.util.VariableResolver;

import javax.annotation.Nonnull;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;

public final class Utils {

    private Utils() { }

    public static @Nonnull
    Properties parseProperties(final String properties) throws IOException {
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

    public static List<String> parseClassPath(String classPath, VariableResolver<String> vr) {
        List<String> cp = new ArrayList<String>();
        StringTokenizer tokens = new StringTokenizer(classPath);
        while(tokens.hasMoreTokens()) {
            cp.add(Util.replaceMacro(tokens.nextToken(), vr));
        }
        return cp;
    }
}
