package com.onexip.flexboxfx;

import java.util.ArrayList;

/**
 * Created by TB on 19.10.16.
 */
class FlexBoxRow {

    public double rowMinWidth = 0.0;
    private ArrayList<FlexBoxItem> nodes;
    public double flexGrowSum = 0.0;

    public void addFlexBoxItem(FlexBoxItem flexBoxItem) {
        if (nodes == null) {
            nodes = new ArrayList<>();
        }
        nodes.add(flexBoxItem);
    }

    public ArrayList<FlexBoxItem> getNodes() {
        if (nodes == null) {
            nodes = new ArrayList<>();
        }
        return nodes;
    }
}
