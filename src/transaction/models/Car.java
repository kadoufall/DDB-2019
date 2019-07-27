package transaction.models;

import transaction.exceptions.InvalidIndexException;

import java.io.Serializable;

public class Car implements ResourceItem, Serializable {
    private String location;
    private int price;
    private int numCars;
    private int numAvail;
    private boolean isdeleted;

    public Car(String location, int price, int numCars, int numAvail) {
        this.location = location;
        this.price = price;
        this.numCars = numCars;
        this.numAvail = numCars;
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

    public int getNumCars() {
        return numCars;
    }

    public void addCars(int addCars) {
        this.numCars += addCars;
        this.numAvail += addCars;
    }

    public boolean reduceCars(int reduceCars) {
        if (reduceCars > this.numAvail) {
            return false;
        }

        this.numCars -= reduceCars;
        this.numAvail -= reduceCars;
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
        this.isdeleted = false;
    }

    @Override
    public Object clone() {
        return new Car(getLocation(), getPrice(), getNumCars(), getNumAvail());
    }
}
