package org.oucho.musicplayer.activities;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.oucho.musicplayer.filepicker.FilePicker;
import org.oucho.musicplayer.filepicker.FilePickerParcelObject;
import org.oucho.musicplayer.images.FilePickCache;
import org.oucho.musicplayer.R;

public class FilePickerActivity extends AppCompatActivity {

	final private String[] mImagesExtensions = { "jpeg", "jpg", "png", "gif", "bmp", "wbmp" };


	private boolean mOptOnlyOneItem = false;
	private List<String> mOptFilterExclude;
	private List<String> mOptFilterListed;
	private int mOptChoiceType;
	private int mOptSortType;

	//private LruCache<String, Bitmap> mBitmapsCache;

	private AbsListView mAbsListView;
	private View mEmptyView;
	private ArrayList<File> mFilesList = new ArrayList<>();
	private ArrayList<String> mSelected = new ArrayList<>();
	private final HashMap<String, Integer> mListPositioins = new HashMap<>();
	private File mCurrentDirectory;
	private boolean mIsMultiChoice = false;
	private ImageButton mChangeView;
	private TextView mHeaderTitle;
	
	@SuppressLint("InflateParams")
    @Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);


		setContentView(R.layout.file_picker_main);


		Intent intent = getIntent();

		mOptOnlyOneItem = intent.getBooleanExtra(FilePicker.SET_ONLY_ONE_ITEM, false);

		if (intent.hasExtra(FilePicker.SET_FILTER_EXCLUDE)) {
			mOptFilterExclude = Arrays.asList(intent.getStringArrayExtra(FilePicker.SET_FILTER_EXCLUDE));
		}

		if (intent.hasExtra(FilePicker.SET_FILTER_LISTED)) {
			mOptFilterListed = Arrays.asList(intent.getStringArrayExtra(FilePicker.SET_FILTER_LISTED));
		}

		mOptChoiceType = intent.getIntExtra(FilePicker.SET_CHOICE_TYPE, FilePicker.CHOICE_TYPE_ALL);

		mOptSortType = intent.getIntExtra(FilePicker.SET_SORT_TYPE, FilePicker.SORT_NAME_ASC);

		mEmptyView = getLayoutInflater().inflate(R.layout.file_picker_empty, null);
		addContentView(mEmptyView, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

		setAbsListView();
		showSecondHeader(false);

		File path = null;

		if (intent.hasExtra(FilePicker.SET_START_DIRECTORY)) {
			String startPath = intent.getStringExtra(FilePicker.SET_START_DIRECTORY);
			if (startPath != null && startPath.length() > 0) {
				File tmp = new File(startPath);
				if (tmp.exists() && tmp.isDirectory()) path = tmp;
			}
		}

		if (path == null) {
			path = new File("/");

			if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED))
                path = Environment.getExternalStorageDirectory();

		}

		readDirectory(path);

		mHeaderTitle = (TextView) findViewById(R.id.title);
		updateTitle();



		ImageButton sort1 = (ImageButton) findViewById(R.id.menu_sort1);
		ImageButton sort2 = (ImageButton) findViewById(R.id.menu_sort2);
		if (!intent.getBooleanExtra(FilePicker.DISABLE_SORT_BUTTON, false)) {
			OnClickListener listener = new OnClickListener() {
				@Override
				public void onClick(View v) {
					AlertDialog.Builder alert = new AlertDialog.Builder(FilePickerActivity.this);
					alert.setTitle(R.string.fp__sort);
					alert.setItems(R.array.fp__sorting_types, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							switch (which) {
							case 0:
								mOptSortType = FilePicker.SORT_NAME_ASC;
								break;
							case 1:
								mOptSortType = FilePicker.SORT_NAME_DESC;
								break;
							case 2:
								mOptSortType = FilePicker.SORT_SIZE_ASC;
								break;
							case 3:
								mOptSortType = FilePicker.SORT_SIZE_DESC;
								break;
							case 4:
								mOptSortType = FilePicker.SORT_DATE_ASC;
								break;
							case 5:
								mOptSortType = FilePicker.SORT_DATE_DESC;
								break;

							}
							sort();
						}
					});
					alert.show();
				}
			};
            assert sort1 != null;
            sort1.setOnClickListener(listener);
            assert sort2 != null;
            sort2.setOnClickListener(listener);
		} else {
            assert sort1 != null;
            sort1.setVisibility(ImageButton.GONE);
            assert sort2 != null;
            sort2.setVisibility(ImageButton.GONE);
		}

		mChangeView = (ImageButton) findViewById(R.id.menu_change_view);
		setMenuItemView();
		mChangeView.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				setAbsListView();
				setMenuItemView();
			}
		});

		ImageButton cancel1 = (ImageButton) findViewById(R.id.menu_cancel1);
		if (intent.getBooleanExtra(FilePicker.ENABLE_QUIT_BUTTON, false)) {
            assert cancel1 != null;
            cancel1.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					complete(null);
				}
			});
		} else {
            assert cancel1 != null;
            cancel1.setVisibility(ImageButton.GONE);
        }

		ImageButton cancel2 = (ImageButton) findViewById(R.id.menu_cancel2);
        assert cancel2 != null;
        cancel2.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				disableMultiChoice();
				showSecondHeader(false);
			}
		});

		ImageButton ok1 = (ImageButton) findViewById(R.id.menu_ok1);
		View ok1_delimiter = findViewById(R.id.ok1_delimiter);
		if (mOptOnlyOneItem && mOptChoiceType == FilePicker.CHOICE_TYPE_DIRECTORIES) {
            assert ok1 != null;
            ok1.setVisibility(ImageButton.VISIBLE);
            assert ok1_delimiter != null;
            ok1_delimiter.setVisibility(ImageButton.VISIBLE);
			ok1.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					ArrayList<String> list = new ArrayList<>();
					String parent;
					File parentFile = mCurrentDirectory.getParentFile();
					if (parentFile == null) {
						parent = "";
						list.add("/");
					} else {
						parent = parentFile.getAbsolutePath();
						if (!parent.endsWith("/")) parent += "/";
						list.add(mCurrentDirectory.getName());
					}
					FilePickerParcelObject object = new FilePickerParcelObject(parent, list, 1);
					complete(object);
				}
			});

		} else {
            assert ok1 != null;
            ok1.setVisibility(ImageButton.GONE);
            assert ok1_delimiter != null;
            ok1_delimiter.setVisibility(ImageButton.GONE);
		}

		ImageButton select_all = (ImageButton) findViewById(R.id.menu_select_all);
		ImageButton deselect = (ImageButton) findViewById(R.id.menu_deselect);
		ImageButton invert = (ImageButton) findViewById(R.id.menu_invert);
		if (mOptOnlyOneItem) {
            assert select_all != null;
            select_all.setVisibility(ImageButton.GONE);
            assert deselect != null;
            deselect.setVisibility(ImageButton.GONE);
			invert.setVisibility(ImageButton.GONE);
		} else {
			select_all.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					mSelected.clear();
					for (int i = 0; i < mFilesList.size(); i++)
						mSelected.add(mFilesList.get(i).getName());
					((BaseAdapter) mAbsListView.getAdapter()).notifyDataSetChanged();
				}
			});


			deselect.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					mSelected.clear();
					((BaseAdapter) mAbsListView.getAdapter()).notifyDataSetChanged();
				}
			});


			invert.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					ArrayList<String> tmp = new ArrayList<>();
					for (int i = 0; i < mFilesList.size(); i++) {
						String filename = mFilesList.get(i).getName();
						if (!mSelected.contains(filename)) tmp.add(filename);
					}
					mSelected = tmp;
					((BaseAdapter) mAbsListView.getAdapter()).notifyDataSetChanged();
				}
			});
		}

		ImageButton ok2 = (ImageButton) findViewById(R.id.menu_ok2);
		ok2.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (mSelected.size() > 0) {
					complete(null);
				} else {
					disableMultiChoice();
				}
			}
		});
	}



	@Override
	public boolean dispatchKeyEvent(KeyEvent event) {
		if (event.getKeyCode() == KeyEvent.KEYCODE_BACK) {
			if (event.getAction() == KeyEvent.ACTION_UP) {
				if (mIsMultiChoice) {
					disableMultiChoice();
				} else {
					File parentFile = mCurrentDirectory.getParentFile();
					if (parentFile == null) {
						complete(null);
					} else {
						readDirectory(parentFile);

						String path = mCurrentDirectory.getAbsolutePath();
						if (mListPositioins.containsKey(path)) {
							mAbsListView.setSelection(mListPositioins.get(path));
							mListPositioins.remove(path);
						}

						updateTitle();
					}
				}
			} else if (event.getAction() == KeyEvent.ACTION_DOWN && (event.getFlags() & KeyEvent.FLAG_LONG_PRESS) == KeyEvent.FLAG_LONG_PRESS) {
				mSelected.clear();
				complete(null);
			}
			return true;
		}
		return super.dispatchKeyEvent(event);
	}

	private void disableMultiChoice() {
		showSecondHeader(false);
		mIsMultiChoice = false;
		mSelected.clear();
		if (mOptChoiceType == FilePicker.CHOICE_TYPE_FILES && !mOptOnlyOneItem) {
			readDirectory(mCurrentDirectory);
		}
		((BaseAdapter) mAbsListView.getAdapter()).notifyDataSetChanged();
	}

	private void showSecondHeader(boolean show) {
		if (show) {
			findViewById(R.id.header1).setVisibility(View.GONE);
			findViewById(R.id.header2).setVisibility(View.VISIBLE);
		} else {
			findViewById(R.id.header1).setVisibility(View.VISIBLE);
			findViewById(R.id.header2).setVisibility(View.GONE);
		}
	}

	private void updateTitle() {
		mHeaderTitle.setText(mCurrentDirectory.getName());
	}

	private void complete(FilePickerParcelObject object) {
		if (object == null) {
			String path = mCurrentDirectory.getAbsolutePath();
			if (!path.endsWith("/")) path += "/";
			object = new FilePickerParcelObject(path, mSelected, mSelected.size());
		}
		Intent intent = new Intent();
		intent.putExtra(FilePickerParcelObject.class.getCanonicalName(), object);
		setResult(RESULT_OK, intent);
		finish();
	}

	private void readDirectory(File path) {
		mCurrentDirectory = path;
		mFilesList.clear();
		File[] files = path.listFiles();
		if (files != null) {

            for (File file : files) {
                if (mOptChoiceType == FilePicker.CHOICE_TYPE_DIRECTORIES && !file.isDirectory())
                    continue;
                if (file.isFile()) {
                    String extension = getFileExtension(file.getName());
                    if (mOptFilterListed != null && !mOptFilterListed.contains(extension)) continue;
                    if (mOptFilterExclude != null && mOptFilterExclude.contains(extension))
                        continue;
                }
                mFilesList.add(file);
            }
		}

		sort();
	}

	private void sort() {
		Collections.sort(mFilesList, new Comparator<File>() {
			@Override
			public int compare(File file1, File file2) {
				boolean isDirectory1 = file1.isDirectory();
				boolean isDirectory2 = file2.isDirectory();
				if (isDirectory1 && !isDirectory2) return -1;
				if (!isDirectory1 && isDirectory2) return 1;
				switch (mOptSortType) {
				case FilePicker.SORT_NAME_DESC:
					return file2.getName().toLowerCase(Locale.getDefault()).compareTo(file1.getName().toLowerCase(Locale.getDefault()));
				case FilePicker.SORT_SIZE_ASC:
					return Long.valueOf(file1.length()).compareTo(Long.valueOf(file2.length()));
				case FilePicker.SORT_SIZE_DESC:
					return Long.valueOf(file2.length()).compareTo(Long.valueOf(file1.length()));
				case FilePicker.SORT_DATE_ASC:
					return Long.valueOf(file1.lastModified()).compareTo(Long.valueOf(file2.lastModified()));
				case FilePicker.SORT_DATE_DESC:
					return Long.valueOf(file2.lastModified()).compareTo(Long.valueOf(file1.lastModified()));
				}
				// Default, FilePicker.SORT_NAME_ASC
				return file1.getName().toLowerCase(Locale.getDefault()).compareTo(file2.getName().toLowerCase(Locale.getDefault()));
			}
		});
		((BaseAdapter) mAbsListView.getAdapter()).notifyDataSetChanged();
	}

	private void setMenuItemView() {
		if (mAbsListView.getId() == R.id.gridview) {
			//mChangeView.setImageResource(attrToResId(R.attr.efp__ic_action_list));
			mChangeView.setContentDescription(getString(R.string.filePick_list));
		} else {
			//mChangeView.setImageResource(attrToResId(R.attr.efp__ic_action_grid));
			mChangeView.setContentDescription(getString(R.string.filePick_grid));
		}
	}

	private void setAbsListView() {
		int curView, nextView;

		if (mAbsListView == null || mAbsListView.getId() == R.id.listview) {
            curView = R.id.listview;
            nextView = R.id.gridview;
		} else {
            curView = R.id.gridview;
            nextView = R.id.listview;
		}

		mAbsListView = (AbsListView) findViewById(nextView);
		mAbsListView.setEmptyView(mEmptyView);

		FilesListAdapter adapter = new FilesListAdapter(this, (nextView == R.id.listview) ? R.layout.file_picker_list_item : R.layout.file_picker_grid_item);

		if (nextView == R.id.listview)
			((ListView) mAbsListView).setAdapter(adapter);
		else
			((GridView) mAbsListView).setAdapter(adapter);

		mAbsListView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				if (position < mFilesList.size()) {
					File file = mFilesList.get(position);
					if (mIsMultiChoice) {
						CheckBox checkBox = (CheckBox) view.findViewById(R.id.checkbox);
						if (checkBox.isChecked()) {
							checkBox.setChecked(false);
							//setItemBackground(view, false);
							mSelected.remove(file.getName());
						} else {
							if (mOptOnlyOneItem) {
								mSelected.clear();
								((BaseAdapter) mAbsListView.getAdapter()).notifyDataSetChanged();
							}
							checkBox.setChecked(true);
							//setItemBackground(view, true);
							mSelected.add(file.getName());
						}
					} else {
						if (file.isDirectory()) {
							int currentPosition = mAbsListView.getFirstVisiblePosition();
							mListPositioins.put(mCurrentDirectory.getAbsolutePath(), currentPosition);
							readDirectory(file);
							updateTitle();
							mAbsListView.setSelection(0);
						} else {
							mSelected.add(file.getName());
							complete(null);
						}
					}
				}
			}
		});

		if (mOptChoiceType != FilePicker.CHOICE_TYPE_FILES || !mOptOnlyOneItem) {
			mAbsListView.setOnItemLongClickListener(new OnItemLongClickListener() {
				@Override
				public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
					if (!mIsMultiChoice) {
						mIsMultiChoice = true;
						if (position < mFilesList.size()) {
							File file = mFilesList.get(position);
							if (mOptChoiceType != FilePicker.CHOICE_TYPE_FILES || file.isFile()) mSelected.add(file.getName());
						}

						if (mOptChoiceType == FilePicker.CHOICE_TYPE_FILES && !mOptOnlyOneItem) {
							ArrayList<File> tmpList = new ArrayList<>();
							for (int i = 0; i < mFilesList.size(); i++) {
								File file = mFilesList.get(i);
								if (file.isFile()) tmpList.add(file);
							}
							mFilesList = tmpList;
						}

						((BaseAdapter) mAbsListView.getAdapter()).notifyDataSetChanged();

						showSecondHeader(true);
						return true;
					}
					return false;
				}
			});
		}
		findViewById(curView).setVisibility(View.GONE);
		mAbsListView.setVisibility(View.VISIBLE);
	}



	@SuppressLint("DefaultLocale")
	private String getFileExtension(String fileName) {
		int index = fileName.lastIndexOf(".");
		if (index == -1) return "";
		return fileName.substring(index + 1, fileName.length()).toLowerCase(Locale.getDefault());
	}


	class FilesListAdapter extends BaseAdapter {
		private final Context mContext;
		private final int mResource;

		public FilesListAdapter(Context context, int resource) {
			mContext = context;
			mResource = resource;
		}

		@Override
		public int getCount() {
			return mFilesList.size();
		}

		@Override
		public Object getItem(int position) {
			return mFilesList.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@SuppressLint("ViewHolder")
        @Override
		public View getView(final int position, View convertView, ViewGroup parent) {
			File file = mFilesList.get(position);

			convertView = LayoutInflater.from(mContext).inflate(mResource, parent, false);
			ImageView thumbnail = (ImageView) convertView.findViewById(R.id.thumbnail);

			CheckBox checkbox = (CheckBox) convertView.findViewById(R.id.checkbox);

			if (mSelected.contains(file.getName())) {

				checkbox.setChecked(true);

			} else {

				checkbox.setChecked(false);

			}

			if (mIsMultiChoice) checkbox.setVisibility(View.VISIBLE);

			if (file.isDirectory()) {
				thumbnail.setImageResource(R.drawable.fp__ic_folder);

			} else {

				if ((Arrays.asList(mImagesExtensions).contains(getFileExtension(file.getName())))) {
					Bitmap bitmap = FilePickCache.getInstance().getBitmapFromMemCache(file.getAbsolutePath());

					if (bitmap == null)
                        new ThumbnailLoader(thumbnail).execute(file);
					else
                        thumbnail.setImageBitmap(bitmap);

				}
			}

			TextView filename = (TextView) convertView.findViewById(R.id.filename);
			filename.setText(file.getName());

			TextView filesize = (TextView) convertView.findViewById(R.id.filesize);

			if (filesize != null) {

				if (file.isFile()) {
					filesize.setText(getHumanFileSize(file.length()));
				} else {
					filesize.setText("");
				}
			}

			return convertView;
		}

		String getHumanFileSize(long size) {

			String[] units = getResources().getStringArray(R.array.fp__size_units);

			for (int i = units.length - 1; i >= 0; i--) {
				if (size >= Math.pow(1024, i)) {
					return Math.round((size / Math.pow(1024, i))) + " " + units[i];
				}
			}
			return size + " " + units[0];
		}


		class ThumbnailLoader extends AsyncTask<File, Void, Bitmap> {

			private final WeakReference<ImageView> imageViewReference;

			public ThumbnailLoader(ImageView imageView) {
				imageViewReference = new WeakReference<>(imageView);
				
			}

			@TargetApi(Build.VERSION_CODES.ECLAIR)
			@Override
			protected Bitmap doInBackground(File... arg0) {
				Bitmap thumbnailBitmap = null;
				File file = arg0[0];

				if (file != null) {

					try {
						ContentResolver crThumb = getContentResolver();
						if (Arrays.asList(mImagesExtensions).contains(getFileExtension(file.getName()))) {

							Cursor cursor = crThumb.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, new String[] { MediaStore.Images.Media._ID }, MediaStore.Images.Media.DATA + "='" + file.getAbsolutePath() + "'", null, null);
							if (cursor != null) {

								if (cursor.getCount() > 0) {

									cursor.moveToFirst();
									thumbnailBitmap = MediaStore.Images.Thumbnails.getThumbnail(crThumb, cursor.getInt(0), MediaStore.Images.Thumbnails.MINI_KIND, null);
								}
								cursor.close();
							}
						}

					} catch (Exception e) {
						e.printStackTrace();
					} catch (Error e) {
						e.printStackTrace();
					}
				}

				if (thumbnailBitmap != null) FilePickCache.getInstance().addBitmapToMemoryCache(file.getAbsolutePath(), thumbnailBitmap);
				return thumbnailBitmap;
			}

			@Override
			protected void onPostExecute(Bitmap bitmap) {
				if (imageViewReference != null) {
					final ImageView imageView = imageViewReference.get();
					if (imageView != null) {
						if (bitmap == null) imageView.setImageResource(R.drawable.fp__ic_file);
						else imageView.setImageBitmap(bitmap);
					}
				}
			}

		}

	}


}
