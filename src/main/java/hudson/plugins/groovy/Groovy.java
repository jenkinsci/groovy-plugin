package hudson.plugins.groovy;

import hudson.CopyOnWrite;
import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Util;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Computer;
import hudson.model.ParametersAction;
import hudson.util.VariableResolver;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.StringTokenizer;

/**
 * A Builder for Groovy scripts.
 *
 * @author dvrzalik, vjuranek
 */
public class Groovy extends AbstractGroovy {

    private String groovyName;
    private String parameters;
    private String scriptParameters;
    private String properties; // -D properties
    private String javaOpts;   //sometimes other options than -D are needed to be set up, like -XX. It can be done here.
                               // Properties are kept for user, who don't want bother with -D and backward compatibility

    private String classPath;  //for user convenience when added more item into class path not have to deal with path separator

    @DataBoundConstructor
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
            List<String> cmd = buildCommandLine(build,listener,script,launcher.isUnix());

            int result;
            try {
                Map<String,String> envVars = build.getEnvironment(listener);
                hudson.plugins.groovy.GroovyInstallation installation = getGroovy();
                if(installation != null) {
                    installation = installation.forNode(Computer.currentComputer().getNode(), listener);
                    envVars.put("GROOVY_HOME", installation.getHome());
                }

                for(Map.Entry<String,String> e : build.getBuildVariables().entrySet()){
                    envVars.put(e.getKey(),e.getValue());
                }

                String origJavaOpts = build.getBuildVariables().get("JAVA_OPTS");
                StringBuilder javaOpts = new StringBuilder((origJavaOpts != null) ? origJavaOpts : "");
                //Add javaOpts at the end
                if(this.javaOpts != null) //backward compatibility
                    javaOpts.append(' ').append(this.javaOpts);
                envVars.put("JAVA_OPTS", javaOpts.toString());

                envVars.put("$PATH_SEPARATOR",":::"); //TODO why??

                result = launcher.launch().cmds(cmd.toArray(new String[] {})).envs(envVars).stdout(listener).pwd(ws).join();
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

    @Extension
    public static final class DescriptorImpl extends AbstractGroovyDescriptor {

        private boolean allowMacro;

        @CopyOnWrite
        private volatile GroovyInstallation[] installations = new GroovyInstallation[0];

        @CopyOnWrite
        private volatile List<hudson.plugins.groovy.GroovyInstallation> installations2 = new ArrayList<hudson.plugins.groovy.GroovyInstallation>();

        public DescriptorImpl() {
            super(Groovy.class);
            load();
        }

        public Object readResolve(){
            // convert to new installation and drop the old one
            if(installations.length > 0){
                for(GroovyInstallation inst: installations){
                    hudson.plugins.groovy.GroovyInstallation inst2 = new hudson.plugins.groovy.GroovyInstallation(inst.getName(),inst.getHome(),null);
                    installations2.add(inst2);
                }
                installations = null;
            }
            return this;
        }

        public boolean getAllowMacro(){
            return allowMacro;
        }

        @Override
        public String getDisplayName() {
            return "Execute Groovy script";
        }

        @Override
        @SuppressWarnings("rawtypes")
        public boolean isApplicable(Class<? extends AbstractProject> jobType){
            return true;
        }

        @Override
        public String getHelpFile() {
            return "/plugin/groovy/project-config.html";
        }

        public hudson.plugins.groovy.GroovyInstallation[] getInstallations() {
            hudson.plugins.groovy.GroovyInstallation[] installs = new hudson.plugins.groovy.GroovyInstallation[installations2.size()];
            return installations2.toArray(installs);
        }

        public static hudson.plugins.groovy.GroovyInstallation getGroovy(String groovyName) {
            for( hudson.plugins.groovy.GroovyInstallation i : ((DescriptorImpl) Jenkins.getInstance().getDescriptor(Groovy.class)).getInstallations()) {
                if(groovyName!=null && i.getName().equals(groovyName))
              return i;
            }
          return null;
        }

        public void setInstallations(hudson.plugins.groovy.GroovyInstallation... installations) {
            //this.installations = installations;
            List<hudson.plugins.groovy.GroovyInstallation> installations2 = new ArrayList<hudson.plugins.groovy.GroovyInstallation>();
            for(hudson.plugins.groovy.GroovyInstallation install: installations){
                installations2.add(install);
            }
            this.installations2 = installations2;
            save();
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject json) throws hudson.model.Descriptor.FormException {
            allowMacro = json.getBoolean("allowMacro");
            save();
            return true;
        }
    }

    // Keep it for backward compatibility
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

        private static final long serialVersionUID = 1L;
    }


