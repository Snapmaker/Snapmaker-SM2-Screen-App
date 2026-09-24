package fabscreen.libraries.legacy.lib;

import java.util.ArrayList;

import fabscreen.libraries.legacy.data.Model;
import fabscreen.libraries.legacy.data.model.LaserPattern;
import fabscreen.libraries.legacy.data.serial.fabpacket.FabPacketContent;
import fabscreen.libraries.legacy.lib.parser.Position;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.SingleSubject;

/**
 * FineTune Executor: execute laser pattern engrave in laser calibration.
 */
public class LaserFineTuneExecutor {
    private LaserPattern mPattern;

    private Model mModel;
    private CompositeDisposable mDisposables;

    private ArrayList<Position> mStartPositions;
    private ArrayList<Position> mEndPositions;

    private BehaviorSubject<Integer> mEngraveIndexSubject = BehaviorSubject.createDefault(0);

    public LaserFineTuneExecutor(Model model, CompositeDisposable disposables) {
        mModel = model;
        mDisposables = disposables;
    }

    public void setLaserPattern(LaserPattern pattern) {
        mPattern = pattern;
    }

    public Single<Boolean> startFineTune() {
        if (mPattern == null) {
            return SingleSubject.error(new FabException("Laser Pattern can not be null!"));
        }

        SingleSubject<Boolean> resultSubject = SingleSubject.create();

        // calculate all point
        Disposable sub = calculatePoints()
                .flatMap(success -> {
                    if (!success) {
                        return Observable.just(-1);
                    }
                    return mEngraveIndexSubject.hide();
                })
                .subscribe(index -> {
                    if (index == -1) {
                        resultSubject.onSuccess(false);
                        return;
                    }
                    if (index < mPattern.getTotalLines()) {
                        nextLine(index);
                    } else {
                        // goto work origin
                        mDisposables.add(mModel.getSlaveComputer().sendGcode("G0 X0 Y0 F3000")
                                .subscribe(success -> {
                                    resultSubject.onSuccess(true);
                                }, LogHelper::log));
                    }
                });
        mDisposables.add(sub);

        return resultSubject.hide();
    }

    public Observable<Boolean> calculatePoints() {
        if (mPattern == null) return Observable.just(false);

        int alignment = mPattern.getAlignment();
        int direction = mPattern.getEngraveDirection();

        mStartPositions = new ArrayList<>();
        mEndPositions = new ArrayList<>();

        // get current z position
        final float initialZPos = (float) mModel.getSlaveComputer().getMachineStatus().z;

        for (int i = 0; i < mPattern.getTotalLines(); i++) {
            float startX, startY;
            float endX, endY;
            int lineLength = 0;

            lineLength = i % mPattern.getUnitCountPerDivision() == 0 ? mPattern.getLongEngraveLineLength()
                    : mPattern.getShortEngraveLineLength();

            // Calculate start and end point with pattern direction and alignment.
            if (direction == LaserPattern.DIRECTION_X) {
                startX = mPattern.getSpacingPerLine() * (-10 + i);
                endX = startX;
                switch (alignment) {
                    case LaserPattern.ALIGNMENT_ENGRAVE_CENTER:
                        startY = (-1) * lineLength * 0.5f;
                        endY = lineLength * 0.5f;
                        break;
                    case LaserPattern.ALIGNMENT_ENGRAVE_END:
                        startY = mPattern.getLongEngraveLineLength() * 0.5f;
                        endY = startY - lineLength;
                        break;
                    case LaserPattern.ALIGNMENT_ENGRAVE_START:
                    default:
                        startY = (-1) * mPattern.getLongEngraveLineLength() * 0.5f;
                        endY = startY + lineLength;
                        break;
                }
            } else if (direction == LaserPattern.DIRECTION_Y) {
                startY = mPattern.getSpacingPerLine() * (-10 + i);
                endY = startY;
                switch (alignment) {
                    case LaserPattern.ALIGNMENT_ENGRAVE_CENTER:
                        startX = (-1) * lineLength * 0.5f;
                        endX = lineLength * 0.5f;
                        break;
                    case LaserPattern.ALIGNMENT_ENGRAVE_END:
                        startX = mPattern.getLongEngraveLineLength() * 0.5f;
                        endX = startX - lineLength;
                        break;
                    case LaserPattern.ALIGNMENT_ENGRAVE_START:
                    default:
                        startX = (-1) * mPattern.getLongEngraveLineLength() * 0.5f;
                        endX = startX + lineLength;
                        break;
                }
            } else {
                // TODO: If have any other direction?
                return Observable.just(false);
            }
            float lineZ = initialZPos + ((-10 + i) * mPattern.getZOffset());

            // save point
            mStartPositions.add(new Position(startX, startY, lineZ));
            mEndPositions.add(new Position(endX, endY, lineZ));
        }

        return Observable.just(true);
    }

    private void nextLine(int index) {
        Disposable sub = moveToNextStartPosition(mStartPositions.get(index))
                .flatMap(success -> engraveLine(mEndPositions.get(index)))
                .subscribe(success -> {
                    mEngraveIndexSubject.onNext(mEngraveIndexSubject.getValue() + 1);
                }, e -> {
                    LogHelper.log(e);
                    mEngraveIndexSubject.onNext(-1);
                });
        mDisposables.add(sub);
    }

    private Observable<Boolean> engraveLine(Position endPos) {
        return turnOnLaser()
                .flatMap(ret -> mModel.getSlaveComputer().gotoAbsolutePosition(endPos.x, endPos.y, endPos.z, 300))
                .flatMap(success -> turnOffLaser())
                .flatMap(response -> Observable.just(true));
    }

    private Observable<Boolean> moveToNextStartPosition(Position position) {
        return mModel.getSlaveComputer().gotoAbsolutePosition(position.x, position.y, position.z, 3000);
    }

    private Observable<FabPacketContent.GcodeResponse> turnOnLaser() {
        return mModel.getSlaveComputer().sendGcode("M3 P70");
    }

    private Observable<FabPacketContent.GcodeResponse> turnOffLaser() {
        return mModel.getSlaveComputer().sendGcode("M5");
    }
}
