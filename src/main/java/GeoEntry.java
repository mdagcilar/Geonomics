import java.time.LocalDate;

class GeoEntry {

    private int geoID;
    private String name;
    private LocalDate geoOccupiedDate;

    GeoEntry(int geoID, String name, LocalDate geoOccupiedDate) {
        this.geoID = geoID;
        this.name = name;
        this.geoOccupiedDate = geoOccupiedDate;
    }

    public int getGeoID() {
        return geoID;
    }

    public String getName() {
        return name;
    }

    public LocalDate getGeoOccupiedDate() {
        return geoOccupiedDate;
    }

    /**
     * Calculate a GeoIDs 2D coordinates in a 10,000 by 10,000 grid.
     *
     * @return - x, y coordinate of the GeoID in a 2D array
     */
    public GeoCoordinate getGeoCoordinates(int width, int height) {
        int x = geoID % width;
        int y = geoID / width;

        return new GeoCoordinate(x, y);
    }
}