package com.yan.pullrefreshlayout;

import android.content.Context;
import android.graphics.Rect;
import android.os.Build;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.View;
import android.view.WindowManager;
import android.widget.AbsListView;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;

class PullRefreshLayoutUtil {

    private PullRefreshLayoutUtil() {
    }

    /**
     * @return Whether it is possible for the child view of this layout to
     * scroll up. Override this if the child view is a custom view.
     */
    static boolean canChildScrollUp(View targetView) {
        if (Build.VERSION.SDK_INT < 14) {
            if (targetView instanceof AbsListView) {
                final AbsListView absListView = (AbsListView) targetView;
                return absListView.getChildCount() > 0
                        && (absListView.getFirstVisiblePosition() > 0 || absListView.getChildAt(0)
                        .getTop() < absListView.getPaddingTop());
            } else {
                return ViewCompat.canScrollVertically(targetView, -1) || targetView.getScrollY() > 0;
            }
        } else {
            return ViewCompat.canScrollVertically(targetView, -1);
        }
    }

    /**
     * @return Whether it is possible for the child view of this layout to
     * scroll down. Override this if the child view is a custom view.
     */
    static boolean canChildScrollDown(View targetView) {
        if (Build.VERSION.SDK_INT < 14) {
            if (targetView instanceof AbsListView) {
                final AbsListView absListView = (AbsListView) targetView;
                if (absListView.getChildCount() > 0) {
                    int lastChildBottom = absListView.getChildAt(absListView.getChildCount() - 1).getBottom();
                    return absListView.getLastVisiblePosition() == absListView.getAdapter().getCount() - 1
                            && lastChildBottom <= absListView.getMeasuredHeight();
                } else {
                    return false;
                }
            } else {
                return ViewCompat.canScrollVertically(targetView, 1) || targetView.getScrollY() > 0;
            }
        } else {
            return ViewCompat.canScrollVertically(targetView, 1);
        }
    }

    /**
     * ---------------------- recyclerView can scroll -----------------------
     *
     * @param recyclerView
     * @return
     */
    static boolean isRecyclerViewToTop(RecyclerView recyclerView) {
        if (recyclerView != null) {
            RecyclerView.LayoutManager manager = recyclerView.getLayoutManager();
            if (manager == null) {
                return true;
            }
            if (manager.getItemCount() == 0) {
                return true;
            }

            int firstChildTop = 0;
            if (recyclerView.getChildCount() > 0) {
                View firstVisibleChild = recyclerView.getChildAt(0);
                if (firstVisibleChild != null && firstVisibleChild.getMeasuredHeight() >= recyclerView.getMeasuredHeight()) {
                    if (Build.VERSION.SDK_INT < 14) {
                        return !(ViewCompat.canScrollVertically(recyclerView, -1) || recyclerView.getScrollY() > 0);
                    } else {
                        return !ViewCompat.canScrollVertically(recyclerView, -1);
                    }
                }

                View firstChild = recyclerView.getChildAt(0);
                RecyclerView.LayoutParams layoutParams = (RecyclerView.LayoutParams) firstChild.getLayoutParams();
                firstChildTop = firstChild.getTop() - layoutParams.topMargin - getRecyclerViewItemTopInset(layoutParams) - recyclerView.getPaddingTop();
            }
            if (manager instanceof LinearLayoutManager) {
                LinearLayoutManager layoutManager = (LinearLayoutManager) manager;
                if (layoutManager.findFirstCompletelyVisibleItemPosition() < 1 && firstChildTop == 0) {
                    return true;
                }
            } else if (manager instanceof StaggeredGridLayoutManager) {
                StaggeredGridLayoutManager layoutManager = (StaggeredGridLayoutManager) manager;
                int[] out = layoutManager.findFirstCompletelyVisibleItemPositions(null);
                if (out[0] < 1 && firstChildTop == 0) {
                    return true;
                }
            }
        }
        return false;
    }

    static boolean isRecyclerViewToBottom(RecyclerView recyclerView) {
        if (recyclerView != null) {
            RecyclerView.LayoutManager manager = recyclerView.getLayoutManager();
            if (manager == null || manager.getItemCount() == 0) {
                return false;
            }

            if (manager instanceof LinearLayoutManager) {
                View lastVisibleChild = recyclerView.getChildAt(recyclerView.getChildCount() - 1);
                if (lastVisibleChild != null && lastVisibleChild.getMeasuredHeight() >= recyclerView.getMeasuredHeight()) {
                    if (Build.VERSION.SDK_INT < 14) {
                        return !(ViewCompat.canScrollVertically(recyclerView, 1) || recyclerView.getScrollY() < 0);
                    } else {
                        return !ViewCompat.canScrollVertically(recyclerView, 1);
                    }
                }

                LinearLayoutManager layoutManager = (LinearLayoutManager) manager;
                if (layoutManager.findLastCompletelyVisibleItemPosition() == layoutManager.getItemCount() - 1) {
                    return true;
                }
            } else if (manager instanceof StaggeredGridLayoutManager) {
                StaggeredGridLayoutManager layoutManager = (StaggeredGridLayoutManager) manager;

                int[] out = layoutManager.findLastCompletelyVisibleItemPositions(null);
                int lastPosition = layoutManager.getItemCount() - 1;
                for (int position : out) {
                    if (position == lastPosition) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static int getRecyclerViewItemTopInset(RecyclerView.LayoutParams layoutParams) {
        try {
            Field field = RecyclerView.LayoutParams.class.getDeclaredField("mDecorInsets");
            field.setAccessible(true);
            Rect decorInsets = (Rect) field.get(layoutParams);
            return decorInsets.top;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    /**
     * common utils
     *
     * @param context
     * @return
     */
    static int getWindowHeight(Context context) {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        windowManager.getDefaultDisplay().getMetrics(displayMetrics);
        return displayMetrics.heightPixels;
    }

    static int dipToPx(Context context, float value) {
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, metrics);
    }

    /**
     * parseClassName
     *
     * @param context   context
     * @param className className
     * @return
     */
    static View parseClassName(Context context, String className) {
        if (!TextUtils.isEmpty(className)) {
            try {
                final Class<?>[] CONSTRUCTOR_PARAMS = new Class<?>[]{Context.class};
                final Class<View> clazz = (Class<View>) Class.forName(className, true, context.getClassLoader());
                Constructor<View> constructor = clazz.getConstructor(CONSTRUCTOR_PARAMS);
                constructor.setAccessible(true);
                return constructor.newInstance(context);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }
}