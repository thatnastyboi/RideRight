package bg.sofia.uni.fmi.mjt.itinerary;

import bg.sofia.uni.fmi.mjt.itinerary.exception.CityNotKnownException;
import bg.sofia.uni.fmi.mjt.itinerary.exception.NoPathToDestinationException;

import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.SequencedCollection;
import java.util.Set;

public class RideRight implements ItineraryPlanner {

    private final List<Journey> schedule;

    public RideRight(List<Journey> schedule) {
        this.schedule = schedule;
    }

    private ArrayList<City> listAllCities(List<Journey> schedule) {

        ArrayList<City> list = new ArrayList<>();

        for (Journey j : schedule) {

            if (!list.contains(j.from())) {
                list.add(j.from());
            }

            if (!list.contains(j.to())) {
                list.add(j.to());
            }
        }

        return list;
    }

    public List<Journey> reconstructPath(Map<City, Journey> cameFrom, City current) {

//        LinkedHashSet<Journey> path = new LinkedHashSet<>();

        List<Journey> path = new ArrayList<>();

        while (cameFrom.containsKey(current)) {

            path.add(cameFrom.get(current));
            current = cameFrom.get(current).to();
        }

//        System.out.println("\n" + path + "\n");

        Collections.reverse(path);

        return path;
    }

    public SequencedCollection<Journey> findCheapestPath(City start, City destination, boolean allowTransfer)
            throws CityNotKnownException, NoPathToDestinationException {

        if (!listAllCities(schedule).contains(start)) {
            throw new CityNotKnownException("start city not found");
        }

        if (!listAllCities(schedule).contains(destination)) {

            throw new CityNotKnownException("destination city not found");
        }

        //create linked hash set for the cheapest path
        List<Journey> cheapestPath = new ArrayList<>();

        //create map to reconstruct the path once we've found the destination
        Map<City, Journey> cameFrom = new LinkedHashMap<>();

        Map<City, BigDecimal> gScore = new HashMap<>();
        Map<City, BigDecimal> fScore = new HashMap<>();

        for (City c : listAllCities(schedule)) {
            gScore.put(c, BigDecimal.valueOf(999999));
            fScore.put(c, BigDecimal.valueOf(999999));
        }

        gScore.replace(start, BigDecimal.valueOf(0));
        fScore.replace(start, start.calculateHeuristics(destination));

        Set<City> closedList = new HashSet<>();
        PriorityQueue<City> openList = new PriorityQueue<>(new Comparator<City>() {
            @Override
            public int compare(City first, City second) {

                if (fScore.get(first).equals(fScore.get(second))) {

                    return first.getName().compareTo(second.getName());
                }

                return fScore.get(first).compareTo(fScore.get(second));
            }
        });

        openList.add(start);

        int iterations = 0;

        while (!openList.isEmpty()) {
            if (!allowTransfer && iterations > 0) {

                break;
            } else {

                City current = openList.poll();

                if (current.equals(destination)) {
                    System.out.println(cameFrom);
                    return cheapestPath = reconstructPath(cameFrom, start);
                }

                closedList.add(current);

                for (Journey j : schedule) {

                    if (j.from().equals(current)) {

                        City neighbor = j.to();


                        if (!closedList.contains(neighbor)) {

                            BigDecimal totalGScore = gScore.get(current)
                                    .add(j.price()
                                            .multiply((BigDecimal.ONE.add(j.vehicleType().getGreenTax()))));

                            System.out.println(totalGScore + " " + current + " " + neighbor);

                            if (totalGScore.compareTo(gScore.get(neighbor)) < 0) {

                                cameFrom.put(current, j);
                                //todo: masiven fix - problema e che vinagi se dobavq toq j shtoto vinagi
                                // default value e 999999 (ne iskam da e null che shte vzema da opleskam neshto) i
                                // posle ot tova reconstructa se ebe i ne out-va kakvoto trqbva

                                gScore.put(neighbor, totalGScore);
                                fScore.put(neighbor, totalGScore.add(neighbor.calculateHeuristics(destination)));

                                if (!openList.contains(neighbor)) {

                                    openList.add(neighbor);
                                }
                            }
                        }
                    }
                }
            }
            ++iterations;
        }

        if (cheapestPath.isEmpty()) {
            throw new NoPathToDestinationException("there is no such possible path");
        }

        return cheapestPath;
    }
}
