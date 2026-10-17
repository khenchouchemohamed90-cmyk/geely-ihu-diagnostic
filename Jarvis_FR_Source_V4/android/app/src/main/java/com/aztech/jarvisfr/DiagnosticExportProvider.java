package com.aztech.jarvisfr;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;
import java.io.File;
import java.io.FileNotFoundException;

/** Partage explicite d'un unique instantané en lecture seule, sans chemin arbitraire. */
public final class DiagnosticExportProvider extends ContentProvider {
    public static final String EXPORT_PATH = "/export/jarvis-fr-diagnostic.txt";
    @Override public boolean onCreate() { return true; }
    private File checkedFile(Uri uri) {
        if (getContext() == null || !EXPORT_PATH.equals(uri.getPath())
                || !(getContext().getPackageName() + ".diagnostic").equals(uri.getAuthority()))
            throw new SecurityException("Chemin diagnostic refusé");
        return DiagnosticJournal.exportFile(getContext());
    }
    @Override public String getType(Uri uri) { checkedFile(uri); return "text/plain"; }
    @Override public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        File file = checkedFile(uri);
        if (!"r".equals(mode)) throw new SecurityException("Lecture seule");
        return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
    }
    @Override public Cursor query(Uri uri, String[] projection, String selection, String[] args, String sort) {
        File file = checkedFile(uri);
        String[] columns = projection == null ? new String[]{OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE} : projection;
        MatrixCursor cursor = new MatrixCursor(columns); Object[] row = new Object[columns.length];
        for (int i = 0; i < columns.length; i++) {
            if (OpenableColumns.DISPLAY_NAME.equals(columns[i])) row[i] = file.getName();
            else if (OpenableColumns.SIZE.equals(columns[i])) row[i] = file.length();
        }
        cursor.addRow(row); return cursor;
    }
    @Override public Uri insert(Uri uri, ContentValues values) { throw new UnsupportedOperationException("Lecture seule"); }
    @Override public int update(Uri uri, ContentValues values, String selection, String[] args) { throw new UnsupportedOperationException("Lecture seule"); }
    @Override public int delete(Uri uri, String selection, String[] args) { throw new UnsupportedOperationException("Lecture seule"); }
}
