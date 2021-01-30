# Changelog

## Release 2.2 (2019-03-06)

-   [Fix security
    issue](https://jenkins.io/security/advisory/2019-03-06/#SECURITY-1339)

## Release 2.1 (2019-01-28)

-   [Fix security
    issue](https://jenkins.io/security/advisory/2019-01-28/)

## Release 2.0 (2017-04-10)

-   Arbitrary code execution by unprivileged user
    ([SECURITY-292](https://jenkins.io/security/advisory/2017-04-10/#groovy-plugin))
-   continue with code cleanup - fixed Findbugs issues

## Release 1.30 (2016-11-18)

-   XSS protection
-   code cleanup

## Release 1.28, 1.29 (2016-01-05)

-   code cleanup

## Release 1.27 (2015-08-05)

-   `Callable` roles are properly checked

## Release 1.26 (2015-07-27)

-   Ensured correct position of class path option
    ([JENKINS-29577](https://issues.jenkins-ci.org/browse/JENKINS-29577))
-   Improved help ([pr
    \#18](https://github.com/jenkinsci/groovy-plugin/pull/18))

## Release 1.25 (2015-05-11)

-   Made default choice also for System Groovy script to avoid zero
    height of textarea
    ([JENKINS-25455](https://issues.jenkins-ci.org/browse/JENKINS-25455))
-   Add help file for Groovy version
    ([JENKINS-12988](https://issues.jenkins-ci.org/browse/JENKINS-12988))
-   Made setting Groovy installations thread-safe
    ([JENKINS-28287](https://issues.jenkins-ci.org/browse/JENKINS-28287))

## Release 1.24 (2014-11-09)

-   Ensure non-zero height of Groovy command text box, making it default
    choice when adding new build step
    ([JENKINS-25455](https://issues.jenkins-ci.org/browse/JENKINS-25455))

## Release 1.23 (2014-10-27)

-   Set up correct GROOVY\_HOME environment variable
    ([JENKINS-25275](https://issues.jenkins-ci.org/browse/JENKINS-25275))

## Release 1.22 (2014-09-30)

-   Fixed slashes conversion in script parameters
    ([JENKINS-24870](https://issues.jenkins-ci.org/browse/JENKINS-24870))

## Release 1.21 (2014-09-18)

-   Allow spaces in script parameters
    ([JENKINS-24757](https://issues.jenkins-ci.org/browse/JENKINS-24757))

## Release 1.20 (2014-07-30)

-   Unable to specify multiple jars on class path for a system groovy
    script
    ([JENKINS-23997](https://issues.jenkins-ci.org/browse/JENKINS-23997))

## Release 1.19 (2014-07-07)

-   Better parsing of parameters passed to Groovy binary, [Apache
    commons-exec](http://commons.apache.org/proper/commons-exec/) used
    for parsing
    ([JENKINS-23617](https://issues.jenkins-ci.org/browse/JENKINS-23617))

## Release 1.18 (2014-05-13)

-   NPE fixes
    ([JENKINS-17171](https://issues.jenkins-ci.org/browse/JENKINS-17171))

## Release 1.17 (2014-05-09)

-   Allow whitespaces in properties (passed via -D switch)
    ([pull13](https://github.com/jenkinsci/groovy-plugin/pull/13))

## Release 1.16 (2014-04-07)

-   Upgrade to @DataBoundConstructor
    ([JENKINS-6797](https://issues.jenkins-ci.org/browse/JENKINS-6797))
-   Fixed typo in warrning message
    ([pull12](https://github.com/jenkinsci/groovy-plugin/pull/12))

## Release 1.15 (2014-01-31)

-   Syntax highlighting
-   Syntax validation button
-   Prepare for Jenkins core upgrade to Groovy 2.x
    ([pull9](https://github.com/jenkinsci/groovy-plugin/pull/9))

## Release 1.14 (2013-07-02)

-   Right to run the System Groovy script changed from ADMINISTER to
    RUN\_SCRIPTS
    ([pull7](https://github.com/jenkinsci/groovy-plugin/pull/7))

## Release 1.13 (2013-03-01)

-   Added build context (build, launcher, listener) into system groovy
    build step
    ([pull6](https://github.com/jenkinsci/groovy-plugin/pull/6))

## Release 1.12 (2012-03-08)

-   Fixed configuration of Token macro
    ([pull5](https://github.com/jenkinsci/groovy-plugin/pull/5))

## Release 1.11 (2012-02-26)

-   Enabled env. variables expansion class path, groovy and script
    parameters

## Release 1.10 (2012-02-09)

-   Fixed possible job configuration corruption when user isn't admin
    ([JENKINS-12080](https://issues.jenkins-ci.org/browse/JENKINS-12080))
-   Avoid NPE, add fallback if groovy executable is misoncifured
    ([JENKINS-11652](https://issues.jenkins-ci.org/browse/JENKINS-11652))

## Release 1.9 (2011-09-14)

-   Auto installer
    ([JENKINS-7113](https://issues.jenkins-ci.org/browse/JENKINS-7113)
    and
    [JENKINS-10920](https://issues.jenkins-ci.org/browse/JENKINS-10920))
-   Fixed error message on global config page
    ([JENKINS-10768](https://issues.jenkins-ci.org/browse/JENKINS-10768))
-   Expansion of job parameters
    ([JENKINS-10525](https://issues.jenkins-ci.org/browse/JENKINS-10525))
-   Full access to JAVA\_OPTS (i.e. parameters like -Xmx can be set up)
-   Editable class path

## Release 1.8 (2011-05-13)

-   Fixed a configuration persistence problem that can create huge
    config.xml

## Release 1.7 (2011-03-09)

-   Added support for [Token Macro
    Plugin](https://wiki.jenkins.io/display/JENKINS/Token+Macro+Plugin)

## Release 1.6 (2011-02-08)

-   Fixed security issue

## Release 1.5 (2010-11-10)

-   Classloader for actual System Groovy
    ([JENKINS-6068](https://issues.jenkins-ci.org/browse/JENKINS-6068))
-   Allowed groovy.bat in addition to groovy.exe
    ([JENKINS-6839](https://issues.jenkins-ci.org/browse/JENKINS-6839))
-   Temp files are removed
    ([JENKINS-3269](https://issues.jenkins-ci.org/browse/JENKINS-3269))
-   Hudson global properties are expanded in groovy script file path
    ([JENKINS-8048](https://issues.jenkins-ci.org/browse/JENKINS-8048))
-   Upgraded to 1.358
    ([JENKINS-6081](https://issues.jenkins-ci.org/browse/JENKINS-6081))

## Release 1.4 (2009-12-29)

-   Improve error message for missing groovy executable
-   Update uses of deprecated APIs

## Release 1.2

-   Added possibility to specify properties and script parameters.
-   Added script source choice (file/command) for system groovy scripts.
-   Used .exe instead of .bat on Windows (as suggested by Scott Armit).
-   Added configuration option for classpath and initial variable
    bindings for  
    [system
    groovy](https://wiki.jenkins.io/display/JENKINS/Jenkins+Script+Console)
    scripts.
