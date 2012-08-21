package jp.kuseful.viewflippersample;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.TranslateAnimation;
import android.widget.ViewFlipper;

public class ViewFlipperSampleActivity extends Activity
	implements OnTouchListener {
	
	// メニューID
	private static final int MENU_VIEWFLIP_ANIM = Menu.FIRST;
	private static final int MENU_MOVE_NEXT = Menu.FIRST + 1;
	private static final int MENU_MOVE_CURRENT = Menu.FIRST + 2;
	
	// ページ切り替えの動作モード
	private int procMode = MENU_VIEWFLIP_ANIM;
	
	// 移動量しきい値
	private final int SWITCH_THRESHOLD = 10;
	
	// ページ切り替えモード
	private final int FLIPMODE_NOMOVE = 0;
	private final int FLIPMODE_NEXT = 1;
	private final int FLIPMODE_PREV = -1;
	private int flipMode = FLIPMODE_NOMOVE;
	
	private ViewFlipper viewflipper = null;
	private View currentView = null;
	private View nextView = null;
	private View prevView = null;
	
	// ページIDと順序を管理
	private int viewOrder[] = null;
	private int currentIdx = -1;
	private int prevIdx = -1;
	private int nextIdx = -1;
	
	private int movePageThreshold = 0;
	private float startX;
	private int travelDistanceX;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        viewflipper = (ViewFlipper)findViewById(R.id.viewflipper);
        viewflipper.setOnTouchListener(this);
        
        /**
         * レイアウトをViewFlipperに格納
         * また、ViewのIDを配列に格納
         */
        LayoutInflater inflater = getLayoutInflater();
        int layouts[] = new int[] {
        	R.layout.layout_page1,
        	R.layout.layout_page2,
        	R.layout.layout_page3
        };
        
        viewOrder = new int[layouts.length];
        for (int i = 0; i < layouts.length; i++) {
        	View v = inflater.inflate(layouts[i], null);
        	
        	// ViewFlipperにViewを格納
        	viewflipper.addView(v);
        	
        	// ViewのIDを格納
        	viewOrder[i] = v.getId();
        }
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
    	// メニューでページめくりの実装を切り替える
    	super.onCreateOptionsMenu(menu);
    	
    	menu.add(0, MENU_VIEWFLIP_ANIM, 0, R.string.viewflipper_anim);
    	menu.add(0, MENU_MOVE_CURRENT, 0, R.string.current_page_move);
    	menu.add(0, MENU_MOVE_NEXT, 0, R.string.next_prev_page_move);
    	
    	return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	procMode = item.getItemId();
    	
    	// ViewFlipperのアニメーションを解除
    	viewflipper.setInAnimation(null);
    	viewflipper.setOutAnimation(null);
    	
    	return true;
    }

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		boolean ret = false;
		
		switch (procMode) {
			case MENU_VIEWFLIP_ANIM:
				ret = pageFlipWithSampleAnimation(v, event);
				break;
			case MENU_MOVE_CURRENT:
				ret = pageFlipWithFingerMoveCurrent(v, event);
				break;
			case MENU_MOVE_NEXT:
				ret = pageFlipWithFingerMoveNext(v, event);
				break;
		}
		return ret;
	}
	
	/**
	 * ViewFlipperとアニメーションを利用してページをめくる
	 * @param v
	 * @param event
	 * @return
	 */
	public boolean pageFlipWithSampleAnimation(View v, MotionEvent event) {
		switch (event.getAction()) {
			case MotionEvent.ACTION_DOWN:
				startX = event.getX();
				break;
			case MotionEvent.ACTION_UP:
				float currentX = event.getX();
				
				if (startX > currentX) {
					viewflipper.setInAnimation(AnimationUtils.loadAnimation(this, R.anim.push_left_in));
					viewflipper.setOutAnimation(AnimationUtils.loadAnimation(this, R.anim.push_left_out));
					viewflipper.showNext();
				} else if (startX < currentX) {
					viewflipper.setInAnimation(AnimationUtils.loadAnimation(this, R.anim.push_right_out));
					viewflipper.setOutAnimation(AnimationUtils.loadAnimation(this, R.anim.push_right_in));
					viewflipper.showPrevious();
				}
				break;
			default:
				break;
		}
		return true;
	}
	
	/**
	 * 現在のページを移動してページをめくる
	 * @param v
	 * @param event
	 * @return
	 */
	public boolean pageFlipWithFingerMoveCurrent(View v, MotionEvent event) {
		float currentX = event.getX();
		
		switch (event.getAction()) {
			case MotionEvent.ACTION_DOWN:
				startX = currentX;
				
				// 前後のページを設定する
				settingPageFlipView();
				break;
			case MotionEvent.ACTION_MOVE:
				travelDistanceX = (int)(currentX - startX);
				
				if (flipMode == FLIPMODE_NOMOVE) {
					// 移動量によって切り替えモードを設定する
					flipMode = getFlipMode();
				} else if (flipMode == FLIPMODE_PREV) {
					currentView.layout(travelDistanceX,
										currentView.getTop(),
										travelDistanceX + currentView.getWidth(),
										currentView.getBottom());
							
					viewflipper.bringChildToFront(currentView);
					prevView.setVisibility(View.VISIBLE);
				} else if (flipMode == FLIPMODE_NEXT) {
					currentView.layout(travelDistanceX,
										currentView.getTop(),
										currentView.getWidth() - (travelDistanceX * -1),
										currentView.getBottom());
					
					viewflipper.bringChildToFront(currentView);
					nextView.setVisibility(View.VISIBLE);
				}
				break;
			case MotionEvent.ACTION_UP:
				int activeIdx = -1;
				
				Animation anim_in = null;
				Animation anim_out = null;

				if (travelDistanceX > movePageThreshold) {
					activeIdx = prevIdx;
					
					anim_out = new TranslateAnimation(0, travelDistanceX + currentView.getWidth(), 0, 0);
					anim_out.setDuration(500);
				} else if (travelDistanceX < (movePageThreshold * -1)) {
					activeIdx = nextIdx;

					anim_out = new TranslateAnimation(travelDistanceX, (currentView.getWidth() * -1), 0, 0);
					anim_out.setDuration(500);
				} else {
					activeIdx = currentIdx;
				}

				// ページをめくる
				displayChangeView(activeIdx, anim_in, anim_out);
				break;
			default:
				break;
		}
		return true;
	}
	
	/**
	 * 前後のページを移動してページをめくる
	 * @param v
	 * @param event
	 * @return
	 */
	public boolean pageFlipWithFingerMoveNext(View v, MotionEvent event) {
		float currentX = event.getX();
		
		switch (event.getAction()) {
			case MotionEvent.ACTION_DOWN:
				startX = currentX;
				
				// 前後のページを設定する
				settingPageFlipView();
				break;
			case MotionEvent.ACTION_MOVE:
				travelDistanceX = (int)(currentX - startX);
				
				if (flipMode == FLIPMODE_NOMOVE) {
					// 移動量によって切り替えモードを設定する
					flipMode = getFlipMode();
				} else if (flipMode == FLIPMODE_PREV) {
					prevView.layout(travelDistanceX - prevView.getWidth(),
									prevView.getTop(),
									travelDistanceX,
									prevView.getBottom());
					
					viewflipper.bringChildToFront(prevView);
					prevView.setVisibility(View.VISIBLE);
				} else if (flipMode == FLIPMODE_NEXT) {
					nextView.layout(travelDistanceX + currentView.getWidth(),
									nextView.getTop(),
									travelDistanceX + currentView.getWidth() + nextView.getWidth(),
									nextView.getBottom());
					
					viewflipper.bringChildToFront(nextView);
					nextView.setVisibility(View.VISIBLE);
				}
				break;
			case MotionEvent.ACTION_UP:
				int activeIdx = -1;
				
				Animation anim_in = null;
				Animation anim_out = null;

				if (travelDistanceX > movePageThreshold) {
					activeIdx = prevIdx;
					
					anim_in = new TranslateAnimation(travelDistanceX - prevView.getWidth(), 0, 0, 0);
					anim_in.setDuration(500);
					anim_out = new TranslateAnimation(0, 0, 0, 0);
					anim_out.setDuration(500);
				} else if (travelDistanceX < (movePageThreshold * -1)) {
					activeIdx = nextIdx;

					anim_in = new TranslateAnimation(travelDistanceX + currentView.getWidth(), 0, 0, 0);
					anim_in.setDuration(500);
					anim_out = new TranslateAnimation(0, 0, 0, 0);
					anim_out.setDuration(500);
				} else {
					activeIdx = currentIdx;
				}

				// ページをめくる
				displayChangeView(activeIdx, anim_in, anim_out);
				break;
			default:
				break;
		}
		return true;
	}
	
	/**
	 * 現在のページから前後のページを設定する
	 */
	private void settingPageFlipView() {
		currentView = viewflipper.getCurrentView();
		movePageThreshold = (currentView.getWidth() / 5);
		
		int viewCount = viewOrder.length;
		for (int i = 0; i < viewCount; i++) {
			if (viewOrder[i] == currentView.getId()) {
				currentIdx = i;
				break;
			}
		}
		
		if (currentIdx >= 0) {
			prevIdx = currentIdx - 1;
			nextIdx = currentIdx + 1;
			prevIdx = (prevIdx < 0) ? viewCount - 1 : prevIdx;
			nextIdx = (nextIdx >= viewCount) ? 0 : nextIdx;
							
			prevView = viewflipper.findViewById(viewOrder[prevIdx]);
			nextView = viewflipper.findViewById(viewOrder[nextIdx]);
		}
	}
	
	/**
	 * 移動量からページの切り替えを判断する
	 * @return
	 */
	private int getFlipMode() {
		if (travelDistanceX > SWITCH_THRESHOLD) {
			return FLIPMODE_PREV;
		} else if (travelDistanceX < (SWITCH_THRESHOLD * -1)) {
			return FLIPMODE_NEXT;
		}

		return FLIPMODE_NOMOVE;
	}
	
	/**
	 * 切り替えに合わせてページをめくる
	 * @param activeIdx
	 * @param anim_in
	 * @param anim_out
	 */
	private void displayChangeView(int activeIdx, Animation anim_in, Animation anim_out) {
		int activeId = viewOrder[activeIdx];
		for (int i = 0; i < viewflipper.getChildCount(); i++) {
			if (viewflipper.getChildAt(i).getId() == activeId) {
				// アニメーションを設定
				viewflipper.setInAnimation(anim_in);
				viewflipper.setOutAnimation(anim_out);
				
				// ページを設定
				viewflipper.setDisplayedChild(i);
				break;
			}
		}
		
		flipMode = FLIPMODE_NOMOVE;
	}
}