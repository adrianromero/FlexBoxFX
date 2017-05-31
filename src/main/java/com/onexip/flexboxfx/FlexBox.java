package com.onexip.flexboxfx;

import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Node;
import javafx.scene.layout.Pane;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by TB on 11.10.16.
 */
public class FlexBox extends Pane {

    private final static Logger LOGGER = Logger.getLogger(FlexBox.class.getName());
    
    private static final String ORDER_CONSTRAINT = "flexbox-order";
    private static final String GROW_CONSTRAINT = "flexbox-grow";
    
    private final SimpleDoubleProperty horizontalSpace = new SimpleDoubleProperty(0);
    private final SimpleDoubleProperty verticalSpace = new SimpleDoubleProperty(0);
    private final SimpleObjectProperty<FlexBoxDirection> direction = new SimpleObjectProperty<>(FlexBoxDirection.ROW);
    private double computedMinHeight;
    private boolean performingLayout = false;

    public FlexBoxDirection getDirection() {
        return direction.get();
    }

    public SimpleObjectProperty<FlexBoxDirection> directionProperty() {
        return direction;
    }

    public void setDirection(FlexBoxDirection direction) {
        this.direction.set(direction);
    }

    public double getHorizontalSpace() {
        return horizontalSpace.get();
    }

    public SimpleDoubleProperty horizontalSpaceProperty() {
        return horizontalSpace;
    }

    public void setHorizontalSpace(double horizontalSpace) {
        this.horizontalSpace.set(horizontalSpace);
    }

    public double getVerticalSpace() {
        return verticalSpace.get();
    }

    public SimpleDoubleProperty verticalSpaceProperty() {
        return verticalSpace;
    }

    public void setVerticalSpace(double verticalSpace) {
        this.verticalSpace.set(verticalSpace);
    }

    /**
     * By default, flex items are laid out in the source order. However, the
     * order property controls the order in which they appear in the flex
     * container.
     *
     * @param child the child of an FlexBox
     * @param value the order in which the child appear in the flex container
     */
    public static void setOrder(Node child, int value) {
        setConstraint(child, ORDER_CONSTRAINT, value);
    }

    public static int getOrder(Node child) {
        Object constraint = getConstraint(child, ORDER_CONSTRAINT);
        if (constraint == null) {
            return 0;
        } else {
            return (int) constraint;
        }
    }

    /**
     * This defines the ability for a flex item to grow if necessary. It accepts
     * a unitless value that serves as a proportion. It dictates what amount of
     * the available space inside the flex container the item should take up. If
     * all items have flex-grow set to 1, the remaining space in the container
     * will be distributed equally to all children. If one of the children has a
     * value of 2, the remaining space would take up twice as much space as the
     * others (or it will try to, at least).
     *
     * @param child the child of an FlexBox
     * @param value grow proportion
     */
    public static void setGrow(Node child, double value) {
        setConstraint(child, GROW_CONSTRAINT, value);
    }

    public static double getGrow(Node child) {
        Object o = getConstraint(child, GROW_CONSTRAINT);
        if (o == null) {
            return 1.0;
        } else {
            return (double) o;
        }
    }

    private static void setConstraint(Node node, Object key, Object value) {
        if (value == null) {
            node.getProperties().remove(key);
        } else {
            node.getProperties().put(key, value);
        }
        if (node.getParent() != null) {
            node.getParent().requestLayout();
        }
    }

    private static Object getConstraint(Node node, Object key) {
        if (node.hasProperties()) {
            return node.getProperties().get(key);
        }
        return null;
    }

    @Override
    protected double computeMinHeight(double width) {
        return computedMinHeight;
    }

    @Override
    protected double computePrefHeight(double width) {
        return computedMinHeight;
    }

    @Override
    protected void layoutChildren() {
        long timeStart = System.nanoTime();
        
        performingLayout = true;
        
        List<FlexBoxItem> nodesList = new ArrayList<>();

        /**
         * First we transform all Nodes to a FlexBoxItem for caching purposes.
         */
        for (Node node : getManagedChildren()) {
            nodesList.add(new FlexBoxItem(node));
        }

        if (getDirection() == FlexBoxDirection.ROW || getDirection() == FlexBoxDirection.ROW_REVERSE) { //todo:
            layoutChildrenForRowDirection(nodesList);
        } else { //todo:
            layoutChildrenForColumnDirection(nodesList);
        }
        
        performingLayout = false;
        
        LOGGER.log(Level.FINE, "Layout duration: {0} ms", (System.nanoTime() - timeStart) / 1000L);
    }

