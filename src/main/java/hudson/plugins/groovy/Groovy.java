package hudson.plugins.groovy;

import hudson.CopyOnWrite;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Launcher.LocalLauncher;
import hudson.Util;
import hudson.EnvVars;
import hudson.Extension;
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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletException;
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
    private String properties; // -D properties
    private String javaOpts;   //sometimes other options than -D are needed to be set up, like -XX. It can be done here. 
    						   // Properties are kept for user, who don't want bother with -D and backward compatibility 

    private String classPath;  //for user convenience when added more item into class path not have to deal with path separator
    
    public Groovy(ScriptSource scriptSource, String groovyName, String parameters, 
            String scriptParameters, String properties, String javaOpts, String classPath) {
        super(scriptSource);
        this.groovyName = groovyName;
        this.parameters = parameters;
        this.scriptParameters = scriptParameters;
        this.properties = properties;
        this.javaOpts = javaOpts;
        this.classPath = classPath;
    }


    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {

        if (scriptSource == null) {
            listener.fatalError("There is no script configured for this builder");
            return false;
        }

        FilePath ws = build.getWorkspace();
        FilePath script = null;
        try {
            script = scriptSource.getScriptFile(ws,build,listener);
        } catch (IOException e) {
            Util.displayIOException(e, listener);
            e.printStackTrace(listener.fatalError("Unable to produce a script file"));
            return false;
        }
        try {
            String[] cmd = buildCommandLine(script,launcher.isUnix());

            int result;
            try {
                Map<String,String> envVars = build.getEnvironment(listener);
                GroovyInstallation installation = getGroovy();
                if(installation != null) {
                    envVars.put("GROOVY_HOME", installation.getHome());
                }

                for(Map.Entry<String,String> e : build.getBuildVariables().entrySet()){
                    envVars.put(e.getKey(),e.getValue());
                    System.out.println("key:value: " + e.getKey() + " " + e.getValue());
                }

                if(properties != null) {
                    String origJavaOpts = build.getBuildVariables().get("JAVA_OPTS");
                    StringBuffer javaOpts = new StringBuffer((origJavaOpts != null) ? origJavaOpts : "");
                    Properties props = parseProperties(properties);

                    for (Entry<Object,Object> entry : props.entrySet()) {
                        javaOpts.append(" -D" + entry.getKey() + "=" + entry.getValue());
                    }

                    //Add javaOpts at the end
                    javaOpts.append(" " + this.javaOpts);
                    envVars.put("JAVA_OPTS", javaOpts.toString());
                 }
                
                envVars.put("$PATH_SEPARATOR",":::");
                

                result = launcher.launch().cmds(cmd).envs(envVars).stdout(listener).pwd(ws).join();
            } catch (IOException e) {
                Util.displayIOException(e,listener);
                e.printStackTrace( listener.fatalError("command execution failed") );
                result = -1;
            }
            return result==0;
        } finally {
            try {
                if((scriptSource instanceof StringScriptSource) && (script!=null)){
                	script.delete();
                }
            } catch (IOException e) {
                Util.displayIOException(e,listener);
                e.printStackTrace( listener.fatalError("Unable to delete script file "+script) );
            }
        }
    }

    @Override
    public Descriptor<Builder> getDescriptor() {
        return DESCRIPTOR;
    }

    @Extension
    public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();

    public static final class DescriptorImpl extends AbstractGroovyDescriptor {

    	private boolean allowMacro;
    	
        @CopyOnWrite
        private volatile GroovyInstallation[] installations = new GroovyInstallation[0];

        DescriptorImpl() {
            super(Groovy.class);
            load();
        }
        
        public boolean getAllowMacro(){
        	return allowMacro;
        }

        public String getDisplayName() {
            return "Execute Groovy script";
        }

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> jobType){
        	return true;
        }
        
        @Override
        public String getHelpFile() {
            return "/plugin/groovy/project-config.html";
        }

        public GroovyInstallation[] getInstallations() {
            return installations;
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject formData) {
            try {
                installations = req.bindJSONToList(GroovyInstallation.class, req.getSubmittedForm().get("groovy")).toArray(new GroovyInstallation[0]);
                allowMacro = req.getSubmittedForm().getBoolean("allowMacro");
                save();
                return true;
            } catch (ServletException ex) {
                Logger.getLogger(Groovy.class.getName()).log(Level.SEVERE, null, ex);
                return false;
            }
        }

        @Override
        public Builder newInstance(StaplerRequest req, JSONObject data) throws FormException {
            ScriptSource source = getScriptSource(req, data);
            String instName = data.getString("groovyName");
            String params = data.getString("parameters");
            String classPath = data.getString("classPath").trim();
            String scriptParams = data.getString("scriptParameters");
            String props = data.getString("properties");
            String javaOpts = data.getString("javaOpts");
            
            return new Groovy(source, instName, params, scriptParams, props, javaOpts, classPath);
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
                    } else {
                        throw new FileNotFoundException(exe.getPath() + " doesn't exist, please check your Groovy installation");
                    }
                }
            });
        }

        private File getExeFile(String execName) {
            String groovyHome = Util.replaceMacro(getHome(),EnvVars.masterEnvVars);
            File binDir = new File(groovyHome, "bin/");
            if (File.separatorChar == '\\') {                
                if(new File(binDir, execName + ".exe").exists()) {
                    execName += ".exe";
                } else {
                    execName += ".bat";
                }
            }
            return new File(binDir, execName);            
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

    //backward compatibility, default is Unix
    protected String[] buildCommandLine(FilePath script) throws IOException, InterruptedException  {
    	return buildCommandLine(script, true);
    }
    
    protected String[] buildCommandLine(FilePath script, boolean isOnUnix) throws IOException, InterruptedException  {
        ArrayList<String> list = new ArrayList<String>();

        String cmd = "groovy";//last hope in case of missing or not selected installation

        GroovyInstallation installation = getGroovy();
        if(installation != null) {
            cmd = installation.getExecutable(script.getChannel());
        }
        list.add(cmd);

        //Add class path
        if(classPath != null && classPath != ""){
        	String pathSeparator = isOnUnix ? ":" : ";";
        	StringTokenizer tokens = new StringTokenizer(classPath);
        	list.add("-cp");
        	// class path has to be one item, otherwise spaces are add around class path separator and build will fail
        	StringBuilder sb = new StringBuilder();  
        	sb.append(tokens.nextToken());
            while(tokens.hasMoreTokens()) {
            	sb.append(pathSeparator);
                sb.append(tokens.nextToken());
            }
            list.add(sb.toString());
        }
        
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
    
    public String getJavaOpts(){
    	return javaOpts;
    }
    
    public String getClassPath(){
    	return classPath;
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
