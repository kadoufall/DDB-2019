package transaction.models;

import transaction.exceptions.InvalidIndexException;

import java.io.Serializable;

public class Flight implements ResourceItem, Serializable {
    private String flightNum;
    private int price;
    private int numSeats;
    private int numAvail;
    private boolean isdeleted;

    public Flight(String flightNum, int price, int numSeats, int numAvail) {
        this.flightNum = flightNum;
        this.price = price;
        this.numSeats = numSeats;
        this.numAvail = numAvail;
        this.isdeleted = false;
    }

    public String getFlightNum() {
        return flightNum;
    }

    public int getPrice() {
        return price;
    }

    public void setPrice(int price) {
        this.price = price;
    }

    public int getNumSeats() {
        return numSeats;
    }

    public void addSeats(int addSeats) {
        this.numSeats += addSeats;
        this.numAvail += addSeats;
    }

    public int getNumAvail() {
        return numAvail;
    }

    public void cancelResv() {
        this.numAvail += 1;
    }

    public boolean addResv() {
        if (this.numAvail < 1) {
            return false;
        }

        this.numAvail -= 1;
        return true;
    }

    @Override
    public String[] getColumnNames() {
        return new String[0];
    }

    @Override
    public String[] getColumnValues() {
        return new String[0];
    }

    @Override
    public Object getIndex(String indexName) throws InvalidIndexException {
        return null;
    }

    @Override
    public Object getKey() {
        return this.flightNum;
    }

    @Override
    public boolean isDeleted() {
        return this.isdeleted;
    }

    @Override
    public void delete() {
        this.isdeleted = true;
    }

    @Override
    public Object clone() {
        return new Flight(getFlightNum(), getPrice(), getNumSeats(), getNumAvail());
    }
}