    private void layoutChildrenForColumnDirection(List<FlexBoxItem> nodesList) {

        Map<Integer, FlexBoxRow> grid = new HashMap<>();
        double w = getWidth();
        double growWidth = w - getPadding().getLeft() - getPadding().getRight();

        int row = 0;
        int i = 0;

        for (FlexBoxItem flexBoxItem : nodesList) {
            FlexBoxRow flexBoxRow = new FlexBoxRow();
            flexBoxRow.addFlexBoxItem(flexBoxItem);
            grid.put(row, flexBoxRow);
            row++;
            i++;
        }

        LOGGER.log(Level.FINE, "Grid = {0}", grid.toString());

        //Rows durchgehen und width berechnen
        double lastY2 = getPadding().getTop();
        i = 0;
        int noGridRows = grid.size();

        //iterate all rows and calculate node sizes and positions
        for (Integer rowIndex : grid.keySet()) {
            //contains all nodes per row
            FlexBoxRow flexBoxRow = grid.get(rowIndex);
            ArrayList<FlexBoxItem> rowNodes = flexBoxRow.getNodes();
            double rowNodeX2 = 0;
            double lastMaxHeight = 0;
            //iterate node of row
            for (FlexBoxItem flexBoxItem : rowNodes) {
                double rowNodeMinWidth = flexBoxItem.node.minWidth(10);
                double rowNodeMaxWidth = flexBoxItem.node.maxWidth(10);
                double rowNodeWidth = Math.min(rowNodeMaxWidth, Math.max(growWidth, rowNodeMinWidth));

                double h = flexBoxItem.node.prefHeight(growWidth);
                flexBoxItem.node.resizeRelocate(rowNodeX2, lastY2, rowNodeWidth, h);

                lastMaxHeight = Math.max(lastMaxHeight, h);

                rowNodeX2 = rowNodeX2 + rowNodeWidth + getHorizontalSpace();
            }

            lastY2 = lastY2 + lastMaxHeight;
            if (i + 1 < noGridRows) {
                lastY2 += getVerticalSpace();
            }
            i++;
        }
    }

    private void layoutChildrenForRowDirection(List<FlexBoxItem> nodesList) {

        Map<Integer, FlexBoxRow> grid = new HashMap<>();
        double w = getWidth() - getPadding().getLeft() - getPadding().getRight();

        /**
         * Precalculations
         */
        boolean useOrder = false;
        for (FlexBoxItem flexBoxItem : nodesList) {
            flexBoxItem.minWidth = flexBoxItem.node.minWidth(10.0);
            flexBoxItem.order = getOrder(flexBoxItem.node);
            if (flexBoxItem.order != 0) {
                useOrder = true;
            }
            flexBoxItem.grow = getGrow(flexBoxItem.node);
        }

        if (useOrder) {
            Collections.sort(nodesList, (FlexBoxItem item1, FlexBoxItem item2) -> Integer.compare(item1.order, item2.order));
        }

        /**
         * Calculate column-row-grid for auto wrapping
         */
        int row = 0;
        double minWidthSum = 0.0;
        FlexBoxRow flexBoxRow = new FlexBoxRow();
        grid.put(row, flexBoxRow);

        int i = 0;
        for (FlexBoxItem flexBoxItem : nodesList) {           
            
            double nodeWidth = flexBoxItem.minWidth;           
            minWidthSum += nodeWidth;
            if (flexBoxRow.getNodes().isEmpty()) {
                // If empty the item must be included
            } else {
                minWidthSum += getHorizontalSpace();
                if (minWidthSum > w) {                   
                    row++;
                    minWidthSum = nodeWidth;
                    flexBoxRow = new FlexBoxRow();
                    grid.put(row, flexBoxRow);
                }                              
            } 
            
            flexBoxRow.rowMinWidth += flexBoxItem.minWidth;
            flexBoxRow.flexGrowSum += flexBoxItem.grow;
            flexBoxRow.addFlexBoxItem(flexBoxItem);
            i++;
        }
        //iterate rows and calculate width
        double lastY2 = getPadding().getTop();
        i = 0;
        int noGridRows = grid.size();
        /**
         * iterate grid to calculate node sizes and positions iterate grid-rows
         * first
         */
        for (Integer rowIndex : grid.keySet()) {
            //contains all nodes per row
            flexBoxRow = grid.get(rowIndex);
            ArrayList<FlexBoxItem> rowNodes = flexBoxRow.getNodes();
            int noRowNodes = rowNodes.size();

            double remainingWidth = w - flexBoxRow.rowMinWidth - (getHorizontalSpace() * (noRowNodes - 1));
            double flexGrowCellWidth = remainingWidth / flexBoxRow.flexGrowSum;

            Iterable<FlexBoxItem> rowNodexIterator = (getDirection() == FlexBoxDirection.ROW_REVERSE)
                    ? ReverseIterable.reverse(rowNodes)
                    : rowNodes;

            double rowNodeX2 = getPadding().getLeft();
            double lastMaxHeight = 0.0;

            //iterate nodes of row
            for (FlexBoxItem flexBoxItem : rowNodexIterator) {
                Node rowNode = flexBoxItem.node;

                double rowNodeMinWidth = flexBoxItem.minWidth;
                double rowNodeMaxWidth = rowNode.maxWidth(10);
                double rowNodeStrechtedWidth = rowNodeMinWidth + (flexGrowCellWidth * flexBoxItem.grow);
                double rowNodeWidth = Math.min(rowNodeMaxWidth, Math.max(rowNodeStrechtedWidth, rowNodeMinWidth));

                double h = rowNode.prefHeight(rowNodeWidth);
                rowNode.resizeRelocate(rowNodeX2, lastY2, rowNodeWidth, h);
                lastMaxHeight = Math.max(lastMaxHeight, h);
                rowNodeX2 = rowNodeX2 + rowNodeWidth + getHorizontalSpace();
            }

            lastY2 = lastY2 + lastMaxHeight;
            if (i + 1 < noGridRows) {
                lastY2 += getVerticalSpace();
            }
            i++;
        }
        lastY2 += getPadding().getBottom();
        setMinHeight(lastY2);
        setPrefWidth(lastY2);
        computedMinHeight = lastY2;
    }

    @Override
    public void requestLayout() {
        if (performingLayout) {
            return;
        }
        super.requestLayout();
    }
}
