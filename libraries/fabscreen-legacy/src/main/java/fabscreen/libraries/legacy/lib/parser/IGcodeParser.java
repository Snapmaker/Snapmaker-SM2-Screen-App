package fabscreen.libraries.legacy.lib.parser;

import android.graphics.Bitmap;

import fabscreen.libraries.legacy.data.model.ModelBoundary;
import fabscreen.libraries.legacy.lib.file.IFile;
import io.reactivex.Observable;


public interface IGcodeParser {

    void startParse(IFile file, int fileType);

    void destroy();

    int getFileType();

    Bitmap getGcodeThumbnail();

    int getTotalLinesCount();

    float getEstimatedTime();

    float getBedTargetTemperature();

    float getNozzleTargetTemperature();

    float getNozzle1TargetTemperature();

    float getLaserPower();

    float getCNCPower();

    float getSpindleSpeed();

    float getWorkSpeed();

    float getJogSpeed();

    float getDiameter();

    ModelBoundary getBoundary();

    Observable<Integer> getParseProgressObservable();

    float getExtruder0RetractionDistance();

    float getExtruder1RetractionDistance();

    float getExtruder0SwitchRetractionDistance();

    float getExtruder1SwitchRetractionDistance();


}
