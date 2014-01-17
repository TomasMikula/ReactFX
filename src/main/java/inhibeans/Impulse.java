package inhibeans;


public class Impulse extends ObservableBase implements Observable {

    private boolean blocked = false;
    private boolean fireOnRelease = false;

    @Override
    public Block block() {
        if(blocked) {
            return Block.EMPTY_BLOCK;
        } else {
            blocked = true;
            return this::release;
        }
    }

    private void release() {
        blocked = false;
        if(fireOnRelease) {
            fireOnRelease = false;
            notifyListeners();
        }
    }

    public void trigger() {
        if(blocked)
            fireOnRelease = true;
        else
            notifyListeners();
    }
}
