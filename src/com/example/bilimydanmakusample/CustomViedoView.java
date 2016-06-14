package com.example.bilimydanmakusample;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.VideoView;

public class CustomViedoView extends VideoView {
	private PlayPauseListener mListener;

	public CustomViedoView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	public CustomViedoView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public CustomViedoView(Context context) {
		super(context);
	}
	
	public interface PlayPauseListener{
		void onPlay();
		void onPause();
	}
	
	public void setPlayPauseListener(PlayPauseListener listener){
		mListener=listener;
	}
	
	@Override
	public void pause() {
		if (mListener!=null) {
			mListener.onPause();
		}
	}
	
	@Override
	public void resume() {
		if (mListener!=null) {
			mListener.onPlay();
		}
	}
	
//	@Override
//	public void start() {
//		if (mListener!=null) {
//			mListener.onPlay();
//		}
//	}
}
