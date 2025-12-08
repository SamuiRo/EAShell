package com.eashell.model;

import java.util.List;
import java.util.Objects;

public class ScriptEntry {
    private String name;
    private String workingDir;
    private List<String> commands;

    public ScriptEntry(String name, String workingDir, List<String> commands) {
        this.name = name;
        this.workingDir = workingDir;
        this.commands = commands;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getWorkingDir() {
        return workingDir;
    }

    public void setWorkingDir(String workingDir) {
        this.workingDir = workingDir;
    }

    public List<String> getCommands() {
        return commands;
    }

    public void setCommands(List<String> commands) {
        this.commands = commands;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ScriptEntry that = (ScriptEntry) o;
        return Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public String toString() {
        return "ScriptEntry{" +
                "name='" + name + '\'' +
                ", workingDir='" + workingDir + '\'' +
                ", commands=" + commands +
                '}';
    }
}