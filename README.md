# PullRefreshLayout
[![Download](https://api.bintray.com/packages/yan157/maven/pullrefreshlayout/images/download.svg)](https://bintray.com/yan157/maven/pullrefreshlayout/_latestVersion) 
[![MinSdk](https://img.shields.io/badge/API-11%2B-brightgreen.svg?style=flat)](https://android-arsenal.com/api?level=11) 
[![Methods](https://img.shields.io/badge/Methods%20and%20size-342%20%7C%2034%20KB-e91e63.svg)](http://www.methodscount.com/?lib=com.yan%3Apullrefreshlayout%3A1.6.4)
### [DEMO下载](https://github.com/genius158/PullRefreshLayout/raw/master/demo.apk)
![演示gif](gif/demo.gif) 

###### 首先吐槽一下现在流行的刷新库，一个字大，包涵个人很多集成到项目中不需要的类，也很难找到很满意的效果(个人目标效果：各个刷新状态各种手势操作无限，真实的回弹效果，无痕过度)。
## 1.概述
对所有基础控件(包括，嵌套滑动例如RecyclerView、NestedScrollView，普通的TextView、ListView、ScrollerView、LinearLayout等)提供下拉刷新、上拉加载的支持
，且实现无痕过度，随意定制header和footer，移动的或者不可以动的，完全不受限制（动画可以onPullChange()完全自主设置）。

## gradle  [ ![Download](https://api.bintray.com/packages/yan157/maven/pullrefreshlayout/images/download.svg) ](https://bintray.com/yan157/maven/pullrefreshlayout/_latestVersion) ↘
compile 'com.yan:pullrefreshlayout:(↖)'
<br/>
## 2.说明  
支持所有基础控件
<br/>
<br/>
#### loading 出现效果默认(STATE_PLACEHOLDER、STATE_FOLLOW、STATE_PLACEHOLDER_FOLLOW、STATE_CENTER、STATE_PLACEHOLDER_CENTER、STATE_FOLLOW_CENTER、STATE_CENTER_FOLLOW)
![STATE_PLACEHOLDER](gif/placeholder.gif)![STATE_FOLLOW](gif/follow.gif)![STATE_PLACEHOLDER_FOLLOW](gif/placeholder_follow.gif)
![STATE_PLACEHOLDER_CENTER](gif/placeholder_center.gif)![STATE_CENTER](gif/center.gif)
![STATE_FOLLOW_CENTER](gif/follow_center.gif)![STATE_CENTER_FOLLOW](gif/center_follow.gif)

```
//-控件设置-
    refreshLayout.autoRefresh();// 自动刷新
    refreshLayout.setOverScrollDampingRatio(0.2f);//  值越大overscroll越短 default 0.2
    refreshLayout.setAdjustTwinkDuring(3);// 值越大overscroll越慢 default 3
    refreshLayout.setScrollInterpolator(interpolator);// 设置scroller的插值器
    
    refreshLayout.setAnimationDuring(300);// 动画总时长，不包括overScroll动画 default 300
    refreshLayout.setDragDampingRatio(0.6f);// 阻尼系数 default 0.6
    
    refreshLayout.setOverScrollAdjustValue(1f);// 用于控制overscroll时间 default 1f ,越大overscroll的时间越长
    refreshLayout.setOverScrollMaxTriggerOffset(300);// 用于控制overscroll的距离 default 80dp

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
    
     refreshLayout.getRefreshState();// 得到当前的刷新状态 刷新中:1 加载中:2 无状态:0
     refreshLayout.getOverScrollState();// 得到当前overScroll状态 头部下拉:1 尾部上拉:2 无状态:0
     
    refreshLayout.setRefreshTriggerDistance(200);// 设置下拉刷新触发位置，默认为header的高度 default 60dp
    refreshLayout.setLoadTriggerDistance(200);// 设置上拉加载触发位置，默认为footer的高度 default 60dp
    refreshLayout.setPullLimitDistance(400);// 拖拽最大范围，为-1时拖拽范围不受限制 default -1

    refreshLayout.setTargetView(nestedScrollView);// 设置目标view，可以改变滑动判断效果 见 BEHAIVOR2
   
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
    * STATE_FOLLOW, STATE_PLACEHOLDER_FOLLOW, STATE_PLACEHOLDER_CENTER
    * , STATE_CENTER, STATE_CENTER_FOLLOW, STATE_FOLLOW_CENTER
    * ,STATE_PLACEHOLDER
    */
    refreshLayout.setRefreshShowGravity(RefreshShowHelper.STATE_CENTER,RefreshShowHelper.STATE_CENTER);
    refreshLayout.setHeaderShowGravity(RefreshShowHelper.STATE_CENTER)// header出现动画
    refreshLayout.setFooterShowGravity(RefreshShowHelper.STATE_CENTER)// footer出现动画
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
        app:prl_footerShowGravity="stateFollow"
        app:prl_footerViewId="@layout/header_or_footer"
        app:prl_headerViewId="@layout/header_or_footer"
        app:prl_headerClass="com.yan.refreshloadlayouttest.testactivity.PlaceHolderHeader"
        app:prl_footerClass="com.yan.refreshloadlayouttest.testactivity.PlaceHolderHeader"
        app:prl_headerShowGravity="statePlaceholder"
        app:prl_loadMoreEnable="true"
        app:prl_loadTriggerDistance="70dp"
        app:prl_overScrollDampingRatio="0.2"
        app:prl_overScrollMaxTriggerOffset="80dp"
        app:prl_pullLimitDistance="150dp"
        app:prl_refreshEnable="true"
        app:prl_refreshTriggerDistance="90dp"
        app:prl_overScrollAdjustValue="1"
        app:prl_twinkEnable="true">     
```

## 3.版本说明
 version:1.2.7 ： 滑动顺滑柔和度已达到预期效果，各个刷新状态各种手势操作无限制，强迫症患者最佳体验
 <br/>
 version:1.3.0 ： 状态触发调整，之前finish和holding触发次数没有控制,代码微调
 <br/>
 version:1.3.4 ： add xml setting  
 <br/>
 version:1.4.1 ： 增加overScroll距离限制的控制setOverScrollMaxTriggerOffset(offset)
 <br/>
 version:1.4.7 ： 单独对recyclerView overscroll 做判断，解决快速滑动响应过慢的问题，代码小调整
 <br/>
 version:1.5.4 ： 增加ListView和ScrollerView overscroll 状态下 ，fling 返回的处理
 <br/>
 version:1.5.6 ： ScrollerView overscroll 状态下 ，fling 返回的处理 bug 处理
 <br/>
 version:1.5.7 ： 调整界面稳定后再触发刷新，除去卡顿隐患
 <br/>
 version:1.5.9 ： 打开margin设置 ，调整内容头部设置，可见demo中的NestedActivity和CommonActivity
 <br/>
 version:1.6.0 ： 平滑处理，方法调整 attr 调整
 <br/>
 version:1.6.2 ： 由于invalidate()有时候不生效 所以用 ViewCompat.postInvalidateOnAnimation(this) 代替 invalidate()
 
 <br/>
 version:1.6.4 ： 稳定版(Stable version)
 
## 4.demo用到的库
 loading 动画
 <br/>
 AVLoadingIndicatorView(https://github.com/81813780/AVLoadingIndicatorView)
