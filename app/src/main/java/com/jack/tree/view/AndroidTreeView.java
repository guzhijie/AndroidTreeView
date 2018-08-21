package com.jack.tree.view;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import com.jack.tree.R;
import com.jack.tree.holder.SimpleViewHolder;
import com.jack.tree.model.TreeNode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by Bogdan Melnychuk on 2/10/15.
 * modify by jack.gu
 * 增加
 *
 * @see #addNode(TreeNode, TreeNode, int)
 * @see #expandToNode(TreeNode)
 * @see #sortNode(TreeNode, Comparator)
 * @see #findNode(TreeNode.TreeNodeFilter, int)
 * @see #findNode(TreeNode, TreeNode.TreeNodeFilter, int)
 */
public class AndroidTreeView extends ScrollView {
    private static final String NODES_PATH_SEPARATOR = ";";

    protected TreeNode mRoot;
    //    private Context mContext;
    private boolean applyForRoot;
    private int containerStyle = 0;
    private Class<? extends TreeNode.BaseNodeViewHolder> defaultViewHolderClass = SimpleViewHolder.class;
    private TreeNode.TreeNodeClickListener nodeClickListener;
    private TreeNode.TreeNodeLongClickListener nodeLongClickListener;
    private boolean mSelectionModeEnabled;
    private boolean mUseDefaultAnimation = false;
    private boolean use2dScroll = false;
    private boolean enableAutoToggle = true;

    public AndroidTreeView(@NonNull Context context) {
        super(context);
    }

    public AndroidTreeView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

//    public AndroidTreeView(Context context) {
//        mContext = context;
//    }

    public void setRoot(TreeNode mRoot) {
        this.mRoot = mRoot;
        this.addView(this.getView());
    }

//    public AndroidTreeView(Context context, TreeNode root) {
//        mRoot = root;
//        mContext = context;
//    }

    public void setDefaultAnimation(boolean defaultAnimation) {
        this.mUseDefaultAnimation = defaultAnimation;
    }

    public void setDefaultContainerStyle(int style) {
        setDefaultContainerStyle(style, false);
    }

    public void setDefaultContainerStyle(int style, boolean applyForRoot) {
        containerStyle = style;
        this.applyForRoot = applyForRoot;
    }

    public void setUse2dScroll(boolean use2dScroll) {
        this.use2dScroll = use2dScroll;
    }

    public boolean is2dScrollEnabled() {
        return use2dScroll;
    }

    public void setUseAutoToggle(boolean enableAutoToggle) {
        this.enableAutoToggle = enableAutoToggle;
    }

    public boolean isAutoToggleEnabled() {
        return enableAutoToggle;
    }

    public void setDefaultViewHolder(Class<? extends TreeNode.BaseNodeViewHolder> viewHolder) {
        defaultViewHolderClass = viewHolder;
    }

    public void setDefaultNodeClickListener(TreeNode.TreeNodeClickListener listener) {
        nodeClickListener = listener;
    }

    public void setDefaultNodeLongClickListener(TreeNode.TreeNodeLongClickListener listener) {
        nodeLongClickListener = listener;
    }

    public void expandAll() {
        expandNode(mRoot, true);
    }

    public void collapseAll() {
        for (TreeNode n : mRoot.getChildren()) {
            collapseNode(n, true);
        }
    }


    public View getView(int style) {
        Context context = this.getContext();
        final ViewGroup view;
        if (style > 0) {
            ContextThemeWrapper newContext = new ContextThemeWrapper(context, style);
            view = use2dScroll ? new TwoDScrollView(newContext) : new ScrollView(newContext);
        } else {
            view = use2dScroll ? new TwoDScrollView(context) : new ScrollView(context);
        }

        Context containerContext = context;
        if (containerStyle != 0 && applyForRoot) {
            containerContext = new ContextThemeWrapper(context, containerStyle);
        }
        final LinearLayout viewTreeItems = new LinearLayout(containerContext, null, containerStyle);

        viewTreeItems.setId(R.id.tree_items);
        viewTreeItems.setOrientation(LinearLayout.VERTICAL);
        view.addView(viewTreeItems);

        mRoot.setViewHolder(new TreeNode.BaseNodeViewHolder(this.getContext()) {
            @Override
            public View createNodeView(TreeNode node, Object value) {
                return null;
            }

            @Override
            public ViewGroup getNodeItemsView() {
                return viewTreeItems;
            }
        });

        expandNode(mRoot, false);
        return view;
    }

    public View getView() {
        return getView(-1);
    }


