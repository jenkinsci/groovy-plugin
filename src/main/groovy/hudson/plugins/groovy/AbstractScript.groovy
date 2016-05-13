package hudson.plugins.groovy

import hudson.EnvVars
import hudson.model.Computer
import hudson.remoting.VirtualChannel

abstract class AbstractScript extends Script {

    def runScriptOnChannel(VirtualChannel channel, String script) {
        SlaveTask task = new SlaveTask(script)
        EnvVars envVars = build.getEnvironment(listener)
        envVars.overrideAll(build.getBuildVariables())
        task.putPropMap(envVars)
        channel.call(task)
    }

    def runScriptOnCurrentNode(String script) {
        Computer computer = build.getBuiltOn().toComputer()
        runScriptOnChannel(computer.getChannel(), script)
    }

    def runScriptOnNode(String name, String script) {
        if (null != jenkins.getNode(name)) {
            Computer computer = jenkins.getNode(name).toComputer()
            runScriptOnChannel(computer.getChannel(), script)
        } else {
            out.println 'Failed to run script on node $name'
        }
    }


}
