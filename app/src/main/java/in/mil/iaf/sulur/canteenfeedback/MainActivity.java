package in.mil.iaf.sulur.canteenfeedback;

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
    private RatingBar ratingFood, ratingService, ratingCleanliness, ratingValue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dbHelper = new FeedbackDbHelper(this);

        editName = findViewById(R.id.editName);
        editPhone = findViewById(R.id.editPhone);
        editComments = findViewById(R.id.editComments);
        ratingFood = findViewById(R.id.ratingFood);
        ratingService = findViewById(R.id.ratingService);
        ratingCleanliness = findViewById(R.id.ratingCleanliness);
        ratingValue = findViewById(R.id.ratingValue);

        MaterialButton btnSubmit = findViewById(R.id.btnSubmit);
        btnSubmit.setOnClickListener(v -> submitFeedback());
    }

    private void submitFeedback() {
        String name = editName.getText().toString().trim();
        String phone = editPhone.getText().toString().trim();
        String comments = editComments.getText().toString().trim();

        if (name.isEmpty()) {
            editName.setError("Please enter your name");
            return;
        }

        int food = (int) ratingFood.getRating();
        int service = (int) ratingService.getRating();
        int cleanliness = (int) ratingCleanliness.getRating();
        int value = (int) ratingValue.getRating();

        dbHelper.insertFeedback(name, phone, food, service, cleanliness, value, comments);

        String summary = "Thank you, " + name + "!\n\n" +
                "Food Quality: " + food + "/5\n" +
                "Service: " + service + "/5\n" +
                "Cleanliness: " + cleanliness + "/5\n" +
                "Value for Money: " + value + "/5\n" +
                (comments.isEmpty() ? "" : "\nComments: " + comments);

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
        ratingFood.setRating(0);
        ratingService.setRating(0);
        ratingCleanliness.setRating(0);
        ratingValue.setRating(0);
        Toast.makeText(this, "Feedback recorded. Jai Hind!", Toast.LENGTH_SHORT).show();
    }
}
