package transaction;

import lockmgr.DeadlockException;
import transaction.exceptions.InvalidIndexException;
import transaction.exceptions.InvalidTransactionException;
import transaction.exceptions.TransactionAbortedException;
import transaction.models.*;

import java.io.*;
import java.net.Socket;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.*;

/**
 * Workflow Controller for the Distributed Travel Reservation System.
 * <p>
 * Description: toy implementation of the WC.  In the real
 * implementation, the WC should forward calls to either RM or TM,
 * instead of doing the things itself.
 */

public class WorkflowControllerImpl extends java.rmi.server.UnicastRemoteObject
        implements WorkflowController {

    // If WC die, restart and load xids from "data/wc_xids.log"
    private final static String WC_TRANSACTION_LOG_FILENAME = "data/wc_xids.log";

    private ResourceManager rmFlights;
    private ResourceManager rmRooms;
    private ResourceManager rmCars;
    private ResourceManager rmCustomers;
    private TransactionManager tm;

    // all active xids
    private Set<Integer> xids;

    public WorkflowControllerImpl() throws RemoteException {
        this.rmFlights = null;
        this.rmRooms = null;
        this.rmCars = null;
        this.rmCustomers = null;
        this.tm = null;
        this.xids = new HashSet<>();

        // recover from die
        this.recover();

        while (!reconnect()) {
            // would be better to sleep a while
            try {
                Thread.sleep(500);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    private void recover() {
        Set<Integer> cacheXids = loadTransactionLogs();

        if (cacheXids != null) {
            this.xids = cacheXids;
        }
    }

    private Set<Integer> loadTransactionLogs() {
        File xidLog = new File(WC_TRANSACTION_LOG_FILENAME);
        ObjectInputStream oin = null;
        try {
            oin = new ObjectInputStream(new FileInputStream(xidLog));
            return (HashSet<Integer>) oin.readObject();
        } catch (Exception e) {
            return null;
        } finally {
            try {
                if (oin != null)
                    oin.close();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }
    }

    private void storeTransactionLogs(Set<Integer> xids) {
        File xidLog = new File(WC_TRANSACTION_LOG_FILENAME);
        xidLog.getParentFile().mkdirs();
        ObjectOutputStream oout = null;
        try {
            oout = new ObjectOutputStream(new FileOutputStream(xidLog));
            oout.writeObject(xids);
            oout.flush();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (oout != null)
                    oout.close();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }
    }

    // TRANSACTION INTERFACE
    public int start() throws RemoteException {
        // do not need synchronized, because tm.start() is synchronized, so xid is unique
        int xid = tm.start();
        this.xids.add(xid);

        this.storeTransactionLogs(this.xids);

        return xid;
    }

    public boolean commit(int xid)
            throws RemoteException, TransactionAbortedException, InvalidTransactionException {
        if (!this.xids.contains(xid)) {
            throw new InvalidTransactionException(xid, "commit");
        }

        boolean ret = this.tm.commit(xid);
        this.xids.remove(xid);
        this.storeTransactionLogs(this.xids);

        return ret;
    }

    public void abort(int xid) throws RemoteException, InvalidTransactionException {
        if (!this.xids.contains(xid)) {
            throw new InvalidTransactionException(xid, "abort");
        }

        this.tm.abort(xid);
        this.xids.remove(xid);
        this.storeTransactionLogs(this.xids);
    }

    // ADMINISTRATIVE INTERFACE
    public boolean addFlight(int xid, String flightNum, int numSeats, int price)
            throws RemoteException, TransactionAbortedException, InvalidTransactionException {
        // check input
        if (!this.xids.contains(xid)) {
            throw new InvalidTransactionException(xid, "addFlight");
        }
        if (flightNum == null || numSeats < 0) {
            return false;
        }

        price = price < 0 ? 0 : price;

        // find flight related to flightNum
        ResourceItem resourceItem;
        try {
            resourceItem = this.rmFlights.query(xid, this.rmFlights.getID(), flightNum);
        } catch (DeadlockException e) {
            this.abort(xid);
            throw new TransactionAbortedException(xid, e.getMessage());
        }

        // new flight
        if (resourceItem == null) {
            Flight flight = new Flight(flightNum, price, numSeats, numSeats);

            // insert
            try {
                return this.rmFlights.insert(xid, this.rmFlights.getID(), flight);
            } catch (DeadlockException e) {
                this.abort(xid);
                throw new TransactionAbortedException(xid, e.getMessage());
            }
        }

        // existing flight
        Flight flight = (Flight) resourceItem;
        flight.addSeats(numSeats);
        flight.setPrice(price);

        // update
        try {
            return this.rmFlights.update(xid, this.rmFlights.getID(), flightNum, flight);
        } catch (DeadlockException e) {
            this.abort(xid);
            throw new TransactionAbortedException(xid, e.getMessage());
        }

    }

    public boolean deleteFlight(int xid, String flightNum)
            throws RemoteException, TransactionAbortedException, InvalidTransactionException {
        // check input
        if (!this.xids.contains(xid)) {
            throw new InvalidTransactionException(xid, "deleteFlight");
        }
        if (flightNum == null) {
            return false;
        }

        try {
            ResourceItem resourceItem = this.rmFlights.query(xid, this.rmFlights.getID(), flightNum);
            if (resourceItem == null) {
                return false;
            }

            // find all related reservations
            Collection reservations = this.rmCustomers.query(
                    xid,
                    ResourceManager.TableMameReservations,
                    Reservation.INDEX_CUSTNAME,
                    flightNum);

            // has reservations, stop delete
            if (!reservations.isEmpty()) {
                return false;
            }

            // no reservations, delete
            resourceItem.delete();

            return this.rmFlights.delete(xid, this.rmFlights.getID(), flightNum);
        } catch (DeadlockException e) {
            this.abort(xid);
            throw new TransactionAbortedException(xid, e.getMessage());
        } catch (InvalidIndexException e) {
            // should not occur
            e.printStackTrace();
            return false;
        }
    }

    public boolean addRooms(int xid, String location, int numRooms, int price)
            throws RemoteException, TransactionAbortedException, InvalidTransactionException {
        // check input
        if (!this.xids.contains(xid)) {
            throw new InvalidTransactionException(xid, "addRooms");
        }
        if (location == null || numRooms < 0) {
            return false;
        }
        price = price < 0 ? 0 : price;

        // find hotel related to location
        ResourceItem resourceItem;
        try {
            resourceItem = this.rmRooms.query(xid, this.rmRooms.getID(), location);
        } catch (DeadlockException e) {
            this.abort(xid);
            throw new TransactionAbortedException(xid, e.getMessage());
        }

        // new hotel
        if (resourceItem == null) {
            Hotel hotel = new Hotel(location, price, numRooms, numRooms);

            // insert
            try {
                return this.rmRooms.insert(xid, this.rmRooms.getID(), hotel);
            } catch (DeadlockException e) {
                this.abort(xid);
                throw new TransactionAbortedException(xid, e.getMessage());
            }
        }

        // existing hotel
        Hotel hotel = (Hotel) resourceItem;
        hotel.addRooms(numRooms);
        hotel.setPrice(price);

        // update
        try {
            return this.rmRooms.update(xid, this.rmRooms.getID(), location, hotel);
        } catch (DeadlockException e) {
            this.abort(xid);
            throw new TransactionAbortedException(xid, e.getMessage());
        }
    }

    public boolean deleteRooms(int xid, String location, int numRooms)
            throws RemoteException, TransactionAbortedException, InvalidTransactionException {
        // check input
        if (!this.xids.contains(xid)) {
            throw new InvalidTransactionException(xid, "deleteRooms");
        }
        if (location == null || numRooms < 0) {
            return false;
        }

        try {
            ResourceItem resourceItem = this.rmRooms.query(xid, this.rmRooms.getID(), location);
            if (resourceItem == null) {
                return false;
            }

            Hotel hotel = (Hotel) resourceItem;

            boolean reduce = hotel.reduceRooms(numRooms);
            if (!reduce) {
                return false;
            }

            return this.rmRooms.update(xid, this.rmRooms.getID(), location, hotel);
        } catch (DeadlockException e) {
            abort(xid);
            throw new TransactionAbortedException(xid, e.getMessage());
        }

    }

    public boolean addCars(int xid, String location, int numCars, int price)
            throws RemoteException, TransactionAbortedException, InvalidTransactionException {
        // check input
        if (!this.xids.contains(xid)) {
            throw new InvalidTransactionException(xid, "addCars");
        }
        if (location == null || numCars < 0) {
            return false;
        }
        price = price < 0 ? 0 : price;

        // find car related to location
        ResourceItem resourceItem;
        try {
            resourceItem = this.rmCars.query(xid, this.rmCars.getID(), location);
        } catch (DeadlockException e) {
            this.abort(xid);
            throw new TransactionAbortedException(xid, e.getMessage());
        }

        // new car
        if (resourceItem == null) {
            Car car = new Car(location, price, numCars, numCars);

            // insert
            try {
                return this.rmCars.insert(xid, this.rmCars.getID(), car);
            } catch (DeadlockException e) {
                this.abort(xid);
                throw new TransactionAbortedException(xid, e.getMessage());
            }
        }

        // existing car
        Car car = (Car) resourceItem;
        car.addCars(numCars);
        car.setPrice(price);

        // update
        try {
            return this.rmCars.update(xid, this.rmCars.getID(), location, car);
        } catch (DeadlockException e) {
            this.abort(xid);
            throw new TransactionAbortedException(xid, e.getMessage());
        }
    }

    public boolean deleteCars(int xid, String location, int numCars)
            throws RemoteException, TransactionAbortedException, InvalidTransactionException {
        // check input
        if (!this.xids.contains(xid)) {
            throw new InvalidTransactionException(xid, "deleteCars");
        }
        if (location == null || numCars < 0) {
            return false;
        }

        try {
            ResourceItem resourceItem = this.rmCars.query(xid, this.rmCars.getID(), location);

            if (resourceItem == null) {
                return false;
            }

            Car car = (Car) resourceItem;

            boolean reduce = car.reduceCars(numCars);
            if (!reduce) {
                return false;
            }

            return this.rmCars.update(xid, this.rmCars.getID(), location, car);
        } catch (DeadlockException e) {
            abort(xid);
            throw new TransactionAbortedException(xid, e.getMessage());
        }
    }

    public boolean newCustomer(int xid, String custName)
            throws RemoteException, TransactionAbortedException, InvalidTransactionException {
        // check input
        if (!this.xids.contains(xid)) {
            throw new InvalidTransactionException(xid, "newCustomer");
        }
        if (custName == null) {
            return false;
        }

        ResourceItem resourceItem;
        try {
            resourceItem = this.rmCustomers.query(xid, this.rmCustomers.getID(), custName);
        } catch (DeadlockException e) {
            this.abort(xid);
            throw new TransactionAbortedException(xid, e.getMessage());
        }

        // exist
        if (resourceItem != null) {
            return true;
        }

        // add new customer
        Customer customer = new Customer(custName);
        try {
            return this.rmCustomers.insert(xid, this.rmCustomers.getID(), customer);
        } catch (DeadlockException e) {
            this.abort(xid);
            throw new TransactionAbortedException(xid, e.getMessage());
        }
    }

    public boolean deleteCustomer(int xid, String custName)
            throws RemoteException, TransactionAbortedException, InvalidTransactionException {
        // check input
        if (!this.xids.contains(xid)) {
            throw new InvalidTransactionException(xid, "deleteCustomer");
        }
        if (custName == null) {
            return false;
        }

        try {
            ResourceItem resourceItem = this.rmCustomers.query(xid, this.rmCustomers.getID(), custName);
            if (resourceItem == null) {
                return false;
            }

            // find all reservations related to custName
            Collection reservations = this.rmCustomers.query(
                    xid,
                    ResourceManager.TableMameReservations,
                    Reservation.INDEX_CUSTNAME,
                    custName
            );

            // cancel each reservation
            for (Object obj : reservations) {
                Reservation reservation = (Reservation) obj;
                String resvKey = reservation.getResvKey();
                int resvType = reservation.getResvType();

                if (resvType == Reservation.RESERVATION_TYPE_FLIGHT) {
                    Flight flight = (Flight) this.rmFlights.query(xid, this.rmFlights.getID(), resvKey);
                    flight.cancelResv();
                    this.rmFlights.update(xid, this.rmFlights.getID(), resvKey, flight);
                } else if (resvType == Reservation.RESERVATION_TYPE_HOTEL) {
                    Hotel hotel = (Hotel) this.rmRooms.query(xid, this.rmRooms.getID(), resvKey);
                    hotel.cancelResv();
                    this.rmRooms.update(xid, this.rmRooms.getID(), resvKey, hotel);
                } else if (resvType == Reservation.RESERVATION_TYPE_CAR) {
                    Car car = (Car) this.rmCars.query(xid, this.rmCars.getID(), resvKey);
                    car.cancelResv();
                    this.rmCars.update(xid, this.rmCars.getID(), resvKey, car);
                }
            }

            this.rmCustomers.delete(
                    xid,
                    ResourceManager.TableMameReservations,
                    Reservation.INDEX_CUSTNAME,
                    custName);


            return this.rmCustomers.delete(xid, this.rmCustomers.getID(), custName);
        } catch (DeadlockException e) {
            this.abort(xid);
            throw new TransactionAbortedException(xid, e.getMessage());
        } catch (InvalidIndexException e) {
            // should not occur
            e.printStackTrace();
            return false;
        }
    }


    // QUERY INTERFACE
    public int queryFlight(int xid, String flightNum)
            throws RemoteException, TransactionAbortedException, InvalidTransactionException {
        if (!this.xids.contains(xid)) {
            throw new InvalidTransactionException(xid, "queryFlight");
        }
        if (flightNum == null) {
            return -1;
        }

        try {
            ResourceItem resourceItem = this.rmFlights.query(xid, this.rmFlights.getID(), flightNum);
            if (resourceItem == null) {
                return -1;
            }

            return ((Flight) resourceItem).getNumAvail();
        } catch (DeadlockException e) {
            this.abort(xid);
            throw new TransactionAbortedException(xid, e.getMessage());
        }
    }

    public int queryFlightPrice(int xid, String flightNum)
            throws RemoteException, TransactionAbortedException, InvalidTransactionException {
        if (!this.xids.contains(xid)) {
            throw new InvalidTransactionException(xid, "queryFlightPrice");
        }
        if (flightNum == null) {
            return -1;
        }

        try {
            ResourceItem resourceItem = this.rmFlights.query(xid, this.rmFlights.getID(), flightNum);
            if (resourceItem == null) {
                return -1;
            }

            return ((Flight) resourceItem).getPrice();
        } catch (DeadlockException e) {
            this.abort(xid);
            throw new TransactionAbortedException(xid, e.getMessage());
        }
    }

    public int queryRooms(int xid, String location)
            throws RemoteException, TransactionAbortedException, InvalidTransactionException {
        if (!this.xids.contains(xid)) {
            throw new InvalidTransactionException(xid, "queryRooms");
        }

        if (location == null) {
            return -1;
        }

        try {
            ResourceItem resourceItem = this.rmRooms.query(xid, this.rmRooms.getID(), location);
            if (resourceItem == null) {
                return -1;
            }

            return ((Hotel) resourceItem).getNumAvail();
        } catch (DeadlockException e) {
            this.abort(xid);
            throw new TransactionAbortedException(xid, e.getMessage());
        }
    }

    public int queryRoomsPrice(int xid, String location)
            throws RemoteException, TransactionAbortedException, InvalidTransactionException {
        if (!this.xids.contains(xid)) {
            throw new InvalidTransactionException(xid, "queryRoomsPrice");
        }

        if (location == null) {
            return -1;
        }

        try {
            ResourceItem resourceItem = this.rmRooms.query(xid, this.rmRooms.getID(), location);
            if (resourceItem == null) {
                return -1;
            }

            return ((Hotel) resourceItem).getPrice();
        } catch (DeadlockException e) {
            this.abort(xid);
            throw new TransactionAbortedException(xid, e.getMessage());
        }
    }

    public int queryCars(int xid, String location)
            throws RemoteException, TransactionAbortedException, InvalidTransactionException {
        if (!this.xids.contains(xid)) {
            throw new InvalidTransactionException(xid, "queryCars");
        }
        if (location == null) {
            return -1;
        }

        try {
            ResourceItem resourceItem = this.rmCars.query(xid, this.rmCars.getID(), location);
            if (resourceItem == null) {
                return -1;
            }

            return ((Car) resourceItem).getNumAvail();
        } catch (DeadlockException e) {
            this.abort(xid);
            throw new TransactionAbortedException(xid, e.getMessage());
        }
    }

    public int queryCarsPrice(int xid, String location)
            throws RemoteException, TransactionAbortedException, InvalidTransactionException {
        if (!this.xids.contains(xid)) {
            throw new InvalidTransactionException(xid, "queryCarsPrice");
        }

        if (location == null) {
            return -1;
        }

        try {
            ResourceItem resourceItem = this.rmCars.query(xid, this.rmCars.getID(), location);
            if (resourceItem == null) {
                return -1;
            }

            return ((Car) resourceItem).getPrice();
        } catch (DeadlockException e) {
            this.abort(xid);
            throw new TransactionAbortedException(xid, e.getMessage());
        }
    }

    public int queryCustomerBill(int xid, String custName)
            throws RemoteException, TransactionAbortedException, InvalidTransactionException {
        if (!this.xids.contains(xid)) {
            throw new InvalidTransactionException(xid, "queryCustomerBill");
        }

        if (custName == null) {
            return -1;
        }

        try {
            ResourceItem resourceItem = this.rmCustomers.query(xid, this.rmCustomers.getID(), custName);
            if (resourceItem == null) {
                return -1;
            }

            Collection reservations = this.rmCustomers.query(
                    xid,
                    ResourceManager.TableMameReservations,
                    Reservation.INDEX_CUSTNAME,
                    custName
            );

            int totalPrice = 0;
            for (Object obj : reservations) {
                Reservation reservation = (Reservation) obj;
                String resvKey = reservation.getResvKey();
                int resvType = reservation.getResvType();

                if (resvType == Reservation.RESERVATION_TYPE_FLIGHT) {
                    Flight flight = (Flight) this.rmFlights.query(xid, this.rmFlights.getID(), resvKey);
                    totalPrice += flight.getPrice();
                } else if (resvType == Reservation.RESERVATION_TYPE_HOTEL) {
                    Hotel hotel = (Hotel) this.rmRooms.query(xid, this.rmRooms.getID(), resvKey);
                    totalPrice += hotel.getPrice();
                } else if (resvType == Reservation.RESERVATION_TYPE_CAR) {
                    Car car = (Car) this.rmCars.query(xid, this.rmCars.getID(), resvKey);
                    totalPrice += car.getPrice();
                }
            }

            return totalPrice;
        } catch (DeadlockException e) {
            this.abort(xid);
            throw new TransactionAbortedException(xid, e.getMessage());
        } catch (InvalidIndexException e) {
            e.printStackTrace();
            return -1;
        }
    }

    // RESERVATION INTERFACE
    public boolean reserveFlight(int xid, String custName, String flightNum)
            throws RemoteException, TransactionAbortedException, InvalidTransactionException {
        if (!this.xids.contains(xid)) {
            throw new InvalidTransactionException(xid, "reserveFlight");
        }

        if (custName == null || flightNum == null) {
            return false;
        }

        try {
            ResourceItem resourceCustomer = this.rmCustomers.query(xid, this.rmCustomers.getID(), custName);
            if (resourceCustomer == null) {
                return false;
            }
            ResourceItem resourceItemFlight = this.rmFlights.query(xid, this.rmFlights.getID(), flightNum);
            if (resourceItemFlight == null) {
                return false;
            }

            Flight flight = (Flight) resourceItemFlight;
            if (!flight.addResv()) {
                return false;
            }

            this.rmFlights.update(xid, this.rmFlights.getID(), flightNum, flight);

            Reservation reservation = new Reservation(custName, Reservation.RESERVATION_TYPE_FLIGHT, flightNum);
            return this.rmCustomers.insert(xid, ResourceManager.TableMameReservations, reservation);
        } catch (DeadlockException e) {
            this.abort(xid);
            throw new TransactionAbortedException(xid, e.getMessage());
        }
    }

    public boolean reserveCar(int xid, String custName, String location)
            throws RemoteException, TransactionAbortedException, InvalidTransactionException {
        if (!this.xids.contains(xid)) {
            throw new InvalidTransactionException(xid, "reserveCar");
        }

        if (custName == null || location == null) {
            return false;
        }

        try {
            ResourceItem resourceCustomer = this.rmCustomers.query(xid, this.rmCustomers.getID(), custName);
            if (resourceCustomer == null) {
                return false;
            }
            ResourceItem resourceItemCar = this.rmCars.query(xid, this.rmCars.getID(), location);
            if (resourceItemCar == null) {
                return false;
            }

            Car car = (Car) resourceItemCar;
            if (!car.addResv()) {
                return false;
            }

            this.rmCars.update(xid, this.rmCars.getID(), location, car);

            Reservation reservation = new Reservation(custName, Reservation.RESERVATION_TYPE_CAR, location);
            return this.rmCustomers.insert(xid, ResourceManager.TableMameReservations, reservation);
        } catch (DeadlockException e) {
            this.abort(xid);
            throw new TransactionAbortedException(xid, e.getMessage());
        }
    }

    public boolean reserveRoom(int xid, String custName, String location)
            throws RemoteException, TransactionAbortedException, InvalidTransactionException {
        if (!this.xids.contains(xid)) {
            throw new InvalidTransactionException(xid, "reserveRoom");
        }

        if (custName == null || location == null) {
            return false;
        }

        try {
            ResourceItem resourceCustomer = this.rmCustomers.query(xid, this.rmCustomers.getID(), custName);
            if (resourceCustomer == null) {
                return false;
            }
            ResourceItem resourceItemRoom = this.rmRooms.query(xid, this.rmRooms.getID(), location);
            if (resourceItemRoom == null) {
                return false;
            }

            Hotel hotel = (Hotel) resourceItemRoom;
            if (!hotel.addResv()) {
                return false;
            }

            this.rmRooms.update(xid, this.rmRooms.getID(), location, hotel);

            Reservation reservation = new Reservation(custName, Reservation.RESERVATION_TYPE_HOTEL, location);
            return this.rmCustomers.insert(xid, ResourceManager.TableMameReservations, reservation);
        } catch (DeadlockException e) {
            this.abort(xid);
            throw new TransactionAbortedException(xid, e.getMessage());
        }
    }

    public boolean reserveItinerary(int xid, String custName, List flightNumList, String location, boolean needCar, boolean needRoom)
            throws RemoteException, TransactionAbortedException, InvalidTransactionException {
        if (!this.xids.contains(xid)) {
            throw new InvalidTransactionException(xid, "reserveItinerary");
        }

        if (custName == null || location == null || flightNumList == null) {
            return false;
        }

        try {
            ResourceItem resourceCustomer = this.rmCustomers.query(xid, this.rmCustomers.getID(), custName);
            if (resourceCustomer == null) {
                return false;
            }

            List<String> flights = new ArrayList<>();
            for (Object obj : flightNumList) {
                String flightNum = (String) obj;
                ResourceItem resourceFlight = this.rmFlights.query(xid, this.rmFlights.getID(), flightNum);
                if (resourceFlight == null) {
                    return false;
                }

                Flight flight = (Flight) resourceFlight;
                if (flight.getNumAvail() < 1) {
                    return false;
                }
                flights.add(flightNum);
            }

            if (needCar) {
                ResourceItem resourceCar = this.rmCars.query(xid, this.rmCars.getID(), location);
                if (resourceCar == null) {
                    return false;
                }

                Car car = (Car) resourceCar;
                if (car.getNumAvail() < 1) {
                    return false;
                }
            }

            if (needRoom) {
                ResourceItem resourceRoom = this.rmRooms.query(xid, this.rmRooms.getID(), location);
                if (resourceRoom == null) {
                    return false;
                }

                Hotel hotel = (Hotel) resourceRoom;
                if (hotel.getNumAvail() < 1) {
                    return false;
                }
            }

            for (String flightNum : flights) {
                boolean ret = this.reserveFlight(xid, custName, flightNum);
                if (!ret) {
                    return false;
                }
            }

            if (needCar) {
                boolean ret = this.reserveCar(xid, custName, location);
                if (!ret) {
                    return false;
                }
            }

            if (needRoom) {
                return this.reserveRoom(xid, custName, location);
            }

            return true;
        } catch (DeadlockException e) {
            this.abort(xid);
            throw new TransactionAbortedException(xid, e.getMessage());
        }
    }

    // TECHNICAL/TESTING INTERFACE
    public boolean reconnect() throws RemoteException {
        String rmiPort = System.getProperty("rmiPort");
        rmiPort = Utils.getOriginRmiport(rmiPort);

        try {
            Registry registry = LocateRegistry.getRegistry(Utils.getHostname(), 3345, Socket::new);

            rmFlights = (ResourceManager) registry.lookup(rmiPort + ResourceManager.RMINameFlights);
            System.out.println("WC bound to RMFlights");

            rmRooms = (ResourceManager) registry.lookup(rmiPort + ResourceManager.RMINameRooms);
            System.out.println("WC bound to RMRooms");

            rmCars = (ResourceManager) registry.lookup(rmiPort + ResourceManager.RMINameCars);
            System.out.println("WC bound to RMCars");

            rmCustomers = (ResourceManager) registry.lookup(rmiPort + ResourceManager.RMINameCustomers);
            System.out.println("WC bound to RMCustomers");

            tm = (TransactionManager) registry.lookup(rmiPort + TransactionManager.RMIName);
            System.out.println("WC bound to TM");
        } catch (Exception e) {
            System.err.println("WC cannot bind to some component:" + e);
            return false;
        }

        try {
            if (rmFlights.reconnect() && rmRooms.reconnect() &&
                    rmCars.reconnect() && rmCustomers.reconnect()) {
                return true;
            }
        } catch (Exception e) {
            System.err.println("Some RM cannot reconnect:" + e);
            return false;
        }

        return false;
    }

    public boolean dieNow(String who) throws RemoteException {
        if (who.equals(TransactionManager.RMIName) || who.equals("ALL")) {
            try {
                tm.dieNow();
            } catch (RemoteException e) {
//                e.printStackTrace();
            }
        }
        if (who.equals(ResourceManager.RMINameFlights) || who.equals("ALL")) {
            try {
                rmFlights.dieNow();
            } catch (RemoteException e) {
//                e.printStackTrace();
            }
        }
        if (who.equals(ResourceManager.RMINameRooms) || who.equals("ALL")) {
            try {
                rmRooms.dieNow();
            } catch (RemoteException e) {
//                e.printStackTrace();
            }
        }
        if (who.equals(ResourceManager.RMINameCars) || who.equals("ALL")) {
            try {
                rmCars.dieNow();
            } catch (RemoteException e) {
//                e.printStackTrace();
            }
        }
        if (who.equals(ResourceManager.RMINameCustomers) || who.equals("ALL")) {
            try {
                rmCustomers.dieNow();
            } catch (RemoteException e) {
//                e.printStackTrace();
            }
        }
        if (who.equals(WorkflowController.RMIName) || who.equals("ALL")) {
            System.exit(1);
        }
        return true;
    }

    private boolean dieRMIWhen(String who, String time) {
        ResourceManager resourceManager = null;

        switch (who) {
            case ResourceManager.RMINameFlights:
                resourceManager = this.rmFlights;
                break;
            case ResourceManager.RMINameRooms:
                resourceManager = this.rmRooms;
                break;
            case ResourceManager.RMINameCars:
                resourceManager = this.rmCars;
                break;
            case ResourceManager.RMINameCustomers:
                resourceManager = this.rmCustomers;
                break;
            default:
                System.err.println("Error error in WC dieRMIWhen(): Invalid RMIName");
                break;
        }

        try {
            resourceManager.setDieTime(time);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return true;
    }

    public boolean dieRMAfterEnlist(String who) throws RemoteException {
        return this.dieRMIWhen(who, "AfterEnlist");
    }

    public boolean dieRMBeforePrepare(String who) throws RemoteException {
        return this.dieRMIWhen(who, "BeforePrepare");
    }

    public boolean dieRMAfterPrepare(String who) throws RemoteException {
        return this.dieRMIWhen(who, "AfterPrepare");
    }

    public boolean dieTMBeforeCommit() throws RemoteException {
        this.tm.setDieTime("BeforeCommit");
        return true;
    }

    public boolean dieTMAfterCommit() throws RemoteException {
        this.tm.setDieTime("AfterCommit");
        return true;
    }

    public boolean dieRMBeforeCommit(String who) throws RemoteException {
        return this.dieRMIWhen(who, "BeforeCommit");
    }

    public boolean dieRMBeforeAbort(String who) throws RemoteException {
        return this.dieRMIWhen(who, "BeforeAbort");
    }

    public static void main(String[] args) {
        System.setSecurityManager(new SecurityManager());

        String rmiPort = System.getProperty("rmiPort");
        rmiPort = Utils.getOriginRmiport(rmiPort);

        try {
            WorkflowControllerImpl obj = new WorkflowControllerImpl();
            Registry registry = LocateRegistry.getRegistry(Utils.getHostname(), 3345, Socket::new);
            registry.rebind(rmiPort + WorkflowController.RMIName, obj);
            System.out.println("WC bound");
        } catch (Exception e) {
            System.err.println("WC not bound:" + e);
            System.exit(1);
        }
    }
}
