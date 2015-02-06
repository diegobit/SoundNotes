package it.giorgini.soundnotes;

import android.app.Application;

public class SNApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        // Handler per sapere quando l'app è/non è visibile/in foreground/in background
        registerActivityLifecycleCallbacks(new LifecycleHandler());
    }
}