package com.ridhoperdana.slot;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.os.Build;
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
import android.view.WindowInsets;
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
        getWindow().setNavigationBarColor(Color.TRANSPARENT);
        shelfView = new ShelfView();
        setContentView(shelfView);
        hideSystemBars();
    }

    private void hideSystemBars() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            getWindow().setDecorFitsSystemWindows(false);
            android.view.WindowInsetsController controller = getWindow().getDecorView().getWindowInsetsController();
            if (controller != null) {
                controller.hide(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
                controller.setSystemBarsBehavior(
                        android.view.WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        } else {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        }
    }

    @Override public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) hideSystemBars();
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
        private float transitionOffset;
        private float transitionDirection;
        private boolean transitionSwitched;
        private ValueAnimator transitionAnimator;

        ShelfView() {
            super(MainActivity.this);
            shelf = ShelfModel.demo();
            background = load(com.ridhoperdana.slot.R.drawable.background_default);
            // *_cart.png are transparent masks from the original renderer. The
            // neutral shell bitmaps are the visible cartridge artwork for this port.
            gbaCart = load(com.ridhoperdana.slot.R.drawable.gba_neutral);
            gbcCart = load(com.ridhoperdana.slot.R.drawable.gbc_neutral);
            gbCart = load(com.ridhoperdana.slot.R.drawable.gb_neutral);
            buttonA = load(com.ridhoperdana.slot.R.drawable.button_a);
            buttonB = load(com.ridhoperdana.slot.R.drawable.button_b);
            buttonMenu = load(com.ridhoperdana.slot.R.drawable.button_menu);
            setFocusable(true);
        }

        private Bitmap load(int id) { return BitmapFactory.decodeResource(getResources(), id); }

        @Override protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            // Fit the 720x480 artwork inside any viewport. Using max() clipped the
            // sides on square devices; min() preserves the whole shelf composition.
            float scale = Math.min(getWidth() / 720f, getHeight() / 480f);
            float ox = (getWidth() - 720f * scale) / 2f;
            float oy = (getHeight() - 480f * scale) / 2f;
            canvas.drawBitmap(background, null, new Rect(0, 0, getWidth(), getHeight()), paint);
            canvas.save();
            canvas.translate(ox, oy);
            canvas.scale(scale, scale);
            canvas.clipRect(0, 0, 720, 480);
            canvas.translate(transitionOffset, 0);
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
            // The RG Pocket Classic is nearly square. Keep all three actions inside
            // the fitted 720px composition instead of letting the last label touch
            // the viewport edge on narrow aspect ratios.
            // Three equal action slots keep the row centered on both wide and square screens.
            int[] centers = {120, 360, 600};
            Bitmap[] buttons = {buttonA, buttonB, buttonMenu};
            String[] labels = {"ADD ROM FOLDER", "CHANGE SYSTEM", "MENU"};
            float[] labelWidths = {82f, 82f, 30f};
            for (int i = 0; i < centers.length; i++) {
                float groupWidth = 40f + labelWidths[i];
                int left = Math.round(centers[i] - groupWidth / 2f);
                canvas.drawBitmap(buttons[i], null, new Rect(left, 432, left + 32, 464), paint);
                text(canvas, labels[i], left + 40, 453, 10, Color.WHITE, true);
            }
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
            float scale = Math.min(getWidth() / 720f, getHeight() / 480f);
            float x = (event.getX() - (getWidth() - 720f * scale) / 2f) / scale;
            float y = (event.getY() - (getHeight() - 480f * scale) / 2f) / scale;
            float delta = event.getX() - downX;
            if (menu) {
                if (y > 170 && y < 225) chooseFolder();
                else if (y > 220 && y < 275) { menu = false; invalidate(); }
                return true;
            }
            if (Math.abs(delta) > 60) {
                animateShelf(delta < 0 ? -1f : 1f, () -> {
                    if (delta < 0) shelf.next(); else shelf.previous();
                });
                return true;
            }
            float scaleForTouch = Math.min(getWidth() / 720f, getHeight() / 480f);
            float contentWidth = 720f * scaleForTouch;
            float contentLeft = (getWidth() - contentWidth) / 2f;
            float contentX = (event.getX() - contentLeft) / scaleForTouch;
            float menuStart = 490;
            float systemStart = 230;
            if (y > 410 && contentX >= menuStart) { menu = true; invalidate(); return true; }
            if (y > 410 && contentX > systemStart && contentX < menuStart) { animateShelf(-1f, this::nextPlatform); return true; }
            if (y > 410 && contentX <= systemStart) { chooseFolder(); return true; }
            if (y > 110 && y < 350 && System.currentTimeMillis() - downAt < 500) launchSelected();
            return true;
        }

        private void animateShelf(float direction, Runnable switchContent) {
            if (transitionAnimator != null && transitionAnimator.isRunning()) return;
            transitionDirection = direction;
            transitionOffset = 0f;
            transitionSwitched = false;
            transitionAnimator = ValueAnimator.ofFloat(0f, 1f);
            transitionAnimator.setDuration(280L);
            transitionAnimator.setInterpolator(new android.view.animation.DecelerateInterpolator());
            transitionAnimator.addUpdateListener(animation -> {
                float progress = (Float) animation.getAnimatedValue();
                if (!transitionSwitched && progress >= 0.5f) {
                    switchContent.run();
                    transitionSwitched = true;
                }
                transitionOffset = transitionSwitched
                        ? -transitionDirection * (1f - progress) * 720f
                        : transitionDirection * progress * 720f;
                invalidate();
            });
            transitionAnimator.addListener(new AnimatorListenerAdapter() {
                @Override public void onAnimationEnd(Animator animation) {
                    transitionOffset = 0f;
                    transitionAnimator = null;
                    invalidate();
                }
            });
            transitionAnimator.start();
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
