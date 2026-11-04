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
    public Game selected() { return games.get(index); }
    public int index() { return index; }

    public void replaceGames(List<Game> replacement) {
        games.clear();
        games.addAll(replacement);
        index = 0;
    }

    public void next() { if (!games.isEmpty()) index = (index + 1) % games.size(); }
    public void previous() { if (!games.isEmpty()) index = (index + games.size() - 1) % games.size(); }
    public void setPlatform(Platform platform) {
        this.platform = platform;
        index = 0;
    }
}
