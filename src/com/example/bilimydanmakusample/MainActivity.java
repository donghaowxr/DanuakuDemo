package com.example.bilimydanmakusample;

import io.vov.vitamio.widget.MediaController;
import io.vov.vitamio.widget.VideoView;
import io.vov.vitamio.widget.VideoView.OnPlayStateListener;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import master.flame.danmaku.controller.IDanmakuView;
import master.flame.danmaku.danmaku.loader.ILoader;
import master.flame.danmaku.danmaku.loader.IllegalDataException;
import master.flame.danmaku.danmaku.loader.android.DanmakuLoaderFactory;
import master.flame.danmaku.danmaku.model.BaseDanmaku;
import master.flame.danmaku.danmaku.model.DanmakuTimer;
import master.flame.danmaku.danmaku.model.IDisplayer;
import master.flame.danmaku.danmaku.model.android.BaseCacheStuffer;
import master.flame.danmaku.danmaku.model.android.DanmakuContext;
import master.flame.danmaku.danmaku.model.android.Danmakus;
import master.flame.danmaku.danmaku.model.android.SpannedCacheStuffer;
import master.flame.danmaku.danmaku.parser.BaseDanmakuParser;
import master.flame.danmaku.danmaku.parser.IDataSource;
import master.flame.danmaku.danmaku.parser.android.BiliDanmukuParser;
import master.flame.danmaku.danmaku.util.IOUtils;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ImageSpan;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;


@SuppressLint("UseSparseArrays") public class MainActivity extends Activity implements OnClickListener {

    private VideoView vvVideo;
	private IDanmakuView mDanmakuView;
	private Button btnSendText;
	private EditText etSendText;
	private BaseDanmakuParser mParser;
	private DanmakuContext mContext;
	private Button btnPicSend;
	private EditText etPicText;
	
