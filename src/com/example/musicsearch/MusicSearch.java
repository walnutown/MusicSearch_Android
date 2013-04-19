package com.example.musicsearch;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.SimpleAdapter.ViewBinder;
import android.widget.TextView;

public class MusicSearch extends Activity {
	private String urlStr;
	private TextView tvCaption;
	private ListView list;
	private ArrayList<Map<String,Object>> list_view = new ArrayList<Map<String, Object>>();
	private boolean list_listener = false;
	private String type = "";
	private String title = "";


	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.music_search);
		tvCaption = (TextView) findViewById(R.id.tvCaption);

		new listBuilder().execute(this);
		Log.d("Debug:", "List_listener: " + list_listener);


	}
	
	public String decodeHtml(String str)
	{
		return Html.fromHtml(str).toString();
	}
	
	// listen to list view to check the selected entry
	public void listenList()
	{
		list.setClickable(true);
		list.setOnItemClickListener(new AdapterView.OnItemClickListener() 
		{
		  @Override
		  public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) 
		  {
			  prePost();
		  }
		});
	}
	
	public void prePost()
	{
		Log.d("Debug:", "List Item is clicked");
		final Dialog post_dialog = new Dialog(this);
		post_dialog.setContentView(R.layout.post_dialog);
		Button btnDialog = (Button) post_dialog.findViewById(R.id.btnDialog);
		btnDialog.setOnClickListener(new View.OnClickListener() 
		{		
			@Override
			public void onClick(View v) 
			{
				post();
			}
		});
		post_dialog.show();
	}
	
	public void post()
	{
		
	}
	
	
	// above Android 3.0, we need to use AsynTask to implement network request
	protected class listBuilder extends  AsyncTask<Context, Integer, String>
	{
		
		@Override
		protected String doInBackground(Context... params)
		{
			String json_string ="";
			try
			{
				// Receiving the parameters from MainActivity
				Intent intent = getIntent();
				type = intent.getExtras().getString("type");
				title = intent.getExtras().getString("title");
				urlStr = "http://cs-server.usc.edu:36710/examples/servlet/HelloWorldExample?title=" + URLEncoder.encode(title,"UTF-8") + "&type=" + type ;

				//initialize http request, to get json stream
				URL url = new URL(urlStr);
				HttpURLConnection uc = (HttpURLConnection)url.openConnection();
				Log.d("Debug","after url connection: " + urlStr);
				uc.setAllowUserInteraction(false);
				InputStream urlStream = uc.getInputStream();
				BufferedReader reader = new BufferedReader(new InputStreamReader(urlStream, "UTF-8"));
				//json_string = Html.fromHtml(reader.readLine()).toString();
				json_string = reader.readLine();
				Log.d("Debug","after bufferedReader: " + json_string);

				// parse json entries
				JSONObject json_doc = new JSONObject(json_string);
				JSONObject json_root = json_doc.getJSONObject("results");
				JSONArray json_entry = json_root.getJSONArray("result");
				//store json entries into list_view array
				Log.d("Debug","length:" + json_entry.length());
				Log.d("Debug","type:" + type);
				if (json_entry.length() == 0)
					return "no_discography";
				else
				{
					list_listener = true;
					for(int i = 0 ; i < json_entry.length() ; i++)
					{
						JSONObject json_temp = (JSONObject)json_entry.get(i);
						Map<String, Object> map = new HashMap<String, Object>();
						// parse artist info
						if(type.equals("artist"))
						{
							
							map.put("Name", decodeHtml((String)json_temp.get("name")));
							map.put("Genre", decodeHtml((String)json_temp.get("genre")));
							map.put("Year", (String)json_temp.get("year"));
							map.put("Details", (String)json_temp.get("details"));
							
							// get image from source webpage and put it into the map
							String imgStr = (String)json_temp.get("image");
							URL imgUrl;
							if (imgStr.equals("NA"))
								imgUrl = new URL("http://cs-server.usc.edu:36709/noImage_artist.png");
							else
								imgUrl = new URL(imgStr);
							
							HttpURLConnection imgUc = (HttpURLConnection) imgUrl.openConnection();
							InputStream imgIs = imgUc.getInputStream();
							map.put("Image",BitmapFactory.decodeStream(imgIs));
						}
						// parse album info
						else if (type.equals("album"))
						{
							map.put("Title",decodeHtml((String)json_temp.get("title")));
							map.put("Artist", decodeHtml((String)json_temp.get("artist")));
							map.put("Genre", decodeHtml((String)json_temp.get("genre")));
							map.put("Year", (String)json_temp.get("year"));
							map.put("Details", (String)json_temp.get("details"));
							
							// get image from source webpage and put it into the map
							String imgStr = (String)json_temp.get("image");
							URL imgUrl;
							if (imgStr.equals("NA"))
								imgUrl = new URL("http://cs-server.usc.edu:36709/noImage_album.png");
							else
								imgUrl = new URL(imgStr);
							
							HttpURLConnection imgUc = (HttpURLConnection) imgUrl.openConnection();
							InputStream imgIs = imgUc.getInputStream();
							map.put("Image",BitmapFactory.decodeStream(imgIs));
						}
						//parse song info
						else
						{
							map.put("Sample", (String)json_temp.get("sample"));
							map.put("Title", decodeHtml((String)json_temp.get("title")));
							map.put("Performer", decodeHtml((String)json_temp.get("performer")));
							map.put("Composer", decodeHtml((String)json_temp.get("composer")));
							map.put("Details", (String)json_temp.get("details"));
							
							// get sample image from source webpage and put it into the map	
							URL	imgUrl = new URL("http://cs-server.usc.edu:36709/noImage_song.png");	
							HttpURLConnection imgUc = (HttpURLConnection) imgUrl.openConnection();
							InputStream imgIs = imgUc.getInputStream();
							map.put("Image",BitmapFactory.decodeStream(imgIs));
						}
						Log.d("Debug", "In each json loop: " + map.toString());
						list_view.add(map);
					}
					return "OK";
				}
			}

			catch (UnsupportedEncodingException e) 
			{
				e.printStackTrace();
				return "UnsupportedEncodingException: " + e.getMessage();
			} 
			catch (MalformedURLException e) 
			{
				e.printStackTrace();
				return "MalformedURLException: " + e.getMessage();
			} 
			catch (IOException e) 
			{
				e.printStackTrace();
				return "IOException: " + e.getMessage();
			} catch (JSONException e) 
			{
				e.printStackTrace();
				return "JSONException: " + e.getMessage();
			}
			//Log.d("Debug:", "List View: " + list_view.toString());
			//return list_view.toString();
		}

		// display list view content 
		@Override
		protected void onPostExecute(String result) 
		{
			Log.d("Debug", "Enter onPostExecute(): " + result);
			list = (ListView) findViewById(R.id.listView);
			SimpleAdapter listAdapter = null;
			String[] columTags = null;
			int[] columIds = null;

			Map<String, String> map = new HashMap<String, String>();
			if (result.equals("no_discography"))
			{
				tvCaption.setText("No Discography Found.");
			}
			// show multiple colums in a row of list
			else if( result.equals("OK") && type.equals("artist"))
			{
				columTags = new String[] {"Image", "Name", "Genre", "Year"};
				columIds = new int[] {R.id.image, R.id.tvName, R.id.tvGenre, R.id.tvYear};
				listAdapter = new SimpleAdapter(MusicSearch.this, list_view, R.layout.list_row_artist, columTags, columIds);
				
				// bind imageView
				listAdapter.setViewBinder(new ViewBinder() 
				{
					@Override
					public boolean setViewValue(View view, Object data,
							String textRepresentation) 
					{
						if (view instanceof ImageView && data instanceof Bitmap) 
						{
							ImageView iv = (ImageView) view;
							iv.setImageBitmap((Bitmap) data);
							return true;
						} 
						else
							return false;
					}
				});
				tvCaption.setVisibility(View.GONE);
				list.setAdapter(listAdapter);
			}
			else if( result.equals("OK") && type.equals("album"))
			{
				columTags = new String[] {"Image", "Title", "Artist", "Genre", "Year"};
				columIds = new int[] {R.id.image, R.id.tvTitle,R.id.tvArtist, R.id.tvGenre, R.id.tvYear};
				listAdapter = new SimpleAdapter(MusicSearch.this, list_view, R.layout.list_row_album, columTags, columIds);
				
				// bind imageView
				listAdapter.setViewBinder(new ViewBinder() 
				{
					@Override
					public boolean setViewValue(View view, Object data,
							String textRepresentation) 
					{
						if (view instanceof ImageView && data instanceof Bitmap) 
						{
							ImageView iv = (ImageView) view;
							iv.setImageBitmap((Bitmap) data);
							return true;
						} 
						else
							return false;
					}
				});
				tvCaption.setVisibility(View.GONE);
				list.setAdapter(listAdapter);
			}
			else if( result.equals("OK") && type.equals("song"))
			{
				columTags = new String[] {"Image", "Title", "Performer", "Composer"};
				columIds = new int[] {R.id.image, R.id.tvTitle, R.id.tvPerformer, R.id.tvComposer};
				listAdapter = new SimpleAdapter(MusicSearch.this, list_view, R.layout.list_row_song, columTags, columIds);
				
				// bind imageView
				listAdapter.setViewBinder(new ViewBinder() 
				{
					@Override
					public boolean setViewValue(View view, Object data,
							String textRepresentation) 
					{
						if (view instanceof ImageView && data instanceof Bitmap) 
						{
							ImageView iv = (ImageView) view;
							iv.setImageBitmap((Bitmap) data);
							return true;
						} 
						else
							return false;
					}
				});
				tvCaption.setVisibility(View.GONE);
				list.setAdapter(listAdapter);
			}
			else
			{
				tvCaption.setText("Error: " + result);
			}
			Log.d("Debug:", "List_listener: " + list_listener);
			if(list_listener)
				listenList();
		}	
	}	
}
