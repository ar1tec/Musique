package org.oucho.musicplayer.activities;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SwitchCompat;
import android.text.Html;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout.LayoutParams;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.Spinner;
import android.widget.TextView;

import org.oucho.musicplayer.R;
import org.oucho.musicplayer.audiofx.AudioEffects;
import org.oucho.musicplayer.utils.NavigationUtils;

public class EqualizerActivity extends AppCompatActivity {


    private SwitchCompat mSwitchButton;
    private boolean mSwitchBound;

    private Spinner mSpinner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_equalizer);

        Context context = getApplicationContext();


        int couleurTitre = ContextCompat.getColor(context, R.color.colorAccent);

        String titre = context.getString(R.string.equalizer);


        ActionBar actionBar = getSupportActionBar();
        assert actionBar != null;
        if (android.os.Build.VERSION.SDK_INT >= 24) {
            actionBar.setTitle(Html.fromHtml("<font color='" + couleurTitre + "'>" + titre + "</font>", Html.FROM_HTML_MODE_LEGACY));
        } else {
            //noinspection deprecation
            actionBar.setTitle(Html.fromHtml("<font color='" + couleurTitre + "'>" + titre + "</font>"));
        }

        mSwitchBound = false;
        init();
    }

    @Override
    public void onPause() {
        super.onPause();
        AudioEffects.savePrefs(this);
    }

    @Override
    public void onBackPressed() {

            NavigationUtils.showMainActivity(this);
    }

    private void bindSwitchToEqualizer() {
        if (!mSwitchBound && mSwitchButton != null) {

            mSwitchButton.setChecked(AudioEffects.areAudioEffectsEnabled());
            mSwitchButton
                    .setOnCheckedChangeListener((buttonView, isChecked) -> AudioEffects.setAudioEffectsEnabled(isChecked));
            mSwitchBound = true;
        }
    }

    private void init() {


        bindSwitchToEqualizer();

        initBassBoost();

        initSeekBars();

        updateSeekBars();

        initPresets();
    }

    private void initPresets() {

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item,
                AudioEffects.getEqualizerPresets(this));

        mSpinner = (Spinner) findViewById(R.id.presets_spinner);

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        mSpinner.setAdapter(adapter);

        mSpinner.setSelection(AudioEffects.getCurrentPreset());

        mSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> parent, View view,
                                       int position, long id) {
                if (position >= 1) {
                    AudioEffects.usePreset((short) (position - 1));
                }
                updateSeekBars();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                //  Auto-generated method stub
            }
        });
    }

    private void initBassBoost() {
        SeekBar bassBoost = (SeekBar) findViewById(R.id.bassboost_slider);
        assert bassBoost != null;
        bassBoost.setMax(AudioEffects.BASSBOOST_MAX_STRENGTH);
        bassBoost.setProgress(AudioEffects.getBassBoostStrength());
        bassBoost.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // This constructor is intentionally empty, pourquoi ? parce que !
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

                // This constructor is intentionally empty, pourquoi ? parce que !

            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress,
                                          boolean fromUser) {
                if (fromUser) {
                    AudioEffects.setBassBoostStrength((short) seekBar.getProgress());
                }
            }
        });
    }

    private void initSeekBars() {
            ViewGroup layout = (ViewGroup) findViewById(R.id.equalizer_layout);

            final short[] range = AudioEffects.getBandLevelRange();

            LayoutParams lp = new LayoutParams(LayoutParams.MATCH_PARENT, 0, 1);
            short bands = AudioEffects.getNumberOfBands();

            for (short band = 0; band < bands; band++) {

                View v = getLayoutInflater().inflate(R.layout.activity_equalizer_slider, layout, false);


                SeekBar seekBar = v.findViewById(R.id.seek_bar);


                assert range != null;
                seekBar.setMax((range[1]) - range[0]);

                seekBar.setTag(band);

                final TextView levelTextView = v
                        .findViewById(R.id.level);
                seekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {

                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {
                        //  Auto-generated method stub

                    }

                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

                        if (fromUser) {
                            short band = (Short) seekBar.getTag();
                            short level = (short) (seekBar.getProgress() + range[0]);
                            AudioEffects.setBandLevel(band, level);
                            String niveau = (level > 0 ? "+" : "") + level / 100 + "dB";
                            levelTextView.setText(niveau);
                            mSpinner.setSelection(0);
                        }
                    }
                });

                assert layout != null;
                layout.addView(v, band, lp);
            }
    }

    private void updateSeekBars() {
        ViewGroup layout = (ViewGroup) findViewById(R.id.equalizer_layout);

        final short[] range = AudioEffects.getBandLevelRange();

        short bands = AudioEffects.getNumberOfBands();

        for (short band = 0; band < bands; band++) {

            assert layout != null;
            View v = layout.getChildAt(band);

            final TextView freqTextView = v.findViewById(R.id.frequency);
            final TextView levelTextView = v
                    .findViewById(R.id.level);
            final SeekBar seekBar = v.findViewById(R.id.seek_bar);


            int freq = AudioEffects.getCenterFreq(band);
            if (freq < 1000 * 1000) {
                String frequence = freq / 1000 + "Hz";
                freqTextView.setText(frequence);
            } else {
                String frequence = freq / (1000 * 1000) + "kHz";
                freqTextView.setText(frequence);
            }


            short level = AudioEffects.getBandLevel(band);
            seekBar.setProgress(level - (range != null ? range[0] : 0));

            String niveau = (level > 0 ? "+" : "") + level / 100 + "dB";
            levelTextView.setText(niveau);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.equalizer, menu);
        MenuItem item = menu.findItem(R.id.action_switch);

        mSwitchButton = item.getActionView().findViewById(R.id.switch_button);
        bindSwitchToEqualizer();
        return true;
    }

}
