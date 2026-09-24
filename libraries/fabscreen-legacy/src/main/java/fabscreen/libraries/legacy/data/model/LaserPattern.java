package fabscreen.libraries.legacy.data.model;

import fabscreen.libraries.legacy.lib.parser.Position;

/**
 * Laser Pattern: A Model used for laser calibration engraving pattern.
 */
public class LaserPattern {
    // enum for shapes
    public final static int SHAPE_RULER = 1;

    // enum for pattern direction
    public final static int DIRECTION_X = 1;
    public final static int DIRECTION_Y = 2;

    // enum for pattern alignment
    public final static int ALIGNMENT_ENGRAVE_START = 1;
    public final static int ALIGNMENT_ENGRAVE_CENTER = 2;
    public final static int ALIGNMENT_ENGRAVE_END = 3;

    private int mShape;
    private int mDirection;
    private int mAlignment;
    private float mZOffset;
    private ModelBoundary mBoundary;

    private int mUnitCountPerDivision = 5;
    private float spacingPerLine = 2;
    private int longEngraveLineLength = 10;
    private int shortEngraveLineLength = 5;
    private int totalLines = 21;

    public LaserPattern() {
        this(SHAPE_RULER, DIRECTION_X, ALIGNMENT_ENGRAVE_START, 0.5f);
    }

    public LaserPattern(int shape, int direction, int alignment, float offset) {
        mShape = shape;
        mDirection = direction;
        mAlignment = alignment;
        mZOffset = offset;

        mBoundary = calculateBoundary();
    }

    public ModelBoundary getPatternBoundary() {
        return mBoundary;
    }

    public int getPatternShape() {
        return mShape;
    }

    public void setPatternShape(int shape) {
        mShape = shape;
    }

    public int getAlignment() {
        return mAlignment;
    }

    public void setAlignment(int alignment) {
        mAlignment = alignment;
    }

    public int getEngraveDirection() {
        return mDirection;
    }

    public void setEngraveDirection(int direction) {
        mDirection = direction;
    }

    public float getZOffset() {
        return mZOffset;
    }

    public void setZOffset(int offset) {
        mZOffset = offset;
    }

    public int getTotalLines() {
        return totalLines;
    }

    public void setTotalLines(int lines) {
        totalLines = lines;
    }

    public int getLongEngraveLineLength() {
        return longEngraveLineLength;
    }

    public void setLongEngraveLineLength(int length) {
        longEngraveLineLength = length;
    }

    public int getShortEngraveLineLength() {
        return shortEngraveLineLength;
    }

    public void setShortEngraveLineLength(int length) {
        shortEngraveLineLength = length;
    }

    public float getSpacingPerLine() {
        return spacingPerLine;
    }

    public void setSpacingPerLine(int spacing) {
        spacingPerLine = spacing;
    }

    public int getUnitCountPerDivision() {
        return mUnitCountPerDivision;
    }

    public void setUnitCountPerDivision(int count) {
        mUnitCountPerDivision = count;
    }


    private ModelBoundary calculateBoundary() {
        ModelBoundary boundary = new ModelBoundary();
        float patternLength = 0;
        float patternHeight = 0;
        float minX = 0;
        float maxX = 0;
        float minY = 0;
        float maxY = 0;

        if (mDirection == DIRECTION_X) {
            patternLength = spacingPerLine * (totalLines - 1);
            patternHeight = longEngraveLineLength;
        } else {
            patternLength = longEngraveLineLength;
            patternHeight = spacingPerLine * (totalLines - 1);
        }

        minX = -patternLength / 2f;
        maxX = patternLength / 2f;
        minY = -patternHeight / 2f;
        maxY = patternHeight / 2f;

        boundary.updateBoundary(new Position(minX, minY, 0));
        boundary.updateBoundary(new Position(maxX, minY, 0));
        boundary.updateBoundary(new Position(minX, maxY, 0));
        boundary.updateBoundary(new Position(minX, maxY, 0));
        boundary.setDimension(ModelBoundary.DIMENSION_XY);
        return boundary;
    }
}
