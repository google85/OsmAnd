package net.osmand.plus.backup;

import static net.osmand.plus.backup.NetworkSettingsHelper.BACKUP_ITEMS_KEY;
import static net.osmand.plus.backup.NetworkSettingsHelper.RESTORE_ITEMS_KEY;
import static net.osmand.plus.backup.NetworkSettingsHelper.SyncOperationType.SYNC_OPERATION_DOWNLOAD;
import static net.osmand.plus.backup.NetworkSettingsHelper.SyncOperationType.SYNC_OPERATION_SYNC;
import static net.osmand.plus.backup.NetworkSettingsHelper.SyncOperationType.SYNC_OPERATION_UPLOAD;
import static net.osmand.plus.backup.PrepareBackupResult.RemoteFilesType.UNIQUE;

import android.os.AsyncTask;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.PlatformUtil;
import net.osmand.plus.AppInitializer;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.backup.NetworkSettingsHelper.BackupExportListener;
import net.osmand.plus.backup.NetworkSettingsHelper.SyncOperationType;
import net.osmand.plus.backup.PrepareBackupResult.RemoteFilesType;
import net.osmand.plus.backup.PrepareBackupTask.OnPrepareBackupListener;
import net.osmand.plus.settings.backend.ExportSettingsType;
import net.osmand.plus.settings.backend.backup.SettingsHelper.ImportListener;
import net.osmand.plus.settings.backend.backup.items.SettingsItem;

