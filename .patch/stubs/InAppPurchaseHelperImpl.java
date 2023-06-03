package net.osmand.plus.inapp;

import android.app.Activity;
import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import net.osmand.plus.OsmandApplication;
import java.lang.ref.WeakReference;

public class InAppPurchaseHelperImpl extends InAppPurchaseHelper {
    public InAppPurchaseHelperImpl(OsmandApplication ctx) { super(ctx); }
    public void isInAppPurchaseSupported(
        @NonNull final Activity activity,
        @Nullable final InAppPurchaseInitCallback callback
    ) { }
    protected void execImpl(
        @NonNull final InAppPurchaseTaskType taskType,
        @NonNull final InAppCommand runnable
    ) { }
    public void purchaseFullVersion(@NonNull final Activity activity) { }
    public void purchaseDepthContours(@NonNull final Activity activity) { }
    public void purchaseContourLines(@NonNull Activity activity)
        throws UnsupportedOperationException { }
    public void manageSubscription(
        @NonNull Context ctx, @Nullable String sku
    ) { }
    protected InAppCommand getPurchaseSubscriptionCommand(
        final WeakReference<Activity> activity,
        final String sku,
        final String userInfo
    ) { return null; }
    protected InAppCommand getRequestInventoryCommand(boolean userRequested) {
        return null;
    }
    protected boolean isBillingManagerExists() { return false; }
    protected void destroyBillingManager() { }
}

