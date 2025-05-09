/*-
 * #%L
 * JavaFX GUI for processing single-molecule TIRF and FMT data in the Structure and Dynamics of Molecular Machines research group.
 * %%
 * Copyright (C) 2018 - 2025 Karl Duderstadt
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */
/*
 * Copyright (c) 2016 JFoenix
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package de.mpg.biochem.mars.fx.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.jfoenix.transitions.CachedTransition;

import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.ParallelTransition;
import javafx.animation.Timeline;
import javafx.animation.Transition;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ListChangeListener;
import javafx.collections.WeakListChangeListener;
import javafx.geometry.BoundingBox;
import javafx.scene.Node;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.util.Duration;

/**
 * A JFXMasonryPane implements asymmetrical grid layoutMode, it places the child
 * nodes according to one of the modes:
 * <p>
 * <h3>Masonry Layout</h3> Nodes will be added one after another, first in the
 * horizontal direction , then vertically. sort of like a mason fitting stones
 * in a wall.
 * <p>
 * <h3>Bin Packing Layout(First Fit)</h3> it works similar to masonry
 * layoutMode, however it tries to fill the empty gaps caused in masonry
 * layoutMode. <b>Note:</b> children that doesn't fit in the grid will be
 * hidden.
 *
 * @author Shadi Shaheen
 * @version 1.0
 * @since 2016-05-24
 */
public class MarsJFXMasonryPane extends Pane {

	private boolean performingLayout = false;
	// these variables are computed when layoutChildren is called
	private int[][] matrix;
	private HashMap<Region, Transition> animationMap = null;
	private ParallelTransition trans = new ParallelTransition();
	private HashMap<Node, BoundingBox> boundingBoxes = new HashMap<>();
	private boolean dirtyBoxes = false;

	private final ListChangeListener<Node> childrenListener = change -> {
		if (change.next()) {
			// flag dirty boxes
			dirtyBoxes = true;

			// clean removed child nodes from animationMap
			// fixed #1003 JFXMasonryPane nullpointer when init before layout.
			if (animationMap != null) {
				for (Node removedNode : change.getRemoved()) {
					animationMap.remove(removedNode);
				}
			}
		}
		clearLayout();
		requestLayout();
	};

	/**
	 * Constructs a new JFXMasonryPane
	 */
	public MarsJFXMasonryPane() {
		getChildren().addListener(new WeakListChangeListener<>(childrenListener));
	}

