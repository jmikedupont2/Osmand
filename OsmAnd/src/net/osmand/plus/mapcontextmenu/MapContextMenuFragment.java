package net.osmand.plus.mapcontextmenu;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import net.osmand.PlatformUtil;
import net.osmand.plus.IconsCache;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.mapcontextmenu.sections.MenuController;

import org.apache.commons.logging.Log;

import static android.util.TypedValue.COMPLEX_UNIT_DIP;


public class MapContextMenuFragment extends Fragment {

	public static final String TAG = "MapContextMenuFragment";
	private static final Log LOG = PlatformUtil.getLog(MapContextMenuFragment.class);

	private View view;
	private View mainView;
	private View bottomView;
	private View shadowView;
	private View bottomBorder;

	MenuController menuController;

	private int menuTopHeight;
	private int menuButtonsHeight;
	private int menuBottomViewHeight;
	private int menuFullHeight;

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);

		/*
		if(!portrait) {
			mapActivity.getMapView().setMapPositionX(1);
			mapActivity.getMapView().refreshMap();
		}

		if(!AndroidUiHelper.isXLargeDevice(mapActivity)) {
			AndroidUiHelper.updateVisibility(mapActivity.findViewById(R.id.map_right_widgets_panel), false);
			AndroidUiHelper.updateVisibility(mapActivity.findViewById(R.id.map_left_widgets_panel), false);
		}
		if(!portrait) {
			AndroidUiHelper.updateVisibility(mapActivity.findViewById(R.id.map_route_land_left_margin), true);
		}
		*/
	}

	@Override
	public void onDetach() {
		super.onDetach();

		/*
		mapActivity.getMapView().setMapPositionX(0);
		mapActivity.getMapView().refreshMap();

		AndroidUiHelper.updateVisibility(mapActivity.findViewById(R.id.map_route_land_left_margin), false);
		AndroidUiHelper.updateVisibility(mapActivity.findViewById(R.id.map_right_widgets_panel), true);
		AndroidUiHelper.updateVisibility(mapActivity.findViewById(R.id.map_left_widgets_panel), true);
		*/
	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
							 Bundle savedInstanceState) {

		view = inflater.inflate(R.layout.map_context_menu_fragment, container, false);

		ViewTreeObserver vto = view.getViewTreeObserver();
		vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {

			@Override
			public void onGlobalLayout() {

				menuTopHeight = view.findViewById(R.id.context_menu_top_view).getHeight();
				menuButtonsHeight = view.findViewById(R.id.context_menu_buttons).getHeight();
				menuBottomViewHeight = view.findViewById(R.id.context_menu_bottom_view).getHeight();
				menuFullHeight = view.findViewById(R.id.context_menu_main).getHeight();

				ViewTreeObserver obs = view.getViewTreeObserver();

				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
					obs.removeOnGlobalLayoutListener(this);
				} else {
					obs.removeGlobalOnLayoutListener(this);
				}

				doLayoutMenu();
			}

		});

		bottomBorder = view.findViewById(R.id.context_menu_bottom_border);
		bottomBorder.setOnTouchListener(new View.OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				return true;
			}
		});

		shadowView = view.findViewById(R.id.context_menu_shadow_view);
		shadowView.setOnTouchListener(new View.OnTouchListener() {
			public boolean onTouch(View view, MotionEvent event) {
				dismissMenu();
				return true;
			}
		});

		View topView = view.findViewById(R.id.context_menu_top_view);
		mainView = view.findViewById(R.id.context_menu_main);

		topView.setOnTouchListener(new View.OnTouchListener() {

			private float dy;
			private float dyMain;
			private int destinationState;
			private VelocityTracker velocity;
			private boolean slidingUp;
			private boolean slidingDown;

			private float velocityY;

			@Override
			public boolean onTouch(View v, MotionEvent event) {

				switch (event.getAction()) {
					case MotionEvent.ACTION_DOWN:
						dy = event.getY();
						dyMain = mainView.getY();
						velocity = VelocityTracker.obtain();
						velocityY = 0;
						velocity.addMovement(event);
						break;

					case MotionEvent.ACTION_MOVE:
						float y = event.getY();
						float newY = mainView.getY() + (y - dy);
						mainView.setY(newY);

						ViewGroup.LayoutParams lp = bottomBorder.getLayoutParams();
						lp.height = (int)(view.getHeight() - newY - menuFullHeight) + 10;
						bottomBorder.setLayoutParams(lp);
						bottomBorder.setY(newY + menuFullHeight);
						bottomBorder.requestLayout();

						velocity.addMovement(event);
						velocity.computeCurrentVelocity(1000);
						float vel = Math.abs(velocity.getYVelocity());
						if (vel > velocityY)
							velocityY = vel;

						break;

					case MotionEvent.ACTION_UP:
					case MotionEvent.ACTION_CANCEL:

						slidingUp = Math.abs(velocityY) > 500 && (mainView.getY() - dyMain) < -50;
						slidingDown = Math.abs(velocityY) > 500 && (mainView.getY() - dyMain) > 50;

						velocity.recycle();

						if (menuController != null) {
							if (slidingUp) {
								menuController.slideUp();
							} else if (slidingDown) {
								menuController.slideDown();
							}
							destinationState = menuController.getCurrentMenuState();
						} else {
							destinationState = MenuController.MenuState.HEADER_ONLY;
						}

						float posY = 0;
						switch (destinationState) {
							case MenuController.MenuState.HEADER_ONLY:
								posY = view.getHeight() - (menuFullHeight - menuBottomViewHeight);
								break;
							case MenuController.MenuState.HALF_SCREEN:
								posY = view.getHeight() - menuFullHeight;
								break;
							case MenuController.MenuState.FULL_SCREEN:
								posY = 0;
								break;
							default:
								break;
						}

						float minY = Math.min(posY, mainView.getY());
						lp = bottomBorder.getLayoutParams();
						lp.height = (int)(view.getHeight() - minY - menuFullHeight) + 10;
						if (lp.height < 0)
							lp.height = 0;
						bottomBorder.setLayoutParams(lp);
						bottomBorder.requestLayout();

						if (mainView.getY() != posY) {
							mainView.animate().y(posY).setDuration(200).setInterpolator(new DecelerateInterpolator()).start();
							bottomBorder.animate().y(posY + menuFullHeight).setDuration(200).setInterpolator(new DecelerateInterpolator()).start();
						}


						/*
						lp = shadowView.getLayoutParams();
						lp.height = view.getHeight() - (int)posY;
						shadowView.setLayoutParams(lp);
						shadowView.requestLayout();
						*/

						break;

				}
				return true;
			}
		});

		// Left icon
		IconsCache iconsCache = getMyApplication().getIconsCache();
		boolean light = getMyApplication().getSettings().isLightContent();

		int iconId = MapContextMenu.getInstance().getLeftIconId();

		final View iconLayout = view.findViewById(R.id.context_menu_icon_layout);
		final ImageView iconView = (ImageView)view.findViewById(R.id.context_menu_icon_view);
		if (iconId == 0) {
			iconLayout.setVisibility(View.GONE);
		} else {
			iconView.setImageDrawable(iconsCache.getIcon(iconId,
					light ? R.color.osmand_orange : R.color.osmand_orange_dark));
		}

		// Text line 1
		TextView line1 = (TextView) view.findViewById(R.id.context_menu_line1);
		line1.setText(MapContextMenu.getInstance().getAddressStr());

		// Text line 2
		TextView line2 = (TextView) view.findViewById(R.id.context_menu_line2);
		line2.setText(MapContextMenu.getInstance().getLocationStr());

		// Close button
		final ImageView closeButtonView = (ImageView)view.findViewById(R.id.context_menu_close_btn_view);
		closeButtonView.setImageDrawable(iconsCache.getIcon(R.drawable.ic_action_remove_dark,
				light ? R.color.icon_color_light : R.color.dash_search_icon_dark));
		closeButtonView.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				((MapActivity) getActivity()).getMapLayers().getContextMenuLayer().hideMapContextMenuMarker();
				dismissMenu();
			}
		});

		// Action buttons
		final ImageButton buttonNavigate = (ImageButton) view.findViewById(R.id.context_menu_route_button);
		buttonNavigate.setImageDrawable(iconsCache.getIcon(R.drawable.map_directions,
				light ? R.color.icon_color : R.color.dash_search_icon_dark));
		buttonNavigate.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				MapContextMenu.getInstance().buttonNavigatePressed();
			}
		});

		final ImageButton buttonFavorite = (ImageButton) view.findViewById(R.id.context_menu_fav_button);
		buttonFavorite.setImageDrawable(iconsCache.getIcon(R.drawable.ic_action_fav_dark,
				light ? R.color.icon_color : R.color.dash_search_icon_dark));
		buttonFavorite.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				MapContextMenu.getInstance().buttonFavoritePressed();
			}
		});

		final ImageButton buttonShare = (ImageButton) view.findViewById(R.id.context_menu_share_button);
		buttonShare.setImageDrawable(iconsCache.getIcon(R.drawable.abc_ic_menu_share_mtrl_alpha,
				light ? R.color.icon_color : R.color.dash_search_icon_dark));
		buttonShare.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				MapContextMenu.getInstance().buttonSharePressed();
			}
		});

		final ImageButton buttonMore = (ImageButton) view.findViewById(R.id.context_menu_more_button);
		buttonMore.setImageDrawable(iconsCache.getIcon(R.drawable.ic_overflow_menu_white,
				light ? R.color.icon_color : R.color.dash_search_icon_dark));
		buttonMore.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				MapContextMenu.getInstance().buttonMorePressed();
			}
		});

		// Menu controller
		menuController = MapContextMenu.getInstance().getMenuController();
		bottomView = view.findViewById(R.id.context_menu_bottom_view);
		if (menuController != null) {
			bottomView.setOnTouchListener(new View.OnTouchListener() {
				@Override
				public boolean onTouch(View v, MotionEvent event) {
					return true;
				}
			});
			menuController.build(bottomView);
		}

		return view;
	}

	private void doLayoutMenu() {
		int shadowViewHeight = 0;
		int bottomBorderHeight = 0;

		int menuState;
		if (menuController != null)
			menuState = menuController.getCurrentMenuState();
		else
			menuState = MenuController.MenuState.HEADER_ONLY;

		switch (menuState) {
			case MenuController.MenuState.HEADER_ONLY:
				shadowViewHeight = view.getHeight() - (menuFullHeight - menuBottomViewHeight);
				bottomBorderHeight = 0;
				break;
			case MenuController.MenuState.HALF_SCREEN:
				int maxHeight = (int)(menuController.getHalfScreenMaxHeightKoef() * view.getHeight());
				if (maxHeight > menuFullHeight) {
					shadowViewHeight = view.getHeight() - menuFullHeight;
					bottomBorderHeight = 0;
				} else {
					shadowViewHeight = view.getHeight() - maxHeight;
					bottomBorderHeight = 0;
					mainView.setY(shadowViewHeight);
				}
				break;
			case MenuController.MenuState.FULL_SCREEN:
				shadowViewHeight = 0;
				bottomBorderHeight = view.getHeight() - menuFullHeight;
				break;
			default:
				break;
		}

		ViewGroup.LayoutParams lp = bottomBorder.getLayoutParams();
		lp.height = bottomBorderHeight + 10;
		bottomBorder.setLayoutParams(lp);
		bottomBorder.setY(view.getHeight() - bottomBorderHeight);

		lp = shadowView.getLayoutParams();
		lp.height = shadowViewHeight;
		shadowView.setLayoutParams(lp);

		lp = mainView.getLayoutParams();
		lp.height = menuFullHeight;
		mainView.setLayoutParams(lp);

	}

	public void dismissMenu() {
		getActivity().getSupportFragmentManager().popBackStack();
	}

	public OsmandApplication getMyApplication() {
		if (getActivity() == null) {
			return null;
		}
		return (OsmandApplication) getActivity().getApplication();
	}

	public static void showInstance(final MapActivity mapActivity) {
		MapContextMenuFragment fragment = new MapContextMenuFragment();
		mapActivity.getSupportFragmentManager().beginTransaction()
				.setCustomAnimations(R.anim.slide_in_bottom, R.anim.slide_out_bottom, R.anim.slide_in_bottom, R.anim.slide_out_bottom)
				.add(R.id.fragmentContainer, fragment, "MapContextMenuFragment")
				.addToBackStack(null).commit();
	}

	// Utils
	public int getScreenHeight() {
		DisplayMetrics dm = new DisplayMetrics();
		getActivity().getWindowManager().getDefaultDisplay().getMetrics(dm);
		return dm.heightPixels;
	}

	public int dpToPx(float dp) {
		Resources r = getActivity().getResources();
		return (int) TypedValue.applyDimension(
				COMPLEX_UNIT_DIP,
				dp,
				r.getDisplayMetrics()
		);
	}
}

