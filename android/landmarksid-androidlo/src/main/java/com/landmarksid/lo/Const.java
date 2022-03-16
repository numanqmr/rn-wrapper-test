package com.landmarksid.lo;

public final class Const {
    public final static class Fields {
        public static final String TYPE = "type";
        public static final String DEVICE_SPEED = "deviceSpeed";
        public static final String AUTHORIZATION = "Authorization";

        public static final String _ID = "_id";
        public static final String MESSAGE_TYPE = "messageType";
        public static final String SCHEMA_VERSION = "schemaVersion";
        public static final String APP_ID = "appId";
        public static final String DEVICE_ID = "deviceId";
        public static final String SOURCE_EVENT_ID = "sourceEventId";
        public static final String SOURCE = "source";
        public static final String LAT = "lat";
        public static final String LONG = "long";
        public static final String HORIZONTAL_ACCURACY = "horizontalAccuracy";
        public static final String VERTICAL_ACCURACY = "verticalAccuracy";
        public static final String ALTITUDE = "altitude";
        public static final String EVENT_TRIGGER = "eventTrigger";
        public static final String SDK_VERSION = "sdkVersion";
        public static final String DEVICE_TYPE = "deviceType";
        public static final String OS_VERSION = "osVersion";
        public static final String APP_BUILD_VERSION = "appBuildVersion";
        public static final String AD_TRACKING_ENABLED = "adTrackingEnabled";
        public static final String CUSTOM_DATA = "customData";
        public static final String KEY = "key";
        public static final String VALUE = "value";
        public static final String BODY = "body";
        public static final String ANDROID_ENABLED = "androidEnabled";
        public static final String TIME_INTERVAL_MINS = "timeIntervalMins";
        public static final String DISTANCE_INTERVAL_METERS = "distanceIntervalMeters";
        public static final String DISCOVERY_MODE = "discoveryMode";
        public static final String MONITORED_APPS = "monitoredApps";
        public static final String EVENT_TIME = "eventTime";
        public static final String EVENTS = "events";
        public static final String CLIENT_ID = "clientId";
        public static final String APP_VERSION = "appVersion";
        public static final String APP_SATE = "appState";
        public static final String BATTERY_STATUS = "batteryStatus";
        public static final String BATTERY_LEVEL = "batteryLevel";
        public static final String NETWORK_STATUS = "networkStatus";
        public static final String CUSTOMER_ID = "customerId";
        public static final String VENDOR_ID = "vendorId";
        public static final String LOCATION_PERMISSION = "locationPermission";
        public static final String DEVICE_MODEL = "deviceModel";
        public static final String DEVICE_OS = "os";
        public static final String INSTALLED_APPS = "installedApps";
        public static final String MIN_SPEED_KPH = "minSpeedKph";
        public static final String MAX_SPEED_KPH = "maxSpeedKph";
        public static final String DISTANCE_FILTER_METERS = "distanceFilterMeters";
        public static final String SOURCE_EVENTS = "sourceEvents";
        public static final String SIM_OPERATOR_NAME = "simOperatorName";
        public static final String SIM_OPERATOR_ISO = "simOperatorIso";
        public static final String NETWORK_OPERATOR_NAME = "networkOperatorName";
        public static final String NETWORK_OPERATOR_ISO = "networkOperatorIso";
        public static final String BATCH_SIZE = "batchSize";
        public static final String MOTION_ACTIVITY = "motionActivity";
        public static final String ENABLE_MOTION_ACTIVITY = "enableMotionActivity";
        public static final String ENABLE_SENTRY = "enableSentry";
        public static final String SUPPRESS_CHECK_CONFIG_MINS = "suppressCheckConfigMins";
    }

    public final static class Values {
        public static final String DEVICE_TYPE = "Android";
        public static final String MESSAGE_TYPE = "lore";
        public static final String SOURCE_LANDMARKS = "landmarksIDLO";

        public static final String EVENT_GEOFENCE = "geofence";
        public static final String EVENT_PERIODICAL = "periodical";
    }

    public final static class Extras {
        public static final String API_KEY = "com.landmarksid.android.extra_apiKey";
    }

    public final static class RequestCodes {
        public static final int LOCATION_PERMISSIONS = 10;
    }

    public final static class Endpoints {
        public static final String API_DEBUG = "https://events-staging.landmarksid.com";
        public static final String API_PRODUCTION = "https://events.landmarksid.com";

        public static final String LOCATION_PINGS = "/lore/event"; // POST
        public static final String LOCATION_PING_CONFIG = "/config/"; // :appId GET
        public static final String DEVICE_INFO = "/device-info"; // POST

        public static final String SENTRY_DSN = "https://08c56774d3204cb48b988394f482db60@o478368.ingest.sentry.io/5520806";
    }

    public final static class Preferences {
        public static final String APP_ID = "com.landmarksid.android.pref_appId";
        public static final String APP_SECRET = "com.landmarksid.android.pref_appSecret";
        public static final String UUID = "com.landmarksid.android.pref_uuid";
        public static final String DEBUG_MODE = "com.landmarksid.android.pref_debugMode";

        public static final String CUSTOM_DATA = "com.landmarksid.android.pref_customData";
        public static final String CUSTOM_DATA_PREV = "com.landmarksid.android.pref_customDataPrev";
        public static final String AD_TRACKING = "com.landmarksid.android.pref_adTracking";

