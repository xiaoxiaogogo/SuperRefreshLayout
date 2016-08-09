# SuperRefreshLayout
This is a  layout for pull-to-refresh and pull-down-to-loadmore.It support RecyclerView ,ListView and normal View.(这是一个支持上拉刷新和下拉加载更多的布局控件。它支持RecyclerView ,ListView和普通view)

这个刷新控件的特性:

  1.完美支持内嵌滑动(所以本控件对RecyclerView是完美支持，所有功能以RecyclerView完美实现为主)
  
  2.完美支持RecyclerView的三种LayoutManager(都只支持vertical方向)
  3.完美支持那些不支持内嵌滑动的view（ListView，GridView以及普通的view）
  3.支持两种刷新操作（SwipeRefreshLayout自带的刷新方式 和 将刷新的header拉下来的方式刷新）
  4.支持两种加载更多的操作（上拉footerview加载更多 和 添加尾巴到list上面，滚动到尾巴位置加载更多）
  6.支持在list列表上面添加一个top view（适配一些需求）（GridView不支持）
  7.支持更新数据的同时，判断是否加载完成，如果完成，将不能加载更多
  8.支持自己配置是否支持refresh和loadmore操作
