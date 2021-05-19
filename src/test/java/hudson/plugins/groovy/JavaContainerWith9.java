package hudson.plugins.groovy;

import org.jenkinsci.test.acceptance.docker.DockerFixture;
import org.jenkinsci.test.acceptance.docker.fixtures.JavaContainer;

@DockerFixture(id="java9", ports={22, 8080})
public class JavaContainerWith9 extends JavaContainer {}
