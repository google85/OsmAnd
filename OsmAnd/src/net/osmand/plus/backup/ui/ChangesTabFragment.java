package net.osmand.plus.backup.ui;

import static net.osmand.plus.backup.NetworkSettingsHelper.SyncOperationType.SYNC_OPERATION_DELETE;
import static net.osmand.plus.backup.ui.ChangesFragment.RecentChangesType.RECENT_CHANGES_CONFLICTS;
import static net.osmand.plus.backup.ui.ChangesFragment.RecentChangesType.RECENT_CHANGES_LOCAL;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.plus.R;
import net.osmand.plus.backup.BackupHelper;
import net.osmand.plus.backup.LocalFile;
import net.osmand.plus.backup.NetworkSettingsHelper;
import net.osmand.plus.backup.NetworkSettingsHelper.SyncOperationType;
import net.osmand.plus.backup.PrepareBackupResult;
import net.osmand.plus.backup.PrepareBackupTask.OnPrepareBackupListener;
import net.osmand.plus.backup.RemoteFile;
import net.osmand.plus.backup.SyncBackupTask.OnBackupSyncListener;
import net.osmand.plus.backup.ui.ChangesFragment.RecentChangesType;
import net.osmand.plus.base.BaseOsmAndFragment;
import net.osmand.plus.settings.backend.backup.items.SettingsItem;

import java.util.List;

public abstract class ChangesTabFragment extends BaseOsmAndFragment implements OnPrepareBackupListener,
		OnBackupSyncListener {

	protected BackupHelper backupHelper;
	protected NetworkSettingsHelper settingsHelper;

	protected ChangesAdapter adapter;
	protected RecentChangesType tabType = getChangesTabType();

	@NonNull
	public abstract RecentChangesType getChangesTabType();

	@NonNull
	public abstract List<CloudChangeItem> generateData();

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		backupHelper = app.getBackupHelper();
		settingsHelper = app.getNetworkSettingsHelper();
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		updateNightMode();
		FragmentActivity activity = requireActivity();
		View view = themedInflater.inflate(R.layout.fragment_changes_tab, container, false);

		adapter = new ChangesAdapter(app, this, nightMode);

		RecyclerView recyclerView = view.findViewById(R.id.recycler_view);
		recyclerView.setLayoutManager(new LinearLayoutManager(activity));
		recyclerView.setAdapter(adapter);
		recyclerView.setItemAnimator(null);
		recyclerView.setLayoutAnimation(null);

		return view;
	}

	@Override
	public void onResume() {
		super.onResume();
		backupHelper.addPrepareBackupListener(this);
		settingsHelper.addBackupSyncListener(this);
		updateAdapter();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		backupHelper.removePrepareBackupListener(this);
		settingsHelper.removeBackupSyncListener(this);
	}

	@Override
	public void onBackupPreparing() {
		app.runInUIThread(this::updateAdapter);
	}

	@Override
	public void onBackupPrepared(@Nullable PrepareBackupResult backupResult) {
		app.runInUIThread(this::updateAdapter);
	}

	@Override
	public void onBackupSyncStarted() {
		app.runInUIThread(() -> adapter.onBackupSyncStarted());
	}

	@Override
	public void onBackupProgressUpdate(int progress) {
		app.runInUIThread(() -> adapter.onBackupProgressUpdate(progress));
	}

	@Override
	public void onBackupSyncFinished(@Nullable String error) {
		app.runInUIThread(this::updateAdapter);
	}

	@Override
	public void onBackupItemStarted(@NonNull String type, @NonNull String fileName, int work) {
		app.runInUIThread(() -> adapter.onBackupItemStarted(type, fileName, work));
	}

	@Override
	public void onBackupItemProgress(@NonNull String type, @NonNull String fileName, int value) {
		app.runInUIThread(() -> adapter.onBackupItemProgress(type, fileName, value));
	}

	@Override
	public void onBackupItemFinished(@NonNull String type, @NonNull String fileName) {
		app.runInUIThread(() -> adapter.onBackupItemFinished(type, fileName));
	}

	private void updateAdapter() {
		if (adapter != null) {
			adapter.setCloudChangeItems(generateData());
		}
	}

	private String localizedSummaryForOperation(SyncOperationType operation, LocalFile localFile, RemoteFile remoteFile) {
		switch (operation) {
			case SYNC_OPERATION_DOWNLOAD:
				return app.getString(localFile != null ? R.string.shared_string_modified : R.string.shared_string_added);
			case SYNC_OPERATION_UPLOAD:
				return app.getString(remoteFile != null ? R.string.shared_string_modified : R.string.shared_string_added);
			case SYNC_OPERATION_DELETE:
				return app.getString(R.string.shared_string_deleted);
			default:
				return app.getString(tabType == RECENT_CHANGES_CONFLICTS
						? R.string.last_sync : R.string.shared_string_modified);
		}
	}

	static class FileInfo {
		public LocalFile localFile;
		public RemoteFile remoteFile;
		public boolean deleted;
	}

	public static class CloudChangeItem {

		public String title;
		public String fileName;
		public String description;
		public int iconId;
		public LocalFile localFile;
		public RemoteFile remoteFile;
		public SettingsItem settingsItem;
		public SyncOperationType operation;
		public String summary;
		public String time;
		public boolean synced;

		@NonNull
		@Override
		public String toString() {
			return fileName;
		}
	}

	@Nullable
	protected CloudChangeItem createChangeItem(@NonNull SyncOperationType operationType,
	                                           @Nullable LocalFile localFile, @Nullable RemoteFile remoteFile) {
		boolean local = tabType == RECENT_CHANGES_LOCAL;
		SettingsItem settingsItem = getSettingsItem(local, localFile, remoteFile);
		if (settingsItem != null) {
			long time = getTime(operationType, localFile, remoteFile);

			CloudChangeItem item = new CloudChangeItem();
			item.title = BackupUiUtils.getItemName(app, settingsItem);
			item.summary = localizedSummaryForOperation(operationType, localFile, remoteFile);
			item.description = BackupUiUtils.generateTimeString(app, item.summary, time);
			item.time = BackupUiUtils.getTimeString(app, time);
			item.iconId = BackupUiUtils.getIconId(settingsItem);
			item.settingsItem = settingsItem;
			item.operation = operationType;
			item.localFile = localFile;
			item.remoteFile = remoteFile;
			item.fileName = BackupHelper.getItemFileName(settingsItem);

			return item;
		}
		return null;
	}

	private long getTime(@NonNull SyncOperationType operationType, @Nullable LocalFile localFile, @Nullable RemoteFile remoteFile) {
		long time = 0;
		if (tabType == RECENT_CHANGES_LOCAL && operationType == SYNC_OPERATION_DELETE)
			time = remoteFile.getClienttimems();
		else if (tabType == RECENT_CHANGES_LOCAL)
			time = localFile.localModifiedTime;
		else if (tabType == RECENT_CHANGES_CONFLICTS)
			time = localFile.uploadTime;
		else if (operationType == SYNC_OPERATION_DELETE)
			time = localFile.uploadTime;
		else {
			time = remoteFile.getUpdatetimems();
		}
		return time;
	}

	@Nullable
	private SettingsItem getSettingsItem(boolean local, @Nullable LocalFile localFile, @Nullable RemoteFile remoteFile) {
		SettingsItem settingsItem;
		if (local) {
			settingsItem = localFile == null ? remoteFile.item : localFile.item;
		} else {
			settingsItem = remoteFile == null ? localFile.item : remoteFile.item;
		}
		return settingsItem;
	}
}