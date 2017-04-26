package com.soft.cleargrass.otaupdate;

import android.app.Activity;

import no.nordicsemi.android.dfu.DfuBaseService;

/**
 * Created by dongwei on 2017/4/25.
 */

public class DfuService extends DfuBaseService {
    @Override
    protected Class<? extends Activity> getNotificationTarget() {

        return null;
    }
}
