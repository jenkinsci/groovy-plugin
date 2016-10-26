package hudson.plugins.groovy

import org.jenkinsci.plugins.workflow.cps.steps.ingroovy.GroovyStepExecution

public class GroovyPipelineStepExecution extends GroovyStepExecution {

    def call() {
        def unix = isUnix()
        def exe = step.tool != null ? (unix ? "${tool step.tool}/bin/groovy" : "${tool step.tool}\\bin\\groovy") : 'groovy'
        unix ? sh("'${exe}' ${step.args}") : bat("\"${exe}\" ${step.args}")
    }

}
