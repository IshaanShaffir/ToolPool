package com.androidpractice.toolpool;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.regex.Pattern;

public class Register extends AppCompatActivity {
    TextInputEditText editTextEmail, editTextPassword, editTextConfirmPassword, editTextName;
    Button registerButton;
    FirebaseAuth mAuth;
    ProgressBar progressBar;
    TextView textView;
    DatabaseReference databaseReference; // For Realtime Database

    private static final Pattern PASSWORD_PATTERN =
            Pattern.compile("^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[!@#$%^&*()_+=|<>?{}\\[\\]~-]).{8,}$");

    @Override
    public void onStart() {
        super.onStart();
        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            Intent intent = new Intent(getApplicationContext(), homeActivity.class);
            startActivity(intent);
            finish();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_register);
        mAuth = FirebaseAuth.getInstance();
        databaseReference = FirebaseDatabase.getInstance().getReference("users"); // Initialize database reference
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        editTextName = findViewById(R.id.name);
        editTextEmail = findViewById(R.id.email);
        editTextPassword = findViewById(R.id.password);
        editTextConfirmPassword = findViewById(R.id.confirmPass);
        registerButton = findViewById(R.id.registerButton);
        progressBar = findViewById(R.id.progressBar);
        textView = findViewById(R.id.loginNow);

        textView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplicationContext(), Login.class);
                startActivity(intent);
                finish();
            }
        });

        registerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                progressBar.setVisibility(View.VISIBLE);
                String name = String.valueOf(editTextName.getText());
                String email = String.valueOf(editTextEmail.getText());
                String password = String.valueOf(editTextPassword.getText());
                String confirmPass = String.valueOf(editTextConfirmPassword.getText());

                if (TextUtils.isEmpty(name)) {
                    Toast.makeText(Register.this, "Enter name", Toast.LENGTH_SHORT).show();
                    progressBar.setVisibility(View.GONE);
                    return;
                }

                if (TextUtils.isEmpty(email)) {
                    Toast.makeText(Register.this, "Enter email", Toast.LENGTH_SHORT).show();
                    progressBar.setVisibility(View.GONE);
                    return;
                }

                if (TextUtils.isEmpty(password)) {
                    Toast.makeText(Register.this, "Enter password", Toast.LENGTH_SHORT).show();
                    progressBar.setVisibility(View.GONE);
                    return;
                }

                if (TextUtils.isEmpty(confirmPass)) {
                    Toast.makeText(Register.this, "Confirm password", Toast.LENGTH_SHORT).show();
                    progressBar.setVisibility(View.GONE);
                    return;
                }

                if (!password.equals(confirmPass)) {
                    Toast.makeText(Register.this, "Passwords do not match", Toast.LENGTH_SHORT).show();
                    progressBar.setVisibility(View.GONE);
                    return;
                }

                if (!PASSWORD_PATTERN.matcher(password).matches()) {
                    Toast.makeText(Register.this, "Password must be at least 8 characters, with 1 special character, 1 lowercase, 1 uppercase, and 1 number", Toast.LENGTH_LONG).show();
                    progressBar.setVisibility(View.GONE);
                    return;
                }

                mAuth.createUserWithEmailAndPassword(email, password)
                        .addOnCompleteListener(task -> {
                            progressBar.setVisibility(View.GONE);
                            if (task.isSuccessful()) {
                                FirebaseUser user = mAuth.getCurrentUser();
                                if (user != null) {
                                    // Set display name in Firebase Auth
                                    UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                            .setDisplayName(name)
                                            .build();
                                    user.updateProfile(profileUpdates)
                                            .addOnCompleteListener(profileTask -> {
                                                if (!profileTask.isSuccessful()) {
                                                    Toast.makeText(Register.this, "Failed to set name in Auth: " + profileTask.getException().getMessage(), Toast.LENGTH_SHORT).show();
                                                }
                                            });

                                    // Save name to Realtime Database
                                    String uid = user.getUid();
                                    databaseReference.child(uid).child("name").setValue(name)
                                            .addOnCompleteListener(dbTask -> {
                                                if (!dbTask.isSuccessful()) {
                                                    Toast.makeText(Register.this, "Failed to save name to database: " + dbTask.getException().getMessage(), Toast.LENGTH_SHORT).show();
                                                }
                                            });
                                }

                                Toast.makeText(Register.this, "Account Created", Toast.LENGTH_SHORT).show();
                                Intent intent = new Intent(getApplicationContext(), homeActivity.class);
                                startActivity(intent);
                                finish();
                            } else {
                                Toast.makeText(Register.this, "Authentication failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });
            }
        });
    }
}