import org.apache.commons.logging.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SyncBackupTask extends AsyncTask<Void, Void, Void> implements OnPrepareBackupListener, ImportListener, BackupExportListener {

	private static final Log LOG = PlatformUtil.getLog(SyncBackupTask.class);

	private final String key;

	private final OsmandApplication app;
	private final BackupHelper backupHelper;
	private final NetworkSettingsHelper networkSettingsHelper;

	private final SyncOperationType operation;
	private final OnBackupSyncListener syncListener;
	private final boolean singleOperation;

	private int maxProgress;
	private int importProgress;
	private int exportProgress;

	public SyncBackupTask(@NonNull OsmandApplication app, @NonNull String key,
	                      @NonNull SyncOperationType operation,
	                      @Nullable OnBackupSyncListener syncListener) {
		this.app = app;
		this.key = key;
		this.operation = operation;
		this.singleOperation = operation != SYNC_OPERATION_SYNC;
		this.syncListener = syncListener;
		this.backupHelper = app.getBackupHelper();
		this.networkSettingsHelper = app.getNetworkSettingsHelper();

		backupHelper.addPrepareBackupListener(this);
	}

	public int getMaxProgress() {
		return maxProgress;
	}

	public int getGeneralProgress() {
		return importProgress + exportProgress;
	}

	@Override
	protected void onCancelled() {
		backupHelper.removePrepareBackupListener(this);
	}

	@Override
	protected Void doInBackground(Void... voids) {
		if (!backupHelper.isBackupPreparing()) {
			startSync();
		}
		return null;
	}

	private void startSync() {
		PrepareBackupResult backup = backupHelper.getBackup();
		BackupInfo info = backup.getBackupInfo();

		List<SettingsItem> settingsItems = BackupHelper.getItemsForRestore(info, backup.getSettingsItems());

		if (operation != SYNC_OPERATION_DOWNLOAD) {
			maxProgress += calculateExportMaxProgress() / 1024;
		}
		if (operation != SYNC_OPERATION_UPLOAD) {
			maxProgress += ImportBackupTask.calculateMaxProgress(app);
		}
		if (syncListener != null) {
			syncListener.onBackupSyncStarted();
		}
		if (settingsItems.size() > 0 && operation != SYNC_OPERATION_UPLOAD) {
			networkSettingsHelper.importSettings(RESTORE_ITEMS_KEY, settingsItems, UNIQUE, true, this);
		} else if (operation != SYNC_OPERATION_DOWNLOAD) {
			uploadNewItems();
		} else {
			onSyncFinished(null);
		}
	}

	public void uploadLocalItem(@NonNull SettingsItem item) {
		networkSettingsHelper.exportSettings(
				BackupHelper.getItemFileName(item), Collections.singletonList(item),
				Collections.emptyList(), Collections.emptyList(), this);
	}

	public void deleteItem(@NonNull SettingsItem item) {
		networkSettingsHelper.exportSettings(BackupHelper.getItemFileName(item), Collections.emptyList(),
				Collections.singletonList(item), Collections.emptyList(), this);
	}

	public void deleteLocalItem(@NonNull SettingsItem item) {
		networkSettingsHelper.exportSettings(BackupHelper.getItemFileName(item), Collections.emptyList(),
				Collections.emptyList(), Collections.singletonList(item), this);
	}

	public void downloadRemoteVersion(@NonNull SettingsItem item, @NonNull RemoteFilesType filesType) {
		item.setShouldReplace(true);
		String name = BackupHelper.getItemFileName(item);
		networkSettingsHelper.importSettings(name, Collections.singletonList(item), filesType, true, this);
	}

	private void uploadNewItems() {
		if (isCancelled()) {
			return;
		}
		try {
			BackupInfo info = backupHelper.getBackup().getBackupInfo();
			List<SettingsItem> itemsToUpload = info.itemsToUpload;
			List<SettingsItem> itemsToDelete = info.itemsToDelete;
			List<SettingsItem> itemsToLocalDelete = info.itemsToLocalDelete;
			if (itemsToUpload.size() > 0 || itemsToDelete.size() > 0 || itemsToLocalDelete.size() > 0) {
				networkSettingsHelper.exportSettings(BACKUP_ITEMS_KEY, itemsToUpload, itemsToDelete, itemsToLocalDelete, this);
			} else {
				onSyncFinished(null);
			}
		} catch (Exception e) {
			LOG.error("Backup generation error: ", e);
		}
	}

	private long calculateExportMaxProgress() {
		BackupInfo info = backupHelper.getBackup().getBackupInfo();
		if (info != null) {
			List<SettingsItem> oldItemsToDelete = new ArrayList<>();
			for (SettingsItem item : info.itemsToUpload) {
				ExportSettingsType exportType = ExportSettingsType.getExportSettingsTypeForItem(item);
				if (exportType != null && backupHelper.getVersionHistoryTypePref(exportType).get()) {
					oldItemsToDelete.add(item);
				}
			}
			return ExportBackupTask.getEstimatedItemsSize(app, info.itemsToUpload,
					info.itemsToDelete, info.itemsToLocalDelete, oldItemsToDelete);
		}
		return 0;
	}

	@Override
	public void onBackupPreparing() {

	}

	@Override
	public void onBackupPrepared(@Nullable PrepareBackupResult backupResult) {
		startSync();
		if (syncListener != null) {
			syncListener.onBackupSyncStarted();
		}
	}

	@Override
	public void onImportFinished(boolean succeed, boolean needRestart, @NonNull List<SettingsItem> items) {
		if (isCancelled()) {
			onSyncFinished(null);
			return;
		}
		if (succeed) {
			app.getRendererRegistry().updateExternalRenderers();
			app.getPoiFilters().loadSelectedPoiFilters();
			AppInitializer.loadRoutingFiles(app, null);
//			app.getResourceManager().reloadIndexesAsync(null, null);
//			AudioVideoNotesPlugin plugin = PluginsHelper.getPlugin(AudioVideoNotesPlugin.class);
//			if (plugin != null) {
//				plugin.indexingFiles(true, true);
//			}
		}
		if (singleOperation) {
			onSyncFinished(null);
		} else {
			uploadNewItems();
		}
	}

	@Override
	public void onImportItemStarted(@NonNull String type, @NonNull String fileName, int work) {
		if (syncListener != null) {
			syncListener.onBackupItemStarted(type, fileName, work);
		}
	}

	@Override
	public void onImportItemProgress(@NonNull String type, @NonNull String fileName, int value) {
		if (syncListener != null) {
			syncListener.onBackupItemProgress(type, fileName, value);
		}
	}

	@Override
	public void onImportItemFinished(@NonNull String type, @NonNull String fileName) {
		if (syncListener != null) {
			syncListener.onBackupItemFinished(type, fileName);
		}
	}

	@Override
	public void onImportProgressUpdate(int value, int uploadedKb) {
		importProgress = uploadedKb;
		if (syncListener != null) {
			syncListener.onBackupProgressUpdate(getGeneralProgress());
		}
	}

	private void onSyncFinished(@Nullable String error) {
		backupHelper.removePrepareBackupListener(this);
		networkSettingsHelper.unregisterSyncBackupTask(key);

		if (syncListener != null) {
			syncListener.onBackupSyncFinished(error);
		}
	}

	@Override
	public void onBackupExportProgressUpdate(int progress) {
		exportProgress = progress;
		if (syncListener != null) {
			syncListener.onBackupProgressUpdate(getGeneralProgress());
		}
	}

	@Override
	public void onBackupExportStarted() {

	}

	@Override
	public void onBackupExportFinished(@Nullable String error) {
		onSyncFinished(error);
	}

	@Override
	public void onBackupExportItemFinished(@NonNull String type, @NonNull String fileName) {
		if (syncListener != null) {
			syncListener.onBackupItemFinished(type, fileName);
		}
	}

	@Override
	public void onBackupExportItemStarted(@NonNull String type, @NonNull String fileName, int work) {
		if (syncListener != null) {
			syncListener.onBackupItemStarted(type, fileName, work);
		}
	}

	@Override
	public void onBackupExportItemProgress(@NonNull String type, @NonNull String fileName, int value) {
		if (syncListener != null) {
			syncListener.onBackupItemProgress(type, fileName, value);
		}
	}

	public interface OnBackupSyncListener {

		default void onBackupSyncTasksUpdated() {

		}

		default void onBackupSyncStarted() {

		}

		default void onBackupProgressUpdate(int progress) {

		}

		default void onBackupSyncFinished(@Nullable String error) {

		}

		default void onBackupItemStarted(@NonNull String type, @NonNull String fileName, int work) {

		}

		default void onBackupItemProgress(@NonNull String type, @NonNull String fileName, int value) {

		}

		default void onBackupItemFinished(@NonNull String type, @NonNull String fileName) {

		}
	}
}
