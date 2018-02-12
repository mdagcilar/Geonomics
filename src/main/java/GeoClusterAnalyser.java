import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

class GeoClusterAnalyser {

    private List<GeoEntry> geoEntries = new ArrayList<>();
    private List<GeoEntry> resultCluster = new ArrayList<>();
    private List<Integer> largestCluster = new ArrayList<>();
    private int[][] geoBlock;

    private int width, height;

    public static void main(String[] args) {
        long startTime = System.nanoTime();

        if (args.length != 3) {
            System.out.println("Please enter three parameters: width of GeoBlock, height of GeoBlock and the name of the csv file");
            System.exit(1);
        }

        GeoClusterAnalyser geoClusterAnalyser = new GeoClusterAnalyser();
        try {
            geoClusterAnalyser.width = Integer.valueOf(args[0]);
            geoClusterAnalyser.height = Integer.valueOf(args[0]);
        } catch (NumberFormatException e) {
            System.out.println("Width and Height of GeoBlock should be Integers");
            System.exit(1);
        }

        // Read in the contents of the csv file
        geoClusterAnalyser.readCSVFile(geoClusterAnalyser.width, geoClusterAnalyser.height, args[2]);

        // initialise the GeoBlock
        geoClusterAnalyser.initialiseGeoBlock(geoClusterAnalyser.width, geoClusterAnalyser.height);

        // find the largest cluster
        geoClusterAnalyser.resultCluster = geoClusterAnalyser.getGeoEntrys(geoClusterAnalyser.getLargestCluster());

        // Print out the largest cluster
        if (geoClusterAnalyser.resultCluster != null) {
            System.out.println("The Geos in the largest cluster of occupied Geos for this GeoBlock are:");
            for (GeoEntry geoEntry : geoClusterAnalyser.resultCluster) {
                System.out.println(geoEntry.getGeoID() + ", " + geoEntry.getName() + ", " + geoEntry.getGeoOccupiedDate());
            }
        } else {
            System.out.println("There are no clusters within this geoblock");
        }

        long endTime = System.nanoTime();
        System.out.println("\nProgram Execution time: " + (endTime - startTime) / 1000000000.0 + " seconds");
    }

