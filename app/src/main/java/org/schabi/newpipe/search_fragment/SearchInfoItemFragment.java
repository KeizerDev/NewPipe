package org.schabi.newpipe.search_fragment;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import org.schabi.newpipe.ErrorActivity;
import org.schabi.newpipe.R;
import org.schabi.newpipe.extractor.SearchResult;
import org.schabi.newpipe.extractor.ServiceList;

/**
 * Created by Christian Schabesberger on 02.08.16.
 */

public class SearchInfoItemFragment extends Fragment {

    private static final String TAG = SearchInfoItemFragment.class.toString();

    public class SearchQueryListener implements SearchView.OnQueryTextListener {

        @Override
        public boolean onQueryTextSubmit(String query) {
            Activity a = getActivity();
            try {
                searchQuery = query;
                search(query);

                // hide virtual keyboard
                InputMethodManager inputManager =
                        (InputMethodManager) a.getSystemService(Context.INPUT_METHOD_SERVICE);
                try {
                    //noinspection ConstantConditions
                    inputManager.hideSoftInputFromWindow(
                            a.getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
                } catch(NullPointerException e) {
                    e.printStackTrace();
                    ErrorActivity.reportError(a, e, null,
                            a.findViewById(android.R.id.content),
                            ErrorActivity.ErrorInfo.make(ErrorActivity.SEARCHED,
                                    ServiceList.getNameOfService(streamingServiceId),
                                    "Could not get widget with focus", R.string.general_error));
                }
                // clear focus
                // 1. to not open up the keyboard after switching back to this
                // 2. It's a workaround to a seeming bug by the Android OS it self, causing
                //    onQueryTextSubmit to trigger twice when focus is not cleared.
                // See: http://stackoverflow.com/questions/17874951/searchview-onquerytextsubmit-runs-twice-while-i-pressed-once
                a.getCurrentFocus().clearFocus();
            } catch(Exception e) {
                e.printStackTrace();
            }
            View bg = a.findViewById(R.id.mainBG);
            bg.setVisibility(View.GONE);
            return true;
        }

        @Override
        public boolean onQueryTextChange(String newText) {
            if(!newText.isEmpty()) {
                searchSuggestions(newText);
            }
            return true;
        }
    }

    // TODO: Customize parameter argument names
    private static final String ARG_COLUMN_COUNT = "column-count";
    // TODO: Customize parameters
    private int mColumnCount = 1;

    private int streamingServiceId = -1;
    private String searchQuery = "";

    private SearchView searchView = null;
    private SuggestionListAdapter suggestionListAdapter = null;
    private StreamInfoListAdapter streamInfoListAdapter = null;

    // savedInstanceBundle arguments
    private static final String QUERY = "query";
    private static final String STREAMING_SERVICE = "streaming_service";

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public SearchInfoItemFragment() {
    }

    // TODO: Customize parameter initialization
    @SuppressWarnings("unused")
    public static SearchInfoItemFragment newInstance(int columnCount) {
        SearchInfoItemFragment fragment = new SearchInfoItemFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_COLUMN_COUNT, columnCount);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            mColumnCount = getArguments().getInt(ARG_COLUMN_COUNT);
        }

        if(savedInstanceState != null) {
            searchQuery = savedInstanceState.getString(QUERY);
            streamingServiceId = savedInstanceState.getInt(STREAMING_SERVICE);
        } else {
            try {
                streamingServiceId = ServiceList.getIdOfService("Youtube");
            } catch(Exception e) {
                e.printStackTrace();
                ErrorActivity.reportError(getActivity(), e, null,
                        getActivity().findViewById(android.R.id.content),
                        ErrorActivity.ErrorInfo.make(ErrorActivity.SEARCHED,
                                ServiceList.getNameOfService(streamingServiceId),
                                "", R.string.general_error));
            }
        }

        SearchWorker sw = SearchWorker.getInstance();
        sw.setSearchWorkerResultListner(new SearchWorker.SearchWorkerResultListner() {
            @Override
            public void onResult(SearchResult result) {
                streamInfoListAdapter.addVideoList(result.resultList);
            }

            @Override
            public void onNothingFound(int stringResource) {
                //setListShown(true);
                Toast.makeText(getActivity(), getString(stringResource),
                        Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(String message) {
                //setListShown(true);
                Toast.makeText(getActivity(), message,
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_searchinfoitem, container, false);

        // Set the adapter
        if (view instanceof RecyclerView) {
            Context context = view.getContext();
            RecyclerView recyclerView = (RecyclerView) view;
            if (mColumnCount <= 1) {
                recyclerView.setLayoutManager(new LinearLayoutManager(context));
            } else {
                recyclerView.setLayoutManager(new GridLayoutManager(context, mColumnCount));
            }

            streamInfoListAdapter = new StreamInfoListAdapter(getActivity(),
                    getActivity().findViewById(android.R.id.content));
            recyclerView.setAdapter(streamInfoListAdapter);
        }

        return view;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.search_menu, menu);

        MenuItem searchItem = menu.findItem(R.id.action_search);
        searchView = (SearchView) searchItem.getActionView();
        setupSearchView(searchView);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item);
    }

    private void setupSearchView(SearchView searchView) {
        suggestionListAdapter = new SuggestionListAdapter(getActivity());
        searchView.setSuggestionsAdapter(suggestionListAdapter);
        searchView.setOnSuggestionListener(new SearchSuggestionListener(searchView, suggestionListAdapter));
        searchView.setOnQueryTextListener(new SearchQueryListener());
        if(!searchQuery.isEmpty()) {
            searchView.setQuery(searchQuery, false);
            searchView.setIconifiedByDefault(false);
        }
    }

    private void search(String query) {
        streamInfoListAdapter.clearVideoList();
        search(query, 0);
    }

    private void search(String query, int page) {
        SearchWorker sw = SearchWorker.getInstance();
        sw.search(streamingServiceId, query, page, getActivity());
    }

    private void searchSuggestions(String query) {
        SuggestionSearchRunnable suggestionSearchRunnable =
                new SuggestionSearchRunnable(streamingServiceId, query, getActivity(), suggestionListAdapter);
        Thread suggestionThread = new Thread(suggestionSearchRunnable);
        suggestionThread.start();
    }

    private void postNewErrorToast(Handler h, final int stringResource) {
        h.post(new Runnable() {
            @Override
            public void run() {

            }
        });
    }

    private void postNewNothingFoundToast(Handler h, final int stringResource) {
        h.post(new Runnable() {
            @Override
            public void run() {

            }
        });
    }
}