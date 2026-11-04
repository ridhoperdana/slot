package com.ridhoperdana.slot;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.ContentResolver;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.database.Cursor;
import android.provider.DocumentsContract;
import android.provider.Settings;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class MainActivity extends Activity {
    private ShelfView shelfView;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        shelfView = new ShelfView();
        setContentView(shelfView);
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != 7 || resultCode != RESULT_OK || data == null || data.getData() == null) return;
        Uri tree = data.getData();
        try {
            getContentResolver().takePersistableUriPermission(tree, Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } catch (SecurityException ignored) { }
        shelfView.addFolder(tree);
    }

    private final class ShelfView extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final ShelfModel shelf;
        private final Bitmap background, gbaCart, gbcCart, gbCart, buttonA, buttonB, buttonMenu;
        private float downX;
        private long downAt;
        private boolean menu;

        ShelfView() {
            super(MainActivity.this);
            shelf = ShelfModel.demo();
            background = load(com.ridhoperdana.slot.R.drawable.background_default);
            gbaCart = load(com.ridhoperdana.slot.R.drawable.gba_cart);
            gbcCart = load(com.ridhoperdana.slot.R.drawable.gbc_cart);
            gbCart = load(com.ridhoperdana.slot.R.drawable.gb_cart);
            buttonA = load(com.ridhoperdana.slot.R.drawable.button_a);
            buttonB = load(com.ridhoperdana.slot.R.drawable.button_b);
            buttonMenu = load(com.ridhoperdana.slot.R.drawable.button_menu);
            setFocusable(true);
        }

        private Bitmap load(int id) { return BitmapFactory.decodeResource(getResources(), id); }

        @Override protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float scale = Math.max(getWidth() / 720f, getHeight() / 480f);
            float ox = (getWidth() - 720f * scale) / 2f;
            float oy = (getHeight() - 480f * scale) / 2f;
            canvas.save();
            canvas.translate(ox, oy);
            canvas.scale(scale, scale);
            canvas.drawBitmap(background, null, new Rect(0, 0, 720, 480), paint);
            drawHeader(canvas);
            drawShelf(canvas);
            drawFooter(canvas);
            if (menu) drawMenu(canvas);
            canvas.restore();
        }

        private void drawHeader(Canvas canvas) {
            text(canvas, "slot", 34, 45, 25, Color.WHITE, true);
            text(canvas, "ANDROID EDITION", 120, 43, 12, 0xfff2c14e, true);
            text(canvas, "GAME BOY LIBRARY", 34, 76, 12, 0xffd7d7df, false);
        }

        private void drawShelf(Canvas canvas) {
            ShelfModel.Platform p = shelf.platform();
            Bitmap cart = p == ShelfModel.Platform.GBA ? gbaCart : p == ShelfModel.Platform.GBC ? gbcCart : gbCart;
            int w = p == ShelfModel.Platform.GBA ? 240 : 210;
            int h = p == ShelfModel.Platform.GBA ? 142 : 228;
            int left = (720 - w) / 2;
            int top = p == ShelfModel.Platform.GBA ? 120 : 84;
            canvas.drawBitmap(cart, null, new Rect(left, top, left + w, top + h), paint);
            String title = shelf.selected().title;
            textCentered(canvas, title, 360, 357, 20, Color.WHITE, true);
            textCentered(canvas, p.name(), 360, 382, 13, 0xfff2c14e, true);
            String hint = shelf.games().isEmpty() ? "No ROMs found — choose a folder to add games" : "Swipe to browse • Tap the cart to launch";
            textCentered(canvas, hint, 360, 410, 11, 0xffd7d7df, false);
        }

        private void drawFooter(Canvas canvas) {
            canvas.drawBitmap(buttonA, null, new Rect(36, 432, 68, 464), paint);
            text(canvas, "ADD ROM FOLDER", 78, 453, 11, Color.WHITE, true);
            canvas.drawBitmap(buttonB, null, new Rect(278, 432, 310, 464), paint);
            text(canvas, "CHANGE SYSTEM", 320, 453, 11, Color.WHITE, true);
            canvas.drawBitmap(buttonMenu, null, new Rect(570, 432, 602, 464), paint);
            text(canvas, "MENU", 612, 453, 11, Color.WHITE, true);
        }

        private void drawMenu(Canvas canvas) {
            paint.setColor(0xee17171e);
            canvas.drawRect(120, 66, 600, 414, paint);
            text(canvas, "SLOT MENU", 155, 112, 24, Color.WHITE, true);
            text(canvas, "Android port of slot — Game Boy-centric frontend", 155, 145, 12, 0xffd7d7df, false);
            text(canvas, "A   Add ROM folder", 155, 200, 16, Color.WHITE, true);
            text(canvas, "B   Close menu", 155, 238, 16, Color.WHITE, true);
            text(canvas, "Games launch through RetroArch when installed", 155, 298, 12, 0xfff2c14e, false);
            text(canvas, "ROMs must be legally obtained and unzipped", 155, 325, 12, 0xffd7d7df, false);
        }

        private void text(Canvas c, String s, float x, float y, float size, int color, boolean bold) {
            paint.setColor(color); paint.setTextSize(size); paint.setTypeface(bold ? android.graphics.Typeface.DEFAULT_BOLD : android.graphics.Typeface.DEFAULT); c.drawText(s, x, y, paint);
        }
        private void textCentered(Canvas c, String s, float x, float y, float size, int color, boolean bold) {
            paint.setTextSize(size); text(c, s, x - paint.measureText(s) / 2, y, size, color, bold);
        }

        @Override public boolean onTouchEvent(MotionEvent event) {
            if (event.getAction() == MotionEvent.ACTION_DOWN) { downX = event.getX(); downAt = System.currentTimeMillis(); return true; }
            if (event.getAction() != MotionEvent.ACTION_UP) return true;
            float scale = Math.max(getWidth() / 720f, getHeight() / 480f);
            float x = (event.getX() - (getWidth() - 720f * scale) / 2f) / scale;
            float y = (event.getY() - (getHeight() - 480f * scale) / 2f) / scale;
            float delta = event.getX() - downX;
            if (menu) {
                if (y > 170 && y < 225) chooseFolder();
                else if (y > 220 && y < 275) { menu = false; invalidate(); }
                return true;
            }
            if (Math.abs(delta) > 60) { if (delta < 0) shelf.next(); else shelf.previous(); invalidate(); return true; }
            if (y > 410 && x > 530) { menu = true; invalidate(); return true; }
            if (y > 410 && x > 240 && x < 530) { nextPlatform(); invalidate(); return true; }
            if (y > 410 && x < 235) { chooseFolder(); return true; }
            if (y > 110 && y < 350 && System.currentTimeMillis() - downAt < 500) launchSelected();
            return true;
        }

        private void addFolder(Uri tree) {
            List<ShelfModel.Game> found = new ArrayList<>();
            scanFolder(tree, DocumentsContract.getTreeDocumentId(tree), found);
            if (!found.isEmpty()) {
                shelf.replaceGames(found);
                Toast.makeText(MainActivity.this, found.size() + " game(s) added", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(MainActivity.this, "No .gba, .gbc, or .gb ROMs found", Toast.LENGTH_SHORT).show();
            }
            invalidate();
        }

        private void scanFolder(Uri tree, String documentId, List<ShelfModel.Game> found) {
            ContentResolver resolver = getContentResolver();
            Uri children = DocumentsContract.buildChildDocumentsUriUsingTree(tree, documentId);
            String[] columns = { DocumentsContract.Document.COLUMN_DOCUMENT_ID, DocumentsContract.Document.COLUMN_DISPLAY_NAME, DocumentsContract.Document.COLUMN_MIME_TYPE };
            try (Cursor cursor = resolver.query(children, columns, null, null, null)) {
                if (cursor == null) return;
                while (cursor.moveToNext()) {
                    String id = cursor.getString(0);
                    String name = cursor.getString(1);
                    String mime = cursor.getString(2);
                    Uri child = DocumentsContract.buildDocumentUriUsingTree(tree, id);
                    if (DocumentsContract.Document.MIME_TYPE_DIR.equals(mime)) scanFolder(tree, id, found);
                    else {
                        String lower = name.toLowerCase(Locale.ROOT);
                        ShelfModel.Platform p = lower.endsWith(".gba") ? ShelfModel.Platform.GBA : lower.endsWith(".gbc") ? ShelfModel.Platform.GBC : lower.endsWith(".gb") ? ShelfModel.Platform.GB : null;
                        if (p != null) found.add(new ShelfModel.Game(name, p, child.toString(), p == ShelfModel.Platform.GBA ? "mGBA" : p == ShelfModel.Platform.GBC ? "mGBA" : "Gambatte"));
                    }
                }
            }
        }

        private void nextPlatform() {
            ShelfModel.Platform p = shelf.platform();
            shelf.setPlatform(p == ShelfModel.Platform.GBA ? ShelfModel.Platform.GBC : p == ShelfModel.Platform.GBC ? ShelfModel.Platform.GB : ShelfModel.Platform.GBA);
        }

        private void chooseFolder() {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
            try { startActivityForResult(intent, 7); } catch (ActivityNotFoundException e) { Toast.makeText(MainActivity.this, "No file picker available", Toast.LENGTH_SHORT).show(); }
        }

        private void launchSelected() {
            ShelfModel.Game game = shelf.selected();
            if (game.romPath.isEmpty()) { chooseFolder(); return; }
            Intent intent = new Intent();
            intent.setClassName("com.retroarch", "com.retroarch.browser.retroactivity.RetroActivityFuture");
            intent.setData(Uri.parse(game.romPath));
            intent.putExtra("LIBRETRO", game.core.equals("Gambatte") ? "gambatte" : "mgba");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            try { startActivity(intent); } catch (ActivityNotFoundException e) { Toast.makeText(MainActivity.this, "Install RetroArch to launch games", Toast.LENGTH_SHORT).show(); }
        }
    }
}
