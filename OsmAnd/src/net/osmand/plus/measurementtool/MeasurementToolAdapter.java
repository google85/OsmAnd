package net.osmand.plus.measurementtool;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import net.osmand.plus.GPXUtilities.WptPt;
import net.osmand.plus.IconsCache;
import net.osmand.plus.OsmAndFormatter;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.util.MapUtils;

import java.util.List;

class MeasurementToolAdapter extends RecyclerView.Adapter<MeasurementToolAdapter.Holder> {

	private final MapActivity mapActivity;
	private final List<WptPt> points;
	private RemovePointListener removePointListener;
	private ItemClickListener itemClickListener;

	MeasurementToolAdapter(MapActivity mapActivity, List<WptPt> points) {
		this.mapActivity = mapActivity;
		this.points = points;
	}

	void setRemovePointListener(RemovePointListener listener) {
		this.removePointListener = listener;
	}

	public void setItemClickListener(ItemClickListener itemClickListener) {
		this.itemClickListener = itemClickListener;
	}

	@Override
	public Holder onCreateViewHolder(ViewGroup viewGroup, int i) {
		View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.measure_points_list_item, viewGroup, false);
		final boolean nightMode = mapActivity.getMyApplication().getDaynightHelper().isNightModeForMapControls();
		if (!nightMode) {
			view.findViewById(R.id.points_divider).setBackgroundResource(R.drawable.divider);
		}
		view.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				itemClickListener.onItemClick(view);
			}
		});
		return new Holder(view);
	}

	@Override
	public void onBindViewHolder(final Holder holder, int pos) {
		IconsCache iconsCache = mapActivity.getMyApplication().getIconsCache();
		holder.icon.setImageDrawable(iconsCache.getThemedIcon(R.drawable.ic_action_measure_point));
		holder.title.setText(mapActivity.getString(R.string.plugin_distance_point) + " - " + (pos + 1));
		if (pos < 1) {
			holder.descr.setText(mapActivity.getString(R.string.shared_string_control_start));
		} else {
			float dist = 0;
			for (int i = 1; i <= pos; i++) {
				dist += MapUtils.getDistance(points.get(i - 1).lat, points.get(i - 1).lon,
						points.get(i).lat, points.get(i).lon);
			}
			holder.descr.setText(OsmAndFormatter.getFormattedDistance(dist, mapActivity.getMyApplication()));
		}
		holder.deleteBtn.setImageDrawable(iconsCache.getThemedIcon(R.drawable.ic_action_remove_dark));
		holder.deleteBtn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				points.remove(holder.getAdapterPosition());
				removePointListener.onPointRemove();
			}
		});
	}

	@Override
	public int getItemCount() {
		return points.size();
	}

	static class Holder extends RecyclerView.ViewHolder {

		final ImageView icon;
		final TextView title;
		final TextView descr;
		final ImageButton deleteBtn;

		Holder(View view) {
			super(view);
			icon = (ImageView) view.findViewById(R.id.measure_point_icon);
			title = (TextView) view.findViewById(R.id.measure_point_title);
			descr = (TextView) view.findViewById(R.id.measure_point_descr);
			deleteBtn = (ImageButton) view.findViewById(R.id.measure_point_remove_image_button);
		}
	}

	interface RemovePointListener {
		void onPointRemove();
	}

	interface ItemClickListener {
		void onItemClick(View view);
	}
}