package com.example.musicsearch;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;

public class MainActivity extends Activity {

	private Spinner spType;
	private Button btnSubmit;
	private EditText etTitle; 

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		etTitle = (EditText) findViewById(R.id.etTitle);
		spType = (Spinner) findViewById(R.id.spType);
		// set adapter of spinner
		String[] type_array = getResources().getStringArray(R.array.type_arrays);
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, type_array);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spType.setAdapter(adapter);

		addListenerOnButton();
	}

	// when button is clicked, send data to MusicSearch activity
	public void addListenerOnButton()
	{
		btnSubmit = (Button) findViewById(R.id.btnSubmit);

		btnSubmit.setOnClickListener(new OnClickListener() 
		{

			@Override
			public void onClick(View v) 
			{
				String type = spType.getSelectedItem().toString();
				String title = etTitle.getText().toString().trim();
				// if the title is null, display message
				if (title.equals("") || title == null)
				{
					Dialog dialog = new Dialog(MainActivity.this);
					dialog.setTitle("Enter something in the search box.");
					dialog.show();
				}
				else
				{
					// Initialize a new intent and send parameters to another activity
					Intent intent = new Intent(MainActivity.this, MusicSearch.class);
					intent.putExtra("type", type);
					intent.putExtra("title", title);
					startActivity(intent);
				}
			}

		});

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) 
	{
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

}