	private BaseCacheStuffer.Proxy mCacheStufferAdapter=new BaseCacheStuffer.Proxy() {
		private Drawable mDrawable;
		@Override
		public void releaseResource(BaseDanmaku danmaku) {
		}
		
		@Override
		public void prepareDrawing(final BaseDanmaku danmaku, boolean fromWorkerThread) {
			if (danmaku.text instanceof Spanned) {
				ExecutorService cachedThreadPool=Executors.newCachedThreadPool();
				cachedThreadPool.execute(new Runnable() {
					@Override
					public void run() {
						String url = "http://192.168.0.93:8080/tomcat.png";
	                    InputStream inputStream = null;
	                    Drawable drawable = mDrawable;
	                    if (drawable == null) {
	                        try {
	                            URLConnection urlConnection = new URL(url).openConnection();
	                            inputStream = urlConnection.getInputStream();
	                            drawable = BitmapDrawable.createFromStream(inputStream, "bitmap");
	                            mDrawable = drawable;
	                        } catch (MalformedURLException e) {
	                            e.printStackTrace();
	                        } catch (IOException e) {
	                            e.printStackTrace();
	                        } finally {
	                            IOUtils.closeQuietly(inputStream);
	                        }
	                    }
	                    if (drawable != null) {
	                        drawable.setBounds(0, 0, 100, 100);
	                        SpannableStringBuilder spannable = createSpannable(drawable,etPicText.getText().toString());
	                        danmaku.text = spannable;
	                        if (mDanmakuView != null) {
	                            mDanmakuView.invalidateDanmaku(danmaku, false);
	                        }
	                        return;
	                    }
					}
				});
			}
		}
	};

	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initView();
        initData();
        initListener();
    }
	
	/**
	 * 初始化布局
	 */
	private void initView() {
		setContentView(R.layout.activity_main);
		vvVideo = (VideoView) findViewById(R.id.video);
        mDanmakuView = (IDanmakuView) findViewById(R.id.sv_danmaku);
        btnSendText = (Button) findViewById(R.id.btnSend);
        etSendText = (EditText) findViewById(R.id.et_sendText);
        btnPicSend = (Button) findViewById(R.id.btnPicSend);
        etPicText = (EditText) findViewById(R.id.et_sendPicText);
	}
	
	/**
	 * 初始化监听事件
	 */
	private void initListener() {
		btnSendText.setOnClickListener(this);
		btnPicSend.setOnClickListener(this);
		vvVideo.setOnPlayStateListener(new OnPlayStateListener() {
			
			@Override
			public void playStateListener(int state) {
				if (state==vvVideo.STATE_PLAYING) {
					mDanmakuView.resume();
				}
				if (state==vvVideo.STATE_PAUSED) {
					mDanmakuView.pause();
				}
			}
		});
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (mDanmakuView!=null) {
			mDanmakuView.release();
			mDanmakuView=null;
		}
	}
	
	/**
	 * 初始化数据
	 */
	private void initData() {
		initDanmuak();
		initVideo();
	}
	
	@SuppressLint("UseSparseArrays") private void initDanmuak() {
		//设置滚动弹幕最大显示5行
		HashMap<Integer, Integer>maxLinesPair=new HashMap<Integer, Integer>();
		maxLinesPair.put(BaseDanmaku.TYPE_SCROLL_RL, 5);
		
		HashMap<Integer, Boolean>overlappingEnablePair=new HashMap<Integer, Boolean>();
		overlappingEnablePair.put(BaseDanmaku.TYPE_SCROLL_RL, true);
		overlappingEnablePair.put(BaseDanmaku.TYPE_FIX_TOP, true);
		
		//获取弹幕上下文
		mContext = DanmakuContext.create();
		
		//设置参数
		mContext.setDanmakuStyle(IDisplayer.DANMAKU_STYLE_SHADOW, 0);//设置描边样式
		mContext.setDuplicateMergingEnabled(true);//是否启用合并重复弹幕
		mContext.setScrollSpeedFactor(1.2f);//弹幕滚动速度
		mContext.setScaleTextSize(1.2f);
//		mContext.setCacheStuffer(new SpannedCacheStuffer(), mCacheStufferAdapter); //设置图文混排模式
		mContext.setCacheStuffer(new SpannedCacheStuffer(), mCacheStufferAdapter);
		mContext.setMaximumLines(maxLinesPair);
		mContext.preventOverlapping(overlappingEnablePair);//设置防止弹幕重叠，null为允许重叠
//		mParser = createParser(this.getResources().openRawResource(R.raw.comments));
		mParser=createParser(null);
		
		mDanmakuView.setCallback(new master.flame.danmaku.controller.DrawHandler.Callback(){
			@Override
			public void prepared() {
				mDanmakuView.start();
			}
			@Override
			public void updateTimer(DanmakuTimer timer) {
			}
			@Override
			public void danmakuShown(BaseDanmaku danmaku) {
			}
			@Override
			public void drawingFinished() {
			}
		});
		
		mDanmakuView.prepare(mParser, mContext);
//		mDanmakuView.showFPS(true);
        mDanmakuView.enableDanmakuDrawingCache(true);
        mDanmakuView.show();
	}
	/**
	 * 初始化播放器
	 */
	private void initVideo() {
		if (!io.vov.vitamio.LibsChecker.checkVitamioLibs(this)) {
			return;
		}
		Uri uri=Uri.parse("http://192.168.0.93:8080/1.flv");
        vvVideo.setVideoURI(uri);
        MediaController mediaController = new MediaController(this);
        vvVideo.setMediaController(mediaController);
        vvVideo.requestFocus();
        mediaController.setMediaPlayer(vvVideo);
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.btnSend:
			addDanmaku(true,etSendText.getText().toString());
			break;
		case R.id.btnPicSend:
			addDanmakuShowTextAndImage(true, etPicText.getText().toString());
			break;
		}
	}
	
	/**
	 * 创建解析器对象，解析输入流
	 * @param stream
	 * @return
	 */
	private BaseDanmakuParser createParser(InputStream stream) {

        if (stream == null) {
            return new BaseDanmakuParser() {

                @Override
                protected Danmakus parse() {
                    return new Danmakus();
                }
            };
        }
        ILoader loader = DanmakuLoaderFactory.create(DanmakuLoaderFactory.TAG_BILI);//xml解析
//        ILoader loader=DanmakuLoaderFactory.create(DanmakuLoaderFactory.TAG_ACFUN)//json格式解析

        try {
            loader.load(stream);
        } catch (IllegalDataException e) {
            e.printStackTrace();
        }
        BaseDanmakuParser parser = new BiliDanmukuParser();
        IDataSource<?> dataSource = loader.getDataSource();
        parser.load(dataSource);
        return parser;
    }
	
	/**
	 * 添加弹幕文本
	 * @param islive
	 * @param text
	 */
	private void addDanmaku(boolean islive,String text) {
        BaseDanmaku danmaku = mContext.mDanmakuFactory.createDanmaku(BaseDanmaku.TYPE_SCROLL_RL);
        if (danmaku == null || mDanmakuView == null) {
            return;
        }
        // for(int i=0;i<100;i++){
        // }
        danmaku.text = text + System.nanoTime();
        danmaku.padding = 5;
        danmaku.priority = 0;  // 可能会被各种过滤器过滤并隐藏显示
        danmaku.isLive = islive;
        danmaku.time = mDanmakuView.getCurrentTime() + 1200;
        danmaku.textSize = 30f * (mParser.getDisplayer().getDensity() - 0.6f);
//        danmaku.textSize=25f;
        danmaku.textColor = Color.YELLOW;
        danmaku.textShadowColor = Color.WHITE;
        // danmaku.underlineColor = Color.GREEN;
//        danmaku.borderColor = Color.GREEN;//边框颜色，0表示无边框
        danmaku.borderColor=0;
        mDanmakuView.addDanmaku(danmaku);
    }
	
	/**
	 * 添加图文混排弹幕
	 * @param islive
	 * @param text
	 */
	private void addDanmakuShowTextAndImage(boolean islive,String text){
		BaseDanmaku danmaku=mContext.mDanmakuFactory.createDanmaku(BaseDanmaku.TYPE_SCROLL_RL);
		Drawable drawable = getResources().getDrawable(R.drawable.ic_launcher);
	    drawable.setBounds(0, 0, 100, 100);
	    SpannableStringBuilder spannable = createSpannable(drawable,text);
	    danmaku.text = spannable;
	    danmaku.padding = 5;
	    danmaku.priority = 1;  // 一定会显示, 一般用于本机发送的弹幕
	    danmaku.isLive = islive;
	    danmaku.time = mDanmakuView.getCurrentTime() + 1200;
	    danmaku.textSize = 25f * (mParser.getDisplayer().getDensity() - 0.6f);
	    danmaku.textColor = Color.YELLOW;
	    danmaku.textShadowColor = 0; // 重要：如果有图文混排，最好不要设置描边(设textShadowColor=0)，否则会进行两次复杂的绘制导致运行效率降低
	    danmaku.underlineColor = 0;
	    mDanmakuView.addDanmaku(danmaku);
	}

	/**
	 * 创建图文混排模式
	 */
	private SpannableStringBuilder createSpannable(Drawable drawable,
			String text) {
        SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder(text);
        ImageSpan span = new ImageSpan(drawable);//ImageSpan.ALIGN_BOTTOM);
        spannableStringBuilder.setSpan(span, 0, text.length(), Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
        spannableStringBuilder.append(text);
        spannableStringBuilder.setSpan(0, 0, spannableStringBuilder.length(), Spannable.SPAN_INCLUSIVE_INCLUSIVE);
        return spannableStringBuilder;
	}
}
