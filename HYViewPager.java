package com.example.administrator.testmap;

import android.content.Context;
import android.graphics.Color;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;



import java.util.ArrayList;

/**
 *  Created by huangyi on 16/3/24.</br>
 * 自定义ViewPager自动滚动</br>
 *    1.在xml或者代码中初始化 HYViewPager</br>
 *    2.设置显示的图片 {@link #setViews(ArrayList, boolean)}</br> 
 *    3.若需要处理图片的点击事件 setOnBannerClick(OnBannerClick)</br>
 *  @see #setOnBannerClick(OnBannerClick)
 *  @author huangyi
 *  @version 1.0
 *
 *
 */
public class HYViewPager extends RelativeLayout implements ViewPager.OnPageChangeListener, View.OnClickListener {

    private static final String TAG = "HYViewPager";
    private int messageWhat = 0;//循环消息的what
    /**
     * 右下角标记
     */
    public static final int RIGHT_MARKER = 1001;
    /**
     * 居中标记
     */
    public static final int CENTER_MARKER = 1002;
    private int MARKER = RIGHT_MARKER;
    private MyPagerAdapter mMyPagerAdapter;//适配器
    private View[] mPoints;//圆点
    private ArrayList<ImageView> mPagers = new ArrayList<>();//滚动的View
    private ViewPager mViewPager;//ViewPager
    private Context mContext;
    private LinearLayout linPoints;//圆点的View
    private boolean canAUTO = true;//自动滚
    private int AUTOSpace = 2000;//滚动间隔
    private LayoutParams paramsPoints;//控制圆点显示的位置
    private Handler handler;
    private boolean isAuto;
    //解决4张以下网络图片加载的BUG
    private boolean isNetImage;
    private int originSize;
    private int needToCopy;


    public HYViewPager(Context context) {
        super(context);
        mContext = context;
        initViewPager();
    }

