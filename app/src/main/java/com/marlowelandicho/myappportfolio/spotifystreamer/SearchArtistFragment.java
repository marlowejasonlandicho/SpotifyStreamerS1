package com.marlowelandicho.myappportfolio.spotifystreamer;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.marlowelandicho.myappportfolio.spotifystreamer.data.SpotifyStreamerArtist;
import com.marlowelandicho.myappportfolio.spotifystreamer.data.SpotifyStreamerResult;

import java.util.ArrayList;
import java.util.List;

import kaaes.spotify.webapi.android.SpotifyApi;
import kaaes.spotify.webapi.android.SpotifyService;
import kaaes.spotify.webapi.android.models.Artist;
import kaaes.spotify.webapi.android.models.ArtistsPager;
import kaaes.spotify.webapi.android.models.Image;


/**
 * A placeholder fragment containing a simple view.
 */
public class SearchArtistFragment extends Fragment {

    private ArtistAdapter artistAdapter;
    private List<SpotifyStreamerArtist> searchArtistResultList = new ArrayList<>();
    private String q;

    public SearchArtistFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_artist_search, container, false);
        ListView listView = (ListView) rootView.findViewById(R.id.list_view_artist_search_result);
        EditText inputArtistNameTextView = (EditText) rootView.findViewById(R.id.input_artist_name);

        String localQ = SpotifyStreamerResult.getQueryString();
        if (localQ != null) {
            inputArtistNameTextView.setText(localQ);
        } else {
            inputArtistNameTextView.requestFocus();
        }

        inputArtistNameTextView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    q = v.getText().toString();
                    if (q != null && q.length() > 0) {
                        SpotifyStreamerResult.clearSearchArtistResults();
                        updateArtistResult(q);
                        return false;
                    }
                }
                return false;
            }
        });

        artistAdapter = new ArtistAdapter(getActivity().getApplicationContext(), searchArtistResultList);
        listView.setAdapter(artistAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                SpotifyStreamerArtist selectedArtist = (SpotifyStreamerArtist) parent.getItemAtPosition(position);
                Intent trackListActivityIntent =
                        new Intent(getActivity(), TrackListActivity.class).putExtra(Intent.EXTRA_TEXT, selectedArtist.getArtistId());
                startActivity(trackListActivityIntent);
            }
        });

        return rootView;
    }

    public void updateArtistResult(String q) {
        SearchArtistTask searchArtistTask = new SearchArtistTask();
        searchArtistTask.execute(q);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onPause() {
        super.onPause();

        ListView listView = (ListView) getActivity().findViewById(R.id.list_view_artist_search_result);
        int firstVisiblePosition = listView.getFirstVisiblePosition();
        SpotifyStreamerResult.setFirstVisiblePosition(firstVisiblePosition);
        SpotifyStreamerResult.setQueryString(q);
        SpotifyStreamerResult.getArtists().addAll(searchArtistResultList);
    }

    @Override
    public void onResume() {
        super.onResume();
        q = SpotifyStreamerResult.getQueryString();
        ListView listView = (ListView) getActivity().findViewById(R.id.list_view_artist_search_result);
        listView.setSelection(SpotifyStreamerResult.getFirstVisiblePosition());
        listView.requestFocus();
        searchArtistResultList.clear();
        searchArtistResultList.addAll(SpotifyStreamerResult.getArtists());
        artistAdapter.notifyDataSetChanged();

    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    @Override
    public void onViewStateRestored(Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);
    }

    public class SearchArtistTask extends AsyncTask<String, Void, List<SpotifyStreamerArtist>> {

        private final String LOG_TAG = SearchArtistTask.class.getSimpleName();

        public SearchArtistTask() {
        }

        @Override
        protected void onPostExecute(List<SpotifyStreamerArtist> result) {
            artistAdapter.clear();
            if (result == null || result.isEmpty()) {
                int duration = Toast.LENGTH_SHORT;
                Toast toast = Toast.makeText(getActivity().getApplicationContext(), getString(R.string.artist_no_result), duration);
                toast.show();
            } else {
                for (SpotifyStreamerArtist spotifyStreamerArtist : result) {
                    artistAdapter.add(spotifyStreamerArtist);
                }
            }
            artistAdapter.notifyDataSetChanged();

        }

        @Override
        protected List<SpotifyStreamerArtist> doInBackground(String... params) {
            List<SpotifyStreamerArtist> spotifyStreamerArtistList = new ArrayList<>();
            if (params == null) {
                return searchArtistResultList;
            }
            String artistQuery = params[0];

            if (artistQuery == null || artistQuery.length() == 0) {
                return searchArtistResultList;
            }
            try {
                SpotifyApi api = new SpotifyApi();
                SpotifyService spotifyService = api.getService();

                ArtistsPager artistsPager = spotifyService.searchArtists(artistQuery);
                List<Artist> artistList = artistsPager.artists.items;
                for (Artist artist : artistList) {
                    SpotifyStreamerArtist spotifyStreamerArtist = new SpotifyStreamerArtist();
                    spotifyStreamerArtist.setArtistId(artist.id);
                    spotifyStreamerArtist.setArtistName(artist.name);
                    for (Image image : artist.images) {
                        if (image.height <= 200 && image.url != null) {
                            spotifyStreamerArtist.setThumbnailUrl(image.url);
                            break;
                        }
                    }
                    spotifyStreamerArtistList.add(spotifyStreamerArtist);
                }

            } catch (Exception e) {
                Log.e(LOG_TAG, e.getMessage(), e);
            }
            searchArtistResultList = spotifyStreamerArtistList;
            return spotifyStreamerArtistList;
        }
    }


}