    public void expandLevel(int level) {
        for (TreeNode n : mRoot.getChildren()) {
            expandLevel(n, level);
        }
    }

    private void expandLevel(TreeNode node, int level) {
        if (node.getLevel() <= level) {
            expandNode(node, false);
        }
        for (TreeNode n : node.getChildren()) {
            expandLevel(n, level);
        }
    }

    public void expandNode(TreeNode node) {
        expandNode(node, false);
    }

    public void collapseNode(TreeNode node) {
        collapseNode(node, false);
    }

    public String getSaveState() {
        final StringBuilder builder = new StringBuilder();
        getSaveState(mRoot, builder);
        if (builder.length() > 0) {
            builder.setLength(builder.length() - 1);
        }
        return builder.toString();
    }

    public void restoreState(String saveState) {
        if (!TextUtils.isEmpty(saveState)) {
            collapseAll();
            final String[] openNodesArray = saveState.split(NODES_PATH_SEPARATOR);
            final Set<String> openNodes = new HashSet<>(Arrays.asList(openNodesArray));
            restoreNodeState(mRoot, openNodes);
        }
    }

    private void restoreNodeState(TreeNode node, Set<String> openNodes) {
        for (TreeNode n : node.getChildren()) {
            if (openNodes.contains(n.getPath())) {
                expandNode(n);
                restoreNodeState(n, openNodes);
            }
        }
    }

    private void getSaveState(TreeNode root, StringBuilder sBuilder) {
        for (TreeNode node : root.getChildren()) {
            if (node.isExpanded()) {
                sBuilder.append(node.getPath());
                sBuilder.append(NODES_PATH_SEPARATOR);
                getSaveState(node, sBuilder);
            }
        }
    }

    public void toggleNode(TreeNode node) {
        if (node.isExpanded()) {
            collapseNode(node, false);
        } else {
            expandNode(node, false);
        }

    }

    private void collapseNode(TreeNode node, final boolean includeSubnodes) {
        node.setExpanded(false);
        TreeNode.BaseNodeViewHolder nodeViewHolder = getViewHolderForNode(node);

        if (mUseDefaultAnimation) {
            collapse(nodeViewHolder.getNodeItemsView());
        } else {
            nodeViewHolder.getNodeItemsView().setVisibility(View.GONE);
        }
        nodeViewHolder.toggle(false);
        if (includeSubnodes) {
            for (TreeNode n : node.getChildren()) {
                collapseNode(n, includeSubnodes);
            }
        }
    }

    private void expandNode(final TreeNode node, boolean includeSubnodes) {
        node.setExpanded(true);
        final TreeNode.BaseNodeViewHolder parentViewHolder = getViewHolderForNode(node);
        parentViewHolder.getNodeItemsView().removeAllViews();


        parentViewHolder.toggle(true);

        for (final TreeNode n : node.getChildren()) {
            addNode(parentViewHolder.getNodeItemsView(), n);

            if (n.isExpanded() || includeSubnodes) {
                expandNode(n, includeSubnodes);
            }

        }
        if (mUseDefaultAnimation) {
            expand(parentViewHolder.getNodeItemsView());
        } else {
            parentViewHolder.getNodeItemsView().setVisibility(View.VISIBLE);
        }

    }


    /**
     * modify by jack.gu
     *
     * @param container
     * @param n
     */
    private void addNode(ViewGroup container, final TreeNode n) {
        addNode(container, n, container.getChildCount());
    }

