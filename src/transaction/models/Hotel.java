package transaction.models;

import transaction.exceptions.InvalidIndexException;

import java.io.Serializable;

public class Hotel implements ResourceItem, Serializable {
    private String location;
    private int price;
    private int numRooms;
    private int numAvail;
    private boolean isdeleted;

    public Hotel(String location, int price, int numRooms, int numAvail) {
        this.location = location;
        this.price = price;
        this.numRooms = numRooms;
        this.numAvail = numAvail;
        this.isdeleted = false;
    }

    public String getLocation() {
        return location;
    }

    public int getPrice() {
        return price;
    }

    public void setPrice(int price) {
        this.price = price;
    }

    public int getNumRooms() {
        return numRooms;
    }

    public void addRooms(int addRooms) {
        this.numRooms += addRooms;
        this.numAvail += addRooms;
    }

    public boolean reduceRooms(int reduceRooms) {
        if (reduceRooms > this.numAvail) {
            return false;
        }

        this.numRooms -= reduceRooms;
        this.numAvail -= reduceRooms;
        return true;
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
        return this.location;
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
        return new Hotel(getLocation(), getPrice(), getNumRooms(), getNumAvail());
    }
}
