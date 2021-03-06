package com.gome.ecmall.home.homepage;

import java.util.ArrayList;
import java.util.Timer;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.gome.ecmall.bean.Product;
import com.gome.ecmall.bean.Product.FilterType;
import com.gome.ecmall.bean.SearchResult;
import com.gome.ecmall.custom.LoadingDialog;
import com.gome.ecmall.custom.NoResultView;
import com.gome.ecmall.framework.AbsSubActivity;
import com.gome.ecmall.home.category.ProductListAdapter;
import com.gome.ecmall.home.category.ProductListAdapter.OnProductClickListener;
import com.gome.ecmall.home.category.ProductShowActivity;
import com.gome.ecmall.util.AppMeasurementUtils;
import com.gome.ecmall.util.CommonUtility;
import com.gome.ecmall.util.Constants;
import com.gome.ecmall.util.ConvertUtils;
import com.gome.ecmall.util.NetUtility;
import com.gome.eshopnew.R;

/**
 * 搜索结果列表 筛选后有新SearchFilterResultActivity类似结构 如修改，两处都对等修改
 */
public class SearchResultActivity extends AbsSubActivity implements OnItemClickListener, OnScrollListener,
        OnClickListener {

    public static final String INTENT_KEY_WORDS = "keyWords";
    public static final String CURRENTFITERTYPEID = "currentFiterTypeId";
    public static final String INTENT_KEY_TYPE_INDEX = "index";
    public static final String INTENT_KEY_CURRENT_CHECK_INDEX = "checkedIndex";
    public static final String INTENT_KEY_IS_GRID_MODE = "isGridMode";
    private boolean isGridMode = true;
    public static final int MODE_GRID = 1;
    public static final int MODE_LIST = 2;
    public static final String TAG = "SearchResultActivity";
    public static final String calssType = "SearchResultActivity";
    private String keyWords;
    private TextView tvTitle;
    private Button btnBack;
    private Button btnFilter;
    private Button btnList;
    private Button btnGird;
    private LinearLayout layoutSwitch;
    private ListView listView;
    private LinearLayout loadingView;
    private TextView tvEmpty;
    private ImageView no_net_img;
    private int currentPage = 1;
    private int currentSortBy = Product.SORTBY_HOT;
    private LayoutInflater inflater;
    private String currentFiterTypeId = "";
    private boolean hasMoreData = true;
    private ProductListAdapter productListAdapter;
    private AlertDialog filterDialog;
    private int filterTypeIndex = -1;
    private final int[] LABEL_IDS = new int[] { R.string.product_hot, R.string.product_sale, R.string.product_price,
            R.string.product_time };
    private final int[] PRODUCT_ORDERS = new int[] { Product.SORTBY_HOT, Product.SORTBY_SALE_DESC,
            Product.SORTBY_PRICE_ASC, Product.SORTBY_TIME_DESC };
    private final int[] TAB_BTNS_ID = new int[] { R.id.product_list_tab_item_hot, R.id.product_list_tab_item_sale,
            R.id.product_list_tab_item_price, R.id.product_list_tab_item_time };
    private TextView[] tabBtns = new TextView[LABEL_IDS.length];
    private int checkedIndex = 0;
    private Drawable downDrawable;
    private Drawable upDrawable;
    private Handler handler;
    private ArrayList<FilterType> filterTypes;
    private LinearLayout mContainer;
    private LinearLayout mContainerTab;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.home_search_result_main);
        keyWords = getIntent().getStringExtra(INTENT_KEY_WORDS);
        currentFiterTypeId = getIntent().getStringExtra(CURRENTFITERTYPEID);
        setupView();
        recordSearchRecord(keyWords);
        setData();
    }

    @Override
    protected void onResume() {

        super.onResume();
    }

    private void setupView() {
        inflater = LayoutInflater.from(this);
        tvTitle = (TextView) findViewById(R.id.common_title_tv_text);
        tvTitle.setOnClickListener(this);
        btnBack = (Button) findViewById(R.id.common_title_btn_back);
        btnBack.setVisibility(View.VISIBLE);
        btnBack.setText(R.string.back);
        btnBack.setOnClickListener(this);
        btnFilter = (Button) findViewById(R.id.common_title_btn_right);
        btnFilter.setVisibility(View.VISIBLE);
        btnFilter.setText(R.string.filter);
        layoutSwitch = (LinearLayout) inflater.inflate(R.layout.common_product_display_switch_layout, null);
        btnGird = (Button) layoutSwitch.findViewById(R.id.category_product_display_type_grid);
        btnList = (Button) layoutSwitch.findViewById(R.id.category_product_display_type_list);
        btnList.setSelected(true);
        listView = (ListView) findViewById(R.id.home_search_result_product_list);
        loadingView = (LinearLayout) inflater.inflate(R.layout.common_loading_layout, null);
        listView.addHeaderView(layoutSwitch);
        tvEmpty = (TextView) findViewById(android.R.id.empty);
        no_net_img = (ImageView) findViewById(R.id.no_net_img);
        downDrawable = getResources().getDrawable(R.drawable.product_rank_down_bg_selector);
        upDrawable = getResources().getDrawable(R.drawable.product_rank_up_bg_selector);
        initRankTabs();
        mContainer = (LinearLayout) findViewById(R.id.container);
        mContainerTab = (LinearLayout) findViewById(R.id.result_tabs_container);
        setListViewMode(true);
    }

    private void initRankTabs() {
        for (int i = 0; i < TAB_BTNS_ID.length; i++) {
            tabBtns[i] = (TextView) findViewById(TAB_BTNS_ID[i]);
        }
    }

    private void setData() {
        initRankTabs();
        currentSortBy = PRODUCT_ORDERS[checkedIndex];
        tabBtns[checkedIndex].setSelected(true);
        reloadData(keyWords, currentFiterTypeId, currentSortBy);
    }

    private void reloadData(final String keyWords, final String filterTypeId, final int sortBy) {
        if (!NetUtility.isNetworkAvailable(SearchResultActivity.this)) {
            CommonUtility.showMiddleToast(SearchResultActivity.this, "",
                    getString(R.string.can_not_conntect_network_please_check_network_settings));
            no_net_img.setVisibility(View.VISIBLE);
            listView.setVisibility(View.GONE);
            return;
        }
        no_net_img.setVisibility(View.GONE);
        listView.setVisibility(View.VISIBLE);
        // 重新加载数据时取消加载更多的操作
        if (asyncTask != null) {
            asyncTask.cancel(true);
        }
        currentPage = 1;
        new AsyncTask<Object, Void, SearchResult>() {
            LoadingDialog loadingDialog = null;

            @Override
            protected void onPreExecute() {
                loadingDialog = CommonUtility.showLoadingDialog(SearchResultActivity.this, getString(R.string.loading),
                        true, new DialogInterface.OnCancelListener() {

                            @Override
                            public void onCancel(DialogInterface dialog) {
                                cancel(true);
                            }

                        });
            }

            @Override
            protected SearchResult doInBackground(Object... params) {
                String request = SearchResult.createRequestSearchResultJson(keyWords, filterTypeId, currentPage,
                        Constants.PAGE_SIZE, sortBy);
                String response = NetUtility.sendHttpRequestByPost(Constants.URL_PRODUCT_KEYWORD_SEARCH, request);
                if (NetUtility.NO_CONN.equals(response)) {
                    return null;
                }
                return SearchResult.parseSearchResult(response);
            }

            @Override
            protected void onPostExecute(SearchResult result) {
                if (isCancelled()) {
                    return;
                }
                loadingDialog.dismiss();
                if (result == null) {
                    CommonUtility.showMiddleToast(SearchResultActivity.this, "",
                            getString(R.string.data_load_fail_exception));
                    return;
                }
                ArrayList<Product> products = result.getProductList();
                if (products == null || products.size() == 0) {
                    bindTheEmptyView();
                } else {
                    bindDataToView(keyWords, result, products);
                }
            }

        }.execute();
    }

    /**
     * 绑定空数据视图
     */
    protected void bindTheEmptyView() {
        listView.setVisibility(View.GONE);
        NoResultView noResult = new NoResultView(this, keyWords);
        mContainer.addView(noResult, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        //隐藏选项卡
        hideTabs(true);
    }
    
    /**
     * 以藏选项卡
     * true-隐藏   false-显示
     */
    private void hideTabs(boolean isHide){
    	mContainerTab.setVisibility(isHide?View.GONE:View.VISIBLE);
    	btnFilter.setVisibility(isHide?View.GONE:View.VISIBLE);
    	if(isHide){
    		tvTitle.setText("未搜索到相关商品");
    	}
    }

    /**
     * 绑定数据到列表
     * 
     * @param keyWords
     * @param result
     * @param products
     */
    private void bindDataToView(final String keyWords, SearchResult result, ArrayList<Product> products) {
        hideTabs(false);
    	if (products.size() < Constants.PAGE_SIZE) {// 没有商品可取
            hasMoreData = false;
            if (listView.getFooterViewsCount() > 0) {
                // listView.removeFooterView(loadingView);
                ((TextView) loadingView.findViewById(R.id.common_loading_title)).setVisibility(View.INVISIBLE);
                ((TextView) loadingView.findViewById(R.id.common_loading_title)).setText(R.string.no_more);
                ((ProgressBar) loadingView.findViewById(R.id.loadingbar)).setVisibility(View.GONE);
            }
        } else {// 还可以继续取
            ((TextView) loadingView.findViewById(R.id.common_loading_title)).setVisibility(View.VISIBLE);
            if (listView.getFooterViewsCount() == 0) {
                listView.addFooterView(loadingView);
            } else {
                ((TextView) loadingView.findViewById(R.id.common_loading_title)).setText(R.string.load_more);
                ((ProgressBar) loadingView.findViewById(R.id.loadingbar)).setVisibility(View.VISIBLE);
            }
            hasMoreData = true;
            currentPage++;
        }
        if (productListAdapter == null) {
            productListAdapter = new ProductListAdapter(SearchResultActivity.this, products, calssType);
            listView.setAdapter(productListAdapter);
            listView.setEmptyView(tvEmpty);
            tvTitle.setOnClickListener(SearchResultActivity.this);
            tvTitle.setText(SearchResultActivity.this.keyWords + "(" + result.getCount() + ")");
            AppMeasurementUtils appMeasurementUtils = new AppMeasurementUtils(SearchResultActivity.this);
            if ("0".equals(result.getCount()) || TextUtils.isEmpty(result.getCount())) {
                appMeasurementUtils.getUrl(getString(R.string.appMeas_searchlist) + ":" + keyWords,
                        getString(R.string.appMeas_searchlist),
                        getString(R.string.appMeas_searchlist) + ":" + keyWords,
                        getString(R.string.appMeas_searchlist_page_noresult), getString(R.string.appMeas_search_page),
                        "", AppMeasurementUtils.EVENT_SEARCHRESULT_NORESULT, "", "", "", "", "",
                        getString(R.string.appMeas_mysearch), "", keyWords, "", null);
            } else {
                appMeasurementUtils.getUrl(getString(R.string.appMeas_searchlist) + ":" + keyWords,
                        getString(R.string.appMeas_searchlist),
                        getString(R.string.appMeas_searchlist) + ":" + keyWords,
                        getString(R.string.appMeas_searchlist_page), getString(R.string.appMeas_search_page), "",
                        AppMeasurementUtils.EVENT_SEARCHRESULT, "", "", "", "", "",
                        getString(R.string.appMeas_mysearch), "", keyWords, result.getCount(), null);
            }
            btnFilter.setOnClickListener(SearchResultActivity.this);
            btnGird.setOnClickListener(SearchResultActivity.this);
            btnList.setOnClickListener(SearchResultActivity.this);
            tvEmpty.setText(R.string.empty);
            listView.setOnItemClickListener(SearchResultActivity.this);
            listView.setOnScrollListener(SearchResultActivity.this);
            productListAdapter.setClickListener(new OnProductClickListener() {
                @Override
                public void onProductClick(Product product) {
                    Intent intent = new Intent(SearchResultActivity.this, ProductShowActivity.class);
                    intent.putExtra(ProductShowActivity.INTENT_KEY_GOODS_NO, product.getGoodsNo());
                    intent.putExtra("fromPage", getString(R.string.appMeas_searchlist));
                    recordProductBrowseHistory(product);
                    startActivity(intent);
                }
            });
            for (int i = 0; i < tabBtns.length; i++) {
                tabBtns[i].setOnClickListener(SearchResultActivity.this);
            }
            filterTypes = new ArrayList<Product.FilterType>();
            filterTypes = result.getFilterTypeList();
            // initFilterDialog();// 初始化筛选对话框
            onCreateDialog(-1);
            afterTimeSelectListView();
        } else {
            // 设置筛选后title 显示的 商品数量
            tvTitle.setText(SearchResultActivity.this.keyWords + "(" + result.getCount() + ")");
            productListAdapter.reload(products);
            listView.setSelection(1);
        }
    }

    private AsyncTask<Object, Void, SearchResult> asyncTask = null;

    /**
     * 加载更多列表项
     * 
     * @param goodsTypeId
     *            商品类型ID
     * @param page
     *            页码
     * @param sortBy
     *            排序方式
     */
    private void loadMoreData(final String keyWords, final String filterTypeId, final int page, final int sortBy) {
        if (!NetUtility.isNetworkAvailable(SearchResultActivity.this)) {
            CommonUtility.showMiddleToast(SearchResultActivity.this, "",
                    getString(R.string.can_not_conntect_network_please_check_network_settings));
            return;
        }
        if (asyncTask != null) {
            return;
        }
        asyncTask = new AsyncTask<Object, Void, SearchResult>() {

            @Override
            protected SearchResult doInBackground(Object... params) {
                String request = SearchResult.createRequestSearchResultJson(keyWords, filterTypeId, page,
                        Constants.PAGE_SIZE, sortBy);
                String response = NetUtility.sendHttpRequestByPost(Constants.URL_PRODUCT_KEYWORD_SEARCH, request);
                if (NetUtility.NO_CONN.equals(response)) {
                    return null;
                }
                return SearchResult.parseSearchResult(response);
            }

            @Override
            protected void onCancelled() {
                asyncTask = null;
            }

            @Override
            protected void onPostExecute(SearchResult result) {
                if (isCancelled()) {
                    return;
                }
                if (result == null) {
                    CommonUtility.showMiddleToast(SearchResultActivity.this, "",
                            getString(R.string.data_load_fail_exception));
                    return;
                }
                ArrayList<Product> products = result.getProductList();
                if (products.size() < Constants.PAGE_SIZE) {// 没有商品可取
                    hasMoreData = false;
                    if (listView.getFooterViewsCount() > 0) {
                        // listView.removeFooterView(loadingView);
                        ((TextView) loadingView.findViewById(R.id.common_loading_title)).setVisibility(View.INVISIBLE);
                        ((TextView) loadingView.findViewById(R.id.common_loading_title)).setText(R.string.no_more);
                        ((ProgressBar) loadingView.findViewById(R.id.loadingbar)).setVisibility(View.GONE);
                    }
                } else {// 还可以继续取
                    ((TextView) loadingView.findViewById(R.id.common_loading_title)).setVisibility(View.VISIBLE);
                    if (listView.getFooterViewsCount() == 0) {
                        listView.addFooterView(loadingView);
                    } else {
                        ((TextView) loadingView.findViewById(R.id.common_loading_title)).setText(R.string.load_more);
                        ((ProgressBar) loadingView.findViewById(R.id.loadingbar)).setVisibility(View.VISIBLE);
                    }
                    hasMoreData = true;
                    currentPage++;
                }
                productListAdapter.addList(products);
                asyncTask = null;
            }
        };
        asyncTask.execute();
    }

    protected Dialog onCreateDialog(int id) {
        int size = filterTypes.size();
        if (filterTypes == null || size == 0) {
            return null;
        }
        String[] itemLabels = new String[size];
        for (int i = 0; i < size; i++) {
            FilterType filterType = filterTypes.get(i);
            itemLabels[i] = filterType.getTypeName();
        }
        filterDialog = new AlertDialog.Builder(this).setTitle("筛选")
                .setSingleChoiceItems(itemLabels, filterTypeIndex, new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        removeDialog(-1);
                        FilterType filterType = filterTypes.get(which);
                        int filterTypeIndex = which;
                        String currentFiterTypeId = filterType.getTypeId();
                        currentPage = 1;
                        Intent intent = new Intent();
                        intent.setClass(SearchResultActivity.this, SearchFilterResultActivity.class);
                        intent.putExtra(SearchResultActivity.INTENT_KEY_WORDS, keyWords);
                        intent.putExtra(SearchResultActivity.CURRENTFITERTYPEID, currentFiterTypeId);
                        intent.putExtra(SearchResultActivity.INTENT_KEY_TYPE_INDEX, filterTypeIndex);
                        intent.putExtra(SearchResultActivity.INTENT_KEY_CURRENT_CHECK_INDEX, checkedIndex);
                        intent.putExtra(SearchResultActivity.INTENT_KEY_IS_GRID_MODE,
                                productListAdapter.getItemViewType(0) == MODE_GRID ? true : false);
                        startActivity(intent);
                        // reloadData(keyWords, currentFiterTypeId,
                        // currentSortBy);

                    }
                }).create();
        return filterDialog;
    }

    @Override
    public void onClick(View v) {
        if (v == btnBack) {
            goback();
        } else if (v == tvTitle) {
            if (listView.getAdapter() != null) {
                listView.setSelection(1);
            }
        } else if (v == btnFilter) {
            if (filterDialog == null) {
                CommonUtility.showToast(this, "没有筛选信息!");
            } else {
                showDialog(-1);
            }
        } else if (v == btnGird) {
            btnGird.setSelected(true);
            btnList.setSelected(false);
            if (productListAdapter != null) {
            	setListViewMode(false);
                productListAdapter.setToGridMode(true);
            }
        } else if (v == btnList) {
            btnList.setSelected(true);
            btnGird.setSelected(false);
            if (productListAdapter != null) {
            	setListViewMode(true);
                productListAdapter.setToGridMode(false);
            }
        } else if (v == tvTitle) {
            // 点击标题栏定位到第一个
            if (productListAdapter != null && productListAdapter.getCount() > 0) {
                listView.setSelection(1);
            }
        } else {
            // 点击顶部排序栏按钮
            for (int i = 0; i < tabBtns.length; i++) {
                if (v == tabBtns[i]) {
                    if (checkedIndex != i) {// 点击了不同的标签卡
                        currentSortBy = PRODUCT_ORDERS[i];
                        if (currentSortBy == Product.SORTBY_PRICE_ASC) {
                            tabBtns[i].setCompoundDrawablesWithIntrinsicBounds(null, null, upDrawable, null);
                        }
                        checkedIndex = i;
                        tabBtns[i].setSelected(true);
                        currentPage = 1;
                        reloadData(keyWords, currentFiterTypeId, currentSortBy);
                    } else {
                        if (i == 2) {// 价格可以切换状态
                            if (currentSortBy == Product.SORTBY_PRICE_DESC) {
                                currentSortBy = Product.SORTBY_PRICE_ASC;
                                tabBtns[i].setCompoundDrawablesWithIntrinsicBounds(null, null, upDrawable, null);
                                reloadData(keyWords, currentFiterTypeId, currentSortBy);
                            } else if (currentSortBy == Product.SORTBY_PRICE_ASC) {
                                currentSortBy = Product.SORTBY_PRICE_DESC;
                                tabBtns[i].setCompoundDrawablesWithIntrinsicBounds(null, null, downDrawable, null);
                                reloadData(keyWords, currentFiterTypeId, currentSortBy);
                            }
                        }
                    }
                } else {
                    tabBtns[i].setSelected(false);
                }
            }
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        // if (position > 0) {
        // ArrayList<Product> list = productListAdapter.getItem(position - 1);
        // Intent intent = new Intent(this, ProductShowActivity.class);
        // Product product = list.get(0);
        // intent.putExtra(ProductShowActivity.INTENT_KEY_GOODS_NO,
        // product.getGoodsNo());
        // recordProductBrowseHistory(product);
        // startActivity(intent);
        // }
    }

    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {

    }

    @Override
    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
        if (firstVisibleItem + visibleItemCount == totalItemCount) {// 滑动到底部
            if (hasMoreData && productListAdapter != null && productListAdapter.getCount() > 0) {
                loadMoreData(keyWords, currentFiterTypeId, currentPage, currentSortBy);
            }
        }
    }

    /**
     * 启动Timer执行
     */
    public void afterTimeSelectListView() {
        Timer timer = new Timer();
        initHandler(timer);
        // 启动timer任务
        timer.schedule(new MyTask(), 1000);
    }

    private void initHandler(final Timer timer) {
        if (handler == null) {
            handler = new Handler() {
                @Override
                public void dispatchMessage(Message msg) {

                    super.dispatchMessage(msg);
                    listView.setSelection(1);
                    timer.cancel();
                }
            };
        }
    }

    class MyTask extends java.util.TimerTask {

        @Override
        public void run() {

            handler.sendEmptyMessage(0);
        }

    }
    
    /**
     * 判断是否为列表模式
     * @param isList
     */
    private void setListViewMode(boolean isList){
    	if(isList){
    		listView.setPadding(0, 0, 0, 0);
    	}else{
    		int size = ConvertUtils.dip2px(8.0f, this);
    		listView.setPadding(size, 0, size, 0);
    	}
    }

}
