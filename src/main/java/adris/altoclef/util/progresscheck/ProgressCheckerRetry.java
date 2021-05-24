package adris.altoclef.util.progresscheck;


/**
 * A progress checker that can fail a few times before it "actually" fails.
 */
public class ProgressCheckerRetry<T> implements IProgressChecker<T> {
    
    private final IProgressChecker<T> delegate;
    private final int allowedAttempts;
    
    private int failCount;
    
    @SuppressWarnings("BoundedWildcard")
    public ProgressCheckerRetry(IProgressChecker<T> delegate, int allowedAttempts) {
        this.delegate = delegate;
        this.allowedAttempts = allowedAttempts;
    }
    
    @Override
    public void setProgress(T progress) {
        delegate.setProgress(progress);
        
        // If our delegate checker fails, retry with an updated fail counter.
        if (delegate.failed()) {
            failCount++;
            delegate.reset();
        }
    }
    
    @Override
    public boolean failed() {
        return failCount >= allowedAttempts;
    }
    
    @Override
    public void reset() {
        delegate.reset();
        failCount = 0;
    }
}
