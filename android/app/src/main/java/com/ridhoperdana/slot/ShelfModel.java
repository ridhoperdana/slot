package com.ridhoperdana.slot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Pure shelf behavior shared by the Android view and JVM tests. */
public final class ShelfModel {
    public enum Platform { GBA, GBC, GB }

    public static final class Game {
        public final String title;
        public final Platform platform;
        public final String romPath;
        public final String core;

        public Game(String title, Platform platform, String romPath, String core) {
            this.title = title;
            this.platform = platform;
            this.romPath = romPath;
            this.core = core;
        }
    }

    private final List<Game> games;
    private Platform platform = Platform.GBA;
    private int index;

    public ShelfModel(List<Game> games) {
        this.games = new ArrayList<>(games);
        this.index = 0;
    }

    public static ShelfModel demo() {
        List<Game> games = new ArrayList<>();
        games.add(new Game("Add your legally obtained ROMs", Platform.GBA, "", "mGBA"));
        return new ShelfModel(games);
    }

    public List<Game> games() { return Collections.unmodifiableList(games); }
    public Platform platform() { return platform; }
    public Game selected() {
        List<Game> visible = visibleGames();
        return visible.isEmpty()
                ? new Game("No games found", platform, "", platform == Platform.GB ? "Gambatte" : "mGBA")
                : visible.get(index);
    }
    public int index() { return index; }

    private List<Game> visibleGames() {
        List<Game> visible = new ArrayList<>();
        for (Game game : games) if (game.platform == platform) visible.add(game);
        return visible;
    }

    public void replaceGames(List<Game> replacement) {
        games.clear();
        games.addAll(replacement);
        index = 0;
    }

    public void next() {
        int count = visibleGames().size();
        if (count > 0) index = (index + 1) % count;
    }
    public void previous() {
        int count = visibleGames().size();
        if (count > 0) index = (index + count - 1) % count;
    }
    public void setPlatform(Platform platform) {
        this.platform = platform;
        index = 0;
    }
}
