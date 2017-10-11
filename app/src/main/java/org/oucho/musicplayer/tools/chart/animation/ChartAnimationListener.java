package org.oucho.musicplayer.tools.chart.animation;

import org.oucho.musicplayer.tools.chart.model.ChartSet;

import java.util.ArrayList;


/**
 * Interface used by {@link org.oucho.musicplayer.tools.chart.animation.Animation} to interact with {@link org.oucho.musicplayer.tools.chart.view.ChartView}
 */
public interface ChartAnimationListener {

    /**
     * Callback to let {@link org.oucho.musicplayer.tools.chart.view.ChartView} know when to invalidate and present new data.
     *
     * @param data Chart data to be used in the next view invalidation.
     * @return True if {@link org.oucho.musicplayer.tools.chart.view.ChartView} accepts the call, False otherwise.
     */
    boolean onAnimationUpdate(ArrayList<ChartSet> data);
}
