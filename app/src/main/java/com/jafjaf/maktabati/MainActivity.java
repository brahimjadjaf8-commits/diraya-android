package com.jafjaf.maktabati;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Locale;
import java.util.UUID;

public class MainActivity extends Activity {
    private static final int PICK_BOOK = 301;
    private static final String PREFS = "maktabati";
    private static final String KEY_BOOKS = "books";
    private final ArrayList<Book> books = new ArrayList<>();
    private final ArrayList<Book> shown = new ArrayList<>();
    private BookAdapter adapter;
    private TextView emptyView;
    private TextView countView;
    private EditText searchInput;
    private Uri selectedUri;
    private String selectedName;
    private String selectedMime;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(Color.rgb(78, 14, 14));
        getWindow().setNavigationBarColor(Color.rgb(78, 14, 14));
        loadBooks();
        buildUi();
        filter("");
    }

    private void buildUi() {
        LinearLayout root = vertical();
        root.setBackgroundColor(Color.rgb(247, 242, 230));

        LinearLayout header = horizontal();
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(16), dp(15), dp(16), dp(15));
        header.setBackgroundColor(Color.rgb(122, 23, 23));

        TextView title = text("مكتبة الشيخ جفجاف إبراهيم", 21, Color.WHITE);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        header.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

        Button add = new Button(this);
        add.setText("＋ إضافة كتاب");
        add.setTextSize(15);
        add.setTextColor(Color.rgb(55, 21, 0));
        add.setBackgroundColor(Color.rgb(211, 163, 41));
        add.setOnClickListener(v -> chooseBook());
        header.addView(add, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(48)));
        root.addView(header);

        LinearLayout content = vertical();
        content.setPadding(dp(14), dp(14), dp(14), dp(10));

        searchInput = new EditText(this);
        searchInput.setHint("ابحث باسم الكتاب أو المؤلف أو القسم");
        searchInput.setTextSize(16);
        searchInput.setSingleLine(true);
        searchInput.setPadding(dp(16), 0, dp(16), 0);
        searchInput.setBackgroundColor(Color.WHITE);
        searchInput.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        searchInput.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            public void onTextChanged(CharSequence s, int st, int before, int count) { filter(s.toString()); }
            public void afterTextChanged(Editable e) {}
        });
        content.addView(searchInput, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(52)));

        LinearLayout summary = horizontal();
        summary.setGravity(Gravity.CENTER_VERTICAL);
        summary.setPadding(dp(2), dp(13), dp(2), dp(8));
        TextView booksLabel = text("الكتب", 18, Color.rgb(122, 23, 23));
        booksLabel.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        countView = text("", 14, Color.DKGRAY);
        summary.addView(booksLabel, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        summary.addView(countView);
        content.addView(summary);

        ListView list = new ListView(this);
        list.setDividerHeight(dp(9));
        list.setDivider(null);
        list.setPadding(0, 0, 0, dp(8));
        list.setClipToPadding(false);
        adapter = new BookAdapter();
        list.setAdapter(adapter);
        list.setOnItemClickListener((p, v, position, id) -> openBook(shown.get(position)));
        list.setOnItemLongClickListener((p, v, position, id) -> {
            showBookActions(shown.get(position));
            return true;
        });

        emptyView = text("المكتبة فارغة\nاضغط «إضافة كتاب» واختر كتابًا من الهاتف", 18, Color.rgb(99, 91, 76));
        emptyView.setGravity(Gravity.CENTER);
        emptyView.setPadding(dp(25), dp(25), dp(25), dp(25));

        LinearLayout listBox = new LinearLayout(this);
        listBox.setOrientation(LinearLayout.VERTICAL);
        listBox.addView(list, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));
        listBox.addView(emptyView, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));
        content.addView(listBox, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));
        root.addView(content, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));
        setContentView(root);
    }

    private void chooseBook() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{
                "application/pdf", "text/plain", "text/html", "application/epub+zip",
                "application/msword", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
        });
        startActivityForResult(intent, PICK_BOOK);
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != PICK_BOOK || resultCode != RESULT_OK || data == null || data.getData() == null) return;
        selectedUri = data.getData();
        selectedMime = getContentResolver().getType(selectedUri);
        selectedName = displayName(selectedUri);
        showMetadataDialog(null);
    }

    private void showMetadataDialog(Book existing) {
        LinearLayout box = vertical();
        box.setPadding(dp(18), dp(8), dp(18), 0);
        EditText title = field("اسم الكتاب", existing == null ? stripExtension(selectedName) : existing.title);
        EditText author = field("اسم المؤلف – اختياري", existing == null ? "" : existing.author);
        EditText category = field("القسم: حديث، رجال، فقه...", existing == null ? "" : existing.category);
        box.addView(title); box.addView(author); box.addView(category);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(existing == null ? "إضافة كتاب" : "تعديل معلومات الكتاب")
                .setView(box)
                .setNegativeButton("إلغاء", null)
                .setPositiveButton("حفظ", null)
                .create();
        dialog.setOnShowListener(x -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String bookTitle = title.getText().toString().trim();
            if (bookTitle.isEmpty()) { title.setError("اكتب اسم الكتاب"); return; }
            if (existing != null) {
                existing.title = bookTitle;
                existing.author = author.getText().toString().trim();
                existing.category = category.getText().toString().trim();
                saveBooks(); filter(searchInput.getText().toString()); dialog.dismiss();
            } else {
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
                importBook(bookTitle, author.getText().toString().trim(), category.getText().toString().trim(), dialog);
            }
        }));
        dialog.show();
    }

    private void importBook(String title, String author, String category, AlertDialog dialog) {
        new Thread(() -> {
            try {
                File dir = new File(getFilesDir(), "books");
                if (!dir.exists() && !dir.mkdirs()) throw new Exception("تعذر إنشاء مجلد الكتب");
                String extension = extension(selectedName);
                String internalName = UUID.randomUUID().toString() + (extension.isEmpty() ? "" : "." + extension);
                File target = new File(dir, internalName);
                long total = 0;
                try (InputStream in = getContentResolver().openInputStream(selectedUri);
                     FileOutputStream out = new FileOutputStream(target)) {
                    if (in == null) throw new Exception("تعذر قراءة الملف");
                    byte[] buffer = new byte[65536]; int n;
                    while ((n = in.read(buffer)) != -1) { out.write(buffer, 0, n); total += n; }
                }
                Book b = new Book();
                b.id = UUID.randomUUID().toString(); b.title = title; b.author = author; b.category = category;
                b.originalName = selectedName; b.internalName = internalName;
                b.mime = selectedMime == null ? mimeFromName(selectedName) : selectedMime;
                b.size = total; b.createdAt = System.currentTimeMillis();
                books.add(b); saveBooks();
                runOnUiThread(() -> { dialog.dismiss(); filter(""); Toast.makeText(this, "تمت إضافة الكتاب", Toast.LENGTH_SHORT).show(); });
            } catch (Exception e) {
                runOnUiThread(() -> { dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true); Toast.makeText(this, "تعذر إضافة الكتاب: " + e.getMessage(), Toast.LENGTH_LONG).show(); });
            }
        }).start();
    }

    private void openBook(Book b) {
        File file = new File(new File(getFilesDir(), "books"), b.internalName);
        if (!file.exists()) { Toast.makeText(this, "ملف الكتاب غير موجود", Toast.LENGTH_LONG).show(); return; }
        String mime = b.mime == null ? mimeFromName(b.originalName) : b.mime;
        if (mime.startsWith("text/") || mime.equals("application/xhtml+xml")) {
            Intent reader = new Intent(this, ReaderActivity.class);
            reader.putExtra("path", file.getAbsolutePath()); reader.putExtra("title", b.title); reader.putExtra("mime", mime);
            startActivity(reader); return;
        }
        Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".files", file);
        Intent open = new Intent(Intent.ACTION_VIEW).setDataAndType(uri, mime).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try { startActivity(open); }
        catch (Exception e) { Toast.makeText(this, "لا يوجد في الهاتف قارئ مناسب لهذا النوع", Toast.LENGTH_LONG).show(); }
    }

    private void showBookActions(Book b) {
        new AlertDialog.Builder(this).setTitle(b.title)
                .setItems(new String[]{"تعديل المعلومات", "حذف الكتاب"}, (d, which) -> {
                    if (which == 0) showMetadataDialog(b); else confirmDelete(b);
                }).show();
    }

    private void confirmDelete(Book b) {
        new AlertDialog.Builder(this).setTitle("حذف الكتاب")
                .setMessage("هل تريد حذف «" + b.title + "» من الهاتف؟")
                .setNegativeButton("إلغاء", null)
                .setPositiveButton("حذف", (d, w) -> {
                    File file = new File(new File(getFilesDir(), "books"), b.internalName);
                    if (file.exists()) file.delete();
                    books.remove(b); saveBooks(); filter(searchInput.getText().toString());
                }).show();
    }

    private void filter(String value) {
        if (adapter == null) return;
        String q = normalize(value);
        shown.clear();
        ArrayList<Book> ordered = new ArrayList<>(books);
        Collections.sort(ordered, (a, b) -> Long.compare(b.createdAt, a.createdAt));
        for (Book b : ordered) {
            String hay = normalize(b.title + " " + b.author + " " + b.category);
            if (q.isEmpty() || hay.contains(q)) shown.add(b);
        }
        adapter.notifyDataSetChanged();
        countView.setText(shown.size() + " كتاب");
        emptyView.setVisibility(shown.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void loadBooks() {
        books.clear();
        try {
            JSONArray array = new JSONArray(getSharedPreferences(PREFS, MODE_PRIVATE).getString(KEY_BOOKS, "[]"));
            for (int i = 0; i < array.length(); i++) books.add(Book.from(array.getJSONObject(i)));
        } catch (Exception ignored) {}
    }

    private void saveBooks() {
        try {
            JSONArray array = new JSONArray(); for (Book b : books) array.put(b.toJson());
            getSharedPreferences(PREFS, MODE_PRIVATE).edit().putString(KEY_BOOKS, array.toString()).apply();
        } catch (Exception ignored) {}
    }

    private String displayName(Uri uri) {
        String name = "كتاب";
        try (Cursor c = getContentResolver().query(uri, null, null, null, null)) {
            if (c != null && c.moveToFirst()) { int i = c.getColumnIndex(OpenableColumns.DISPLAY_NAME); if (i >= 0) name = c.getString(i); }
        } catch (Exception ignored) {}
        return name == null ? "كتاب" : name;
    }

    private String stripExtension(String s) { int i = s.lastIndexOf('.'); return i > 0 ? s.substring(0, i) : s; }
    private String extension(String s) { int i = s.lastIndexOf('.'); return i >= 0 && i < s.length() - 1 ? s.substring(i + 1).toLowerCase(Locale.ROOT) : ""; }
    private String mimeFromName(String n) {
        String e = extension(n); if (e.equals("pdf")) return "application/pdf"; if (e.equals("html") || e.equals("htm")) return "text/html";
        if (e.equals("epub")) return "application/epub+zip"; if (e.equals("doc")) return "application/msword";
        if (e.equals("docx")) return "application/vnd.openxmlformats-officedocument.wordprocessingml.document"; return "text/plain";
    }
    private String normalize(String s) { return Normalizer.normalize(s == null ? "" : s, Normalizer.Form.NFD).replaceAll("[\\u064B-\\u065F\\u0670]", "").toLowerCase(Locale.ROOT); }
    private int dp(int v) { return Math.round(v * getResources().getDisplayMetrics().density); }
    private LinearLayout vertical() { LinearLayout x = new LinearLayout(this); x.setOrientation(LinearLayout.VERTICAL); x.setLayoutDirection(View.LAYOUT_DIRECTION_RTL); return x; }
    private LinearLayout horizontal() { LinearLayout x = new LinearLayout(this); x.setOrientation(LinearLayout.HORIZONTAL); x.setLayoutDirection(View.LAYOUT_DIRECTION_RTL); return x; }
    private TextView text(String s, int sp, int color) { TextView t = new TextView(this); t.setText(s); t.setTextSize(sp); t.setTextColor(color); t.setLayoutDirection(View.LAYOUT_DIRECTION_RTL); return t; }
    private EditText field(String hint, String value) { EditText e = new EditText(this); e.setHint(hint); e.setText(value); e.setTextSize(16); e.setSingleLine(true); e.setLayoutDirection(View.LAYOUT_DIRECTION_RTL); e.setPadding(dp(10), dp(8), dp(10), dp(8)); e.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(58))); return e; }

    private class BookAdapter extends BaseAdapter {
        public int getCount() { return shown.size(); }
        public Object getItem(int p) { return shown.get(p); }
        public long getItemId(int p) { return p; }
        public View getView(int position, View convert, ViewGroup parent) {
            Book b = shown.get(position);
            LinearLayout row = horizontal(); row.setGravity(Gravity.CENTER_VERTICAL); row.setPadding(dp(14), dp(14), dp(14), dp(14)); row.setBackgroundColor(Color.WHITE);
            TextView badge = text(extension(b.originalName).toUpperCase(Locale.ROOT), 12, Color.WHITE); badge.setGravity(Gravity.CENTER); badge.setTypeface(Typeface.DEFAULT, Typeface.BOLD); badge.setBackgroundColor(Color.rgb(122, 23, 23));
            row.addView(badge, new LinearLayout.LayoutParams(dp(52), dp(52)));
            LinearLayout info = vertical(); info.setPadding(dp(13), 0, dp(13), 0);
            TextView title = text(b.title, 18, Color.rgb(40, 45, 48)); title.setTypeface(Typeface.DEFAULT, Typeface.BOLD); info.addView(title);
            TextView meta = text((b.author.isEmpty() ? "مؤلف غير محدد" : b.author) + (b.category.isEmpty() ? "" : "  ·  " + b.category), 14, Color.rgb(105, 105, 100)); info.addView(meta);
            TextView size = text(formatSize(b.size) + "  —  اضغط للفتح، واضغط مطولًا للتعديل", 12, Color.rgb(145, 112, 48)); info.addView(size);
            row.addView(info, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
            return row;
        }
    }

    private String formatSize(long n) { return n >= 1048576 ? String.format(Locale.US, "%.1f م.ب", n / 1048576.0) : Math.max(1, n / 1024) + " ك.ب"; }

    static class Book {
        String id = "", title = "", author = "", category = "", originalName = "", internalName = "", mime = "application/octet-stream";
        long size, createdAt;
        JSONObject toJson() throws Exception { JSONObject o = new JSONObject(); o.put("id", id); o.put("title", title); o.put("author", author); o.put("category", category); o.put("originalName", originalName); o.put("internalName", internalName); o.put("mime", mime); o.put("size", size); o.put("createdAt", createdAt); return o; }
        static Book from(JSONObject o) { Book b = new Book(); b.id=o.optString("id"); b.title=o.optString("title"); b.author=o.optString("author"); b.category=o.optString("category"); b.originalName=o.optString("originalName"); b.internalName=o.optString("internalName"); b.mime=o.optString("mime","application/octet-stream"); b.size=o.optLong("size"); b.createdAt=o.optLong("createdAt"); return b; }
    }
}
