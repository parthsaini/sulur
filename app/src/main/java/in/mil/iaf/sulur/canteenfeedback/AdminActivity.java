package in.mil.iaf.sulur.canteenfeedback;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class AdminActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin);

        SQLiteDatabase db = new FeedbackDbHelper(this).getReadableDatabase();

        // Show averages
        Cursor avg = db.rawQuery("SELECT COUNT(*), AVG(overall), AVG(atmosphere), AVG(staff), " +
                "AVG(availability), AVG(quality), AVG(billing), AVG(location) FROM feedback", null);
        avg.moveToFirst();
        int count = avg.getInt(0);
        ((TextView) findViewById(R.id.txtCount)).setText(count + " responses");
        if (count > 0) {
            String averages = String.format(
                    "Overall: %.1f  |  Atmosphere: %.1f  |  Staff: %.1f\n" +
                    "Availability: %.1f  |  Quality: %.1f\n" +
                    "Billing: %.1f  |  Location: %.1f",
                    avg.getDouble(1), avg.getDouble(2), avg.getDouble(3),
                    avg.getDouble(4), avg.getDouble(5), avg.getDouble(6), avg.getDouble(7));
            ((TextView) findViewById(R.id.txtAverages)).setText(averages);
        }
        avg.close();

        // Load all feedback
        List<String[]> items = new ArrayList<>();
        Cursor c = db.rawQuery("SELECT name, phone, overall, atmosphere, staff, availability, " +
                "quality, billing, location, comments, timestamp FROM feedback ORDER BY timestamp DESC", null);
        while (c.moveToNext()) {
            items.add(new String[]{
                    c.getString(0), c.getString(1),
                    c.getInt(2) + "," + c.getInt(3) + "," + c.getInt(4) + "," +
                            c.getInt(5) + "," + c.getInt(6) + "," + c.getInt(7) + "," + c.getInt(8),
                    c.getString(9), c.getString(10)
            });
        }
        c.close();

        RecyclerView rv = findViewById(R.id.recyclerFeedback);
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(new RecyclerView.Adapter<VH>() {
            @Override
            public VH onCreateViewHolder(ViewGroup parent, int viewType) {
                return new VH(LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_feedback, parent, false));
            }

            @Override
            public void onBindViewHolder(VH h, int pos) {
                String[] item = items.get(pos);
                h.name.setText(item[0] + (item[1].isEmpty() ? "" : " (" + item[1] + ")"));
                String[] r = item[2].split(",");
                h.ratings.setText("Overall:" + r[0] + " Atm:" + r[1] + " Staff:" + r[2] +
                        " Avail:" + r[3] + " Qual:" + r[4] + " Bill:" + r[5] + " Loc:" + r[6]);
                h.comments.setText(item[3].isEmpty() ? "" : "\"" + item[3] + "\"");
                h.comments.setVisibility(item[3].isEmpty() ? View.GONE : View.VISIBLE);
                h.timestamp.setText(item[4]);
            }

            @Override
            public int getItemCount() { return items.size(); }
        });
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView name, ratings, comments, timestamp;
        VH(View v) {
            super(v);
            name = v.findViewById(R.id.txtName);
            ratings = v.findViewById(R.id.txtRatings);
            comments = v.findViewById(R.id.txtComments);
            timestamp = v.findViewById(R.id.txtTimestamp);
        }
    }
}