    protected hudson.plugins.groovy.GroovyInstallation getGroovy() {
        return DescriptorImpl.getGroovy(groovyName);
    }

    //backward compatibility, default is Unix
    @Deprecated
    protected List<String> buildCommandLine(AbstractBuild<?,?> build,FilePath script) throws IOException, InterruptedException  {
    	return buildCommandLine(build, null, script, true);
    }

    protected List<String> buildCommandLine(AbstractBuild<?,?> build, BuildListener listener, FilePath script, boolean isOnUnix) throws IOException, InterruptedException  {
        ArrayList<String> list = new ArrayList<String>();

        //prepare variable resolver - more efficient than calling env.expand(s)
        EnvVars env = build.getEnvironment(listener);
        env.overrideAll(build.getBuildVariables());
        VariableResolver<String> vr = new VariableResolver.ByMap<String>(env);


        String cmd = "groovy";//last hope in case of missing or not selected installation

        hudson.plugins.groovy.GroovyInstallation installation = getGroovy();
        if(installation != null) {
            installation = installation.forNode(Computer.currentComputer().getNode(), listener);
            installation = installation.forEnvironment(env);
            cmd = installation.getExecutable(script.getChannel());
            //some misconfiguration, reverting back to default groovy cmd
            if(null == cmd){
                cmd = "groovy";
                listener.getLogger().println("[GROOVY WARNING] Groovy executable is NULL, please check your Groovy configuration, trying fallback 'groovy' instead.");
            }
        }
        list.add(cmd);

        //Add class path
        if(StringUtils.isNotBlank(classPath)) {
            String pathSeparator = isOnUnix ? ":" : ";";
            StringTokenizer tokens = new StringTokenizer(classPath);
            list.add("-cp");
            // class path has to be one item, otherwise spaces are add around class path separator and build will fail
            StringBuilder sb = new StringBuilder();
            sb.append(Util.replaceMacro(tokens.nextToken(),vr));
            while(tokens.hasMoreTokens()) {
                sb.append(pathSeparator);
                sb.append(Util.replaceMacro(tokens.nextToken(),vr));
            }
            list.add(sb.toString());
        }

        //Add java properties
        if(StringUtils.isNotBlank(properties)) {
            for (Entry<Object, Object> entry : Utils.parseProperties(properties).entrySet()) {
                list.add("-D" + entry.getKey() + "=" + entry.getValue());
            }
        }

        //Add groovy parameters
        if(StringUtils.isNotBlank(parameters)) {
            String[] args = parseParams(parameters);
            for(String arg : args) {
                list.add(Util.replaceMacro(arg, vr));
            }
        }
        list.add(script.getRemote());

        //Add script parameters
        if(StringUtils.isNotBlank(scriptParameters)) {
            String[] params = parseParams(scriptParameters);
            ParametersAction parameters = build.getAction(ParametersAction.class);
            for(String param : params) {
            	//first replace parameter from parameterized build
            	if (parameters != null) {
                    param = parameters.substitute(build, param);
                }
            	//then replace evn vars
            	param = Util.replaceMacro(param,vr);
                list.add(param);
            }
        }

        return list;

    }

    /**
     * Parse parameters to be passed as arguments to the groovy binary
     *
     */
    private String[] parseParams(String line) {
        //JENKINS-24870 CommandLine.getExecutable tries to fix file separators, so if the first param contains slashes, it can cause problems
        //Adding some placeholder instead of executable
        CommandLine cmdLine = CommandLine.parse("executable_placeholder " + line);
        String[] parsedArgs = cmdLine.getArguments();
        String[] args = new String[parsedArgs.length];
        if(parsedArgs.length > 0) {
            System.arraycopy(parsedArgs, 0, args, 0, parsedArgs.length);
        }
        return args;
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
