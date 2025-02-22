package me.lidan.zednpc.npc;

public enum ActionType {
    CMD,CONSOLE,CHAT,MESSAGE,SERVER;

    public String toString() {
        return this.name().toLowerCase();
    }
}
