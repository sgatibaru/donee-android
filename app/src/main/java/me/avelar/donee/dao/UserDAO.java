package me.avelar.donee.dao;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.NonNull;
import android.widget.ImageView;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import me.avelar.donee.controller.SessionManager;
import me.avelar.donee.model.User;
import me.avelar.donee.util.PhotoCacheLoader;
import me.avelar.donee.web.UrlRepository;

public final class UserDAO {

    public static void insert(@NonNull SQLiteDatabase db, @NonNull User user) throws SQLException {
        ContentValues values = new ContentValues();
        values.put(DoneeDbHelper.C_USER_ID, user.getId());
        values.put(DoneeDbHelper.C_USER_NAME, user.getName());
        values.put(DoneeDbHelper.C_USER_EMAIL, user.getEmail());
        values.put(DoneeDbHelper.C_USER_ACCOUNT, user.getAccount());
        
        long result = db.insertWithOnConflict(DoneeDbHelper.T_USER, null,
                values, SQLiteDatabase.CONFLICT_REPLACE);
        if (result == DoneeDbHelper.DB_ERROR) throw new SQLException();
    }

    @SuppressWarnings("unused")
    public static void update(@NonNull Context context, @NonNull User user) {
        SQLiteDatabase db = DoneeDbHelper.getInstance(context).getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(DoneeDbHelper.C_USER_ID, user.getId());
        if (user.getName()    != null) values.put(DoneeDbHelper.C_USER_NAME,    user.getName());
        if (user.getEmail()   != null) values.put(DoneeDbHelper.C_USER_EMAIL,   user.getEmail());
        if (user.getAccount() != null) values.put(DoneeDbHelper.C_USER_ACCOUNT, user.getAccount());
        if (user.getLastSynced()  > 0) values.put(DoneeDbHelper.C_USER_SYNCED,  user.getLastSynced());

        String[] args = { user.getId() };
        db.update(DoneeDbHelper.T_USER, values, DoneeDbHelper.C_USER_ID + " = ?", args);
    }

    public static User find(@NonNull Context context, String id) {
        SQLiteDatabase db = DoneeDbHelper.getInstance(context).getReadableDatabase();
        String[] args = { id };
        Cursor cursor = db.query(DoneeDbHelper.T_USER, null,
                DoneeDbHelper.C_USER_ID + " = ?", args, null, null, null);

        if (cursor.getCount() == 0) return null;
        cursor.moveToFirst();
        String name    = cursor.getString(cursor.getColumnIndex(DoneeDbHelper.C_USER_NAME));
        String email   = cursor.getString(cursor.getColumnIndex(DoneeDbHelper.C_USER_EMAIL));
        String account = cursor.getString(cursor.getColumnIndex(DoneeDbHelper.C_USER_ACCOUNT));
        long   updated = cursor.getLong  (cursor.getColumnIndex(DoneeDbHelper.C_USER_SYNCED));
        User user = new User(id, name, email, account, updated);
        cursor.close();

        return user;
    }

    public static List<User> getAll(@NonNull Context context) {
        ArrayList<User> users = new ArrayList<>();
        SQLiteDatabase db = DoneeDbHelper.getInstance(context).getReadableDatabase();
        Cursor cursor = db.query(DoneeDbHelper.T_USER, null, null,
                null, null, null, DoneeDbHelper.C_USER_NAME);
        while (cursor.moveToNext()) {
            users.add(new User(
                cursor.getString(cursor.getColumnIndex(DoneeDbHelper.C_USER_ID)),
                cursor.getString(cursor.getColumnIndex(DoneeDbHelper.C_USER_NAME)),
                cursor.getString(cursor.getColumnIndex(DoneeDbHelper.C_USER_EMAIL)),
                cursor.getString(cursor.getColumnIndex(DoneeDbHelper.C_USER_ACCOUNT)),
                cursor.getLong  (cursor.getColumnIndex(DoneeDbHelper.C_USER_SYNCED))
            ));
        }
        cursor.close();

        return users;
    }

    public static List<User> getOthers(Context context) {
        List<User> others = getAll(context);
        others.remove(SessionManager.getLastSession(context).getUser());
        return others;
    }

    public static void notifySync(Context context, User user) {
        // sets the lastSynced attribute to current time and updates in DB
        update(context, user.setLastSynced(new Date().getTime()));
    }

    public static void delete(Context context, User user) {
        SQLiteDatabase db = DoneeDbHelper.getInstance(context).getWritableDatabase();
        String[] args = {user.getId()};

        db.delete(DoneeDbHelper.T_USER, DoneeDbHelper.C_USER_ID + " = ?", args);
    }
}