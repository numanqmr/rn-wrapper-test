package androidx.core.app;

import androidx.core.app.JobIntentService;

public abstract class JobIntentServiceImpl extends JobIntentService {
    // This method is the main reason for the bug and crash
    @Override
    GenericWorkItem dequeueWork() {
        try {
            return super.dequeueWork();
        } catch (SecurityException exception) {
            // the exception will be ignored here.
        }
        return null;
    }

}
