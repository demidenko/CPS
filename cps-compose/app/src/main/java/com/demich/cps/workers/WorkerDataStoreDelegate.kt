package com.demich.cps.workers

import com.demich.datastore_itemized.dataStoreWrapper


// TODO: pass work instead of provider?
fun workerDataStoreDelegate(provider: CPSPeriodicWorkProvider) =
    dataStoreWrapper(name = "WORKER_${provider.workName}_storage")
