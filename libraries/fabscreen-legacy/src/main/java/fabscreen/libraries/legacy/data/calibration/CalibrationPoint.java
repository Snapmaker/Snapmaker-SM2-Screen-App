package fabscreen.libraries.legacy.data.calibration;

/**
 * A point which machine locate and probes the print surface,
 * could be viewed on screen and check the progress of the
 * 3d print calibration procedure.
 * <p>
 * viewOrder: Indexes that view on screen, represent the point orders for auto calibration.
 * It starts with the left down corner, increase with counterclockwise.
 *          7     6     5
 *          *-----*-----*
 *        8|    9|    4|
 *        *-----*-----*
 *      1|    2|    3|
 *      *-----*-----*
 *
 * coordinateOrder: Orders that creates with row and column. In slave computer,
 * we use coordinateOrder to identify which point is calibrating or want to calibrated.
 *        7     8     9
 *        *-----*-----*
 *      4|    5|    6|
 *      *-----*-----*
 *    1|    2|    3|
 *    *-----*-----*
 *
 *  Traversal by calibration procedure: 1 -> 2 -> 3 -> 6 -> 9 -> 8 -> 7 -> 4 -> 5
 */
public class CalibrationPoint {
    private int coordinateOrder;
    private int viewOrder;
    private int row;
    private int column;
    private boolean isSelected;
    private boolean isActivated;

    public CalibrationPoint(int row, int column, int viewOrder, int coordinateOrder) {
        this.row = row;
        this.column = column;
        this.viewOrder = viewOrder;
        this.coordinateOrder = coordinateOrder;
        this.isSelected = false;
        this.isActivated = false;
    }

    public int getViewOrder() {
        return viewOrder;
    }

    public int getCoordinateOrder() {
        return coordinateOrder;
    }

    public int getRow() {
        return row;
    }

    public int getColumn() {
        return column;
    }

    public boolean isSelected() {
        return isSelected;
    }

    public void setSelected(boolean selected) {
        isSelected = selected;
    }

    public boolean isActivated() {
        return isActivated;
    }

    public void setActivated(boolean activated) {
        isActivated = activated;
    }
}
