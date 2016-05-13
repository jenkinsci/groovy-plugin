package hudson.plugins.groovy

import hudson.remoting.DelegatingCallable
import jenkins.model.Jenkins
import org.codehaus.groovy.control.CompilerConfiguration
import org.jenkinsci.remoting.RoleChecker

class SlaveTask implements DelegatingCallable<Void, Exception> {

    private String script
    private String bindings
    private def map = [:]

    SlaveTask(String script, String bindings) {
        this.script = script
        this.bindings = bindings
    }

    def putProp = { k, v -> map.put(k, v) }

    def pupPropMap = { m -> map.putAll(m) }

    @Override
    ClassLoader getClassLoader() {
        ClassLoader cl = Jenkins.instance.pluginManager.uberClassLoader
        if (null == cl) {
            cl = Thread.currentThread().contextClassLoader
        }
        return cl
    }

    @Override
    Void call() throws Exception {
        def config = new CompilerConfiguration()
        def binding = new Binding(Utils.parseProperties(bindings))
        map.each { k, v -> binding.setProperty(k, v)}
        def shell = new GroovyShell(getClassLoader(), binding, config)
        shell.evaluate(script) as Void
    }

    @Override
    void checkRoles(RoleChecker checker) throws SecurityException {
    }
}
