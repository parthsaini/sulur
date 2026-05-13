package in.mil.iaf.sulur.canteenfeedback;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class FeedbackDbHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "feedback.db";
    private static final int DB_VERSION = 1;

    public FeedbackDbHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE feedback (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "name TEXT," +
                "phone TEXT," +
                "food_rating INTEGER," +
                "service_rating INTEGER," +
                "cleanliness_rating INTEGER," +
                "value_rating INTEGER," +
                "comments TEXT," +
                "timestamp DATETIME DEFAULT CURRENT_TIMESTAMP)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS feedback");
        onCreate(db);
    }

    public void insertFeedback(String name, String phone, int food, int service, int cleanliness, int value, String comments) {
        ContentValues cv = new ContentValues();
        cv.put("name", name);
        cv.put("phone", phone);
        cv.put("food_rating", food);
        cv.put("service_rating", service);
        cv.put("cleanliness_rating", cleanliness);
        cv.put("value_rating", value);
        cv.put("comments", comments);
        getWritableDatabase().insert("feedback", null, cv);
    }
}
