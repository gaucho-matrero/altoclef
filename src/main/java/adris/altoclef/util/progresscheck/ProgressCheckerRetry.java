package adris.altoclef.util.progresscheck;

/**
 * A progress checker that can fail a few times before it "actually" fails.
 */
public class ProgressCheckerRetry<T> implements IProgressChecker<T> {

    private final IProgressChecker<T> _subChecker;
    private final int _allowedFailures;

    private int _failCount;

    public ProgressCheckerRetry(IProgressChecker subChecker, int allowedFailures) {
        _subChecker = subChecker;
        _allowedFailures = allowedFailures;
    }

    @Override
    public void setProgress(T progress) {
        _subChecker.setProgress(progress);

        // If our subchecker fails, retry with an updated fail counter.
        if (_subChecker.failed()) {
            _failCount++;
            _subChecker.reset();
        }
    }

    @Override
    public boolean failed() {
        return _failCount > _allowedFailures;
    }

    @Override
    public void reset() {
        _subChecker.reset();
        _failCount = 0;
    }
}