        public static final String CLIENT_ID_PREV = "com.landmarksid.android.pref_clientIdPrev";
        public static final String CLIENT_ID = "com.landmarksid.android.pref_clientId";
        public static final String APP_ID_PREV = "com.landmarksid.android.pref_appIdPrev";
        public static final String APP_VERSION_PREV = "com.landmarksid.android.pref_appVersionPrev";
        public static final String APP_VERSION = "com.landmarksid.android.pref_appVersion";
        public static final String CUSTOMER_ID_PREV = "com.landmarksid.android.pref_customerIdPrev";
        public static final String CUSTOMER_ID = "com.landmarksid.android.pref_customerId";
        public static final String VENDOR_ID_PREV = "com.landmarksid.android.pref_vendorIdPrev";
        public static final String VENDOR_ID = "com.landmarksid.android.pref_vendorId";
        public static final String DEVICE_ID_PREV = "com.landmarksid.android.pref_deviceIdPrev";
        public static final String AD_TRACKING_PREV = "com.landmarksid.android.pref_adTrackingPrev";
        public static final String LOCATION_PERMISSION_PREV = "com.landmarksid.android.pref_locPermissionPrev";
        public static final String LOCATION_PERMISSION = "com.landmarksid.android.pref_locPermission";
        public static final String DEVICE_MODEL_PREV = "com.landmarksid.android.pref_deviceModelPrev";
        public static final String DEVICE_OS_PREV = "com.landmarksid.android.pref_deviceOsPrev";
        public static final String OS_VERSION_PREV = "com.landmarksid.android.pref_osVersionPrev";
        public static final String INSTALLED_APPS_PREV = "com.landmarksid.android.pref_installedAppsPrev";
        public static final String INSTALLED_APPS = "com.landmarksid.android.pref_installedApps";

        public static final String SIM_OPERATOR_NAME_PREV = "com.landmarksid.android.pref_simOperatorNamePrev";
        public static final String SIM_OPERATOR_NAME = "com.landmarksid.android.pref_simOperatorName";
        public static final String SIM_OPERATOR_ISO_PREV = "com.landmarksid.android.pref_simOperatorIsoPrev";
        public static final String SIM_OPERATOR_ISO = "com.landmarksid.android.pref_simOperatorIso";
        public static final String NETWORK_OPERATOR_NAME_PREV = "com.landmarksid.android.pref_networkOperatorNamePrev";
        public static final String NETWORK_OPERATOR_NAME = "com.landmarksid.android.pref_networkOperatorName";
        public static final String NETWORK_OPERATOR_ISO_PREV = "com.landmarksid.android.pref_networkOperatorIsoPrev";
        public static final String NETWORK_OPERATOR_ISO = "com.landmarksid.android.pref_networkOperatorIso";

        public static final String LOCATION_USABLE = "com.landmarksid.android.pref_locUsable";
        public static final String PERMISSIONS_REQUESTED = "com.landmarksid.android.pref_permsReqd";
        public static final String PERMISSIONS_ANSWERED = "com.landmarksid.android.pref_permsAns";

        public static final String ANDROID_ENABLED = "com.landmarksid.android.pref_androidEnabled";
        public static final String TIME_INTERVAL_MINS = "com.landmarksid.android.pref_timeIntervalMins";
        public static final String DISTANCE_INTERVAL_METERS = "com.landmarksid.android.pref_distanceIntervalMeters";
        public static final String BLUEDOT_MODE = "com.landmarksid.android.pref_bluedotMode";
        public static final String MONITORED_APPS = "com.landmarksid.android.pref_monitoredApps";
        public static final String ENABLE_MOTION_ACTIVITY = "com.landmarksid.android.pref_enableMotionActivity";
        public static final String ALREADY_REGISTERED_MOTION_ACTIVITY = "com.landmarksid.android.pref_alreadyRegisteredMotionActivity";
        public static final String MOTION_ACTIVITY = "com.landmarksid.android.pref_motionActivity";
        public static final String MIN_SPEED_KPH = "com.landmarksid.android.pref_minSpeedKph";
        public static final String MAX_SPEED_KPH = "com.landmarksid.android.pref_maxSpeedKph";
        public static final String DISTANCE_FILTER_METERS = "com.landmarksid.android.pref_distanceFilterMeters";
        public static final String BATCH_SIZE = "com.landmarksid.android.pref_batchSize";

        public static final String LAST_LOC_LONG = "com.landmarksid.android.pref_lastLocLong";
        public static final String LAST_LOC_LAT = "com.landmarksid.android.pref_lastLocLat";
        public static final String LAST_LOC_TIME = "com.landmarksid.android.pref_lastLocTime";

        public static final String ENABLE_SENTRY = "com.landmarksid.android.pref_enableSentry";
        public static final String LAST_CHECKED_ANALYTICS = "com.landmarksid.android.pref_lastCheckedAnalytics";
        public static final String CONFIG_LOADED = "com.landmarksid.android.pref_configLoaded";
        public static final String LAST_FOREGROUND_CALL_DATE_TIME = "com.landmarksid.android.pref_lastForegroundCall";
        public static final String SUPPRESS_CHECK_CONFIG_MINS = "com.landmarksid.android.pref_suppressCheckConfigMins";
    }
}
