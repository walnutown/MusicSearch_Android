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
import android.media.AudioManager;
import android.media.MediaPlayer;
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
import android.widget.Toast;

import com.facebook.FacebookException;
import com.facebook.FacebookOperationCanceledException;
import com.facebook.Request;
import com.facebook.Response;
import com.facebook.Session;
import com.facebook.SessionState;
import com.facebook.model.GraphUser;
import com.facebook.widget.WebDialog;
import com.facebook.widget.WebDialog.OnCompleteListener;

public class MusicSearch extends Activity 
{
	private String urlStr;
	private TextView tvCaption;
	private ListView list;
	private ArrayList<Map<String,Object>> list_view = new ArrayList<Map<String, Object>>();
	private ArrayList<Map<String,Object>> fb_view = new ArrayList<Map<String, Object>>();
	private boolean list_listener = false;
	private String type = "";
	private String title = "";
	private Context thisActivity = this;	
	private int entry_size;
	private int entry_id;
	private MediaPlayer mp;
	private String sampleUrl;


	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.music_search);
		tvCaption = (TextView) findViewById(R.id.tvCaption);
		new listBuilder().execute(this);
	}

	//------------------ Utility -------------------------------------------------------------------//

	// decode json html string 
	public String decodeHtml(String str)
	{
		return Html.fromHtml(str).toString();
	}

	//------------------ Methods for posting feed and playing sample--------------------------------//

	// listen to list view to check the selected entry
	public void listenList()
	{
		list.setClickable(true);
		list.setOnItemClickListener(new AdapterView.OnItemClickListener() 
		{
			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int position, long id) 
			{
				entry_id = (int)id;
				prePost();
			}
		});
	}
	// display post dialog 
	public void prePost()
	{
		final Dialog dialog = new Dialog(this);
		dialog.setTitle("Post to Facebook");

		// display post dialog according to different type
		if (type.equals("song"))
		{
			dialog.setContentView(R.layout.post_dialog_song);
			sampleUrl = (String)(fb_view.get(entry_id).get("Sample"));
			if (!sampleUrl.equals("NA"))
			{
				// set "sample music" button onClickListener
				Button btnDialog_sample = (Button) dialog.findViewById(R.id.btnDialog_sample);
				btnDialog_sample.setOnClickListener(new View.OnClickListener() 
				{		
					@Override
					public void onClick(View v) 
					{
						playSample();
					}
				});
			}
			// set "Facebook" button onClickListener
			Button btnDialog_fb = (Button) dialog.findViewById(R.id.btnDialog_fb);
			btnDialog_fb.setOnClickListener(new View.OnClickListener() 
			{		
				@Override
				public void onClick(View v) 
				{
					dialog.dismiss();
					onPost();
				}
			});
		}
		else
		{
			dialog.setContentView(R.layout.post_dialog);
			// set "Facebook" button onClickListener
			Button btnDialog = (Button) dialog.findViewById(R.id.btnDialog);
			btnDialog.setOnClickListener(new View.OnClickListener() 
			{		
				@Override
				public void onClick(View v) 
				{
					dialog.dismiss();
					onPost();
				}
			});
		}

		dialog.show();
	}
	// initialize facebook session
	public void onPost()
	{
		// close media player if it is playing
		if (mp != null) 
		{
			mp.stop();
			mp.release();
		}
		// start Facebook Login
		Session.openActiveSession(this, true, new Session.StatusCallback() {

			// callback when session changes state
			@Override
			public void call(Session session, SessionState state, Exception exception) {
				if (session.isOpened()) {

					// make request to the /me API
					Request.executeMeRequestAsync(session, new Request.GraphUserCallback() {

						// callback after Graph API response with user object
						@Override
						public void onCompleted(GraphUser user, Response response) {
							if (user != null) {
								postFeed();
							}
						}
					});
				}
			}
		});
	}
	// facebook session related method
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		super.onActivityResult(requestCode, resultCode, data);
		Session.getActiveSession().onActivityResult(this, requestCode, resultCode, data);
	}
	// call feed dialog and post to facebook
	public void postFeed()
	{
		// initialize facebook feed dialog parameters
		Bundle params = new Bundle();
		if(type.equals("artist"))
		{
			String nameStr = (String)fb_view.get(entry_id).get("Name");
			String yearStr = (String)fb_view.get(entry_id).get("Year");
			String genreStr = (String)fb_view.get(entry_id).get("Genre");
			String detailStr = (String)fb_view.get(entry_id).get("Details");
			String imageStr = (String)fb_view.get(entry_id).get("Image");
			params.putString("name", nameStr);
			params.putString("caption", "I like " + nameStr + " who is active since year " + yearStr);
			params.putString("description", "Genre of Music is: " + genreStr);
			params.putString("link", detailStr);
			params.putString("picture", imageStr);

			JSONObject prop=new JSONObject();
			try 
			{
				prop.put("Look at details ",(new JSONObject().put("text","here")).put("href",detailStr));
			} 
			catch (JSONException e) 
			{
				e.printStackTrace();
			}
			params.putString("properties",prop.toString());
		}
		else if(type.equals("album"))
		{
			String titleStr = (String)fb_view.get(entry_id).get("Title");
			String artistStr = (String)fb_view.get(entry_id).get("Artist");
			String yearStr = (String)fb_view.get(entry_id).get("Year");
			String genreStr = (String)fb_view.get(entry_id).get("Genre");
			String detailStr = (String)fb_view.get(entry_id).get("Details");
			String imageStr = (String)fb_view.get(entry_id).get("Image");
			params.putString("name", titleStr);
			params.putString("caption", "I like " + titleStr + " released in " + yearStr);
			params.putString("description", "Artist: " + artistStr + " Genre: " + genreStr);
			params.putString("link", detailStr);
			params.putString("picture", imageStr);

			JSONObject prop=new JSONObject();
			try 
			{
				prop.put("Look at details ",(new JSONObject().put("text","here")).put("href",detailStr));
			} 
			catch (JSONException e) 
			{
				e.printStackTrace();
			}
			params.putString("properties",prop.toString());
		}
		else
		{
			String titleStr = (String)fb_view.get(entry_id).get("Title");
			String composerStr = (String)fb_view.get(entry_id).get("Composer");
			String performerStr = (String)fb_view.get(entry_id).get("Performer");
			String detailStr = (String)fb_view.get(entry_id).get("Details");
			String imageStr = (String)fb_view.get(entry_id).get("Image");
			params.putString("name", titleStr);
			params.putString("caption", "I like " + titleStr + " composed by " + composerStr);
			params.putString("description", "Performer: " + performerStr);
			params.putString("link", detailStr);
			params.putString("picture", imageStr);

			JSONObject prop=new JSONObject();
			try 
			{
				prop.put("Look at details ",(new JSONObject().put("text","here")).put("href",detailStr));
			} 
			catch (JSONException e) 
			{
				e.printStackTrace();
			}
			params.putString("properties",prop.toString());
		}
		
		//initialize feed dialog, modified from facebook tutorial example
		WebDialog feedDialog = (
				new WebDialog.FeedDialogBuilder(this,
						Session.getActiveSession(),
						params))
						.setOnCompleteListener(new OnCompleteListener() {

							@Override
							public void onComplete(Bundle values,
									FacebookException error) {
								if (error == null) {
									// When the story is posted, echo the success
									// and the post Id.
									final String postId = values.getString("post_id");
									if (postId != null) {
										// replace getActivity() to global var thisActivity to erase bug 
										Toast.makeText(thisActivity,
												"Posted story, id: "+postId,
												Toast.LENGTH_SHORT).show();
									} else {
										// User clicked the Cancel button
										Toast.makeText(thisActivity.getApplicationContext(), 
												"Publish cancelled", 
												Toast.LENGTH_SHORT).show();
									}
								} else if (error instanceof FacebookOperationCanceledException) {
									// User clicked the "x" button
									Toast.makeText(thisActivity.getApplicationContext(), 
											"Publish cancelled", 
											Toast.LENGTH_SHORT).show();
								} else {
									// Generic, ex: network error
									Toast.makeText(thisActivity.getApplicationContext(), 
											"Error posting story", 
											Toast.LENGTH_SHORT).show();
								}
							}

						})
						.build();
		feedDialog.show();
	}
	// play the sample song when button is pressed, run a new thread to play music
	public void playSample()
	{
		try 
		{
			// Create a new media player and set the listeners
			mp = new MediaPlayer();
			mp.setAudioStreamType(AudioManager.STREAM_MUSIC);
			// Set the data source in another thread
			// which actually downloads the mp3 to a temporary location
			Runnable r = new Runnable() 
			{
				public void run() 
				{
					try 
					{
						//downloadSample();
						mp.setDataSource(sampleUrl);
						mp.prepare();
						mp.start();
					} 
					catch (IOException e) 
					{
						e.printStackTrace();
					}
					catch (IllegalStateException e) 
					{
						e.printStackTrace();
					} 
				}
			};
			new Thread(r).start();
		} 
		catch (Exception e) 
		{
			if (mp != null) 
			{
				mp.stop();
				mp.release();
			}
		}
	}

	//------------------ Build the list view ------------------------------------------------------//

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
				entry_size = json_entry.length();

				//store json entries into list_view array
				Log.d("Debug","length:" + json_entry.length());
				Log.d("Debug","type:" + type);
				if (entry_size == 0)
					return "no_discography";
				else
				{
					list_listener = true;
					for(int i = 0 ; i < entry_size ; i++)
					{
						JSONObject json_temp = (JSONObject)json_entry.get(i);
						Map<String, Object> map = new HashMap<String, Object>();
						Map<String, Object> fbMap = new HashMap<String, Object>();
						// parse artist info
						if(type.equals("artist"))
						{
							/*------------store to list map------------------ */
							map.put("Name", "Name: " + decodeHtml((String)json_temp.get("name")));
							map.put("Genre", "Genre: " + decodeHtml((String)json_temp.get("genre")));
							map.put("Year", "Year:" + (String)json_temp.get("year"));
							// get image from source webpage and put it into the map
							String imgStr = (String)json_temp.get("image");

							if (imgStr.equals("NA"))
								imgStr = "http://cs-server.usc.edu:36709/noImage_artist.png";
							URL imgUrl = new URL(imgStr);
							HttpURLConnection imgUc = (HttpURLConnection) imgUrl.openConnection();
							InputStream imgIs = imgUc.getInputStream();
							map.put("Image",BitmapFactory.decodeStream(imgIs));

							/*-------------store to fb  map----------------*/
							fbMap.put("Image", imgStr);
							fbMap.put("Name", decodeHtml((String)json_temp.get("name")));
							fbMap.put("Genre", decodeHtml((String)json_temp.get("genre")));
							fbMap.put("Year", (String)json_temp.get("year"));
							fbMap.put("Details", (String)json_temp.get("details"));
						}
						// parse album info
						else if (type.equals("album"))
						{
							/*------------store to list map------------------ */
							map.put("Title","Title: " + decodeHtml((String)json_temp.get("title")));
							map.put("Artist", "Artist: " + decodeHtml((String)json_temp.get("artist")));
							map.put("Genre", "Genre: " + decodeHtml((String)json_temp.get("genre")));
							map.put("Year", "Year:" + (String)json_temp.get("year"));
							// get image from source webpage and put it into the map
							String imgStr = (String)json_temp.get("image");

							if (imgStr.equals("NA"))
								imgStr = "http://cs-server.usc.edu:36709/noImage_album.png";
							URL imgUrl = new URL(imgStr);
							HttpURLConnection imgUc = (HttpURLConnection) imgUrl.openConnection();
							InputStream imgIs = imgUc.getInputStream();
							map.put("Image",BitmapFactory.decodeStream(imgIs));

							/*-------------store to fb  map----------------*/
							fbMap.put("Image", imgStr);
							fbMap.put("Title",decodeHtml((String)json_temp.get("title")));
							fbMap.put("Artist", decodeHtml((String)json_temp.get("artist")));
							fbMap.put("Genre", decodeHtml((String)json_temp.get("genre")));
							fbMap.put("Year", (String)json_temp.get("year"));
							fbMap.put("Details", (String)json_temp.get("details"));

						}
						//parse song info
						else
						{
							/*------------store to list map------------------ */
							map.put("Sample", (String)json_temp.get("sample"));
							map.put("Title", "Title:" + decodeHtml((String)json_temp.get("title")));
							map.put("Performer", "Performer: " + decodeHtml((String)json_temp.get("performer")));
							map.put("Composer", "Composer:" + decodeHtml((String)json_temp.get("composer")));
							// get sample image from source webpage and put it into the map	
							URL	imgUrl = new URL("http://cs-server.usc.edu:36709/noImage_song.png");	
							HttpURLConnection imgUc = (HttpURLConnection) imgUrl.openConnection();
							InputStream imgIs = imgUc.getInputStream();
							map.put("Image",BitmapFactory.decodeStream(imgIs));

							/*-------------store to fb  map----------------*/
							fbMap.put("Image", "http://cs-server.usc.edu:36709/noImage_song.png");
							fbMap.put("Sample", (String)json_temp.get("sample"));
							fbMap.put("Title", decodeHtml((String)json_temp.get("title")));
							fbMap.put("Performer", decodeHtml((String)json_temp.get("performer")));
							fbMap.put("Composer", decodeHtml((String)json_temp.get("composer")));
							fbMap.put("Details", (String)json_temp.get("details"));

						}
						list_view.add(map);
						fb_view.add(fbMap);
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
		}

		// display list view content 
		@Override
		protected void onPostExecute(String result) 
		{
			list = (ListView) findViewById(R.id.listView);
			SimpleAdapter listAdapter = null;
			String[] columTags = null;
			int[] columIds = null;

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

			if(list_listener)
			{
				listenList();
			}
		}	
	}	
}
