package org.oucho.musicplayer.filepicker;

import java.util.ArrayList;

import android.os.Parcel;
import android.os.Parcelable;

public class FilePickerParcelObject implements Parcelable {

	public String path="";
	public ArrayList<String> names=new ArrayList<String>();
	public int count=0;

	public FilePickerParcelObject(String path, ArrayList<String> names, int count) {
		this.path=path;
		this.names=names;
		this.count=count;
	}

	public int describeContents() {
		return 0;
	}

	public void writeToParcel(Parcel parcel, int flags) {
		parcel.writeString(path);
		parcel.writeStringList(names);
		parcel.writeInt(count);
	}

	public static final Creator<FilePickerParcelObject> CREATOR = new Creator<FilePickerParcelObject>() {
		public FilePickerParcelObject createFromParcel(Parcel in) {
			return new FilePickerParcelObject(in);
		}

		public FilePickerParcelObject[] newArray(int size) {
			return new FilePickerParcelObject[size];
		}
	};

	private FilePickerParcelObject(Parcel parcel) {
		path = parcel.readString();
		parcel.readStringList(names);
		count = parcel.readInt();
	}

}
