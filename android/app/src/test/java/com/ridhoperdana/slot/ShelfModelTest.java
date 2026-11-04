package com.ridhoperdana.slot;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import org.junit.Test;

public class ShelfModelTest {
    @Test public void nextWrapsToFirstGame() {
        ShelfModel shelf = new ShelfModel(Arrays.asList(
                new ShelfModel.Game("One", ShelfModel.Platform.GBA, "one.gba", "mGBA"),
                new ShelfModel.Game("Two", ShelfModel.Platform.GBA, "two.gba", "mGBA")));
        shelf.next();
        shelf.next();
        assertEquals("One", shelf.selected().title);
    }

    @Test public void previousWrapsToLastGame() {
        ShelfModel shelf = new ShelfModel(Arrays.asList(
                new ShelfModel.Game("One", ShelfModel.Platform.GB, "one.gb", "Gambatte"),
                new ShelfModel.Game("Two", ShelfModel.Platform.GB, "two.gb", "Gambatte")));
        shelf.previous();
        assertEquals("Two", shelf.selected().title);
    }
}
