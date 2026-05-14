package in.mil.iaf.sulur.canteenfeedback;

import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.RatingBar;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;

public class MainActivity extends AppCompatActivity {

    private FeedbackDbHelper dbHelper;
    private EditText editName, editPhone, editComments;
    private RatingBar ratingOverall, ratingAtmosphere, ratingStaff,
            ratingAvailability, ratingQuality, ratingBilling, ratingLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dbHelper = new FeedbackDbHelper(this);

        editName = findViewById(R.id.editName);
        editPhone = findViewById(R.id.editPhone);
        editComments = findViewById(R.id.editComments);
        ratingOverall = findViewById(R.id.ratingOverall);
        ratingAtmosphere = findViewById(R.id.ratingAtmosphere);
        ratingStaff = findViewById(R.id.ratingStaff);
        ratingAvailability = findViewById(R.id.ratingAvailability);
        ratingQuality = findViewById(R.id.ratingQuality);
        ratingBilling = findViewById(R.id.ratingBilling);
        ratingLocation = findViewById(R.id.ratingLocation);

        MaterialButton btnSubmit = findViewById(R.id.btnSubmit);
        btnSubmit.setOnClickListener(v -> submitFeedback());

        findViewById(R.id.txtHeader).setOnLongClickListener(v -> {
            startActivity(new Intent(this, AdminActivity.class));
            return true;
        });
    }

    private void submitFeedback() {
        String name = editName.getText().toString().trim();
        String phone = editPhone.getText().toString().trim();
        String comments = editComments.getText().toString().trim();

        if (name.isEmpty()) {
            editName.setError("Please enter your name");
            return;
        }

        int overall = (int) ratingOverall.getRating();
        int atmosphere = (int) ratingAtmosphere.getRating();
        int staff = (int) ratingStaff.getRating();
        int availability = (int) ratingAvailability.getRating();
        int quality = (int) ratingQuality.getRating();
        int billing = (int) ratingBilling.getRating();
        int location = (int) ratingLocation.getRating();

        dbHelper.insertFeedback(name, phone, overall, atmosphere, staff,
                availability, quality, billing, location, comments);

        String summary = "Thank you, " + name + "!\n\n" +
                "Overall: " + overall + "/5\n" +
                "Atmosphere: " + atmosphere + "/5\n" +
                "Staff: " + staff + "/5\n" +
                "Availability: " + availability + "/5\n" +
                "Quality: " + quality + "/5\n" +
                "Billing: " + billing + "/5\n" +
                "Location: " + location + "/5";

        new AlertDialog.Builder(this)
                .setTitle("Feedback Submitted")
                .setMessage(summary)
                .setPositiveButton("OK", (d, w) -> resetForm())
                .show();
    }

    private void resetForm() {
        editName.setText("");
        editPhone.setText("");
        editComments.setText("");
        ratingOverall.setRating(0);
        ratingAtmosphere.setRating(0);
        ratingStaff.setRating(0);
        ratingAvailability.setRating(0);
        ratingQuality.setRating(0);
        ratingBilling.setRating(0);
        ratingLocation.setRating(0);
        Toast.makeText(this, "Feedback recorded. Jai Hind!", Toast.LENGTH_SHORT).show();
    }
}
