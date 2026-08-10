package com.eashell.model;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class ScriptEntry {
    private String id;
    private String name;
    private String group; // null -> rendered as "Ungrouped"; a pure view concern, not identity
    private String workingDir;
    private List<String> commands;

    public ScriptEntry(String name, String workingDir, List<String> commands) {
        this(UUID.randomUUID().toString(), name, workingDir, commands);
    }

    public ScriptEntry(String id, String name, String workingDir, List<String> commands) {
        this.id = id;
        this.name = name;
        this.workingDir = workingDir;
        this.commands = commands;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
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
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "ScriptEntry{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", group='" + group + '\'' +
                ", workingDir='" + workingDir + '\'' +
                ", commands=" + commands +
                '}';
    }
}
