package org.oucho.musicplayer.view.chart.model;

import android.support.annotation.NonNull;

import java.util.ArrayList;

import static org.oucho.musicplayer.view.chart.util.Preconditions.checkNotNull;
import static org.oucho.musicplayer.view.chart.util.Preconditions.checkPositionIndex;


public abstract class ChartSet {


    private final ArrayList<ChartEntry> mEntries;

    private float mAlpha;

    private boolean mIsVisible;

    ChartSet() {
        mEntries = new ArrayList<>();
        mAlpha = 1;
        mIsVisible = false;
    }

    void addEntry(@NonNull ChartEntry e) {
        mEntries.add(checkNotNull(e));
    }


    public void updateValues(@NonNull float[] newValues) {

        checkNotNull(newValues);
        if (newValues.length != size()) throw new IllegalArgumentException("New set values given doesn't match previous " + "number of entries.");

        int nEntries = size();
        for (int i = 0; i < nEntries; i++)
            setValue(i, newValues[i]);
    }

    public int size() {
        return mEntries.size();
    }

    public ArrayList<ChartEntry> getEntries() {
        return mEntries;
    }

    public ChartEntry getEntry(int index) {
        return mEntries.get(checkPositionIndex(index, size()));
    }

    public float getValue(int index) {
        return mEntries.get(checkPositionIndex(index, size())).getValue();
    }

    public String getLabel(int index) {
        return mEntries.get(checkPositionIndex(index, size())).getLabel();
    }

    public float[][] getScreenPoints() {

        int nEntries = size();
        float[][] result = new float[nEntries][2];
        for (int i = 0; i < nEntries; i++) {
            result[i][0] = mEntries.get(i).getX();
            result[i][1] = mEntries.get(i).getY();
        }
        return result;
    }

    public float getAlpha() {
        return mAlpha;
    }

    public boolean isVisible() {
        return mIsVisible;
    }

    public void setVisible(boolean visible) {
        mIsVisible = visible;
    }

    private void setValue(int index, float value) {
        mEntries.get(checkPositionIndex(index, size())).setValue(value);
    }

    public String toString() {
        return mEntries.toString();
    }

}
