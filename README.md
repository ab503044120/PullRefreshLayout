# PullRefreshLayout
#
![演示gif](gif/demo_gif.gif)

## 1.概述
对所有基础控件(包括，嵌套滑动例如RecyclerView、NestedScrollView，普通的TextView、ListView、ScrollerView、LinearLayout等)提供下拉刷新、上拉加载的支持,处理了横向滑动冲突（例如:顶部banner的情况）
，且实现无痕过度。

### gradle 

## 2.说明  
支持所有基础控件
<br/>
<br/>
#### loading 出现效果默认6种，demo给出4中效果(STATE_FOLLOW、STATE_PLACEHOLDER_FOLLOW、STATE_CENTER、STATE_PLACEHOLDER_CENTER)
![STATE_FOLLOW](gif/show_demo_1.gif)![STATE_PLACEHOLDER_FOLLOW](gif/show_demo_2.gif)![STATE_PLACEHOLDER_CENTER](gif/show_demo_3.gif)![STATE_CENTER](gif/show_demo_4.gif)

```
//-控件设置-
    refreshLayout.autoRefresh();// 自动刷新
    refreshLayout.setOverScrollDampingRatio(0.2f);//  值越大overscroll越短 default 0.2
    refreshLayout.setAdjustTwinkDuring(3);// 值越大overscroll越慢 default 3
    refreshLayout.setScrollInterpolator(interpolator);// 设置scroller的插值器
    refreshLayout.setLoadMoreEnable(true);// 上拉加载是否可用 default false
    refreshLayout.setDuringAdjustValue(10f);// 动画执行时间调节，越大动画执行越慢 default 10f
    // 刷新或加载完成后回复动画执行时间，为-1时，根据setDuringAdjustValue（）方法实现 default 300
    refreshLayout.setRefreshBackTime(300);
    refreshLayout.setDragDampingRatio(0.6f);// 阻尼系数 default 0.6
    refreshLayout.setPullFlowHeight(400);// 拖拽最大范围，为-1时拖拽范围不受限制 default -1
    refreshLayout.setRefreshEnable(false);// 下拉刷新是否可用 default false
    refreshLayout.setPullTwinkEnable(true);// 回弹是否可用 default true 
    refreshLayout.setAutoLoadingEnable(true);// 自动加载是否可用 default false
    
    // headerView和footerView需实现PullRefreshLayout.OnPullListener接口调整状态
    refreshLayout.setHeaderView(headerView);// 设置headerView
    refreshLayout.setFooterView(footerView);// 设置footerView
    
    /**
    * 设置header或者footer的的出现方式,默认6种方式
    * STATE_FOLLOW, STATE_PLACEHOLDER_FOLLOW, STATE_PLACEHOLDER_CENTER
    * , STATE_CENTER, STATE_CENTER_FOLLOW, STATE_FOLLOW_CENTER
    */
    refreshLayout.setRefreshShowGravity(RefreshShowHelper.STATE_CENTER,RefreshShowHelper.STATE_CENTER);
    // 自定义header或者footer的出现效果
    refreshLayout.setCustomShowRefresh(
        new RefreshShowHelper.IShowRefresh() {
                @Override
                public void offsetRatio(FrameLayout refreshParent, View refreshView, float ratio) {
                    refresh.setY(parent.getHeight()/2);
                }
            }，null);
    
    // PullRefreshLayout.OnPullListener
        public interface OnPullListener {
            // 刷新或加载过程中位置相刷新或加载触发位置的百分比，时刻调用
            void onPullChange(float percent);
            void onPullReset();// 数据重置调用
            void onPullHoldTrigger();// 拖拽超过触发位置调用
            void onPullHoldUnTrigger();// 拖拽回到触发位置之前调用
            void onPullHolding(); // 正在刷新
            void onPullFinish();// 刷新完成
        }
```
## 3.demo用到的库
 loading 动画
 <br/>
 AVLoadingIndicatorView(https://github.com/81813780/AVLoadingIndicatorView)