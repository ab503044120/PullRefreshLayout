# PullRefreshLayout(这是一个专注回弹和手势操作无阻塞的刷新库)
[![Stable Version](https://img.shields.io/badge/Stable%20Version-2.0.3-brightgreen.svg)](https://github.com/genius158/PullRefreshLayout) 
[![Latest Version](https://img.shields.io/badge/Latest%20Version-2.0.3-FFD54F.svg)](https://bintray.com/yan157/maven/pullrefreshlayout/_latestVersion) 
[![MinSdk](https://img.shields.io/badge/MinSdk-11%2B-green.svg)](https://android-arsenal.com/api?level=11) 
[![Methods](https://img.shields.io/badge/Methods%20and%20size-393%20%7C%2036%20KB-e91e63.svg)](http://www.methodscount.com/?lib=com.yan%3Apullrefreshlayout%3A2.0.2)
### [DEMO下载](https://github.com/genius158/PullRefreshLayout/raw/master/demo.apk)
header和状态切换演示gif
<br/>
![header和状态切换演示gif](gif/swipe_refresh.gif)
![header和状态切换演示gif](gif/new_demo.gif) 

fun header 来自from https://github.com/scwang90/SmartRefreshLayout
<br/>

![header和状态切换演示gif](gif/fun_header.gif) 

<br/>

![演示gif](gif/demo.gif) 

## 1.概述
#### 本库的主要特点:和与其他回弹刷新库相比更加真实的回弹效果、即使控件不可滑动,也有惯性缓冲效果(ps:如何触发——比如下拉到一定距离不放，往回滑动，即可看到效果)
对所有基础控件(包括，嵌套滑动例如RecyclerView、NestedScrollView，普通的TextView、ListView、ScrollerView、LinearLayout等)提供下拉刷新、上拉加载的支持
，且实现无痕过度，和与其他库相比更真实的回弹效果(（即使不是滑动控件）也有惯性缓冲效果)，随意定制header和footer，移动的或者不可以动的，完全不受限制（动画可以onPullChange()完全自主设置）。
<br/>
ps:本库没有做解耦处理（那样会增加.class，大小也会增加），目的是使库足够小，而且本库功能目的明确，不必做无用功。

## gradle  [![Stable Version](https://img.shields.io/badge/Stable%20Version-2.0.3-brightgreen.svg)](https://github.com/genius158/PullRefreshLayout)  ↘
compile 'com.yan:pullrefreshlayout:(↖)'
<br/>
## 2.说明  
支持所有基础控件
<br/>
<br/>
#### loading 出现效果默认(PLACEHOLDER、FOLLOW、PLACEHOLDER_FOLLOW、CENTER、PLACEHOLDER_CENTER、FOLLOW_CENTER、CENTER_FOLLOW、FOLLOW_PLACEHOLDER)
![PLACEHOLDER](gif/placeholder.gif)
![FOLLOW](gif/follow.gif)
![PLACEHOLDER_FOLLOW](gif/placeholder_follow.gif)
![PLACEHOLDER_CENTER](gif/placeholder_center.gif)
![CENTER](gif/center.gif)
![FOLLOW_CENTER](gif/follow_center.gif)
![CENTER_FOLLOW](gif/center_follow.gif)

```
//-控件设置-
    refreshLayout.autoRefresh();// 自动刷新
    refreshLayout.setOverScrollDampingRatio(0.2f);//  值越大overscroll越短 default 0.2
    refreshLayout.setAdjustTwinkDuring(3);// 值越大overscroll越慢 default 3
    refreshLayout.setScrollInterpolator(interpolator);// 设置scroller的插值器
    
    refreshLayout.setAnimationDuring(300);// 动画总时长，不包括overScroll动画 default 300
    refreshLayout.setDragDampingRatio(0.6f);// 阻尼系数 default 0.6
    
    refreshLayout.setOverScrollAdjustValue(1f);// 用于控制overscroll时间 default 1f ,越大overscroll的时间越长
    refreshLayout.setOverScrollMaxTriggerOffset(300);// 用于控制overscroll的距离 default 50dp

    refreshLayout.setRefreshEnable(false);// 下拉刷新是否可用 default true
    refreshLayout.setLoadMoreEnable(true);// 上拉加载是否可用 default false
    refreshLayout.setTwinkEnable(true);// 回弹是否可用 default true 
    refreshLayout.setAutoLoadingEnable(true);// 自动加载是否可用 default false
    
    // headerView和footerView需实现PullRefreshLayout.OnPullListener接口调整状态
    refreshLayout.setHeaderView(headerView);// 设置headerView
    refreshLayout.setFooterView(footerView);// 设置footerView
    
    refreshLayout.isLayoutMoving();// 是否处于移动的过程中
    refreshLayout.isRefreshing();// 是否处于刷新加载状态
    
    refreshLayout.isTwinkEnable();// 是否开启回弹
    refreshLayout.isRefreshEnable();// 是否开启刷新
    refreshLayout.isLoadMoreEnable();// 是否开启加载更多
    
    refreshLayout.isMovingDirectDown();// 是否处于向下移动的趋势
    
    refreshLayout.isDragUp();// 是否正在向上拖拽
    refreshLayout.isDragDown();// 是否正在向下拖拽
    refreshLayout.isRefreshing();// 是否正在刷新
    refreshLayout.isLoading();// 是否正在加载
    refreshLayout.isOverScrollDown();// 是否正在向下越界回弹
    refreshLayout.isOverScrollUp();// 是否正在向上越界回弹
    
    refreshLayout.isHoldingTrigger();// 是否已经触发刷新或加载
    refreshLayout.isHoldingFinishTrigger();// 是否已经触发刷新或加载完毕
     
    refreshLayout.getMoveDistance();// 得到refreshlayout的移动距离
    refreshlayout.getRefreshTriggerDistance();// 得到下拉刷新的触发距离
    refreshlayout.getLoadingTriggerDistance();// 得到上拉加载的触发距离
     
    refreshLayout.setRefreshTriggerDistance(200);// 设置下拉刷新触发位置，默认为header的高度 default 60dp
    refreshLayout.setLoadTriggerDistance(200);// 设置上拉加载触发位置，默认为footer的高度 default 60dp
    refreshLayout.setPullUpLimitDistance(400);// 向上拖拽最大范围，默认控件高度
    refreshLayout.setPullDownLimitDistance(400);// 向下拖拽最大范围，默认控件高度

    refreshLayout.setTargetView(nestedScrollView);// 设置目标view，可以改变滑动判断
   
    refreshLayout.setDispatchPullTouchAble(false);// 是否阻止pullrefreshLayout的默认事件分发(下拉滑动的逻辑)
    refreshLayout.setFooterFront(true);// 设置footer前置 default false
    refreshLayout.setHeaderFront(true);// 设置header前置 default false
    refreshLayout.setMoveWithFooter(true);// 设置footer跟随移动 default true
    refreshLayout.setMoveWithContent(true);// 设置直接子view跟随移动 default true
    refreshLayout.setMoveWithHeader(true);// 设置header跟随移动 default true
 
    refreshLayout.cancelAllAnimation();//取消所有正在执行的动画
    refreshLayout.cancelTouchEvent();//主动执行ACTION_CANCEL事件
  
    refreshLayout.moveChildren(0);// 移动子view
   
    refreshLayout.setOnDragIntercept(PullRefreshLayout.OnDragIntercept);// 设置滑动判定 见 BEHAIVOR2
    public static class OnDragIntercept {
        public boolean onHeaderDownIntercept() {// header下拉之前的拦截事件
            return true;// true将拦截子view的滑动
        }
        public boolean onFooterUpIntercept() {// footer上拉之前的拦截事件
            return true;// true将拦截子view的滑动
        }
    }
    
    /**
    * 设置header或者footer的的出现方式,默认7种方式
    * FOLLOW,FOLLOW_PLACEHOLDER, PLACEHOLDER_FOLLOW
    * , FOLLOW_CENTER, PLACEHOLDER_CENTER
    * , CENTER, CENTER_FOLLOW
    * , PLACEHOLDER
    */
    refreshLayout.setRefreshShowGravity(RefreshShowHelper.CENTER,RefreshShowHelper.CENTER);
    refreshLayout.setHeaderShowGravity(RefreshShowHelper.CENTER)// header出现动画
    refreshLayout.setFooterShowGravity(RefreshShowHelper.CENTER)// footer出现动画
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
         
    <!-- xml setting -->     
    <com.yan.pullrefreshlayout.PullRefreshLayout xmlns:android="http://schemas.android.com/apk/res/android"
        app:prl_autoLoadingEnable="false"
        app:prl_dragDampingRatio="0.6"
        app:prl_animationDuring="300"
        app:prl_footerShowGravity="follow"
        app:prl_footerViewId="@layout/header_or_footer"
        app:prl_headerViewId="@layout/header_or_footer"
        app:prl_headerClass="com.yan.refreshloadlayouttest.testactivity.PlaceHolderHeader"
        app:prl_footerClass="com.yan.refreshloadlayouttest.testactivity.PlaceHolderHeader"
        app:prl_headerShowGravity="statePlaceholder"
        app:prl_loadMoreEnable="true"
        app:prl_loadTriggerDistance="70dp"
        app:prl_overScrollDampingRatio="0.2"
        app:prl_overScrollMaxTriggerOffset="80dp"
        app:prl_pullDownLimitDistance="150dp"
        app:prl_pullUpLimitDistance="150dp"
        app:prl_headerFront="true"
        app:prl_footerFront="true"
        app:prl_targetId="@+id/recyclerView"
        app:prl_refreshEnable="true"
        app:prl_refreshTriggerDistance="90dp"
        app:prl_overScrollAdjustValue="1"
        app:prl_twinkEnable="true">     
        
        
        <!-- 通过以下例子，你可以轻易实现recyclerView(任何View)的header，和数据错误、网络错误等的状态切换--> 
        <com.yan.pullrefreshlayout.PullRefreshLayout xmlns:android="http://schemas.android.com/apk/res/android"
            xmlns:app="http://schemas.android.com/apk/res-auto"
            android:id="@+id/refreshLayout"
            android:layout_width="match_parent"
            android:layout_height="match_parent"
            app:prl_targetId="@+id/recyclerView">
        
            <!-- 包装层，实现了嵌套滑动的功能，也可以是普通的FrameLayout(实现机制不同) -->
            <com.yan.refreshloadlayouttest.widget.NestedFrameLayout
                android:layout_width="match_parent"
                android:layout_height="match_parent">
        
                <!-- 数据添装 -->
                <android.support.v7.widget.RecyclerView
                    android:id="@id/recyclerView"
                    android:layout_width="match_parent"
                    android:layout_height="match_parent"
                    android:background="#f1f1f1"
                    android:overScrollMode="never" />
         
                <!-- header -->
                <android.support.v7.widget.CardView
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content">
        
                    <TextView
                        android:layout_width="match_parent"
                        android:layout_height="46dp"
                        android:gravity="center"
                        android:text="header"
                        android:textSize="18sp" />
                </android.support.v7.widget.CardView>
        
                <!-- 状态显示界面 -->
                <TextView
                    android:id="@+id/no_data"
                    android:background="#ffffff"
                    android:gravity="center"
                    android:layout_width="match_parent"
                    android:layout_height="match_parent"
                    android:textColor="#212121"
                    android:textSize="20sp"
                    android:text="no data click to try again"  />
            </com.yan.refreshloadlayouttest.widget.NestedFrameLayout>
        
        </com.yan.pullrefreshlayout.PullRefreshLayout>

```

## 3.版本说明
  
 version:1.7.3 ： 事件切换优化
 <br/>
 version:1.7.4 ： 一直处于触摸状态下的，执行setTargetView（）方法，增加cancel掉事件
 <br/>
 version:1.7.5 ： 修复事件状态错误问题
 <br/>
 version:1.7.8 ： 调整由于onStartNestedScroll()先于onNestedPreFling()执行的二次handleAction
 <br/>
 version:1.8.5 ： 优化flingBack效果 
 
 <br/>
 version:2.0.0 ： 新增SwipeRefreshLayout效果,优化
 <br/>
 version:2.0.2 ： 新增viewpager实例
 <br/>
 version:2.0.3 ： dragState 状态调整
 
## 4.demo用到的库
 loading 动画
 <br/>
 AVLoadingIndicatorView(https://github.com/81813780/AVLoadingIndicatorView)
