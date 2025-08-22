package com.offsec.nethunter;

import android.app.Activity;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;

import com.offsec.nethunter.utils.NhPaths;
import com.offsec.nethunter.utils.ShellExecuter;

import java.util.Locale;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NavUtils;
import androidx.core.content.ContextCompat;

public class EditSourceActivity extends AppCompatActivity {
    private String configFilePath = "";
    private Activity activity;
    private final ShellExecuter exe = new ShellExecuter();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activity = this;
        Bundle b = getIntent().getExtras();
        assert b != null;
        configFilePath = b.getString("path");
        setContentView(R.layout.source);
        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.darkTitle));

        EditText source = findViewById(R.id.source);
        source.setText(String.format(Locale.getDefault(),getString(R.string.loading_file), configFilePath));
        exe.ReadFile_ASYNC(configFilePath, source);

        ActionBar ab = getSupportActionBar();
        if (ab != null) {
            ab.setDisplayHomeAsUpEnabled(true);
        }
        NhPaths.showMessage(activity, "文件已加载");
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            NavUtils.navigateUpFromSameTask(this);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void updateSource(View view) {
        EditText source = findViewById(R.id.source);
        String newSource = source.getText().toString();
        boolean isSaved = exe.SaveFileContents(newSource, configFilePath);
        if (isSaved) {
            NhPaths.showMessage(activity,"源代码已更新");
        } else {
            NhPaths.showMessage(activity,"源代码未更新");
        }
    }
}