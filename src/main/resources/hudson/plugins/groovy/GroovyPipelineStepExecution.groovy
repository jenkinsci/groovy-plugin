package hudson.plugins.groovy

import java.util.concurrent.atomic.AtomicReference
import org.jenkinsci.plugins.workflow.cps.steps.ingroovy.GroovyStepExecution

public class GroovyPipelineStepExecution extends GroovyStepExecution {

    private AtomicReference<String> loc = new AtomicReference<>()

    def call() {
        def unix = isUnix()
        def exe = step.tool != null ? (unix ? "${tool step.tool}/bin/groovy" : "${tool step.tool}\\bin\\groovy") : 'groovy'
        def args = prepArgs()
        try {
            unix ? sh("'${exe}' ${args}") : bat("\"${exe}\" ${args}")
            returnValue()
        } finally {
            if (loc.get() != null) {
                dir(loc.get()) {
                    deleteDir()
                }
            }
        }
    }

    @NonCPS
    private String prepArgs() {
        step.prepArgs(context, loc)
    }

    @NonCPS
    private Object returnValue() {
        step.returnValue(context, loc.get())
    }

}
