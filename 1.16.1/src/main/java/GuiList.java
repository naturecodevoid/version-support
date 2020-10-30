/*
 * Copyright (c) 2019-2020 5zig Reborn
 *
 * This file is part of The 5zig Mod
 * The 5zig Mod is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * The 5zig Mod is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with The 5zig Mod.  If not, see <http://www.gnu.org/licenses/>.
 */

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.mojang.blaze3d.systems.RenderSystem;
import eu.the5zig.mod.MinecraftFactory;
import eu.the5zig.mod.gui.elements.*;
import eu.the5zig.mod.util.MatrixStacks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.widget.ElementListWidget;
import net.minecraft.client.gui.widget.EntryListWidget;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class GuiList<E extends Row> extends ElementListWidget implements IGuiList<E> {
	private MatrixStack matrixStack;
	protected final List<E> rows;
	private final Clickable<E> clickable;

	private int rowWidth = 95;
	private int bottomPadding;
	private boolean leftbound = false;
	private int scrollX;

	private boolean hasSelected = false;

	protected int mouseX, mouseY;

	private int selected;
	private IButton selectedButton;

	private String header;

	private boolean drawDefaultBackground = false;
	private Object backgroundTexture;
	private int backgroundWidth, backgroundHeight;

	private long lastClicked;

	protected List<Integer> heightMap = Lists.newArrayList();
	private boolean renderSelection;

	public class ListElement extends ElementListWidget.Entry {
		private final E element;

		public ListElement(E element) {
			this.element = element;
		}

		@Override
		public List<? extends Element> children() {
			return ImmutableList.of();
		}

		@Override
		public void render(MatrixStack matrixStack, int slotId, int y, int x, int rowLeft, int slotHeight, int mouseX, int mouseY, boolean focused, float partialTicks) {
			if(element instanceof RowExtended) ((RowExtended) element).draw(x, y, slotHeight, mouseX, mouseY);
			else element.draw(x, y);
		}

		@Override
		public boolean mouseClicked(double d, double e, int i) {
			boolean doubleClick = getSelected() == this && MinecraftFactory.getVars().getSystemTime() - GuiList.this.lastClicked < 250L;
			GuiList.this.lastClicked = MinecraftFactory.getVars().getSystemTime();
			if(element instanceof RowExtended) ((RowExtended) element).mousePressed((int)d, (int)e);
			GuiList.this.onSelect(GuiList.this.children().indexOf(this), element, doubleClick);
			return true;
		}

		public E getElement() {
			return element;
		}
	}

	@Override
	protected int getRowLeft() {
		return left;
	}

	@Override
	public void addEntry(int slot, E entry) {
		children().add(slot, new ListElement(entry));
	}

	@Override
	public void removeEntry(E entry) {
		children().removeIf(e -> ((ListElement) e).getElement() == entry);
	}

	@Override
	public void doClearEntries() {
		clearEntries();
	}

	@Override
	protected int getScrollbarPositionX() {
		return right;
	}

	public GuiList(Clickable<E> clickable, int width, int height, int top, int bottom, int left, int right, List<E> rows) {
		super(MinecraftClient.getInstance(), width, height, top, bottom, 18);
		this.rows = rows;
		this.clickable = clickable;
		setLeft(left);
		setRight(right);
		setDrawSelection(true);
		replaceEntries(rows == null ? new ArrayList() : rows.stream().map(ListElement::new).collect(Collectors.toList()));
	}

	@Override
	protected void renderList(MatrixStack matrixStack, int i, int j, int k, int l, float f) {
		int m = this.getItemCount();
		Tessellator tessellator = Tessellator.getInstance();
		BufferBuilder bufferBuilder = tessellator.getBuffer();
		for(int n = 0; n < m; ++n) {
			int o = this.getRowTop(n);
			int p = o + this.itemHeight;
			if (p >= this.top && o <= this.bottom) {
				int q = j + n * this.itemHeight + this.headerHeight;
				int r = this.itemHeight - 4;
				EntryListWidget.Entry entry = this.getEntry(n);
				int s = this.getRowWidth();
				int v;
				if (this.renderSelection && this.isSelectedItem(n)) {
					v = this.left;
					int u = this.left + s;
					RenderSystem.disableTexture();
					float g = this.isFocused() ? 1.0F : 0.5F;
					RenderSystem.color4f(g, g, g, 1.0F);
					bufferBuilder.begin(7, VertexFormats.POSITION);
					bufferBuilder.vertex(v, q + r + 2, 0.0D).next();
					bufferBuilder.vertex(u, q + r + 2, 0.0D).next();
					bufferBuilder.vertex(u, q - 2, 0.0D).next();
					bufferBuilder.vertex(v, q - 2, 0.0D).next();
					tessellator.draw();
					RenderSystem.color4f(0.0F, 0.0F, 0.0F, 1.0F);
					bufferBuilder.begin(7, VertexFormats.POSITION);
					bufferBuilder.vertex(v + 1, q + r + 1, 0.0D).next();
					bufferBuilder.vertex(u - 1, q + r + 1, 0.0D).next();
					bufferBuilder.vertex(u - 1, q - 1, 0.0D).next();
					bufferBuilder.vertex(v + 1, q - 1, 0.0D).next();
					tessellator.draw();
					RenderSystem.enableTexture();
				}
				v = this.getRowLeft();
				entry.render(matrixStack, n, o, v, s, r, k, l, this.isMouseOver(k, l) && Objects.equals(this.getAtPos(k, l), entry), f);
			}
		}
	}

	@Override
	public void callDrawScreen(int mouseX, int mouseY, float partialTicks) {
		render(this.matrixStack == null ? MatrixStacks.hudMatrixStack : matrixStack, mouseX, mouseY, partialTicks);
	}

	@Override
	public void render(MatrixStack matrixStack, int i, int j, float f) {
		this.matrixStack = matrixStack;
		this.mouseX = i;
		this.mouseY = j;
		super.render(matrixStack, i, j, f);
	}

	@Override
	public void callHandleMouseInput() {

	}

	@Override
	public void onSelect(int id, E row, boolean doubleClick) {
		setSelectedId(id);
		if (clickable != null && row != null)
			clickable.onSelect(id, row, doubleClick);
	}

	@Override
	public void mouseClicked(int mouseX, int mouseY) {
		mouseClicked(mouseX, mouseY, 0);
	}

	private Entry getAtPos(double d, double e) {
		int i = this.getRowWidth();
		int j = this.left;
		int k = j - i;
		int l = j + i;
		int m = MathHelper.floor(e - (double)this.top) - this.headerHeight + (int)this.getScrollAmount() - 4;
		int n = m / this.itemHeight;
		return d < (double)this.getScrollbarPositionX() && d >= (double)k && d <= (double)l && n >= 0 && m >= 0 && n < this.getItemCount() ? (Entry) this.children().get(n) : null;
	}

	@Override
	public boolean mouseClicked(double d, double e, int i) {
		this.updateScrollingState(d, e, i);
		if (!this.isMouseOver(d, e)) {
			return false;
		} else {
			EntryListWidget.Entry entry = this.getAtPos(d, e);
			if (entry != null) {
				if (entry.mouseClicked(d, e, i)) {
					this.setFocused(entry);
					this.setDragging(true);
					return true;
				}
			} else if (i == 0) {
				this.clickedHeader((int)(d - (double)(this.left + this.width / 2 - this.getRowWidth() / 2)), (int)(e - (double)this.top) + (int)this.getScrollAmount() - 4);
				return true;
			}

			return i == 0 && d >= (double)this.getScrollbarPositionX() && d < (double)(this.getScrollbarPositionX() + 6);
		}
	}

	@Override
	public void mouseReleased(int mouseX, int mouseY, int state) {
		mouseReleased((double) mouseX, mouseY, state);
	}

	@Override
	public boolean callMouseDragged(double v, double v1, int i, double v2, double v3) {
		return mouseDragged(v, v1, i, v2, v3);
	}

	@Override
	public void setScrollAmount(double d) {
		super.setScrollAmount(isMouseOver(mouseX, mouseY) ? d : 0);
	}

	@Override
	public boolean callMouseScrolled(double v) {
		return mouseScrolled(0, 0, v);
	}

	@Override
	public void scrollToBottom() {

	}

	@Override
	public float getCurrentScroll() {
		return 0;
	}

	@Override
	public void scrollTo(float to) {

	}

	@Override
	public boolean callIsSelected(int id) {
		return isSelectedItem(id);
	}

	@Override
	protected boolean isSelectedItem(int i) {
		return selected == i;
	}

	@Override
	public int callGetContentHeight() {
		return 0;
	}

	@Override
	public int callGetRowWidth() {
		return 0;
	}

	public int getRowWidth() {
		return rowWidth;
	}

	@Override
	public void setRowWidth(int rowWidth) {
		this.rowWidth = rowWidth;
	}

	@Override
	public int getSelectedId() {
		synchronized (rows) {
			if (selected < 0 || selected > rows.size())
				selected = setSelectedId(0);
		}
		return selected;
	}

	@Override
	public int setSelectedId(int selected) {
		synchronized (rows) {
			if (selected < 0 || selected > rows.size())
				selected = 0;
		}
		this.selected = selected;
		this.setSelected(getEntry(selected));
		return selected;
	}

	@Override
	public E getSelectedRow() {
		synchronized (rows) {
			if (rows.isEmpty())
				return null;
			if (selected < 0) {
				selected = 0;
				return rows.get(0);
			}
			while (selected >= rows.size()) {
				selected--;
			}
			return rows.get(selected);
		}
	}

	@Override
	public int getWidth() {
		return width;
	}

	@Override
	public void setWidth(int width) {
		this.width = width;
	}

	@Override
	public int getHeight() {
		return height;
	}

	@Override
	public void setHeight(int height) {
		this.height = height;
	}

	@Override
	public int getHeight(int id) {
		return heightMap.get(id);
	}

	@Override
	public int getTop() {
		return top;
	}

	@Override
	public void setTop(int top) {
		this.top = top;
	}

	@Override
	public int getBottom() {
		return bottom;
	}

	@Override
	public void setBottom(int bottom) {
		this.bottom = bottom;
	}

	@Override
	public int getLeft() {
		return left;
	}

	@Override
	public void setLeft(int left) {
		this.left = left;
	}

	@Override
	public int getRight() {
		return right;
	}

	@Override
	public void setRight(int right) {
		this.right = right;
	}

	@Override
	public int getScrollX() {
		return scrollX;
	}

	@Override
	public void setScrollX(int scrollX) {
		this.scrollX = scrollX;
	}

	@Override
	public boolean isLeftbound() {
		return leftbound;
	}

	@Override
	public void setLeftbound(boolean leftbound) {
		this.leftbound = leftbound;
	}

	@Override
	public boolean isDrawSelection() {
		return renderSelection;
	}

	@Override
	public void setDrawSelection(boolean drawSelection) {
		this.renderSelection = drawSelection;
		method_29344(drawSelection); // setRenderSelection
	}

	@Override
	public int getHeaderPadding() {
		return headerHeight;
	}

	@Override
	public void callSetHeaderPadding(int headerPadding) {
		this.setRenderHeader(headerPadding > 0, headerPadding);
	}

	@Override
	public String getHeader() {
		return header;
	}

	@Override
	public void setHeader(String header) {
		this.header = header;
	}

	@Override
	public int getBottomPadding() {
		return bottomPadding;
	}

	@Override
	public void setBottomPadding(int bottomPadding) {
		this.bottomPadding = bottomPadding;
	}

	@Override
	public E getHoverItem(int mouseX, int mouseY) {
		calculateHeightMap();
		int x1, x2;
		if (leftbound) {
			x1 = getLeft();
			x2 = getLeft() + getRowWidth();
		} else {
			x1 = getLeft() + (getWidth() / 2 - getRowWidth() / 2);
			x2 = getLeft() + getWidth() / 2 + getRowWidth() / 2;
		}
		if (mouseX >= x1 && mouseX <= x2) {
			synchronized (rows) {
				for (int i = 0; i < heightMap.size(); i++) {
					Integer y = (int) (heightMap.get(i) + getTop() + getHeaderPadding() - getCurrentScroll());
					E element = rows.get(i);
					if (mouseY >= y && mouseY <= y + element.getLineHeight()) {
						return element;
					}
				}
			}
		}
		return null;
	}

	@Override
	public boolean isDrawDefaultBackground() {
		return drawDefaultBackground;
	}

	@Override
	public void setDrawDefaultBackground(boolean drawDefaultBackground) {
		this.drawDefaultBackground = drawDefaultBackground;
	}

	@Override
	public Object getBackgroundTexture() {
		return backgroundTexture;
	}

	@Override
	public void setBackgroundTexture(Object backgroundTexture, int imageWidth, int imageHeight) {
		this.backgroundTexture = backgroundTexture;

		if (backgroundTexture != null) {
			double w = imageWidth;
			double h = imageHeight;
			int listWidth = getRight() - getLeft();
			int listHeight = getBottom() - getTop();

			while (w > listWidth && h > listHeight) {
				w -= 1;
				h -= h / w;
			}
			while (w < listWidth || h < listHeight) {
				w += 1;
				h += h / w;
			}
			this.backgroundWidth = (int) w;
			this.backgroundHeight = (int) h;
		}
	}

	@Override
	public List<E> getRows() {
		return rows;
	}

	@Override
	public void calculateHeightMap() {

	}

	/**
	 * @return the size of all rows
	 */
	/* ZIG116
	@Override
	protected int getItemCount() {
		synchronized (rows) {
			return rows.size();
		}
	}

	@Override
	public int getItemHeight() {
		return callGetContentHeight();
	}

	@Override
	protected boolean selectItem(int id, int p_selectItem_2_, double p_selectItem_3_, double p_selectItem_5_) {
		int last = selected;
		selected = id;
		boolean var5 = (GuiList.this.selected >= 0) && (GuiList.this.selected < getItemCount());
		if (var5) {
			if (clickable != null) {
				synchronized (rows) {
					boolean doubleClick = id == last && System.currentTimeMillis() - this.lastClicked < 250L;
					onSelect(id, rows.get(id), doubleClick);
					this.lastClicked = System.currentTimeMillis();
					return true;
				}
			}
		}
		return false;
	}

	@Override
	public void onSelect(int id, E row, boolean doubleClick) {
		setSelectedId(id);
		if (clickable != null && row != null)
			clickable.onSelect(id, row, doubleClick);
	}

	@Override
	public boolean callIsSelected(int id) {
		return isSelectedItem(id);
	}

	@Override
	protected boolean isSelectedItem(int id) {
		return selected == id;
	}

	@Override
	protected void renderBackground(MatrixStack matrixStack) {
	}

	@Override
	public int callGetRowWidth() {
		return getRowWidth();
	}

	@Override
	protected void renderList(MatrixStack matrixStack, int x, int y, int mouseX, int mouseY, float pTicks) {
		_drawSlot(matrixStack, id, x, y, slotHeight, mouseX, mouseY, pTicks);
	}

	protected void _drawSlot(MatrixStack stack, int id, int x, int y, int slotHeight, int mouseX, int mouseY, float pTicks) {
		synchronized (rows) {
			if (id < 0 || id >= rows.size())
				return;
			Row selectedRow = rows.get(id);
			selectedRow.draw(x, y);
			if (selectedRow instanceof RowExtended) {
				((RowExtended) selectedRow).draw(x, y, slotHeight, mouseX, mouseY);
			}
		}
	}

	@Override
	public void callDrawScreen(int mouseX, int mouseY, float partialTicks) {
		drawScreen(mouseX, mouseY, partialTicks);
	}

	public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		calculateHeightMap();
			this.mouseX = mouseX;
			this.mouseY = mouseY;
			this.renderBackground(null);
			int var3 = this.getScrollbarPosition();
			int var4 = var3 + 6;
			this.capYPosition();
			GlStateManager.func_227722_g_();
			GlStateManager.func_227769_y_();
			Tessellator var5 = Tessellator.getInstance();
			BufferBuilder var6 = var5.getBuffer();
			GlStateManager.func_227702_d_(1.0F, 1.0F, 1.0F, 1.0F);
			if (backgroundTexture != null) {
				MinecraftFactory.getVars().bindTexture(backgroundTexture);
				Gui.drawModalRectWithCustomSizedTexture(getLeft(), getTop(), 0, 0, getRight() - getLeft(), getBottom() - getTop(), backgroundWidth, backgroundHeight);
			} else if (drawDefaultBackground || MinecraftFactory.getVars().isPlayerNull()) {
				minecraft.getTextureManager().bindTexture(AbstractGui.BACKGROUND_LOCATION);
				float var7 = 32.0F;
				var6.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
				// pos, tex
				var6.func_225582_a_((double) this.x0, (double) this.y1, 0.0D).func_225583_a_(((float) this.x0 / var7), ((float) (this.y1 + this.getScroll()) / var7))
						.func_225586_a_(32, 32, 32, 255).endVertex();
				var6.func_225582_a_((double) this.x1, (double) this.y1, 0.0D).func_225583_a_(((float) this.x1 / var7), ((float) (this.y1 + this.getScroll()) / var7))
						.func_225586_a_(32, 32, 32, 255).endVertex();
				var6.func_225582_a_((double) this.x1, (double) this.y0, 0.0D).func_225583_a_(((float) this.x1 / var7), ((float) (this.y0 + this.getScroll()) / var7))
						.func_225586_a_(32, 32, 32, 255).endVertex();
				var6.func_225582_a_((double) this.x0, (double) this.y0, 0.0D).func_225583_a_(((float) this.x0 / var7), ((float) (this.y0 + this.getScroll()) / var7))
						.func_225586_a_(32, 32, 32, 255).endVertex();
				var5.draw();
			}
			int var8 = this.x0 + this.width / 2 - this.getRowWidth() / 2 + 2;
			int var9 = this.y0 + 4 - (int) this.getScroll();

			glEnable(GL_SCISSOR_TEST);
			float scaleFactor = MinecraftFactory.getVars().getScaleFactor();
			glScissor((int) Math.ceil(getLeft() * scaleFactor), (int) Math.ceil((getHeight() - getBottom()) * scaleFactor), (int) Math.floor((getRight() - getLeft()) * scaleFactor),
					(int) Math.floor((getBottom() - getTop()) * scaleFactor));
			if (this.renderHeader) {
				this.a(var8, var9, var5);
			}
			this.renderList(var8, var9, mouseX, mouseY, partialTicks);
			glDisable(GL_SCISSOR_TEST);
			byte var10 = 4;
			GlStateManager.func_227740_m_();
			GlStateManager.func_227706_d_(770, 771, 0, 1);
			GlStateManager.func_227700_d_();
			GlStateManager.func_227762_u_(7425);
			GlStateManager.func_227621_I_();
			// Schatten
			int var11 = this.getMaxScroll();
			if (drawDefaultBackground || MinecraftFactory.getVars().isPlayerNull() || var11 > 0) {
				var6.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
				var6.func_225582_a_((double) this.x0, (double) (this.y0 + var10), 0.0D).func_225583_a_(0.0f, 1.0f).func_225586_a_(0, 0, 0, 0).endVertex();
				var6.func_225582_a_((double) this.x1, (double) (this.y0 + var10), 0.0D).func_225583_a_(1.0f, 1.0f).func_225586_a_(0, 0, 0, 0).endVertex();
				var6.func_225582_a_((double) this.x1, (double) this.y0, 0.0D).func_225583_a_(1.0f, 0.0f).func_225586_a_(0, 0, 0, 255).endVertex();
				var6.func_225582_a_((double) this.x0, (double) this.y0, 0.0D).func_225583_a_(0.0f, 0.0f).func_225586_a_(0, 0, 0, 255).endVertex();
				var5.draw();
				var6.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
				var6.func_225582_a_((double) this.x0, (double) this.y1, 0.0D).func_225583_a_(0.0f, 1.0f).func_225586_a_(0, 0, 0, 255).endVertex();
				var6.func_225582_a_((double) this.x1, (double) this.y1, 0.0D).func_225583_a_(1.0f, 1.0f).func_225586_a_(0, 0, 0, 255).endVertex();
				var6.func_225582_a_((double) this.x1, (double) (this.y1 - var10), 0.0D).func_225583_a_(1.0f, 0.0f).func_225586_a_(0, 0, 0, 0).endVertex();
				var6.func_225582_a_((double) this.x0, (double) (this.y1 - var10), 0.0D).func_225583_a_(0.0f, 0.0f).func_225586_a_(0, 0, 0, 0).endVertex();
				var5.draw();
			}
			if (var11 > 0) {
				int var12 = (this.y1 - this.y0) * (this.y1 - this.y0) / this.getItemHeight();
				var12 = MathHelper.clamp(var12, 32, this.y1 - this.y0 - 8);
				int var13 = (int) this.getCurrentScroll() * (this.y1 - this.y0 - var12) / var11 + this.y0;
				if (var13 < this.y0) {
					var13 = this.y0;
				}

				var6.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
				var6.func_225582_a_((double) var3, (double) this.y1, 0.0D).func_225583_a_(0.0f, 1.0f).func_225586_a_(0, 0, 0, 255).endVertex();
				var6.func_225582_a_((double) var4, (double) this.y1, 0.0D).func_225583_a_(1.0f, 1.0f).func_225586_a_(0, 0, 0, 255).endVertex();
				var6.func_225582_a_((double) var4, (double) this.y0, 0.0D).func_225583_a_(1.0f, 0.0f).func_225586_a_(0, 0, 0, 255).endVertex();
				var6.func_225582_a_((double) var3, (double) this.y0, 0.0D).func_225583_a_(0.0f, 0.0f).func_225586_a_(0, 0, 0, 255).endVertex();
				var5.draw();
				var6.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
				var6.func_225582_a_((double) var3, (double) (var13 + var12), 0.0D).func_225583_a_(0.0f, 1.0f).func_225586_a_(128, 128, 128, 255).endVertex();
				var6.func_225582_a_((double) var4, (double) (var13 + var12), 0.0D).func_225583_a_(1.0f, 1.0f).func_225586_a_(128, 128, 128, 255).endVertex();
				var6.func_225582_a_((double) var4, (double) var13, 0.0D).func_225583_a_(1.0f, 0.0f).func_225586_a_(128, 128, 128, 255).endVertex();
				var6.func_225582_a_((double) var3, (double) var13, 0.0D).func_225583_a_(0.0f, 0.0f).func_225586_a_(128, 128, 128, 255).endVertex();
				var5.draw();
				var6.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
				var6.func_225582_a_((double) var3, (double) (var13 + var12 - 1), 0.0D).func_225583_a_(0.0f, 1.0f).func_225586_a_(192, 192, 192, 255).endVertex();
				var6.func_225582_a_((double) (var4 - 1), (double) (var13 + var12 - 1), 0.0D).func_225583_a_(1.0f, 1.0f).func_225586_a_(192, 192, 192, 255).endVertex();
				var6.func_225582_a_((double) (var4 - 1), (double) var13, 0.0D).func_225583_a_(1.0f, 0.0f).func_225586_a_(192, 192, 192, 255).endVertex();
				var6.func_225582_a_((double) var3, (double) var13, 0.0D).func_225583_a_(0.0f, 0.0f).func_225586_a_(192, 192, 192, 255).endVertex();
				var5.draw();
			}

			this.renderDecorations(mouseX, mouseY);
			GlStateManager.func_227619_H_();
			GlStateManager.func_227762_u_(7424);
			GlStateManager.func_227709_e_();
			GlStateManager.func_227737_l_();
		}
		getSelectedRow();
	}
	*/
/* ZIG116
	@Override
	protected void renderList(int x, int y, int p_renderList_3_, int p_renderList_4_, float pTicks) {
		if (leftbound) {
			x = getLeft() + 2;
		}
		Tessellator localckx = Tessellator.getInstance();
		BufferBuilder localciv = localckx.getBuffer();
		for (int rowIndex = 0; rowIndex < heightMap.size(); ++rowIndex) {
			int newY = y + heightMap.get(rowIndex) + getHeaderPadding();
			int slotHeight = rows.get(rowIndex).getLineHeight() - 4;
			if ((newY > getBottom()) || (newY + slotHeight < getTop())) {
				this.updateItemPosition(rowIndex, x, newY, pTicks);
			} else {
				if (isDrawSelection() && (isSelectedItem(rowIndex))) {
					int x1, x2;
					if (leftbound) {
						x1 = getLeft();
						x2 = getLeft() + getRowWidth();
					} else {
						x1 = getLeft() + (getWidth() / 2 - getRowWidth() / 2);
						x2 = getLeft() + getWidth() / 2 + getRowWidth() / 2;
					}
					GLUtil.color(1.0F, 1.0F, 1.0F, 1.0F);
					GlStateManager.func_227621_I_();
					localciv.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
					localciv.func_225582_a_((double) x1, (double) (newY + slotHeight + 2), 0.0D).func_225583_a_(0.0f, 1.0f)
							.func_225586_a_(128, 128, 128, 255).endVertex();
					localciv.func_225582_a_((double) x2, (double) (newY + slotHeight + 2), 0.0D).func_225583_a_(1.0f, 1.0f)
							.func_225586_a_(128, 128, 128, 255).endVertex();
					localciv.func_225582_a_((double) x2, (double) (newY - 2), 0.0D).func_225583_a_(1.0f, 0.0f)
							.func_225586_a_(128, 128, 128, 255).endVertex();
					localciv.func_225582_a_((double) x1, (double) (newY - 2), 0.0D).func_225583_a_(0.0f, 0.0f)
							.func_225586_a_(128, 128, 128, 255).endVertex();
					localciv.func_225582_a_((double) (x1 + 1), (double) (newY + slotHeight + 1), 0.0D).func_225583_a_(0.0f, 1.0f)
							.func_225586_a_(0, 0, 0, 255).endVertex();
					localciv.func_225582_a_((double) (x2 - 1), (double) (newY + slotHeight + 1), 0.0D).func_225583_a_(1.0f, 1.0f)
							.func_225586_a_(0, 0, 0, 255).endVertex();
					localciv.func_225582_a_((double) (x2 - 1), (double) (newY - 1), 0.0D).func_225583_a_(1.0f, 0.0f)
							.func_225586_a_(0, 0, 0, 255).endVertex();
					localciv.func_225582_a_((double) (x1 + 1), (double) (newY - 1), 0.0D).func_225583_a_(0.0f, 0.0f)
							.func_225586_a_(0, 0, 0, 255).endVertex();
					localckx.draw();
					GlStateManager.func_227619_H_();
				}
				renderItem(rowIndex, x, newY, slotHeight, mouseX, mouseY, pTicks);
			}
		}
	}

	@Override
	public boolean callMouseDragged(double v, double v1, int i, double v2, double v3) {
		if ((this.getFocused() != null && this.hasSelected && i == 0) && this.getFocused().mouseDragged(v, v1, i, v2, v3)) {
			return true;
		} else if (this.isVisible() && i == 0 && this.hasSelected && v > getScrollbarPosition() && v < getScrollbarPosition() + 6) {
			if (v1 < (double)this.y0) {
				this.scrollTo(0);
			} else if (v1 > (double)this.y1) {
				this.scrollTo(this.getMaxScroll());
			} else {
				double var10 = this.getMaxScroll();
				if (var10 < 1.0D) {
					var10 = 1.0D;
				}

				int var12 = (int)((float)((this.y1 - this.y0) * (this.y1 - this.y0)) / (float)this.getItemHeight());
				var12 = MathHelper.clamp(var12, 32, this.y1 - this.y0 - 8);
				double var13 = var10 / (double)(this.y1 - this.y0 - var12);
				if (var13 < 1.0D) {
					var13 = 1.0D;
				}

				this.scrollTo((int) (getScroll() + v3 * var13));
				this.capYPosition();
			}

			return true;
		} else {
			return false;
		}
	}

	@Override
	public boolean callMouseScrolled(double var1) {
		if (!this.isVisible() || !(mouseX >= getLeft() && mouseX <= getRight() && mouseY >= getTop() & mouseY <= getBottom())) {
			return false;
		} else {
			this.scrollTo((int) (getScroll() - var1 * (double)this.itemHeight / 2.0D));
			return true;
		}
	}

	@Override
	public void mouseClicked(int mouseX, int mouseY) {
		hasSelected = mouseX >= getLeft() && mouseX <= getRight() && mouseY >= getTop() & mouseY <= getBottom();

		E item = getHoverItem(mouseX, mouseY);
		if(item != null) {
			int id = rows.indexOf(item);
			setSelectedId(id);
			this.selectItem(id, 0, mouseX, mouseY);
		}

		if (hasSelected) {
			synchronized (rows) {
				for (int rowIndex = 0; rowIndex < heightMap.size(); ++rowIndex) {
					int newY = (int) (getTop() + heightMap.get(rowIndex) + getHeaderPadding() - getCurrentScroll());
					int slotHeight = rows.get(rowIndex).getLineHeight() - 4;
					if ((newY <= getBottom()) && (newY + slotHeight >= getTop())) {
						Row row = rows.get(rowIndex);
						if (row instanceof RowExtended) {
							IButton pressed = ((RowExtended) row).mousePressed(mouseX, mouseY);
							if (pressed != null) {
								if (selectedButton != null && pressed != selectedButton)
									selectedButton.mouseClicked(mouseX, mouseY);
								selectedButton = pressed;
								return;
							}
						}
					}
				}
			}
		}

	}*/

	/**
	 * @return x-coordinate of the scroll bar
	 */
	/*ZIG116
	@Override
	protected int getScrollbarPosition() {
		return scrollX > 0 ? scrollX : super.getScrollbarPosition();
	}

	@Override
	public int getItemAtPosition(double x, double y) {
		int var3, var4;
		if (leftbound) {
			var3 = getLeft();
			var4 = getLeft() + getRowWidth();
		} else {
			var3 = getLeft() + getWidth() / 2 - getRowWidth() / 2;
			var4 = getLeft() + getWidth() / 2 + getRowWidth() / 2;
		}
		int var5 = (int) (y - getTop()) - this.headerHeight + (int) getCurrentScroll() - 4;
		int var6 = -1;
		for (int i1 = 0; i1 < heightMap.size(); i1++) {
			Integer integer = heightMap.get(i1);
			Row line = rows.get(i1);
			if (y >= integer && y <= integer + line.getLineHeight()) {
				var6 = i1;
				break;
			}
		}
		return x < this.getScrollbarPosition() && x >= var3 && x <= var4 && var6 >= 0 && var5 >= 0 && var6 < this.getItemCount() ? var6 - 3 : -1;
	}


	@Override
	public boolean mouseReleased(double mouseX, double mouseY, int state) {
		if(this.getFocused() != null) {
			this.getFocused().mouseReleased(mouseX, mouseY, state);
		}
		this.children().forEach(child -> child.mouseReleased(mouseX, mouseY, state));
		return false;
	}

	@Override
	public void mouseReleased(int mouseX, int mouseY, int state) {
		if (this.selectedButton != null && state == 0) {
			this.selectedButton.callMouseReleased(mouseX, mouseY);
			this.selectedButton = null;
		}
	}

	@Override
	public void callHandleMouseInput() {

	}

	@Override
	public void scrollToBottom() {
		scrollTo(getItemHeight());
	}

	@Override
	public float getCurrentScroll() {
		return (float) getScroll();
	}

	@Override
	public void scrollTo(float to) {
		// Reset scroll (1.14)
		this.scroll(-this.getScroll());
		this.scroll((int) to);
	}

	@Override
	public int callGetContentHeight() {
		int height = bottomPadding + (getHeaderPadding() > 0 ? (getHeaderPadding() + 8) : 0);
		List<E> chatLines = Lists.newArrayList(rows);
		for (Row row : chatLines) {
			height += row.getLineHeight();
		}
		return height;
	}

	public void calculateHeightMap() {
		heightMap.clear();

		int curHeight = getHeaderPadding();
		List<E> chatLines = Lists.newArrayList(rows);
		for (Row row : chatLines) {
			heightMap.add(curHeight);
			curHeight += row.getLineHeight();
		}
	}

	public int getRowWidth() {
		return rowWidth;
	}

	@Override
	public void setRowWidth(int rowWidth) {
		this.rowWidth = rowWidth;
	}

	@Override
	public int getSelectedId() {
		synchronized (rows) {
			if (selected < 0 || selected > rows.size())
				selected = setSelectedId(0);
		}
		return selected;
	}

	@Override
	public int setSelectedId(int selected) {
		synchronized (rows) {
			if (selected < 0 || selected > rows.size())
				selected = 0;
		}
		this.selected = selected;
		return selected;
	}

	@Override
	public E getSelectedRow() {
		synchronized (rows) {
			if (rows.isEmpty())
				return null;
			if (selected < 0) {
				selected = 0;
				return rows.get(0);
			}
			while (selected >= rows.size()) {
				selected--;
			}
			return rows.get(selected);
		}
	}

	@Override
	public int getWidth() {
		return width;
	}

	@Override
	public void setWidth(int width) {
		this.width = width;
	}

	@Override
	public int getHeight() {
		return height;
	}

	@Override
	public void setHeight(int height) {
		this.height = height;
	}

	@Override
	public int getHeight(int id) {
		return heightMap.get(id);
	}

	@Override
	public int getTop() {
		return y0;
	}

	@Override
	public void setTop(int top) {
		this.y0 = top;
	}

	@Override
	public int getBottom() {
		return y1;
	}

	@Override
	public void setBottom(int bottom) {
		this.y1 = bottom;
	}

	@Override
	public int getLeft() {
		return x0;
	}

	@Override
	public void setLeft(int left) {
		this.x0 = left;
	}

	@Override
	public int getRight() {
		return x1;
	}

	@Override
	public void setRight(int right) {
		this.x1 = right;
	}

	@Override
	public int getScrollX() {
		return scrollX;
	}

	@Override
	public void setScrollX(int scrollX) {
		this.scrollX = scrollX;
	}

	@Override
	public boolean isLeftbound() {
		return leftbound;
	}

	@Override
	public void setLeftbound(boolean leftbound) {
		this.leftbound = leftbound;
	}

	@Override
	public boolean isDrawSelection() {
		return renderSelection;
	}

	@Override
	public void setDrawSelection(boolean drawSelection) {
		this.renderSelection = drawSelection;
	}

	@Override
	public int getHeaderPadding() {
		return headerHeight;
	}

	@Override
	public void callSetHeaderPadding(int headerPadding) {
		this.setRenderHeader(headerPadding > 0, headerPadding);
	}

	@Override
	public String getHeader() {
		return header;
	}

	@Override
	public void setHeader(String header) {
		this.header = header;
	}

	protected void a(int x, int y, Tessellator tesselator) {
		if (header != null) {
			MinecraftFactory.getVars().drawCenteredString(ChatColor.UNDERLINE.toString() + ChatColor.BOLD.toString() + header, getLeft() + (getRight() - getLeft()) / 2,
					Math.min(getTop() + 5, y));
		}
	}

	@Override
	public int getBottomPadding() {
		return bottomPadding;
	}

	@Override
	public void setBottomPadding(int bottomPadding) {
		this.bottomPadding = bottomPadding;
	}

	@Override
	public E getHoverItem(int mouseX, int mouseY) {
		calculateHeightMap();
		int x1, x2;
		if (leftbound) {
			x1 = getLeft();
			x2 = getLeft() + getRowWidth();
		} else {
			x1 = getLeft() + (getWidth() / 2 - getRowWidth() / 2);
			x2 = getLeft() + getWidth() / 2 + getRowWidth() / 2;
		}
		if (mouseX >= x1 && mouseX <= x2) {
			synchronized (rows) {
				for (int i = 0; i < heightMap.size(); i++) {
					Integer y = (int) (heightMap.get(i) + getTop() + getHeaderPadding() - getCurrentScroll());
					E element = rows.get(i);
					if (mouseY >= y && mouseY <= y + element.getLineHeight()) {
						return element;
					}
				}
			}
		}
		return null;
	}

	@Override
	public boolean isDrawDefaultBackground() {
		return drawDefaultBackground;
	}

	@Override
	public void setDrawDefaultBackground(boolean drawDefaultBackground) {
		this.drawDefaultBackground = drawDefaultBackground;
	}

	@Override
	public Object getBackgroundTexture() {
		return backgroundTexture;
	}

	@Override
	public void setBackgroundTexture(Object backgroundTexture, int imageWidth, int imageHeight) {
		this.backgroundTexture = backgroundTexture;

		if (backgroundTexture != null) {
			double w = imageWidth;
			double h = imageHeight;
			int listWidth = getRight() - getLeft();
			int listHeight = getBottom() - getTop();

			while (w > listWidth && h > listHeight) {
				w -= 1;
				h -= h / w;
			}
			while (w < listWidth || h < listHeight) {
				w += 1;
				h += h / w;
			}
			this.backgroundWidth = (int) w;
			this.backgroundHeight = (int) h;
		}
	}

	@Override
	public List<E> getRows() {
		return rows;
	}*/
}
