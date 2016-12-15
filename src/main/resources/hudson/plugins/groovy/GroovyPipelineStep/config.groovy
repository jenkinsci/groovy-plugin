package hudson.plugins.groovy.GroovyPipelineStep

f = namespace(lib.FormTagLib)

// TODO should be a standard form control for picking a ToolInstallation
def installations = app.getDescriptorByType(hudson.plugins.groovy.Groovy.DescriptorImpl).installations
if (installations.length != 0) {
    f.entry(title:_("Groovy Version")) {
        select(class: "setting-input", name: "groovy.tool") {
            option(value: "", _("Default"))
            installations.each {
                f.option(selected: it.name == instance?.tool, value: it.name, it.name)
            }
        }
    }
}

f.entry(title: _("Arguments"), field: "args") {
    f.expandableTextbox()
}

f.entry(title: 'input', field: 'input') {}