    /**
     * Initialise the GeoBlock as a 2D array, and add the occupied Geos with the GeoIDs.
     */
    private void initialiseGeoBlock(int width, int height) {
        geoBlock = new int[width][height];

        /* All elements in a 2D array upon initialisation are default set to the value 0.
         * Because we need to differentiate from our GeoID 0 being at index[0][0], the default value must be something
         * other than 0. Therefore, setting it to -1 allows us to differentiate if this Geo is occupied or not.*/
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                geoBlock[x][y] = -1;
            }
        }

        //Loop through the geo entries and enter the occupied Geos to the geoBlock
        for (GeoEntry geoEntry : geoEntries) {
            geoBlock[geoEntry.getGeoCoordinates(width, height).getCol()][geoEntry.getGeoCoordinates(width, height).getRow()] = geoEntry.getGeoID();
        }
    }

    /**
     * Returns the largest cluster within the GeoBlock
     * - should only return one cluster
     * - if there is more than one cluster with the largest size, sum the values of the cluster and return the lowest
     *
     * @return - List of GeoIDs in the largest cluster
     */
    private List<Integer> getLargestCluster() {
        List<Integer> tempGeoCluster;

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                if (geoBlock[x][y] != -1) {
                    tempGeoCluster = findConnectingGeos(x, y, null);

                    // if the temporary cluster being visited is larger than the current largest, a larger cluster has been found
                    if (tempGeoCluster.size() > largestCluster.size()) {
                        largestCluster = tempGeoCluster;
                    } else if (tempGeoCluster.size() == largestCluster.size()) {
                        if (tempGeoCluster.stream().mapToInt(Integer::intValue).sum() < largestCluster.stream().mapToInt(Integer::intValue).sum())
                            largestCluster = tempGeoCluster;
                    }
                }
            }
        }
        return largestCluster;
    }

    /**
     * Finds clusters with connecting Geos recursively.
     *
     * @param x    - x pos in 2D array
     * @param y    - y pos in 2D array
     * @param temp - List containing temporary cluster
     * @return - List of GeoIDs in the largest cluster
     */
    private List<Integer> findConnectingGeos(int x, int y, List<Integer> temp) {
        List<Integer> tempGeoCluster;

        if (temp != null) {
            tempGeoCluster = temp;
        } else {
            tempGeoCluster = new ArrayList<>();
            tempGeoCluster.add(geoBlock[x][y]);
        }

        // check if any of the 4 neighboring Geos are occupied (not 0).
        // recursively check each non-null Geo for their connecting neighbors to build the cluster
        if (y != height - 1) {
            if (geoBlock[x][y + 1] != -1) {      // up
                if (!tempGeoCluster.contains(geoBlock[x][y + 1])) { // avoid stackOverflow exception by recursively re-checking nodes that are already in the cluster.
                    tempGeoCluster.add(geoBlock[x][y + 1]);
                    findConnectingGeos(x, y + 1, tempGeoCluster);
                }
            }
        }
        if (x != width - 1) {
            if (geoBlock[x + 1][y] != -1) {      // right
                if (!tempGeoCluster.contains(geoBlock[x + 1][y])) {
                    tempGeoCluster.add(geoBlock[x + 1][y]);
                    findConnectingGeos(x + 1, y, tempGeoCluster);
                }
            }
        }
        if (y != 0) {
            if (geoBlock[x][y - 1] != -1) {      // down
                if (!tempGeoCluster.contains(geoBlock[x][y - 1])) {
                    tempGeoCluster.add(geoBlock[x][y - 1]);
                    findConnectingGeos(x, y - 1, tempGeoCluster);
                }
            }
        }
        if (x != 0) {
            if (geoBlock[x - 1][y] != -1) {  // left
                if (!tempGeoCluster.contains(geoBlock[x - 1][y])) {
                    tempGeoCluster.add(geoBlock[x - 1][y]);
                    findConnectingGeos(x - 1, y, tempGeoCluster);
                }
            }
        }
        return tempGeoCluster;
    }

    /**
     * Given an integer list of GeoIDs, match the ID to the original list of GeoEntry Objects.
     *
     * @param largestCluster - List of Geo IDS that are in the largest cluster
     * @return List of GeoEntry Objects
     */
    private List<GeoEntry> getGeoEntrys(List<Integer> largestCluster) {
        List<GeoEntry> largestGeoCluster = new ArrayList<>();

        // sort the largest cluster by id first
        Collections.sort(largestCluster);
        for (Integer geoID : largestCluster) {
            for (GeoEntry geoEntry : geoEntries) {
                if (geoID == geoEntry.getGeoID()) {
                    largestGeoCluster.add(geoEntry);
                }
            }
        }
        return largestGeoCluster;
    }

    /**
     * Read in the contents of the .csv file and store each line as a GeoEntry object in a List
     *
     * @param width   - width of the GeoBlock
     * @param height  - height of the GeoBlock
     * @param csvFile - the name of the csv file
     */
    private void readCSVFile(int width, int height, String csvFile) {
        BufferedReader bufferedReader = null;
        String line, csvSplitter = ",";
        int rowCounter = 0;
        int maxGeoID = (width * height) - 1; // the maximum GeoID possible within the GeoBlock width and height constraints

        try {
            bufferedReader = new BufferedReader(new FileReader(csvFile));

            while ((line = bufferedReader.readLine()) != null) {
                rowCounter++;
                // use comma as separator
                String[] input = line.split(csvSplitter);

                if (input.length != 3) {
                    System.out.println("Error in CSV File: row " + rowCounter + " " +
                            "(Insufficient data: Each row should contain 3 data points GeoID, Occupier's Name, Date Geo was occupied)");
                    System.exit(1);
                }

                // If the GeoID is greater than the maximum GeoID calculated
                // then the csv file data is incorrect and should display an error
                if (Integer.valueOf(input[0]) > maxGeoID) {
                    System.out.println("Error in CSV File: row " +
                            rowCounter + " (Invalid value: GeoID: " + Integer.valueOf(input[0]) +
                            " is too high for the given GeoBlock constraints " + width + "x" + height);
                    System.exit(1);
                }

                // Occupier's name data input is too short. Less than 2 for a valid name
                if (input[1].trim().length() < 2) {
                    System.out.println("Error in CSV file: row " + rowCounter +
                            " (Data input too short: Occupier's name needs to be longer than one character)");
                    System.exit(1);
                }
                // Date should be
                if (input[2].trim().length() < 10) {
                    System.out.println("Error in CSV file: row " + rowCounter +
                            " (Data input too long: Date should be in YYYY-MM-DD format)");
                    System.exit(1);
                } else if (input[2].trim().length() > 10) {
                    System.out.println("Error in CSV file: row " + rowCounter +
                            " (Data input too short: Date should be in YYYY-MM-DD format)");
                    System.exit(1);
                }

                // row is valid, add it to the list of Geo objects
                geoEntries.add(new GeoEntry(Integer.valueOf(input[0]), input[1], LocalDate.parse(input[2])));
            }

        } catch (IOException e) {
            System.out.println("Error CSV File not found");
            System.exit(1);
        } catch (NumberFormatException e) {
            System.out.println("Error in CSV file: row " + rowCounter + " (Invalid value: Geo ID should be an integer)");
            System.exit(1);
        } catch (DateTimeParseException e) {
            System.out.println("Error in CSV file: row " + rowCounter +
                    " (Invalid value: Date should be in YYYY-MM-DD format)");
            System.exit(1);
        } finally {
            if (bufferedReader != null) {
                try {
                    bufferedReader.close();
                } catch (IOException e) {
                    System.out.println("Error: Failed to close BufferedReader");
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Generates 10,000 random numbers between 0 and 100,000,000
     * - This was to help create the test csv file
     */
    private void generateRandomGeoIDs() {
        int[] potentialNumbers = new int[100000000];
        Set<Integer> geoIDs = new HashSet<>();

        for (int i = 0; i < 100000000; i++) {
            potentialNumbers[i] = i;
        }

        while (geoIDs.size() != 10000) {
            int random = ThreadLocalRandom.current().nextInt(0, 100000000);
            geoIDs.add(potentialNumbers[random]);
        }
        for (int g : geoIDs) {
            System.out.println(g);
        }
    }
}