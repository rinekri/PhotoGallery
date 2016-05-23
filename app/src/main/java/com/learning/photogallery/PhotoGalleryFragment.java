package com.learning.photogallery;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.learning.photogallery.gallery.GalleryItem;

import java.lang.reflect.Array;
import java.util.ArrayList;

public class PhotoGalleryFragment extends Fragment {
    public static final String TAG = "PhotoGalleryFragment";
    public static final int PHOTO_GALLERY_GRID_COUNT = 2;

    private static final String ARG_COLUMN_COUNT = "column-count";
    private int mColumnCount = 2;
    private OnListFragmentInteractionListener mListener;
    private RecyclerView mRecyclerView;
    private ArrayList<GalleryItem> mItems;
    private FlickrFetchr mFlickrFetchr;

    private FlickrFetchr.FetchingType currentType;

    public PhotoGalleryFragment() {
    }

    public static PhotoGalleryFragment newInstance(int columnCount) {
        PhotoGalleryFragment fragment = new PhotoGalleryFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_COLUMN_COUNT, columnCount);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        if (getArguments() != null) {
            mColumnCount = getArguments().getInt(ARG_COLUMN_COUNT);
        }

//        mItems = new ArrayList<>();
        mFlickrFetchr = new FlickrFetchr();
        getItemsFromFlickr();
        currentType = FlickrFetchr.FetchingType.RECENT;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_flickrphoto_list, container, false);

        if (view instanceof RecyclerView) {
            Context context = view.getContext();
            mRecyclerView = (RecyclerView) view;
            setupRecyclerView(context);
        }

        return view;
    }


    private void setupRecyclerViewPagination(RecyclerView.LayoutManager layoutManager) {
        if (layoutManager instanceof LinearLayoutManager) {
            LinearLayoutManager linearLayoutManager = (LinearLayoutManager) layoutManager;
            mRecyclerView.addOnScrollListener(new EndlessRecyclerViewScrollListener(linearLayoutManager) {
                @Override
                public void onLoadMore(int page, int totalItemsCount) {
                    if (currentType == FlickrFetchr.FetchingType.RECENT) getItemsFromFlickr();
                }
            });
        } else if (layoutManager instanceof GridLayoutManager) {
            GridLayoutManager gridLayoutManager = (GridLayoutManager) layoutManager;
            mRecyclerView.addOnScrollListener(new EndlessRecyclerViewScrollListener(gridLayoutManager) {
                @Override
                public void onLoadMore(int page, int totalItemsCount) {
                    if (currentType == FlickrFetchr.FetchingType.RECENT) getItemsFromFlickr();
                }
            });
        } else if (layoutManager instanceof StaggeredGridLayoutManager) {
            StaggeredGridLayoutManager staggeredGridLayoutManager = (StaggeredGridLayoutManager) layoutManager;
            mRecyclerView.addOnScrollListener(new EndlessRecyclerViewScrollListener(staggeredGridLayoutManager) {
                @Override
                public void onLoadMore(int page, int totalItemsCount) {
                    if (currentType == FlickrFetchr.FetchingType.RECENT) getItemsFromFlickr();
                }
            });
        }
    }

    public void getItemsFromFlickr() {
        new FetchItemsTask().execute();
    }

    private void setupRecyclerView(Context context) {
        if (getActivity() == null || mRecyclerView == null) return;

        setLayoutManager(context);
        setAdapter(new ArrayList<GalleryItem>());
        setupRecyclerViewPagination(mRecyclerView.getLayoutManager());
    }

    private void setLayoutManager(Context context) {
        if (mColumnCount <= 1) {
            mRecyclerView.setLayoutManager(new LinearLayoutManager(context));
        } else {
            mRecyclerView.setLayoutManager(new GridLayoutManager(context, mColumnCount));
        }
    }

    private void setAdapter(ArrayList<GalleryItem> items) {
        mRecyclerView.setAdapter(new MyFlickrPhotoRecyclerViewAdapter(items, mListener));
    }

    private void addItemsIntoAdapter(ArrayList<GalleryItem> items) {
        ((MyFlickrPhotoRecyclerViewAdapter) mRecyclerView.getAdapter()).addItems(items);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnListFragmentInteractionListener) {
            mListener = (OnListFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnListFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    private class FetchItemsTask extends AsyncTask<Void, Void, ArrayList<GalleryItem>> {

        @Override
        protected ArrayList<GalleryItem> doInBackground(Void... params) {
            Activity activity = getActivity();

            if (activity == null) return new ArrayList<GalleryItem>();

            String query = PreferenceManager.getDefaultSharedPreferences(activity)
                    .getString(FlickrFetchr.PREF_SEARCH_QUERY, null);

            if (query != null) {
                currentType = FlickrFetchr.FetchingType.SEARCH;
                return new FlickrFetchr().search(query);
            } else {
                currentType = FlickrFetchr.FetchingType.RECENT;
                return mFlickrFetchr.fetchItems();
            }
        }

        @Override
        protected void onPostExecute(ArrayList<GalleryItem> galleryItems) {
            Log.i(TAG, "Returned items: "+galleryItems.size());
//            mItems = galleryItems;

            if (galleryItems.size() != 0) {
                if (currentType == FlickrFetchr.FetchingType.RECENT) {
                    updateAdapter(galleryItems);
                    mFlickrFetchr.increaseCurrentPageAtOne();
                } else {
                    setAdapter(galleryItems);
                }
            }
        }
    }

    private void updateAdapter(ArrayList<GalleryItem> items) {
        if (mFlickrFetchr.getCurrentPage() == 1) {
            setAdapter(items);
        } else {
            addItemsIntoAdapter(items);
        }
//        mItems = null;
    }

    public void clearAdapter() {
        ((MyFlickrPhotoRecyclerViewAdapter) mRecyclerView.getAdapter()).clearItems();
    }

    public interface OnListFragmentInteractionListener {
        void onListFragmentInteraction(GalleryItem item);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.fragment_photo_gallery, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.menu_item_search:
                getActivity().onSearchRequested();
                return true;
            case R.id.menu_item_clear:
                PreferenceManager.getDefaultSharedPreferences(getActivity())
                        .edit()
                        .putString(FlickrFetchr.PREF_SEARCH_QUERY, null)
                        .commit();
                clearAdapter();
                getItemsFromFlickr();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

}
