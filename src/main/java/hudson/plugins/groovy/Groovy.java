package hudson.plugins.groovy;

import hudson.CopyOnWrite;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Launcher.LocalLauncher;
import hudson.StructuredForm;
import hudson.Util;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Descriptor;
import hudson.remoting.Callable;
import hudson.remoting.VirtualChannel;
import hudson.tasks.Builder;
import hudson.util.NullStream;
import hudson.util.StreamTaskListener;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.StringTokenizer;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

/**
 * A Builder for Groovy scripts.
 *
 * @author dvrzalik
 */
public class Groovy extends AbstractGroovy {
    
    private String groovyName;
    private String parameters;
    private String scriptParameters;
    private String properties;

    public Groovy(ScriptSource scriptSource, String groovyName, String parameters, 
            String scriptParameters, String properties) {
        super(scriptSource);
        this.groovyName = groovyName;
        this.parameters = parameters;
        this.scriptParameters = scriptParameters;
        this.properties = properties;
    }


    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {

        if (scriptSource == null) {
            listener.fatalError("There is no script configured for this builder");
            return false;
        }

        AbstractProject proj = build.getProject();
        FilePath ws = proj.getWorkspace();
        FilePath script = null;
        try {
            script = scriptSource.getScriptFile(ws);
        } catch (IOException e) {
            Util.displayIOException(e, listener);
            e.printStackTrace(listener.fatalError("Unable to produce a script file"));
            return false;
        }
        try {
            String[] cmd = buildCommandLine(script);

            int result;
            try {
                Map<String,String> envVars = build.getEnvVars();
                GroovyInstallation installation = getGroovy();
                if(installation != null) {
                    envVars.put("GROOVY_HOME", installation.getHome());
                }

                for(Map.Entry<String,String> e : build.getBuildVariables().entrySet())
                    envVars.put(e.getKey(),e.getValue());

                //Pass properties as JAVA_OPTS
                if(properties != null) {
                    String origJavaOpts = build.getBuildVariables().get("JAVA_OPTS");
                    StringBuffer javaOpts = new StringBuffer((origJavaOpts != null) ? origJavaOpts : "");
                    Properties props = parseProperties(properties);

                    for (Entry<Object,Object> entry : props.entrySet()) {
                        javaOpts.append(" -D" + entry.getKey() + "=" + entry.getValue());
                    }

                    envVars.put("JAVA_OPTS", javaOpts.toString());
                 }

                result = launcher.launch(cmd,envVars,listener.getLogger(),ws).join();
            } catch (IOException e) {
                Util.displayIOException(e,listener);
                e.printStackTrace( listener.fatalError("command execution failed") );
                result = -1;
            }
            return result==0;
        } finally {
            try {
                if((type == BuilderType.COMMAND) && (script!=null))
                script.delete();
            } catch (IOException e) {
                Util.displayIOException(e,listener);
                e.printStackTrace( listener.fatalError("Unable to delete script file "+script) );
            }
        }
    }

    public Descriptor<Builder> getDescriptor() {
        return DESCRIPTOR;
    }


    public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();

    public static final class DescriptorImpl extends AbstractGroovyDescriptor {

        @CopyOnWrite
        private volatile GroovyInstallation[] installations = new GroovyInstallation[0];

        DescriptorImpl() {
            super(Groovy.class);
            load();
        }

        public String getDisplayName() {
            return "Execute Groovy script";
        }

        @Override
        public String getHelpFile() {
            return "/plugin/groovy/project-config.html";
        }

        public GroovyInstallation[] getInstallations() {
            return installations;
        }

        @Override
        public boolean configure(StaplerRequest req) {
            installations = req.bindJSONToList(
                    GroovyInstallation.class, StructuredForm.get(req).get("groovy")).toArray(new GroovyInstallation[0]);
            save();
            return true;
        }

        @Override
        public Builder newInstance(StaplerRequest req, JSONObject data) throws FormException {
            ScriptSource source = getScriptSource(req, data);
            String instName = data.getString("groovyName");
            String params = data.getString("parameters");
            String scriptParams = data.getString("scriptParameters");
            String props = data.getString("properties");
            
            return new Groovy(source, instName, params, scriptParams, props);
        }

        public static GroovyInstallation getGroovy(String groovyName) {
            for( GroovyInstallation i : DESCRIPTOR.getInstallations() ) {
                if(groovyName!=null && i.getName().equals(groovyName))
              return i;
            }
          return null;
        }
    }

    public static final class GroovyInstallation implements Serializable {

        private final String name;
        private final String home;

        @DataBoundConstructor
        public GroovyInstallation(String name, String home) {
            this.name = name;
            this.home = home;
        }

        /**
         * install directory.
         */
        public String getHome() {
            return home;
        }

        /**
         * Human readable display name.
         */
        public String getName() {
            return name;
        }

        /**
         * Gets the executable path of this groovy installation on the given target system.
         */
        public String getExecutable(VirtualChannel channel) throws IOException, InterruptedException {
            return channel.call(new Callable<String, IOException>() {

                public String call() throws IOException {
                    File exe = getExeFile("groovy");
                    if (exe.exists()) {
                        return exe.getPath();
                    }
                    return null;
                }
            });
        }

        private File getExeFile(String execName) {
            if (File.separatorChar == '\\') {
                execName += ".exe";
            }
            return new File(getHome(), "bin/" + execName);
        }

        /**
         * Returns true if the executable exists.
         */
        public boolean exists() {
            try {
                return getExecutable(new LocalLauncher(new StreamTaskListener(new NullStream())).getChannel()) != null;
            } catch (IOException e) {
                return false;
            } catch (InterruptedException e) {
                return false;
            }
        }

        private static final long serialVersionUID = 1L;
    }



    protected GroovyInstallation getGroovy() {
        return DescriptorImpl.getGroovy(groovyName);
    }

    protected String[] buildCommandLine(FilePath script) throws IOException, InterruptedException  {
        ArrayList<String> list = new ArrayList<String>();

        String cmd = "groovy";//last hope in case of missing or not selected installation

        GroovyInstallation installation = getGroovy();
        if(installation != null) {
            cmd = installation.getExecutable(script.getChannel());
        }
        list.add(cmd);

        //Add groovy parameters
        if(parameters != null) {
            StringTokenizer tokens = new StringTokenizer(parameters);
            while(tokens.hasMoreTokens()) {
                list.add(tokens.nextToken());
            }
        }

        list.add(script.getRemote());

        //Add script parameters
        if(scriptParameters != null) {
            StringTokenizer tokens = new StringTokenizer(scriptParameters);
            while(tokens.hasMoreTokens()) {
                list.add(tokens.nextToken());
            }
        }

        return list.toArray(new String[] {});

    }


    public String getCommand() {
        return command;
    }

    public String getScriptFile() {
        return scriptFile;
    }

    public String getGroovyName() {
        return groovyName;
    }

    public BuilderType getType() {
        return type;
    }

    public String getParameters() {
        return parameters;
    }

    public String getScriptParameters() {
        return scriptParameters;
    }

    public String getProperties() {
        return properties;
    }
    
    
    //---- Backward compatibility -------- //
    
    public enum BuilderType { COMMAND,FILE }
    
    private BuilderType type;
    private String command;
    private String scriptFile;
    
    private Object readResolve() {
        if (type != null) {
            switch (type) {
                case COMMAND:
                    scriptSource = new StringScriptSource(command);
                    break;
                case FILE:
                    scriptSource = new FileScriptSource(scriptFile);
                    break;
            }
        }
        
        type = null;
        command = null;
        scriptFile = null;
        
        return this;
    }

}
