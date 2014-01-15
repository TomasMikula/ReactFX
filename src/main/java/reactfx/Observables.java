package reactfx;

import javafx.beans.Observable;
import javafx.beans.WeakInvalidationListener;

public class Observables {

    public static Observable merge(Observable... observables) {
        return new ObservableBase() {
            {
                for(Observable obs: observables) {
                    obs.addListener(new WeakInvalidationListener(o -> notifyListeners()));
                }
            }
        };
    }

    public static Observable releaseOnImpulse(Observable impulse, Observable observable) {
        return new ObservableBase() {
            private boolean fireOnImpulse = false;
            {
                observable.addListener(new WeakInvalidationListener(o -> fireOnImpulse = true));
                impulse.addListener(new WeakInvalidationListener(o -> {
                    if(fireOnImpulse) {
                        fireOnImpulse = false;
                        notifyListeners();
                    }
                }));
            }
        };
    }
}