    /**
     * add by jack.gu
     *
     * @param container
     * @param n
     * @param index
     */
    private void addNode(ViewGroup container, final TreeNode n, int index) {
        final TreeNode.BaseNodeViewHolder viewHolder = getViewHolderForNode(n);
        final View nodeView = viewHolder.getView();
        //        container.addView(nodeView);
        container.addView(nodeView, index);
        if (mSelectionModeEnabled) {
            viewHolder.toggleSelectionMode(mSelectionModeEnabled);
        }

        nodeView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (n.getClickListener() != null) {
                    n.getClickListener().onClick(n, n.getValue());
                } else if (nodeClickListener != null) {
                    nodeClickListener.onClick(n, n.getValue());
                }
                if (enableAutoToggle) {
                    toggleNode(n);
                }
            }
        });

        nodeView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                if (n.getLongClickListener() != null) {
                    return n.getLongClickListener().onLongClick(n, n.getValue());
                } else if (nodeLongClickListener != null) {
                    return nodeLongClickListener.onLongClick(n, n.getValue());
                }
                if (enableAutoToggle) {
                    toggleNode(n);
                }
                return false;
            }
        });
    }

    //------------------------------------------------------------
    //  Selection methods

    public void setSelectionModeEnabled(boolean selectionModeEnabled) {
        if (!selectionModeEnabled) {
            // TODO fix double iteration over tree
            deselectAll();
        }
        mSelectionModeEnabled = selectionModeEnabled;

        for (TreeNode node : mRoot.getChildren()) {
            toggleSelectionMode(node, selectionModeEnabled);
        }

    }

    public <E> List<E> getSelectedValues(Class<E> clazz) {
        List<E> result = new ArrayList<>();
        List<TreeNode> selected = getSelected();
        for (TreeNode n : selected) {
            Object value = n.getValue();
            if (value != null && value.getClass().equals(clazz)) {
                result.add((E) value);
            }
        }
        return result;
    }

    public boolean isSelectionModeEnabled() {
        return mSelectionModeEnabled;
    }

    private void toggleSelectionMode(TreeNode parent, boolean mSelectionModeEnabled) {
        toogleSelectionForNode(parent, mSelectionModeEnabled);
        if (parent.isExpanded()) {
            for (TreeNode node : parent.getChildren()) {
                toggleSelectionMode(node, mSelectionModeEnabled);
            }
        }
    }

    public List<TreeNode> getSelected() {
        if (mSelectionModeEnabled) {
            return getSelected(mRoot);
        } else {
            return new ArrayList<>();
        }
    }

    // TODO Do we need to go through whole tree? Save references or consider collapsed nodes as not selected
    private List<TreeNode> getSelected(TreeNode parent) {
        List<TreeNode> result = new ArrayList<>();
        for (TreeNode n : parent.getChildren()) {
            if (n.isSelected()) {
                result.add(n);
            }
            result.addAll(getSelected(n));
        }
        return result;
    }

    public void selectAll(boolean skipCollapsed) {
        makeAllSelection(true, skipCollapsed);
    }

    public void deselectAll() {
        makeAllSelection(false, false);
    }

    private void makeAllSelection(boolean selected, boolean skipCollapsed) {
        if (mSelectionModeEnabled) {
            for (TreeNode node : mRoot.getChildren()) {
                selectNode(node, selected, skipCollapsed);
            }
        }
    }

    public void selectNode(TreeNode node, boolean selected) {
        if (mSelectionModeEnabled) {
            node.setSelected(selected);
            toogleSelectionForNode(node, true);
        }
    }

    private void selectNode(TreeNode parent, boolean selected, boolean skipCollapsed) {
        parent.setSelected(selected);
        toogleSelectionForNode(parent, true);
        boolean toContinue = skipCollapsed ? parent.isExpanded() : true;
        if (toContinue) {
            for (TreeNode node : parent.getChildren()) {
                selectNode(node, selected, skipCollapsed);
            }
        }
    }

    private void toogleSelectionForNode(TreeNode node, boolean makeSelectable) {
        TreeNode.BaseNodeViewHolder holder = getViewHolderForNode(node);
        if (holder.isInitialized()) {
            getViewHolderForNode(node).toggleSelectionMode(makeSelectable);
        }
    }

    private TreeNode.BaseNodeViewHolder getViewHolderForNode(TreeNode node) {
        TreeNode.BaseNodeViewHolder viewHolder = node.getViewHolder();
        if (viewHolder == null) {
            try {
                final Object object = defaultViewHolderClass.getConstructor(Context.class).newInstance(this.getContext());
                viewHolder = (TreeNode.BaseNodeViewHolder) object;
                node.setViewHolder(viewHolder);
            } catch (Exception e) {
                throw new RuntimeException("Could not instantiate class " + defaultViewHolderClass);
            }
        }
        if (viewHolder.getContainerStyle() <= 0) {
            viewHolder.setContainerStyle(containerStyle);
        }
        if (viewHolder.getTreeView() == null) {
            viewHolder.setTreeViev(this);
        }
        return viewHolder;
    }

    private static void expand(final View v) {
        v.measure(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        final int targetHeight = v.getMeasuredHeight();

        v.getLayoutParams().height = 0;
        v.setVisibility(View.VISIBLE);
        Animation a = new Animation() {
            @Override
            protected void applyTransformation(float interpolatedTime, Transformation t) {
                v.getLayoutParams().height = interpolatedTime == 1
                        ? LinearLayout.LayoutParams.WRAP_CONTENT
                        : (int) (targetHeight * interpolatedTime);
                v.requestLayout();
            }

            @Override
            public boolean willChangeBounds() {
                return true;
            }
        };

        // 1dp/ms
        a.setDuration((int) (targetHeight / v.getContext().getResources().getDisplayMetrics().density));
        v.startAnimation(a);
    }

    private static void collapse(final View v) {
        final int initialHeight = v.getMeasuredHeight();

        Animation a = new Animation() {
            @Override
            protected void applyTransformation(float interpolatedTime, Transformation t) {
                if (interpolatedTime == 1) {
                    v.setVisibility(View.GONE);
                } else {
                    v.getLayoutParams().height = initialHeight - (int) (initialHeight * interpolatedTime);
                    v.requestLayout();
                }
            }

            @Override
            public boolean willChangeBounds() {
                return true;
            }
        };

        // 1dp/ms
        a.setDuration((int) (initialHeight / v.getContext().getResources().getDisplayMetrics().density));
        v.startAnimation(a);
    }

    //-----------------------------------------------------------------
    //Add / Remove

    public void addNode(TreeNode parent, final TreeNode nodeToAdd) {
        parent.addChild(nodeToAdd);
        if (parent.isExpanded()) {
            final TreeNode.BaseNodeViewHolder parentViewHolder = getViewHolderForNode(parent);
            addNode(parentViewHolder.getNodeItemsView(), nodeToAdd);
        }
    }

    public void removeNode(TreeNode node) {
        if (node.getParent() != null) {
            TreeNode parent = node.getParent();
            int index = parent.deleteChild(node);
            if (parent.isExpanded() && index >= 0) {
                final TreeNode.BaseNodeViewHolder parentViewHolder = getViewHolderForNode(parent);
                parentViewHolder.getNodeItemsView().removeViewAt(index);
            }
        }
    }
    //=========================================add by jack.gu as follows===============================

    /**
     * @param parent
     * @param nodeToAdd 1: if index <= 0 add note to head; 2: if index >= children's length, add to tail
     * @param index
     */
    public void addNode(TreeNode parent, TreeNode nodeToAdd, int index) {
        List<TreeNode> children = parent.getChildren();
        if (index < 0) {
            index = 0;
        } else if (index > children.size()) {
            index = children.size();
        }
        children.add(index, nodeToAdd);
        if (parent.isExpanded()) {
            final TreeNode.BaseNodeViewHolder parentViewHolder = getViewHolderForNode(parent);
            addNode(parentViewHolder.getNodeItemsView(), nodeToAdd, index);
        }
    }

    /**
     * @param parent
     * @param comparator
     */
    public void sortNode(TreeNode parent, Comparator<TreeNode> comparator) {
        List<TreeNode> children = parent.getChildren();
        Collections.sort(children, comparator);
        if (parent.isExpanded()) {
            final TreeNode.BaseNodeViewHolder parentViewHolder = getViewHolderForNode(parent);
            ViewGroup viewGroup = parentViewHolder.getNodeItemsView();
            viewGroup.removeAllViews();
            for (TreeNode node : children) {
                View view = getViewHolderForNode(node).getView();
                viewGroup.addView(view);
            }
        }
    }

    /**
     * @param parent root node of search from
     * @param filter
     * @param level  level for search
     * @return
     */
    public List<TreeNode> findNode(TreeNode parent, TreeNode.TreeNodeFilter filter, int level) {
        if (level <= 0) {
            return new ArrayList<>();
        }
        List<TreeNode> treeNodeList = new ArrayList<>();
        List<TreeNode> children = parent.getChildren();
        for (TreeNode node : children) {
            if (filter.accept(node)) {
                treeNodeList.add(node);
            }
            treeNodeList.addAll(findNode(node, filter, level - 1));
        }
        return treeNodeList;
    }

    public List<TreeNode> findNode(TreeNode.TreeNodeFilter filter, int level) {
        return findNode(mRoot, filter, level);
    }

    public void expandToNode(final TreeNode node) {
        if (null == node) {
            throw new NullPointerException("node is null");
        }
        //获取当前node的所有祖先节点，逆序
        List<TreeNode> forefathers = node.getParents();
        for (TreeNode n : forefathers) {
            n.setExpanded(true);
        }
        expandNode(forefathers.get(forefathers.size() - 1), false);
        this.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                int[] location = new int[2];
                View view = node.getViewHolder().getView().findViewById(R.id.node_header);
                view.getLocationOnScreen(location);
                AndroidTreeView.this.smoothScrollBy(0, location[1] - view.getMeasuredHeight());
                AndroidTreeView.this.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });
    }
}