    public HYViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        initViewPager();

    }

    /**
     * 设置是否可以滚动
     *
     * @param canAUTO  设置是否支持自动滚动
     */
    public void setCanAUTO(Boolean canAUTO) {
        stopAutoPlay();
        this.canAUTO = canAUTO;
        if (canAUTO) {
            startAutoPlay();
        }
    }

    /**
     * 设置滚动间隔  毫秒  默认2000ms
     *
     * @param AUTOSpace  切换时间间隔 默认2000ms
     */
    public void setAUTOSpace(int AUTOSpace) {
        this.AUTOSpace = AUTOSpace;
    }

    /**
     * 设置标记的位置
     * @param marker 设置标记显示的位置
     * @see #RIGHT_MARKER
     * @see #CENTER_MARKER default
     */
    public void setMarkerLocal(int marker) {
        switch (marker) {
            case RIGHT_MARKER:
                MARKER = RIGHT_MARKER;
                paramsPoints.addRule(RelativeLayout.ALIGN_PARENT_END);
                paramsPoints.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
                linPoints.setLayoutParams(paramsPoints);
                break;
            case CENTER_MARKER:
                MARKER = CENTER_MARKER;
                paramsPoints.addRule(RelativeLayout.CENTER_HORIZONTAL);
                paramsPoints.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
                linPoints.setLayoutParams(paramsPoints);
                break;
        }

    }

    /**
     * 将ViewPager添加到视图中
     */
    private void initViewPager() {
        mViewPager = new ViewPager(mContext);
        mViewPager.addOnPageChangeListener(this);
        ViewPager.LayoutParams params = new ViewPager.LayoutParams();
        params.height = ViewPager.LayoutParams.MATCH_PARENT;
        params.width = ViewPager.LayoutParams.MATCH_PARENT;
        mViewPager.setLayoutParams(params);
        addView(mViewPager);
        linPoints = new LinearLayout(mContext);
        linPoints.setOrientation(LinearLayout.HORIZONTAL);
        paramsPoints = new LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        paramsPoints.setMargins(0, 0, 10, 10);
        handler = new AutoHandle();

    }

    /**
     * 设置需要展示的图片
     * @param views 设置需要显示的图片
     * @param isNetImage 是否为网络图片  (网络图片需要特殊处理)
     *
     */
    public void setViews(@NonNull ArrayList<ImageView> views, boolean isNetImage) {
        this.isNetImage = isNetImage;
        originSize = views.size();

        removeView(linPoints);
        mPagers.clear();
        linPoints.removeAllViews();
        mPagers = views;
        if (views.size() == 0) {
            canAUTO = false;
            handler.removeMessages(messageWhat);
            return;
        }
        mPoints = new View[views.size()];
        for (int i = 0; i < views.size(); i++) {
            views.get(i).setTag(i);
            views.get(i).setOnClickListener(this);
        }
        setMarks();
        addViews(views);
        if (isNetImage) {

        }
        mMyPagerAdapter = new MyPagerAdapter();
        mViewPager.setAdapter(mMyPagerAdapter);
        mViewPager.setCurrentItem(0);
        setMarkerPosition(0);

        if (canAUTO) {
            mViewPager.setCurrentItem(views.size() * 60, false);
            startAutoPlay();
        }
    }


    /**
     * mPagers数量必须>=4
     *
     * @param views 展示的图片
     */
    private void addViews(ArrayList<ImageView> views) {
        switch (views.size()) {
            case 1: //1张补加3张一样的
                needToCopy = 3;
                mPagers.add(cloneImageView(views.get(0)));
                mPagers.add(cloneImageView(views.get(0)));
                mPagers.add(cloneImageView(views.get(0)));
                break;
            case 2://1张补加2张一样的
                needToCopy = 2;
                mPagers.add(cloneImageView(views.get(0)));
                mPagers.add(cloneImageView(views.get(1)));
                break;
            case 3://3张补加一轮
                needToCopy = 3;
                mPagers.add(cloneImageView(mPagers.get(0)));
                mPagers.add(cloneImageView(mPagers.get(1)));
                mPagers.add(cloneImageView(mPagers.get(2)));
                break;
        }
    }


    /**
     * 设置圆点
     */
    private void setMarks() {
        switch (MARKER) {
            case RIGHT_MARKER:
                paramsPoints.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
                paramsPoints.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
                break;
            case CENTER_MARKER:
                paramsPoints.addRule(RelativeLayout.CENTER_HORIZONTAL);
                paramsPoints.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
                break;
        }
        linPoints.setLayoutParams(paramsPoints);
        int local = mViewPager.getCurrentItem();
        for (int i = 0; i < mPoints.length; i++) {
            TextView tvPoints = new TextView(mContext);
            tvPoints.setPadding(2, 2, 2, 2);
            tvPoints.setText("●");
            tvPoints.setTextColor(Color.WHITE);
            tvPoints.setTextSize(8);
            linPoints.addView(tvPoints);
            if (local == i) {
                tvPoints.setTextColor(mContext.getResources().getColor(R.color.colorPrimary));
            }
        }
        addView(linPoints);


    }

    /**
     * 复制一个图片   包括id   图片
     *
     * @param view
     * @return newImageView
     */
    private ImageView cloneImageView(ImageView view) {
        ImageView view1 = new ImageView(mContext);
        view1.setScaleType(view.getScaleType());
        view1.setImageDrawable(view.getDrawable());
        view1.setTag(view.getTag());
        view1.setOnClickListener(this);
        return view1;
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

    }

    @Override
    public void onPageSelected(int position) {
        setMarkerPosition(-1);
    }

    @Override
    public void onPageScrollStateChanged(int state) {

    }


    @Override
    public void onClick(View v) {
        int realPosition = (int) v.getTag();
//        Log.d(TAG, realPosition + "-->onClick");
        if (mOnBannerClick != null)
            mOnBannerClick.bannerClick(realPosition);
    }

    private class MyPagerAdapter extends PagerAdapter {

        @Override
        public int getCount() {
            return Integer.MAX_VALUE;
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            if (isNetImage && originSize < 4 && needToCopy > 0) {
                checkImage();
            }
            ImageView view = mPagers.get(position % mPagers.size());
            container.addView(view);
            return view;
        }


        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            container.removeView(mPagers.get(position % mPagers.size()));

        }
    }

    private void checkImage() {
        switch (originSize) {
            case 1:
                if (mPagers.get(0).getDrawable() != mPagers.get(1).getDrawable()) {
                    mPagers.get(1).setImageDrawable(mPagers.get(0).getDrawable());
                    needToCopy--;
                }
                if (mPagers.get(0).getDrawable() != mPagers.get(2).getDrawable()) {
                    mPagers.get(2).setImageDrawable(mPagers.get(0).getDrawable());
                    needToCopy--;
                }
                if (mPagers.get(0).getDrawable() != mPagers.get(3).getDrawable()) {
                    mPagers.get(3).setImageDrawable(mPagers.get(0).getDrawable());
                    needToCopy--;
                }
                break;
            case 2:
                if (mPagers.get(0).getDrawable() != mPagers.get(2).getDrawable()) {
                    mPagers.get(2).setImageDrawable(mPagers.get(0).getDrawable());
                    needToCopy--;
                }
                if (mPagers.get(1).getDrawable() != mPagers.get(3).getDrawable()) {
                    mPagers.get(3).setImageDrawable(mPagers.get(1).getDrawable());
                    needToCopy--;
                }
                break;
            case 3://3张补加一轮
                if (mPagers.get(0).getDrawable() != mPagers.get(3).getDrawable()) {
                    mPagers.get(3).setImageDrawable(mPagers.get(0).getDrawable());
                    needToCopy--;
                }
                if (mPagers.get(1).getDrawable() != mPagers.get(4).getDrawable()) {
                    mPagers.get(4).setImageDrawable(mPagers.get(1).getDrawable());
                    needToCopy--;
                }
                if (mPagers.get(2).getDrawable() != mPagers.get(5).getDrawable()) {
                    mPagers.get(5).setImageDrawable(mPagers.get(2).getDrawable());
                    needToCopy--;
                }
                break;
        }
    }


    private class AutoHandle extends Handler {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (canAUTO) {
//                Log.d(TAG, "moving" + mViewPager.getCurrentItem());
                mViewPager.setCurrentItem(mViewPager.getCurrentItem() + 1);
                handler.sendEmptyMessageDelayed(messageWhat, AUTOSpace);
            }
        }
    }

    public void startAutoPlay() {
        if (canAUTO && !isAuto) {
            isAuto = true;
            handler.sendEmptyMessageDelayed(messageWhat, AUTOSpace);
        }
    }

    public void stopAutoPlay() {
        if (canAUTO && isAuto) {
            isAuto = false;
            handler.removeMessages(messageWhat);
        }
    }

    /**
     * 设置圆点滑动过程中位置的变化
     */
    private void setMarkerPosition(int pos) {
        int position;
        if (pos == -1) {
            position = mViewPager.getCurrentItem();
        } else {
            position = pos;
        }
        for (int i = 0; i < linPoints.getChildCount(); i++) {
            TextView tv = (TextView) linPoints.getChildAt(i);
            if (position % mPoints.length == i) {
                tv.setTextColor(mContext.getResources().getColor(R.color.colorPrimary));
            } else {
                tv.setTextColor(Color.GRAY);
            }
        }
    }

    /**
     * 在此处拦截事件的分发
     *
     * @param ev  事件
     * @return 是否拦截
     */
    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                stopAutoPlay();
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                startAutoPlay();
                break;

        }
        return super.dispatchTouchEvent(ev);
    }


    OnBannerClick mOnBannerClick;

    /**
     * 设置点击事件
     * @param mOnBannerClick  点击监听回调接口
     */
    public void setOnBannerClick(OnBannerClick mOnBannerClick) {
        this.mOnBannerClick = mOnBannerClick;
    }

    /**
     * 点击事件回调接口
     */
    public interface OnBannerClick {
        void bannerClick(int realPosition);
    }

    @Override
    protected void onWindowVisibilityChanged(int visibility) {
        super.onWindowVisibilityChanged(visibility);
        switch (visibility) {
            case VISIBLE:
                startAutoPlay();
                break;
            case GONE:
                stopAutoPlay();
                break;
            case INVISIBLE:
                break;
        }
    }
}
