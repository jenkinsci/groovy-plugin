# Groovy plugin for Jenkins
[![Jenkins Plugin](https://img.shields.io/jenkins/plugin/v/groovy.svg)](https://plugins.jenkins.io/groovy)
[![Jenkins Plugin Installs](https://img.shields.io/jenkins/plugin/i/groovy.svg?color=blue)](https://plugins.jenkins.io/groovy)


This plugin adds the ability to directly execute Groovy code.

## Configuration

To configure available Groovy installation on your system, go to Jenkins
configuration page, find section 'Groovy' and fill the form as shown
bellow.

  
![](/docs/images/snapshot6.png)

If you don't configure any Groovy installation and select `(Default)`
option in a job, the plugin will fallback into calling just the `groovy`
command, assuming you have `groovy` binary on the default path on given
machine.

## Usage

To create Groovy-based project, add new free-style project and select
"Execute Groovy script" in the Build section, select previously
configured Groovy installation and then type your command, or specify
your script file name. In the second case path taken is relatively from
the project workspace directory.

Jenkins 1.x:

![Jenkins 1.x](/docs/images/snapshot5.png)

  

Jenkins 2.x:

![Jenkins 2.x](/docs/images/image2018-9-13_11-52-26.png)

The plugin also adds the functionality of the [Script
Console](https://www.jenkins.io/doc/book/managing/script-console/)
to the project configuration page.

You can schedule your system management script...

  
![](/docs/images/image2018-9-13_15-15-45.png)

...and then observe progress in the build log.

  
![](/docs/images/image2018-9-13_15-14-57.png)

### Groovy Script vs System Groovy Script

The plain "Groovy Script" is run in a forked JVM, on the slave where the
build is run. It's the basically the same as running the "groovy"
command and pass in the script.

The system Groovy script on the other hand runs inside the Jenkins
master's JVM. Thus it will have access to all the internal objects of
Jenkins, so you can use this to alter the state of Jenkins. It is
similar to the [Jenkins Script
Console](https://www.jenkins.io/doc/book/managing/script-console/)
functionality.

### Security

System groovy jobs has access to whole Jenkins, therefore only users
with admin rights can add system Groovy build step and configure the
system Groovy script. Permissions are not checked when the build is
triggered (i.e. also uses without admin rights can also run the script).
The idea is to allow users run some well defined (defined by admin)
system tasks when they need it (e.g. put slave offline/online, when user
wants to start some debugging on slave). To have Jenkins instance
secure, the support for Token macro plugin has to be switched off, see
section below.

### Token macro plugin support

Groovy plugin provides support for  [Token Macro
Plugin](https://plugins.jenkins.io/token-macro/).
Expression is *GROOVY* with parameter *script*:

    ${GROOVY,script = "return hudson.model.Hudson.instance.pluginManager.plugins"}

By default, the support for token macro pressing is switched off and has
to be switch on in global config page.

If token macro processing via Token Macro Plugin is allowed, the
evaluation of macro is done in System Groovy, therefore any user can run
arbitrary system script, regardless he has administer permission!

## Examples

### Retrieving parameters and triggering another build

Execute a system Groovy script like:

``` syntaxhighlighter-pre
import hudson.model.*
import hudson.AbortException
import hudson.console.HyperlinkNote
import java.util.concurrent.CancellationException

// Retrieve parameters of the current build
def foo = build.buildVariableResolver.resolve("FOO")
println "FOO=$foo"

// Start another job
def job = Hudson.instance.getJob('MyJobName')
def anotherBuild
try {
    def params = [
      new StringParameterValue('FOO', foo),
    ]
    def future = job.scheduleBuild2(0, new Cause.UpstreamCause(build), new ParametersAction(params))
    println "Waiting for the completion of " + HyperlinkNote.encodeTo('/' + job.url, job.fullDisplayName)
    anotherBuild = future.get()
} catch (CancellationException x) {
    throw new AbortException("${job.fullDisplayName} aborted.")
}
println HyperlinkNote.encodeTo('/' + anotherBuild.url, anotherBuild.fullDisplayName) + " completed. Result was " + anotherBuild.result

// Check that it succeeded
build.result = anotherBuild.result
if (anotherBuild.result != Result.SUCCESS && anotherBuild.result != Result.UNSTABLE) {
    // We abort this build right here and now.
    throw new AbortException("${anotherBuild.fullDisplayName} failed.")
}

// Do something with the output.
// On the contrary to Parameterized Trigger Plugin, you may now do something from that other build instance.
// Like the parsing the build log (see http://javadoc.jenkins-ci.org/hudson/model/FreeStyleBuild.html )
// You probably may also wish to update the current job's environment.
build.addAction(new ParametersAction(new StringParameterValue('BAR', '3')))
```

### Retrieve properties

To retrieve properties defined in the Properties field use:

``` syntaxhighlighter-pre
System.getProperty('FOO')
```

## Usage with pipeline

Currently the plugin does not support pipeline syntax. One workaround
from Alexander Samoylov
was mentioned here: <https://stackoverflow.com/a/58381147/4807875>.

#  Changelog

Please refer to [the changelog](CHANGELOG.md)

# Known bugs

-   Configuring more builders at once actually doesn't absolutely work.
    If you need more groovy builders in your project, you have to
    configure them one by one and always save project configuration
    before you add new one.
