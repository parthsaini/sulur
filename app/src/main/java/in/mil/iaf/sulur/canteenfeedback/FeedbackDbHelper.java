package in.mil.iaf.sulur.canteenfeedback;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class FeedbackDbHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "feedback.db";
    private static final int DB_VERSION = 2;

    public FeedbackDbHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE feedback (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "name TEXT," +
                "phone TEXT," +
                "overall INTEGER," +
                "atmosphere INTEGER," +
                "staff INTEGER," +
                "availability INTEGER," +
                "quality INTEGER," +
                "billing INTEGER," +
                "location INTEGER," +
                "comments TEXT," +
                "timestamp DATETIME DEFAULT CURRENT_TIMESTAMP)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS feedback");
        onCreate(db);
    }

    public void insertFeedback(String name, String phone, int overall, int atmosphere,
                               int staff, int availability, int quality, int billing,
                               int location, String comments) {
        ContentValues cv = new ContentValues();
        cv.put("name", name);
        cv.put("phone", phone);
        cv.put("overall", overall);
        cv.put("atmosphere", atmosphere);
        cv.put("staff", staff);
        cv.put("availability", availability);
        cv.put("quality", quality);
        cv.put("billing", billing);
        cv.put("location", location);
        cv.put("comments", comments);
        getWritableDatabase().insert("feedback", null, cv);
    }
}
