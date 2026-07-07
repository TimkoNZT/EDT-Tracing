package com._1c.g5.v8.dt.internal.tracing.ui.view;

public class ModuleFilterEntry {
    public String pattern;
    public boolean enabled;

    public ModuleFilterEntry(String pattern, boolean enabled) {
        this.pattern = pattern;
        this.enabled = enabled;
    }

    @Override
    public String toString() {
        return (enabled ? "[x] " : "[ ] ") + pattern;
    }
}