	@Override
	protected double computePrefWidth(double height) {
		return snappedLeftInset() + getCellWidth() + snappedRightInset() + 2 *
			getHSpacing();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void layoutChildren() {
		performingLayout = true;

		int col, row;
		col = (int) Math.floor((getWidth() + getHSpacing() - snappedLeftInset() -
			snappedRightInset()) / (getCellWidth() + getHSpacing()));
		col = getLimitColumn() != -1 && col > getLimitColumn() ? getLimitColumn()
			: col;

		if (matrix != null && col == matrix[0].length) {
			performingLayout = false;
			return;
		}
		// (int) Math.floor(this.getHeight() / (cellH + 2*vSpacing));
		row = getLimitRow();
		matrix = new int[row][col];
		double minWidth = -1;
		double minHeight = -1;

		List<BoundingBox> newBoxes;
		List<Region> managedChildren = getManagedChildren();

		// filter Region nodes
		for (int i = 0; i < managedChildren.size(); i++) {
			if (!(managedChildren.get(i) instanceof Region)) {
				managedChildren.remove(i);
				i--;
			}
		}

		// get bounding boxes layout
		newBoxes = layoutMode.get().fillGrid(matrix, managedChildren,
			getCellWidth(), getCellHeight(), row, col, getHSpacing(), getVSpacing());

		if (newBoxes == null) {
			performingLayout = false;
			return;
		}

		HashMap<Node, BoundingBox> oldBoxes = boundingBoxes;
		if (dirtyBoxes) {
			boundingBoxes = new HashMap<>();
		}

		for (int i = 0; i < managedChildren.size() && i < newBoxes.size(); i++) {
			final Region child = managedChildren.get(i);
			final BoundingBox boundingBox = newBoxes.get(i);
			if (!(child instanceof GridPane)) {
				double blockX;
				double blockY;
				double blockWidth;
				double blockHeight;
				if (boundingBox != null) {
					blockX = boundingBox.getMinY() * getCellWidth() + boundingBox
						.getMinY() * getHSpacing() + snappedLeftInset();
					blockY = boundingBox.getMinX() * getCellHeight() + boundingBox
						.getMinX() * getVSpacing() + snappedTopInset();
					blockWidth = boundingBox.getWidth() * getCellWidth() + (boundingBox
						.getWidth() - 1) * getHSpacing();
					blockHeight = boundingBox.getHeight() * getCellHeight() + (boundingBox
						.getHeight() - 1) * getVSpacing();
				}
				else {
					blockX = child.getLayoutX();
					blockY = child.getLayoutY();
					blockWidth = -1;
					blockHeight = -1;
				}

				if (animationMap == null) {
					// init static children
					child.setPrefSize(blockWidth, blockHeight);
					child.resizeRelocate(blockX, blockY, blockWidth, blockHeight);
				}
				else {
					BoundingBox oldBoundingBox = oldBoxes.get(child);
					if (oldBoundingBox == null || (!oldBoundingBox.equals(boundingBox) &&
						dirtyBoxes))
					{
						// handle new children
						child.setOpacity(0);
						child.setPrefSize(blockWidth, blockHeight);
						child.resizeRelocate(blockX, blockY, blockWidth, blockHeight);
					}

					if (boundingBox != null) {
						// handle children repositioning
						if (child.getWidth() != blockWidth || child
							.getHeight() != blockHeight)
						{
							child.setOpacity(0);
							child.setPrefSize(blockWidth, blockHeight);
							child.resizeRelocate(blockX, blockY, blockWidth, blockHeight);
						}
						final KeyFrame keyFrame = new KeyFrame(Duration.millis(2000),
							new KeyValue(child.opacityProperty(), 1, Interpolator.LINEAR),
							new KeyValue(child.layoutXProperty(), blockX,
								Interpolator.LINEAR), new KeyValue(child.layoutYProperty(),
									blockY, Interpolator.LINEAR));
						animationMap.put(child, new CachedTransition(child, new Timeline(
							keyFrame))
						{

							{
								setCycleDuration(Duration.seconds(0.320));
								setDelay(Duration.seconds(0));
								setOnFinished((finish) -> {
									child.setLayoutX(blockX);
									child.setLayoutY(blockY);
									child.setOpacity(1);
								});
							}
						});

					}
					else {
						// handle children is being hidden ( cause it can't fit in the pane
						// )
						final KeyFrame keyFrame = new KeyFrame(Duration.millis(2000),
							new KeyValue(child.opacityProperty(), 0, Interpolator.LINEAR),
							new KeyValue(child.layoutXProperty(), blockX,
								Interpolator.LINEAR), new KeyValue(child.layoutYProperty(),
									blockY, Interpolator.LINEAR));
						animationMap.put(child, new CachedTransition(child, new Timeline(
							keyFrame))
						{

							{
								setCycleDuration(Duration.seconds(0.320));
								setDelay(Duration.seconds(0));
								setOnFinished((finish) -> {
									child.setLayoutX(blockX);
									child.setLayoutY(blockY);
									child.setOpacity(0);
								});
							}
						});
					}
				}

				// update bounding box
				boundingBoxes.put(child, boundingBox);

				if (boundingBox != null) {
					if (blockX + blockWidth > minWidth) {
						minWidth = blockX + blockWidth;
					}
					if (blockY + blockHeight > minHeight) {
						minHeight = blockY + blockHeight;
					}
				}
			}
		}
		if (minHeight != -1) {
			minHeight += snappedBottomInset();
			setPrefHeight(minHeight);
		}

		if (animationMap == null) {
			animationMap = new HashMap<>();
		}

		trans.stop();
		ParallelTransition newTransition = new ParallelTransition();
		newTransition.getChildren().addAll(animationMap.values());
		newTransition.play();
		trans = newTransition;
		dirtyBoxes = false;
		performingLayout = false;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void requestLayout() {
		if (performingLayout) {
			return;
		}
		super.requestLayout();
	}

	/**
	 * this method will clear the layout matrix forcing the bin packing algorithm
	 * to recompute the children boxes on the next layout pass
	 */
	public final void clearLayout() {
		matrix = null;
	}

	/***************************************************************************
	 * * Properties * *
	 **************************************************************************/
	/**
	 * the layout mode of Masonry Pane
	 */
	private ObjectProperty<LayoutMode> layoutMode = new SimpleObjectProperty<>(
		LayoutMode.MASONRY);

	public final ObjectProperty<LayoutMode> layoutModeProperty() {
		return this.layoutMode;
	}

	/**
	 * @return the LayoutMode of masonry pane
	 */
	public final LayoutMode getLayoutMode() {
		return this.layoutModeProperty().get();
	}

	/**
	 * sets the layout mode
	 *
	 * @param layoutMode to be used, either MASONRY or BIN_PACKING
	 */
	public final void setLayoutMode(final LayoutMode layoutMode) {
		this.layoutModeProperty().set(layoutMode);
	}

	/**
	 * the cell width of masonry grid
	 */
	private DoubleProperty cellWidth = new SimpleDoubleProperty(70) {

		@Override
		protected void invalidated() {
			requestLayout();
		}
	};

	public final DoubleProperty cellWidthProperty() {
		return this.cellWidth;
	}

	/**
	 * @return the cell width of the masonry pane grid
	 */
	public final double getCellWidth() {
		return this.cellWidthProperty().get();
	}

	/**
	 * sets the cell width of the masonry pane grid
	 *
	 * @param cellWidth of the grid
	 */
	public final void setCellWidth(final double cellWidth) {
		this.cellWidthProperty().set(cellWidth);
	}

	/**
	 * the cell height of masonry grid
	 */
	private DoubleProperty cellHeight = new SimpleDoubleProperty(70) {

		@Override
		protected void invalidated() {
			requestLayout();
		}
	};

	public final DoubleProperty cellHeightProperty() {
		return this.cellHeight;
	}

	/**
	 * @return the cell height of the masonry pane grid
	 */
	public final double getCellHeight() {
		return this.cellHeightProperty().get();
	}

	/**
	 * sets the cell height of the masonry pane grid
	 *
	 * @param cellHeight of the grid
	 */
	public final void setCellHeight(final double cellHeight) {
		this.cellHeightProperty().set(cellHeight);
	}

	/**
	 * horizontal spacing between nodes in grid
	 */
	private DoubleProperty hSpacing = new SimpleDoubleProperty(5) {

		@Override
		protected void invalidated() {
			requestLayout();
		}
	};

	public final DoubleProperty hSpacingProperty() {
		return this.hSpacing;
	}

	/**
	 * @return the horizontal spacing between nodes in the grid
	 */
	public final double getHSpacing() {
		return this.hSpacingProperty().get();
	}

	/**
	 * sets the horizontal spacing in the grid
	 *
	 * @param spacing horizontal spacing
	 */
	public final void setHSpacing(final double spacing) {
		this.hSpacingProperty().set(spacing);
	}

	/**
	 * vertical spacing between nodes in the grid
	 */
	private DoubleProperty vSpacing = new SimpleDoubleProperty(5) {

		@Override
		protected void invalidated() {
			requestLayout();
		}
	};

	public final DoubleProperty vSpacingProperty() {
		return this.vSpacing;
	}

	/**
	 * @return the vertical spacing between nodes in the grid
	 */
	public final double getVSpacing() {
		return this.vSpacingProperty().get();
	}

	/**
	 * sets the vertical spacing in the grid
	 *
	 * @param spacing vertical spacing
	 */
	public final void setVSpacing(final double spacing) {
		this.vSpacingProperty().set(spacing);
	}

	/**
	 * limit the grid columns to certain number
	 */
	private IntegerProperty limitColumn = new SimpleIntegerProperty(-1) {

		@Override
		protected void invalidated() {
			requestLayout();
		}
	};

	public final IntegerProperty limitColumnProperty() {
		return this.limitColumn;
	}

	/**
	 * @return -1 if no limit on grid columns, else returns the maximum number of
	 *         columns to be used in the grid
	 */
	public final int getLimitColumn() {
		return this.limitColumnProperty().get();
	}

	/**
	 * sets the column limit to be used in the grid
	 *
	 * @param limitColumn number of columns to be used in the grid
	 */
	public final void setLimitColumn(final int limitColumn) {
		this.limitColumnProperty().set(limitColumn);
	}

	/**
	 * limit the grid rows to certain number
	 */
	private IntegerProperty limitRow = new SimpleIntegerProperty(100) {

		@Override
		protected void invalidated() {
			requestLayout();
		}
	};

	public final IntegerProperty limitRowProperty() {
		return this.limitRow;
	}

	/**
	 * @return -1 if no limit on grid rows, else returns the maximum number of
	 *         rows to be used in the grid
	 */
	public final int getLimitRow() {
		return this.limitRowProperty().get();
	}

	/**
	 * sets the rows limit to be used in the grid
	 *
	 * @param limitRow number of rows to be used in the grid
	 */
	public final void setLimitRow(final int limitRow) {
		this.limitRowProperty().set(limitRow);
	}

	/***************************************************************************
	 * * Layout Modes * *
	 **************************************************************************/

	public static abstract class LayoutMode {

		public static final MasonryLayout MASONRY = new MasonryLayout();
		public static final BinPackingLayout BIN_PACKING = new BinPackingLayout();

		protected abstract List<BoundingBox> fillGrid(int[][] matrix,
			List<Region> children, double cellWidth, double cellHeight, int limitRow,
			int limitCol, double gutterX, double gutterY);

		/**
		 * returns the available box at the cell (x,y) of the grid that fits the
		 * block if existed
		 *
		 * @param x
		 * @param y
		 * @param block
		 * @return
		 */
		protected BoundingBox getFreeArea(int[][] matrix, int x, int y,
			Region block, double cellWidth, double cellHeight, int limitRow,
			int limitCol, double gutterX, double gutterY)
		{
			double blockHeight = getBLockHeight(block);
			double blockWidth = getBLockWidth(block);

			int rowsNeeded = (int) Math.ceil(blockHeight / (cellHeight + gutterY));
			if (cellHeight * rowsNeeded + (rowsNeeded - 1) * 2 *
				gutterY < blockHeight)
			{
				rowsNeeded++;
			}
			int maxRow = Math.min(x + rowsNeeded, limitRow);

			int colsNeeded = (int) Math.ceil(blockWidth / (cellWidth + gutterX));
			if (cellWidth * colsNeeded + (colsNeeded - 1) * 2 *
				gutterX < blockWidth)
			{
				colsNeeded++;
			}
			int maxCol = Math.min(y + colsNeeded, limitCol);

			int minRow = maxRow;
			int minCol = maxCol;
			for (int i = x; i < minRow; i++) {
				for (int j = y; j < maxCol; j++) {
					if (matrix[i][j] != 0) {
						if (y < j && j < minCol) {
							minCol = j;
						}
					}
				}
			}
			for (int i = x; i < maxRow; i++) {
				for (int j = y; j < minCol; j++) {
					if (matrix[i][j] != 0) {
						if (x < i && i < minRow) {
							minRow = i;
						}
					}
				}
			}
			return new BoundingBox(x, y, minCol - y, minRow - x);
		}

		protected double getBLockWidth(Region region) {
			if (region.getMinWidth() != -1) {
				return region.getMinWidth();
			}
			if (region.getPrefWidth() != USE_COMPUTED_SIZE) {
				return region.getPrefWidth();
			}
			else {
				return region.prefWidth(-1);
			}
		}

		protected double getBLockHeight(Region region) {
			if (region.getMinHeight() != -1) {
				return region.getMinHeight();
			}
			if (region.getPrefHeight() != USE_COMPUTED_SIZE) {
				return region.getPrefHeight();
			}
			else {
				return region.prefHeight(getBLockWidth(region));
			}
		}

		protected boolean validWidth(BoundingBox box, Region region, double cellW,
			double gutterX, double gutterY)
		{
			boolean valid = false;

			if (region.getMinWidth() != -1 && box.getWidth() * cellW + (box
				.getWidth() - 1) * 2 * gutterX < region.getMinWidth())
			{
				return false;
			}

			if (region.getPrefWidth() == USE_COMPUTED_SIZE && box.getWidth() * cellW +
				(box.getWidth() - 1) * 2 * gutterX >= region.prefWidth(-1))
			{
				valid = true;
			}
			if (region.getPrefWidth() != USE_COMPUTED_SIZE && box.getWidth() * cellW +
				(box.getWidth() - 1) * 2 * gutterX >= region.getPrefWidth())
			{
				valid = true;
			}
			return valid;
		}

		protected boolean validHeight(BoundingBox box, Region region, double cellH,
			double gutterX, double gutterY)
		{
			boolean valid = false;
			if (region.getMinHeight() != -1 && box.getHeight() * cellH + (box
				.getHeight() - 1) * 2 * gutterY < region.getMinHeight())
			{
				return false;
			}

			if (region.getPrefHeight() == USE_COMPUTED_SIZE && box.getHeight() *
				cellH + (box.getHeight() - 1) * 2 * gutterY >= region.prefHeight(region
					.prefWidth(-1)))
			{
				valid = true;
			}
			if (region.getPrefHeight() != USE_COMPUTED_SIZE && box.getHeight() *
				cellH + (box.getHeight() - 1) * 2 * gutterY >= region.getPrefHeight())
			{
				valid = true;
			}
			return valid;
		}

		protected int[][] fillMatrix(int[][] matrix, int id, double row, double col,
			double width, double height)
		{
			for (int x = (int) row; x < row + height; x++) {
				for (int y = (int) col; y < col + width; y++) {
					matrix[x][y] = id;
				}
			}
			return matrix;
		}

	}

	/***************************************************************************
	 * * Masonry Layout * *
	 **************************************************************************/

	private static class MasonryLayout extends LayoutMode {

		@Override
		public List<BoundingBox> fillGrid(int[][] matrix, List<Region> children,
			double cellWidth, double cellHeight, int limitRow, int limitCol,
			double gutterX, double gutterY)
		{
			int row = matrix.length;
			if (row <= 0) {
				return null;
			}
			int col = matrix[0].length;
			List<BoundingBox> boxes = new ArrayList<>();

			for (int b = 0; b < children.size(); b++) {
				Region block = children.get(b);
				for (int i = 0; i < row; i++) {
					int old = boxes.size();
					for (int j = 0; j < col; j++) {
						if (matrix[i][j] != 0) {
							continue;
						}

						// masonry condition
						boolean isValidCell = true;
						for (int k = i + 1; k < row; k++) {
							if (matrix[k][j] != 0) {
								isValidCell = false;
								break;
							}
						}
						if (!isValidCell) {
							continue;
						}

						BoundingBox box = getFreeArea(matrix, i, j, block, cellWidth,
							cellHeight, limitRow, limitCol, gutterX, gutterY);
						if (!validWidth(box, block, cellWidth, gutterX, gutterY) ||
							!validHeight(box, block, cellHeight, gutterX, gutterY))
						{
							continue;
						}
						matrix = fillMatrix(matrix, b + 1, box.getMinX(), box.getMinY(), box
							.getWidth(), box.getHeight());
						boxes.add(box);
						break;
					}
					if (boxes.size() != old) {
						break;
					}
					if (i == row - 1) {
						boxes.add(null);
					}
				}
			}
			return boxes;
		}
	}

	/***************************************************************************
	 * * Bin Packing Layout * *
	 **************************************************************************/
	private static class BinPackingLayout extends LayoutMode {

		@Override
		public List<BoundingBox> fillGrid(int[][] matrix, List<Region> children,
			double cellWidth, double cellHeight, int limitRow, int limitCol,
			double gutterX, double gutterY)
		{
			int row = matrix.length;
			if (row <= 0) {
				return null;
			}
			int col = matrix[0].length;
			List<BoundingBox> boxes = new ArrayList<>();

			for (int b = 0; b < children.size(); b++) {
				Region block = children.get(b);
				for (int i = 0; i < row; i++) {
					int old = boxes.size();
					for (int j = 0; j < col; j++) {
						if (matrix[i][j] != 0) {
							continue;
						}
						BoundingBox box = getFreeArea(matrix, i, j, block, cellWidth,
							cellHeight, limitRow, limitCol, gutterX, gutterY);

						// HACK: prevents boxes from disappearing unless under cellWidth *
						// 5, which is usually 250
						// This also allows boxes to resize if the window resizes to some
						// extent
						// Despite considerable troubleshooting, this appears to be the best
						// solution for now.
						// if (!validWidth(box, block, cellWidth, gutterX, gutterY)) {
						if (box.getWidth() < 5) {
							continue;
						}

						matrix = fillMatrix(matrix, b + 1, box.getMinX(), box.getMinY(), box
							.getWidth(), box.getHeight());
						boxes.add(box);
						break;
					}
					if (boxes.size() != old) {
						break;
					}
					if (i == row - 1) {
						boxes.add(null);
					}
				}
			}
			return boxes;
		}
	}

}
