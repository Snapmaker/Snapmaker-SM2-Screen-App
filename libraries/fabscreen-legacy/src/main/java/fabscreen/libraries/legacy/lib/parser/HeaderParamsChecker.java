package fabscreen.libraries.legacy.lib.parser;

public class HeaderParamsChecker {
    public boolean nozzleTempCheck;
    public boolean heatedBedTempCheck;
    public boolean laserPowerCheck;
    public boolean totalLinesCheck;
    public boolean estimatedTimeCheck;
    public boolean extruder0RetractionCheck;
    public boolean extruder1RetractionCheck;
    public boolean boundaryCheck;

    private static volatile HeaderParamsChecker mInstance;

    public static HeaderParamsChecker getInstance() {
        if (mInstance == null) {
            synchronized (HeaderParamsChecker.class) {
                if (mInstance == null) {
                    mInstance = new HeaderParamsChecker();
                }
            }
        }

        return mInstance;
    }

    private HeaderParamsChecker() {
        this.totalLinesCheck = false;
        this.estimatedTimeCheck = false;
        this.nozzleTempCheck = false;
        this.heatedBedTempCheck = false;
        this.laserPowerCheck = false;
        this.extruder0RetractionCheck = false;
        this.extruder1RetractionCheck = false;
        this.boundaryCheck = false;
    }

    public void setTotalLinesCheck(boolean totalLinesCheck) {
        this.totalLinesCheck = totalLinesCheck;
    }

    public void setEstimatedTimeCheck(boolean estimatedTimeCheck) {
        this.estimatedTimeCheck = estimatedTimeCheck;
    }

    public void setExtruder0RetractionCheck(boolean extruder0RetractionCheck) {
        this.extruder0RetractionCheck = extruder0RetractionCheck;
    }

    public void setExtruder1RetractionCheck(boolean extruder1RetractionCheck) {
        this.extruder1RetractionCheck = extruder1RetractionCheck;
    }

    public void setNozzleTempCheck(boolean nozzleTempCheck) {
        this.nozzleTempCheck = nozzleTempCheck;
    }

    public void setHeatedBedTempCheck(boolean heatedBedTempCheck) {
        this.heatedBedTempCheck = heatedBedTempCheck;
    }

    public void setBoundaryCheck(boolean boundaryCheck) {
        this.boundaryCheck = boundaryCheck;
    }

    public void setLaserPowerCheck(boolean laserPowerCheck) {
        this.laserPowerCheck = laserPowerCheck;
    }

    public boolean isTotalLinesCheck() {
        return totalLinesCheck;
    }

    public boolean isEstimatedTimeCheck() {
        return estimatedTimeCheck;
    }

    public boolean isExtruder0RetractionCheck() {
        return extruder0RetractionCheck;
    }

    public boolean isExtruder1RetractionCheck() {
        return extruder1RetractionCheck;
    }

    public boolean isNozzleTempCheck() {
        return nozzleTempCheck;
    }

    public boolean isHeatedBedTempCheck() {
        return heatedBedTempCheck;
    }

    public boolean isBoundaryCheck() {
        return boundaryCheck;
    }

    public boolean isLaserPowerCheck() {
        return laserPowerCheck;
    }
}
